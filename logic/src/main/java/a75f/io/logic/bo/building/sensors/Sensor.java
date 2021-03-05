package a75f.io.logic.bo.building.sensors;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by samjithsadasivan on 2/28/19.
 */

/**
 * Defines an external sensor which provided raw voltage output.
 */
public class Sensor
{
    public String sensorName;// 'Generic 0-10V'
    public String engineeringUnit;// "V"
    public double minVoltage; // 0
    public double maxVoltage;//: 10
    public double minEngineeringValue;//: 0
    public double maxEngineeringValue;//: 10
    public double incrementEgineeringValue;//: 0.1
    
    public Sensor(String name, String unit, double minV, double maxV, double minEngVal, double maxEngVal, double incVal ) {
        this.sensorName = name;
        this.engineeringUnit = unit;
        this.minVoltage = minV;
        this.maxVoltage = maxV;
        this.minEngineeringValue = minEngVal;
        this.maxEngineeringValue = maxEngVal;
        this.incrementEgineeringValue = incVal;
    }
    
    public boolean equals(Object o) {
        if(!(o instanceof Sensor))
        {
            return false;
        }
        return ((Sensor)o).sensorName.equals(this.sensorName);
    }
}
