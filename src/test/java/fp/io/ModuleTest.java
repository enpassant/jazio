package fp.io;

import java.text.MessageFormat;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import fp.util.Either;
import fp.util.Failure;
import fp.util.Right;

public class ModuleTest {
    final static DefaultPlatform platform = new DefaultPlatform();

    final Runtime<Void> defaultVoidRuntime =
        new DefaultRuntime<Void>(null, platform);

    @AfterClass
    public static void setUp() {
        platform.shutdown();
    }

    private class TestConsole implements Console.Service {
        private final StringBuilder sb = new StringBuilder();
        private final String[] inputs;
        private int inputIndex = 0;

        public TestConsole(final String... inputs) {
            this.inputs = inputs;
        }

        @Override
        public IO<Object, Object, Void> println(String line) {
            return IO.effectTotal(() -> { sb.append(line).append("\n"); });
        }
        @Override
        public IO<Object, Failure, String> readLine() {
            return IO.effect(() -> inputs[inputIndex++]);
        }
        public String getOutputs() {
            return sb.toString();
        }
    }

    private class TestLog implements Log.Service {
        private final StringBuilder sb = new StringBuilder();

        private IO<Object, Object, Void> log(
            String level,
            String message,
            Object... params
        ) {
            return IO.effectTotal(() -> {
                sb.append("[" + level + "] ")
                    .append(MessageFormat.format(message, params))
                    .append("\n");
            });
        }
        @Override
        public IO<Object, Object, Void> error(String message, Object... params) {
            return log("Error", message, params);
        }
        @Override
        public IO<Object, Object, Void> debug(String message, Object... params) {
            return log("Debug", message, params);
        }
        @Override
        public IO<Object, Object, Void> info(String message, Object... params) {
            return log("Info", message, params);
        }
        @Override
        public IO<Object, Object, Void> warning(String message, Object... params) {
            return log("Warning", message, params);
        }

        public String getOutputs() {
            return sb.toString();
        }
    }

    @Test
    public void testModules() {
        IO<Environment, Object, String> io =
            Log.info("Start program").flatMap(l1 ->
            Console.println("Good morning, what is your name?").flatMap(c1 ->
            Console.readLine().flatMap(name ->
            Log.debug("User''s name: {0}", name).flatMap(l2 ->
            Console.println("Good to meet you, " + name + "!").flatMap(c2 ->
            Log.info("Program has finished").map(l3 ->
            name
        ))))));

        final TestConsole testConsole = new TestConsole("John");
        final TestLog testLog = new TestLog();

//        final Logger logger = Logger.getLogger(ModuleTest.class.getName());
        final Environment environment =
//            Environment.of(Console.Service.class, new Console.Live())
//                .and(Log.Service.class, new Log.Live(logger));
            Environment.of(Console.Service.class, testConsole)
                .and(Log.Service.class, testLog);

        final Either<Cause<Object>, String> name =
            defaultVoidRuntime.unsafeRun(io.provide(environment));

        Assert.assertEquals(Right.of("John"), name);
        Assert.assertEquals(
            "Good morning, what is your name?\n" + "Good to meet you, John!\n",
            testConsole.getOutputs()
        );
        Assert.assertEquals(
            "[Info] Start program\n" +
            "[Debug] User's name: John\n" +
            "[Info] Program has finished\n",
            testLog.getOutputs()
        );
    }
}
