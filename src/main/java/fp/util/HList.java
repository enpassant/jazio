package fp.util;

import java.util.Optional;
import java.util.stream.Stream;

public interface HList {
    static <T> HItem<T, HNil> of(T value) {
        return HItem.of(value);
    }

    static HNil empty() {
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

    default <T> Stream<T> valueStream() {
        return Stream.iterate(this, HList::tail)
                .limit(size())
                .map(HList::head);
    }
}
