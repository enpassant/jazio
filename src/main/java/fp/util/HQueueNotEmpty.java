package fp.util;

import java.util.Optional;
import java.util.stream.Collectors;

public class HQueueNotEmpty implements HQueue {
    private final HList forward;
    private final HList backward;

    private HQueueNotEmpty(
            final HList forward,
            final HList backward
    ) {
        this.forward = forward;
        this.backward = backward;
    }

    public static <T> HQueue of(final T value) {
        return new HQueueNotEmpty(HList.of(value), new HNil());
    }

    @Override
    public <S> HQueueNotEmpty add(final S value) {
        return new HQueueNotEmpty(
                forward,
                backward.add(value)
        );
    }

    @Override
    public <T> T head() {
        return forward.head();
    }

    @Override
    public <T> Optional<T> headOptional() {
        return Optional.of(forward.head());
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
                        new HNil()
                );
            }
        } else {
            return new HQueueNotEmpty(tail, backward);
        }
    }

    @Override
    public <T> Optional<T> get(final int index) {
        if (index == 0) {
            return headOptional();
        } else if (index > 0 && index < size()) {
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
        return forward.size() + backward.size();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof final HQueueNotEmpty other)) {
            return false;
        }

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
