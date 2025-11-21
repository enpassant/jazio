package fp.util;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class Tuple2Test {

    @Test
    void testCreation() {
        Tuple2<String, Double> tuple = Tuple2.of("2", 2.0);

        Assertions.assertEquals("2", tuple.first());
        Assertions.assertEquals((Object) 2.0, tuple.second());
    }

    @Test
    void testEqualMaps() {
        Map<String, Object> mapExpected = new LinkedHashMap<>();
        mapExpected.put("1", 1);
        mapExpected.put("2", 2.0);
        mapExpected.put("3", 3);
        mapExpected.put("4", 4.0);
        mapExpected.put("5", 5);

        Map<String, Object> map = Tuple2.toMap(
                Tuple2.of("1", 1),
                Tuple2.of("2", 2.0),
                Tuple2.of("3", 3),
                Tuple2.of("4", 4.0),
                Tuple2.of("5", 5)
        );
        Assertions.assertEquals(mapExpected, map);
    }
}
