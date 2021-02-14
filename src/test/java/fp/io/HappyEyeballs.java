package fp.io;

import java.io.InputStream;

import java.net.InetAddress;
import java.net.Socket;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.logging.LogManager;
import java.util.stream.Collectors;
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

public class HappyEyeballs {
    final static DefaultPlatform platform = new DefaultPlatform();

    final Runtime<Object> defaultRuntime = new DefaultRuntime<Object>(null, platform);

    static {
        final InputStream is = HappyEyeballs.class.getClassLoader()
            .getResourceAsStream("logging.properties");

        try {
            LogManager.getLogManager().readConfiguration(is);
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final static Logger LOG = Logger.getLogger(HappyEyeballs.class.getName());

    private final static long second = 10000000L;

    private static long number = 0L;

    private static final List<IO<Object, Failure, String>> tasks =
        Arrays.asList(
            printSleepPrint(10 * second, "task1"),
            printSleepFail(1 * second, "task2"),
            printSleepPrint(3 * second, "task3"),
            printSleepPrint(2 * second, "task4"),
            printSleepPrint(2 * second, "task5")
        )
        .stream()
        .map(io -> bracket(io))
        .collect(Collectors.toList());

    @AfterClass
    public static void setUp() {
        platform.shutdown();
    }

    public static <R> IO<Object, Failure, R> run(
        List<IO<Object, Failure, R>> tasks,
        long delay
    ) {
        if (tasks.isEmpty()) {
            return IO.fail(Cause.fail(GeneralFailure.of("no tasks")));
        } else if (tasks.size() == 1) {
            return tasks.get(0);
        } else {
            return tasks.get(0).race(
                run(tasks.subList(1, tasks.size()), delay).delay(delay)
            );
        }
    }

    public static <R> IO<Object, Failure, R> run2(
        List<IO<Object, Failure, R>> tasks,
        long delay
    ) {
        if (tasks.isEmpty()) {
            return IO.fail(Cause.fail(GeneralFailure.of("no tasks")));
        } else if (tasks.size() == 1) {
            return tasks.get(0);
        } else {
            IO<Object, Failure, Fiber<Failure, Object>> sleepIO =
                IO.<Object, Failure, Object>sleep(delay).fork();
            return sleepIO.flatMap(sleep ->
                tasks.get(0).onError(f -> sleep.interruptSleeping())
                .race(
                    IO.join(sleep).<R>andThen(
                    run2(tasks.subList(1, tasks.size()), delay)
                ))
            );
        }
    }

    public static IO<Object, Failure, String> bracket(
        IO<Object, Failure, String> taskIo
    ) {
        number++;
        return IO.bracket(
            IO.succeed("task" + number),
            name -> IO.effectTotal(() -> LOG.info("Close: " + name)),
            name -> taskIo
        );
    }

    public static IO<Object, Failure, String> printSleepPrint(long sleep, String name) {
        return IO.effect(() -> log("START: " + name)).flatMap(p1 ->
            IO.sleep(sleep).flatMap(p2 ->
            IO.effect(() -> log("DONE:  " + name)).flatMap(p3 ->
            IO.succeed(name))));
    }

    public static IO<Object, Failure, String> printSleepFail(long sleep, String name) {
        return IO.effect(() -> log("START: " + name)).flatMap(p1 ->
            IO.sleep(sleep).flatMap(p2 ->
            IO.effect(() -> log("FAIL:  " + name)).flatMap(p3 ->
            IO.fail(Cause.fail(GeneralFailure.of("Fail: " + name))))));
    }

    private static String log(String message) {
        LOG.info(message);
        return message;
    }

    @Test
    public void testHappySockets() {
        IO<Object, Failure, Socket> io =
            IO.effect(() -> Arrays.asList(
                InetAddress.getAllByName("debian.org")
            )).blocking()
            .peek(a -> LOG.info(a.toString()))
            .map(addresses -> addresses.stream().map(a ->
                IO.effect(() -> new Socket(a, 443)).blocking()
            ).collect(Collectors.toList()))
            .<Failure, Socket>flatMap(tasks -> HappyEyeballs.<Socket>run2(tasks, 250000000L))
            .peek(s -> LOG.info("Connected: " + s.getInetAddress()));
        Either<Cause<Failure>, Socket> result = defaultRuntime.unsafeRun(io);
        Assert.assertTrue(result.isRight());
    }

    @Test
    public void testHappyEyeballs1() {
        IO<Object, Failure, String> io = run(tasks, 2 * second);
        Either<Cause<Failure>, String> result = defaultRuntime.unsafeRun(io);
        Assert.assertEquals(Right.of("task3"), result);
    }

    @Test
    public void testHappyEyeballs2() {
        IO<Object, Failure, String> io = run2(tasks, 2 * second);
        Either<Cause<Failure>, String> result = defaultRuntime.unsafeRun(io);
        Assert.assertEquals(Right.of("task3"), result);
    }
}
