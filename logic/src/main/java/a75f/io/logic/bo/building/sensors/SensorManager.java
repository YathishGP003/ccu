package a75f.io.logic.bo.building.sensors;

import java.util.ArrayList;
import java.util.List;

/**
 * Class the provides singleton access to all all the supported sensor types.
 * SensorList is iterated every minute to parse inputs or update measurements received. The list created once and
 * maintained through the app-life to avoid them getting recreated every time.
 *
 * There are too many magic numbers here.
 * The values are directly copied from 75F spec for sensors. Any changes to spec will need updating this file.
 */
public class SensorManager {
    
    private static SensorManager instance = null;
    
    public ArrayList<NativeSensor> nativeSensors = null;
    public ArrayList<Sensor> externalSensors = null;
    
    private SensorManager() {
    }
    
    public static SensorManager getInstance() {
        if (instance == null) {
            synchronized(SensorManager.class) {
                if (instance == null) {
                    instance = new SensorManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * sensorName: 'Current Temp'
     * engineeringUnit: "F/C"
     * minEngineeringValue: -10
     * maxEngineeringValue: 150
     * incrementEgineeringValue: 0.5
     
     * sensorName: 'Humidity'
     * engineeringUnit: "%"
     * minEngineeringValue: 0
     * maxEngineeringValue: 100
     * incrementEgineeringValue: 1.0
     *
     * sensorName: 'CO2'
     * engineeringUnit: "ppm"
     * minEngineeringValue: 0
     * maxEngineeringValue: 2000
     * incrementEngineeringValue: 100
     *
     * sensorName: 'CO'
     * engineeringUnit: "ppm"
     * minEngineeringValue: 0
     * maxEngineeringValue: 100
     * incrementEngineeringValue: 1.0
     *
     * sensorName: 'NO2'
     * engineeringUnit: "ppm"
     * minEngineeringValue: 0
     * maxEngineeringValue: 5
     * incrementEngineeringValue: 0.1
     *
     * sensorName: 'VOC'
     * engineeringUnit: "ppb"
     * minEngineeringValue: 0
     * maxEngineeringValue: 60000
     * incrementEngineeringValue: 1000
     *
     * sensorName: 'Pressure Sensor' [CCU to do conversion for Pressure: from pascals to inch wc]
     * engineeringUnit: "inches wc."
     * minEngineeringValue: 0
     * maxEngineeringValue: 2
     * incrementEgineeringValue: 0.1
     *
     * sensorName: 'Sound'
     * engineeringUnit: "dB."
     * minEngineeringValue: 0
     * maxEngineeringValue: 140
     * incrementEgineeringValue: 1
     *
     * sensorName: 'Native Occupancy'
     * engineeringUnit: ""
     * minEngineeringValue: 0
     * maxEngineeringValue: 1
     * incrementEgineeringValue:
     *
     * sensorName: 'Illuminance'
     * engineeringUnit: "lux"
     * minEngineeringValue: 0
     * maxEngineeringValue: 2000
     * incrementEgineeringValue: 10
     *
     * sensorName: 'Co2 Equivalent'
     * engineeringUnit: "ppm"
     * minEngineeringValue: 400
     * maxEngineeringValue: 60000
     * incrementEgineeringValue: 10
     *
     * sensorName: 'UVIndex'
     * engineeringUnit: ""
     * minEngineeringValue: 0
     * maxEngineeringValue: 10
     * incrementEgineeringValue: 0.1
     *
     * sensorName: 'PM2P5'
     * engineeringUnit: "ug/m3"
     * minEngineeringValue: 0
     * maxEngineeringValue: 1000
     * incrementEgineeringValue: 10
     *
     * sensorName: 'PM10'
     * engineeringUnit: "ug/m3"
     * minEngineeringValue: 0
     * maxEngineeringValue: 1000
     * incrementEgineeringValue: 10
     *
     *
     *
     * Native sensors are defined as per the above spec.
     */
    
    public ArrayList<NativeSensor> getNativeSensorList() {
        if (nativeSensors != null) {
            return nativeSensors;
        }
        nativeSensors = new ArrayList<>();
    
        nativeSensors.add(new NativeSensor("Native-Temperature", "\u00B0F", -10, 150, 0.5, SensorType.TEMPERATURE));
        nativeSensors.add(new NativeSensor("Native-Humidity", "%", 0, 100, 1, SensorType.HUMIDITY));
        nativeSensors.add(new NativeSensor("Native-CO2", "ppm", 0, 2000, 100, SensorType.CO2));
        nativeSensors.add(new NativeSensor("Native-CO", "ppm", 0, 100, 1, SensorType.CO));
        nativeSensors.add(new NativeSensor("Native-NO", "ppm", 0, 5, 0.1, SensorType.NO));
        nativeSensors.add(new NativeSensor("Native-VOC", "ppb", 0, 60000, 1000, SensorType.VOC));
        nativeSensors.add(new NativeSensor("Native-Pressure", "inches wc", 0, 2, 0.1, SensorType.PRESSURE));
        nativeSensors.add(new NativeSensor("Native-Sound", "dB", 0, 140, 1, SensorType.SOUND));
        nativeSensors.add(new NativeSensor("Native-Occupancy", "", 0, 1, 1, SensorType.OCCUPANCY));
        nativeSensors.add(new NativeSensor("Native-Illuminance", "lux", 0, 2000, 10, SensorType.ILLUMINANCE));
        nativeSensors.add(new NativeSensor("Native-Co2 Equivalent", "ppm", 400, 60000, 10, SensorType.CO2_EQUIVALENT));
        nativeSensors.add(new NativeSensor("Native-UVIndex", "", 0, 10, 0.1, SensorType.UVI));
        nativeSensors.add(new NativeSensor("Native-PM2P5", "ug/\u33A5", 0, 1000, 10, SensorType.PM2P5));
        nativeSensors.add(new NativeSensor("Native-PM10", "ug/\u33A5", 0, 1000, 10, SensorType.PM10));
        
        return nativeSensors;
    }

    /**
     * @return existing sensor details along with keycard and door window sensor at last position
     * Following function adds additional Keycard and door window sensor to existing external sensor list
     * Some of the profile are using these external sensors list while configuring the profile and the important
     * thing is when any of the Thermistor or Analog inputs are mapped to following sensor we will bind (type) that
     * input with position of the sensor in the external sensor list. ie Index of the particular Sensor in the
     * external sensor list. reference  a75f.io.logic.bo.building.sensors.SensorManager#getExternalSensorList() .
     * When we receive the data for particular sensor we will identify the type of the sensor by checking with index
     * which is present in the type of the sensor. Some of the profile won't allow the keycard and door window
     * sensors. So we have created following function which can be used to access where door window and keycard sensors
     * are suppose to use. Ex Hyperstat CPU profile has these two sensor usage
     */
    public List<Sensor> getAdditionalWithExternalSensorList() {

        List<Sensor> externalSensors = new ArrayList<>(getExternalSensorList());
        externalSensors.add(new Sensor("Key Card Sensor","",0,10,0,1,1));
        externalSensors.add(new Sensor("Door Window Sensor","ohms",0,10,0,10000,100));
        return externalSensors;
    }
    
    public List<Sensor> getExternalSensorList() {
        if (externalSensors != null) {
            return externalSensors;
        }
        externalSensors = new ArrayList<>();
    
        externalSensors.add(new Sensor("Generic 0-10","V", 0, 10,0,10,0.1));
        externalSensors.add(new Sensor("0-2 in. Pressure Sensor","inches wc",0,10,0,2,0.1));
        externalSensors.add(new Sensor("0-0.25 Differential Pressure Sensor","inches wc",0,10,-0.25,0.25,0.01));
        externalSensors.add(new Sensor("Airflow Sensor","CFM",0,10,0,1000,10.0));
        externalSensors.add(new Sensor("Humidity","%",0,10,0,100,1.0));
        externalSensors.add(new Sensor("CO2","ppm",0,10,0,2000,10.0));
        externalSensors.add(new Sensor("CO","ppm",0,10,0,100,1.0));
        externalSensors.add(new Sensor("NO2","ppm",0,10,0,5,0.1));
        externalSensors.add(new Sensor("CT 0-10","amps",0,10,0,10,0.1));
        externalSensors.add(new Sensor("CT 0-20","amps",0,10,0,20,0.1));
        externalSensors.add(new Sensor("CT 0-50","amps",0,10,0,50,0.1));
        externalSensors.add(new Sensor("ION Meter 0-1 Million","ions/cc",0,10,0,1000000,1000));
        return externalSensors;
    }
}
