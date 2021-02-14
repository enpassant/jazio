package fp.io;

import java.lang.IllegalArgumentException;
import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<Class<?>, Object> map = new HashMap<>();

    private Environment() {
    }

    public static <T> Environment of(Class<T> key, T value) {
        final Environment environment = new Environment();
        environment.map.put(key, value);
        return environment;
    }

    public <T> Environment and(Class<T> key, T value) {
        map.put(key, value);
        return this;
    }

    public <T> T get(Class<T> key) {
        final T value = (T) map.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing environment: " + key);
        }
        return value;
    }
}
