package fp.io;

import fp.util.HMap;
import fp.util.Tuple2;
import java.util.Optional;

public class Environment {
    private HMap map = HMap.empty();

    private Environment() {
    }

    public static <T> Environment of(Class<T> key, T value) {
        final Environment environment = new Environment();
        return environment.and(key, value);
    }

    public <T> Environment and(Class<T> key, T value) {
        map = map.add(key.getName(), value);
        return this;
    }

    public <T> T get(Class<T> key) {
        final Optional<T> valueOpt = (Optional<T>) map.getValue(key.getName());
        if (valueOpt.isEmpty()) {
            throw new IllegalArgumentException("Missing environment: " + key);
        } else {
            return valueOpt.get();
        }
    }

    public HMap getMap() {
        return map;
    }

    public <F, R> IO<F, R> provides(final IO<F, R> io) {
        return map.allValues()
                .<Tuple2<String, Object>>valueStream()
                .reduce(
                        io,
                        ((frio, entry) ->
                                frio.provide(entry.getFirst(), Object.class, entry.getSecond())
                        ),
                        ((frio, frio2) -> frio)
                );
    }
}
