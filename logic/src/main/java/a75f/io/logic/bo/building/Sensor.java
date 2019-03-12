package a75f.io.logic.bo.building;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by samjithsadasivan on 2/28/19.
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
    
    
    
    public static List<Sensor> getSensorList() {
        ArrayList<Sensor> sensors = new ArrayList<>();
        
        sensors.add(new Sensor("Generic 0-10V","V", 0, 10,0,10,0.1));
        sensors.add(new Sensor("0-2 in. Pressure Sensor","inches wc.",0,10,0,2,0.1));
        sensors.add(new Sensor("humidity","%",0,10,0,100,1.0));
        sensors.add(new Sensor("co2","ppm",0,10,0,2000,10.0));
        sensors.add(new Sensor("co","ppm",0,10,0,2000,10.0));
    
        return sensors;
    }
}
