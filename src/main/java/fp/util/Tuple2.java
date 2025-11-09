package fp.util;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public record Tuple2<A, B>(A first, B second) {

    public static <A, B> Tuple2<A, B> of(A first, B second) {
        return new Tuple2<>(first, second);
    }

    @SafeVarargs
    public static <A, B> Map<A, B> toMap(Tuple2<A, B>... tuples) {
        return Arrays.stream(tuples)
                .collect(Collectors.toMap(
                        Tuple2::first,
                        Tuple2::second,
                        (u, v) -> v,
                        LinkedHashMap::new
                ));
    }

    public A _1() {
        return first;
    }

    public B _2() {
        return second;
    }

}
