package fp.util;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class GeneralFailure<F> implements Failure {
    public static final String EXCEPTION = "EXCEPTION";
    protected final F code;
    protected final Map<String, Object> params;

    private GeneralFailure(F code, Map<String, Object> params) {
        this.code = code;
        this.params = params;
    }

    public static <F> GeneralFailure<F> of(F code) {
        return new GeneralFailure<>(code, new HashMap<>());
    }

    public static <F> GeneralFailure<F> of(F code, String key, Object value) {
        Map<String, Object> params = new HashMap<>();
        params.put(key, value);
        return new GeneralFailure<>(code, params);
    }

    @SafeVarargs
    public static <F> GeneralFailure<F> of(F code, Tuple2<String, Object>... tuples) {
        return new GeneralFailure<>(code, Tuple2.toMap(tuples));
    }

    public F getCode() {
        return code;
    }

    public F getValue() {
        return getCode();
    }

    public Set<String> getParamNames() {
        return params.keySet();
    }

    @SuppressWarnings("unchecked")
    public <T> T getParamValue(String paramName) {
        return (T) params.get(paramName);
    }

    public String format(String pattern) {
        return MessageFormat.format(
            pattern,
            params.values().toArray(new Object[0])
        );
    }

    @Override
    public String toString() {
        final Optional<String> paramStrOpt = params.entrySet()
            .stream()
            .map(entry -> entry.getKey() + " -> " + entry.getValue())
            .reduce((s1, s2) -> s1 + ", " + s2);
        final String paramStr = paramStrOpt.map(p -> ", " + p).orElse("");

        return "GeneralFailure(" + code + paramStr + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof GeneralFailure) {
            @SuppressWarnings("unchecked")
            GeneralFailure<F> failure = (GeneralFailure<F>) other;
            return failure.toString().equals(this.toString());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }
}
