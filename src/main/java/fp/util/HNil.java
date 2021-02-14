package fp.util;

public class HNil implements HList {
    @Override
    public <R> HItem<R, HNil> add(R value) {
        return HList.of(value);
    }

    public HNil get() {
        return this;
    }

    @Override
    public HList tail() {
        return this;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof HNil);
    }

    @Override
    public String toString() {
        return "HNil";
    }
}
