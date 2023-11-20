package a75f.io.logic.tuners;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.DomainName;
import a75f.io.logic.bo.building.definitions.Consts;

/**
 * Created by samjithsadasivan on 1/8/19.
 */
//Refer to 75F-RP1455_Spec for initial values of all variables/tuners
public class VavTRTuners
{
    public static void addSatTRTunerPoints(String equipRef) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString()+"-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        CCUHsApi hayStack = CCUHsApi.getInstance();
        Point satSPInit = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"satSPInit")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipRef).setHisInterpolate("cov")
                                             .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                             .addMarker("sat").addMarker("spinit").addMarker("writable").addMarker("his")
                                             .setMinVal("50").setMaxVal("70").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                             .setUnit("\u00B0F")
                                             .setTz(tz)
                                             .build();
        String satSPInitId = hayStack.addPoint(satSPInit);
        TunerUtil.copyDefaultBuildingTunerVal(satSPInitId, DomainName.satSPInit, hayStack);
    
        Point satSPMin = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"satSPMin")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                  .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                  .addMarker("sat").addMarker("spmin").addMarker("writable").addMarker("his")
                                  .setMinVal("45").setMaxVal("65").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String satSPMinId = hayStack.addPoint(satSPMin);
        TunerUtil.copyDefaultBuildingTunerVal(satSPMinId, DomainName.satSPMin, hayStack);
    
        Point satSPMax = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"satSPMax")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                 .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                 .addMarker("sat").addMarker("spmax").addMarker("writable").addMarker("his")
                                 .setMinVal("55").setMaxVal("75").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                 .setUnit("\u00B0F")
                                 .setTz(tz)
                                 .build();
        String satSPMaxId = hayStack.addPoint(satSPMax);
        TunerUtil.copyDefaultBuildingTunerVal(satSPMaxId, DomainName.satSPMax, hayStack);
    
        Point satTimeDelay = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"satTimeDelay")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                 .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                 .addMarker("sat").addMarker("timeDelay").addMarker("writable").addMarker("his")
                                 .setMinVal("0").setMaxVal("30").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                 .setUnit("m")
                                 .setTz(tz)
                                 .build();
        String satTimeDelayId = hayStack.addPoint(satTimeDelay);
        TunerUtil.copyDefaultBuildingTunerVal(satTimeDelayId, DomainName.satTimeDelay, hayStack);
    
        Point satTimeInterval = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+"satTimeInterval")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipRef).setHisInterpolate("cov")
                                     .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                     .addMarker("sat").addMarker("timeInterval").addMarker("writable").addMarker("his")
                                     .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                     .setUnit("m")
                                     .setTz(tz)
                                     .build();
        String satTimeIntervalId = hayStack.addPoint(satTimeInterval);
        TunerUtil.copyDefaultBuildingTunerVal(satTimeIntervalId, DomainName.satTimeInterval, hayStack);
    
        Point satIgnoreRequest = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"satIgnoreRequest")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                        .addMarker("sat").addMarker("ignoreRequest").addMarker("writable").addMarker("his")
                                        .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String satIgnoreRequestId = hayStack.addPoint(satIgnoreRequest);
        TunerUtil.copyDefaultBuildingTunerVal(satIgnoreRequestId, DomainName.satIgnoreRequest, hayStack);
    
        Point satSPTrim = new Point.Builder()
                                         .setDisplayName(equipDis+"-"+"satSPTrim")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                         .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                         .addMarker("sat").addMarker("sptrim").addMarker("writable").addMarker("his")
                                         .setMinVal("-0.5").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                         .setUnit("\u00B0F")
                                         .setTz(tz)
                                         .build();
        String satSPTrimId = hayStack.addPoint(satSPTrim);
        TunerUtil.copyDefaultBuildingTunerVal(satSPTrimId, DomainName.satSPTrim, hayStack);
    
        Point satSPRes = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"satSPRes")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                  .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                  .addMarker("sat").addMarker("spres").addMarker("writable").addMarker("his")
                                  .setMaxVal("-0.1").setMinVal("-2.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String satSPResId = hayStack.addPoint(satSPRes);
        TunerUtil.copyDefaultBuildingTunerVal(satSPResId, DomainName.satSPRes, hayStack);
    
        Point satSPResMax = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"satSPResMax")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                  .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                  .addMarker("sat").addMarker("spresmax").addMarker("writable").addMarker("his")
                                  .setMaxVal("-0.1").setMinVal("-3.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String satSPResMaxId = hayStack.addPoint(satSPResMax);
        TunerUtil.copyDefaultBuildingTunerVal(satSPResMaxId, DomainName.satSPResMax, hayStack);
    }
    
    public static double getSatTRTunerVal(String trParam) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and default and tuner and tr and sat and "+trParam);
    
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        throw new IllegalStateException("Tuner not initialized :"+trParam);
    }
    public static void setSatTRTunerVal(String trParam, double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and default and tuner and tr and sat and "+trParam, val);
    }
    
    public static void addStaticPressureTRTunerPoints(String equipRef) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString()+"-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        CCUHsApi hayStack = CCUHsApi.getInstance();
        Point staticPressureSPInit = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"staticPressureSPInit")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                  .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                  .addMarker("staticPressure").addMarker("spinit").addMarker("writable").addMarker("his")
                                  .setMinVal("0.1").setMaxVal("2.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                  .setUnit(Consts.PRESSURE_UNIT)
                                  .setTz(tz)
                                  .build();
        String staticPressureSPInitId = hayStack.addPoint(staticPressureSPInit);
        TunerUtil.copyDefaultBuildingTunerVal(staticPressureSPInitId, DomainName.staticPressureSPInit, hayStack);
        
        Point staticPressureSPMin = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"staticPressureSPMin")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                 .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                 .addMarker("staticPressure").addMarker("spmin").addMarker("writable").addMarker("his")
                                 .setMinVal("0.1").setMaxVal("2.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                 .setUnit(Consts.PRESSURE_UNIT)
                                 .setTz(tz)
                                 .build();
        String staticPressureSPMinId = hayStack.addPoint(staticPressureSPMin);
        TunerUtil.copyDefaultBuildingTunerVal(staticPressureSPMinId, DomainName.staticPressureSPMin, hayStack);
        
        Point staticPressureSPMax = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"staticPressureSPMax")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                 .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                 .addMarker("staticPressure").addMarker("spmax").addMarker("writable").addMarker("his")
                                 .setMinVal("0.1").setMaxVal("2.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                 .setUnit(Consts.PRESSURE_UNIT)
                                 .setTz(tz)
                                 .build();
        String staticPressureSPMaxId = hayStack.addPoint(staticPressureSPMax);
        TunerUtil.copyDefaultBuildingTunerVal(staticPressureSPMaxId, DomainName.staticPressureSPMax, hayStack);
        
        Point staticPressureTimeDelay = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+"staticPressureTimeDelay")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipRef).setHisInterpolate("cov")
                                     .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                     .addMarker("staticPressure").addMarker("timeDelay").addMarker("writable").addMarker("his")
                                     .setMinVal("0").setMaxVal("30").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                     .setUnit("m")
                                     .setTz(tz)
                                     .build();
        String staticPressureTimeDelayId = hayStack.addPoint(staticPressureTimeDelay);
        TunerUtil.copyDefaultBuildingTunerVal(staticPressureTimeDelayId, DomainName.staticPressureTimeDelay, hayStack);
        
        Point staticPressureTimeInterval = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"staticPressureTimeInterval")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef).setHisInterpolate("cov")
                                        .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                        .addMarker("staticPressure").addMarker("timeInterval").addMarker("writable").addMarker("his")
                                        .setMinVal("0").setMaxVal("30").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                        .setUnit("m")
                                        .setTz(tz)
                                        .build();
        String staticPressureTimeIntervalId = hayStack.addPoint(staticPressureTimeInterval);
        TunerUtil.copyDefaultBuildingTunerVal(staticPressureTimeIntervalId, DomainName.staticPressureTimeInterval, hayStack);
        
        Point staticPressureIgnoreRequest = new Point.Builder()
                                         .setDisplayName(equipDis+"-"+"staticPressureIgnoreRequest")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                         .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                         .addMarker("staticPressure").addMarker("ignoreRequest").addMarker("writable").addMarker("his")
                                         .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                         .setTz(tz)
                                         .build();
        String staticPressureIgnoreRequestId = hayStack.addPoint(staticPressureIgnoreRequest);
        TunerUtil.copyDefaultBuildingTunerVal(staticPressureIgnoreRequestId, DomainName.staticPressureIgnoreRequest, hayStack);
        
        Point staticPressureSPTrim = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"staticPressureSPTrim")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef).setHisInterpolate("cov")
                                  .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                  .addMarker("staticPressure").addMarker("sptrim").addMarker("writable").addMarker("his")
                                  .setMinVal("-0.5").setMaxVal("-0.01").setIncrementVal("0.01").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                  .setUnit(Consts.PRESSURE_UNIT)
                                  .setTz(tz)
                                  .build();
        String staticPressureSPTrimId = hayStack.addPoint(staticPressureSPTrim);
        TunerUtil.copyDefaultBuildingTunerVal(staticPressureSPTrimId, DomainName.staticPressureSPTrim, hayStack);
        
        Point staticPressureSPRes = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"staticPressureSPRes")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef).setHisInterpolate("cov")
                                 .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                 .addMarker("staticPressure").addMarker("spres").addMarker("writable").addMarker("his")
                                 .setMinVal("0.01").setMaxVal("0.2").setIncrementVal("0.01").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                 .setUnit(Consts.PRESSURE_UNIT)
                                 .setTz(tz)
                                 .build();
        String staticPressureSPResId = hayStack.addPoint(staticPressureSPRes);
        TunerUtil.copyDefaultBuildingTunerVal(staticPressureSPResId, DomainName.staticPressureSPRes, hayStack);
        
        Point staticPressureSPResMax = new Point.Builder()
                                    .setDisplayName(equipDis+"-"+"staticPressureSPResMax")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipRef).setHisInterpolate("cov")
                                    .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                    .addMarker("staticPressure").addMarker("spresmax").addMarker("writable").addMarker("his")
                                    .setMinVal("0.05").setMaxVal("0.5").setIncrementVal("0.05").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                    .setUnit(Consts.PRESSURE_UNIT)
                                    .setTz(tz)
                                    .build();
        String staticPressureSPResMaxId = hayStack.addPoint(staticPressureSPResMax);
        TunerUtil.copyDefaultBuildingTunerVal(staticPressureSPResMaxId, DomainName.staticPressureSPResMax, hayStack);
    }
    
    public static double getStaticPressureTRTunerVal(String trParam) {
        
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and default and tuner and tr and staticPressure and "+trParam);
    
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        throw new IllegalStateException("Tuner not initialized :"+trParam);
    }
    public static void setStaticPressureTRTunerVal(String trParam, double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and default and tuner and tr and staticPressure and "+trParam, val);
    }
    
    public static void addCO2TRTunerPoints(String equipRef) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String equipDis = siteMap.get("dis").toString()+"-SystemEquip";
        String siteRef = siteMap.get("id").toString();
        String tz = siteMap.get("tz").toString();
        CCUHsApi hayStack = CCUHsApi.getInstance();
        Point co2SPInit = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"co2SPInit")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipRef).setHisInterpolate("cov")
                                             .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                             .addMarker("co2").addMarker("spinit").addMarker("writable").addMarker("his")
                                             .setMinVal("0").setMaxVal("1500").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                             .setUnit("ppm")
                                             .setTz(tz)
                                             .build();
        String co2SPInitId = hayStack.addPoint(co2SPInit);
        TunerUtil.copyDefaultBuildingTunerVal(co2SPInitId, DomainName.co2SPInit, hayStack);
        
        Point co2SPMin = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"co2SPMin")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipRef).setHisInterpolate("cov")
                                            .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                            .addMarker("co2").addMarker("spmin").addMarker("writable").addMarker("his")
                                            .setMinVal("0").setMaxVal("1500").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                            .setUnit("ppm")
                                            .setTz(tz)
                                            .build();
        String co2SPMinId = hayStack.addPoint(co2SPMin);
        TunerUtil.copyDefaultBuildingTunerVal(co2SPMinId, DomainName.co2SPMin, hayStack);
        
        Point co2SPMax = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"co2SPMax")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipRef).setHisInterpolate("cov")
                                            .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                            .addMarker("co2").addMarker("spmax").addMarker("writable").addMarker("his")
                                            .setMinVal("100").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                            .setUnit("ppm")
                                            .setTz(tz)
                                            .build();
        String co2SPMaxId = hayStack.addPoint(co2SPMax);
        TunerUtil.copyDefaultBuildingTunerVal(co2SPMaxId, DomainName.co2SPMax, hayStack);
        
        Point co2TimeDelay = new Point.Builder()
                                                .setDisplayName(equipDis+"-"+"co2TimeDelay")
                                                .setSiteRef(siteRef)
                                                .setEquipRef(equipRef).setHisInterpolate("cov")
                                                .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                                .addMarker("co2").addMarker("timeDelay").addMarker("writable").addMarker("his")
                                                .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                                .setUnit("m")
                                                .setTz(tz)
                                                .build();
        String co2TimeDelayId = hayStack.addPoint(co2TimeDelay);
        TunerUtil.copyDefaultBuildingTunerVal(co2TimeDelayId, DomainName.co2TimeDelay, hayStack);
        
        Point co2TimeInterval = new Point.Builder()
                                                   .setDisplayName(equipDis+"-"+"co2TimeInterval")
                                                   .setSiteRef(siteRef)
                                                   .setEquipRef(equipRef).setHisInterpolate("cov")
                                                   .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                                   .addMarker("co2").addMarker("timeInterval").addMarker("writable").addMarker("his")
                                                   .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                                   .setUnit("m")
                                                   .setTz(tz)
                                                   .build();
        String co2TimeIntervalId = hayStack.addPoint(co2TimeInterval);
        TunerUtil.copyDefaultBuildingTunerVal(co2TimeIntervalId, DomainName.co2TimeInterval, hayStack);
        
        Point co2IgnoreRequest = new Point.Builder()
                                                    .setDisplayName(equipDis+"-"+"co2IgnoreRequest")
                                                    .setSiteRef(siteRef)
                                                    .setEquipRef(equipRef).setHisInterpolate("cov")
                                                    .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                                    .addMarker("co2").addMarker("ignoreRequest").addMarker("writable").addMarker("his")
                                                    .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                                    .setTz(tz)
                                                    .build();
        String co2IgnoreRequestId = hayStack.addPoint(co2IgnoreRequest);
        TunerUtil.copyDefaultBuildingTunerVal(co2IgnoreRequestId, DomainName.co2IgnoreRequest, hayStack);
        
        Point co2SPTrim = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"co2SPTrim")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipRef).setHisInterpolate("cov")
                                             .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                             .addMarker("co2").addMarker("sptrim").addMarker("writable").addMarker("his")
                                             .setMinVal("0").setMaxVal("50").setIncrementVal("1.0").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                             .setUnit("ppm")
                                             .setTz(tz)
                                             .build();
        String co2SPTrimId = hayStack.addPoint(co2SPTrim);
        TunerUtil.copyDefaultBuildingTunerVal(co2SPTrimId, DomainName.co2SPTrim, hayStack);
        
        Point co2SPRes = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"co2SPRes")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipRef).setHisInterpolate("cov")
                                            .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                            .addMarker("co2").addMarker("spres").addMarker("writable").addMarker("his")
                                            .setMinVal("-30.0").setMaxVal("-1.0").setIncrementVal("1.0").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                            .setUnit("ppm")
                                            .setTz(tz)
                                            .build();
        String co2SPResId = hayStack.addPoint(co2SPRes);
        TunerUtil.copyDefaultBuildingTunerVal(co2SPResId, DomainName.co2SPRes, hayStack);
        
        Point co2SPResMax = new Point.Builder()
                                               .setDisplayName(equipDis+"-"+"co2SPResMax")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef).setHisInterpolate("cov")
                                               .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                               .addMarker("co2").addMarker("spresmax").addMarker("writable").addMarker("his")
                                               .setMinVal("-50.0").setMaxVal("-1.0").setIncrementVal("1.0").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                               .setUnit("ppm")
                                               .setTz(tz)
                                               .build();
        String co2SPResMaxId = hayStack.addPoint(co2SPResMax);
        TunerUtil.copyDefaultBuildingTunerVal(co2SPResMaxId, DomainName.co2SPResMax, hayStack);
    }
}
