package fp.io;

import java.io.InputStream;

import java.net.InetAddress;
import java.net.InetSocketAddress;
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

    final Runtime defaultRuntime = new DefaultRuntime(null, platform);

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

    private static final List<IO<Failure, String>> tasks =
        Arrays.asList(
            printSleepPrint(10 * second, "task1"),
            printSleepFail(1 * second, "task2"),
            printSleepPrint(3 * second, "task3"),
            printSleepPrint(2 * second, "task4"),
            printSleepPrint(2 * second, "task5")
        )
        .stream()
        .collect(Collectors.toList());

    @AfterClass
    public static void setUp() {
        platform.shutdown();
    }

    public static <R> IO<Failure, R> run(
        List<IO<Failure, R>> tasks,
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

    public static <R> IO<Failure, R> run2(
        final List<IO<Failure, R>> tasks,
        final long delay
    ) {
        if (tasks.isEmpty()) {
            return IO.fail(Cause.fail(GeneralFailure.of("no tasks")));
        } else if (tasks.size() == 1) {
            return tasks.get(0);
        } else {
            final IO<Failure, Fiber<Failure, Void>> sleepIO =
                IO.<Failure>sleep(delay).fork();
            return sleepIO.flatMap(sleepFiber ->
                tasks.get(0).onError(f -> sleepFiber.interruptSleeping())
                .race(
                    IO.join(sleepFiber).<R>andThen(
                    run2(tasks.subList(1, tasks.size()), delay)
                ))
            );
        }
    }

    public static IO<Failure, String> printSleepPrint(long sleep, String name) {
        IO<Failure, String> task = IO.bracket(
            IO.effectTotal(() -> log("START: " + name)),
            n -> IO.effectTotal(() -> log("Close: " + name)),
            n -> IO.sleep(sleep).flatMap(p2 ->
            IO.effectTotal(() -> log("DONE:  " + name)).flatMap(p3 ->
            IO.succeed(name)))
        );
        return task.setName(name);
    }

    public static IO<Failure, String> printSleepFail(long sleep, String name) {
        IO<Failure, String> task = IO.bracket(
            IO.effectTotal(() -> log("START: " + name)),
            n -> IO.effectTotal(() -> log("Close: " + name)),
            n -> IO.sleep(sleep).flatMap(p2 ->
            IO.effectTotal(() -> log("FAIL:  " + name)).flatMap(p3 ->
            IO.fail(Cause.fail(GeneralFailure.of("Fail: " + name)))))
        );
        return task.setName(name);
    }

    private static String log(String message) {
        LOG.fine(message);
        return message;
    }

    @Test
    public void testHappySockets() {
        IO<Failure, Socket> io =
            IO.effect(() -> Arrays.asList(
                InetAddress.getAllByName("debian.org")
            )).blocking()
            .peek(a -> LOG.fine(a.toString()))
            .map(addresses -> addresses.stream().map(a ->
                IO.bracket(
                    IO.effect(() -> {
                        Socket socket = new Socket();
                        InetSocketAddress inetSocketAddress =
                            new InetSocketAddress(a, 443);
                        LOG.finer("Create socket: " + inetSocketAddress);
                        socket.connect(inetSocketAddress, 1000);
                        LOG.finer("Socket created: " + socket);
                        return socket;
                    }).blocking().setName("createSocket " + a),
                    socket -> IO.effect(() -> {
                        socket.close();
                        LOG.finer("Closed: " + socket);
                    }).blocking().setName("closeSocket " + a),
                    socket -> IO.effectTotal(() -> {
                        LOG.finer("Connected: " + socket);
                        return socket;
                    })
                )
            ).collect(Collectors.toList()))
            .<Failure, Socket>flatMap(
                tasks -> HappyEyeballs.<Socket>run2(tasks, 250000L)
            )
            .peek(s -> LOG.fine("Connected: " + s.getInetAddress()));
        Either<Cause<Failure>, Socket> result = defaultRuntime.unsafeRun(
            io
        );
        Assert.assertTrue(result.isRight());
    }

    @Test
    public void testHappyEyeballs1() {
        log("Start testHappyEyeballs1");
        IO<Failure, String> io = run(tasks, 2 * second);
        Either<Cause<Failure>, String> result = defaultRuntime.unsafeRun(io);
        Assert.assertEquals(Right.of("task3"), result);
    }

    @Test
    public void testHappyEyeballs2() {
        log("Start testHappyEyeballs2");
        IO<Failure, String> io = run2(tasks, 2 * second);
        Either<Cause<Failure>, String> result = defaultRuntime.unsafeRun(io);
        Assert.assertEquals(Right.of("task3"), result);
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

    private <A> IO<Failure, A> slow(long millis, A value) {
        return IO.effect(() -> {
            Thread.sleep(millis);
            return value;
        }).blocking();
    }
}
