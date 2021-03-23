package a75f.io.logic.bo.building.sensors;

import java.util.ArrayList;
import java.util.List;

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
        nativeSensors.add(new NativeSensor("Native-VOC", "ppb", 0, 60000, 1000, SensorType.VOC));
        nativeSensors.add(new NativeSensor("Native-Pressure", "inches wc", 0, 2, 0.1, SensorType.PRESSURE));
        nativeSensors.add(new NativeSensor("Native-Sound", "dB", 0, 140, 1, SensorType.SOUND));
        nativeSensors.add(new NativeSensor("Native-Occupancy", "", 0, 1, 1, SensorType.OCCUPANCY));
        nativeSensors.add(new NativeSensor("Native-Illuminance", "lux", 0, 2000, 10, SensorType.ILLUMINANCE));
        nativeSensors.add(new NativeSensor("Native-Co2 Equivalent", "ppm", 400, 60000, 10, SensorType.CO2_EQUIVALENT));
        nativeSensors.add(new NativeSensor("Native-UVIndex", "", 0, 10, 0.1, SensorType.UVI));
        nativeSensors.add(new NativeSensor("Native-PM2P5", "ug/m3", 0, 1000, 10, SensorType.PM2P5));
        nativeSensors.add(new NativeSensor("Native-PM10", "ug/m3", 0, 1000, 10, SensorType.PM10));
        
        return nativeSensors;
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
        return externalSensors;
    }
}
