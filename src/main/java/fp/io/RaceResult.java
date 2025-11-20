package fp.io;

public class RaceResult<F, R> {
    private final Fiber<F, R> fiberFirst;
    private final Fiber<F, R> fiberSecond;
    private final boolean isFirstTheWinner;

    public RaceResult(
            final Fiber<F, R> fiberFirst,
            final Fiber<F, R> fiberSecond,
            final boolean isFirstTheWinner
    ) {
        this.fiberFirst = fiberFirst;
        this.fiberSecond = fiberSecond;
        this.isFirstTheWinner = isFirstTheWinner;
    }

    public Fiber<F, R> getLooser() {
        return !isFirstTheWinner ? fiberFirst : fiberSecond;
    }

    public Fiber<F, R> getWinner() {
        return isFirstTheWinner ? fiberFirst : fiberSecond;
    }
}
