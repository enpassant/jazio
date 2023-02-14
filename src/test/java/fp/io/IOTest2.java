package fp.io;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import fp.util.Either;
import fp.util.ExceptionFailure;
import fp.util.Failure;
import fp.util.GeneralFailure;
import fp.util.Left;
import fp.util.Right;
import fp.util.Tuple2;

public class IOTest2 {
    final static DefaultPlatform platform = new DefaultPlatform();

    final Runtime<Void> defaultVoidRuntime = new DefaultRuntime<Void>(null, platform);
    final Runtime<Object> defaultRuntime = new DefaultRuntime<Object>(null, platform);

    @AfterClass
    public static void setUp() {
        platform.shutdown();
    }

    @Test
    public void testRace() {
        IO<Object, Failure, Integer> io = slow(100, 2).race(
            slow(1, 5)
        );
        Assert.assertEquals(
            Right.of(5),
            defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testRaceWinnerFail() {
        IO<Object, Failure, Integer> io = slow(50, 2).race(
            slow(1, 5).<Failure, Integer>flatMap(n ->
                IO.fail(Cause.fail(GeneralFailure.of(n)))
            )
        );
        Assert.assertEquals(
            Right.of(2),
            defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testRaceFails() {
        IO<Object, Integer, Integer> io = slow(50, 2).<Integer, Integer>flatMap(n ->
            IO.fail(Cause.fail(n))
        ).race(
            slow(1, 5).<Integer, Integer>flatMap(n ->
                IO.fail(Cause.fail(n))
            )
        );
        Assert.assertEquals(
            Left.of(Cause.fail(5).then(Cause.fail(2))),
            defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testRaceAttempt() {
        IO<Object, Failure, Integer> io = slow(100, 2).raceAttempt(
            slow(1, 5)
        );
        Assert.assertEquals(
            Right.of(5),
            defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testRaceAttemptWinnerFail() {
        IO<Object, Failure, Integer> io = slow(50, 2).raceAttempt(
            slow(1, 5).<Failure, Integer>flatMap(n ->
                IO.fail(Cause.fail(GeneralFailure.of(n)))
            )
        );
        Assert.assertEquals(
            Left.of(Cause.fail(GeneralFailure.of(5))),
            defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testRaceAttemptFails() {
        IO<Object, Integer, Integer> io = slow(50, 2).<Integer, Integer>flatMap(n ->
            IO.fail(Cause.fail(n))
        ).raceAttempt(
            slow(1, 5).<Integer, Integer>flatMap(n ->
                IO.fail(Cause.fail(n))
            )
        );
        Assert.assertEquals(
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

    private <A> IO<Object, Failure, A> slow(long millis, A value) {
        return IO.effect(() -> {
            Thread.sleep(millis);
            return value;
        }).blocking();
    }
}
