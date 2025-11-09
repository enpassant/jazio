package fp.util;

import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;

public class HMapTest {

    @Test
    public void testToString() {
        final HMap hmap = HMap.of("int", 13)
                .add("str", "String")
                .add("double", 3.45)
                .add("optional", Optional.empty())
                .add("long", 15L);

        Assert.assertEquals(
                "HMap(long -> 15, optional -> Optional.empty, double -> 3.45, str -> String, int -> 13)",
                hmap.toString()
        );
    }

    @Test
    public void testSize() {
        final HMap hmap = HMap.of("int", 13)
                .add("str", "String")
                .add("double", 3.45)
                .add("optional", Optional.empty())
                .add("long", 15L);

        Assert.assertEquals(5, hmap.size());
    }

    @Test
    public void testGetValue() {
        final HMap hmap = HMap.of("int", 13)
                .add("str", "String")
                .add("double", 3.45)
                .add("optional", Optional.empty())
                .add("long", 15L);

        final Optional<Double> value = hmap.getValue("double");

        Assert.assertEquals(value, Optional.of(3.45));
    }

    @Test
    public void testGetValueMissing() {
        final HMap hmap = HMap.of("int", 13)
                .add("str", "String")
                .add("double", 3.45)
                .add("optional", Optional.empty())
                .add("long", 15L);

        final Optional<Double> value = hmap.getValue("key");

        Assert.assertEquals(value, Optional.empty());
    }

    @Test
    public void testGetValues() {
        final HMap hmap = HMap.of("number", 13)
                .add("str", "String")
                .add("number", 3.45)
                .add("optional", Optional.empty())
                .add("number", 15L);

        final HList hlist = hmap.getValues("number");

        Assert.assertEquals(hlist, HList.of(15L).add(3.45).add(13));
    }

    @Test
    public void testGetValuesMissing() {
        final HMap hmap = HMap.of("number", 13)
                .add("str", "String")
                .add("number", 3.45)
                .add("optional", Optional.empty())
                .add("number", 15L);

        final HList hlist = hmap.getValues("key");

        Assert.assertEquals(new HNil(), hlist);
    }
}
