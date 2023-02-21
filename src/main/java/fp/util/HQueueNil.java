package fp.util;

import java.util.NoSuchElementException;
import java.util.Optional;

public class HQueueNil implements HQueue {
    @Override
    public <T> HQueue add(final T value) {
        return HQueueNotEmpty.of(value);
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
    public HQueue tail() {
        return this;
    }

    @Override
    public <T> Optional<T> get(final int index) {
        return Optional.empty();
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
    public boolean equals(final Object obj) {
        return (obj instanceof HQueueNil);
    }

    @Override
    public String toString() {
        return "HQueueNil";
    }
}
