package fp.util;

@FunctionalInterface
public interface ThrowingStatement<E extends Throwable> {
    void call() throws E;
}
