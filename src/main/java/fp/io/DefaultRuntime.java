package fp.io;

import fp.util.Either;
import fp.util.ExceptionFailure;
import fp.util.HMap;
import fp.util.Left;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;

public class DefaultRuntime implements Runtime {
    private final HMap context;
    private final Platform platform;

    public DefaultRuntime(HMap context, Platform platform) {
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

    public <F, R> Future<Either<Cause<F>, R>> unsafeRunAsync(IO<F, R> io) {
        final FiberContext<F, R> fiberContext = createFiberContext();
        return fiberContext.runAsync((IO<F, R>) io);
    }

    public <F, R> Either<Cause<F>, R> unsafeRun(IO<F, R> io) {
        return ExceptionFailure.tryCatch(() ->
            ((ForkJoinTask<Either<Cause<F>, R>>) unsafeRunAsync(io)).get()
        ).fold(
            failure -> Left.of(Cause.die((ExceptionFailure) failure)),
            success -> success
        );
    }
}
