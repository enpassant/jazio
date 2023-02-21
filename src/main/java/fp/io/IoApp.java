package fp.io;

import java.util.logging.Logger;

import fp.io.console.Console;
import fp.io.log.Log;
import fp.util.Either;

public abstract class IoApp<F, R> {
    private static IoApp ioApp;
    private Environment environment;
    private Platform platform;
    private Runtime runtime;

    protected IoApp() {
        ioApp = this;
    }

    public static IoApp getIoApp() {
        return ioApp;
    }

    public abstract IO<Environment, F, R> program();

    public Either<Cause<F>, R> runApp() {
        try {
            final Either<Cause<F>, R> result =
                getRuntime().unsafeRun(
                    program().provide(getEnvironment())
                );
            return result;
        } finally {
            getPlatform().shutdown();
        }
    }

    protected Environment newEnvironment() {
        final Logger logger = Logger.getLogger(IoApp.class.getName());
        return Environment.of(Console.Service.class, new Console.Live())
            .and(Log.Service.class, new Log.Live(logger));
    }

    protected Platform newPlatform() {
        return new DefaultPlatform();
    }

    protected Runtime<R> newRuntime() {
        return new DefaultRuntime<R>(null, getPlatform());
    }

    public Environment getEnvironment() {
        if (environment == null) {
            environment = newEnvironment();
        }
        return environment;
    }

    public Platform getPlatform() {
        if (platform == null) {
            platform = newPlatform();
        }
        return platform;
    }

    public Runtime<R> getRuntime() {
        if (runtime == null) {
            runtime = newRuntime();
        }
        return runtime;
    }
}
