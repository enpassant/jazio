package fp.util;

import java.util.Optional;
import java.util.stream.Collectors;

public class HItem<T, R extends HList> implements HList {
    private final T value1;
    private final R value2;
    private final int size;

    private HItem(
            final T value1,
            final R value2,
            final int size
    ) {
        this.value1 = value1;
        this.value2 = value2;
        this.size = size;
    }

    public static <T> HItem<T, HNil> of(final T value) {
        return new HItem<>(value, new HNil(), 1);
    }

    public static <T, R extends HList> HItem<T, R> of(
            final T value1,
            final R value2
    ) {
        return new HItem<>(value1, value2, value2.size() + 1);
    }

    @Override
    public <S> HItem<S, HItem<T, R>> add(final S value) {
        return new HItem<>(value, this, size + 1);
    }

    @Override
    public HList addAll(final HList hlist) {
        return hlist.valueStream()
                .reduce(
                        this,
                        HList::add,
                        HList::addAll
                );
    }

    @Override
    public T head() {
        return value1;
    }

    @Override
    public Optional<T> headOptional() {
        return Optional.of(value1);
    }

    @Override
    public R tail() {
        return value2;
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
    public HList reverse() {
        HList hlist = this;
        HList forward = new HNil();
        while (!hlist.isEmpty()) {
            forward = forward.add(hlist.head());
            hlist = hlist.tail();
        }
        return forward;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof final HItem<?, ?> other)) {
            return false;
        }

        return other.head().equals(value1) && other.tail().equals(value2);
    }

    @Override
    public String toString() {
        final String values = valueStream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        return "HList(" + values + ")";
    }
}
