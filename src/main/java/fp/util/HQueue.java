package fp.util;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.Stream.Builder;

public interface HQueue {
    public static <T> HQueue of(final T value) {
        return HQueueNotEmpty.<T>of(value);
    }

    public static HQueueNil empty() {
        return new HQueueNil();
    }

    <R> HQueue add(final R value);

    <T> T head();

    <T> Optional<T> headOptional();

    HQueue tail();

    <T> Optional<T> get(final int index);

    boolean isEmpty();

    int size();

    default Stream<Object> valueStream() {
        return Stream.iterate(this, hq -> !hq.isEmpty(), HQueue::tail)
            .map(HQueue::head);
    }
}
