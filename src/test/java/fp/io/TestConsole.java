package fp.io;

import fp.util.Failure;

public class TestConsole implements Console.Service {
    private final StringBuilder sb = new StringBuilder();
    private final String[] inputs;
    private int inputIndex = 0;

    public TestConsole(final String... inputs) {
        this.inputs = inputs;
    }

    @Override
    public IO<Object, Object, Void> println(String line) {
        return IO.effectTotal(() -> { sb.append(line).append("\n"); });
    }
    @Override
    public IO<Object, Failure, String> readLine() {
        return IO.effect(() -> inputs[inputIndex++]);
    }
    public String getOutputs() {
        return sb.toString();
    }
}
