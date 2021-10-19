package a75f.io.device.mesh;

import android.util.Log;

import a75f.io.api.haystack.RawPoint;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.sensors.Sensor;
import a75f.io.logic.bo.building.sensors.SensorManager;
import a75f.io.logic.bo.util.CCUUtils;

public class AnalogUtil {
    
    public static Double getAnalogConversion(RawPoint rawPoint, Double val) {
        double analogVal = val/1000;
        Sensor analogSensor;
        //If the analogType of physical point is set to one of the sensor types (Sensor.getSensorList) , corresponding sensor's
        //conversion formula is applied. Otherwise the input value that is already divided by 1000 is just returned.
        try
        {
            int index = (int)Double.parseDouble(rawPoint.getType());
            analogSensor = SensorManager.getInstance().getAdditionalWithExternalSensorList().get(index);
        }catch (NumberFormatException e) {
            e.printStackTrace();
            return analogVal;
        }
        double analogConversion = analogSensor.minEngineeringValue +
                                  (analogSensor.maxEngineeringValue- analogSensor.minEngineeringValue) * analogVal / (analogSensor.maxVoltage - analogSensor.minVoltage);
        Log.d(L.TAG_CCU_DEVICE, "Sensor input : analogConversion " + rawPoint.getType() + " val " + analogConversion);
        return CCUUtils.roundToTwoDecimal(analogConversion);

        
    }
}
