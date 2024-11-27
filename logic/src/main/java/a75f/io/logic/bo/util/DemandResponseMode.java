package a75f.io.logic.bo.util;

import com.google.gson.JsonObject;
import java.util.HashMap;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Kind;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.Domain;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.logic.CCUEquipConfiguration;

import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.schedules.occupancy.DemandResponse;
import a75f.io.logic.interfaces.ZoneDataInterface;
import a75f.io.logic.tuners.BuildingTunerUtil;
import a75f.io.logic.tuners.TunerConstants;

public class DemandResponseMode {
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
        if(DemandResponse.isDRModeActivated()){
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
        }
        if (isDRActivationPoint(pointEntity)) {
            CcuLog.i(L.TAG_CCU_DR_MODE,"Handle DR Activation message ");
            boolean isDREnrollmentEnabled = msgObject.get("val").getAsInt() == 1;
            CcuLog.i(L.TAG_CCU_DR_MODE,"isDRActivationPoint "+isDREnrollmentEnabled);
            setDRModeActivationStatus(isDREnrollmentEnabled);
            zoneDataInterface.refreshScreen("",false);
        }
    }

    public static void handleDRActivationConfiguration(boolean isDREnrollmentEnabled, CCUHsApi ccuHsApi) {
        CCUEquipConfiguration ccuEquipConfiguration = new CCUEquipConfiguration(null, ccuHsApi);
        ccuEquipConfiguration.saveDemandResponseEnrollmentStatus(isDREnrollmentEnabled);
        setDREnrollmentStatus(isDREnrollmentEnabled);
    }

    public static boolean isDREnrollmentPoint(HashMap<Object, Object> demandResponseEnrollment) {
        return   ((demandResponseEnrollment.containsKey("domainName")) &&
                (demandResponseEnrollment.get("domainName").toString().equals(DomainName.demandResponseEnrollment)));
    }
    public static boolean isDRActivationPoint(HashMap<Object, Object> demandResponseEnrollment) {
        return ((demandResponseEnrollment.containsKey("domainName")) &&
                (demandResponseEnrollment.get("domainName").toString().equals(DomainName.demandResponseActivation)));
    }
    public static boolean isDemandResponseConfigPoint(HashMap<Object, Object> pointEntity) {
        return isDREnrollmentPoint(pointEntity) || isDRActivationPoint(pointEntity);
    }

    public static boolean isDREnrollmentSelected() {
        return Domain.readDefaultValByDomain(DomainName.demandResponseEnrollment) > 0;
    }
    public static void setDREnrollmentStatus(boolean enabled) {
        Domain.writeDefaultValByDomain(DomainName.demandResponseEnrollment, enabled ? 1.0 : 0);
        Domain.writeHisValByDomain(DomainName.demandResponseEnrollment, enabled ? 1.0 : 0);
    }

    public static boolean isDRModeActivated() {
        return Domain.readDefaultValByDomain(DomainName.demandResponseActivation) > 0;
    }
    public static void setDRModeActivationStatus(boolean enabled) {
        Domain.writeDefaultValByDomain(DomainName.demandResponseActivation, enabled ? 1.0 : 0);
    }
    public static double getCoolingSetBack(double coolingDT, double buildingLimitMin) {
        return Math.min(coolingDT, buildingLimitMin);
    }
    public static double getHeatingSetBack(double heatingDT, double buildingLimitMax) {
        return Math.max(heatingDT, buildingLimitMax);
    }
}
