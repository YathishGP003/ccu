package a75f.io.logic.tuners;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;

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
                                             .setEquipRef(equipRef)
                                             .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                             .addMarker("sat").addMarker("spinit").addMarker("writable").addMarker("his").addMarker("equipHis")
                                             .setUnit("\u00B0F")
                                             .setTz(tz)
                                             .build();
        String satSPInitId = hayStack.addPoint(satSPInit);
        hayStack.writeDefaultValById(satSPInitId, 65.0 );
        hayStack.writeHisValById(satSPInitId,65.0);
    
        Point satSPMin = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"satSPMin")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                  .addMarker("sat").addMarker("spmin").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String satSPMinId = hayStack.addPoint(satSPMin);
        hayStack.writeDefaultValById(satSPMinId, 55.0 );
        hayStack.writeHisValById(satSPMinId,55.0);
    
        Point satSPMax = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"satSPMax")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                 .addMarker("sat").addMarker("spmax").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .setUnit("\u00B0F")
                                 .setTz(tz)
                                 .build();
        String satSPMaxId = hayStack.addPoint(satSPMax);
        hayStack.writeDefaultValById(satSPMaxId, 65.0 );
        hayStack.writeHisValById(satSPMaxId,65.0);
    
        Point satTimeDelay = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"satTimeDelay")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                 .addMarker("sat").addMarker("timeDelay").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .setUnit("m")
                                 .setTz(tz)
                                 .build();
        String satTimeDelayId = hayStack.addPoint(satTimeDelay);
        hayStack.writeDefaultValById(satTimeDelayId, 2.0 );
        hayStack.writeHisValById(satTimeDelayId,2.0);
    
        Point satTimeInterval = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+"satTimeInterval")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipRef)
                                     .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                     .addMarker("sat").addMarker("timeInterval").addMarker("writable").addMarker("his").addMarker("equipHis")
                                     .setUnit("m")
                                     .setTz(tz)
                                     .build();
        String satTimeIntervalId = hayStack.addPoint(satTimeInterval);
        hayStack.writeDefaultValById(satTimeIntervalId, 2.0 );
        hayStack.writeHisValById(satTimeIntervalId,2.0);
    
        Point satIgnoreRequest = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"satIgnoreRequest")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef)
                                        .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                        .addMarker("sat").addMarker("ignoreRequest").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .setTz(tz)
                                        .build();
        String satIgnoreRequestId = hayStack.addPoint(satIgnoreRequest);
        hayStack.writeDefaultValById(satIgnoreRequestId, 2.0 );
        hayStack.writeHisValById(satIgnoreRequestId,2.0);
    
        Point satSPTrim = new Point.Builder()
                                         .setDisplayName(equipDis+"-"+"satSPTrim")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipRef)
                                         .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                         .addMarker("sat").addMarker("sptrim").addMarker("writable").addMarker("his").addMarker("equipHis")
                                         .setUnit("\u00B0F")
                                         .setTz(tz)
                                         .build();
        String satSPTrimId = hayStack.addPoint(satSPTrim);
        hayStack.writeDefaultValById(satSPTrimId, 0.2 );
        hayStack.writeHisValById(satSPTrimId,0.2);
    
        Point satSPRes = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"satSPRes")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                  .addMarker("sat").addMarker("spres").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String satSPResId = hayStack.addPoint(satSPRes);
        hayStack.writeDefaultValById(satSPResId, -0.3 );
        hayStack.writeHisValById(satSPResId,-0.3);
    
        Point satSPResMax = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"satSPResMax")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                  .addMarker("sat").addMarker("spresmax").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String satSPResMaxId = hayStack.addPoint(satSPResMax);
        hayStack.writeDefaultValById(satSPResMaxId, -1.0 );
        hayStack.writeHisValById(satSPResMaxId,-1.0);
    }
    
    public static double getSatTRTunerVal(String trParam) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and system and tuner and tr and sat and "+trParam);
    
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
        CCUHsApi.getInstance().writeDefaultVal("point and system and tuner and tr and sat and "+trParam, val);
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
                                  .setEquipRef(equipRef)
                                  .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                  .addMarker("staticPressure").addMarker("spinit").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .setUnit("inch wc")
                                  .setTz(tz)
                                  .build();
        String staticPressureSPInitId = hayStack.addPoint(staticPressureSPInit);
        hayStack.writeDefaultValById(staticPressureSPInitId, 0.2 );
        hayStack.writeHisValById(staticPressureSPInitId,0.2);
        
        Point staticPressureSPMin = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"staticPressureSPMin")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                 .addMarker("staticPressure").addMarker("spmin").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .setUnit("inch wc")
                                 .setTz(tz)
                                 .build();
        String staticPressureSPMinId = hayStack.addPoint(staticPressureSPMin);
        hayStack.writeDefaultValById(staticPressureSPMinId, 0.2 );
        hayStack.writeHisValById(staticPressureSPMinId,0.2);
        
        Point staticPressureSPMax = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"staticPressureSPMax")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                 .addMarker("staticPressure").addMarker("spmax").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .setUnit("inch wc")
                                 .setTz(tz)
                                 .build();
        String staticPressureSPMaxId = hayStack.addPoint(staticPressureSPMax);
        hayStack.writeDefaultValById(staticPressureSPMaxId, 1.0 );
        hayStack.writeHisValById(staticPressureSPMaxId,1.0);
        
        Point staticPressureTimeDelay = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+"staticPressureTimeDelay")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipRef)
                                     .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                     .addMarker("staticPressure").addMarker("timeDelay").addMarker("writable").addMarker("his").addMarker("equipHis")
                                     .setUnit("m")
                                     .setTz(tz)
                                     .build();
        String staticPressureTimeDelayId = hayStack.addPoint(staticPressureTimeDelay);
        hayStack.writeDefaultValById(staticPressureTimeDelayId, 2.0 );
        hayStack.writeHisValById(staticPressureTimeDelayId,2.0);
        
        Point staticPressureTimeInterval = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"staticPressureTimeInterval")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef)
                                        .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                        .addMarker("staticPressure").addMarker("timeInterval").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .setUnit("m")
                                        .setTz(tz)
                                        .build();
        String staticPressureTimeIntervalId = hayStack.addPoint(staticPressureTimeInterval);
        hayStack.writeDefaultValById(staticPressureTimeIntervalId, 2.0 );
        hayStack.writeHisValById(staticPressureTimeIntervalId,2.0);
        
        Point staticPressureIgnoreRequest = new Point.Builder()
                                         .setDisplayName(equipDis+"-"+"staticPressureIgnoreRequest")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipRef)
                                         .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                         .addMarker("staticPressure").addMarker("ignoreRequest").addMarker("writable").addMarker("his").addMarker("equipHis")
                                         .setTz(tz)
                                         .build();
        String staticPressureIgnoreRequestId = hayStack.addPoint(staticPressureIgnoreRequest);
        hayStack.writeDefaultValById(staticPressureIgnoreRequestId, 2.0 );
        hayStack.writeHisValById(staticPressureIgnoreRequestId,2.0);
        
        Point staticPressureSPTrim = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"staticPressureSPTrim")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                  .addMarker("staticPressure").addMarker("sptrim").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .setUnit("inch wc")
                                  .setTz(tz)
                                  .build();
        String staticPressureSPTrimId = hayStack.addPoint(staticPressureSPTrim);
        hayStack.writeDefaultValById(staticPressureSPTrimId, -0.02 );
        hayStack.writeHisValById(staticPressureSPTrimId,-0.02);
        
        Point staticPressureSPRes = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"staticPressureSPRes")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                 .addMarker("staticPressure").addMarker("spres").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .setUnit("inch wc")
                                 .setTz(tz)
                                 .build();
        String staticPressureSPResId = hayStack.addPoint(staticPressureSPRes);
        hayStack.writeDefaultValById(staticPressureSPResId, 0.05 );
        hayStack.writeHisValById(staticPressureSPResId,0.05);
        
        Point staticPressureSPResMax = new Point.Builder()
                                    .setDisplayName(equipDis+"-"+"staticPressureSPResMax")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipRef)
                                    .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                    .addMarker("staticPressure").addMarker("spresmax").addMarker("writable").addMarker("his").addMarker("equipHis")
                                    .setUnit("inch wc")
                                    .setTz(tz)
                                    .build();
        String staticPressureSPResMaxId = hayStack.addPoint(staticPressureSPResMax);
        hayStack.writeDefaultValById(staticPressureSPResMaxId, 0.1 );
        hayStack.writeHisValById(staticPressureSPResMaxId,0.1);
    }
    
    public static double getStaticPressureTRTunerVal(String trParam) {
        
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and system and tuner and tr and staticPressure and "+trParam);
    
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
        CCUHsApi.getInstance().writeDefaultVal("point and system and tuner and tr and staticPressure and "+trParam, val);
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
                                             .setEquipRef(equipRef)
                                             .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                             .addMarker("co2").addMarker("spinit").addMarker("writable").addMarker("his").addMarker("equipHis")
                                             .setUnit("ppm")
                                             .setTz(tz)
                                             .build();
        String co2SPInitId = hayStack.addPoint(co2SPInit);
        hayStack.writeDefaultValById(co2SPInitId, 800.0 );
        hayStack.writeHisValById(co2SPInitId,800.0);
        
        Point co2SPMin = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"co2SPMin")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipRef)
                                            .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                            .addMarker("co2").addMarker("spmin").addMarker("writable").addMarker("his").addMarker("equipHis")
                                            .setUnit("ppm")
                                            .setTz(tz)
                                            .build();
        String co2SPMinId = hayStack.addPoint(co2SPMin);
        hayStack.writeDefaultValById(co2SPMinId, 800.0 );
        hayStack.writeHisValById(co2SPMinId,800.0);
        
        Point co2SPMax = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"co2SPMax")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipRef)
                                            .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                            .addMarker("co2").addMarker("spmax").addMarker("writable").addMarker("his").addMarker("equipHis")
                                            .setUnit("ppm")
                                            .setTz(tz)
                                            .build();
        String co2SPMaxId = hayStack.addPoint(co2SPMax);
        hayStack.writeDefaultValById(co2SPMaxId, 1000.0 );
        hayStack.writeHisValById(co2SPMaxId,1000.0);
        
        Point co2TimeDelay = new Point.Builder()
                                                .setDisplayName(equipDis+"-"+"co2TimeDelay")
                                                .setSiteRef(siteRef)
                                                .setEquipRef(equipRef)
                                                .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                                .addMarker("co2").addMarker("timeDelay").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                .setUnit("m")
                                                .setTz(tz)
                                                .build();
        String co2TimeDelayId = hayStack.addPoint(co2TimeDelay);
        hayStack.writeDefaultValById(co2TimeDelayId, 2.0 );
        hayStack.writeHisValById(co2TimeDelayId,2.0);
        
        Point co2TimeInterval = new Point.Builder()
                                                   .setDisplayName(equipDis+"-"+"co2TimeInterval")
                                                   .setSiteRef(siteRef)
                                                   .setEquipRef(equipRef)
                                                   .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                                   .addMarker("co2").addMarker("timeInterval").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                   .setUnit("m")
                                                   .setTz(tz)
                                                   .build();
        String co2TimeIntervalId = hayStack.addPoint(co2TimeInterval);
        hayStack.writeDefaultValById(co2TimeIntervalId, 2.0 );
        hayStack.writeHisValById(co2TimeIntervalId,2.0);
        
        Point co2IgnoreRequest = new Point.Builder()
                                                    .setDisplayName(equipDis+"-"+"co2IgnoreRequest")
                                                    .setSiteRef(siteRef)
                                                    .setEquipRef(equipRef)
                                                    .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                                    .addMarker("co2").addMarker("ignoreRequest").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                    .setTz(tz)
                                                    .build();
        String co2IgnoreRequestId = hayStack.addPoint(co2IgnoreRequest);
        hayStack.writeDefaultValById(co2IgnoreRequestId, 2.0 );
        hayStack.writeHisValById(co2IgnoreRequestId,2.0);
        
        Point co2SPTrim = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"co2SPTrim")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipRef)
                                             .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                             .addMarker("co2").addMarker("sptrim").addMarker("writable").addMarker("his").addMarker("equipHis")
                                             .setUnit("ppm")
                                             .setTz(tz)
                                             .build();
        String co2SPTrimId = hayStack.addPoint(co2SPTrim);
        hayStack.writeDefaultValById(co2SPTrimId, 20.0 );
        hayStack.writeHisValById(co2SPTrimId,20.0);
        
        Point co2SPRes = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"co2SPRes")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipRef)
                                            .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                            .addMarker("co2").addMarker("spres").addMarker("writable").addMarker("his").addMarker("equipHis")
                                            .setUnit("ppm")
                                            .setTz(tz)
                                            .build();
        String co2SPResId = hayStack.addPoint(co2SPRes);
        hayStack.writeDefaultValById(co2SPResId, -10.0 );
        hayStack.writeHisValById(co2SPResId,-10.0);
        
        Point co2SPResMax = new Point.Builder()
                                               .setDisplayName(equipDis+"-"+"co2SPResMax")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef)
                                               .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                               .addMarker("co2").addMarker("spresmax").addMarker("writable").addMarker("his").addMarker("equipHis")
                                               .setUnit("ppm")
                                               .setTz(tz)
                                               .build();
        String co2SPResMaxId = hayStack.addPoint(co2SPResMax);
        hayStack.writeDefaultValById(co2SPResMaxId, -30.0 );
        hayStack.writeHisValById(co2SPResMaxId,-30.0);
    }
    
    public static double getCO2TRTunerVal(String trParam) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and system and tuner and tr and co2 and "+trParam);
    
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
    public static void setCO2TRTunerVal(String trParam, double val) {
        CCUHsApi.getInstance().writeDefaultVal("point and system and tuner and tr and co2 and "+trParam, val);
    }
}
