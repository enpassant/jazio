package fp.io;

import fp.util.Either;
import fp.util.Left;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class FiberVirtual<F, R> implements Fiber<F, R> {
    private final Future<Either<Cause<F>, R>> future;
    private final FiberContext<F, R> fiberContext;

    private final Logger LOG = Logger.getLogger(FiberVirtual.class.getName());

    public FiberVirtual(
            final Future<Either<Cause<F>, R>> future,
            final FiberContext<F, R> fiberContext
    ) {
        this.future = future;
        this.fiberContext = fiberContext;
    }

    @Override
    public long number() {
        return fiberContext.number();
    }

    @Override
    public boolean isDone() {
        LOG.fine(() -> "Fiber " + number() + " is Done");
        return future.isDone();
    }

    @Override
    public Either<Cause<F>, R> join() {
        LOG.fine(() -> Thread.currentThread().getName() + ": join");
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            LOG.fine(() -> Thread.currentThread().getName() + ": InterruptedException");
            throw new RuntimeException(e);
        }
    }

    @Override
    public Either<Cause<F>, R> getValue() {
        LOG.finer(() -> "Fiber " + number() + " getValue");
        try {
            return future.get();
        } catch (InterruptedException e) {
            return Left.of(Cause.interrupt());
        } catch (ExecutionException e) {
            return Left.of(Cause.die(e));
        }
    }

    @Override
    public Either<Cause<F>, R> getCompletedValue() {
        return fiberContext.getCompletedValue();
    }

    @Override
    public IO<F, Void> interrupt() {
        LOG.fine(() -> Thread.currentThread().getName() + ": interrupt");
//        future.cancel(true);
        return fiberContext.interrupt();
    }

    @Override
    public void interruptSleeping() {
        LOG.fine(() -> Thread.currentThread().getName() + ": interruptSleeping");
        fiberContext.interruptSleeping();
    }

    @Override
    public void andThen(final Consumer<Fiber<F, R>> consumer) {
        LOG.fine(() -> Thread.currentThread().getName() + ": andThen");
        fiberContext.andThen(consumer);
    }

    @Override
    public void register(final CompletableFuture<Fiber<F, R>> observer) {
        LOG.finest(() -> Thread.currentThread().getName() + ": register");
        fiberContext.register(observer);
    }
}
