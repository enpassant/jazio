package fp.util;

import java.util.Optional;
import java.util.stream.Collectors;

public class HMap {
    private final HList hlist;

    private HMap(HList hlist) {
        this.hlist = hlist;
    }

    public static HMap empty() {
        return new HMap(HList.empty());
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
                .map(Tuple2::_2)
                .reduce(new HNil(), HList::add, HList::addAll);
    }

    public HList allValues() {
        return hlist;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof final HMap other)) {
            return false;
        }

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
