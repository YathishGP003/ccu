package a75f.io.api.haystack;

public class Occupied {

    private boolean mOccupied;
    private Object mValue;
    private Double mCoolingVal;
    private Double mHeatingVal;

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

    public Double getHeatingVal() {
        return mHeatingVal;
    }

    public void setHeatingVal(Double heatingVal) {
        this.mHeatingVal = heatingVal;
    }

    public Double getCoolingVal() {
        return mCoolingVal;
    }

    public void setCoolingVal(Double coolingVal) {
        this.mCoolingVal = coolingVal;
    }
}
