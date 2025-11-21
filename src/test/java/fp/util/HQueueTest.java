package fp.util;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HQueueTest {

    @Test
    void testHQueueNilEquals() {
        Assertions.assertEquals(new HQueueNil(), HQueue.empty());
    }

    @Test
    void testAddRemoveIsEmpty() {
        HQueue hlist = HQueue.of(13).tail();
        Assertions.assertEquals(hlist, HQueue.empty());
    }

    @Test
    void testFloatItem() {
        HQueue hlist = HQueue.of(13)
                .add("String")
                .add(3.45f)
                .add(Optional.empty())
                .add(15L);
        float result = hlist.tail().tail().head();
        Assertions.assertEquals(3.45f, result, 0.0001);
    }

    @Test
    void testSize() {
        HQueue hlist = HQueue.of(13)
                .add("String")
                .add(3.45f)
                .add(Optional.empty())
                .add(15L);
        Assertions.assertEquals(5, hlist.size());
        Assertions.assertEquals(3, hlist.tail().tail().size());
        Assertions.assertEquals(1, hlist.tail().tail().tail().tail().size());
        Assertions.assertEquals(0, hlist.tail().tail().tail().tail().tail().size());
    }

    @Test
    void testGet() {
        HQueue hlist = HQueue.of(13)
                .add("String")
                .add(3.45f)
                .add(Optional.empty())
                .add(15L);
        Assertions.assertEquals(Optional.empty(), hlist.get(-1));
        Assertions.assertEquals(13, hlist.get(0).get());
        Assertions.assertEquals("String", hlist.get(1).get());
        Assertions.assertEquals(3.45f, hlist.get(2).get());
        Assertions.assertEquals(Optional.empty(), hlist.get(3).get());
        Assertions.assertEquals(15L, hlist.get(4).get());
        Assertions.assertEquals(Optional.empty(), hlist.get(5));
    }
}
