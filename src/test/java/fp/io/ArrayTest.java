package fp.io;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import fp.util.Failure;
import fp.util.Right;

/**
        Based on: https://wwconfirmedw.codejava.net/java-core/concurrency/understanding-java-fork-join-framework-with-examples
*/
public class ArrayTest {
    final static DefaultPlatform platform = new DefaultPlatform();
    final Runtime<Object> runtime = new DefaultRuntime<Object>(null, platform);

    @AfterClass
    public static void setUp() {
        platform.shutdown();
    }

    public static void main(String[] args) {
    }

    @Test
    public void testCounter() {
        final int SIZE = 10_000_000;
        final int[] array = incrementArray(SIZE);
        final int threshold = 100_000;

        final IO<Object, Void, Integer> io =
            compute(array, 0, SIZE, threshold);

        Assert.assertEquals(
            Right.of(SIZE / 2),
            runtime.unsafeRun(io)
        );
    }

    private static IO<Object, Void, Integer> compute(
        final int[] array,
        final int start,
        final int end,
        final int threshold
    ) {
        if (end - start <= threshold) {
            return IO.effectTotal(() ->
                computeDirectly(array, start, end)
            );
        } else {
            final int middle = (end + start) / 2;

            return compute(array, start, middle, threshold).zipParWith(
                compute(array, middle, end, threshold),
                (a, b) -> a + b
            );
        }
    }

    private static Integer computeDirectly(
        final int[] array,
        final int start,
        final int end
    ) {
        Integer count = 0;

        for (int i = start; i < end; i++) {
            if (array[i] % 2 == 0) {
                count++;
            }
        }

        return count;
    }

    private static int[] incrementArray(final int size) {
        final int[] array = new int[size];

        for (int i = 0; i < size; i++) {
            array[i] = i;
        }

        return array;
    }
}
