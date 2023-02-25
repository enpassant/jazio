package fp.util;

import java.util.stream.Collectors;
import java.util.Optional;

public class HMap {
    private final HList hlist;

    private <K, V> HMap(HList hlist) {
        this.hlist = hlist;
    }

    public static <K, V> HMap of(final K key, final V value) {
        final HList hlist = HList.of(Tuple2.of(key, value));
        return new HMap(hlist);
    }

    public <K, V> HMap add(final K key, final V value) {
        final HList newHlist = hlist.add(Tuple2.of(key, value));
        return new HMap(newHlist);
    }

    public int size() {
        return hlist.size();
    }

    public <K, V> Optional<V> getValue(K key) {
        return hlist.valueStream()
            .map(Tuple2.class::cast)
            .filter(tuple -> tuple._1().equals(key))
            .map(tuple -> (V) tuple._2())
            .findAny();
    }

    public <K> HList getValues(K key) {
        return hlist.valueStream()
            .map(Tuple2.class::cast)
            .filter(tuple -> tuple._1().equals(key))
            .map(tuple -> tuple._2())
            .reduce((HList) new HNil(), HList::add, (h1, h2) -> h1.addAll(h2));
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof HMap)) {
            return false;
        }

        final HMap other = (HMap) obj;

        return other.hlist.equals(hlist);
    }

    @Override
    public String toString() {
        final String values = hlist.valueStream()
            .map(Tuple2.class::cast)
            .map(tuple -> tuple._1() + " -> " + tuple._2())
            .collect(Collectors.joining(", "));
        return "HMap(" + values + ")";
    }
}
