package fp.util;

import java.util.stream.Collectors;

public class HItem<T, R extends HList> implements HList {
    private T value1;
    private R value2;

    private HItem(T value1, R value2) {
        this.value1 = value1;
        this.value2 = value2;
    }

    public static <T> HItem<T, HNil> of(T value) {
        return new HItem<T, HNil>(value, new HNil());
    }

    public static <T, R extends HList> HItem<T, R> of(T value1, R value2) {
        return new HItem<T, R>(value1, value2);
    }

    @Override
    public <S> HItem<S, HItem<T, R>> add(S value) {
        return new HItem<S, HItem<T, R>>(value, this);
    }

    /**
     * @return the value
     */
    public T get() {
        return value1;
    }

    /**
     * @return the value
     */
    public R tail() {
        return value2;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public HList reverse() {
        HList hlist = this;
        HList forward = new HNil();
        while (!hlist.isEmpty()) {
            forward = forward.add(hlist.get());
            hlist = hlist.tail();
        }
        return forward;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof HItem)) {
            return false;
        }

        final HItem<?, ?> other = (HItem<?, ?>) obj;

        return other.get().equals(value1) && other.tail().equals(value2);
    }

    @Override
    public String toString() {
        final String values = valueStream()
            .map(Object::toString)
            .collect(Collectors.joining(", "));
        return "HList(" + values + ")";
    }
}
