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
    public double incrementEgineeringValue;//: 1
    
    //TODO- Table
    public Thermistor(String name, String unit, String table, double incVal) {
        this.sensorName = name;
        this.engineeringUnit = unit;
        this.mappingTable = table;
        this.incrementEgineeringValue = incVal;
        
    }
    
    public static List<Thermistor> getThermistorList() {
        ArrayList<Thermistor> thermistors = new ArrayList<>();
        thermistors.add(new Thermistor("10K type 2 probe","\u00B0F","20K probe lookup table",1));
        thermistors.add(new Thermistor("Generic 1-100kohms","\u00B0F","20K probe lookup table",1));
        
        return thermistors;
    }
}
