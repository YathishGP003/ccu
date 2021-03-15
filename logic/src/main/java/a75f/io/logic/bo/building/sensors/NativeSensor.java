package a75f.io.logic.bo.building.sensors;

import java.util.ArrayList;

/**
 * NativeSensor class define a sensor that is on-board , there measurements are received as engineering value
 * unlike the external sensor which gives voltages.
 */
public class NativeSensor {
    
    public String sensorName;// 'Generic 0-10V'
    public String engineeringUnit;// "V"
    public double minEngineeringValue;//: 0
    public double maxEngineeringValue;//: 10
    public double incrementEngineeringValue;//: 0.1
    
    public SensorType sensorType;
    public static ArrayList<Sensor> sensors = null;
    
    public NativeSensor(String name, String unit, double minEngVal, double maxEngVal, double incVal, SensorType type ) {
        this.sensorName = name;
        this.engineeringUnit = unit;
        this.minEngineeringValue = minEngVal;
        this.maxEngineeringValue = maxEngVal;
        this.incrementEngineeringValue = incVal;
        this.sensorType = type;
    }
    
    
    public boolean equals(Object o) {
        if(!(o instanceof Sensor)) {
            return false;
        }
        return ((Sensor)o).sensorName.equals(this.sensorName);
    }
}
