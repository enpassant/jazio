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
        Assert.assertEquals(result, 3.45f, 0.0001);
    }

    @Test
    public void testSize() {
        HQueue hlist = HQueue.of(13)
            .add("String")
            .add(3.45f)
            .add(Optional.empty())
            .add(15L);
        Assert.assertEquals(hlist.size(), 5);
        Assert.assertEquals(hlist.tail().tail().size(), 3);
        Assert.assertEquals(hlist.tail().tail().tail().tail().size(), 1);
        Assert.assertEquals(hlist.tail().tail().tail().tail().tail().size(), 0);
    }

    @Test
    public void testGet() {
        HQueue hlist = HQueue.of(13)
            .add("String")
            .add(3.45f)
            .add(Optional.empty())
            .add(15L);
        Assert.assertEquals(hlist.get(-1), Optional.empty());
        Assert.assertEquals(hlist.get(0).get(), 13);
        Assert.assertEquals(hlist.get(1).get(), "String");
        Assert.assertEquals(hlist.get(2).get(), 3.45f);
        Assert.assertEquals(hlist.get(3).get(), Optional.empty());
        Assert.assertEquals(hlist.get(4).get(), 15L);
        Assert.assertEquals(hlist.get(5), Optional.empty());
    }
}
