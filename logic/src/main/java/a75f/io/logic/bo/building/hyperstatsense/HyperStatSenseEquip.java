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
                .addMarker("equip").addMarker("hyperstat").addMarker("sense").addMarker("zone")
                .setAhuRef(ahuRef)
                .setTz(tz)
                .setGroup(String.valueOf(mNodeAddr));
        mEquipRef = CCUHsApi.getInstance().addEquip(b.build());


        mHayStack.writeDefaultVal("point and config and analog1relay and enabled and equipRef == \"" + mEquipRef + "\"",
                config.isAnalog1Enable ? 1.0 : 0);
        mHayStack.writeDefaultVal("point and config and analog2relay and enabled and equipRef == \"" + mEquipRef + "\"",
                config.isAnalog2Enable ? 1.0 : 0);
        mHayStack.writeDefaultVal("point and config and th1relay and enabled and equipRef == \"" + mEquipRef + "\"",
                config.isTh1Enable ? 1.0 : 0);
        mHayStack.writeDefaultVal("point and config and th2relay and enabled and equipRef == \"" + mEquipRef + "\"",
                config.isTh2Enable ? 1.0 : 0);

        Point temperatureOffset = new Point.Builder()
                .setDisplayName(equipDis+"-temperatureOffset")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .addMarker("config").addMarker("writable").addMarker("zone")
                .addMarker("temp").addMarker("offset").addMarker("hyperstat").addMarker("sense")
                .setGroup(String.valueOf(mNodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String tempoffsetId = mHayStack.addPoint(temperatureOffset);
        mHayStack.writeDefaultValById(tempoffsetId, 0.0);


        Point analog1InputSensor = new Point.Builder()
                .setDisplayName(equipDis + "-analog1InputSensor")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setShortDis("Analog1 Input Config")
                .addMarker("config").addMarker("zone").addMarker("writable")
                .addMarker("analog1").addMarker("input").addMarker("sensor")
                .addMarker("hyperstat").addMarker("sense")
                .setGroup(String.valueOf(mNodeAddr))
                .setTz(tz).build();
        String analog1InputSensorId = mHayStack.addPoint(analog1InputSensor);
        mHayStack.writeDefaultValById(analog1InputSensorId, 0.0);


        Point analog2InputSensor = new Point.Builder()
                .setDisplayName(equipDis + "-analog2InputSensor")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setShortDis("Analog2 Input Config")
                .addMarker("config").addMarker("zone").addMarker("writable")
                .addMarker("analog2").addMarker("input").addMarker("sensor")
                .addMarker("hyperstat").addMarker("sense")
                .setGroup(String.valueOf(mNodeAddr))
                .setTz(tz)
                .build();
        String analog2InputSensorId = mHayStack.addPoint(analog2InputSensor);
        mHayStack.writeDefaultValById(analog2InputSensorId, 0.0);

        Point th1InputSensor = new Point.Builder()
                .setDisplayName(equipDis + "-th1InputSensor")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("zone").addMarker("writable")
                .addMarker("th1").addMarker("input").addMarker("sensor")
                .addMarker("hyperstat").addMarker("sense")
                .setGroup(String.valueOf(mNodeAddr))
                .setTz(tz)
                .build();
        String th1InputSensorId = mHayStack.addPoint(th1InputSensor);
        mHayStack.writeDefaultValById(th1InputSensorId, 0.0);

        Point th2InputSensor = new Point.Builder()
                .setDisplayName(equipDis + "-th2InputSensor")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("zone").addMarker("writable")
                .addMarker("th2").addMarker("input").addMarker("sensor")
                .addMarker("hyperstat").addMarker("sense")
                .setGroup(String.valueOf(mNodeAddr))
                .setTz(tz)
                .build();
        String th2InputSensorId = mHayStack.addPoint(th2InputSensor);
        mHayStack.writeDefaultValById(th2InputSensorId, 0.0);


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
