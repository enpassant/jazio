package fp.io;

import fp.util.Either;
import fp.util.Failure;
import fp.util.GeneralFailure;
import fp.util.Left;
import fp.util.Right;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IORaceTest {
    final static DefaultPlatform platform = new DefaultPlatform();

    final Runtime defaultRuntime = new DefaultRuntime(null, platform);

    @AfterAll
    public static void setUp() {
        platform.shutdown();
    }

    @Test
    void testRace() {
        IO<Failure, Integer> io = slow(100, 2).race(
                slow(1, 5)
        );
        Assertions.assertEquals(
                Right.of(5),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    void testRaceWinnerFail() {
        IO<Failure, Integer> io = slow(50, 2).race(
                slow(1, 5).flatMap(n ->
                        IO.fail(Cause.fail(GeneralFailure.of(n)))
                )
        );
        Assertions.assertEquals(
                Right.of(2),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    void testRaceFails() {
        IO<Integer, Integer> io = slow(50, 2).<Integer, Integer>flatMap(n ->
                IO.fail(Cause.fail(n))
        ).race(
                slow(1, 5).flatMap(n ->
                        IO.fail(Cause.fail(n))
                )
        );
        Assertions.assertEquals(
                Left.of(Cause.fail(5).then(Cause.fail(2))),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    void testRaceAttempt() {
        IO<Failure, Integer> io = slow(100, 2).raceAttempt(
                slow(1, 5)
        );
        Assertions.assertEquals(
                Right.of(5),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    void testRaceAttemptWinnerFail() {
        IO<Failure, Integer> io = slow(50, 2).raceAttempt(
                slow(1, 5).flatMap(n ->
                        IO.fail(Cause.fail(GeneralFailure.of(n)))
                )
        );
        Assertions.assertEquals(
                Left.of(Cause.fail(GeneralFailure.of(5))),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    void testRaceAttemptFails() {
        IO<Integer, Integer> io = slow(50, 2).<Integer, Integer>flatMap(n ->
                IO.fail(Cause.fail(n))
        ).raceAttempt(
                slow(1, 5).flatMap(n ->
                        IO.fail(Cause.fail(n))
                )
        );
        Assertions.assertEquals(
                Left.of(Cause.fail(5)),
                defaultRuntime.unsafeRun(io)
        );
    }

    private <A> Either<Exception, A> slowOld(long millis, A value) {
        try {
            Thread.sleep(millis);
//            System.out.println("Value: " + value);
            return Right.of(value);
        } catch (InterruptedException e) {
            //e.printStackTrace();
            return Left.of(e);
        }
    }

    private <A> IO<Failure, A> slow(long millis, A value) {
        return IO.effect(() -> {
            Thread.sleep(millis);
            return value;
        }).blocking();
    }
}
