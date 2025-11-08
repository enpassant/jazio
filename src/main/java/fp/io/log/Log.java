package fp.io.log;

import fp.io.IO;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {
    private Log() {
    }

    public static interface Service {
        IO<Object, Void> error(String message, Object... params);
        IO<Object, Void> debug(String message, Object... params);
        IO<Object, Void> info(String message, Object... params);
        IO<Object, Void> trace(String message, Object... params);
        IO<Object, Void> warning(String message, Object... params);
    }

    public static class Live implements Service {
        private final Logger logger;

        public Live(final Logger logger) {
            this.logger = logger;
        }

        private IO<Object, Void> log(
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
        public IO<Object, Void> error(String message, Object... params) {
            return log(Level.SEVERE, message, params);
        }
        @Override
        public IO<Object, Void> debug(String message, Object... params) {
            return log(Level.FINEST, message, params);
        }
        @Override
        public IO<Object, Void> info(String message, Object... params) {
            return log(Level.INFO, message, params);
        }
        @Override
        public IO<Object, Void> trace(String message, Object... params) {
            return log(Level.INFO, message, params);
        }
        @Override
        public IO<Object, Void> warning(String message, Object... params) {
            return log(Level.WARNING, message, params);
        }
    }

    public static IO<Object, Void> error(String message, Object... params) {
        return IO.accessM(Service.class, env -> env.error(message, params));
    }

    public static IO<Object, Void> debug(String message, Object... params) {
        return IO.accessM(Service.class, env -> env.debug(message, params));
    }

    public static IO<Object, Void> info(String message, Object... params) {
        return IO.accessM(Service.class, env -> env.info(message, params));
    }

    public static IO<Object, Void> trace(String message, Object... params) {
        return IO.accessM(Service.class, env -> env.info(message, params));
    }

    public static IO<Object, Void> warning(String message, Object... params) {
        return IO.accessM(Service.class, env -> env.warning(message, params));
    }
}
