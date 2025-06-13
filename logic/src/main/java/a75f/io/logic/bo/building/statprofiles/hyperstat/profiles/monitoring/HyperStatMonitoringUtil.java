package a75f.io.logic.bo.building.statprofiles.hyperstat.profiles.monitoring;

import android.os.Bundle;

import a75f.io.logic.bo.building.definitions.Consts;

/*
 * created by spoorthidev on 20-July-2021
 */

public class HyperStatMonitoringUtil {


    public static Bundle getAnalogBundle(int analog) {
        Bundle bundle = new Bundle();
        String shortDis = "Generic 0:10 Voltage";
        String shortDisTarget = "Dynamic Target Voltage";
        String unit = "V";
        String maxVal = "10";
        String minVal = "0";
        String incrementVal = "0.1";
        String[] markers = null;
        switch (analog) {
            case 0:
                shortDis = "Generic (0:10)V";
                shortDisTarget = "Dynamic Target Voltage";
                unit = "V";
                maxVal = "10";
                minVal = "0";
                incrementVal = "0.1";
                break;
            case 1:
                shortDis = "Pressure Sensor (0:2)inH₂O";
                shortDisTarget = "Dynamic Target Pressure";
                unit = Consts.PRESSURE_UNIT;
                maxVal = "2";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"pressure"};
                break;
            case 2:
                shortDis = "Differential Pressure Sensor (0:0.25)inH₂O";
                shortDisTarget = "Dynamic Target Pressure Differential";
                unit = Consts.PRESSURE_UNIT;
                maxVal = "0.25";
                minVal = "-0.25";
                incrementVal = "0.01";
                markers = new String[]{"pressure"};
                break;
            case 3:
                shortDis = "Airflow Sensor (0:1000)cfm";
                shortDisTarget = "Dynamic Target Airflow";
                unit = "cfm";
                maxVal = "1000";
                minVal = "0";
                incrementVal = "10";
                markers = new String[]{"airflow"};
                break;
            case 4:
                shortDis = "Humidity (0:100)%";
                shortDisTarget = "Dynamic Target Humidity";
                unit = "%";
                maxVal = "100";
                minVal = "0";
                incrementVal = "1.0";
                markers = new String[]{"humidity"};
                break;
            case 5:
                shortDis = "CO2 (0:2000)ppm";
                shortDisTarget = "Dynamic Target CO2 Level";
                unit = "ppm";
                maxVal = "2000";
                minVal = "0";
                incrementVal = "100";
                markers = new String[]{"co2"};
                break;
            case 6:
                shortDis = "CO (0:100)ppm";
                shortDisTarget = "Dynamic Target CO Level";
                unit = "ppm";
                maxVal = "100";
                minVal = "0";
                incrementVal = "1.0";
                markers = new String[]{"co"};
                break;
            case 7:
                shortDis = "NO2 (0:5)ppm";
                shortDisTarget = "Dynamic Target NO2 Level";
                unit = "ppm";
                maxVal = "5";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"no2"};
                break;
            case 8:
                shortDis = "CT (0:10)amps";
                shortDisTarget = "Dynamic Target Current Draw";
                unit = "amps";
                maxVal = "10";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"current", "transformer"};
                break;
            case 9:
                shortDis = "CT (0:20)amps";
                shortDisTarget = "Dynamic Target Current Draw";
                unit = "amps";
                maxVal = "20";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"current", "transformer"};
                break;
            case 10:
                shortDis = "CT (0:50)amps";
                shortDisTarget = "Dynamic Target Current Draw";
                unit = "amps";
                maxVal = "50";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"current", "transformer"};
                break;
            case 11:
                shortDis = "ION Meter (0:1 Million)ions/cc";
                shortDisTarget = "Dynamic Target ION Density";
                unit = "ions/cc";
                maxVal = "10";
                minVal = "0";
                incrementVal = "1000";
                markers = new String[]{"ion", "density"};
                break;
        }

        bundle.putString("shortDis", shortDis);
        bundle.putString("shortDisTarget", shortDisTarget);
        bundle.putString("unit", unit);
        bundle.putString("maxVal", maxVal);
        bundle.putString("minVal", minVal);
        bundle.putString("incrementVal", incrementVal);
        bundle.putStringArray("markers", markers);

        return bundle;
    }

}
