package fp.io;

import fp.util.Either;
import fp.util.Failure;
import fp.util.Left;
import fp.util.Right;
import fp.util.Statement;
import fp.util.ThrowingStatement;
import fp.util.ThrowingSupplier;
import fp.util.Tuple2;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

public abstract class IO<F, R> {
    Tag tag;
    Optional<String> nameOptional = Optional.empty();

    public IO<F, R> setName(String name) {
        nameOptional = Optional.ofNullable(name);
        return this;
    }

    public Optional<String> getNameOptional() {
        return nameOptional;
    }

    public static <F, R> IO<F, R> absolve(IO<F, Either<F, R>> io) {
        return io.flatMap(either -> either.fold(
                failure -> IO.fail(Cause.fail(failure)),
                IO::succeed
        ));
    }

    public static <C, F, R> IO<F, R> accessM(
            Class<C> contextClass,
            Function<C, IO<F, R>> fn
    ) {
        return new Access<>(contextClass.getName(), contextClass, fn);
    }

    public static <C, F, R> IO<F, R> access(
            Class<C> contextClass,
            Function<C, R> fn
    ) {
        return new Access<>(contextClass.getName(), contextClass, r -> IO.succeed(fn.apply(r)));
    }

    public static <C, F, R> IO<F, R> accessM(
            String context,
            Class<C> contextClass,
            Function<C, IO<F, R>> fn
    ) {
        return new Access<>(context, contextClass, fn);
    }

    public static <C, F, R> IO<F, R> access(
            String context,
            Class<C> contextClass,
            Function<C, R> fn
    ) {
        return new Access<>(context, contextClass, r -> IO.succeed(fn.apply(r)));
    }

    public <R2> IO<F, R2> andThen(IO<F, R2> fnIO) {
        return this.foldM(
                failure -> fnIO,
                success -> fnIO
        );
    }

    public IO<F, R> blocking() {
        return new Blocking<>(this);
    }

    public static <F, R> IO<F, R> call(Supplier<IO<F, R>> supplier) {
        return new Call<>(supplier);
    }

    public static <F, R> IO<F, R> succeed(R r) {
        return new Succeed<>(r);
    }

    public <F2> IO<F2, Either<F, R>> either() {
        return foldM(
                failure -> IO.succeed(Left.of(failure)),
                success -> IO.succeed(Right.of(success))
        );
    }

    public static <F, R> IO<F, R> fail(Cause<F> f) {
        return new Fail<>(f);
    }

    public <F2, R2> IO<F2, R2> foldCauseM(
            Function<Cause<F>, IO<F2, R2>> failure,
            Function<R, IO<F2, R2>> success
    ) {
        return new Fold<>(
                this,
                failure,
                success
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <F2, R2> IO<F2, R2> foldM(
            Function<F, IO<F2, R2>> failure,
            Function<R, IO<F2, R2>> success
    ) {
        return new Fold<F, F2, R, R2>(
                this,
                cause -> cause.isFail() ? failure.apply(cause.getValue()) : fail((Cause) cause),
                success
        );
    }

    public IO<F, R> peek(Consumer<R> consumer) {
        return new Peek<>(this, consumer);
    }

    public <R2> IO<F, R> peekM(Function<R, IO<F, R2>> consumerIO) {
        return this.foldCauseM(
                IO::fail,
                success -> consumerIO.apply(success).map(v -> success)
        );
    }

    public IO<F, R> peekFailure(Consumer<F> consumer) {
        return this.foldM(
                failure -> {
                    consumer.accept(failure);
                    return IO.fail(Cause.fail(failure));
                },
                IO::succeed
        );
    }

    public <F2, R2> IO<F, R> peekFailureIO(Function<F, IO<F2, R2>> ioFn) {
        return this.foldM(
                failure -> ioFn.apply(failure).foldM(
                        f -> IO.fail(Cause.fail(failure)),
                        s -> IO.fail(Cause.fail(failure))

                ),
                IO::succeed
        );
    }

    public static <F> IO<F, Void> effectTotal(Statement statement) {
        return new EffectTotal<>(() -> {
            statement.call();
            return null;
        });
    }

    public static <F extends Failure> IO<F, Void> effect(
            ThrowingStatement<Throwable> statement
    ) {
        return new EffectPartial<>(() -> {
            statement.call();
            return null;
        });
    }

    public static <F, R> IO<F, R> effectTotal(Supplier<R> supplier) {
        return new EffectTotal<>(supplier);
    }

    public static <F extends Failure, R> IO<F, R> effect(
            ThrowingSupplier<R, Throwable> supplier
    ) {
        return new EffectPartial<>(supplier);
    }

    public <F2, R2> IO<F2, R2> flatMap(Function<R, IO<F2, R2>> fn) {
        return new FlatMap<>(this, fn);
    }

    public IO<F, Fiber<F, R>> fork() {
        return new Fork<>(this);
    }

    public static <F, R> IO<F, R> join(Fiber<F, R> fiber) {
        return new Join<>(fiber);
    }

    public static <F, R> IO<F, R> halt(Cause<F> cause) {
        return new Fail<>(cause);
    }

    public static <F, R> IO<F, R> interrupt() {
        return new Fail<>(Cause.interrupt());
    }

    public IO<F, R> interruptible() {
        return new InterruptStatus<>(this, true);
    }

    public IO<F, R> uninterruptible() {
        return new InterruptStatus<>(this, false);
    }

    public IO<F, R> checkInterrupt(
            Function<InterruptStatus<F, R>, IO<F, R>> fn
    ) {
        return new CheckInterrupt<>(fn);
    }

    public <R2> IO<F, R2> map(Function<R, R2> fn) {
        return new FlatMap<>(this, r -> IO.succeed(fn.apply(r)));
    }

    public <F2> IO<F2, R> mapFailure(Function<F, Cause<F2>> fn) {
        return foldM(
                failure -> IO.fail(fn.apply(failure)),
                IO::succeed
        );
    }

    public IO<F, R> onError(Consumer<Cause<F>> fn) {
        return this.foldCauseM(
                failure -> {
                    fn.accept(failure);
                    return IO.fail(failure);
                },
                IO::succeed
        );
    }

    //public IO<F, R> on(ExecutorService executor) {
    //return new Lock<F, R>(this, executor);
    //}

    public static <F, A, R, R2> IO<F, R> bracket(
            IO<F, A> acquire,
            Function<A, IO<F, R2>> release,
            Function<A, IO<F, R>> use
    ) {
        return acquire.uninterruptible().flatMap(a ->
                use.apply(a).foldCauseM(
                        cause1 -> release.apply(a).uninterruptible().foldCauseM(
                                cause2 -> IO.fail(cause1.then(cause2)),
                                value -> IO.fail(cause1)
                        ),
                        success -> release.apply(a).uninterruptible().foldCauseM(
                                IO::fail,
                                value -> IO.succeed(success)
                        )
                ).uninterruptible()
        );
    }

    public <C> IO<F, R> provide(Class<C> contextClass, C contextValue) {
        return new Provide<>(contextClass.getName(), contextClass, contextValue, this);
    }

    public <C> IO<F, R> provide(String context, Class<C> contextClass, C contextValue) {
        return new Provide<>(context, contextClass, contextValue, this);
    }

    public IO<F, R> race(
            IO<F, R> that
    ) {
        return this.fork().flatMap(fiber ->
                that.fork().flatMap(fiberThat ->
                        IO.effect(() ->
                                        fiber.raceWith(fiberThat).get()
                                ).mapFailure(Cause::die)
                                .flatMap(raceResult ->
                                        raceResult.<R>getWinner().getCompletedValue().fold(
                                                failure -> raceResult.<R>getLooser().getValue().fold(
                                                        f -> IO.fail(failure.then(f)),
                                                        IO::succeed
                                                ),
                                                success -> {
                                                    raceResult.getLooser().interrupt();
                                                    return IO.succeed(success);
                                                }
                                        ))
                )
        );
    }

    public IO<F, R> raceAttempt(
            IO<F, R> that
    ) {
        return fork().flatMap(fiber ->
                that.fork().flatMap(fiberThat ->
                        IO.effect(() ->
                                        fiber.raceWith(fiberThat).get()
                                ).mapFailure(Cause::die)
                                .peek(raceResult -> raceResult.getLooser().interrupt())
                                .flatMap(raceResult ->
                                        raceResult.<R>getWinner().getCompletedValue().fold(
                                                IO::fail,
                                                IO::succeed
                                        )
                                )
                )
        ).flatMap(r -> (r == null) ? IO.interrupt() : IO.succeed(r));
    }

    @SuppressWarnings("unchecked")
    public <F2, R2> IO<F2, R2> recover(Function<F, IO<F2, R2>> fn) {
        return foldM(
                fn,
                success -> IO.succeed((R2) success)
        );
    }

    @SuppressWarnings("unchecked")
    public <F2, R2> IO<F2, R2> recoverCause(Function<Cause<F>, IO<F2, R2>> fn) {
        return foldCauseM(
                fn,
                success -> IO.succeed((R2) success)
        );
    }

    public static <F> IO<F, Void> sleep(long nanoseconds) {
        return new Schedule<>(
                IO.unit(),
                new Scheduler.Delayer(nanoseconds),
                schedule -> IO::fail,
                schedule -> IO::succeed
        );
    }

    public IO<F, R> delay(long nanoseconds) {
        return new Schedule<>(
                this,
                new Scheduler.Delayer(nanoseconds),
                schedule -> IO::fail,
                schedule -> IO::succeed
        );
    }

    public IO<F, R> repeat(int count) {
        return new Schedule<>(
                this,
                new Scheduler.Counter(count),
                schedule -> IO::fail,
                schedule -> s -> new IO.Schedule<>(
                        schedule.io,
                        schedule.scheduler.updateState(),
                        schedule.failure,
                        schedule.success
                )
        );
    }

    public IO<F, R> retry(int count) {
        return new Schedule<>(
                this,
                new Scheduler.Counter(count),
                schedule -> f -> new IO.Schedule<>(
                        schedule.io,
                        schedule.scheduler.updateState(),
                        schedule.failure,
                        schedule.success
                ),
                schedule -> IO::succeed
        );
    }

    public IO<F, R> schedule(
            final Scheduler scheduler,
            Function<Schedule<F, R>, Function<Cause<F>, IO<F, R>>> failure,
            Function<Schedule<F, R>, Function<R, IO<F, R>>> success
    ) {
        return new Schedule<>(this, scheduler, failure, success);
    }

    public static <F, R> IO<F, Stream<R>> sequence(
            Stream<IO<F, R>> stream
    ) {
        final Builder<R> builder = Stream.builder();
        final Iterator<IO<F, R>> iterator = stream.iterator();
        if (iterator.hasNext()) {
            final IO<F, R> valueIO = iterator.next();
            return sequenceLoop(builder, iterator, valueIO);
        } else {
            return IO.succeed(Stream.of());
        }
    }

    private static <F, R> IO<F, Stream<R>> sequenceLoop(
            final Builder<R> builder,
            final Iterator<IO<F, R>> iterator,
            final IO<F, R> io
    ) {
        return io.flatMap(value -> {
            builder.accept(value);
            return IO.<F, Boolean>succeed(
                    iterator.hasNext()
            ).flatMap(hasNext -> {
                if (hasNext) {
                    final IO<F, R> valueIO = iterator.next();
                    return sequenceLoop(builder, iterator, valueIO);
                } else {
                    return io.map(r -> builder.build());
                }
            });
        });
    }

    public static <F, R> IO<F, Stream<R>> sequencePar(
            Stream<IO<F, R>> stream
    ) {
        final IO<F, Stream<Fiber<F, R>>> fiberStreamIO =
                IO.sequence(stream.map(IO::fork));
        return fiberStreamIO.flatMap(s -> IO.sequence(
                s.map(IO::join)
        ));
    }

    public static <F, R> IO<F, R> sequenceRace(
            Stream<IO<F, R>> stream
    ) {
        Builder<Fiber<F, R>> builder = Stream.builder();
        CompletableFuture<Fiber<F, R>> winner = new CompletableFuture<>();
        final IO<F, Stream<Fiber<F, R>>> fiberStreamIO =
                sequenceRaceLoop(builder, winner, stream.iterator(), IO.succeed(null))
                        .flatMap(i -> sequence(builder.build().map(IO::succeed)));
        return fiberStreamIO.flatMap(streamFiber ->
                IO.effect(() -> winner.thenApply(winnerFiber -> {
                            streamFiber
                                    .filter(f -> f != winnerFiber)
                                    .forEach(Fiber::interrupt);
                            return winnerFiber.getValue();
                        }).get())
                        .mapFailure(Cause::die)
        ).flatMap(either -> either.fold(
                IO::fail,
                IO::succeed
        ));
    }

    private static <F, R> IO<F, Object> sequenceRaceLoop(
            Builder<Fiber<F, R>> builder,
            CompletableFuture<Fiber<F, R>> winner,
            Iterator<IO<F, R>> iterator,
            IO<F, Object> io
    ) {
        return IO.<F, Boolean>succeed(
                iterator.hasNext()
        ).flatMap(hasNext -> {
            if (hasNext) {
                final IO<F, R> valueIO = iterator.next();
                final IO<F, Object> newIo = io.flatMap(r -> valueIO.fork())
                        .peek(builder)
                        .peek(f -> f.register(winner))
                        .map(i -> i);
                return sequenceRaceLoop(builder, winner, iterator, newIo);
            } else {
                return io;
            }
        });
    }

    public IO<F, R> timeout(long nanoseconds) {
        return raceAttempt(
                IO.<F, R>unit().delay(nanoseconds)
        );
    }

    public static <F, R> IO<F, R> unit() {
        return new Succeed<>(null);
    }

    public <F2, R2> IO<F2, Tuple2<R, R2>> zip(
            IO<F2, R2> that
    ) {
        return this.flatMap(r ->
                that.map(r2 ->
                        Tuple2.of(r, r2)
                ));
    }

    public <F2, R2, R3> IO<F2, R3> zipWith(
            IO<F2, R2> that,
            BiFunction<R, R2, R3> fn
    ) {
        return this.flatMap(r ->
                that.map(r2 ->
                        fn.apply(r, r2)
                ));
    }

    public <R2> IO<F, Tuple2<R, R2>> zipPar(
            IO<F, R2> that
    ) {
        return this.fork().flatMap(fiber ->
                that.fork().flatMap(fiberThat -> {
                    fiber.andThen(f ->
                            f.getCompletedValue().forEachLeft(
                                    fail -> fiberThat.interrupt())
                    );
                    fiberThat.andThen(f ->
                            f.getCompletedValue().forEachLeft(
                                    fail -> fiber.interrupt())
                    );
                    return IO.join(fiber).flatMap((R value) ->
                            IO.join(fiberThat).map((R2 valueThat) ->
                                    Tuple2.of(value, valueThat)
                            ));
                })
        );
    }

    public <R2, R3> IO<F, R3> zipParWith(
            IO<F, R2> that,
            BiFunction<R, R2, R3> fn
    ) {
        return zipPar(that)
                .map(tuple2 -> fn.apply(tuple2.first(), tuple2.second()));
    }

    enum Tag {
        Access,
        Blocking,
        Call,
        Pure,
        Fail,
        Fold,
        Fork,
        EffectTotal,
        EffectPartial,
        InterruptStatus,
        CheckInterrupt,
        FlatMap,
        Join,
        Lock,
        Peek,
        Provide,
        Schedule
    }

    enum Interruptible {
        Interruptible,
        Uninterruptible
    }

    static class Access<C, F, R> extends IO<F, R> {
        final String context;
        final Class<C> contextClass;
        final Function<C, IO<F, R>> fn;

        public Access(
                String context,
                Class<C> contextClass,
                Function<C, IO<F, R>> fn
        ) {
            tag = Tag.Access;
            this.context = context;
            this.contextClass = contextClass;
            this.fn = fn;
        }
    }

    static class Succeed<F, R> extends IO<F, R> {
        final R r;

        public Succeed(R r) {
            tag = Tag.Pure;
            this.r = r;
        }
    }

    static class Fail<F, R> extends IO<F, R> {
        final Cause<F> f;

        public Fail(Cause<F> f) {
            tag = Tag.Fail;
            this.f = f;
        }
    }

    static class EffectTotal<F, R> extends IO<F, R> {
        final Supplier<R> supplier;

        public EffectTotal(Supplier<R> supplier) {
            tag = Tag.EffectTotal;
            this.supplier = supplier;
        }
    }

    static class EffectPartial<F extends Failure, R> extends IO<F, R> {
        final ThrowingSupplier<R, Throwable> supplier;

        public EffectPartial(ThrowingSupplier<R, Throwable> supplier) {
            tag = Tag.EffectPartial;
            this.supplier = supplier;
        }
    }

    static class Blocking<F, R>
            extends IO<F, R> {
        IO<F, R> io;

        public Blocking(
                IO<F, R> io
        ) {
            tag = Tag.Blocking;
            this.io = io;
        }
    }

    static class Call<F, R> extends IO<F, R> {
        final Supplier<IO<F, R>> fn;

        public Call(Supplier<IO<F, R>> fn) {
            tag = Tag.Call;
            this.fn = fn;
        }

        @Override
        public String toString() {
            return "Call(" + fn + ")";
        }
    }

    static class Fold<F, F2, A, R>
            extends IO<F2, R>
            implements Function<A, IO<F2, R>> {
        IO<F, A> io;
        Function<Cause<F>, IO<F2, R>> failure;
        Function<A, IO<F2, R>> success;

        public Fold(
                IO<F, A> io,
                Function<Cause<F>, IO<F2, R>> failure,
                Function<A, IO<F2, R>> success
        ) {
            tag = Tag.Fold;
            this.io = io;
            this.failure = failure;
            this.success = success;
        }

        @Override
        public IO<F2, R> apply(A a) {
            return success.apply(a);
        }
    }

    static class Fork<F, R>
            extends IO<F, Fiber<F, R>> {
        IO<F, R> io;

        public Fork(
                IO<F, R> io
        ) {
            tag = Tag.Fork;
            this.io = io;
        }

        @Override
        public String toString() {
            return "Fork(" + io + ")";
        }
    }

    static class FlatMap<F, F2, R, R2> extends IO<F2, R2> {
        final IO<F, R> io;
        final Function<R, IO<F2, R2>> fn;

        public FlatMap(IO<F, R> io, Function<R, IO<F2, R2>> fn) {
            tag = Tag.FlatMap;
            this.io = io;
            this.fn = fn;
        }

        @Override
        public String toString() {
            return "FlatMap(" + io + ", " + fn + ")";
        }
    }

    static class InterruptStatus<F, R> extends IO<F, R> {
        final IO<F, R> io;
        final boolean flag;

        public InterruptStatus(
                final IO<F, R> io,
                final boolean flag
        ) {
            tag = Tag.InterruptStatus;
            this.io = io;
            this.flag = flag;
        }
    }

    static class CheckInterrupt<F, R> extends IO<F, R> {
        final Function<InterruptStatus<F, R>, IO<F, R>> fn;

        public CheckInterrupt(Function<InterruptStatus<F, R>, IO<F, R>> fn) {
            tag = Tag.CheckInterrupt;
            this.fn = fn;
        }
    }

    static class Join<F, R> extends IO<F, R> {
        final Fiber<F, R> fiber;

        public Join(final Fiber<F, R> fiber) {
            tag = Tag.Join;
            this.fiber = fiber;
        }
    }

    //static class Lock<F, R> extends IO<F, R> {
    //final IO<F, R> io;
    //final ExecutorService executor;
    //public Lock(IO<F, R> io, ExecutorService executor) {
    //tag = Tag.Lock;
    //this.io = io;
    //this.executor = executor;
    //}
    //}

    static class Peek<F, R> extends IO<F, R> {
        final IO<F, R> io;
        final Consumer<R> consumer;

        public Peek(IO<F, R> io, Consumer<R> consumer) {
            tag = Tag.Peek;
            this.io = io;
            this.consumer = consumer;
        }
    }

    static class Provide<C, F, R> extends IO<F, R> {
        final String context;
        final Class<C> contextClass;
        final Object contextValue;
        final IO<F, R> next;

        public Provide(
                String context,
                Class<C> contextClass,
                Object contextValue,
                IO<F, R> next
        ) {
            tag = Tag.Provide;
            this.context = context;
            this.contextClass = contextClass;
            this.contextValue = contextValue;
            this.next = next;
        }
    }

    static class Schedule<F, R> extends IO<F, R> {
        final IO<F, R> io;
        final Scheduler scheduler;
        final Function<Schedule<F, R>, Function<Cause<F>, IO<F, R>>> failure;
        final Function<Schedule<F, R>, Function<R, IO<F, R>>> success;

        public Schedule(
                final IO<F, R> io,
                final Scheduler scheduler,
                final Function<Schedule<F, R>, Function<Cause<F>, IO<F, R>>> failure,
                final Function<Schedule<F, R>, Function<R, IO<F, R>>> success
        ) {
            tag = Tag.Schedule;
            this.io = io;
            this.scheduler = scheduler;
            this.failure = failure;
            this.success = success;
        }
    }
}
