package fp.util;

import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;

public class HQueueTest {

    @Test
    public void testHQueueNilEquals() {
        Assert.assertEquals(new HQueueNil(), HQueue.empty());
    }

    @Test
    public void testAddRemoveIsEmpty() {
        HQueue hlist = HQueue.of(13).tail();
        Assert.assertEquals(hlist, HQueue.empty());
    }

    @Test
    public void testFloatItem() {
        HQueue hlist = HQueue.of(13)
                .add("String")
                .add(3.45f)
                .add(Optional.empty())
                .add(15L);
        float result = hlist.tail().tail().head();
        Assert.assertEquals(3.45f, result, 0.0001);
    }

    @Test
    public void testSize() {
        HQueue hlist = HQueue.of(13)
                .add("String")
                .add(3.45f)
                .add(Optional.empty())
                .add(15L);
        Assert.assertEquals(5, hlist.size());
        Assert.assertEquals(3, hlist.tail().tail().size());
        Assert.assertEquals(1, hlist.tail().tail().tail().tail().size());
        Assert.assertEquals(0, hlist.tail().tail().tail().tail().tail().size());
    }

    @Test
    public void testGet() {
        HQueue hlist = HQueue.of(13)
                .add("String")
                .add(3.45f)
                .add(Optional.empty())
                .add(15L);
        Assert.assertEquals(Optional.empty(), hlist.get(-1));
        Assert.assertEquals(13, hlist.get(0).get());
        Assert.assertEquals("String", hlist.get(1).get());
        Assert.assertEquals(3.45f, hlist.get(2).get());
        Assert.assertEquals(Optional.empty(), hlist.get(3).get());
        Assert.assertEquals(15L, hlist.get(4).get());
        Assert.assertEquals(Optional.empty(), hlist.get(5));
    }
}
