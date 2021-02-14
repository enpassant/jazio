package fp.util;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Left<L, R> implements Either<L, R> {
    private final L value;

    private Left(L l) {
        value = l;
    }

    public static <L, R> Left<L, R> of(L l) {
        return new Left<>(l);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <B> Either<L, B> map(Function<R, B> f) {
        return (Either<L, B>) this;
    }

    @Override
    public <B> Either<B, R> mapLeft(Function<L, B> f) {
        return new Left<>(f.apply(value));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <B> Either<L, B> flatMap(Function<R, Either<L, B>> f) {
        return (Either<L, B>) this;
    }

    @Override
    public <B> Either<B, R> flatMapLeft(Function<L, Either<B, R>> f) {
        return f.apply(value);
    }

    @Override
    public <B> Either<L, B> recover(Function<L, B> f) {
        return Right.of(f.apply(value));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <B> Either<L, B> flatten() {
        return (Either<L, B>) this;
    }

    @Override
    public Either<L, R> forEach(Consumer<R> f) {
        return this;
    }

    @Override
    public Either<L, R> forEachLeft(Consumer<L> f) {
        f.accept(value);
        return this;
    }

    @Override
    public R orElse(R value) {
        return value;
    }

    @Override
    public L left() {
        return value;
    }

    @Override
    public R right() {
        throw new NoSuchElementException("No value present");
    }

    @Override
    public boolean isLeft() {
        return true;
    }

    @Override
    public boolean isRight() {
        return false;
    }

    @Override
    public Either<R, L> swap() {
        return Right.of(value);
    }

    @Override
    public R get() {
        throw new NoSuchElementException();
    }

    @Override
    public <B> B fold(Function<L, B> fnLeft, Function<R, B> fnRight) {
        return fnLeft.apply(value);
    }

    @Override
    public String toString() {
        return "Left(" + value + ")";
    }

    @Override
    public boolean equals(Object value) {
        if (value == this) {
            return true;
        }
        if (value instanceof Left) {
            @SuppressWarnings("unchecked")
            Left<L, R> valueLeft = (Left<L, R>) value;
            return this.value.equals(valueLeft.left());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
