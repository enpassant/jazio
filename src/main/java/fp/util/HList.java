package fp.util;

import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

public interface HList {
    public static <T> HItem<T, HNil> of(T value) {
        return HItem.of(value);
    }

    public static HNil empty() {
        return new HNil();
    }

    <R> HList add(R value);

    Object get();

    HList tail();

    boolean isEmpty();

    default Stream<Object> valueStream() {
        HList hlist = this;
        Builder<Object> builder = Stream.builder();
        while (!hlist.isEmpty()) {
            builder.accept(hlist.get());
            hlist = hlist.tail();
        }
        return builder.build();
    }
}
