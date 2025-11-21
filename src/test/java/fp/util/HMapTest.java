package fp.util;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HMapTest {

    @Test
    void testToString() {
        final HMap hmap = HMap.of("int", 13)
                .add("str", "String")
                .add("double", 3.45)
                .add("optional", Optional.empty())
                .add("long", 15L);

        Assertions.assertEquals(
                "HMap(long -> 15, optional -> Optional.empty, double -> 3.45, str -> String, int -> 13)",
                hmap.toString()
        );
    }

    @Test
    void testSize() {
        final HMap hmap = HMap.of("int", 13)
                .add("str", "String")
                .add("double", 3.45)
                .add("optional", Optional.empty())
                .add("long", 15L);

        Assertions.assertEquals(5, hmap.size());
    }

    @Test
    void testGetValue() {
        final HMap hmap = HMap.of("int", 13)
                .add("str", "String")
                .add("double", 3.45)
                .add("optional", Optional.empty())
                .add("long", 15L);

        final Optional<Double> value = hmap.getValue("double");

        Assertions.assertEquals(value, Optional.of(3.45));
    }

    @Test
    void testGetValueMissing() {
        final HMap hmap = HMap.of("int", 13)
                .add("str", "String")
                .add("double", 3.45)
                .add("optional", Optional.empty())
                .add("long", 15L);

        final Optional<Double> value = hmap.getValue("key");

        Assertions.assertEquals(value, Optional.empty());
    }

    @Test
    void testGetValues() {
        final HMap hmap = HMap.of("number", 13)
                .add("str", "String")
                .add("number", 3.45)
                .add("optional", Optional.empty())
                .add("number", 15L);

        final HList hlist = hmap.getValues("number");

        Assertions.assertEquals(hlist, HList.of(15L).add(3.45).add(13));
    }

    @Test
    void testGetValuesMissing() {
        final HMap hmap = HMap.of("number", 13)
                .add("str", "String")
                .add("number", 3.45)
                .add("optional", Optional.empty())
                .add("number", 15L);

        final HList hlist = hmap.getValues("key");

        Assertions.assertEquals(new HNil(), hlist);
    }
}
