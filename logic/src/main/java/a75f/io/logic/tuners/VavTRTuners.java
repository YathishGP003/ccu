package a75f.io.logic.tuners;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

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
                                             .setMinVal("50").setMaxVal("70").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                             .setUnit("\u00B0F")
                                             .setTz(tz)
                                             .build();
        String satSPInitId = hayStack.addPoint(satSPInit);
        HashMap satSPInitPoint = hayStack.read("point and default and tuner and sat and spinit");
        ArrayList<HashMap> satSPInitArr = hayStack.readPoint(satSPInitPoint.get("id").toString());
        for (HashMap valMap : satSPInitArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(satSPInitId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(satSPInitId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
    
        Point satSPMin = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"satSPMin")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                  .addMarker("sat").addMarker("spmin").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .setMinVal("45").setMaxVal("65").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String satSPMinId = hayStack.addPoint(satSPMin);
        HashMap satSPMinPoint = hayStack.read("point and default and tuner and sat and spmin");
        ArrayList<HashMap> satSPMinArr = hayStack.readPoint(satSPMinPoint.get("id").toString());
        for (HashMap valMap : satSPMinArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(satSPMinId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(satSPMinId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
    
        Point satSPMax = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"satSPMax")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                 .addMarker("sat").addMarker("spmax").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .setMinVal("55").setMaxVal("75").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                 .setUnit("\u00B0F")
                                 .setTz(tz)
                                 .build();
        String satSPMaxId = hayStack.addPoint(satSPMax);
        HashMap satSPMaxPoint = hayStack.read("point and default and tuner and sat and spmax");
        ArrayList<HashMap> satSPMaxArr = hayStack.readPoint(satSPMaxPoint.get("id").toString());
        for (HashMap valMap : satSPMaxArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(satSPMaxId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(satSPMaxId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
    
        Point satTimeDelay = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"satTimeDelay")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                 .addMarker("sat").addMarker("timeDelay").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .setMinVal("0").setMaxVal("30").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                 .setUnit("m")
                                 .setTz(tz)
                                 .build();
        String satTimeDelayId = hayStack.addPoint(satTimeDelay);
        HashMap satTimeDelayPoint = hayStack.read("point and default and tuner and sat and timeDelay");
        ArrayList<HashMap> satTimeDelayArr = hayStack.readPoint(satTimeDelayPoint.get("id").toString());
        for (HashMap valMap : satTimeDelayArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(satTimeDelayId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(satTimeDelayId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
    
        Point satTimeInterval = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+"satTimeInterval")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipRef)
                                     .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                     .addMarker("sat").addMarker("timeInterval").addMarker("writable").addMarker("his").addMarker("equipHis")
                                     .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                     .setUnit("m")
                                     .setTz(tz)
                                     .build();
        String satTimeIntervalId = hayStack.addPoint(satTimeInterval);
        HashMap satTimeIntervalPoint = hayStack.read("point and default and tuner and sat and timeInterval");
        ArrayList<HashMap> satTimeIntervalArr = hayStack.readPoint(satTimeIntervalPoint.get("id").toString());
        for (HashMap valMap : satTimeIntervalArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(satTimeIntervalId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(satTimeIntervalId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
    
        Point satIgnoreRequest = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"satIgnoreRequest")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef)
                                        .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                        .addMarker("sat").addMarker("ignoreRequest").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                        .setTz(tz)
                                        .build();
        String satIgnoreRequestId = hayStack.addPoint(satIgnoreRequest);
        HashMap satIgnoreRequestPoint = hayStack.read("point and default and tuner and sat and ignoreRequest");
        ArrayList<HashMap> satIgnoreRequestArr = hayStack.readPoint(satIgnoreRequestPoint.get("id").toString());
        for (HashMap valMap : satIgnoreRequestArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(satIgnoreRequestId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(satIgnoreRequestId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
    
        Point satSPTrim = new Point.Builder()
                                         .setDisplayName(equipDis+"-"+"satSPTrim")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipRef)
                                         .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                         .addMarker("sat").addMarker("sptrim").addMarker("writable").addMarker("his").addMarker("equipHis")
                                         .setMinVal("-0.5").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                         .setUnit("\u00B0F")
                                         .setTz(tz)
                                         .build();
        String satSPTrimId = hayStack.addPoint(satSPTrim);
        HashMap satSPTrimPoint = hayStack.read("point and default and tuner and sat and sptrim");
        ArrayList<HashMap> satSPTrimArr = hayStack.readPoint(satSPTrimPoint.get("id").toString());
        for (HashMap valMap : satSPTrimArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(satSPTrimId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(satSPTrimId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
    
        Point satSPRes = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"satSPRes")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                  .addMarker("sat").addMarker("spres").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .setMaxVal("-0.1").setMinVal("-2.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String satSPResId = hayStack.addPoint(satSPRes);
        HashMap satSPResPoint = hayStack.read("point and default and tuner and sat and spres");
        ArrayList<HashMap> satSPResArr = hayStack.readPoint(satSPResPoint.get("id").toString());
        for (HashMap valMap : satSPResArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(satSPResId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(satSPResId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
    
        Point satSPResMax = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"satSPResMax")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                  .addMarker("sat").addMarker("spresmax").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .setMaxVal("-0.1").setMinVal("-3.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                  .setUnit("\u00B0F")
                                  .setTz(tz)
                                  .build();
        String satSPResMaxId = hayStack.addPoint(satSPResMax);
        HashMap satSPResMaxPoint = hayStack.read("point and default and tuner and sat and spresmax");
        ArrayList<HashMap> satSPResMaxArr = hayStack.readPoint(satSPResMaxPoint.get("id").toString());
        for (HashMap valMap : satSPResMaxArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(satSPResMaxId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(satSPResMaxId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
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
                                  .setEquipRef(equipRef)
                                  .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                  .addMarker("staticPressure").addMarker("spinit").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .setMinVal("0.1").setMaxVal("2.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                  .setUnit("inch wc")
                                  .setTz(tz)
                                  .build();
        String staticPressureSPInitId = hayStack.addPoint(staticPressureSPInit);
        HashMap staticPressureSPInitPoint = hayStack.read("point and default and tuner and staticPressure and spinit");
        ArrayList<HashMap> staticPressureSPInitArr = hayStack.readPoint(staticPressureSPInitPoint.get("id").toString());
        for (HashMap valMap : staticPressureSPInitArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(staticPressureSPInitId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(staticPressureSPInitId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        
        Point staticPressureSPMin = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"staticPressureSPMin")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                 .addMarker("staticPressure").addMarker("spmin").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .setMinVal("0.1").setMaxVal("2.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                 .setUnit("inch wc")
                                 .setTz(tz)
                                 .build();
        String staticPressureSPMinId = hayStack.addPoint(staticPressureSPMin);
        HashMap staticPressureSPMinPoint = hayStack.read("point and default and tuner and staticPressure and spmin");
        ArrayList<HashMap> staticPressureSPMinArr = hayStack.readPoint(staticPressureSPMinPoint.get("id").toString());
        for (HashMap valMap : staticPressureSPMinArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(staticPressureSPMinId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(staticPressureSPMinId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        
        Point staticPressureSPMax = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"staticPressureSPMax")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                 .addMarker("staticPressure").addMarker("spmax").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .setMinVal("0.1").setMaxVal("2.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                 .setUnit("inch wc")
                                 .setTz(tz)
                                 .build();
        String staticPressureSPMaxId = hayStack.addPoint(staticPressureSPMax);
        HashMap staticPressureSPMaxPoint = hayStack.read("point and default and tuner and staticPressure and spmax");
        ArrayList<HashMap> staticPressureSPMaxArr = hayStack.readPoint(staticPressureSPMaxPoint.get("id").toString());
        for (HashMap valMap : staticPressureSPMaxArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(staticPressureSPMaxId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(staticPressureSPMaxId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        
        Point staticPressureTimeDelay = new Point.Builder()
                                     .setDisplayName(equipDis+"-"+"staticPressureTimeDelay")
                                     .setSiteRef(siteRef)
                                     .setEquipRef(equipRef)
                                     .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                     .addMarker("staticPressure").addMarker("timeDelay").addMarker("writable").addMarker("his").addMarker("equipHis")
                                     .setMinVal("0").setMaxVal("30").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                     .setUnit("m")
                                     .setTz(tz)
                                     .build();
        String staticPressureTimeDelayId = hayStack.addPoint(staticPressureTimeDelay);
        HashMap staticPressureTimeDelayPoint = hayStack.read("point and default and tuner and staticPressure and timeDelay");
        ArrayList<HashMap> staticPressureTimeDelayArr = hayStack.readPoint(staticPressureTimeDelayPoint.get("id").toString());
        for (HashMap valMap : staticPressureTimeDelayArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(staticPressureTimeDelayId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(staticPressureTimeDelayId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        
        Point staticPressureTimeInterval = new Point.Builder()
                                        .setDisplayName(equipDis+"-"+"staticPressureTimeInterval")
                                        .setSiteRef(siteRef)
                                        .setEquipRef(equipRef)
                                        .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                        .addMarker("staticPressure").addMarker("timeInterval").addMarker("writable").addMarker("his").addMarker("equipHis")
                                        .setMinVal("0").setMaxVal("30").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                        .setUnit("m")
                                        .setTz(tz)
                                        .build();
        String staticPressureTimeIntervalId = hayStack.addPoint(staticPressureTimeInterval);
        HashMap staticPressureTimeIntervalPoint = hayStack.read("point and default and tuner and staticPressure and timeInterval");
        ArrayList<HashMap> staticPressureTimeIntervalArr = hayStack.readPoint(staticPressureTimeIntervalPoint.get("id").toString());
        for (HashMap valMap : staticPressureTimeIntervalArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(staticPressureTimeIntervalId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(staticPressureTimeIntervalId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        
        Point staticPressureIgnoreRequest = new Point.Builder()
                                         .setDisplayName(equipDis+"-"+"staticPressureIgnoreRequest")
                                         .setSiteRef(siteRef)
                                         .setEquipRef(equipRef)
                                         .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                         .addMarker("staticPressure").addMarker("ignoreRequest").addMarker("writable").addMarker("his").addMarker("equipHis")
                                         .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                         .setTz(tz)
                                         .build();
        String staticPressureIgnoreRequestId = hayStack.addPoint(staticPressureIgnoreRequest);
        HashMap staticPressureIgnoreRequestPoint = hayStack.read("point and default and tuner and staticPressure and ignoreRequest");
        ArrayList<HashMap> staticPressureIgnoreRequestArr = hayStack.readPoint(staticPressureIgnoreRequestPoint.get("id").toString());
        for (HashMap valMap : staticPressureIgnoreRequestArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(staticPressureIgnoreRequestId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(staticPressureIgnoreRequestId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        
        Point staticPressureSPTrim = new Point.Builder()
                                  .setDisplayName(equipDis+"-"+"staticPressureSPTrim")
                                  .setSiteRef(siteRef)
                                  .setEquipRef(equipRef)
                                  .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                  .addMarker("staticPressure").addMarker("sptrim").addMarker("writable").addMarker("his").addMarker("equipHis")
                                  .setMaxVal("-0.01").setMinVal("-0.5").setIncrementVal("0.01").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                  .setUnit("inch wc")
                                  .setTz(tz)
                                  .build();
        String staticPressureSPTrimId = hayStack.addPoint(staticPressureSPTrim);
        HashMap staticPressureSPTrimPoint = hayStack.read("point and default and tuner and staticPressure and sptrim");
        ArrayList<HashMap> staticPressureSPTrimArr = hayStack.readPoint(staticPressureSPTrimPoint.get("id").toString());
        for (HashMap valMap : staticPressureSPTrimArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(staticPressureSPTrimId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(staticPressureSPTrimId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        
        Point staticPressureSPRes = new Point.Builder()
                                 .setDisplayName(equipDis+"-"+"staticPressureSPRes")
                                 .setSiteRef(siteRef)
                                 .setEquipRef(equipRef)
                                 .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                 .addMarker("staticPressure").addMarker("spres").addMarker("writable").addMarker("his").addMarker("equipHis")
                                 .setMinVal("0.01").setMaxVal("0.2").setIncrementVal("0.01").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                 .setUnit("inch wc")
                                 .setTz(tz)
                                 .build();
        String staticPressureSPResId = hayStack.addPoint(staticPressureSPRes);
        HashMap staticPressureSPResPoint = hayStack.read("point and default and tuner and staticPressure and spres");
        ArrayList<HashMap> staticPressureSPResArr = hayStack.readPoint(staticPressureSPResPoint.get("id").toString());
        for (HashMap valMap : staticPressureSPResArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(staticPressureSPResId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(staticPressureSPResId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        
        Point staticPressureSPResMax = new Point.Builder()
                                    .setDisplayName(equipDis+"-"+"staticPressureSPResMax")
                                    .setSiteRef(siteRef)
                                    .setEquipRef(equipRef)
                                    .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                    .addMarker("staticPressure").addMarker("spresmax").addMarker("writable").addMarker("his").addMarker("equipHis")
                                    .setMinVal("0.05").setMaxVal("0.5").setIncrementVal("0.05").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                    .setUnit("inch wc")
                                    .setTz(tz)
                                    .build();
        String staticPressureSPResMaxId = hayStack.addPoint(staticPressureSPResMax);
        HashMap staticPressureSPResMaxPoint = hayStack.read("point and default and tuner and staticPressure and spresmax");
        ArrayList<HashMap> staticPressureSPResMaxArr = hayStack.readPoint(staticPressureSPResMaxPoint.get("id").toString());
        for (HashMap valMap : staticPressureSPResMaxArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(staticPressureSPResMaxId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(staticPressureSPResMaxId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
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
                                             .setEquipRef(equipRef)
                                             .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                             .addMarker("co2").addMarker("spinit").addMarker("writable").addMarker("his").addMarker("equipHis")
                                             .setMinVal("0").setMaxVal("1500").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                             .setUnit("ppm")
                                             .setTz(tz)
                                             .build();
        String co2SPInitId = hayStack.addPoint(co2SPInit);
        HashMap co2SPInitPoint = hayStack.read("point and default and tuner and co2 and spinit");
        ArrayList<HashMap> co2SPInitArr = hayStack.readPoint(co2SPInitPoint.get("id").toString());
        for (HashMap valMap : co2SPInitArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(co2SPInitId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(co2SPInitId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        
        Point co2SPMin = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"co2SPMin")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipRef)
                                            .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                            .addMarker("co2").addMarker("spmin").addMarker("writable").addMarker("his").addMarker("equipHis")
                                            .setMinVal("0").setMaxVal("1500").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                            .setUnit("ppm")
                                            .setTz(tz)
                                            .build();
        String co2SPMinId = hayStack.addPoint(co2SPMin);
        HashMap co2SPMinPoint = hayStack.read("point and default and tuner and co2 and spmin");
        ArrayList<HashMap> co2SPMinArr = hayStack.readPoint(co2SPMinPoint.get("id").toString());
        for (HashMap valMap : co2SPMinArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(co2SPMinId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(co2SPMinId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        
        Point co2SPMax = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"co2SPMax")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipRef)
                                            .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                            .addMarker("co2").addMarker("spmax").addMarker("writable").addMarker("his").addMarker("equipHis")
                                            .setMinVal("100").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                            .setUnit("ppm")
                                            .setTz(tz)
                                            .build();
        String co2SPMaxId = hayStack.addPoint(co2SPMax);
        HashMap co2SPMaxPoint = hayStack.read("point and default and tuner and co2 and spmax");
        ArrayList<HashMap> co2SPMaxArr = hayStack.readPoint(co2SPMaxPoint.get("id").toString());
        for (HashMap valMap : co2SPMaxArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(co2SPMaxId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(co2SPMaxId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        
        Point co2TimeDelay = new Point.Builder()
                                                .setDisplayName(equipDis+"-"+"co2TimeDelay")
                                                .setSiteRef(siteRef)
                                                .setEquipRef(equipRef)
                                                .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                                .addMarker("co2").addMarker("timeDelay").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                                .setUnit("m")
                                                .setTz(tz)
                                                .build();
        String co2TimeDelayId = hayStack.addPoint(co2TimeDelay);
        HashMap co2TimeDelayPoint = hayStack.read("point and default and tuner and co2 and timeDelay");
        ArrayList<HashMap> co2TimeDelayArr = hayStack.readPoint(co2TimeDelayPoint.get("id").toString());
        for (HashMap valMap : co2TimeDelayArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(co2TimeDelayId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(co2TimeDelayId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        
        Point co2TimeInterval = new Point.Builder()
                                                   .setDisplayName(equipDis+"-"+"co2TimeInterval")
                                                   .setSiteRef(siteRef)
                                                   .setEquipRef(equipRef)
                                                   .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                                   .addMarker("co2").addMarker("timeInterval").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                   .setMinVal("0").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                                   .setUnit("m")
                                                   .setTz(tz)
                                                   .build();
        String co2TimeIntervalId = hayStack.addPoint(co2TimeInterval);
        HashMap co2TimeIntervalPoint = hayStack.read("point and default and tuner and co2 and timeInterval");
        ArrayList<HashMap> co2TimeIntervalArr = hayStack.readPoint(co2TimeIntervalPoint.get("id").toString());
        for (HashMap valMap : co2TimeIntervalArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(co2TimeIntervalId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(co2TimeIntervalId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        
        Point co2IgnoreRequest = new Point.Builder()
                                                    .setDisplayName(equipDis+"-"+"co2IgnoreRequest")
                                                    .setSiteRef(siteRef)
                                                    .setEquipRef(equipRef)
                                                    .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                                    .addMarker("co2").addMarker("ignoreRequest").addMarker("writable").addMarker("his").addMarker("equipHis")
                                                    .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                                    .setTz(tz)
                                                    .build();
        String co2IgnoreRequestId = hayStack.addPoint(co2IgnoreRequest);
        HashMap co2IgnoreRequestPoint = hayStack.read("point and default and tuner and co2 and ignoreRequest");
        ArrayList<HashMap> co2IgnoreRequestArr = hayStack.readPoint(co2IgnoreRequestPoint.get("id").toString());
        for (HashMap valMap : co2IgnoreRequestArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(co2IgnoreRequestId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(co2IgnoreRequestId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        
        Point co2SPTrim = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"co2SPTrim")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipRef)
                                             .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                             .addMarker("co2").addMarker("sptrim").addMarker("writable").addMarker("his").addMarker("equipHis")
                                             .setMinVal("0").setMaxVal("50").setIncrementVal("1.0").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                             .setUnit("ppm")
                                             .setTz(tz)
                                             .build();
        String co2SPTrimId = hayStack.addPoint(co2SPTrim);
        HashMap co2SPTrimPoint = hayStack.read("point and default and tuner and co2 and sptrim");
        ArrayList<HashMap> co2SPTrimArr = hayStack.readPoint(co2SPTrimPoint.get("id").toString());
        for (HashMap valMap : co2SPTrimArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(co2SPTrimId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(co2SPTrimId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        
        Point co2SPRes = new Point.Builder()
                                            .setDisplayName(equipDis+"-"+"co2SPRes")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipRef)
                                            .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                            .addMarker("co2").addMarker("spres").addMarker("writable").addMarker("his").addMarker("equipHis")
                                            .setMinVal("-30.0").setMaxVal("-1.0").setIncrementVal("1.0").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                            .setUnit("ppm")
                                            .setTz(tz)
                                            .build();
        String co2SPResId = hayStack.addPoint(co2SPRes);
        HashMap co2SPResPoint = hayStack.read("point and default and tuner and co2 and spres");
        ArrayList<HashMap> co2SPResArr = hayStack.readPoint(co2SPResPoint.get("id").toString());
        for (HashMap valMap : co2SPResArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(co2SPResId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(co2SPResId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
        
        Point co2SPResMax = new Point.Builder()
                                               .setDisplayName(equipDis+"-"+"co2SPResMax")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef)
                                               .addMarker("system").addMarker("tuner").addMarker("tr").addMarker("sp").addMarker("vav")
                                               .addMarker("co2").addMarker("spresmax").addMarker("writable").addMarker("his").addMarker("equipHis")
                                               .setMinVal("-50.0").setMaxVal("-1.0").setIncrementVal("1.0").setTunerGroup(TunerConstants.VAV_TUNER_GROUP)
                                               .setUnit("ppm")
                                               .setTz(tz)
                                               .build();
        String co2SPResMaxId = hayStack.addPoint(co2SPResMax);
        HashMap co2SPResMaxPoint = hayStack.read("point and default and tuner and co2 and spresmax");
        ArrayList<HashMap> co2SPResMaxArr = hayStack.readPoint(co2SPResMaxPoint.get("id").toString());
        for (HashMap valMap : co2SPResMaxArr)
        {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(co2SPResMaxId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                hayStack.writeHisValById(co2SPResMaxId, Double.parseDouble(valMap.get("val").toString()));
            }
        }
    }
    
    public static double getCO2TRTunerVal(String trParam) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap cdb = hayStack.read("point and default and tuner and tr and co2 and "+trParam);
    
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
        CCUHsApi.getInstance().writeDefaultVal("point and default and tuner and tr and co2 and "+trParam, val);
    }
}
