package a75f.io.logic.bo.building.sensors;

import java.util.ArrayList;

public class SensorManager {
    
    private static SensorManager instance = null;
    
    public static ArrayList<OnboardSensor> onBoardSensors = null;
    
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
     * Onboard sensors are defined as per the above spec.
     */
    
    public ArrayList<OnboardSensor> getOnboardSensorList() {
        if (onBoardSensors != null) {
            return onBoardSensors;
        }
        onBoardSensors = new ArrayList<>();
    
        onBoardSensors.add(new OnboardSensor("Current Temp","F",-10,150,0.5));
        onBoardSensors.add(new OnboardSensor("Humidity","%",0,100,0.5));
        onBoardSensors.add(new OnboardSensor("CO2","ppm",0,2000,100));
        onBoardSensors.add(new OnboardSensor("CO","ppm",0,100,1));
        onBoardSensors.add(new OnboardSensor("NO2","ppm",0,5,0.1));
        onBoardSensors.add(new OnboardSensor("VOC","ppb",0,60000,1000));
        onBoardSensors.add(new OnboardSensor("Pressure Sensor","inches wc",0,2,0.1));
        onBoardSensors.add(new OnboardSensor("Sound","dB",0,140,1));
        onBoardSensors.add(new OnboardSensor("Native Occupancy","",0,1,1));
        onBoardSensors.add(new OnboardSensor("Illuminance","lux",0,2000,10));
        onBoardSensors.add(new OnboardSensor("Co2 Equivalent","ppm",400,60000,10));
        onBoardSensors.add(new OnboardSensor("UVIndex","",0,10,0.1));
        onBoardSensors.add(new OnboardSensor("PM2P5","ug/m3",0,1000,10));
        onBoardSensors.add(new OnboardSensor("PM10","ug/m3",0,1000,10));
        
        return onBoardSensors;
    }
}
