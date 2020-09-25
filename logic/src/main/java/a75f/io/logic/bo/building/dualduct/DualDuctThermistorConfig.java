package a75f.io.logic.bo.building.dualduct;

public enum DualDuctThermistorConfig {
    COOLING_AIRFLOW_TEMP(0),
    HEATING_AIRFLOW_TEMP(1);
    
    private final int val;
    
    DualDuctThermistorConfig(int val) {
        this.val = val;
    }
    
    public int getVal() {
        return val;
    }
}
