package a75f.io.logic.bo.building.sensors;

import java.util.ArrayList;

public class OnboardSensor {
    
    public String sensorName;// 'Generic 0-10V'
    public String engineeringUnit;// "V"
    public double minEngineeringValue;//: 0
    public double maxEngineeringValue;//: 10
    public double incrementEgineeringValue;//: 0.1
    
    public static ArrayList<Sensor> sensors = null;
    
    public OnboardSensor(String name, String unit, double minEngVal, double maxEngVal, double incVal ) {
        this.sensorName = name;
        this.engineeringUnit = unit;
        this.minEngineeringValue = minEngVal;
        this.maxEngineeringValue = maxEngVal;
        this.incrementEgineeringValue = incVal;
    }
    
    
    public boolean equals(Object o) {
        if(!(o instanceof Sensor)) {
            return false;
        }
        return ((Sensor)o).sensorName.equals(this.sensorName);
    }
}
