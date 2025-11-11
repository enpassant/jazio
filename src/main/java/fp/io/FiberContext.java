package fp.io;

import fp.io.IO.Tag;
import fp.util.Either;
import fp.util.ExceptionFailure;
import fp.util.Failure;
import fp.util.HMap;
import fp.util.Left;
import fp.util.Right;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class FiberContext<F, R> extends RecursiveTask<Either<Cause<F>, R>>
        implements Fiber<F, R>, ForkJoinPool.ManagedBlocker {
    private static volatile long counter = 0;
    private final long number;

    private final Logger LOG = Logger.getLogger(FiberContext.class.getName());

    private final ExecutorService executor;
    private final Platform platform;
    private IO<?, ?> curIo;
    private Object value = null;
    private Object valueLast = null;
    private ScheduledFuture<?> scheduledFuture = null;
    private final CompletableFuture<Void> mainFuture = null;
    private final CompletableFuture<Fiber<F, R>> observer =
            new CompletableFuture<>();
    private final AtomicReference<FiberState<F, R>> state;

    private final Deque<HMap> environments = new ArrayDeque<>();
    private final Deque<Function<?, IO<?, ?>>> stack =
            new ArrayDeque<>();

    private final Deque<Boolean> interruptStatus = new ArrayDeque<>();
    private volatile boolean interrupted = false;
    private Thread thread;
    private final Stream.Builder<Future<?>> streamFuture = Stream.builder();
    private final List<Fiber<?, ?>> fiberList = new ArrayList<>();

    public FiberContext(
            ExecutorService executor,
            HMap context,
            Platform platform
    ) {
        super();
        counter++;
        number = counter;
        this.executor = executor;

        this.thread = Thread.currentThread();
        LOG.finest(() -> "Fiber " + number + " has started. " + thread.getName());
        environments.add(
                Objects.requireNonNullElseGet(context, HMap::empty)
        );
        this.platform = platform;

        final List<CompletableFuture<Fiber<F, R>>> observers =
                new ArrayList<>();
        observers.add(observer);

        state = new AtomicReference<>(
                new Executing<>(
                        FiberStatus.Running, observers)
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Either<Cause<F>, R> compute() {
        this.thread = Thread.currentThread();

        LOG.finest(() -> "Fiber " + number + " compute " + thread.getName());
        try {
            while (curIo != null) {
                LOG.finest(() -> "Fiber " + number + " IO: " + curIo + ". " + this + thread.getName());
                if (curIo.tag == Tag.Fail || !shouldInterrupt()) {
                    Optional<String> nameOptional = curIo.getNameOptional();
                    nameOptional.ifPresent(name ->
                            LOG.finer(() -> "Fiber " + number + " IO: " + name)
                    );
                    switch (curIo.tag) {
                        case Access: {
                            var accessIo = ((IO.Access<Object, F, R>) curIo);
                            final HMap hmap = environments.peek();
                            var valueOpt = hmap.getValue(accessIo.context);
                            if (valueOpt.isPresent()) {
                                final Object o = valueOpt.get();
                                if (accessIo.contextClass.isInstance(o)) {
                                    curIo = accessIo.fn.apply(accessIo.contextClass.cast(o));
                                } else {
                                    curIo = IO.fail(Cause.fail(
                                            "Wrong context class: %1$s".formatted(
                                                    accessIo.contextClass
                                            )
                                    ));
                                }
                            } else {
                                curIo = IO.fail(Cause.fail(
                                        "Missing context: %1$s".formatted(accessIo.context)
                                ));
                            }
                            break;
                        }
                        case Blocking: {
                            final IO.Blocking<F, R> blockIo =
                                    (IO.Blocking<F, R>) curIo;
                            if (isBlockingThread) {
                                curIo = blockIo.io;
                            } else {
                                final Either<Cause<F>, R> either =
                                        runSync(blockIo.io);
                                if (either.isLeft()) {
                                    value = either.left();
                                    curIo = IO.fail(either.left());
                                } else {
                                    value = either.get();
                                    curIo = nextInstrApply(either.get());
                                }
                            }
                            break;
                        }
                        case Pure:
                            value = ((IO.Succeed<F, R>) curIo).r;
                            curIo = nextInstrApply(value);
                            break;
                        case Fail: {
                            unwindStack(stack);
                            LOG.finest(() -> "IO fail: " + ((IO.Fail<F, R>) curIo).f);
                            final Cause<F> cause = ((IO.Fail<F, R>) curIo).f;
                            if (stack.isEmpty()) {
                                final Cause<F> causeNew =
                                        (interrupted && !cause.isInterrupt()) ?
                                                cause.then(Cause.interrupt()) :
                                                cause;

                                LOG.finest(() -> "Stack empty: " + causeNew + ". " + this + thread.getName());
                                //Exception e = new RuntimeException("End");
                                //e.printStackTrace();
                                done(Left.of(causeNew));
                                return Left.of(causeNew);
                            }
                            value = cause;
                            curIo = nextInstrApply(value);
                            break;
                        }
                        case Fold: {
                            final IO.Fold<F, ?, ?, R> foldIO =
                                    (IO.Fold<F, ?, ?, R>) curIo;
                            stack.push((Function<?, IO<?, ?>>) curIo);
                            curIo = foldIO.io;
                            break;
                        }
                        case Call:
                            final IO.Call<Object, Object> callIo = (IO.Call<Object, Object>) curIo;
                            curIo = callIo.fn.get();
                            break;
                        case EffectTotal:
                            value = ((IO.EffectTotal<F, R>) curIo)
                                    .supplier.get();
                            curIo = nextInstrApply(value);
                            break;
                        case EffectPartial: {
                            Either<Failure, R> either = ExceptionFailure.tryCatch(() ->
                                    ((IO.EffectPartial<Failure, R>) curIo).supplier.get()
                            );
                            if (either.isRight()) {
                                value = either.right();
                                curIo = nextInstrApply(value);
                            } else if (((ExceptionFailure) either.left()).throwable
                                    instanceof InterruptedException
                            ) {
                                curIo = IO.fail(Cause.interrupt());
                            } else {
                                curIo = IO.fail(Cause.fail(either.left()));
                            }
                            break;
                        }
                        case FlatMap:
                            final IO.FlatMap<Object, F, Object, R> flatmapIO =
                                    (IO.FlatMap<Object, F, Object, R>) curIo;
                            stack.push(flatmapIO.fn::apply);
                            curIo = flatmapIO.io;
                            break;
                        case Fork: {
                            final IO.Fork<F, R> forkIo =
                                    (IO.Fork<F, R>) curIo;
                            final IO<F, R> ioValue;
                            if (forkIo.io.tag == IO.Tag.Blocking) {
                                ioValue = ((IO.Blocking<F, R>) forkIo.io).io;
                            } else {
                                ioValue = forkIo.io;
                            }
                            final FiberContext<F, R> fiberContext = new FiberContext<>(
                                    this.executor,
                                    environments.peek(),
                                    platform
                            );
                            fiberContext.isBlockingThread = forkIo.io.tag == IO.Tag.Blocking;
                            LOG.finest(() -> "Fiber " + number + " create " + fiberContext.number + "  fork " + thread.getName());
                            fiberList.add(fiberContext);
                            fiberContext.runAsync(ioValue);
                            value = fiberContext;

                            curIo = nextInstrApply(value);
                            break;
                        }
                        case InterruptStatus:
                            final IO.InterruptStatus<F, R> interruptStatusIo =
                                    (IO.InterruptStatus<F, R>) curIo;

                            interruptStatus.push(interruptStatusIo.flag);
                            stack.push(new InterruptExit());
                            curIo = interruptStatusIo.io;
                            break;
                        case Join:
                            final IO.Join<F, R> joinIo =
                                    (IO.Join<F, R>) curIo;

                            final Either<Cause<F>, R> either = joinIo.fiber.join();
                            LOG.finest(() -> "Join: " + either + ". " + this + thread.getName());
                            if (either.isLeft()) {
                                value = either.left();
                                curIo = IO.fail(either.left());
                            } else {
                                value = either.get();
                                curIo = nextInstrApply(either.get());
                            }
                            break;
                            /*
                        case Lock: {
                            final IO.Lock<F, R> lockIo =
                                (IO.Lock<F, R>) curIo;
                            if (lockIo.executor == this.executor) {
                                curIo = lockIo.io;
                            } else {
                                final FiberContext fiberContext =
                                    execOnNewFiber(lockIo.executor);
                                executor.submit(
                                    () -> fiberContext.evaluate(lockIo.io)
                                );
                                return;
                            }
                            break;
                        }
                        */
                        case Peek:
                            final IO.Peek<F, R> peekIO =
                                    (IO.Peek<F, R>) curIo;
                            stack.push((R r) -> {
                                peekIO.consumer.accept(r);
                                return IO.succeed(r);
                            });
                            curIo = peekIO.io;
                            value = valueLast;
                            break;
                        case Provide: {
                            final IO.Provide<Object, F, Object> provideIO =
                                    (IO.Provide<Object, F, Object>) curIo;
                            final HMap hmap = environments.peek();
                            environments.push(
                                    hmap.add(provideIO.context, provideIO.contextValue)
                            );
                            stack.push((R r) -> IO.effectTotal(() -> {
                                environments.pop();
                                return r;
                            }));
                            curIo = provideIO.next;
                            value = valueLast;
                            break;
                        }
                        case Schedule: {
                            final IO.Schedule<F, R> scheduleIO =
                                    (IO.Schedule<F, R>) curIo;
                            Scheduler.State state = scheduleIO.scheduler.getState();
                            if (state instanceof Scheduler.Execution) {
                                curIo = scheduleIO.io.foldCauseM(
                                        scheduleIO.failure.apply(scheduleIO),
                                        scheduleIO.success.apply(scheduleIO)
                                );
                            } else if (state instanceof final Scheduler.Delay delay) {
                                final ScheduledExecutorService executor =
                                        platform.getScheduler();
                                final FiberContext fiberContext = new FiberContext(
                                        this.executor,
                                        environments.peek(),
                                        platform
                                );
                                fiberList.add(fiberContext);
                                LOG.finest(() -> "Fiber " + number + " create " + fiberContext.number
                                        + "  scheduler delay: " + thread.getName()
                                        + "  fiber thread: " + fiberContext.thread.getName()
                                );
                                fiberContext.curIo = scheduleIO.io;
                                fiberContext.scheduledFuture =
                                        executor.schedule(
                                                () -> fiberContext.compute(),
                                                delay.nanoSecond,
                                                TimeUnit.NANOSECONDS
                                        );
                                final ScheduledBlocker scheduledBlocker =
                                        new ScheduledBlocker(
                                                fiberContext.scheduledFuture
                                        );
                                ForkJoinPool.managedBlock(scheduledBlocker);
                                synchronized (fiberContext) {
                                    fiberContext.scheduledFuture = null;
                                }
                                fiberList.remove(fiberContext);
                                final Either<Cause<Failure>, R> result =
                                        scheduledBlocker.result;
                                LOG.finest(() -> "Fiber " + number + " scheduler result: " + result);
                                if (result.isLeft()) {
                                    curIo = IO.fail(Cause.fail(result.left()));
                                } else {
                                    this.value = result.get();
                                    curIo = nextInstrApply(value);
                                }
                            } else {
                                if (value instanceof final Cause<?> cause) {
                                    curIo = IO.fail(cause);
                                } else {
                                    curIo = nextInstrApply(value);
                                }
                            }

                            break;
                        }
                        default:
                            curIo = IO.interrupt();
                    }
                } else {
                    curIo = IO.interrupt();
                }
            }
        } catch (CancellationException e) {
            LOG.finest(() -> "Fiber " + number + " CancellationException. " + thread.getName());
            done(Left.of(Cause.interrupt()));
            return Left.of(Cause.interrupt());
        } catch (Exception e) {
            LOG.finest(() -> "Fiber " + number + " Exception. "
                    + e.getMessage()
                    + "Thread: " + thread.getName()
            );
            done(Left.of(Cause.die(e)));
            return Left.of(Cause.die(e));
        }

        LOG.finer(() -> "Fiber " + number + " result " + value + ". " + thread.getName());
        done(Right.of((R) value));
        return Right.of((R) value);
    }

    @Override
    public void andThen(final Consumer<Fiber<F, R>> consumer) {
        if (observer.isCancelled()) {
            consumer.accept(this);
        } else {
            observer.thenAccept(consumer);
        }
    }

    @SuppressWarnings("unchecked")
    private IO<F, R> nextInstrApply(final Object value) {
        if (stack.isEmpty()) {
            return null;
        } else {
            final Function<Object, IO<?, ?>> fn =
                    (Function<Object, IO<?, ?>>) stack.pop();
            valueLast = value;
            return (IO<F, R>) fn.apply(value);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void unwindStack(Deque<Function<?, IO<?, ?>>> stack) {
        boolean unwinding = true;

        LOG.finest(() -> "Fiber " + number + " unwinding");

        while (unwinding && !stack.isEmpty()) {
            final Function<?, IO<?, ?>> fn = stack.pop();
            LOG.finest(() -> "Unwind: " + fn);
            if (fn instanceof InterruptExit) {
                popDrop(null);
            } else if (fn instanceof IO.Fold && !shouldInterrupt()) {
                stack.push(((IO.Fold) fn).failure);
                unwinding = false;
            }
        }
    }

    private void done(Either<Cause<F>, R> value) {
        final FiberState<F, R> oldState = state.get();
        if (oldState instanceof final Executing<F, R> executing) {
            final Done<F, R> doneValue = new Done<>(value);
            if (!state.compareAndSet(oldState, doneValue)) {
                done(value);
            } else {
                LOG.finer(() -> "Fiber " + number + " has done. Value: " +
                        value +
                        ". Thread: " + thread.getName());

                executing.notifyObservers(this);

                //if (scheduledFuture != null) {
                //if (!scheduledFuture.isDone()) scheduledFuture.cancel(false);
                //}

                final List<Fiber<?, ?>> copy = new ArrayList<>(fiberList);
                final Stream<Fiber<?, ?>> streamFiber = copy.stream();
                streamFiber.peek(fiber -> {
                            if (!((FiberContext<?, ?>) fiber).isDone()) {
                                LOG.finest(() -> "Fiber " + number + "->" + ((FiberContext<?, ?>) fiber).number + " call interrupt. " + thread.getName());
                                fiber.interrupt();
                            }
                        })
                        .forEach(Fiber::join);
            }
        }
    }

    @Override
    public void register(CompletableFuture<Fiber<F, R>> observer) {
        final FiberState<F, R> oldState = state.get();
        if (oldState instanceof final Executing<F, R> executing) {
            if (!state.compareAndSet(oldState, executing.addObserver(observer))) {
                register(observer);
            } else {
                LOG.finer(() -> "Fiber " + number + " register observer ");
            }
        } else {
            observer.complete(this);
        }
    }

    @Override
    public Either<Cause<F>, R> getCompletedValue() {
        final FiberState<F, R> oldState = state.get();

        final Done<F, R> done = (Done<F, R>) oldState;
        LOG.finest(() -> "Fiber " + number + " getCompletedValue. " + done.value + ". " + thread.getName());
        return done.value;
    }

    public Either<Cause<F>, R> getValue() {
        final FiberState<F, R> oldState = state.get();
        if (oldState instanceof final Executing<F, R> executing) {
            final Either<Cause<F>, R> value = this.join();
            done(value);
            return value;
            //return ExceptionFailure.tryCatch(
            //() -> executing.firstObserver().thenApply(Fiber::getCompletedValue).get()
            //).fold(
            //failure -> Left.of(Cause.die((ExceptionFailure) failure)),
            //success -> success
            //);
        } else {
            final Done<F, R> done = (Done<F, R>) oldState;
            return done.value;
        }
    }

    enum FiberStatus {
        Running,
        Suspended
    }

    private interface FiberState<F, R> {
    }

    private static class Executing<F, R> implements FiberState<F, R> {
        final FiberStatus status;
        private final List<CompletableFuture<Fiber<F, R>>> observers;

        public Executing(
                FiberStatus status,
                List<CompletableFuture<Fiber<F, R>>> observers
        ) {
            this.status = status;
            this.observers = observers;
        }

        public Executing<F, R> addObserver(CompletableFuture<Fiber<F, R>> observer) {
            this.observers.add(observer);
            return new Executing<>(this.status, this.observers);
        }

        public void notifyObservers(Fiber<F, R> value) {
            observers.forEach(future -> future.complete(value));
        }

        public CompletableFuture<Fiber<F, R>> firstObserver() {
            return observers.getFirst();
        }
    }

    private static class Done<F, R> implements FiberState<F, R> {
        final Either<Cause<F>, R> value;

        public Done(Either<Cause<F>, R> value) {
            this.value = value;
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        if (!interruptible()) return false;

        //if (scheduledFuture != null) {
        //scheduledFuture.cancel(mayInterruptIfRunning);
        //scheduledFuture = null;
        //}

        return super.cancel(mayInterruptIfRunning);
    }

    @Override
    public void interruptSleeping() {
        LOG.finest(() -> "Fiber " + number + " interruptSleeping. " + thread.getName());
        this.thread.interrupt();
    }

    @Override
    public IO<F, Void> interrupt() {
        //if (!interruptible()) {
        //LOG.finest(() -> "Fiber " + number + " cannot interruptable");
        //return IO.interrupt();
        //}

        LOG.finest(() -> "Fiber " + number + " has interrupted. ");
        interrupted = true;

        synchronized (this) {
            if (scheduledFuture != null && interruptible()) {
                scheduledFuture.cancel(false);
            }
        }

        final List<Fiber<?, ?>> copy = new ArrayList<>(fiberList);
        final Stream<Fiber<?, ?>> streamFiber = copy.stream();
        streamFiber.forEach(fiber -> {
            LOG.finest(() -> "Fiber " + number + "->" + ((FiberContext<?, ?>) fiber).number + " call interrupt. " + thread.getName());
            fiber.interrupt();
            //return fiber;
        })
        //.forEach(fiber -> fiber.join());
        ;
        //observer.cancel(false);
        //done(Left.of(Cause.interrupt()));

        LOG.finest(() -> "Fiber " + number + " is blocking: " + isBlockingThread);
        if (isBlockingThread && interruptible()) {
            this.thread.interrupt();
            //super.cancel(false);
        }
        //executor.shutdown();

        return IO.interrupt();
    }

    public IO<F, R> joinFiber() {
        Either<Cause<F>, R> value2 = getValue();
        return value2
                .fold(
                        IO::fail,
                        IO::succeed
                );
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R2> Future<RaceResult<F, R, R2>> raceWith(Fiber<F, R2> that) {
        LOG.finer(() -> "RW This: " + number + " That: " + ((FiberContext<?, ?>) that).number);
        CompletableFuture<Fiber<F, Object>> winner = new CompletableFuture<>();
        ((Fiber<F, Object>) this).register(winner);
        ((Fiber<F, Object>) that).register(winner);
        return winner.thenApply(winnerFiber -> {
            LOG.finer(() -> "RW Winner: " + ((FiberContext<?, ?>) winnerFiber).number);
            if (winnerFiber == this) {
                Either<Cause<F>, R> value = getCompletedValue();
                if (value.isRight()) {
                    LOG.finer(() -> "Interrupt: " + ((FiberContext<?, ?>) that).number);
                    that.interrupt();
                }
            } else {
                Either<Cause<F>, R> value = ((FiberContext) that).getCompletedValue();
                if (value.isRight()) {
                    LOG.finer(() -> "Interrupt: " + number);
                    interrupt();
                }
            }
            return new RaceResult<>(this, that, winnerFiber == this);
        });
    }

    private boolean interruptible() {
        return interruptStatus.isEmpty() || interruptStatus.peek();
    }

    private boolean shouldInterrupt() {
        return interrupted && interruptible();
    }

    private <A> A popDrop(A a) {
        if (!interruptStatus.isEmpty()) {
            interruptStatus.pop();
        }
        return a;
    }

    private class InterruptExit<C> implements Function<R, IO<F, R>> {
        @Override
        public IO<F, R> apply(R v) {
            boolean isInterruptible = interruptStatus.isEmpty() || interruptStatus.peek();

            if (isInterruptible) {
                popDrop(null);
                return IO.succeed(v);
            } else {
                return IO.effectTotal(() -> popDrop(v));
            }
        }
    }

    private boolean isBlockingThread = false;
    private boolean done = false;
    private Either<Cause<F>, R> result;

    @Override
    public boolean isReleasable() {
        return done;
    }

    @Override
    public boolean block() {
        this.thread = Thread.currentThread();
        final String name = thread.getName();
        thread.setName(name + ".blocking");
        isBlockingThread = true;
        result = compute();
        thread.setName(name);
        done = true;
        return true;
    }

    public Future<Either<Cause<F>, R>> runAsync(IO<F, R> io) {
        curIo = io;
        return ((ForkJoinPool) executor).submit(this);
    }

    public Either<Cause<F>, R> runSync(final IO<F, R> io)
            throws InterruptedException {
        final FiberContext<F, R> fiberContext = new FiberContext<>(
                executor,
                environments.peek(),
                platform
        );
        fiberContext.isBlockingThread = true;
        fiberContext.curIo = io;
        fiberList.add(fiberContext);

        ForkJoinPool.managedBlock(fiberContext);
        fiberList.remove(fiberContext);
        Either<Cause<F>, R> runSyncResult = fiberContext.getCompletedValue();
        LOG.finest(() -> "Fiber " + number + " runSync result: " + runSyncResult);
        return runSyncResult;
    }

    private static class ScheduledBlocker<R>
            implements ForkJoinPool.ManagedBlocker {
        private final ScheduledFuture<Either<Cause<Failure>, R>> scheduledFuture;
        private Either<Cause<Failure>, R> result = Left.of(Cause.interrupt());
        private boolean done = false;

        ScheduledBlocker(ScheduledFuture<Either<Cause<Failure>, R>> scheduledFuture) {
            this.scheduledFuture = scheduledFuture;
        }

        @Override
        public boolean isReleasable() {
            return false;
        }

        @Override
        public boolean block() {
            result = ExceptionFailure.tryCatch(scheduledFuture::get
            ).fold(
                    failure -> Left.of(Cause.fail(failure)),
                    success -> success
            );
            done = true;
            return true;
        }
    }
}
