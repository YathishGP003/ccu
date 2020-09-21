package a75f.io.logic.bo.building;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by samjithsadasivan on 2/28/19.
 */

public class Thermistor
{
    public String sensorName;//: '10K type 2 probe'
    public String engineeringUnit;//: "degree F"
    public String mappingTable;//: "20K probe lookup table"
    public double minEngineeringValue;//: -40
    public double maxEngineeringValue;//: 302
    public double incrementEngineeringValue;//: 0.5
    
    //TODO- Table
    public Thermistor(String name, String unit, String table, double minEngVal, double maxEngVal,double incVal) {
        this.sensorName = name;
        this.engineeringUnit = unit;
        this.mappingTable = table;
        this.minEngineeringValue = minEngVal;
        this.maxEngineeringValue = maxEngVal;
        this.incrementEngineeringValue = incVal;
        
    }
    
    public static List<Thermistor> getThermistorList() {
        ArrayList<Thermistor> thermistors = new ArrayList<>();
        thermistors.add(new Thermistor("10K type 2 probe","\u00B0F","20K probe lookup table",-40,302,0.5));
        thermistors.add(new Thermistor("Generic 1-100kohms","\u00B0F","20K probe lookup table",-40,302,0.5));
        
        return thermistors;
    }
}
