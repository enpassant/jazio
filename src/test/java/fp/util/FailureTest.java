package fp.util;

import java.text.MessageFormat;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class FailureTest {

    private static final String ERROR_001 = "ERROR_001";

    @Test
    void testCreationWithOnlyCode() {
        GeneralFailure<String> failure = GeneralFailure.of(ERROR_001);
        Assertions.assertEquals(ERROR_001, failure.getCode());
    }

    @Test
    void testCreationWithOneParam() {
        String keyOrderId = "OrderId";
        Integer orderId = 124;

        GeneralFailure<String> failure = GeneralFailure.of(ERROR_001, keyOrderId, orderId);
        Assertions.assertEquals(ERROR_001, failure.getCode());
        Assertions.assertEquals(1, failure.getParamNames().size());
        Assertions.assertTrue(failure.getParamNames().contains(keyOrderId));
        Assertions.assertEquals(orderId, failure.getParamValue(keyOrderId));
    }

    @Test
    void testCreationWithThreeParams() {
        String keyOrderId = "OrderId";
        Integer orderId = 124;
        String keyName = "Name";
        String name = "Teszt Elek";
        String keyPrice = "Price";
        Double price = 124.5;

        Set<String> keys = new HashSet<>();
        keys.add(keyOrderId);
        keys.add(keyName);
        keys.add(keyPrice);

        GeneralFailure<String> failure = GeneralFailure.of(ERROR_001,
                Tuple2.of(keyOrderId, orderId),
                Tuple2.of(keyName, name),
                Tuple2.of(keyPrice, price)
        );

        Assertions.assertEquals(ERROR_001, failure.getCode());
        Assertions.assertEquals(keys, failure.getParamNames());
        Assertions.assertEquals(orderId, failure.getParamValue(keyOrderId));
        Assertions.assertEquals(name, failure.getParamValue(keyName));
        Assertions.assertEquals(price, failure.getParamValue(keyPrice));
    }

    @Test
    void testToString() {
        String keyOrderId = "OrderId";
        Integer orderId = 124;
        String keyName = "Name";
        String name = "Teszt Elek";
        String keyPrice = "Price";
        Double price = 124.5;

        GeneralFailure<String> failure = GeneralFailure.of(ERROR_001,
                Tuple2.of(keyOrderId, orderId),
                Tuple2.of(keyName, name),
                Tuple2.of(keyPrice, price)
        );

        String expectedStr = MessageFormat.format(
                "GeneralFailure({0}, {1} -> {2}, {3} -> {4}, {5} -> {6})",
                ERROR_001,
                keyOrderId,
                orderId,
                keyName,
                name,
                keyPrice,
                price.toString()
        );

        Assertions.assertEquals(expectedStr, failure.toString());
    }

    @Test
    void testFormat() {
        String pattern = "Order({0}, customer: {1}, price: {2})";

        String keyOrderId = "OrderId";
        Integer orderId = 124;
        String keyName = "Name";
        String name = "Teszt Elek";
        String keyPrice = "Price";
        Double price = 124.5;

        GeneralFailure<String> failure = GeneralFailure.of(ERROR_001,
                Tuple2.of(keyOrderId, orderId),
                Tuple2.of(keyName, name),
                Tuple2.of(keyPrice, price)
        );

        String expectedStr = MessageFormat.format(
                pattern,
                orderId,
                name,
                price
        );

        Assertions.assertEquals(expectedStr, failure.format(pattern));
    }

    @Test
    void testTryCatchSuccess() {
        Either<Failure, Integer> valueResult = ExceptionFailure.tryCatch(() ->
                100 / 4
        );

        Assertions.assertEquals(Right.of(25), valueResult);
    }

    @Test
    void testTryCatchFailed() {
        Either<Failure, Integer> valueResult = ExceptionFailure.tryCatch(() ->
                100 / 0
        );

        Assertions.assertTrue(valueResult.isLeft());

        ExceptionFailure failure = (ExceptionFailure) valueResult.left();
        Assertions.assertEquals("ArithmeticException", failure.throwable.getClass().getSimpleName());
        Assertions.assertTrue(failure.throwable instanceof ArithmeticException);
    }
}
