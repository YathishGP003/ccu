package a75f.io.logic.bo.building.sensors;

import java.util.ArrayList;

/**
 * NativeSensor class defines a sensor that is on-board. These measurements are received as readable engineering values
 * unlike the external sensor which gives raw/analog measurement.
 */
public class NativeSensor {
    
    public String sensorName;
    public String engineeringUnit;
    public double minEngineeringValue;
    public double maxEngineeringValue;
    public double incrementEngineeringValue;
    
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
