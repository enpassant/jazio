package fp.io;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {
    private Log() {
    }

    public static interface Service {
        IO<Object, Object, Void> error(String message, Object... params);
        IO<Object, Object, Void> debug(String message, Object... params);
        IO<Object, Object, Void> info(String message, Object... params);
        IO<Object, Object, Void> warning(String message, Object... params);
    }

    public static class Live implements Service {
        private final Logger logger;

        public Live(final Logger logger) {
            this.logger = logger;
        }

        private IO<Object, Object, Void> log(
            Level level,
            String message,
            Object... params
        ) {
            if (logger.isLoggable(level)) {
                return IO.effectTotal(
                    () -> logger.log(level, message, params)
                ).blocking();
            } else {
                return IO.unit();
            }
        }
        @Override
        public IO<Object, Object, Void> error(String message, Object... params) {
            return log(Level.SEVERE, message, params);
        }
        @Override
        public IO<Object, Object, Void> debug(String message, Object... params) {
            return log(Level.FINEST, message, params);
        }
        @Override
        public IO<Object, Object, Void> info(String message, Object... params) {
            return log(Level.INFO, message, params);
        }
        @Override
        public IO<Object, Object, Void> warning(String message, Object... params) {
            return log(Level.WARNING, message, params);
        }
    }

    public static IO<Environment, Object, Void> error(String message, Object... params) {
        return IO.accessM(env -> env.get(Service.class).error(message, params));
    }

    public static IO<Environment, Object, Void> debug(String message, Object... params) {
        return IO.accessM(env -> env.get(Service.class).debug(message, params));
    }

    public static IO<Environment, Object, Void> info(String message, Object... params) {
        return IO.accessM(env -> env.get(Service.class).info(message, params));
    }

    public static IO<Environment, Object, Void> warning(String message, Object... params) {
        return IO.accessM(env -> env.get(Service.class).warning(message, params));
    }
}
