package fp.util;

import java.util.function.Function;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

public class HListTest {

    @Test
    public void testHNilEquals() {
        Assert.assertEquals(new HNil(), HList.empty());
    }

    @Test
    public void testEquals() {
        HList hlist = HList.of(13).add("String").add(3.45).add(Optional.empty()).add(15L);
        HItem<Long, HItem<Optional<Object>, HItem<Double, HItem<String, HItem<Integer, HNil>>>>> hlist2 =
            HList.of(13).add("String").add(3.45).add(Optional.empty()).add(15L);
        Assert.assertEquals(hlist, hlist2);
    }

    @Test
    public void testNotEquals() {
        HList hlist = HList.of(13).add("String").add(3.45).add(Optional.empty()).add(15L);
        HItem<Long, HItem<Optional<Object>, HItem<Float, HItem<String, HItem<Integer, HNil>>>>> hlist2 =
            HList.of(13).add("String").add(3.45f).add(Optional.empty()).add(15L);
        Assert.assertNotEquals(hlist, hlist2);
    }

    @Test
    public void testFloatItem() {
        HItem<Long, HItem<Optional<Object>, HItem<Float, HItem<String, HItem<Integer, HNil>>>>> hlist =
            HList.of(13).add("String").add(3.45f).add(Optional.empty()).add(15L);
        Assert.assertEquals(hlist.tail().tail().head(), (Float) 3.45f);
    }

    @Test
    public void testBigList() {
        final HList hlist = IntStream.range(0, 100_000)
            .mapToObj(i -> i)
            .reduce(
                new HNil(),
                (HList hl, Integer i) -> hl.add(i),
                (h1, h2) -> h1.addAll(h2)
            );
        final Integer head = hlist.reverse().head();
        Assert.assertEquals(head, (Integer) 0);
    }
}
