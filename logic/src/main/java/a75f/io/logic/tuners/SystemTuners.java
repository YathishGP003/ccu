package a75f.io.logic.tuners;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;

import static a75f.io.logic.tuners.TunerConstants.OUTSIDE_TEMP_COOLING_LOCKOUT_DEFAULT;
import static a75f.io.logic.tuners.TunerConstants.OUTSIDE_TEMP_HEATING_LOCKOUT_DEFAULT;

public class SystemTuners {
    
    /**
     * When VAV/DAB system equip is created , this will be called to create PI related tuners on the SystemEquip.
     * And then copies all the levels present in BuildingTuner equip.
     */
    public static void addPITuners(String equipRef, String tunerGroup, String typeTag, CCUHsApi hayStack) {
        
        HashMap<Object, Object> equip = hayStack.readMapById(equipRef);
        String equipDis = equip.get("dis").toString();
        String siteRef = equip.get("siteRef").toString();
        Point propGain = new Point.Builder()
                             .setDisplayName(equipDis+"-"+"proportionalKFactor")
                             .setSiteRef(siteRef)
                             .setEquipRef(equipRef)
                             .setHisInterpolate("cov")
                             .addMarker("tuner").addMarker(typeTag).addMarker("writable").addMarker("his")
                             .addMarker("pgain").addMarker("sp").addMarker("system")
                             .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(tunerGroup)
                             .setTz(hayStack.getTimeZone())
                             .build();
        String pgainId = hayStack.addPoint(propGain);
        BuildingTunerUtil.copyFromBuildingTuner(pgainId, TunerUtil.getQueryString(propGain), hayStack);
        hayStack.writeHisValById(pgainId, HSUtil.getPriorityVal(pgainId));
    
        Point integralGain = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"integralKFactor")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .setHisInterpolate("cov")
                                 .addMarker("tuner").addMarker(typeTag).addMarker("writable").addMarker("his")
                                 .addMarker("igain").addMarker("sp").addMarker("system")
                                 .setMinVal("0.1").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(tunerGroup)
                                 .setTz(hayStack.getTimeZone())
                                 .build();
        String igainId = hayStack.addPoint(integralGain);
        BuildingTunerUtil.copyFromBuildingTuner(igainId, TunerUtil.getQueryString(integralGain), hayStack);
        hayStack.writeHisValById(igainId, HSUtil.getPriorityVal(igainId));
    
        Point propSpread = new Point.Builder()
                               .setDisplayName(equipDis+"-"+"temperatureProportionalRange")
                               .setSiteRef(siteRef)
                               .setEquipRef(equipRef)
                               .setHisInterpolate("cov")
                               .addMarker("tuner").addMarker(typeTag).addMarker("writable").addMarker("his")
                               .addMarker("pspread").addMarker("sp").addMarker("system")
                               .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(tunerGroup)
                               .setTz(hayStack.getTimeZone())
                               .build();
        String pSpreadId = hayStack.addPoint(propSpread);
        BuildingTunerUtil.copyFromBuildingTuner(pSpreadId, TunerUtil.getQueryString(propSpread), hayStack);
        hayStack.writeHisValById(pSpreadId, HSUtil.getPriorityVal(pSpreadId));
    
        Point integralTimeout = new Point.Builder()
                                    .setDisplayName(equipDis+"-"+"temperatureIntegralTime")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipRef)
                                    .setHisInterpolate("cov")
                                    .addMarker("tuner").addMarker(typeTag).addMarker("writable").addMarker("his")
                                    .addMarker("itimeout").addMarker("sp").addMarker("system")
                                    .setMinVal("1").setMaxVal("60").setIncrementVal("1").setTunerGroup(tunerGroup)
                                    .setUnit("m")
                                    .setTz(hayStack.getTimeZone())
                                    .build();
        String iTimeoutId = hayStack.addPoint(integralTimeout);
        BuildingTunerUtil.copyFromBuildingTuner(iTimeoutId, TunerUtil.getQueryString(integralTimeout), hayStack);
        hayStack.writeHisValById(iTimeoutId, HSUtil.getPriorityVal(iTimeoutId));
    }
    
    
    private static String addCoolingTempLockoutPoint(CCUHsApi hayStack, String siteRef, String equipRef,
                                              String equipDis, String tz, String tunerTypeTag, boolean isDefault) {
        Point.Builder outsideTempCoolingLockout  =
            new Point.Builder().setDisplayName(equipDis + "-"+tunerTypeTag.toUpperCase()+"-" + "outsideTempCoolingLockout")
                               .setSiteRef(siteRef)
                               .setEquipRef(equipRef)
                               .setHisInterpolate("cov")
                               .addMarker(Tags.TUNER).addMarker(tunerTypeTag)
                               .addMarker(Tags.WRITABLE).addMarker(Tags.HIS)
                               .addMarker(Tags.OUTSIDE_TEMP).addMarker(Tags.COOLING)
                               .addMarker(Tags.LOCKOUT).addMarker(Tags.SP)
                               .setMinVal("0")
                               .setMaxVal("70")
                               .setIncrementVal("1")
                               .setUnit("\u00B0F")
                               .setTunerGroup(tunerTypeTag.toUpperCase())
                               .setTz(tz);
    
        if (isDefault) {
            outsideTempCoolingLockout.addMarker(Tags.DEFAULT);
        } else {
            outsideTempCoolingLockout.addMarker(Tags.SYSTEM);
        }
        return hayStack.addPoint(outsideTempCoolingLockout.build());
    }
    
    public static String createCoolingTempLockoutPoint(CCUHsApi hayStack, String siteRef, String equipRef,
                                                     String equipDis, String tz, String tunerTypeTag, boolean isDefault) {
        
        
        String outsideTempCoolingLockoutId = addCoolingTempLockoutPoint(hayStack, siteRef, equipRef, equipDis,
                                                                        tz,tunerTypeTag, isDefault);
        if (isDefault) {
            hayStack.writePointForCcuUser(outsideTempCoolingLockoutId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,
                                          OUTSIDE_TEMP_COOLING_LOCKOUT_DEFAULT, 0);
            hayStack.writeHisValById(outsideTempCoolingLockoutId, OUTSIDE_TEMP_COOLING_LOCKOUT_DEFAULT);
        } else {
    
            HashMap<Object, Object> defaultCoolingLockoutTempId =
                hayStack.readEntity("point and tuner and "+tunerTypeTag+" and " +
                                    "default and outsideTemp and cooling and lockout");
    
            ArrayList<HashMap> defaultCoolingLockoutTempPointArr =
                hayStack.readPoint(defaultCoolingLockoutTempId.get("id").toString());
            for (HashMap<Object, Object> valMap : defaultCoolingLockoutTempPointArr) {
                if (valMap.get("val") != null) {
                    hayStack.pointWrite(HRef.copy(outsideTempCoolingLockoutId), (int) Double.parseDouble(valMap.get("level").toString()),
                                        valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(outsideTempCoolingLockoutId, HSUtil.getPriorityVal(outsideTempCoolingLockoutId));
        }
        
        return outsideTempCoolingLockoutId;
    }
    
    private static String addHeatingTempLockoutPoint(CCUHsApi hayStack, String siteRef, String equipRef,
                                                     String equipDis, String tz, String tunerTypeTag, boolean isDefault) {
        Point.Builder outsideTempHeatingLockout  =
            new Point.Builder().setDisplayName(equipDis + "-"+tunerTypeTag.toUpperCase()+"-" + "outsideTempHeatingLockout")
                               .setSiteRef(siteRef)
                               .setEquipRef(equipRef)
                               .setHisInterpolate("cov")
                               .addMarker(Tags.TUNER).addMarker(tunerTypeTag)
                               .addMarker(Tags.WRITABLE).addMarker(Tags.HIS)
                               .addMarker(Tags.OUTSIDE_TEMP).addMarker(Tags.HEATING)
                               .addMarker(Tags.LOCKOUT).addMarker(Tags.SP)
                               .setMinVal("50")
                               .setMaxVal("100")
                               .setIncrementVal("1")
                               .setUnit("\u00B0F")
                               .setTunerGroup(tunerTypeTag.toUpperCase())
                               .setTz(tz);
    
        if (isDefault) {
            outsideTempHeatingLockout.addMarker(Tags.DEFAULT);
        } else {
            outsideTempHeatingLockout.addMarker(Tags.SYSTEM);
        }
        return hayStack.addPoint(outsideTempHeatingLockout.build());
    }
    
    public static String createHeatingTempLockoutPoint(CCUHsApi hayStack, String siteRef, String equipRef,
                                                     String equipDis, String tz, String tunerTypeTag, boolean isDefault) {
        
        
        String outsideTempHeatingLockoutId = addHeatingTempLockoutPoint(hayStack, siteRef, equipRef, equipDis,
                                                                        tz,tunerTypeTag, isDefault);
        if (isDefault) {
            hayStack.writePointForCcuUser(outsideTempHeatingLockoutId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,
                                          OUTSIDE_TEMP_HEATING_LOCKOUT_DEFAULT, 0);
            hayStack.writeHisValById(outsideTempHeatingLockoutId, OUTSIDE_TEMP_HEATING_LOCKOUT_DEFAULT);
        } else {
            HashMap<Object, Object> defaultCoolingLockoutTempId =
                hayStack.readEntity("point and tuner and "+tunerTypeTag+" and " +
                                                          "default and outsideTemp and heating and lockout");
    
            ArrayList<HashMap> defaultCoolingLockoutTempPointArr =
                hayStack.readPoint(defaultCoolingLockoutTempId.get("id").toString());
            for (HashMap<Object, Object> valMap : defaultCoolingLockoutTempPointArr) {
                if (valMap.get("val") != null) {
                    hayStack.pointWrite(HRef.copy(outsideTempHeatingLockoutId), (int) Double.parseDouble(valMap.get("level").toString()),
                                        valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(outsideTempHeatingLockoutId, HSUtil.getPriorityVal(outsideTempHeatingLockoutId));
        }
        return outsideTempHeatingLockoutId;
    }
}
