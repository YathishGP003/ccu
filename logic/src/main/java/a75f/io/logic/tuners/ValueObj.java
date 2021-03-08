package a75f.io.logic.tuners;

/**
 * Created by mahesh on 19-02-2021.
 */
class ValueObj {

    String minVal, maxVal, incVal;

    public ValueObj(String minVal, String maxVal, String incVal) {
        this.minVal = minVal;
        this.maxVal = maxVal;
        this.incVal = incVal;
    }

    public String getIncVal() {
        return incVal;
    }

    public String getMaxVal() {
        return maxVal;
    }

    public String getMinVal() {
        return minVal;
    }

    public void setIncVal(String incVal) {
        this.incVal = incVal;
    }

    public void setMaxVal(String maxVal) {
        this.maxVal = maxVal;
    }

    public void setMinVal(String minVal) {
        this.minVal = minVal;
    }
}
