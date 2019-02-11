package a75f.io.api.haystack;

public class Occupied {

    private boolean mOccupied;
    private Object mValue;

    public boolean isOccupied() {
        return mOccupied;
    }

    public void setOccupied(boolean occupied) {
        this.mOccupied = occupied;
    }

    public Object getValue() {
        return mValue;
    }

    public void setValue(Object value) {
        this.mValue = value;
    }
}
