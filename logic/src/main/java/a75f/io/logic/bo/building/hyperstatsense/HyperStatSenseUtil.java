package a75f.io.logic.bo.building.hyperstatsense;

import android.os.Bundle;
import android.util.Log;

import com.google.gson.JsonObject;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Thermistor;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.haystack.device.DeviceUtil;
import a75f.io.logic.bo.haystack.device.SmartStat;

public class HyperStatSenseUtil {

    private static String LOG_TAG = "HyperStatSenseUtil";

    public static void updateConfigEnabled(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        Log.d(LOG_TAG,"updateConfigEnabled ++");
        HashMap equipMap = hayStack.readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        String floorRef = equip.getFloorRef();
        String equipref = configPoint.getEquipRef();
        String nodeAddr = equip.getGroup();
        String roomRef = equip.getRoomRef();
        int configVal = msgObject.get("val").getAsInt();
        HashMap Th1Val = hayStack.read("point and logical and th1 and equipRef == \"" + equip.getId() + "\"");
        HashMap Th2Val = hayStack.read("point and logical and th2 and equipRef == \"" + equip.getId() + "\"");
        HashMap An1Val = hayStack.read("point and logical and analog1 and equipRef == \"" + equip.getId() + "\"");
        HashMap An2Val = hayStack.read("point and logical and analog2 and equipRef == \"" + equip.getId() + "\"");

        if (configPoint.getMarkers().contains(Tags.TH1)) {
            DeviceUtil.setPointEnabled(Integer.parseInt(nodeAddr), Port.TH1_IN.name(),
                    configVal > 0 ? true : false);
            if(configVal > 0 ){
                String id = createSensorPoint(floorRef, roomRef, "th1", 0, nodeAddr, equipref);
                DeviceUtil.setPointEnabled(Integer.valueOf(nodeAddr), Port.TH1_IN.name(), true);
                DeviceUtil.updatePhysicalPointRef(Integer.valueOf(nodeAddr), Port.TH1_IN.name(), id);
            }else{
                if (Th1Val != null && Th1Val.get("id") != null) {
                    Log.d(LOG_TAG,"updateConfigEnabled ++ delete th1");
                    hayStack.deleteEntityTree(Th1Val.get("id").toString());
                }
            }
        } else if (configPoint.getMarkers().contains(Tags.TH2)) {
            DeviceUtil.setPointEnabled(Integer.parseInt(nodeAddr), Port.TH2_IN.name(),
                    configVal > 0 ? true : false);
            if(configVal > 0 ){
                String id = createSensorPoint(floorRef, roomRef, "th2", 0, nodeAddr, equipref);
                DeviceUtil.setPointEnabled(Integer.valueOf(nodeAddr), Port.TH2_IN.name(), true);
                DeviceUtil.updatePhysicalPointRef(Integer.valueOf(nodeAddr), Port.TH2_IN.name(), id);
            }else{
                if (Th2Val != null && Th2Val.get("id") != null) {
                    Log.d(LOG_TAG,"updateConfigEnabled ++ delete th2");
                    hayStack.deleteEntityTree(Th2Val.get("id").toString());
                }
            }
        } else if (configPoint.getMarkers().contains(Tags.ANALOG1)) {
            DeviceUtil.setPointEnabled(Integer.parseInt(nodeAddr), Port.ANALOG_IN_ONE.name(),
                    configVal > 0 ? true : false);
            if(configVal > 0 ){
                String id = createSensorPoint(floorRef, roomRef, "analog1", 0, nodeAddr, equipref);
                DeviceUtil.setPointEnabled(Integer.valueOf(nodeAddr), Port.ANALOG_IN_ONE.name(), true);
                DeviceUtil.updatePhysicalPointRef(Integer.valueOf(nodeAddr), Port.ANALOG_IN_ONE.name(), id);
            }else{
                if (An1Val != null && An1Val.get("id") != null) {
                    Log.d(LOG_TAG, "updateConfigEnabled ++ delete An1");
                    hayStack.deleteEntityTree(An1Val.get("id").toString());
                }
            }
        } else if (configPoint.getMarkers().contains(Tags.ANALOG2)) {
            DeviceUtil.setPointEnabled(Integer.parseInt(nodeAddr), Port.ANALOG_IN_TWO.name(),
                    configVal > 0 ? true : false);
            if(configVal > 0 ){
                String id = createSensorPoint(floorRef, roomRef, "analog2", 0, nodeAddr, equipref);
                DeviceUtil.setPointEnabled(Integer.valueOf(nodeAddr), Port.ANALOG_IN_TWO.name(), true);
                DeviceUtil.updatePhysicalPointRef(Integer.valueOf(nodeAddr), Port.ANALOG_IN_TWO.name(), id);
            }else{
                if (An2Val != null && An2Val.get("id") != null) {
                    Log.d(LOG_TAG, "updateConfigEnabled ++ delete An2");
                    hayStack.deleteEntityTree(An2Val.get("id").toString());
                }
            }
        }
        writePointFromJson(configPoint.getId(), configVal, msgObject, hayStack);
        hayStack.syncPointEntityTree();

    }

    public static void updatetempOffset(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        Log.d(LOG_TAG,"updatetempOffset ++");
        double val = msgObject.get("val").getAsDouble();
        hayStack.writeDefaultValById(configPoint.getId(), val);
        writePointFromJson(configPoint.getId(), val, msgObject, hayStack);
    }


    public static void updateConfig(JsonObject msgObject, Point configPoint, CCUHsApi hayStack) {
        Log.d(LOG_TAG,"updateConfig ++");
        HashMap equipMap = CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        String floorRef = equip.getFloorRef();
        String equipref = configPoint.getEquipRef();
        String nodeAddr = equip.getGroup();
        String roomRef = equip.getRoomRef();
        int configVal = msgObject.get("val").getAsInt();
        HashMap Th1Val = CCUHsApi.getInstance().read("point and logical and th1 and equipRef == \"" + equipref + "\"");
        HashMap Th2Val = CCUHsApi.getInstance().read("point and logical and th2 and equipRef == \"" + equipref + "\"");
        HashMap An1Val = CCUHsApi.getInstance().read("point and logical and analog1 and equipRef == \"" + equipref+ "\"");
        HashMap An2Val = CCUHsApi.getInstance().read("point and logical and analog2 and equipRef == \"" +equipref + "\"");

        CCUHsApi.getInstance().writeDefaultValById(configPoint.getId(), (double) configVal);

        if (configPoint.getMarkers().contains(Tags.TH1)) {
            if (Th1Val != null && Th1Val.get("id") != null) {
                Log.d(LOG_TAG,"updateConfig ++ delete th1");
                CCUHsApi.getInstance().deleteEntityTree(Th1Val.get("id").toString());
            }
            Log.d(LOG_TAG,"updateConfig  ++ th1");
            String id = createSensorPoint(floorRef, roomRef, "th1", configVal, nodeAddr, equipref);
            DeviceUtil.setPointEnabled(Integer.valueOf(nodeAddr), Port.TH1_IN.name(), true);
            DeviceUtil.updatePhysicalPointRef(Integer.valueOf(nodeAddr), Port.TH1_IN.name(), id);
        } else if (configPoint.getMarkers().contains(Tags.TH2)) {
            if (Th2Val != null && Th2Val.get("id") != null) {
                Log.d(LOG_TAG,"updateConfig ++ delete th2");
                CCUHsApi.getInstance().deleteEntityTree(Th2Val.get("id").toString());
            }
            Log.d(LOG_TAG,"updateConfig  ++  th2");
            String id = createSensorPoint(floorRef, roomRef, "th2", configVal, nodeAddr, equipref);
            DeviceUtil.setPointEnabled(Integer.valueOf(nodeAddr), Port.TH2_IN.name(), true);
            DeviceUtil.updatePhysicalPointRef(Integer.valueOf(nodeAddr), Port.TH2_IN.name(), id);
        } else if (configPoint.getMarkers().contains(Tags.ANALOG1)) {
            if (An1Val != null && An1Val.get("id") != null) {
                Log.d(LOG_TAG,"updateConfig ++ delete an1");
                CCUHsApi.getInstance().deleteEntityTree(An1Val.get("id").toString());
            }
            Log.d(LOG_TAG,"updateConfig ++ an1");
            String id = createSensorPoint(floorRef, roomRef, "analog1", configVal, nodeAddr, equipref);
            DeviceUtil.setPointEnabled(Integer.valueOf(nodeAddr), Port.ANALOG_IN_ONE.name(), true);
            DeviceUtil.updatePhysicalPointRef(Integer.valueOf(nodeAddr), Port.ANALOG_IN_ONE.name(), id);
        } else if (configPoint.getMarkers().contains(Tags.ANALOG2)) {
            if (An2Val != null && An2Val.get("id") != null) {
                Log.d(LOG_TAG,"updateConfig ++ delete an2");
                CCUHsApi.getInstance().deleteEntityTree(An2Val.get("id").toString());
            }
            Log.d(LOG_TAG,"updateConfig ++  an2");
            String id = createSensorPoint(floorRef, roomRef, "analog2", configVal, nodeAddr, equipref);
            DeviceUtil.setPointEnabled(Integer.valueOf(nodeAddr), Port.ANALOG_IN_TWO.name(), true);
            DeviceUtil.updatePhysicalPointRef(Integer.valueOf(nodeAddr), Port.ANALOG_IN_TWO.name(), id);
        }
        CCUHsApi.getInstance().syncEntityTree();
    }

    private static void writePointFromJson(String id, double val, JsonObject msgObject,
                                           CCUHsApi hayStack) {
        try {
            int level = msgObject.get(HayStackConstants.WRITABLE_ARRAY_LEVEL).getAsInt();
            int duration = msgObject.get(HayStackConstants.WRITABLE_ARRAY_DURATION) != null ? msgObject.get(
                    HayStackConstants.WRITABLE_ARRAY_DURATION).getAsInt() : 0;
            hayStack.writePointLocal(id, level, hayStack.getCCUUserName(), val, duration);
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : " + msgObject + " ; " + e.getMessage());
        }
    }

    private static String createSensorPoint(String floorRef, String roomRef, String tag,
                                            int val, String nodeaddr, String equipref) {

        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-SENSE-" + nodeaddr + "-";
        Bundle bundle = new Bundle();
        if (tag.equals("analog1")) {
            bundle = getAnalogBundle(val);
        } else if (tag.equals("analog2")) {
            bundle = getAnalogBundle(val);
        } else if (tag.equals("th1")) {
            bundle = getThermistorBundle(val);
        } else if (tag.equals("th2")) {
            bundle = getThermistorBundle(val);
        }

        String shortDis = bundle.getString("shortDis");
        String unit = bundle.getString("unit");
        String maxVal = bundle.getString("maxVal");
        String minVal = bundle.getString("minVal");
        String[] markers = bundle.getStringArray("markers");

        Point.Builder sensorTag = new Point.Builder()
                .setDisplayName(equipDis + shortDis)
                .setEquipRef(equipref)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setShortDis(shortDis)
                .setHisInterpolate("cov")
                .addMarker("logical").addMarker("zone").addMarker("his").addMarker(tag)
                .addMarker("hyperstat").addMarker("sense")
                .setGroup(String.valueOf(nodeaddr))
                .setMinVal(minVal)
                .setMaxVal(maxVal)
                .setUnit(unit)
                .setTz(tz);
        if (markers != null) {
            for (String marker : markers) {
                sensorTag.addMarker(marker);
            }
        }

        String sensorVariableTagId = CCUHsApi.getInstance().addPoint(sensorTag.build());
        CCUHsApi.getInstance().writeDefaultValById(sensorVariableTagId, 0.0);
        CCUHsApi.getInstance().writeHisValById(sensorVariableTagId, 0.0);

        return sensorVariableTagId;
    }


    private static Bundle getAnalogBundle(int analog) {
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

    private static Bundle getThermistorBundle(int th) {
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
