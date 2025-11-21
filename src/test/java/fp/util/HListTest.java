package fp.util;

import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HListTest {

    @Test
    void testHNilEquals() {
        Assertions.assertEquals(new HNil(), HList.empty());
    }

    @Test
    void testEquals() {
        final HList hlist = HList.of(13)
                .add("String")
                .add(3.45)
                .add(Optional.empty())
                .add(15L);
        final HItem<Long, HItem<Optional<Object>, HItem<Double, HItem<String, HItem<Integer, HNil>>>>> hlist2 =
                HList.of(13).add("String").add(3.45).add(Optional.empty()).add(15L);
        Assertions.assertEquals(hlist, hlist2);
    }

    @Test
    void testNotEquals() {
        final HList hlist = HList.of(13)
                .add("String")
                .add(3.45)
                .add(Optional.empty())
                .add(15L);
        final HItem<Long, HItem<Optional<Object>, HItem<Float, HItem<String, HItem<Integer, HNil>>>>> hlist2 =
                HList.of(13).add("String").add(3.45f).add(Optional.empty()).add(15L);
        Assertions.assertNotEquals(hlist, hlist2);
    }

    @Test
    void testFloatItem() {
        final HItem<Long, HItem<Optional<Object>, HItem<Float, HItem<String, HItem<Integer, HNil>>>>> hlist =
                HList.of(13).add("String").add(3.45f).add(Optional.empty()).add(15L);
        Assertions.assertEquals((Float) 3.45f, hlist.tail().tail().head());
    }

    @Test
    void testAddAll() {
        final HList hlist1 = HList.of(13).add("String").add(3.45);
        final HList hlist2 = HList.of(15L).add(Optional.empty());

        final HList hlist =
                HList.of(13).add("String").add(3.45).add(Optional.empty()).add(15L);
        Assertions.assertEquals(hlist, hlist1.addAll(hlist2));
    }

    @Test
    void testBigList() {
        final HList hlist = IntStream.range(0, 100_000)
                .boxed()
                .reduce(
                        new HNil(),
                        (HList hl, Integer i) -> hl.add(i),
                        HList::addAll
                );
        final Integer head = hlist.reverse().head();
        Assertions.assertEquals((Integer) 0, head);
    }
}
