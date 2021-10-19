package a75f.io.logic.tuners;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;

public class PlcTuners {
    
    public static void addDefaultPlcTuners(CCUHsApi hayStack, String siteRef, String equipRef, String equipDis,
                                           String tz) {
        
        Point propGain = new Point.Builder()
                             .setDisplayName(equipDis+"-"+"proportionalKFactor")
                             .setSiteRef(siteRef)
                             .setEquipRef(equipRef).setHisInterpolate("cov")
                             .addMarker("tuner").addMarker("default").addMarker("pid").addMarker("writable").addMarker("his")
                             .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                             .addMarker("pgain").addMarker("sp")
                             .setTz(tz)
                             .build();
        String pgainId = hayStack.addPoint(propGain);
        hayStack.writePointForCcuUser(pgainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.VAV_PROPORTIONAL_GAIN, 0);
        hayStack.writeHisValById(pgainId, TunerConstants.VAV_PROPORTIONAL_GAIN);
        
        Point integralGain = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"integralKFactor")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker("default").addMarker("pid").addMarker("writable").addMarker("his")
                                 .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                 .addMarker("igain").addMarker("sp")
                                 .setTz(tz)
                                 .build();
        String igainId = hayStack.addPoint(integralGain);
        hayStack.writePointForCcuUser(igainId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.VAV_INTEGRAL_GAIN, 0);
        hayStack.writeHisValById(igainId, TunerConstants.VAV_INTEGRAL_GAIN);
        
        Point integralTimeout = new Point.Builder()
                                    .setDisplayName(equipDis+"-"+"pidIntegralTime")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipRef).setHisInterpolate("cov")
                                    .addMarker("tuner").addMarker("default").addMarker("pid").addMarker("writable").addMarker("his")
                                    .setMinVal("5").setMaxVal("30").setIncrementVal("5").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                    .addMarker("itimeout").addMarker("sp")
                                    .setUnit("m")
                                    .setTz(tz)
                                    .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        hayStack.writePointForCcuUser(iTimeoutId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.VAV_INTEGRAL_TIMEOUT, 0);
        hayStack.writeHisValById(iTimeoutId, TunerConstants.VAV_INTEGRAL_TIMEOUT);
    }
    
    public static void addPlcEquipTuners(CCUHsApi hayStack, String siteRef, String equipdis, String equipref,
                                         String roomRef, String floorRef, String tz){
        
        //addEquipZoneTuners(equipdis, equipref);
        Point propGain = new Point.Builder()
                             .setDisplayName(equipdis+"-"+"proportionalKFactor")
                             .setSiteRef(siteRef)
                             .setEquipRef(equipref)
                             .setRoomRef(roomRef)
                             .setFloorRef(floorRef).setHisInterpolate("cov")
                             .addMarker("tuner").addMarker("pid").addMarker("writable").addMarker("his")
                             .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                             .addMarker("pgain").addMarker("sp")
                             .setTz(tz)
                             .build();
        String pgainId = hayStack.addPoint(propGain);
        BuildingTunerUtil.updateTunerLevels(pgainId, roomRef, hayStack);
        hayStack.writeHisValueByIdWithoutCOV(pgainId, HSUtil.getPriorityVal(pgainId));
        
        Point integralGain = new Point.Builder()
                                 .setDisplayName(equipdis+"-"+"integralKFactor")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipref)
                                 .setRoomRef(roomRef)
                                 .setFloorRef(floorRef).setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker("pid").addMarker("writable").addMarker("his")
                                 .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                 .addMarker("igain").addMarker("sp")
                                 .setTz(tz)
                                 .build();
        String igainId = hayStack.addPoint(integralGain);
        BuildingTunerUtil.updateTunerLevels(igainId, roomRef, hayStack);
        hayStack.writeHisValueByIdWithoutCOV(igainId, HSUtil.getPriorityVal(igainId));
        
        Point integralTimeout = new Point.Builder()
                                    .setDisplayName(equipdis+"-"+"pidIntegralTime")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipref)
                                    .setRoomRef(roomRef)
                                    .setFloorRef(floorRef).setHisInterpolate("cov")
                                    .addMarker("tuner").addMarker("pid").addMarker("writable").addMarker("his")
                                    .setMinVal("5").setMaxVal("30").setIncrementVal("5").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                    .addMarker("itimeout").addMarker("sp")
                                    .setUnit("m")
                                    .setTz(tz)
                                    .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        BuildingTunerUtil.updateTunerLevels(iTimeoutId, roomRef, hayStack);
        hayStack.writeHisValueByIdWithoutCOV(iTimeoutId, HSUtil.getPriorityVal(iTimeoutId));
    }
    
}
