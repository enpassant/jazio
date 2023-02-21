package fp.io.console;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import fp.io.Environment;
import fp.io.IO;
import fp.util.Failure;

public class Console {
    private Console() {
    }

    public static interface Service {
        IO<Object, Object, Void> println(String line);
        IO<Object, Failure, String> readLine();
    }

    public static class Live implements Service {
        public IO<Object, Object, Void> println(String line) {
            return IO.effectTotal(
                () -> System.out.println(line)
            ).blocking();
        }
        public IO<Object, Failure, String> readLine() {
            return IO.effect(() -> {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in)
                );
                return reader.readLine();
            }).blocking();
        }
    }

    public static IO<Environment, Object, Void> println(String line) {
        return IO.accessM(env -> env.get(Service.class).println(line));
    }
    public static IO<Environment, Failure, String> readLine() {
        return IO.accessM(env -> env.get(Service.class).readLine());
    }
}
