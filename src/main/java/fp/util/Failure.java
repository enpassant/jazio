package fp.util;

import java.util.Optional;

public interface Failure {
    <F> F getValue();

    static <E extends Exception, R> Optional<R> tryCatchOptional(
            ThrowingSupplier<R, E> process
    ) {
        try {
            return Optional.of(process.get());
        } catch (Exception e) {
            return ignoreException(e, Optional.empty());
        }
    }

    static <E extends Exception, R> R ignoreException(E e, R r) {
        e.getCause();
        return r;
    }
}
