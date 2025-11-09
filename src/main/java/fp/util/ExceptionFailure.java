package fp.util;

public class ExceptionFailure implements Failure {
    public final Throwable throwable;

    private ExceptionFailure(Throwable throwable) {
        this.throwable = throwable;
    }

    public static ExceptionFailure of(Throwable throwable) {
        return new ExceptionFailure(throwable);
    }

    public Throwable getValue() {
        return throwable;
    }

    public static <E extends Throwable, R> Either<Failure, R> tryCatch(
            ThrowingSupplier<R, E> process
    ) {
        try {
            return Right.of(process.get());
        } catch (Throwable e) {
            return Left.of(
                    ExceptionFailure.of(e)
            );
        }
    }

    public static <E extends Throwable, F, R>
    Either<Failure, R> tryCatchFinal
            (
                    ThrowingSupplier<F, E> supplier,
                    ThrowingFunction<F, R, E> function,
                    ThrowingConsumer<F, E> finalConsumer
            ) {
        F resource = null;
        try {
            resource = supplier.get();
            return Right.of(function.apply(resource));
        } catch (Throwable e) {
            return Left.of(
                    ExceptionFailure.of(e)
            );
        } finally {
            try {
                if (resource != null) {
                    finalConsumer.accept(resource);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public String toString() {
        return "ExceptionFailure(" + throwable.toString() + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof final ExceptionFailure failure) {
            return failure.toString().equals(this.toString());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return throwable.hashCode();
    }
}
