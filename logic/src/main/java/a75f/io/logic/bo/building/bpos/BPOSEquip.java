package a75f.io.logic.bo.building.bpos;

import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Kind;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.ZoneState;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.heartbeat.HeartBeat;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.BPOSTuners;
import a75f.io.logic.tuners.TITuners;

/*
 * created by spoorthidev on 3-August-2021
 */

public class BPOSEquip {

    private static final String LOG_TAG = "BPOSEquip";
    public int mNodeAddr;
    ProfileType mProfileType;
    String mEquipRef = null;
    double currentTemp;
    double humidity;
    double desiredTemp;


    public BPOSEquip(ProfileType type, int node) {
        mNodeAddr = node;
        mProfileType = type;
    }

    public void init() {
        HashMap equip = CCUHsApi.getInstance().read("equip  and group == \"" + mNodeAddr + "\"");

        if (equip.isEmpty()) {
            Log.d(LOG_TAG, "Init Failed : Equip does not exist ");
            return;
        }
        Log.d(LOG_TAG, "Init equip : " + equip.get("id").toString());
        mEquipRef = equip.get("id").toString();
    }

    public void createEntities(BPOSConfiguration config, String floorRef, String roomRef) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-BPOS-" + mNodeAddr;
        String ahuRef = null;
        HashMap systemEquip = CCUHsApi.getInstance().read("equip and system");
        if (systemEquip != null && systemEquip.size() > 0) {
            ahuRef = systemEquip.get("id").toString();
        }

        Log.d(LOG_TAG, "config: " + config.gettempOffset() + " - " + config.getautoAway() + " - " +
                "--" + config.getautoforceOccupied() + " - " + config.getzonePriority());

        Equip.Builder b = new Equip.Builder()
                .setSiteRef(siteRef)
                .setDisplayName(equipDis)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setProfile(mProfileType.name())
                .setPriority(config.getPriority().name())
                .addMarker("equip").addMarker("bpos").addMarker("zone")
                .setAhuRef(ahuRef)
                .setTz(tz)
                .setGroup(String.valueOf(mNodeAddr));
        mEquipRef = CCUHsApi.getInstance().addEquip(b.build());

        BPOSTuners.addEquipTuners(CCUHsApi.getInstance(), siteRef, equipDis, mEquipRef, roomRef,
                floorRef, tz);


        Point currentTemp = new Point.Builder()
                .setDisplayName(equipDis + "-currentTemp")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("zone")
                .addMarker("air").addMarker("temp").addMarker("sensor").addMarker("current")
                .addMarker("his").addMarker("cur").addMarker("logical").addMarker("bpos")
                .setGroup(String.valueOf(mNodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String ctID = CCUHsApi.getInstance().addPoint(currentTemp);
        CCUHsApi.getInstance().writeDefaultValById(ctID, 0.0);
        CCUHsApi.getInstance().writeHisValById(ctID, 0.0);

        Point humidity = new Point.Builder()
                .setDisplayName(equipDis + "-humidity")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("zone")
                .addMarker("humidity").addMarker("sensor").addMarker("his").addMarker("cur")
                .addMarker("logical").addMarker("bpos").addMarker("air")
                .setGroup(String.valueOf(mNodeAddr))
                .setUnit("%")
                .setTz(tz)
                .build();
        String humidityId = CCUHsApi.getInstance().addPoint(humidity);
        CCUHsApi.getInstance().writeDefaultValById(humidityId, 0.0);
        CCUHsApi.getInstance().writeHisValById(humidityId, 0.0);


        Point occupancy = new Point.Builder()
                .setDisplayName(equipDis + "-occupancy")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("zone").addMarker("bpos")
                .addMarker("sensor").addMarker("occupancy").addMarker("his").addMarker("cur")
                .addMarker("logical").addMarker("writable")
                .setGroup(String.valueOf(mNodeAddr))
                .setEnums("off,on")
                .setTz(tz)
                .build();
        String occupancyId = CCUHsApi.getInstance().addPoint(occupancy);
        CCUHsApi.getInstance().writeDefaultValById(occupancyId, 0.0);
        CCUHsApi.getInstance().writeHisValById(occupancyId, 0.0);

        Point zonePriority = new Point.Builder()
                .setDisplayName(equipDis + "-zonePriority")
                .setEquipRef(mEquipRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setSiteRef(siteRef)
                .setHisInterpolate("cov")
                .addMarker("config").addMarker("bpos").addMarker("writable").addMarker("zone")
                .addMarker("priority").addMarker("his")
                .setGroup(String.valueOf(mNodeAddr))
                .setEnums("none,low,normal,high")
                .setTz(tz)
                .build();
        String zonePriorityId = CCUHsApi.getInstance().addPoint(zonePriority);
        CCUHsApi.getInstance().writeDefaultValById(zonePriorityId,
                (double) config.getPriority().ordinal());
        CCUHsApi.getInstance().writeHisValById(zonePriorityId, (double) config.getzonePriority());

        Point temperatureOffset = new Point.Builder()
                .setDisplayName(equipDis + "-temperatureOffset")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("bpos").addMarker("writable").addMarker("zone")
                .addMarker("temperature").addMarker("offset")
                .setGroup(String.valueOf(mNodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String temperatureOffsetId = CCUHsApi.getInstance().addPoint(temperatureOffset);
        CCUHsApi.getInstance().writeDefaultValById(temperatureOffsetId,
                (double) config.gettempOffset());

        Point autoforceoccupied = new Point.Builder()
                .setDisplayName(equipDis + "-autoforceoccupied")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("bpos").addMarker("writable").addMarker("zone")
                .addMarker("autoforceoccupied")
                .setGroup(String.valueOf(mNodeAddr))
                .setEnums("on,off")
                .setTz(tz)
                .build();
        String autoforceoccupiedId = CCUHsApi.getInstance().addPoint(autoforceoccupied);
        CCUHsApi.getInstance().writeDefaultValById(autoforceoccupiedId,
                config.getautoforceOccupied() ? 1.0 : 0.0);

        Point autoforceaway = new Point.Builder()
                .setDisplayName(equipDis + "-autoforceaway")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("bpos").addMarker("writable").addMarker("zone")
                .addMarker("autoforceaway")
                .setGroup(String.valueOf(mNodeAddr))
                .setEnums("on,off")
                .setTz(tz)
                .build();
        String autoforceawayId = CCUHsApi.getInstance().addPoint(autoforceaway);
        CCUHsApi.getInstance().writeDefaultValById(autoforceawayId, config.getautoAway() ? 1.0 :
                0.0);

        Point equipScheduleType = new Point.Builder()
                .setDisplayName(equipDis + "-scheduleType")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("bpos").addMarker("scheduleType")
                .addMarker("writable").addMarker("his")
                .setGroup(String.valueOf(mNodeAddr))
                .setEnums("building,zone,named")
                .setTz(tz)
                .build();
        String equipScheduleTypeId = CCUHsApi.getInstance().addPoint(equipScheduleType);
        CCUHsApi.getInstance().writeDefaultValById(equipScheduleTypeId, 0.0);
        CCUHsApi.getInstance().writeHisValById(equipScheduleTypeId, 0.0);


        Point desiredTemp = new Point.Builder()
                .setDisplayName(equipDis + "-desiredTemp")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("bpos")
                .addMarker("average").addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
                .setGroup(String.valueOf(mNodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String dtId = CCUHsApi.getInstance().addPoint(desiredTemp);

        Point desiredTempCooling = new Point.Builder()
                .setDisplayName(equipDis + "-desiredTempCooling")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("bpos")
                .addMarker("cooling").addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
                .setGroup(String.valueOf(mNodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(desiredTempCooling);

        Point desiredTempHeating = new Point.Builder()
                .setDisplayName(equipDis + "-desiredTempHeating")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("zone").addMarker("air").addMarker("temp").addMarker("desired").addMarker("bpos")
                .addMarker("heating").addMarker("sp").addMarker("writable").addMarker("his").addMarker("userIntent")
                .setGroup(String.valueOf(mNodeAddr))
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        CCUHsApi.getInstance().addPoint(desiredTempHeating);

        Point equipStatus = new Point.Builder()
                .setDisplayName(equipDis + "-equipStatus")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("status").addMarker("bpos").addMarker("his").addMarker("zone")
                .setGroup(String.valueOf(mNodeAddr))
                .setEnums("deadband,cooling,heating,tempdead")
                .setTz(tz)
                .build();
        String equipStatusId = CCUHsApi.getInstance().addPoint(equipStatus);
        CCUHsApi.getInstance().writeHisValById(equipStatusId, 0.0);

        Point equipStatusMessage = new Point.Builder()
                .setDisplayName(equipDis + "-equipStatusMessage")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("status").addMarker("message").addMarker("bpos")
                .addMarker("writable").addMarker("zone")
                .setGroup(String.valueOf(mNodeAddr))
                .setTz(tz)
                .setKind(Kind.STRING)
                .build();
        String equipStatusMessageLd = CCUHsApi.getInstance().addPoint(equipStatusMessage);
        CCUHsApi.getInstance().writeHisValById(equipStatusMessageLd, 0.0);


        Point zoneDynamicPriorityPoint = new Point.Builder()
                .setDisplayName(equipDis + "-zoneDynamicPriority")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("bpos").addMarker("zone").addMarker("dynamic")
                .addMarker("priority").addMarker("writable").addMarker("sp").addMarker("his").addMarker("logical")
                .setGroup(String.valueOf(mNodeAddr))
                .setTz(tz)
                .build();
        String zoneDynamicPriorityPointID =
                CCUHsApi.getInstance().addPoint(zoneDynamicPriorityPoint);
        CCUHsApi.getInstance().writeHisValById(zoneDynamicPriorityPointID, 10.0);


        Point occupancyDetection = new Point.Builder()
                .setDisplayName(equipDis + "-occupancyDetection")
                .setEquipRef(mEquipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("occupancy").addMarker("detection").addMarker("bpos")
                .addMarker("his").addMarker("zone").addMarker("writable")
                .setGroup(String.valueOf(mNodeAddr))
                .setEnums("false,true")
                .setTz(tz)
                .build();
        String occupancyDetectionId = CCUHsApi.getInstance().addPoint(occupancyDetection);
        CCUHsApi.getInstance().writeHisValById(occupancyDetectionId, 0.0);


        String heartBeatId = CCUHsApi.getInstance().addPoint(HeartBeat.getHeartBeatPoint(equipDis
                , mEquipRef,
                siteRef, roomRef, floorRef, mNodeAddr, "bpos", tz, false));

        SmartNode device = new SmartNode(mNodeAddr, siteRef, floorRef, roomRef, mEquipRef);
        device.rssi.setPointRef(heartBeatId);
        device.rssi.setEnabled(true);
        device.currentTemp.setPointRef(ctID);
        device.currentTemp.setEnabled(true);
        device.desiredTemp.setPointRef(dtId);
        device.desiredTemp.setEnabled(true);
        device.addSensor(Port.SENSOR_RH, humidityId);
        device.addSensor(Port.SENSOR_OCCUPANCY, occupancyId);


        device.addPointsToDb();
        setCurrentTemp(0);

        setDesiredTempCooling(74.0);
        setDesiredTemp(72.0);
        setDesiredTempHeating(70.0);
        setHumidity(0);

        // setScheduleStatus("");


        CCUHsApi.getInstance().syncEntityTree();

    }


    public BPOSConfiguration getbposconfiguration() {
        BPOSConfiguration bposconfig = new BPOSConfiguration();
        bposconfig.settempOffset(CCUHsApi.getInstance().readDefaultVal("point and temperature and" +
                " offset and equipRef == \"" + mEquipRef + "\""));
        bposconfig.setzonePriority(CCUHsApi.getInstance().readDefaultVal("point and priority and " +
                "config  and equipRef == \"" + mEquipRef + "\"").intValue());
        bposconfig.setautoforceOccupied(CCUHsApi.getInstance().readDefaultVal("point and " +
                "autoforceoccupied and equipRef == \"" + mEquipRef + "\"") > 0);
        bposconfig.setautoAway(CCUHsApi.getInstance().readDefaultVal("point and autoforceaway and" +
                " equipRef == \"" + mEquipRef + "\"") > 0);
        Log.d(LOG_TAG,
                "config: " + bposconfig.gettempOffset() + " - " + bposconfig.getautoAway() + " - " +
                        "--" + bposconfig.getautoforceOccupied() + " - " + bposconfig.getzonePriority());
        return bposconfig;
    }

    public void update(ProfileType type, int node, BPOSConfiguration config, String floorRef,
                       String roomRef) {

        HashMap tempOffset = CCUHsApi.getInstance().read("point and temperature and offset and " +
                "equipRef == \"" + mEquipRef + "\"");
        HashMap zonepriority = CCUHsApi.getInstance().read("point and priority and config and " +
                "equipRef == \"" + mEquipRef + "\"");
        HashMap autoaway = CCUHsApi.getInstance().read("point and autoforceaway and config and " +
                "equipRef == \"" + mEquipRef + "\"");
        HashMap autooccupied = CCUHsApi.getInstance().read("point and autoforceoccupied and " +
                "config and equipRef == \"" + mEquipRef + "\"");

        CCUHsApi.getInstance().writeDefaultValById(tempOffset.get("id").toString(),
                config.gettempOffset());
        CCUHsApi.getInstance().writeDefaultValById(zonepriority.get("id").toString(),
                (double) config.getzonePriority());
        CCUHsApi.getInstance().writeDefaultValById(autoaway.get("id").toString(),
                config.getautoAway() ? 1.0 : 0.0);
        CCUHsApi.getInstance().writeDefaultValById(autooccupied.get("id").toString(),
                config.getautoforceOccupied() ? 1.0 : 0.0);

        CCUHsApi.getInstance().syncEntityTree();
    }


    public double getCurrentTemp() {
        currentTemp = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and sensor" +
                " " +
                "and current and group == \"" + mNodeAddr + "\"");
        return currentTemp;
    }

    public void setCurrentTemp(double roomTemp) {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and temp and sensor and" +
                " current and group == \"" + mNodeAddr + "\"", roomTemp);
        this.currentTemp = roomTemp;
    }

    public double getHumidity() {
        humidity = CCUHsApi.getInstance().readHisValByQuery("point and air and humidity and " +
                "sensor " +
                "and current and group == \"" + mNodeAddr + "\"");
        return humidity;
    }

    public void setHumidity(double humidity) {
        CCUHsApi.getInstance().writeHisValByQuery("point and air and humidity and sensor and " +
                "current and group == \"" + mNodeAddr + "\"", humidity);
        this.humidity = humidity;
    }

    public double getDesiredTemp() {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired" +
                " and average and sp and group == \"" + mNodeAddr + "\"");
        String id = ((HashMap) points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        desiredTemp = CCUHsApi.getInstance().readDefaultValById(id);
        return desiredTemp;
    }

    public void setDesiredTemp(double desiredTemp) {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired " +
                "and average and sp and group == \"" + mNodeAddr + "\"");
        String id = ((HashMap) points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(id, desiredTemp);
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
        this.desiredTemp = desiredTemp;
    }

    public double getDesiredTempCooling() {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired " +
                "and cooling and sp and group == \"" + mNodeAddr + "\"");
        String id = ((HashMap) points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0) {
            for (int l = 1; l <= values.size(); l++) {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }

    public void setDesiredTempCooling(double desiredTemp) {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired" +
                " and cooling and sp and group == \"" + mNodeAddr + "\"");
        String id = ((HashMap) points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(id),
                HayStackConstants.DEFAULT_POINT_LEVEL,
                HNum.make(desiredTemp), HNum.make(0));

        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
    }

    public double getDesiredTempHeating() {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired " +
                "and heating and sp and group == \"" + mNodeAddr + "\"");
        String id = ((HashMap) points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0) {
            for (int l = 1; l <= values.size(); l++) {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }

    public void setDesiredTempHeating(double desiredTemp) {
        ArrayList points = CCUHsApi.getInstance().readAll("point and air and temp and desired" +
                " and heating and sp and group == \"" + mNodeAddr + "\"");
        String id = ((HashMap) points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().pointWriteForCcuUser(HRef.copy(id),
                HayStackConstants.DEFAULT_POINT_LEVEL,
                HNum.make(desiredTemp), HNum.make(0));
        CCUHsApi.getInstance().writeHisValById(id, desiredTemp);
    }

    public void setScheduleStatus(String status) {
        ArrayList points = CCUHsApi.getInstance().readAll("point and scheduleStatus and group " +
                "== \"" + mNodeAddr + "\"");
        String id = ((HashMap) points.get(0)).get("id").toString();
        if (id == null || id == "") {
            throw new IllegalArgumentException();
        }
        CCUHsApi.getInstance().writeDefaultValById(id, status);
    }

    public void setStatus(double status, boolean emergency) {
        if (getStatus() != status) {
            CCUHsApi.getInstance().writeHisValByQuery("point and status and his and group == \"" + mNodeAddr + "\"", status);
        }

        String message;
        if (emergency) {
            message = (status == 0 ? "Recirculating Air" : status == 1 ? "Emergency Cooling" :
                    "Emergency Heating");
        } else {
            if (ScheduleProcessJob.getSystemOccupancy() == Occupancy.PRECONDITIONING) {
                message = "In Preconditioning ";
            } else {
                message = (status == 0 ? "Recirculating Air" : status == 1 ? "Cooling Space" :
                        "Warming Space");
            }
        }

        String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message" +
                " and writable and group == \"" + mNodeAddr + "\"");
        if (!curStatus.equals(message)) {
            CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and" +
                    " group == \"" + mNodeAddr + "\"", message);
        }
    }

    public double getStatus() {
        return CCUHsApi.getInstance().readHisValByQuery("point and status and his and group == " +
                "\"" + mNodeAddr + "\"");
    }

    public void setStatus(double status) {

        ZoneState state = ZoneState.values()[((int) status)];

        Log.d("Spoo_LOG", "state.toString() equip" + state.toString());

        CCUHsApi.getInstance().writeDefaultVal("point and status and message " +
                " and group == \"" + mNodeAddr + "\"", state.toString());

    }

}
