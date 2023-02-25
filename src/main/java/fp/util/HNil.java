package fp.util;

import java.util.NoSuchElementException;
import java.util.Optional;

public class HNil implements HList {
    @Override
    public <R> HItem<R, HNil> add(final R value) {
        return HList.of(value);
    }

    @Override
    public HList addAll(final HList hlist) {
        return hlist;
    }

    @Override
    public <T> T head() {
        throw new NoSuchElementException();
    }

    @Override
    public <T> Optional<T> headOptional() {
        return Optional.empty();
    }

    @Override
    public HList tail() {
        return this;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public HNil reverse() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof HNil);
    }

    @Override
    public String toString() {
        return "HNil";
    }
}
