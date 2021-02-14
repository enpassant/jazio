package fp.io;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

public interface Platform {
    ExecutorService getBlocking();
    ExecutorService getExecutor();
    ExecutorService getForkJoin();
    ScheduledExecutorService getScheduler();

    default <V> CompletablePromise<V> toCompletablePromise(Future<V> future) {
        return CompletablePromise.of(this, future);
    }

    void shutdown();
}
