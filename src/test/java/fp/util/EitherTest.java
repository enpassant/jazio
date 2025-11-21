package fp.util;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class EitherTest {
    private static final String DIVISION_BY_ZERO = "Division by zero";

    @Test
    void testCreationRight() {
        Either<String, Integer> either = divideHundredBy(50);
        Assertions.assertEquals((Integer) 2, either.get());
    }

    @Test
    void testCreationLeft() {
        Either<String, Integer> either = divideHundredBy(0);
        Assertions.assertEquals(DIVISION_BY_ZERO, either.left());
    }

    @Test
    void testFromRight() {
        Either<String, Integer> either = Either.ofOptional(
                DIVISION_BY_ZERO,
                Optional.of(50)
        );
        Assertions.assertEquals((Integer) 50, either.get());
    }

    @Test
    void testFromLeft() {
        Either<String, Integer> either = Either.ofOptional(
                DIVISION_BY_ZERO,
                Optional.empty()
        );
        Assertions.assertEquals(DIVISION_BY_ZERO, either.left());
    }

    @Test
    void testMap() {
        Either<String, String> either = divideHundredBy(4)
                .map(i -> i + 3)
                .map(Integer::toHexString);
        Assertions.assertEquals("1c", either.get());
    }

    @Test
    void testMapError() {
        Either<String, String> either = divideHundredBy(0)
                .map(i -> i + 3)
                .map(Integer::toHexString);
        Assertions.assertEquals(DIVISION_BY_ZERO, either.left());
    }

    @Test
    void testMapLeft() {
        Either<Integer, Integer> either = divideHundredBy(0)
                .mapLeft(String::length);
        Assertions.assertEquals(Left.of(16), either);
    }

    @Test
    void testOrElse() {
        Integer number = divideHundredBy(0)
                .orElse(12);
        Assertions.assertEquals((Integer) 12, number);
    }

    @Test
    void testFlatMap() {
        Either<String, Integer> either = divideHundredBy(4)
                .flatMap(EitherTest::divideHundredBy);
        Assertions.assertEquals((Integer) 4, either.get());
    }

    @Test
    void testFlatMapError() {
        Either<String, Integer> either = divideHundredBy(200)
                .flatMap(EitherTest::divideHundredBy);
        Assertions.assertEquals(DIVISION_BY_ZERO, either.left());
    }

    @Test
    void testFlatMapLeft() {
        Either<String, Integer> either = divideHundredBy(0)
                .mapLeft(String::length)
                .flatMapLeft(EitherTest::divideHundredBy);
        Assertions.assertEquals(Right.of(6), either);
    }

    @Test
    void testFoldLeft() {
        Integer result = divideHundredBy(0)
                .fold(String::length, Integer::intValue);
        Assertions.assertEquals((Integer) 16, result);
    }

    @Test
    void testFoldRight() {
        Integer result = divideHundredBy(10)
                .fold(String::length, Integer::intValue);
        Assertions.assertEquals((Integer) 10, result);
    }

    private static Either<String, Integer> divideHundredBy(int divisor) {
        return divisor == 0 ?
                Left.of(DIVISION_BY_ZERO) :
                Right.of(100 / divisor);
    }
}
