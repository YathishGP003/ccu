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
    
    public static ArrayList<Sensor> sensors = null;
    
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
        if (sensors != null) {
            return sensors;
        }
        sensors = new ArrayList<>();
        
        sensors.add(new Sensor("Generic 0-10","V", 0, 10,0,10,0.1));
        sensors.add(new Sensor("0-2 in. Pressure Sensor","inches wc",0,10,0,2,0.1));
        sensors.add(new Sensor("0-0.25 Differential Pressure Sensor","inches wc",0,10,-0.25,0.25,0.01));
        sensors.add(new Sensor("Airflow Sensor","%",0,10,0,1000,10.0));
        sensors.add(new Sensor("Humidity","%",0,10,0,100,1.0));
        sensors.add(new Sensor("CO2","ppm",0,10,0,2000,10.0));
        sensors.add(new Sensor("CO","ppm",0,10,0,100,1.0));
        sensors.add(new Sensor("NO2","ppm",0,10,0,5,0.1));
        sensors.add(new Sensor("CT 0-10 Amps","amps",0,10,0,10,0.1));
        sensors.add(new Sensor("CT 0-20 Amps","amps",0,10,0,20,0.1));
        sensors.add(new Sensor("CT 0-50 Amps","amps",0,10,0,50,0.1));
        return sensors;
    }
    
    public int getSensorIndex(String name) {
        for (Sensor s : getSensorList()) {
            if (s.sensorName.contains(name)) {
                return sensors.indexOf(s);
            }
        }
        return -1;
    }
    
    public boolean equals(Object o) {
        if(!(o instanceof Sensor))
        {
            return false;
        }
        return ((Sensor)o).sensorName.equals(this.sensorName);
    }
}
