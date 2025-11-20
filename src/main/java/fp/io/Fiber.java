package fp.io;

import fp.util.Either;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Logger;

public interface Fiber<F, R> {
    Logger LOG = Logger.getLogger(Fiber.class.getName());

    long number();

    boolean isDone();

    Either<Cause<F>, R> join();

    Either<Cause<F>, R> getValue();

    Either<Cause<F>, R> getCompletedValue();

    IO<F, Void> interrupt();

    void interruptSleeping();

    @SuppressWarnings("unchecked")
    default Future<RaceResult<F, R>> raceWith(Fiber<F, R> that) {
        LOG.finer(() -> "RW This: " + number() + " That: " + that.number());
        CompletableFuture<Fiber<F, Object>> winner = new CompletableFuture<>();
        ((Fiber<F, Object>) this).register(winner);
        ((Fiber<F, Object>) that).register(winner);
        return winner.thenApply(winnerFiber -> {
            LOG.finer(() -> "RW Winner: " + winnerFiber.number());
            return new RaceResult<>(this, that, winnerFiber.number() == this.number());
        });
    }

    void register(CompletableFuture<Fiber<F, R>> observer);

    void andThen(final Consumer<Fiber<F, R>> consumer);
}
