package fp.util;

import java.util.Optional;
import java.util.stream.Collectors;

public class HQueueNotEmpty implements HQueue {
    private final HList forward;
    private final HList backward;
    private final int size;

    private HQueueNotEmpty(
        final HList forward,
        final HList backward,
        final int size
    ) {
        this.forward = forward;
        this.backward = backward;
        this.size = size;
    }

    public static <T> HQueue of(final T value) {
        return new HQueueNotEmpty(HList.of(value), new HNil(), 1);
    }

    @Override
    public <S> HQueueNotEmpty add(final S value) {
        return new HQueueNotEmpty(
            forward,
            backward.add(value),
            size + 1
        );
    }

    @Override
    public <T> T head() {
        return (T) forward.get();
    }

    @Override
    public <T> Optional<T> headOptional() {
        return Optional.of((T) forward.get());
    }

    @Override
    public HQueue tail() {
        final HList tail = forward.tail();
        if (tail.isEmpty()) {
            if (backward.isEmpty()) {
                return HQueue.empty();
            } else {
                return new HQueueNotEmpty(
                    backward.reverse(),
                    new HNil(),
                    size - 1
                );
            }
        } else {
            return new HQueueNotEmpty(tail, backward, size - 1);
        }
    }

    @Override
    public <T> Optional<T> get(final int index) {
        if (index == 0) {
            return headOptional();
        } else if (index > 0 && index < size) {
            return tail().get(index - 1);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof HQueueNotEmpty)) {
            return false;
        }

        final HQueueNotEmpty other = (HQueueNotEmpty) obj;

        return other.head().equals(this.head()) && other.tail().equals(tail());
    }

    @Override
    public String toString() {
        final String values = valueStream()
            .map(Object::toString)
            .collect(Collectors.joining(", "));
        return "HQueueNotEmpty(" + values + ")";
    }
}
