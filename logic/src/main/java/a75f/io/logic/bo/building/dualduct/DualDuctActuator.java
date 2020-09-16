package a75f.io.logic.bo.building.dualduct;

public enum DualDuctActuator {
    
    NOT_USED(0),
    COMPOSITE(1),
    COOLING(2),
    HEATING(3);
    
    private final int val;
    
    private DualDuctActuator(int val) {
        this.val = val;
    }
    
    public int getVal() {
        return val;
    }
}
