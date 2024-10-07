package a75f.io.logic.bo.util;

import com.google.gson.JsonObject;
import java.util.HashMap;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Kind;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.Domain;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.schedules.occupancy.DemandResponse;
import a75f.io.logic.interfaces.ZoneDataInterface;
import a75f.io.logic.tuners.BuildingTunerUtil;
import a75f.io.logic.tuners.TunerConstants;

public class DemandResponseMode {
    static String demandResponseEnrollmentQuery = "demand and response and enable";
    static String demandResponseActivationQuery = "demand and response and activation";
    private static void createDemandResponseActivationPoint(CCUHsApi ccuHsApi) {
        CcuLog.i(L.TAG_CCU_DR_MODE, "createDemandResponseActivationPoint:  ");
        HashMap<Object, Object> equip = ccuHsApi.readEntity("equip and system and not modbus and not connectModule");
        Point demandResponseActivation = new Point.Builder().setDisplayName(equip.get("dis").toString()
                        + "-" + "demandResponseActivation").setSiteRef(ccuHsApi.getSiteIdRef().toString())
                .setEquipRef(equip.get("id").toString()).setHisInterpolate("cov").addMarker("system")
                .addMarker("userIntent").addMarker("writable").addMarker("his").addMarker("demand")
                .addMarker("response").setEnums("off,on").setTz(ccuHsApi.getTimeZone())
                .addMarker("activation").addMarker("config").addMarker("cur").addMarker("sp").build();
        String demandResponseActivationId = ccuHsApi.addPoint(demandResponseActivation);
        ccuHsApi.writePointForCcuUser(demandResponseActivationId, TunerConstants.UI_DEFAULT_VAL_LEVEL, 0.0, 0);
        ccuHsApi.writeHisValById(demandResponseActivationId, 0.0);
        ccuHsApi.scheduleSync();
    }
    public void createDemandResponseEnrollmentPoint(String equipDis, String siteRef, String equipRef, String tz, CCUHsApi hayStack) {
        Point demandResponseEnrollment = new Point.Builder().setDisplayName(equipDis + "-" + "demandResponseEnrollment")
                .setSiteRef(siteRef).setEquipRef(equipRef).setHisInterpolate("cov").addMarker("system")
                .addMarker("writable").addMarker("his").addMarker("demand")
                .addMarker("response").setEnums("off,on").setTz(hayStack.getTimeZone()).addMarker("enable")
                .addMarker("config").addMarker("cur").addMarker("sp").build();
        String demandResponseEnrollmentId = CCUHsApi.getInstance().addPoint(demandResponseEnrollment);
        hayStack.writePointForCcuUser(demandResponseEnrollmentId, TunerConstants.UI_DEFAULT_VAL_LEVEL, 0.0, 0);
        hayStack.writeHisValById(demandResponseEnrollmentId, 0.0);
        hayStack.scheduleSync();
    }
    public static void createDemandResponseSetBackTuner(CCUHsApi hayStack, String equipRef,
                                                        String equipDis, boolean isSystem,
                                                        String roomRef, String floorRef ){
        Point.Builder demandResponseSetback   = new Point.Builder()
                .setDisplayName(equipDis+"-"+"Demand Response Setback")
                .setSiteRef(hayStack.getSiteIdRef().toString())
                .setEquipRef(equipRef).setHisInterpolate("cov")
                .addMarker("demand").addMarker("response").addMarker("setback").addMarker("sp")
                .addMarker("tuner").addMarker("cur").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                .addMarker("writable").setMinVal("0").setMaxVal("20").setIncrementVal("1").addMarker(Tags.HIS)
                .setUnit("\u00B0F").setKind(Kind.NUMBER)
                .setTz(hayStack.getTimeZone());
        if(isSystem){
            demandResponseSetback.addMarker(Tags.SYSTEM);
        }else{
            demandResponseSetback.setFloorRef(floorRef);
            demandResponseSetback.setRoomRef(roomRef);
        }
        Point demandResponseSetbackPoint = demandResponseSetback.build();
        String demandResponseSetbackId = hayStack.addPoint(demandResponseSetbackPoint);
        if(!isSystem) {
            BuildingTunerUtil.updateTunerLevels(demandResponseSetbackId, roomRef, hayStack);
        }
        hayStack.writePointForCcuUser(demandResponseSetbackId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,2.0, 0);
        hayStack.writeHisValById(demandResponseSetbackId, 2.0);
    }
    // TODO : USE DOMAIN NAME ONCE FOR ALL SYSTEM PROFILES DM-INTEGRATION IS DONE
    public static double getSystemLevelDemandResponseSetBackIfActive(CCUHsApi hayStack) {
        if(DemandResponse.isDRModeActivated(hayStack)){
            return CCUHsApi.getInstance().readPointPriorityValByQuery("demand and response " +
                    "and setback and equipRef == \""+Domain.systemEquip.getEquipRef()+"\"");
        }
        return 0;
    }
    public static void handleDRMessageUpdate(HashMap<Object, Object> pointEntity, CCUHsApi hayStack,
                                             JsonObject msgObject, ZoneDataInterface zoneDataInterface) {
        CcuLog.i(L.TAG_CCU_DR_MODE,"Handle DR message ");
        if(isDREnrollmentPoint(pointEntity)){
            CcuLog.i(L.TAG_CCU_DR_MODE,"Handle DR Enrollment message ");
            boolean isDREnrollmentEnabled = msgObject.get("val").getAsInt() == 1;
            CcuLog.i(L.TAG_CCU_DR_MODE,"isDREnrollmentEnabled "+isDREnrollmentEnabled);
            handleDRActivationConfiguration(isDREnrollmentEnabled, hayStack);
            setDREnrollmentStatus(hayStack, isDREnrollmentEnabled);
        }
        if (isDRActivationPoint(pointEntity)) {
            CcuLog.i(L.TAG_CCU_DR_MODE,"Handle DR Activation message ");
            boolean isDREnrollmentEnabled = msgObject.get("val").getAsInt() == 1;
            CcuLog.i(L.TAG_CCU_DR_MODE,"isDRActivationPoint "+isDREnrollmentEnabled);
            setDRModeActivationStatus(hayStack, isDREnrollmentEnabled);
            zoneDataInterface.refreshScreen("",false);
        }
    }

    public static void handleDRActivationConfiguration(boolean isDRActivationEnabled, CCUHsApi ccuHsApi) {
        if (isDRActivationEnabled) {
            createDemandResponseActivationPoint(ccuHsApi);
        } else {
            HashMap<Object, Object> demandResponseMode = ccuHsApi.readEntity(demandResponseActivationQuery);
            if(!demandResponseMode.isEmpty()) {
                ccuHsApi.deleteEntity(demandResponseMode.get("id").toString());
            }
            ccuHsApi.scheduleSync();
        }
    }

    public static boolean isDREnrollmentPoint(HashMap<Object, Object> demandResponseEnrollment) {
        if (demandResponseEnrollment.containsKey("domainName")
                && demandResponseEnrollment.get("domainName").toString().equals("demandResponseEnrollment")) {
            return true;
        }
        return demandResponseEnrollment.containsKey("demand") && demandResponseEnrollment.
                containsKey("response") && demandResponseEnrollment.containsKey("enable")
                && demandResponseEnrollment.containsKey("system");
    }
    public static boolean isDRActivationPoint(HashMap<Object, Object> demandResponseEnrollment) {
        return demandResponseEnrollment.containsKey("demand") && demandResponseEnrollment.
                containsKey("response") && demandResponseEnrollment.containsKey("activation")
                && demandResponseEnrollment.containsKey("system");
    }
    public static boolean isDemandResponseConfigPoint(HashMap<Object, Object> pointEntity) {
        return pointEntity.containsKey("demand") && pointEntity.
                containsKey("response") && !pointEntity.containsKey("tuner");
    }

    public static boolean isDREnrollmentSelected(CCUHsApi hayStack) {
        return hayStack.readDefaultVal(demandResponseEnrollmentQuery) > 0;
    }
    public static void setDREnrollmentStatus(CCUHsApi hayStack, boolean enabled) {
        hayStack.writeDefaultVal(demandResponseEnrollmentQuery, enabled ?
                1.0: 0);
        hayStack.writeHisValByQuery(demandResponseEnrollmentQuery, enabled ?
                1.0: 0);
    }

    public static boolean isDRModeActivated(CCUHsApi hayStack) {
        return hayStack.readDefaultVal(demandResponseActivationQuery) > 0;
    }
    public static void setDRModeActivationStatus(CCUHsApi hayStack, boolean enabled) {
        hayStack.writeDefaultVal(demandResponseActivationQuery, enabled ?
                1.0: 0);
        hayStack.writeHisValByQuery(demandResponseActivationQuery, enabled ?
                1.0: 0);
    }
    public static double getCoolingSetBack(double coolingDT, double buildingLimitMin) {
        return Math.min(coolingDT, buildingLimitMin);
    }
    public static double getHeatingSetBack(double heatingDT, double buildingLimitMax) {
        return Math.max(heatingDT, buildingLimitMax);
    }
}
