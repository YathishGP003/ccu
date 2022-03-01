package a75f.io.logic.tuners;

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
    
    public static String createCoolingTempLockoutPoint(CCUHsApi hayStack, String siteRef, String equipRef,
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
        }
        String outsideTempCoolingLockoutId = hayStack.addPoint(outsideTempCoolingLockout.build());
        hayStack.writePointForCcuUser(outsideTempCoolingLockoutId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,
                                      OUTSIDE_TEMP_COOLING_LOCKOUT_DEFAULT, 0);
        hayStack.writeHisValById(outsideTempCoolingLockoutId, OUTSIDE_TEMP_COOLING_LOCKOUT_DEFAULT);
        return outsideTempCoolingLockoutId;
    }
    
    public static String createHeatingTempLockoutPoint(CCUHsApi hayStack, String siteRef, String equipRef,
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
        }
        String outsideTempHeatingLockoutId = hayStack.addPoint(outsideTempHeatingLockout.build());
        hayStack.writePointForCcuUser(outsideTempHeatingLockoutId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,
                                      OUTSIDE_TEMP_HEATING_LOCKOUT_DEFAULT, 0);
        hayStack.writeHisValById(outsideTempHeatingLockoutId, OUTSIDE_TEMP_HEATING_LOCKOUT_DEFAULT);
        return outsideTempHeatingLockoutId;
    }
}
