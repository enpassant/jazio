package fp.io;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;

import fp.io.console.Console;
import fp.io.console.TestConsole;
import fp.io.log.Log;
import fp.io.log.TestLog;
import fp.util.Either;
import fp.util.Right;

public class ModuleTest {
    final static DefaultPlatform platform = new DefaultPlatform();

    final Runtime<Void> defaultVoidRuntime =
        new DefaultRuntime<Void>(null, platform);

    @AfterClass
    public static void setUp() {
        platform.shutdown();
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

        final Environment environment =
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
