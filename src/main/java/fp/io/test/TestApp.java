package fp.io.test;

import fp.io.Cause;
import fp.io.Environment;
import fp.io.IO;
import fp.io.IoApp;
import fp.io.console.Console;
import fp.io.console.TestConsole;
import fp.io.log.Log;
import fp.io.log.TestLog;
import fp.util.Either;
import fp.util.HQueue;
import java.time.LocalDate;
import java.util.logging.Logger;

public class TestApp extends IoApp<Object, HQueue> {
    private final boolean isTest;

    public TestApp(boolean isTest) {
        super();
        this.isTest = isTest;
    }

    @Override
    public IO<Object, HQueue> program() {

        return Log.info("Start program").flatMap(l1 ->
                Console.println("Good morning! What is your name?").flatMap(c1 ->
                        Console.readLine().flatMap(name ->
                                Log.debug("User''s name: {0}", name).flatMap(l2 ->
                                        Console.println("Good to meet you, " + name + "!").flatMap(c2 ->
                                                Console.println("What year were you born?").flatMap(c3 ->
                                                        Console.readLine().flatMap(yearStr ->
                                                                IO.effect(
                                                                        () -> LocalDate.now()
                                                                                .minusYears(Integer.parseInt(yearStr))
                                                                                .getYear()
                                                                ).peekFailureIO(
                                                                        failure -> Log.error("Failure: " + failure)
                                                                ).recover(
                                                                        failure -> IO.succeed(0)
                                                                ).flatMap(age ->
                                                                        Log.debug("User''s age: {0}", age).flatMap(l3 ->
                                                                                Console.println("You are " + age + " year old.").flatMap(c4 ->
                                                                                        Log.info("Program has finished").map(l4 ->
                                                                                                HQueue.of(name).add(yearStr).add(age)
                                                                                        )))))))))));
    }

    @Override
    protected Environment newEnvironment() {
        if (isTest) {
            final TestConsole testConsole = new TestConsole("John", "1998");
            final TestLog testLog = new TestLog();

            return Environment.of(Console.Service.class, testConsole)
                    .and(Log.Service.class, testLog);
        } else {
            final Logger logger = Logger.getLogger(IoApp.class.getName());
            return Environment.of(Console.Service.class, new Console.Live())
                    .and(Log.Service.class, new Log.Live(logger));
        }
    }

    public static void main(String[] args) {
        final boolean isTest = args.length > 0;
        final TestApp testApp = new TestApp(isTest);
        final Either<Cause<Object>, HQueue> result =
                TestApp.getIoApp().runApp();

        if (isTest) {
            final Environment environment = testApp.getEnvironment();
            final TestConsole testConsole =
                    (TestConsole) environment.get(Console.Service.class);
            final TestLog testLog =
                    (TestLog) environment.get(Log.Service.class);
            System.out.println("Console:\n" + testConsole.getOutputs());
            System.out.println("Log:\n" + testLog.getOutputs());
        }

        System.out.println("Result:\n" + result);
    }
}
