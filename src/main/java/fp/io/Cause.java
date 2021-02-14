package fp.io;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import fp.util.Either;
import fp.util.ExceptionFailure;
import fp.util.Failure;
import fp.util.GeneralFailure;
import fp.util.Left;
import fp.util.Right;
import fp.util.Tuple2;

public abstract class Cause<F> {
    protected final Failure failure;
    protected final Kind kind;

    private Cause(Failure failure, Kind cause) {
        this.failure = failure;
        this.kind = cause;
    }

    public enum Kind {
        Die,
        Fail,
        Interrupt,
        Both,
        Then
    }

    /**
     * @return the failure
     */
    public Failure getFailure() {
        return failure;
    }

    /**
     * @return the failure
     */
    public F getValue() {
        throw new UnsupportedOperationException();
    }

    /**
     * @return the cause
     */
    public Kind getKind() {
        return kind;
    }

    public static <F> Cause<F> both(Cause<F> first, Cause<F> second) {
        return new Both<F>(first, second);
    }

    public static <F> Cause<F> die(Failure failure) {
        if (failure instanceof ExceptionFailure) {
            final ExceptionFailure exceptionFailure = (ExceptionFailure) failure;
            final Throwable throwable =
                (exceptionFailure.throwable instanceof ExecutionException) ?
                    exceptionFailure.throwable.getCause() :
                    exceptionFailure.throwable;

            if (throwable instanceof CancellationException) {
                return interrupt();
            } else if (throwable instanceof InterruptedException) {
                return interrupt();
            }
        }
        return new Die<F>(failure);
    }

    public static <F> Cause<F> die(Throwable throwable) {
        return new Die<F>(ExceptionFailure.of(throwable));
    }

    public static <F> Cause<F> fail(F failureValue) {
        return new Fail<F>(failureValue);
    }

    public static <F> Cause<F> interrupt() {
        return new Interrupt<F>();
    }

    public boolean isDie() {
        return kind == Kind.Die;
    }

    public boolean isFail() {
        return kind == Kind.Fail;
    }

    public boolean isInterrupt() {
        return kind == Kind.Interrupt;
    }

    public Cause<F> then(Cause<F> second) {
        return new Then<F>(this, second);
    }

    @Override
    public String toString() {
        return "Cause(" + failure + ", " + kind + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Cause) {
            @SuppressWarnings("unchecked")
            Cause<F> cause = (Cause<F>) other;
            return Objects.equals(failure, cause.failure)
                && Objects.equals(kind, cause.kind);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return failure.hashCode() * 11 + kind.hashCode();
    }

    public static <F, R> Either<Failure, R> resultFlatten(
        Either<Cause<Failure>, R> result
    ) {
        return result.fold(
            cause -> cause.isFail() ?
                    Left.of(cause.getValue()) :
                    Left.of(cause.getFailure()),
            success -> Right.of(success)
        );
    }

    static class Die<F> extends Cause<F> {
        private Die(Failure failure) {
            super(failure, Kind.Die);
        }
    }

    static class Fail<F> extends Cause<F> {
        private final F value;

        private Fail(F value) {
            super(GeneralFailure.of(value), Kind.Fail);
            this.value = value;
        }

        @Override
        public F getValue() {
            return value;
        }

        @Override
        public String toString() {
            return "Fail(" + value + ")";
        }
    }

    static class Interrupt<F> extends Cause<F> {
        private Interrupt() {
            super(GeneralFailure.of(Kind.Interrupt), Kind.Interrupt);
        }
    }

    static class Both <F> extends Cause<F> {
        private final Cause<F> first;
        private final Cause<F> second;

        private Both(Cause<F> first, Cause<F> second) {
            super(GeneralFailure.of(Tuple2.of(first, second)), Kind.Both);

            this.first = first;
            this.second = second;
        }

        @Override
        public String toString() {
            return "Both(" + first + ", " + second + ")";
        }
    }

    static class Then <F> extends Cause<F> {
        private final Cause<F> first;
        private final Cause<F> second;

        private Then(Cause<F> first, Cause<F> second) {
            super(GeneralFailure.of(Tuple2.of(first, second)), Kind.Then);

            this.first = first;
            this.second = second;
        }

        @Override
        public String toString() {
            return "Then(" + first + ", " + second + ")";
        }
    }
}
