package fp.io;

import fp.util.Either;
import fp.util.ExceptionFailure;
import fp.util.Failure;
import fp.util.Left;
import java.util.concurrent.Future;

public interface Runtime {
    default <F, R> Either<Cause<F>, R> unsafeRun(IO<F, R> io) {
        final Either<Failure, Either<Cause<F>, R>> eitherValue =
                ExceptionFailure.tryCatch(() -> unsafeRunAsync(io).get());

        Either<Cause<F>, R> result = eitherValue.fold(
                failure -> Left.of(Cause.die(failure)),
                success -> success
        );

//        result.forEachLeft(cause -> {
//            if (!cause.isFail()) {
//                final Failure failure = cause.getFailure();
//                if (failure instanceof ExceptionFailure) {
//                    ExceptionFailure exceptionFailure = (ExceptionFailure) failure;
//                    exceptionFailure.throwable.printStackTrace(System.err);
//                }
//            }
//        });
        return result;
    }

    default <F, R> Future<Either<Cause<F>, R>> unsafeRunAsync(IO<F, R> io) {
        FiberContext<F, R> fiberContext = createFiberContext();
        return fiberContext.runAsync(io);
    }

    <F, R> FiberContext<F, R> createFiberContext();
}
