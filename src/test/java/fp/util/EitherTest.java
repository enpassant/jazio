package fp.util;

import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;

public class EitherTest {
    private static final String DIVISION_BY_ZERO = "Division by zero";

    @Test
    public void testCreationRight() {
        Either<String, Integer> either = divideHundredBy(50);
        Assert.assertEquals((Integer) 2, either.get());
    }

    @Test
    public void testCreationLeft() {
        Either<String, Integer> either = divideHundredBy(0);
        Assert.assertEquals(DIVISION_BY_ZERO, either.left());
    }

    @Test
    public void testFromRight() {
        Either<String, Integer> either = Either.ofOptional(
            DIVISION_BY_ZERO,
            Optional.of(50)
        );
        Assert.assertEquals((Integer) 50, either.get());
    }

    @Test
    public void testFromLeft() {
        Either<String, Integer> either = Either.ofOptional(
            DIVISION_BY_ZERO,
            Optional.empty()
        );
        Assert.assertEquals(DIVISION_BY_ZERO, either.left());
    }

    @Test
    public void testMap() {
        Either<String, String> either = divideHundredBy(4)
            .map(i -> i + 3)
            .map(Integer::toHexString);
        Assert.assertEquals("1c", either.get());
    }

    @Test
    public void testMapError() {
        Either<String, String> either = divideHundredBy(0)
            .map(i -> i + 3)
            .map(Integer::toHexString);
        Assert.assertEquals(DIVISION_BY_ZERO, either.left());
    }

    @Test
    public void testMapLeft() {
        Either<Integer, Integer> either = divideHundredBy(0)
            .mapLeft(String::length);
        Assert.assertEquals(Left.of(16), either);
    }

    @Test
    public void testOrElse() {
        Integer number = divideHundredBy(0)
            .orElse(12);
        Assert.assertEquals((Integer) 12, number);
    }

    @Test
    public void testFlatMap() {
        Either<String, Integer> either = divideHundredBy(4)
            .flatMap(EitherTest::divideHundredBy);
        Assert.assertEquals((Integer) 4, either.get());
    }

    @Test
    public void testFlatMapError() {
        Either<String, Integer> either = divideHundredBy(200)
            .flatMap(EitherTest::divideHundredBy);
        Assert.assertEquals(DIVISION_BY_ZERO, either.left());
    }

    @Test
    public void testFlatMapLeft() {
        Either<String, Integer> either = divideHundredBy(0)
            .mapLeft(String::length)
            .flatMapLeft(EitherTest::divideHundredBy);
        Assert.assertEquals(Right.of(6), either);
    }

    @Test
    public void testFoldLeft() {
        Integer result = divideHundredBy(0)
            .fold(String::length, Integer::intValue);
        Assert.assertEquals((Integer) 16, result);
    }

    @Test
    public void testFoldRight() {
        Integer result = divideHundredBy(10)
            .fold(String::length, Integer::intValue);
        Assert.assertEquals((Integer) 10, result);
    }

    private static Either<String, Integer> divideHundredBy(int divisor) {
        return divisor == 0 ?
            Left.of(DIVISION_BY_ZERO) :
            Right.of(100 / divisor);
    }
}
