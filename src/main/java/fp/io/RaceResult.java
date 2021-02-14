package fp.io;

public class RaceResult<F, R1, R2> {
    private final Fiber<F, R1> fiberFirst;
    private final Fiber<F, R2> fiberSecond;
    private final boolean isFirstTheWinner;

    public RaceResult(
        final Fiber<F, R1> fiberFirst,
        final Fiber<F, R2> fiberSecond,
        final boolean isFirstTheWinner
    ) {
        this.fiberFirst = fiberFirst;
        this.fiberSecond = fiberSecond;
        this.isFirstTheWinner = isFirstTheWinner;
    }

    @SuppressWarnings("unchecked")
    public <R> Fiber<F, R> getLooser() {
        return (Fiber<F, R>) (!isFirstTheWinner ? fiberFirst : fiberSecond);
    }

    @SuppressWarnings("unchecked")
    public <R> Fiber<F, R> getWinner() {
        return (Fiber<F, R>) (isFirstTheWinner ? fiberFirst : fiberSecond);
    }
}
