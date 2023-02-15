package a75f.io.logic.tuners;

import android.util.Log;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.haystack.device.HyperStatDevice;

public class VrvTuners {
    
    public static void addDefaultVrvTuners(CCUHsApi hayStack, String siteRef, String equipRef, String equipDis,
                                           String tz){
        
        HashMap tuner = CCUHsApi.getInstance().read("point and tuner and default and vrv");
        if (tuner != null && tuner.size() > 0) {
            CcuLog.d(L.TAG_CCU_SYSTEM, "Default VRV Tuner points already exist");
            return;
        }
        
        Point coolingDb = new Point.Builder()
                              .setDisplayName(equipDis+"-VRV-"+"coolingDeadband")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("default").addMarker("vrv").addMarker("writable").addMarker("his")
                              .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                              .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                              .setUnit("\u00B0F")
                              .setTz(tz)
                              .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        hayStack.writePointForCcuUser(coolingDbId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.VAV_COOLING_DB, 0);
        hayStack.writeHisValById(coolingDbId, TunerConstants.VAV_COOLING_DB);
        
        Point coolingDbMultiplier = new Point.Builder()
                                        .setDisplayName(equipDis+"-VRV-"+"coolingDeadbandMultiplier")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("default").addMarker("vrv").addMarker("writable").addMarker("his")
                                        .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                        .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String coolingDbMultiplierId = hayStack.addPoint(coolingDbMultiplier);
        hayStack.writePointForCcuUser(coolingDbMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.VAV_COOLING_DB_MULTPLIER, 0);
        hayStack.writeHisValById(coolingDbMultiplierId, TunerConstants.VAV_COOLING_DB_MULTPLIER);
        
        Point heatingDb = new Point.Builder()
                              .setDisplayName(equipDis+"-VRV-"+"heatingDeadband")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("default").addMarker("vrv").addMarker("writable").addMarker("his")
                              .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                              .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                              .setUnit("\u00B0F")
                              .setTz(tz)
                              .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        hayStack.writePointForCcuUser(heatingDbId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.VAV_HEATING_DB, 0);
        hayStack.writeHisValById(heatingDbId, TunerConstants.VAV_HEATING_DB);
        
        Point heatingDbMultiplier = new Point.Builder()
                                        .setDisplayName(equipDis+"-VRV-"+"heatingDeadbandMultiplier")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("default").addMarker("vrv").addMarker("writable").addMarker("his")
                                        .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                        .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String heatingDbMultiplierId = hayStack.addPoint(heatingDbMultiplier);
        hayStack.writePointForCcuUser(heatingDbMultiplierId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.VAV_HEATING_DB_MULTIPLIER, 0);
        hayStack.writeHisValById(heatingDbMultiplierId, TunerConstants.VAV_HEATING_DB_MULTIPLIER);
    }
    
    public static void addEquipVrvTuners(CCUHsApi hayStack, String siteRef, String equipdis, String equipref,
                                         String roomRef, String floorRef, String tz) {
        
        Log.d("CCU", "addEquipVrvTuners for " + equipdis);
        //ZoneTuners.addZoneTunersForEquip(hayStack, siteRef, equipdis, equipref, roomRef, floorRef, tz);
        
        Point coolingDb = new Point.Builder()
                              .setDisplayName(equipdis+"-"+"coolingDeadband")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipref)
                              .setRoomRef(roomRef)
                              .setFloorRef(floorRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("vrv").addMarker("writable").addMarker("his")
                              .addMarker("cooling").addMarker("deadband").addMarker("base").addMarker("sp")
                              .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                              .setTz(tz)
                              .setUnit("\u00B0F")
                              .build();
        String coolingDbId = hayStack.addPoint(coolingDb);
        BuildingTunerUtil.updateTunerLevels(coolingDbId, roomRef, hayStack);
        hayStack.writeHisValById(coolingDbId, HSUtil.getPriorityVal(coolingDbId));
        
        Point coolingDbMultiplier = new Point.Builder()
                                        .setDisplayName(equipdis+"-"+"coolingDeadbandMultiplier")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .setRoomRef(roomRef)
                                        .setFloorRef(floorRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("vrv").addMarker("writable").addMarker("his")
                                        .addMarker("cooling").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                        .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String coolingDbMultiplierId = hayStack.addPoint(coolingDbMultiplier);
        BuildingTunerUtil.updateTunerLevels(coolingDbMultiplierId, roomRef, hayStack);
        hayStack.writeHisValById(coolingDbMultiplierId, HSUtil.getPriorityVal(coolingDbMultiplierId));
        
        Point heatingDb = new Point.Builder()
                              .setDisplayName(equipdis+"-"+"heatingDeadband")
                              .setSiteRef(siteRef)
                              .setEquipRef(equipref)
                              .setRoomRef(roomRef)
                              .setFloorRef(floorRef).setHisInterpolate("cov")
                              .addMarker("tuner").addMarker("vrv").addMarker("writable").addMarker("his")
                              .addMarker("heating").addMarker("deadband").addMarker("base").addMarker("sp")
                              .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                              .setTz(tz)
                              .setUnit("\u00B0F")
                              .build();
        String heatingDbId = hayStack.addPoint(heatingDb);
        BuildingTunerUtil.updateTunerLevels(heatingDbId, roomRef, hayStack);
        hayStack.writeHisValById(heatingDbId, HSUtil.getPriorityVal(heatingDbId));
        
        Point heatingDbMultiplier = new Point.Builder()
                                        .setDisplayName(equipdis+"-"+"heatingDeadbandMultiplier")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipref)
                                        .setRoomRef(roomRef)
                                        .setFloorRef(floorRef).setHisInterpolate("cov")
                                        .addMarker("tuner").addMarker("vrv").addMarker("writable").addMarker("his")
                                        .addMarker("heating").addMarker("deadband").addMarker("multiplier").addMarker("sp")
                                        .setMinVal("0").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String heatingDbMultiplierId = hayStack.addPoint(heatingDbMultiplier);
        BuildingTunerUtil.updateTunerLevels(heatingDbMultiplierId, roomRef, hayStack);
        hayStack.writeHisValById(heatingDbMultiplierId, HSUtil.getPriorityVal(heatingDbMultiplierId));
     }

    public static void addVRVTunersAndSensorPoints(CCUHsApi hayStack, String siteRef, String displayName, String equipref, String roomRef,
                                            String floorRef, String tz, String nodeAddress) {

        Point autoAwayTime = new Point.Builder()
                .setDisplayName(displayName + "-autoAwayTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("writable").addMarker("his")
                .setMinVal("0").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                .addMarker("zone").addMarker("auto").addMarker("away").addMarker("time").addMarker("sp")
                .setUnit("m")
                .setTz(tz)
                .build();
        String autoAwayTimeId = hayStack.addPoint(autoAwayTime);
        BuildingTunerUtil.updateTunerLevels(autoAwayTimeId, roomRef, hayStack);
        hayStack.writeHisValById(autoAwayTimeId, HSUtil.getPriorityVal(autoAwayTimeId));

        Point forcedOccupiedTime = new Point.Builder()
                .setDisplayName(displayName+"-forcedOccupiedTime")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("tuner").addMarker("writable").addMarker("his")
                .setMinVal("0").setMaxVal("300").setIncrementVal("1").setTunerGroup(TunerConstants.TIMER_TUNER)
                .addMarker("zone").addMarker("forced").addMarker("occupied").addMarker("time").addMarker("sp")
                .setUnit("m")
                .setTz(tz)
                .build();
        String forcedOccupiedTimeId = hayStack.addPoint(forcedOccupiedTime);
        BuildingTunerUtil.updateTunerLevels(forcedOccupiedTimeId, roomRef, hayStack);
        hayStack.writeHisValById(forcedOccupiedTimeId, 2.0);

        Point autoAwaySetback = new  Point.Builder()
                .setDisplayName(displayName + "-autoAwaySetback")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef).setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("tuner").addMarker("writable").addMarker("his")
                .addMarker("his")
                .addMarker("zone").addMarker("auto").addMarker("away").addMarker("setback")
                .addMarker("sp")
                .setMinVal("0").setMaxVal("20").setIncrementVal("1")
                .setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .setUnit("\u00B0F")
                .setTz(tz)
                .build();
        String autoAwaySetbackId = hayStack.addPoint(autoAwaySetback);
        hayStack.writePointForCcuUser(autoAwaySetbackId,TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,2.0,0);
        hayStack.writeHisValById(autoAwaySetbackId, 2.0);

        Point occupancyDetectionPoint = new Point.Builder()
                .setDisplayName(displayName + "-occupancyDetection")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("occupancy").addMarker("detection").addMarker("sp").addMarker("vrv")
                .addMarker("his").addMarker("zone")
                .setEnums("false,true")
                .setGroup(nodeAddress.toString())
                .setTz(tz)
                .build();
        String occupancyDetectionPointID = hayStack.addPoint(occupancyDetectionPoint);
        hayStack.writeHisValueByIdWithoutCOV(occupancyDetectionPointID, 0.0);

        Point occupancySensor = new Point.Builder()
                .setDisplayName(displayName + "-occupancySensor")
                .setSiteRef(siteRef)
                .setEquipRef(equipref)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef).setHisInterpolate("cov")
                .addMarker("vrv").addMarker("occupancy").addMarker("logical").addMarker("zone")
                .addMarker("his").addMarker("cur").addMarker("sensor").addMarker("standalone")
                .setEnums("false,true")
                .setGroup(nodeAddress.toString())
                .setTz(tz)
                .build();
        String occupancySensorId = hayStack.addPoint(occupancySensor);
        hayStack.writeHisValById(occupancySensorId, 0.0);

        HyperStatDevice device = new HyperStatDevice(Integer.parseInt(nodeAddress));
        device.addSensor(Port.SENSOR_OCCUPANCY, occupancySensorId);

    }

    public static void createOccupancyPoints(CCUHsApi hayStack, String siteRef, String displayName,
                                             String equipref, String roomRef, String floorRef, String tz,
                                             String nodeAddr, double autoForcedOccupiedEnabled, double autoAwayEnabled) {
        Point autoForcedOccupiedPoint = new Point.Builder()
                .setDisplayName(displayName +"-autoForceOccupied")
                .setEquipRef(equipref)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("writable").addMarker("zone")
                .addMarker("auto").addMarker("occupancy").addMarker("enabled")
                .addMarker("control").addMarker("forced").addMarker("his")
                .addMarker("cmd").setHisInterpolate("cov").setEnums("off,on").addMarker(Tags.STANDALONE)
                .addMarker("vrv")
                .setGroup(nodeAddr)
                .setTz(tz)
                .build();
        String autoForcedOccupiedPointId = hayStack.addPoint(autoForcedOccupiedPoint);
        hayStack.writeDefaultValById(autoForcedOccupiedPointId,autoForcedOccupiedEnabled);
        hayStack.writeHisValById(autoForcedOccupiedPointId,autoForcedOccupiedEnabled);

        Point autoAwayPoint = new Point.Builder()
                .setDisplayName(displayName + "-autoAway")
                .setEquipRef(equipref)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("config").addMarker("writable").addMarker("zone")
                .addMarker("auto").addMarker("away").addMarker("enabled")
                .addMarker("control").addMarker("his").addMarker("cmd")
                .setHisInterpolate("cov").setEnums("off,on").addMarker(Tags.STANDALONE)
                .addMarker("vrv")
                .setGroup(nodeAddr.toString())
                .setTz(tz)
                .build();
        String autoAwayPointId = hayStack.addPoint(autoAwayPoint);
        hayStack.writeDefaultValById(autoAwayPointId,autoAwayEnabled);
        hayStack.writeHisValById(autoAwayPointId, autoAwayEnabled);
    }
}
