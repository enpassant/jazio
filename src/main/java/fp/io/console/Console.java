package fp.io.console;

import fp.io.IO;
import fp.util.Failure;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Console {
    private Console() {
    }

    public interface Service {
        IO<Object, Void> println(String line);
        IO<Failure, String> readLine();
    }

    public static class Live implements Service {
        public IO<Object, Void> println(String line) {
            return IO.effectTotal(
                () -> System.out.println(line)
            ).blocking();
        }
        public IO<Failure, String> readLine() {
            return IO.effect(() -> {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(System.in)
                );
                return reader.readLine();
            }).blocking();
        }
    }

    public static IO<Object, Void> println(String line) {
        return IO.accessM(Service.class, env -> env.println(line));
    }
    public static IO<Failure, String> readLine() {
        return IO.accessM(Service.class, Service::readLine);
    }
}
