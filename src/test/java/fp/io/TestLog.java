package fp.io;

import java.text.MessageFormat;

public class TestLog implements Log.Service {
    private final StringBuilder sb = new StringBuilder();

    private IO<Object, Object, Void> log(
        String level,
        String message,
        Object... params
    ) {
        return IO.effectTotal(() -> {
            sb.append("[" + level + "] ")
                .append(MessageFormat.format(message, params))
                .append("\n");
        });
    }
    @Override
    public IO<Object, Object, Void> error(String message, Object... params) {
        return log("Error", message, params);
    }
    @Override
    public IO<Object, Object, Void> debug(String message, Object... params) {
        return log("Debug", message, params);
    }
    @Override
    public IO<Object, Object, Void> info(String message, Object... params) {
        return log("Info", message, params);
    }
    @Override
    public IO<Object, Object, Void> trace(String message, Object... params) {
        return log("Info", message, params);
    }
    @Override
    public IO<Object, Object, Void> warning(String message, Object... params) {
        return log("Warning", message, params);
    }

    public String getOutputs() {
        return sb.toString();
    }
}
