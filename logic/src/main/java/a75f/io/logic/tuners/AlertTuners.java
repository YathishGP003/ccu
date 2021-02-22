package a75f.io.logic.tuners;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;

class AlertTuners {
    
    public static void addDefaultAlertTuners(CCUHsApi hayStack, String siteRef, String equipRef, String equipDis,
                                        String tz) {
        Point buildingLimitAlertTimer  = new Point.Builder()
                                             .setDisplayName(equipDis+"-"+"buildingLimitAlertTimer")
                                             .setSiteRef(siteRef)
                                             .setEquipRef(equipRef).setHisInterpolate("cov")
                                             .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                             .addMarker("building").addMarker("limit").addMarker("alert").addMarker("timer").addMarker("sp")
                                             .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                             .setUnit("m")
                                             .setTz(tz)
                                             .build();
        String buildingLimitAlertTimerId = hayStack.addPoint(buildingLimitAlertTimer);
        hayStack.writePointForCcuUser(buildingLimitAlertTimerId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,45.0, 0);
        hayStack.writeHisValById(buildingLimitAlertTimerId, 45.0);
    
        Point constantTempAlertTime  = new Point.Builder()
                                           .setDisplayName(equipDis+"-"+"constantTempAlertTime")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                           .addMarker("constant").addMarker("temp").addMarker("alert").addMarker("time").addMarker("sp")
                                           .setMinVal("0").setMaxVal("60").setIncrementVal("5").setTunerGroup(TunerConstants.ALERT_TUNER)
                                           .setUnit("m")
                                           .setTz(tz)
                                           .build();
        String constantTempAlertTimeId = hayStack.addPoint(constantTempAlertTime);
        hayStack.writePointForCcuUser(constantTempAlertTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,
                                      TunerConstants.CONSTANT_TEMP_ALERT_TIME, 0);
        hayStack.writeHisValById(constantTempAlertTimeId, TunerConstants.CONSTANT_TEMP_ALERT_TIME);
    
        Point abnormalCurTempRiseTrigger  = new Point.Builder()
                                                .setDisplayName(equipDis+"-"+"abnormalCurTempRiseTrigger")
                                                .setSiteRef(siteRef)
                                                .setEquipRef(equipRef).setHisInterpolate("cov")
                                                .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                .addMarker("abnormal").addMarker("cur").addMarker("temp").addMarker("rise").addMarker("trigger").addMarker("sp")
                                                .setMinVal("1").setMaxVal("20").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                .setUnit("\u00B0F")
                                                .setTz(tz)
                                                .build();
        String abnormalCurTempRiseTriggerId = hayStack.addPoint(abnormalCurTempRiseTrigger);
        hayStack.writePointForCcuUser(abnormalCurTempRiseTriggerId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,
                                      TunerConstants.ABNORMAL_TEMP_RISE_TRIGGER, 0);
        hayStack.writeHisValById(abnormalCurTempRiseTriggerId, TunerConstants.ABNORMAL_TEMP_RISE_TRIGGER);
    
        Point airflowSampleWaitTime  = new Point.Builder()
                                           .setDisplayName(equipDis+"-"+"airflowSampleWaitTime")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                           .addMarker("airflow").addMarker("sample").addMarker("wait").addMarker("time").addMarker("sp")
                                           .setMinVal("1").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                           .setUnit("m")
                                           .setTz(tz)
                                           .build();
        String airflowSampleWaitTimeId = hayStack.addPoint(airflowSampleWaitTime);
        hayStack.writePointForCcuUser(airflowSampleWaitTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, 5.0, 0);
        hayStack.writeHisValById(airflowSampleWaitTimeId, 5.0);
    
        Point stage1CoolingAirflowTempLowerOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage1CoolingAirflowTempLowerOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage1").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                                                         .setMinVal("-120").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage1CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage1CoolingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage1CoolingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,-20.0, 0);
        hayStack.writeHisValById(stage1CoolingAirflowTempLowerOffsetId, -20.0);
    
        Point stage1CoolingAirflowTempUpperOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage1CoolingAirflowTempUpperOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage1").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                                                         .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage1CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage1CoolingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage1CoolingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,-8.0, 0);
        hayStack.writeHisValById(stage1CoolingAirflowTempUpperOffsetId, -8.0);
    
        Point stage1HeatingAirflowTempUpperOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage1HeatingAirflowTempUpperOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage1").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                                                         .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage1HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage1HeatingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage1HeatingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,40.0, 0);
        hayStack.writeHisValById(stage1HeatingAirflowTempUpperOffsetId, 40.0);
    
        Point stage1HeatingAirflowTempLowerOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage1HeatingAirflowTempLowerOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage1").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                                                         .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage1HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage1HeatingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage1HeatingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,25.0, 0);
        hayStack.writeHisValById(stage1HeatingAirflowTempLowerOffsetId, 25.0);
    
        Point stage2CoolingAirflowTempLowerOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage2CoolingAirflowTempLowerOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage2").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                                                         .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage2CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage2CoolingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage2CoolingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,-25.0, 0);
        hayStack.writeHisValById(stage2CoolingAirflowTempLowerOffsetId, -25.0);
    
        Point stage2CoolingAirflowTempUpperOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage2CoolingAirflowTempUpperOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage2").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                                                         .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage2CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage2CoolingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage2CoolingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,-12.0, 0);
        hayStack.writeHisValById(stage2CoolingAirflowTempUpperOffsetId, -12.0);
    
        Point stage2HeatingAirflowTempUpperOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage2HeatingAirflowTempUpperOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage2").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                                                         .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage2HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage2HeatingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage2HeatingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,50.0, 0);
        hayStack.writeHisValById(stage2HeatingAirflowTempUpperOffsetId, 50.0);
    
        Point stage2HeatingAirflowTempLowerOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage2HeatingAirflowTempLowerOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage2").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                                                         .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage2HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage2HeatingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage2HeatingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,35.0, 0);
        hayStack.writeHisValById(stage2HeatingAirflowTempLowerOffsetId, 35.0);
    
        Point stage3CoolingAirflowTempLowerOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage3CoolingAirflowTempLowerOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage3").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                                                         .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage3CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage3CoolingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage3CoolingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,-25.0, 0);
        hayStack.writeHisValById(stage3CoolingAirflowTempLowerOffsetId, -25.0);
    
        Point stage3CoolingAirflowTempUpperOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage3CoolingAirflowTempUpperOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage3").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                                                         .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage3CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage3CoolingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage3CoolingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,-12.0, 0);
        hayStack.writeHisValById(stage3CoolingAirflowTempUpperOffsetId, -12.0);
    
        Point stage3HeatingAirflowTempUpperOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage3HeatingAirflowTempUpperOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage3").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                                                         .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage3HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage3HeatingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage3HeatingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,50.0, 0);
        hayStack.writeHisValById(stage3HeatingAirflowTempUpperOffsetId, 50.0);
    
        Point stage3HeatingAirflowTempLowerOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage3HeatingAirflowTempLowerOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage3").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                                                         .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage3HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage3HeatingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage3HeatingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,35.0, 0);
        hayStack.writeHisValById(stage3HeatingAirflowTempLowerOffsetId, 35.0);
    
        Point stage4CoolingAirflowTempLowerOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage4CoolingAirflowTempLowerOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage4").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                                                         .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage4CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage4CoolingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage4CoolingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,-25.0, 0);
        hayStack.writeHisValById(stage1CoolingAirflowTempLowerOffsetId, -25.0);
    
        Point stage4CoolingAirflowTempUpperOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage4CoolingAirflowTempUpperOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage4").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                                                         .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage4CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage4CoolingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage4CoolingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,-12.0, 0);
        hayStack.writeHisValById(stage4CoolingAirflowTempUpperOffsetId, -12.0);
    
        Point stage4HeatingAirflowTempUpperOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage4HeatingAirflowTempUpperOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage4").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                                                         .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage4HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage4HeatingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage4HeatingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,50.0, 0);
        hayStack.writeHisValById(stage4HeatingAirflowTempUpperOffsetId, 50.0);
    
        Point stage4HeatingAirflowTempLowerOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage4HeatingAirflowTempLowerOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage4").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                                                         .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage4HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage4HeatingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage4HeatingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,35.0, 0);
        hayStack.writeHisValById(stage4HeatingAirflowTempLowerOffsetId, 35.0);
    
        Point stage5CoolingAirflowTempLowerOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage5CoolingAirflowTempLowerOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage5").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                                                         .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage5CoolingAirflowTempLowerOffsetId = hayStack.addPoint(stage5CoolingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage5CoolingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,-25.0, 0);
        hayStack.writeHisValById(stage5CoolingAirflowTempLowerOffsetId, -25.0);
    
        Point stage5CoolingAirflowTempUpperOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage5CoolingAirflowTempUpperOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage5").addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                                                         .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage5CoolingAirflowTempUpperOffsetId = hayStack.addPoint(stage5CoolingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage5CoolingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,-12.0, 0);
        hayStack.writeHisValById(stage5CoolingAirflowTempUpperOffsetId, -12.0);
    
        Point stage5HeatingAirflowTempUpperOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage5HeatingAirflowTempUpperOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage5").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                                                         .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage5HeatingAirflowTempUpperOffsetId = hayStack.addPoint(stage5HeatingAirflowTempUpperOffset);
        hayStack.writePointForCcuUser(stage5HeatingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,50.0, 0);
        hayStack.writeHisValById(stage5HeatingAirflowTempUpperOffsetId, 50.0);
    
        Point stage5HeatingAirflowTempLowerOffset  = new Point.Builder()
                                                         .setDisplayName(equipDis+"-"+"stage5HeatingAirflowTempLowerOffset")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("default").addMarker("writable").addMarker("his").addMarker("his")
                                                         .addMarker("stage5").addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                                                         .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String stage5HeatingAirflowTempLowerOffsetId = hayStack.addPoint(stage5HeatingAirflowTempLowerOffset);
        hayStack.writePointForCcuUser(stage5HeatingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,35.0, 0);
        hayStack.writeHisValById(stage5HeatingAirflowTempLowerOffsetId, 35.0);
    
    }
}
