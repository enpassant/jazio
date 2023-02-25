package fp.util;

import java.util.Optional;
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

    HList addAll(HList hlist);

    <T> T head();

    <T> Optional<T> headOptional();

    HList tail();

    boolean isEmpty();

    int size();

    HList reverse();

    default Stream<Object> valueStream() {
        return Stream.iterate(this, HList::tail)
            .limit(size())
            .map(HList::head);
    }
}
