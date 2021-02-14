package fp.util;

@FunctionalInterface
public interface ThrowingSupplier<T, E extends Throwable> {
    T get() throws E;
}
