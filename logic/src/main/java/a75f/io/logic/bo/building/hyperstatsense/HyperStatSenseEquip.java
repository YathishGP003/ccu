package a75f.io.logic.bo.building.hyperstatsense;

import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.Thermistor;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.haystack.device.DeviceUtil;
import a75f.io.logic.bo.haystack.device.HyperStatDevice;
import a75f.io.logic.bo.haystack.device.SmartNode;

/*
 * created by spoorthidev on 30-May-2021
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
        HashMap equip = CCUHsApi.getInstance().read("equip and sense and group == \"" + mNodeAddr + "\"");

        if (equip.isEmpty()) {
            Log.d(LOG_TAG, "Init Failed : Equip does not exist ");
            return;
        }
        Log.d(LOG_TAG, "Init equip : " + equip.get("id").toString());
        mEquipRef = equip.get("id").toString();
    }

    public void createEntities(HyperStatSenseConfiguration config, String floorRef, String roomRef) {
        Log.d(LOG_TAG, "createEntities ++");
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-SENSE-" + mNodeAddr;
        HashMap systemEquip = mHayStack.read("equip and system");
        String ahuRef = null;
        if (systemEquip != null && systemEquip.size() > 0) {
            ahuRef = systemEquip.get("id").toString();
        }

        Log.d(LOG_TAG, "In create " + "TemperatureOffset = " + String.valueOf(config.temperatureOffset) + " /n" +
                "isTh1Enable = " + String.valueOf(config.isTh1Enable) + " \n" +
                "isTh2Enable = " + String.valueOf(config.isTh2Enable) + " \n" +
                "isAnalog1Enable = " + String.valueOf(config.isAnalog1Enable) + " \n" +
                "isAnalog2Enable = " + String.valueOf(config.isAnalog2Enable) + " \n" +
                "th1Sensor = " + String.valueOf(config.th1Sensor) + " \n" +
                "th2Sensor = " + String.valueOf(config.th2Sensor) + " \n" +
                "analog1Sensor = " + String.valueOf(config.analog1Sensor) + " \n" +
                "analog2Sensor = " + String.valueOf(config.analog2Sensor));

        Equip b = new Equip.Builder()
                .setSiteRef(siteRef)
                .setDisplayName(equipDis)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setProfile(mProfileType.name())
                .addMarker("equip").addMarker("hyperstat").addMarker("sense").addMarker("zone")
                .setAhuRef(ahuRef)
                .setTz(tz)
                .setGroup(String.valueOf(mNodeAddr)).build();
        mEquipRef = mHayStack.addEquip(b);

        Point equipScheduleType = new Point.Builder()
                .setDisplayName(equipDis + "-scheduleType")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("hyperstat").addMarker("scheduleType")
                .addMarker("writable").addMarker("his").addMarker("sense")
                .setGroup(String.valueOf(mNodeAddr))
                .setEnums("building,zone,named")
                .setTz(tz)
                .build();
        String equipScheduleTypeId = CCUHsApi.getInstance().addPoint(equipScheduleType);
        mHayStack.writeDefaultValById(equipScheduleTypeId, 0.0);
        mHayStack.writeHisValById(equipScheduleTypeId, 0.0);


        Point isAnalog1enaled = new Point.Builder()
                .setDisplayName(equipDis + "-isAnalog1enaled")//update the display name
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("hyperstat").addMarker("zone").addMarker("writable")
                .addMarker("enabled").addMarker("analog1").addMarker("sense")
                .setGroup(String.valueOf(mNodeAddr))
                .setEnums("false,true")
                .setTz(tz)
                .build();
        String isAn1ID = mHayStack.addPoint(isAnalog1enaled);
        mHayStack.writeDefaultValById(isAn1ID, config.isAnalog1Enable ? 1.0 : 0);

        Point isAn2 = new Point.Builder()
                .setDisplayName(equipDis + "-isAnalog2enabled")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("hyperstat").addMarker("zone").addMarker("writable")
                .addMarker("enabled").addMarker("analog2").addMarker("sense")
                .setGroup(String.valueOf(mNodeAddr))
                .setEnums("false,true")
                .setTz(tz)
                .build();
        String isAn2ID = mHayStack.addPoint(isAn2);
        mHayStack.writeDefaultValById(isAn2ID, config.isAnalog2Enable ? 1.0 : 0.0);

        Point isTh1 = new Point.Builder()
                .setDisplayName(equipDis + "-isThermister1enabled")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("hyperstat").addMarker("zone").addMarker("writable")
                .addMarker("enabled").addMarker("th1").addMarker("sense")
                .setGroup(String.valueOf(mNodeAddr))
                .setEnums("false,true")
                .setTz(tz)
                .build();
        String isTh1ID = mHayStack.addPoint(isTh1);
        mHayStack.writeDefaultValById(isTh1ID, config.isTh1Enable ? 1.0 : 0.0);

        Point isTh2 = new Point.Builder()
                .setDisplayName(equipDis + "-isThermister2enabled")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("hyperstat").addMarker("zone").addMarker("writable")
                .addMarker("enabled").addMarker("th2").addMarker("sense")
                .setGroup(String.valueOf(mNodeAddr))
                .setEnums("false,true")
                .setTz(tz)
                .build();
        String isTh2ID = mHayStack.addPoint(isTh2);
        mHayStack.writeDefaultValById(isTh2ID, config.isTh2Enable ? 1.0 : 0.0);

        Point temperatureOffset = new Point.Builder()
                .setDisplayName(equipDis + "-temperatureOffset")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("writable").addMarker("zone")
                .addMarker("temp").addMarker("offset").addMarker("hyperstat").addMarker("sense")
                .setGroup(String.valueOf(mNodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String tempoffsetId = mHayStack.addPoint(temperatureOffset);
        mHayStack.writeDefaultValById(tempoffsetId, config.temperatureOffset);

        Point currentTemp = new Point.Builder()
                .setDisplayName(siteDis + "-SENSE-" + mNodeAddr + "-currentTemp")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("zone").addMarker("hyperstat").addMarker("sense")
                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("current")
                .addMarker("his").addMarker("cur").addMarker("logical")
                .setGroup(String.valueOf(mNodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String ctID = CCUHsApi.getInstance().addPoint(currentTemp);
        mHayStack.writeDefaultValById(ctID, 0.0);
        mHayStack.writeHisValById(ctID, 0.0);

        Point humidity = new Point.Builder()
                .setDisplayName(siteDis + "-SENSE-" + mNodeAddr + "-humidity")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("zone").addMarker("hyperstat").addMarker("sense")
                .addMarker("humidity").addMarker("sensor").addMarker("his").addMarker("cur").addMarker("logical")
                .setGroup(String.valueOf(mNodeAddr))
                .setUnit("%")
                .setTz(tz)
                .build();
        String humidityId = CCUHsApi.getInstance().addPoint(humidity);
        mHayStack.writeDefaultValById(humidityId, 0.0);
        mHayStack.writeHisValById(humidityId, 0.0);

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
        mHayStack.writeDefaultValById(analog1InputSensorId, (double) config.analog1Sensor >= 0 ? config.analog1Sensor : 0.0);


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
        mHayStack.writeDefaultValById(analog2InputSensorId, (double) config.analog2Sensor >= 0 ? config.analog2Sensor : 0.0);

        Point th1InputSensor = new Point.Builder()
                .setDisplayName(equipDis + "-th1InputSensor")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setShortDis("Thermister1 Input Config")
                .addMarker("config").addMarker("zone").addMarker("writable")
                .addMarker("th1").addMarker("input").addMarker("sensor")
                .addMarker("hyperstat").addMarker("sense")
                .setGroup(String.valueOf(mNodeAddr))
                .setTz(tz)
                .build();
        String th1InputSensorId = mHayStack.addPoint(th1InputSensor);
        mHayStack.writeDefaultValById(th1InputSensorId, (double) config.th1Sensor >= 0 ? config.th1Sensor : 0.0);

        Point th2InputSensor = new Point.Builder()
                .setDisplayName(equipDis + "-th2InputSensor")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setShortDis("Thermister2 Input Config")
                .addMarker("config").addMarker("zone").addMarker("writable")
                .addMarker("th2").addMarker("input").addMarker("sensor")
                .addMarker("hyperstat").addMarker("sense")
                .setGroup(String.valueOf(mNodeAddr))
                .setTz(tz)
                .build();
        String th2InputSensorId = mHayStack.addPoint(th2InputSensor);
        mHayStack.writeDefaultValById(th2InputSensorId, (double) config.th2Sensor >= 0 ? config.th2Sensor : 0.0);

        HyperStatDevice device = new HyperStatDevice(mNodeAddr, siteRef, floorRef, roomRef, mEquipRef, "sense");

        if (config.isAnalog1Enable) {
            String sensorId = createSensorPoint(floorRef, roomRef, "analog1", config);
            device.analog1In.setPointRef(sensorId);
            device.analog1In.setEnabled(true);
            device.analog1In.setType(String.valueOf(config.analog1Sensor));
        }

        if (config.isAnalog2Enable) {
            String sensorId = createSensorPoint(floorRef, roomRef, "analog2", config);
            device.analog2In.setPointRef(sensorId);
            device.analog2In.setEnabled(true);
            device.analog2In.setType(String.valueOf(config.analog2Sensor));
        }

        if (config.isTh1Enable) {
            String sensorId = createSensorPoint(floorRef, roomRef, "th1", config);
            device.th1In.setPointRef(sensorId);
            device.th1In.setEnabled(true);
            device.th1In.setType(String.valueOf(config.th1Sensor));
        }

        if (config.isTh2Enable) {
            String sensorId = createSensorPoint(floorRef, roomRef, "th2", config);
            device.th2In.setPointRef(sensorId);
            device.th2In.setEnabled(true);
            device.th2In.setType(String.valueOf(config.th2Sensor));
        }
        device.currentTemp.setPointRef(ctID);
        device.currentTemp.setEnabled(true);

        device.addSensor(Port.SENSOR_RH, humidityId);
        device.addPointsToDb();
        mHayStack.syncEntityTree();
    }

    public void update(ProfileType type, int node, HyperStatSenseConfiguration config, String floorRef, String roomRef) {

        HashMap tempOffset = mHayStack.read("point and temp and offset and equipRef == \"" + mEquipRef + "\"");
        HashMap isTh1 = mHayStack.read("point and config and th1  and enabled and equipRef == \"" + mEquipRef + "\"");
        HashMap isTh2 = mHayStack.read("point and config and th2  and enabled and equipRef == \"" + mEquipRef + "\"");
        HashMap isAn1 = mHayStack.read("point and config and analog1  and enabled and equipRef == \"" + mEquipRef + "\"");
        HashMap isAn2 = mHayStack.read("point and config and analog2  and enabled and equipRef == \"" + mEquipRef + "\"");
        HashMap Th1 = mHayStack.read("point and config and th1 and input and sensor and equipRef == \"" + mEquipRef + "\"");
        HashMap Th2 = mHayStack.read("point and config and th2 and input and sensor and equipRef == \"" + mEquipRef + "\"");
        HashMap An1 = mHayStack.read("point and config and analog1 and input and sensor and equipRef == \"" + mEquipRef + "\"");
        HashMap An2 = mHayStack.read("point and config and analog2 and input and sensor and equipRef == \"" + mEquipRef + "\"");
        HashMap Th1Val = mHayStack.read("point and logical and th1 and equipRef == \"" + mEquipRef + "\"");
        HashMap Th2Val = mHayStack.read("point and logical and th2 and equipRef == \"" + mEquipRef + "\"");
        HashMap An1Val = mHayStack.read("point and logical and analog1 and equipRef == \"" + mEquipRef + "\"");
        HashMap An2Val = mHayStack.read("point and logical and analog2 and equipRef == \"" + mEquipRef + "\"");
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);

        Log.d(LOG_TAG, "In update " + "TemperatureOffset = " + String.valueOf(config.temperatureOffset) + " /n" +
                "isTh1Enable = " + String.valueOf(config.isTh1Enable) + " \n" +
                "isTh2Enable = " + String.valueOf(config.isTh2Enable) + " \n" +
                "isAnalog1Enable = " + String.valueOf(config.isAnalog1Enable) + " \n" +
                "isAnalog2Enable = " + String.valueOf(config.isAnalog2Enable) + " \n" +
                "th1Sensor = " + String.valueOf(config.th1Sensor) + " \n" +
                "th2Sensor = " + String.valueOf(config.th2Sensor) + " \n" +
                "analog1Sensor = " + String.valueOf(config.analog1Sensor) + " \n" +
                "analog2Sensor = " + String.valueOf(config.analog2Sensor));


        HyperStatSenseConfiguration currentConfig = getHyperStatSenseConfig();

        if (tempOffset != null && tempOffset.get("id") != null && config.temperatureOffset != currentConfig.temperatureOffset) {
            Log.d(LOG_TAG, "tempOffset update : " + config.temperatureOffset);
            mHayStack.writeDefaultValById(tempOffset.get("id").toString(), config.temperatureOffset);
        }

        if (config.isTh1Enable != currentConfig.isTh1Enable){
            Log.d(LOG_TAG, "thermister1 toggle update : " + config.isTh1Enable);
            mHayStack.writeDefaultValById(isTh1.get("id").toString(), config.isTh1Enable ? 1.0 : 0.0);
            if(Th1Val != null && Th1Val.get("id") != null){
                CCUHsApi.getInstance().deleteEntityTree(Th1Val.get("id").toString());
            }else{
                Log.d(LOG_TAG, "TH1 is null");
            }
            if (config.isTh1Enable) {
                Log.d(LOG_TAG, "thermister1 toggle update create new entry: ");
                String id = createSensorPoint(floorRef, roomRef, "th1", config);
                DeviceUtil.setPointEnabled(mNodeAddr, Port.TH1_IN.name(), true);
                DeviceUtil.updatePhysicalPointRef(mNodeAddr, Port.TH1_IN.name(), id);
            }
        }
        else if (config.isTh1Enable && config.th1Sensor != currentConfig.th1Sensor){
            Log.d(LOG_TAG, "thermister1 spinner update : " + config.th1Sensor);
            mHayStack.writeDefaultValById(Th1.get("id").toString(), (double) config.th1Sensor);
            if (Th1Val != null && Th1Val.get("id") != null) {
                CCUHsApi.getInstance().deleteEntityTree(Th1Val.get("id").toString());
            }else{
                Log.d(LOG_TAG, "TH1 is null");
            }
            String id = createSensorPoint(floorRef, roomRef, "th1", config);
            DeviceUtil.setPointEnabled(mNodeAddr, Port.TH1_IN.name(), true);
            DeviceUtil.updatePhysicalPointRef(mNodeAddr, Port.TH1_IN.name(), id);
        }


        if (config.isTh2Enable != currentConfig.isTh2Enable){
            Log.d(LOG_TAG, "thermister2 toggle update : " + config.isTh2Enable);
            mHayStack.writeDefaultValById(isTh2.get("id").toString(), config.isTh2Enable ? 1.0 : 0.0);
            if(Th2Val != null && Th2Val.get("id") != null){
                CCUHsApi.getInstance().deleteEntityTree(Th2Val.get("id").toString());
            }else{
                Log.d(LOG_TAG, "TH2 is null");
            }
            if (config.isTh2Enable) {
                Log.d(LOG_TAG, "thermister2 toggle update create new entry: ");
                String id = createSensorPoint(floorRef, roomRef, "th2", config);
                DeviceUtil.setPointEnabled(mNodeAddr, Port.TH2_IN.name(), true);
                DeviceUtil.updatePhysicalPointRef(mNodeAddr, Port.TH2_IN.name(), id);
            }
        }
        else if (config.isTh2Enable && config.th2Sensor != currentConfig.th2Sensor){
            Log.d(LOG_TAG, "thermister2 spinner update : " + config.th1Sensor);
            mHayStack.writeDefaultValById(Th2.get("id").toString(), (double) config.th2Sensor);
            if (Th2Val != null && Th2Val.get("id") != null) {
                CCUHsApi.getInstance().deleteEntityTree(Th2Val.get("id").toString());
            }else{
                Log.d(LOG_TAG, "TH2 is null");
            }
            String id = createSensorPoint(floorRef, roomRef, "th2", config);
            DeviceUtil.setPointEnabled(mNodeAddr, Port.TH2_IN.name(), true);
            DeviceUtil.updatePhysicalPointRef(mNodeAddr, Port.TH2_IN.name(), id);
        }


        if (config.isAnalog1Enable != currentConfig.isAnalog1Enable){
            Log.d(LOG_TAG, "an1 toggle update : " + config.isAnalog1Enable);
            mHayStack.writeDefaultValById(isAn1.get("id").toString(), config.isAnalog1Enable ? 1.0 : 0.0);
            if(An1Val != null && An1Val.get("id") != null){
                CCUHsApi.getInstance().deleteEntityTree(An1Val.get("id").toString());
            }else{
                Log.d(LOG_TAG, "An1 is null");
            }
            if (config.isAnalog1Enable) {
                Log.d(LOG_TAG, "an1 toggle update create new entry: ");
                String id = createSensorPoint(floorRef, roomRef, "analog1", config);
                DeviceUtil.setPointEnabled(mNodeAddr, Port.ANALOG_IN_ONE.name(), true);
                DeviceUtil.updatePhysicalPointRef(mNodeAddr, Port.ANALOG_IN_ONE.name(), id);
            }
        }
        else if (config.isAnalog1Enable && config.analog1Sensor != currentConfig.analog1Sensor){
            Log.d(LOG_TAG, "an1 spinner update : " + config.analog1Sensor);
            mHayStack.writeDefaultValById(An1.get("id").toString(), (double) config.analog1Sensor);
            if ( An1Val!= null && An1Val.get("id") != null) {
                CCUHsApi.getInstance().deleteEntityTree(An1Val.get("id").toString());
            }
            else{
                Log.d(LOG_TAG, "An1 is null");
            }
            String id = createSensorPoint(floorRef, roomRef, "analog1", config);
            DeviceUtil.setPointEnabled(mNodeAddr, Port.ANALOG_IN_ONE.name(), true);
            DeviceUtil.updatePhysicalPointRef(mNodeAddr, Port.ANALOG_IN_ONE.name(), id);

        }

        if (config.isAnalog2Enable != currentConfig.isAnalog2Enable){
            Log.d(LOG_TAG, "an2 toggle update : " + config.isAnalog2Enable);
            mHayStack.writeDefaultValById(isAn2.get("id").toString(), config.isAnalog2Enable ? 1.0 : 0.0);
            if(An2Val != null && An2Val.get("id") != null){
                CCUHsApi.getInstance().deleteEntityTree(An2Val.get("id").toString());
            }else{
                Log.d(LOG_TAG, "An2 is null");
            }
            if (config.isAnalog2Enable) {
                Log.d(LOG_TAG, "an2 toggle update create new entry: ");
                String id = createSensorPoint(floorRef, roomRef, "analog2", config);
                DeviceUtil.setPointEnabled(mNodeAddr, Port.ANALOG_IN_TWO.name(), true);
                DeviceUtil.updatePhysicalPointRef(mNodeAddr, Port.ANALOG_IN_TWO.name(), id);
            }
        }
        else if (config.isAnalog2Enable && config.analog2Sensor != currentConfig.analog2Sensor){
            Log.d(LOG_TAG, "an2 spinner update : " + config.analog2Sensor);
            mHayStack.writeDefaultValById(An2.get("id").toString(), (double) config.analog2Sensor);
            if ( An2Val!= null && An2Val.get("id") != null) {
                CCUHsApi.getInstance().deleteEntityTree(An2Val.get("id").toString());
            }
            else{
                Log.d(LOG_TAG, "An2 is null");
            }
            String id = createSensorPoint(floorRef, roomRef, "analog2", config);
            DeviceUtil.setPointEnabled(mNodeAddr, Port.ANALOG_IN_TWO.name(), true);
            DeviceUtil.updatePhysicalPointRef(mNodeAddr, Port.ANALOG_IN_TWO.name(), id);
        }

        mHayStack.syncEntityTree();

    }


    public HyperStatSenseConfiguration getHyperStatSenseConfig() {
        HyperStatSenseConfiguration HSSConfig = new HyperStatSenseConfiguration();
        HSSConfig.temperatureOffset = mHayStack.readDefaultVal("point and temp and offset and equipRef == \"" + mEquipRef + "\"");
        HSSConfig.analog1Sensor = mHayStack.readDefaultVal("point and config and analog1 and input and sensor and equipRef == \"" + mEquipRef + "\"").intValue();
        HSSConfig.analog2Sensor = mHayStack.readDefaultVal("point and config and analog2 and input and sensor and equipRef == \"" + mEquipRef + "\"").intValue();
        HSSConfig.th1Sensor = mHayStack.readDefaultVal("point and config and th1 and input and sensor and equipRef == \"" + mEquipRef + "\"").intValue();
        HSSConfig.th2Sensor = mHayStack.readDefaultVal("point and config and th2 and input and sensor and equipRef == \"" + mEquipRef + "\"").intValue();
        HSSConfig.isAnalog1Enable = mHayStack.readDefaultVal("point and config and analog1 and enabled  and equipRef == \"" + mEquipRef + "\"") > 0;
        HSSConfig.isAnalog2Enable = mHayStack.readDefaultVal("point and config and analog2 and enabled and equipRef == \"" + mEquipRef + "\"") > 0;
        HSSConfig.isTh1Enable =
                mHayStack.readDefaultVal("point and config and th1  and enabled and equipRef == \"" + mEquipRef + "\"") > 0;
        HSSConfig.isTh2Enable =
                mHayStack.readDefaultVal("point and config and th2 and enabled and equipRef == \"" + mEquipRef + "\"") > 0;
        return HSSConfig;
    }

    private String createSensorPoint(String floorRef, String roomRef, String tag,
                                     HyperStatSenseConfiguration config) {

        HashMap siteMap = mHayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-SENSE-" + mNodeAddr + "-";
        Bundle bundle = new Bundle();
        if (tag.equals("analog1")) {
            bundle = getAnalogBundle(config.analog1Sensor);
        } else if (tag.equals("analog2")) {
            bundle = getAnalogBundle(config.analog2Sensor);
        } else if (tag.equals("th1")) {
            bundle = getThermistorBundle(config.th1Sensor);
        } else if (tag.equals("th2")) {
            bundle = getThermistorBundle(config.th2Sensor);
        }

        String shortDis = bundle.getString("shortDis");
        String unit = bundle.getString("unit");
        String maxVal = bundle.getString("maxVal");
        String minVal = bundle.getString("minVal");
        String[] markers = bundle.getStringArray("markers");

        Point.Builder sensorTag = new Point.Builder()
                .setDisplayName(equipDis + shortDis)
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setShortDis(shortDis)
                .setHisInterpolate("cov")
                .addMarker("logical").addMarker("zone").addMarker("his").addMarker(tag)
                .addMarker("hyperstat").addMarker("sense")
                .setGroup(String.valueOf(mNodeAddr))
                .setMinVal(minVal)
                .setMaxVal(maxVal)
                .setUnit(unit)
                .setTz(tz);
        if (markers != null) {
            for (String marker : markers) {
                sensorTag.addMarker(marker);
            }
        }

        String sensorVariableTagId = mHayStack.addPoint(sensorTag.build());
        mHayStack.writeDefaultValById(sensorVariableTagId, 0.0);
        mHayStack.writeHisValById(sensorVariableTagId, 0.0);

        return sensorVariableTagId;
    }


    private Bundle getAnalogBundle(int analog) {
        Bundle bundle = new Bundle();
        String shortDis = "Generic 0-10 Voltage";
        String shortDisTarget = "Dynamic Target Voltage";
        String unit = "V";
        String maxVal = "10";
        String minVal = "0";
        String incrementVal = "0.1";
        String[] markers = null;
        switch (analog) {
            case 0:
                shortDis = "Generic 0-10 Voltage";
                shortDisTarget = "Dynamic Target Voltage";
                unit = "V";
                maxVal = "10";
                minVal = "0";
                incrementVal = "0.1";
                markers = null;
                break;
            case 1:
                shortDis = "Pressure [0-2 in.]";
                shortDisTarget = "Dynamic Target Pressure";
                unit = "Inch wc";
                maxVal = "2";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"pressure"};
                break;
            case 2:
                shortDis = "Pressure[0-0.25 in. Differential]";
                shortDisTarget = "Dynamic Target Pressure Differential";
                unit = "Inch wc";
                maxVal = "0.25";
                minVal = "-0.25";
                incrementVal = "0.01";
                markers = new String[]{"pressure"};
                break;
            case 3:
                shortDis = "Airflow";
                shortDisTarget = "Dynamic Target Airflow";
                unit = "CFM";
                maxVal = "1000";
                minVal = "0";
                incrementVal = "10";
                markers = new String[]{"airflow"};
                break;
            case 4:
                shortDis = "Humidity";
                shortDisTarget = "Dynamic Target Humidity";
                unit = "%";
                maxVal = "100";
                minVal = "0";
                incrementVal = "1.0";
                markers = new String[]{"humidity"};
                break;
            case 5:
                shortDis = "CO2 Level";
                shortDisTarget = "Dynamic Target CO2 Level";
                unit = "ppm";
                maxVal = "2000";
                minVal = "0";
                incrementVal = "100";
                markers = new String[]{"co2"};
                break;
            case 6:
                shortDis = "CO Level";
                shortDisTarget = "Dynamic Target CO Level";
                unit = "ppm";
                maxVal = "100";
                minVal = "0";
                incrementVal = "1.0";
                markers = new String[]{"co"};
                break;
            case 7:
                shortDis = "NO2 Level";
                shortDisTarget = "Dynamic Target NO2 Level";
                unit = "ppm";
                maxVal = "5";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"no2"};
                break;
            case 8:
                shortDis = "Current Drawn[CT 0-10]";
                shortDisTarget = "Dynamic Target Current Draw";
                unit = "A";
                maxVal = "10";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"current", "transformer"};
                break;
            case 9:
                shortDis = "Current Drawn[CT 0-20]";
                shortDisTarget = "Dynamic Target Current Draw";
                unit = "A";
                maxVal = "20";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"current", "transformer"};
                break;
            case 10:
                shortDis = "Current Drawn[CT 0-50]";
                shortDisTarget = "Dynamic Target Current Draw";
                unit = "A";
                maxVal = "50";
                minVal = "0";
                incrementVal = "0.1";
                markers = new String[]{"current", "transformer"};
                break;
            case 11:
                shortDis = "ION Density";
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

    private Bundle getThermistorBundle(int th) {
        Bundle bundle = new Bundle();
        Thermistor thermistor = Thermistor.getThermistorList().get(th);
        String[] markers = new String[]{"temp"};

        bundle.putString("shortDis", thermistor.sensorName);
        bundle.putString("shortDisTarget", "Target Temperature");
        bundle.putString("unit", thermistor.engineeringUnit);
        bundle.putString("maxVal", String.valueOf(thermistor.maxEngineeringValue));
        bundle.putString("minVal", String.valueOf(thermistor.minEngineeringValue));
        bundle.putString("incrementVal", String.valueOf(thermistor.incrementEngineeringValue));
        bundle.putStringArray("markers", markers);

        return bundle;
    }
}
