package a75f.io.logic.bo.building.hyperstatsense;

import android.util.Log;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.Thermistor;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.sensors.Sensor;
import a75f.io.logic.bo.building.sensors.SensorManager;

/**
 * Created by spoorthidev on 18/06/2021
 */

public class HyperStatSenseEquip {

    private static final String LOG_TAG = "HyperStatSenseEquip";
    public int mNodeAddr;
    ProfileType mProfileType;
    String mEquipRef = null;
    CCUHsApi mHayStack = CCUHsApi.getInstance();


    public HyperStatSenseEquip(ProfileType type, int node) {
        mNodeAddr = node;
        mProfileType = type;
    }

    public void init() {
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \"" + mNodeAddr + "\"");

        if (equip.isEmpty()) {
            Log.e(LOG_TAG, "Init Failed : Equip does not exist ");
            return;
        }
        mEquipRef = equip.get("id").toString();

    }

    public void createEntities(HyperStatSenseConfiguration config, String floorRef, String roomRef) {
        Log.d(LOG_TAG, "createEntities ++");
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-HSSENSE-" + mNodeAddr;
        String ahuRef = null;
        Sensor sensordata;
        Thermistor thermistordata;

        Equip.Builder b = new Equip.Builder()
                .setSiteRef(siteRef)
                .setDisplayName(equipDis)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setProfile(mProfileType.name())
                .setPriority(config.getPriority().name())
                .addMarker("equip").addMarker("hyperstatsense").addMarker("zone")
                .setAhuRef(ahuRef)
                .setTz(tz)
                .setGroup(String.valueOf(mNodeAddr));
        mEquipRef = CCUHsApi.getInstance().addEquip(b.build());

        Point temperatureOffset = new Point.Builder()
                .setDisplayName(equipDis+"-temperatureOffset")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .addMarker("config").addMarker("writable").addMarker("zone")
                .addMarker("temperature").addMarker("offset").addMarker("hyperstatsense")
                .setGroup(String.valueOf(mNodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String tempoffsetId = mHayStack.addPoint(temperatureOffset);
        mHayStack.writeDefaultValById(tempoffsetId, 0.0);


            Point.Builder analog1InputSensor = new Point.Builder()
                    .setDisplayName(equipDis + "-analog1InputSensor")
                    .setEquipRef(mEquipRef)
                    .setSiteRef(siteRef)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef)
                    .setShortDis("Analog1 Input Config")
                    .addMarker("config").addMarker("zone").addMarker("writable")
                    .addMarker("analog1").addMarker("input").addMarker("sensor")
                    .addMarker("hyperstatsense")
                    .setGroup(String.valueOf(mNodeAddr))
                    .setTz(tz);

            if(config.isAnalog1Enable){
                sensordata = SensorManager.getInstance().getExternalSensorList()
                        .get(config.analog2Sensor);
                analog1InputSensor.addMarker(sensordata.sensorName)
                        .setUnit(sensordata.engineeringUnit)
                        .setMinVal(String.valueOf(sensordata.minEngineeringValue))
                        .setMaxVal(String.valueOf(sensordata.minEngineeringValue));
            }
            analog1InputSensor.build();
            String analog1InputSensorId = mHayStack.addPoint(analog1InputSensor);
            mHayStack.writeDefaultValById(analog1InputSensorId, 0.0);


            mHayStack.writeDefaultVal("point and config and analog1relay and enabled and equipRef == \"" + mEquipRef + "\"",
                    config.isAnalog1Enable ? 1.0 : 0);


        if (config.isAnalog2Enable) {
            sensordata = SensorManager.getInstance().getExternalSensorList()
                    .get(config.analog2Sensor);
            Point analog2InputSensor = new Point.Builder()
                    .setDisplayName(equipDis + "-analog2InputSensor")
                    .setEquipRef(mEquipRef)
                    .setSiteRef(siteRef)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef)
                    .setShortDis("Analog2 Input Config")
                    .addMarker("config").addMarker("zone").addMarker("writable")
                    .addMarker("analog2").addMarker("input").addMarker("sensor")
                    .addMarker("hyperstatsense").addMarker(sensordata.sensorName)
                    .addMarker(sensordata.engineeringUnit)
                    .addMarker(String.valueOf(sensordata.incrementEgineeringValue))
                    .addMarker(String.valueOf(sensordata.minEngineeringValue))
                    .addMarker(String.valueOf(sensordata.maxEngineeringValue))
                    .addMarker(String.valueOf(sensordata.minVoltage))
                    .addMarker(String.valueOf(sensordata.maxVoltage))
                    .setGroup(String.valueOf(mNodeAddr))
                    .setTz(tz)
                    .build();
            String analog2InputSensorId = mHayStack.addPoint(analog2InputSensor);
            mHayStack.writeDefaultValById(analog2InputSensorId, 0.0);

            mHayStack.writeDefaultVal("point and config and analog2relay and enabled and equipRef == \"" + mEquipRef + "\"",
                    config.isAnalog2Enable ? 1.0 : 0);

        }

        if (config.isTh1Enable) {
            thermistordata = Thermistor.getThermistorList().get(config.th1Sensor);
            Point th1InputSensor = new Point.Builder()
                    .setDisplayName(equipDis + "-th1InputSensor")
                    .setEquipRef(mEquipRef)
                    .setSiteRef(siteRef)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef)
                    .addMarker("config").addMarker("zone").addMarker("writable")
                    .addMarker("th1").addMarker("input").addMarker("sensor")
                    .addMarker("hyperstatsense").addMarker(thermistordata.sensorName)
                    .addMarker(thermistordata.engineeringUnit)
                    .addMarker(String.valueOf(thermistordata.incrementEngineeringValue))
                    .addMarker(String.valueOf(thermistordata.minEngineeringValue))
                    .addMarker(String.valueOf(thermistordata.maxEngineeringValue))
                    .setGroup(String.valueOf(mNodeAddr))
                    .setTz(tz)
                    .build();
            String th1InputSensorId = mHayStack.addPoint(th1InputSensor);
            mHayStack.writeDefaultValById(th1InputSensorId, 0.0);

            mHayStack.writeDefaultVal("point and config and th1relay and enabled and equipRef == \"" + mEquipRef + "\"",
                    config.isTh1Enable ? 1.0 : 0);
        }

        if (config.isTh2Enable) {
            thermistordata = Thermistor.getThermistorList().get(config.th2Sensor);
            Point th2InputSensor = new Point.Builder()
                    .setDisplayName(equipDis + "-th2InputSensor")
                    .setEquipRef(mEquipRef)
                    .setSiteRef(siteRef)
                    .setRoomRef(roomRef)
                    .setFloorRef(floorRef)
                    .addMarker("config").addMarker("zone").addMarker("writable")
                    .addMarker("th2").addMarker("input").addMarker("sensor")
                    .addMarker("hyperstatsense").addMarker(thermistordata.sensorName)
                    .addMarker(thermistordata.engineeringUnit)
                    .addMarker(String.valueOf(thermistordata.incrementEngineeringValue))
                    .addMarker(String.valueOf(thermistordata.minEngineeringValue))
                    .addMarker(String.valueOf(thermistordata.maxEngineeringValue))
                    .setGroup(String.valueOf(mNodeAddr))
                    .setTz(tz)
                    .build();
            String th2InputSensorId = mHayStack.addPoint(th2InputSensor);
            mHayStack.writeDefaultValById(th2InputSensorId, 0.0);

            mHayStack.writeDefaultVal("point and config and th2relay and enabled and equipRef == \"" + mEquipRef + "\"",
                    config.isTh2Enable ? 1.0 : 0);
        }

    }

    public HyperStatSenseConfiguration getHyperStatSenseConfig(){
        HyperStatSenseConfiguration HSSConfig = new HyperStatSenseConfiguration();
        HSSConfig.temperatureOffset = mHayStack.readHisValByQuery("point and air and temp and sensor and current and group == \""+mNodeAddr+"\"");
        HSSConfig.analog1Sensor = mHayStack.readHisValByQuery("point and config and analog1 and input and sensor and equipRef == \"" + mEquipRef + "\"").intValue();
        HSSConfig.analog2Sensor = mHayStack.readHisValByQuery("point and config and analog2 and input and sensor and equipRef == \"" + mEquipRef + "\"").intValue();
        HSSConfig.th1Sensor = mHayStack.readHisValByQuery("point and config and th1 and input and sensor and equipRef == \"" + mEquipRef + "\"").intValue();
        HSSConfig.th2Sensor = mHayStack.readHisValByQuery("point and config and th2 and input and sensor and equipRef == \"" + mEquipRef + "\"").intValue();
        HSSConfig.isAnalog1Enable = mHayStack.readHisValByQuery("point and config and analog1relay and enabled and equipRef == \"" + mEquipRef + "\"") > 0;
        HSSConfig.isAnalog2Enable = mHayStack.readHisValByQuery("point and config and analog2relay and enabled and equipRef == \"" + mEquipRef + "\"") > 0;
        HSSConfig.isTh1Enable = mHayStack.readHisValByQuery("point and config and th1relay and enabled and equipRef == \"" + mEquipRef + "\"") > 0;
        HSSConfig.isTh2Enable = mHayStack.readHisValByQuery("point and config and th2relay and enabled and equipRef == \"" + mEquipRef + "\"") > 0;

        return HSSConfig;
    }
}
