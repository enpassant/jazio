package fp.io;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;

import fp.util.Either;
import fp.util.ExceptionFailure;
import fp.util.Failure;
import fp.util.Left;

public class DefaultRuntime<C> implements Runtime<C> {
    private final C context;
    private final Platform platform;

    public DefaultRuntime(C context, Platform platform) {
        this.context = context;
        this.platform = platform;
    }

    public <F, R> FiberContext<F, R> createFiberContext() {
        FiberContext<F, R> fiberContext = new FiberContext<F, R>(
            platform.getForkJoin(),
            context,
            platform
        );
        return fiberContext;
    }

    public <F, R> Future<Either<Cause<F>, R>> unsafeRunAsync(IO<C, F, R> io) {
        final FiberContext<F, R> fiberContext = createFiberContext();
        return fiberContext.runAsync((IO<Object, F, R>) io);
    }

    public <F, R> Either<Cause<F>, R> unsafeRun(IO<C, F, R> io) {
        return ExceptionFailure.tryCatch(() ->
            ((ForkJoinTask<Either<Cause<F>, R>>) unsafeRunAsync(io)).get()
        ).fold(
            failure -> Left.of(Cause.die((ExceptionFailure) failure)),
            success -> success
        );
    }
}
