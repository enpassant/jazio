package fp.io;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class CompletablePromise<V> extends CompletableFuture<V> {
    private final Future<V> future;
    private final Platform platform;

    private CompletablePromise(final Platform platform, final Future<V> future) {
        this.future = future;
        this.platform = platform;
    }

    public static <V> CompletablePromise<V> of(
            final Platform platform,
            final Future<V> future
    ) {
        CompletablePromise<V> completablePromise =
                new CompletablePromise<>(platform, future);

        if (!future.isDone()) {
            platform.getScheduler().schedule(
                    completablePromise::tryToComplete,
                    100000,
                    TimeUnit.NANOSECONDS
            );
        }

        return completablePromise;
    }

    private void tryToComplete() {
        if (future.isDone()) {
            try {
                complete(future.get());
            } catch (InterruptedException | CancellationException e) {
                completeExceptionally(e);
            } catch (ExecutionException e) {
                completeExceptionally(e.getCause());
            }
            return;
        }

        if (future.isCancelled()) {
            cancel(true);
            return;
        }

        platform.getScheduler().schedule(
                this::tryToComplete,
                100000,
                TimeUnit.NANOSECONDS
        );
    }
}
