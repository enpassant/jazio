package fp.io;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import fp.util.Either;

public interface Fiber<F, R> {
    Either<Cause<F>, R> join();
    Either<Cause<F>, R> getValue();
    Either<Cause<F>, R> getCompletedValue();
    <C> IO<C, F, Void> interrupt();
    void interruptSleeping();
    <R2> Future<RaceResult<F, R, R2>> raceWith(Fiber<F, R2> that);
    void register(CompletableFuture<Fiber<F, R>> observer);
    void andThen(final Consumer<Fiber<F, R>> consumer);
}
