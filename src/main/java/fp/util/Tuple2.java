package fp.util;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class Tuple2<A, B> {
    private final A first;
    private final B second;

    public Tuple2(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public static <A, B> Tuple2<A, B> of(A first, B second) {
        return new Tuple2<>(first, second);
    }

    @SafeVarargs
    public static <A, B> Map<A, B> toMap(Tuple2<A, B>... tuples) {
        return Arrays.stream(tuples)
            .collect(Collectors.toMap(
                Tuple2::getFirst,
                Tuple2::getSecond,
                (u, v) -> v,
                LinkedHashMap::new
            ));
    }

    public A getFirst() {
        return first;
    }

    public B getSecond() {
        return second;
    }

    @Override
    public String toString() {
        return "Tuple2(" + first.toString() + "," + second.toString() + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Tuple2) {
            @SuppressWarnings("unchecked")
            Tuple2<A, B> tuple = (Tuple2<A, B>) other;
            return Objects.equals(first, tuple.first)
                && Objects.equals(second, tuple.second);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return first.hashCode() * 11 + second.hashCode();
    }
}
