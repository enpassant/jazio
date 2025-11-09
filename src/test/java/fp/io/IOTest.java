package fp.io;

import fp.util.Either;
import fp.util.ExceptionFailure;
import fp.util.Failure;
import fp.util.GeneralFailure;
import fp.util.Left;
import fp.util.Right;
import fp.util.Tuple2;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

public class IOTest {
    final static DefaultPlatform platform = new DefaultPlatform();

    final Runtime defaultRuntime = new DefaultRuntime(null, platform);

    @AfterClass
    public static void setUp() {
        platform.shutdown();
    }

    @Test
    public void testAbsolveSuccess() {
        IO<Void, Integer> io = IO.absolve(IO.succeed(Right.of(4)));
        Assert.assertEquals(Right.of(4), defaultRuntime.unsafeRun(io));
    }

    @Test
    public void testAbsolveFailure() {
        IO<Integer, Integer> io = IO.absolve(IO.succeed(Left.of(4)));
        Assert.assertEquals(Left.of(Cause.fail(4)), defaultRuntime.unsafeRun(io));
    }

    @Test
    public void testBlocking() {
        IO<Object, String> io = IO.effectTotal(
                () -> Thread.currentThread().getName()
        ).blocking();
        Assert.assertTrue(
                "It is not a blocking thread's name",
                defaultRuntime.unsafeRun(io)
                        .orElse("")
                        .contains("blocking")
        );
    }

    @Test
    public void testPureIO() {
        IO<Void, Integer> io = IO.succeed(4);
        Assert.assertEquals(Right.of(4), defaultRuntime.unsafeRun(io));
    }

    @Test
    public void testDie() {
        IO<Void, Integer> io = IO.effectTotal(() -> 8 / 0);
        final Either<Cause<Void>, Integer> result = defaultRuntime.unsafeRun(io);
        Assert.assertTrue(
                result.toString(),
                result.isLeft() && result.left().isDie()
        );
    }

    @Test
    public void testEitherSuccess() {
        IO<Void, Either<Object, Integer>> io = IO.succeed(8).either();
        Assert.assertEquals(
                Right.of(Right.of(8)),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testEitherFail() {
        IO<Void, Either<String, Object>> io =
                IO.fail(Cause.fail("Syntax error")).either();

        Assert.assertEquals(
                Right.of(Left.of("Syntax error")),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testFail() {
        IO<String, ?> io = IO.fail(Cause.fail("Syntax error"));
        Assert.assertEquals(
                Left.of(Cause.fail("Syntax error")),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testFoldSuccess() {
        IO<String, Integer> ioValue = IO.succeed(4);
        IO<String, Integer> io = ioValue
                .foldM(
                        v -> IO.succeed(8),
                        v -> IO.succeed(v * v)
                );
        Assert.assertEquals(Right.of(16), defaultRuntime.unsafeRun(io));
    }

    @Test
    public void testFoldFailure() {
        IO<String, Integer> ioValue = IO.fail(Cause.fail("Error"));
        IO<String, Integer> io = ioValue
                .foldM(
                        v -> IO.succeed(v.length()),
                        v -> IO.succeed(v * v)
                );
        Assert.assertEquals(Right.of(5), defaultRuntime.unsafeRun(io));
    }

    @Test
    public void testFoldCauseFailure() {
        IO<String, Integer> ioValue = IO.fail(Cause.fail("Error"));
        IO<String, Integer> io = ioValue
                .foldCauseM(
                        v -> IO.succeed(v.getValue().length()),
                        v -> IO.succeed(v * v)
                );
        Assert.assertEquals(Right.of(5), defaultRuntime.unsafeRun(io));
    }

    @Test
    public void testFork() {
        IO<Object, Integer> io =
                IO.effectTotal(() -> 6).fork().flatMap(fiber1 ->
                        IO.effectTotal(() -> 7).fork().flatMap(fiber2 ->
                                IO.join(fiber1).flatMap(value1 ->
                                        IO.join(fiber2).map(value2 ->
                                                value1 * value2
                                        ))));
        Assert.assertEquals(Right.of(42), defaultRuntime.unsafeRun(io));
    }

    @Test
    public void testBlockingFork() {
        IO<Object, Integer> io =
                IO.effectTotal(() -> 6).blocking().fork().flatMap(fiber1 ->
                        IO.effectTotal(() -> 7).blocking().fork().flatMap(fiber2 ->
                                IO.join(fiber1).flatMap(value1 ->
                                        IO.join(fiber2).map(value2 ->
                                                value1 * value2
                                        ))));
        Assert.assertEquals(Right.of(42), defaultRuntime.unsafeRun(io));
    }

    @Test
    public void testBlockingAndNoBlockingForks() {
        IO<Object, String> ioThreadName = IO.effectTotal(() ->
                Thread.currentThread().getName()
        );
        IO<Object, String> io =
                ioThreadName.fork().flatMap(fiber1 ->
                        ioThreadName.blocking().fork().flatMap(fiber2 ->
                                IO.join(fiber1).flatMap(value1 ->
                                        IO.join(fiber2).map(value2 ->
                                                value1 + "," + value2
                                        ))));
        Either<Cause<Object>, String> result = defaultRuntime.unsafeRun(io);
        final String resultStr = result.orElse("");
        Assert.assertEquals(
                "One of the thread's name is not good: " + result,
                resultStr.indexOf("blocking"),
                resultStr.lastIndexOf("blocking")
        );
    }

    @Test
    public void testFlatMapIO() {
        IO<Void, Integer> io = IO.succeed(4).flatMap(
                n -> IO.effectTotal(() -> n * n)
        );
        Assert.assertEquals(Right.of(16), defaultRuntime.unsafeRun(io));
    }

    @Test
    public void testEffectPartial() {
        IO<Failure, Integer> io =
                IO.effect(() -> 8 / 2).flatMap((Integer n) ->
                        IO.effectTotal(() -> n * n)
                );
        Assert.assertEquals(
                Right.of(16),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testEffectPartialWithFailure() {
        IO<Failure, Integer> io =
                IO.effect(() -> 8 / 0).flatMap((Integer n) ->
                        IO.effectTotal(() -> n * n)
                );
        Assert.assertEquals(
                Left.of(Cause.fail(ExceptionFailure.of(
                        new ArithmeticException("/ by zero")))
                ).toString(),
                defaultRuntime.unsafeRun(io).toString()
        );
    }

    @Test
    public void testContext() {
        IO<Object, String> io = IO.access(
                Integer.class,
                (Integer n) -> Integer.toString(n * n)
        ).provide(Integer.class, 4);
        Assert.assertEquals(Right.of("16"), defaultRuntime.unsafeRun(io));
    }

    @Test
    public void testContextMissing() {
        IO<Object, String> io = IO.access(
                Integer.class,
                (Integer n) -> Integer.toString(n * n)
        );
        Assert.assertEquals(
                Left.of(
                        Cause.fail("Missing context: java.lang.Integer")
                ),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testContextScopes() {
        final IO<Object, String> io = IO.succeed("OK").flatMap(
                str -> IO.access(
                        Integer.class,
                        n -> Integer.toString(n * n)
                ).provide(Integer.class, 4)
        ).flatMap(
                str -> IO.access(
                        Integer.class,
                        (Integer n) -> Integer.toString(n * n)
                ).recoverCause(objectCause -> IO.succeed(objectCause.toString()))
        );
        Assert.assertEquals(
                Right.of("Fail(Missing context: java.lang.Integer)"),
                defaultRuntime.unsafeRun(io)
        );
    }

    private static class Resource {
        private boolean acquired = true;

        private int usage = 0;

        public Integer use(int n) {
            usage = usage + n;
            return usage;
        }

        public void close() {
            acquired = false;
        }
    }

    @Test
    public void testBracket() {
        final Resource res = new Resource();
        final IO<Void, Integer> io = IO.bracket(
                IO.succeed(res),
                resource -> IO.effectTotal(resource::close),
                resource -> IO.effectTotal(() -> resource.use(2))
        );
        Assert.assertEquals(Right.of(2), defaultRuntime.unsafeRun(io));
        Assert.assertFalse(res.acquired);
    }

    @Test
    public void testBracketWithTwoFailures() {
        final Resource res = new Resource();
        final GeneralFailure<String> notClosable = GeneralFailure.of("Not closable");
        final GeneralFailure<String> divideByZero = GeneralFailure.of("/ by zero");

        final IO<Failure, Integer> io = IO.bracket(
                IO.succeed(res),
                resource -> IO.fail(Cause.die(notClosable)),
                resource -> IO.effect(() -> 8 / 2)
                        .flatMap(i -> IO.fail(Cause.die(divideByZero)))
        );

        Assert.assertEquals(
                Left.of(
                        Cause.die(divideByZero).then(
                                Cause.die(notClosable))
                ),
                defaultRuntime.unsafeRun(io)
        );
        Assert.assertTrue(res.acquired);
    }

    @Test
    public void testNestedBracket() {
        final Resource res1 = new Resource();
        final Resource res2 = new Resource();
        final IO<Void, Integer> io = IO.bracket(
                IO.effectTotal(() -> res1),
                resource -> IO.effectTotal(resource::close),
                resource -> IO.effectTotal(() -> resource.use(3)).flatMap(n ->
                        IO.bracket(
                                IO.effectTotal(() -> res2),
                                resource2 -> IO.effectTotal(resource2::close),
                                resource2 -> IO.effectTotal(() -> n + resource2.use(4))
                        )
                )
        );
        Assert.assertEquals(Right.of(7), defaultRuntime.unsafeRun(io));
        Assert.assertFalse(res1.acquired);
        Assert.assertFalse(res2.acquired);
    }

    @Test
    public void testNestedBracketWithFailure() {
        final Resource res1 = new Resource();
        final Resource res2 = new Resource();
        final IO<String, Integer> io = IO.bracket(
                IO.effectTotal(() -> res1),
                resource -> IO.effectTotal(resource::close),
                resource -> IO.effectTotal(() -> resource.use(5)).flatMap(n ->
                        IO.bracket(
                                IO.effectTotal(() -> res2),
                                resource2 -> IO.effectTotal(resource2::close),
                                resource2 -> IO.effectTotal(() -> n + resource2.use(6)).flatMap(i ->
                                        IO.fail(Cause.fail("Failure")))
                        )
                )
        );
        Assert.assertEquals(Left.of(Cause.fail("Failure")), defaultRuntime.unsafeRun(io));
        Assert.assertFalse(res1.acquired);
        Assert.assertFalse(res2.acquired);
    }

    private IO<Void, Boolean> odd(int n) {
        return IO.effectTotal(() -> n == 0).flatMap(isZero ->
                isZero ?
                        IO.succeed(false) :
                        even(n - 1)
        );
    }

    private IO<Void, Boolean> even(int n) {
        return IO.effectTotal(() -> n == 0).flatMap(isZero ->
                isZero ?
                        IO.succeed(true) :
                        odd(n - 1)
        );
    }

    @Test
    public void testMutuallyTailRecursive() {
        IO<Void, Boolean> io = even(100000);
        Assert.assertEquals(Right.of(true), defaultRuntime.unsafeRun(io));
    }

    //@Test
    //public void testLock() {
    //ExecutorService asyncExecutor = Executors.newFixedThreadPool(4);
    //ExecutorService blockingExecutor = Executors.newCachedThreadPool();
    //ExecutorService calcExecutor = new ForkJoinPool(2);

    //Function<Integer, IO<Void, Integer>> fnIo = n -> IO.effectTotal(() -> {

    /// /          System.out.println(n + ": " + Thread.currentThread().getName());
    //return n + 1;
    //});
    //IO<Void, Integer> lockIo =
    //IO.succeed(1).flatMap(n ->
    //fnIo.apply(n).on(asyncExecutor).flatMap(n1 ->
    //fnIo.apply(n1).on(blockingExecutor).flatMap(n2 ->
    //fnIo.apply(n2).flatMap(n3 ->
    //fnIo.apply(n3).flatMap(n4 ->
    //fnIo.apply(n4).flatMap(n5 ->
    //fnIo.apply(n5).on(calcExecutor).flatMap(
    //fnIo
    //)))))));
    //Assert.assertEquals(Right.of(8), defaultRuntime.unsafeRun(lockIo));

    //asyncExecutor.shutdown();
    //blockingExecutor.shutdown();
    //calcExecutor.shutdown();
    //}
    @Test
    public void testUnit() {
        IO<Object, Integer> io = IO.succeed(2).flatMap(i1 ->
                IO.unit().map(i2 ->
                        i1 * i1
                ));
        Assert.assertEquals(Right.of(4), defaultRuntime.unsafeRun(io));
    }

    @Test
    public void testPeek() {
        final Resource res = new Resource();
        IO<Object, Resource> io = IO.succeed(res)
                .peek(r1 -> r1.use(7))
                .peek(Resource::close);
        Assert.assertEquals(Right.of(res), defaultRuntime.unsafeRun(io));
        Assert.assertEquals(7, res.usage);
        Assert.assertFalse(res.acquired);
    }

    @Test
    public void testPeekM() {
        final Resource res = new Resource();
        IO<Object, Resource> io = IO.succeed(res)
                .peek(r1 -> r1.use(7))
                .peekM(r2 -> IO.effectTotal(r2::close));
        Assert.assertEquals(Right.of(res), defaultRuntime.unsafeRun(io));
        Assert.assertEquals(7, res.usage);
        Assert.assertFalse(res.acquired);
    }

    @Test
    public void testPeekMWithFailure() {
        final Resource res = new Resource();
        IO<Object, Resource> io = IO.succeed(res)
                .peek(r1 -> r1.use(7))
                .peekM(r2 -> IO.fail(Cause.fail(5))
                );
        Assert.assertEquals(Left.of(Cause.fail(5)), defaultRuntime.unsafeRun(io));
        Assert.assertEquals(7, res.usage);
        Assert.assertTrue(res.acquired);
        res.close();
    }

    @Test
    public void testRecoverWithSuccess() {
        IO<Object, Integer> io = IO.succeed(3)
                .recover(failure -> IO.succeed(5));
        Assert.assertEquals(Right.of(3), defaultRuntime.unsafeRun(io));
    }

    @Test
    public void testRecoverWithFailure() {
        IO<Object, Integer> io = IO.<Object, Integer>fail(Cause.fail(3))
                .recover(failure -> IO.succeed(5));
        Assert.assertEquals(Right.of(5), defaultRuntime.unsafeRun(io));
    }

    @Test
    public void testRecoverWithDie() {
        IO<Object, Integer> io =
                IO.<Object, Integer>fail(Cause.die(GeneralFailure.of(3)))
                        .recover(failure -> IO.succeed(5));
        Assert.assertEquals(
                Left.of(Cause.die(GeneralFailure.of(3))),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testRecoverCauseWithSuccess() {
        IO<Object, Integer> io = IO.succeed(3)
                .recoverCause(failure -> IO.succeed(5));
        Assert.assertEquals(Right.of(3), defaultRuntime.unsafeRun(io));
    }

    @Test
    public void testRecoverCauseWithFailure() {
        IO<Object, Integer> io = IO.<Object, Integer>fail(Cause.fail(3))
                .recoverCause(failure -> IO.succeed(5));
        Assert.assertEquals(Right.of(5), defaultRuntime.unsafeRun(io));
    }

    @Test
    public void testRecoverCauseWithDie() {
        IO<Object, Integer> io =
                IO.<Object, Integer>fail(Cause.die(GeneralFailure.of(3)))
                        .recoverCause(failure -> IO.succeed(5));
        Assert.assertEquals(Right.of(5), defaultRuntime.unsafeRun(io));
    }

    @Test
    public void testDelay() {
        long delayNanoseconds = 5000000L;
        IO<Object, Boolean> io = IO.succeed(System.nanoTime())
                .map(start -> System.nanoTime() - start).delay(delayNanoseconds)
                .map(elapsed -> elapsed >= delayNanoseconds);
        Assert.assertEquals(Right.of(true), defaultRuntime.unsafeRun(io));
    }

    @Test
    public void testRepeat() {
        final Resource res = new Resource();
        IO<Object, Resource> io = IO.succeed(res)
                .peek(r1 -> r1.use(1)).repeat(5)
                .peek(Resource::close);
        Assert.assertEquals(Right.of(res), defaultRuntime.unsafeRun(io));
        Assert.assertEquals(5, res.usage);
    }

    @Test
    public void testRepeatWithFailure() {
        final Resource res = new Resource();
        IO<Object, Resource> ioInit = IO.succeed(res)
                .flatMap(r1 -> {
                    r1.use(1);
                    return IO.<Object, Resource>fail(Cause.fail(5));
                }).repeat(5);
        var io = ioInit.peek(Resource::close);
        Assert.assertEquals(Left.of(Cause.fail(5)), defaultRuntime.unsafeRun(io));
        Assert.assertEquals(1, res.usage);
    }

    @Test
    public void testRetry() {
        final Resource res = new Resource();
        IO<Object, Resource> io = IO.succeed(res)
                .peek(r1 -> r1.use(1)).retry(5)
                .peek(Resource::close);
        Assert.assertEquals(Right.of(res), defaultRuntime.unsafeRun(io));
        Assert.assertEquals(1, res.usage);
    }

    @Test
    public void testRetryWithFailure() {
        final Resource res = new Resource();
        IO<Object, Resource> ioInit = IO.succeed(res)
                .flatMap(r1 -> {
                    r1.use(1);
                    return IO.<Object, Resource>fail(Cause.fail(5));
                }).retry(5);
        var io = ioInit.peek(Resource::close);
        Assert.assertEquals(Left.of(Cause.fail(5)), defaultRuntime.unsafeRun(io));
        Assert.assertEquals(5, res.usage);
    }

    @Test
    public void testRetryWithRecoveredFailure() {
        final Resource res = new Resource();
        IO<Object, Resource> ioInit = IO.succeed(res)
                .flatMap(r1 -> {
                    r1.use(1);
                    if (r1.usage > 3) {
                        return IO.succeed(r1);
                    } else {
                        return IO.<Object, Resource>fail(Cause.fail(5));
                    }
                }).retry(5);
        var io = ioInit.peek(Resource::close);
        Assert.assertEquals(Right.of(res), defaultRuntime.unsafeRun(io));
        Assert.assertEquals(4, res.usage);
    }

    @Test
    public void testTimeoutWithInterrupt() {
        IO<Failure, Integer> io = slow(1000, 2).timeout(20000000);

        Assert.assertEquals(
                Left.of(Cause.interrupt()),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testTimeoutWithoutInterrupt() {
        IO<Failure, Integer> io = slow(10, 2).timeout(1000000000);

        Assert.assertEquals(
                Right.of(2),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testSequence() {
        final Stream<IO<Object, Integer>> streamIO =
                Stream.of(IO.effectTotal(() -> 13), IO.effectTotal(() -> 6), IO.succeed(4));

        IO<Object, Integer> io = IO.sequence(streamIO)
                .map(stream -> stream.mapToInt(i -> i * 2).sum());
        Assert.assertEquals(
                Right.of(46),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testSequenceBig() {
        final int count = 100_000;
        final Stream<IO<Object, Integer>> streamIO =
                IntStream.range(0, count).mapToObj(IO::succeed);

        IO<Object, Integer> io = IO.sequence(streamIO)
                .map(stream -> stream.mapToInt(i -> i * 2).sum());
        Assert.assertEquals(
                Right.of((count - 1) * count),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testSequencePar() {
        final long millis = 100;

        final Stream<IO<Failure, Integer>> streamIO =
                Stream.of(slow(millis, 13), slow(millis, 6), slow(millis, 4));

        final long start = System.currentTimeMillis();

        IO<Failure, Integer> io = IO.sequencePar(streamIO)
                .map(stream ->
                        stream.mapToInt(
                                r -> r * 2
                        ).sum()
                );

        Assert.assertEquals(
                Right.of(46),
                defaultRuntime.unsafeRun(io)
        );

        final long time = System.currentTimeMillis() - start;

        Assert.assertTrue(
                "Time was: " + time,
                time < 3 * millis
        );
    }

    @Test
    public void testSequenceParBig() {
        final int count = 100_000;
        final Stream<IO<Object, Integer>> streamIO =
                IntStream.range(0, count).mapToObj(IO::succeed);

        IO<Object, Integer> io = IO.sequencePar(streamIO)
                .map(stream -> stream.mapToInt(i -> i * 2).sum());
        Assert.assertEquals(
                Right.of((count - 1) * count),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testSequenceRace() {
        final long millis = 100;

        final Stream<IO<Failure, Integer>> streamIO =
                Stream.of(slow(2 * millis, 13), slow(millis, 6), slow(2 * millis, 4));

        final long start = System.currentTimeMillis();

        IO<Failure, Integer> io = IO.sequenceRace(streamIO);

        Assert.assertEquals(
                Right.of(6),
                defaultRuntime.unsafeRun(io)
        );

        final long time = System.currentTimeMillis() - start;

        Assert.assertTrue(
                "Time was: " + time,
                time < 3 * millis
        );
    }

    @Test
    public void testZipWith() {
        IO<Object, String> io = IO.succeed(2).zipWith(
                IO.succeed("Test"),
                (i, s) -> s + "-" + i
        );
        Assert.assertEquals(
                Right.of("Test-2"),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testZip() {
        IO<Object, Tuple2<Integer, String>> io = IO.succeed(2).zip(
                IO.succeed("Test")
        );
        Assert.assertEquals(
                Right.of(Tuple2.of(2, "Test")),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testZipPar() {
        IO<Failure, Tuple2<Integer, String>> io = slow(10, 2).zipPar(
                slow(1, "Test")
        );
        Assert.assertEquals(
                Right.of(Tuple2.of(2, "Test")),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testZipParWith() {
        IO<Failure, String> io = slow(10, 2).zipParWith(
                slow(1, "Test"),
                (i, s) -> s + "-" + i
        );
        Assert.assertEquals(
                Right.of("Test-2"),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testRace() {
        IO<Failure, Integer> io = slow(100, 2).race(
                slow(1, 5)
        );
        Assert.assertEquals(
                Right.of(5),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testRaceWinnerFail() {
        IO<Failure, Integer> io = slow(50, 2).race(
                slow(1, 5).flatMap(n ->
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
        IO<Integer, Integer> io = slow(50, 2).<Integer, Integer>flatMap(n ->
                IO.fail(Cause.fail(n))
        ).race(
                slow(1, 5).flatMap(n ->
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
        IO<Failure, Integer> io = slow(100, 2).raceAttempt(
                slow(1, 5)
        );
        Assert.assertEquals(
                Right.of(5),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testRaceAttemptWinnerFail() {
        IO<Failure, Integer> io = slow(50, 2).raceAttempt(
                slow(1, 5).flatMap(n ->
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
        IO<Integer, Integer> io = slow(50, 2).<Integer, Integer>flatMap(n ->
                IO.fail(Cause.fail(n))
        ).raceAttempt(
                slow(1, 5).flatMap(n ->
                        IO.fail(Cause.fail(n))
                )
        );
        Assert.assertEquals(
                Left.of(Cause.fail(5)),
                defaultRuntime.unsafeRun(io)
        );
    }

    @Test
    public void testZipParWithFailureFirst() {
        IO<Failure, Tuple2<Integer, String>> io =
                IO.<Failure, Integer>interrupt().zipPar(
                        slow(3000, "Test")
                );
        final Either<Cause<Failure>, Tuple2<Integer, String>> result =
                defaultRuntime.unsafeRun(io);

        Assert.assertTrue(
                result.toString(),
                result.isLeft() && result.left().isInterrupt()
        );
    }

    @Test
    public void testZipParWithFailureSecond() {
        IO<Failure, Tuple2<Integer, String>> io = slow(3000, 2).zipPar(
                IO.interrupt()
        );
        final Either<Cause<Failure>, Tuple2<Integer, String>> result =
                defaultRuntime.unsafeRun(io);

        result.forEachLeft(failure -> {
            if (failure.getFailure() instanceof final ExceptionFailure exceptionFailure) {
                exceptionFailure.throwable.printStackTrace(System.err);
            }
        });
        Assert.assertTrue(
                result.toString(),
                result.isLeft() && result.left().isInterrupt()
        );
    }

    @Test
    public void testBlockingFutureCancel() {
        final long start = System.currentTimeMillis();
        final long millis = 100L;

        final Future<?> future =
                platform.toCompletablePromise(
                        platform.getBlocking().submit(() -> slowOld(millis / 4, "OK"))
                ).thenApply(either -> {
                    ExceptionFailure.tryCatch(
                            () -> platform.toCompletablePromise(
                                    platform.getBlocking().submit(() -> slowOld(millis, "OK 2"))
                            ).get()
                    );

                    return ExceptionFailure.tryCatch(
                            () -> platform.toCompletablePromise(
                                    platform.getBlocking().submit(() -> slowOld(millis, "OK 3"))
                            ).get()
                    );
                });

        platform.getExecutor().submit(() -> {
            slowOld(millis / 3, "Cancel");
            future.cancel(true);
        });

        try {
            future.get();
        } catch (CancellationException | InterruptedException ignored) {
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        final long time = System.currentTimeMillis() - start;

        Assert.assertTrue(
                "Time was: " + time,
                time < millis
        );
    }
    /*
    //*/

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
