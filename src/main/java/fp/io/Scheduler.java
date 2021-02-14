package fp.io;

public abstract class Scheduler {
    public abstract State getState();
    public abstract Scheduler updateState();

    public static interface State {}

    public class End implements State {}
    public class Execution implements State {}
    public class Delay implements State {
        public final long nanoSecond;

        public Delay(long nanoSecond) {
            this.nanoSecond = nanoSecond;
        }
    }

    public static class Counter extends Scheduler {
        private final int count;

        public Counter(int count) {
            this.count = count;
        }

        @Override
        public State getState() {
            return count <= 0 ? new End() : new Execution();
        }

        @Override
        public Scheduler updateState() {
            return new Counter(count - 1);
        }
    }

    public static class Delayer extends Scheduler {
        private final long nanoseconds;

        public Delayer(long nanoseconds) {
            this.nanoseconds = nanoseconds;
        }

        @Override
        public State getState() {
            return nanoseconds <= 0 ? new End() : new Delay(nanoseconds);
        }

        @Override
        public Scheduler updateState() {
            return new Delayer(0);
        }
    }
}
