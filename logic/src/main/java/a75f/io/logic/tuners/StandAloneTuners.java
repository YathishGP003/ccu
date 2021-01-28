package a75f.io.logic.tuners;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;

public class StandAloneTuners {
    
    public static void addDefaultStandaloneTuners(CCUHsApi hayStack, String siteRef, String equipRef, String equipDis,
                                                  String tz) {

        Point saHeatingDeadBand = new Point.Builder()
                                      .setDisplayName(equipDis+"-standaloneHeatingDeadband")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("default").addMarker("base").addMarker("writable").addMarker("his")
                                      .addMarker("standalone").addMarker("heating").addMarker("deadband").addMarker("sp")
                                      .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                      .setUnit("\u00B0F")
                                      .setTz(tz)
                                      .build();
        String saHeatingDeadBandId = hayStack.addPoint(saHeatingDeadBand);
        hayStack.writePoint(saHeatingDeadBandId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_HEATING_DEADBAND_DEFAULT, 0);
        hayStack.writeHisValById(saHeatingDeadBandId, TunerConstants.STANDALONE_HEATING_DEADBAND_DEFAULT);

        Point saCoolingDeadBand = new Point.Builder()
                                      .setDisplayName(equipDis+"-standaloneCoolingDeadband")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                      .addMarker("cooling").addMarker("deadband").addMarker("sp")
                                      .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                      .setUnit("\u00B0F")
                                      .setTz(tz)
                                      .build();
        String saCoolingDeadBandId = hayStack.addPoint(saCoolingDeadBand);
        hayStack.writePoint(saCoolingDeadBandId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_COOLING_DEADBAND_DEFAULT, 0);
        hayStack.writeHisValById(saCoolingDeadBandId, TunerConstants.STANDALONE_COOLING_DEADBAND_DEFAULT);
        Point saStage1Hysteresis = new Point.Builder()
                                       .setDisplayName(equipDis+"-standaloneStage1Hysteresis")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                       .addMarker("stage1").addMarker("hysteresis").addMarker("sp")
                                       .setMinVal("0.5").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                       .setTz(tz)
                                       .build();
        String saStage1HysteresisId = hayStack.addPoint(saStage1Hysteresis);
        hayStack.writePoint(saStage1HysteresisId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_STAGE1_HYSTERESIS_DEFAULT, 0);
        hayStack.writeHisValById(saStage1HysteresisId, TunerConstants.STANDALONE_STAGE1_HYSTERESIS_DEFAULT);

        Point saAirflowSampleWaitTime = new Point.Builder()
                                            .setDisplayName(equipDis+"-standaloneAirflowSampleWaitTime")
                                            .setSiteRef(siteRef)
                                            .setEquipRef(equipRef).setHisInterpolate("cov")
                                            .addMarker("tuner").addMarker("default").addMarker("standalone").addMarker("base").addMarker("writable").addMarker("his")
                                            .addMarker("airflow").addMarker("sample").addMarker("wait").addMarker("time").addMarker("sp")
                                            .setMinVal("1").setMaxVal("100").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                            .setTz(tz)
                                            .build();
        String saAirflowSampleWaitTimeId = hayStack.addPoint(saAirflowSampleWaitTime);
        hayStack.writePoint(saAirflowSampleWaitTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_AIRFLOW_SAMPLE_WAIT_TIME, 0);
        hayStack.writeHisValById(saAirflowSampleWaitTimeId, TunerConstants.STANDALONE_AIRFLOW_SAMPLE_WAIT_TIME);

        Point saStage1CoolingLowerOffset = new Point.Builder()
                                               .setDisplayName(equipDis+"-standaloneStage1CoolingLowerOffset")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef).setHisInterpolate("cov")
                                               .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                               .addMarker("stage1").addMarker("cooling").addMarker("sp").addMarker("lower").addMarker("offset")
                                               .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                               .setTz(tz)
                                               .build();
        String saStage1CoolingLowerOffsetId = hayStack.addPoint(saStage1CoolingLowerOffset);
        hayStack.writePoint(saStage1CoolingLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_COOLING_STAGE1_LOWER_OFFSET, 0);
        hayStack.writeHisValById(saStage1CoolingLowerOffsetId, TunerConstants.STANDALONE_COOLING_STAGE1_LOWER_OFFSET);

        Point saStage1CoolingUpperOffset = new Point.Builder()
                                               .setDisplayName(equipDis+"-standaloneStage1CoolingUpperOffset")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef).setHisInterpolate("cov")
                                               .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                               .addMarker("stage1").addMarker("cooling").addMarker("sp").addMarker("upper").addMarker("offset")
                                               .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                               .setTz(tz)
                                               .build();
        String saStage1CoolingUpperOffsetId = hayStack.addPoint(saStage1CoolingUpperOffset);
        hayStack.writePoint(saStage1CoolingUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_COOLING_STAGE1_UPPER_OFFSET, 0);
        hayStack.writeHisValById(saStage1CoolingUpperOffsetId, TunerConstants.STANDALONE_COOLING_STAGE1_UPPER_OFFSET);

        Point saStage1HeatingLowerOffset = new Point.Builder()
                                               .setDisplayName(equipDis+"-standaloneStage1HeatingLowerOffset")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef).setHisInterpolate("cov")
                                               .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                               .addMarker("stage1").addMarker("heating").addMarker("sp").addMarker("lower").addMarker("offset")
                                               .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                               .setTz(tz)
                                               .build();
        String saStage1HeatingLowerOffsetId = hayStack.addPoint(saStage1HeatingLowerOffset);
        hayStack.writePoint(saStage1HeatingLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_HEATING_STAGE1_LOWER_OFFSET, 0);
        hayStack.writeHisValById(saStage1HeatingLowerOffsetId, TunerConstants.STANDALONE_HEATING_STAGE1_LOWER_OFFSET);

        Point saStage1HeatingUpperOffset = new Point.Builder()
                                               .setDisplayName(equipDis+"-standaloneStage1HeatingUpperOffset")
                                               .setSiteRef(siteRef)
                                               .setEquipRef(equipRef).setHisInterpolate("cov")
                                               .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                               .addMarker("stage1").addMarker("heating").addMarker("sp").addMarker("upper").addMarker("offset")
                                               .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                               .setTz(tz)
                                               .build();
        String saStage1HeatingUpperOffsetId = hayStack.addPoint(saStage1HeatingUpperOffset);
        hayStack.writePoint(saStage1HeatingUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_HEATING_STAGE1_UPPER_OFFSET, 0);
        hayStack.writeHisValById(saStage1HeatingUpperOffsetId, TunerConstants.STANDALONE_HEATING_STAGE1_UPPER_OFFSET);

        Point standaloneCoolingPreconditioningRate  = new Point.Builder()
                                                          .setDisplayName(equipDis+"-"+"standaloneCoolingPreconditioningRate")
                                                          .setSiteRef(siteRef)
                                                          .setEquipRef(equipRef).setHisInterpolate("cov")
                                                          .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                                          .addMarker("cooling").addMarker("preconditioning").addMarker("rate").addMarker("sp")
                                                          .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                                          .setUnit("\u00B0F")
                                                          .setTz(tz)
                                                          .build();
        String standaloneCoolingPreconditioningRateId = hayStack.addPoint(standaloneCoolingPreconditioningRate);
        hayStack.writePoint(standaloneCoolingPreconditioningRateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_COOLING_PRECONDITIONING_RATE, 0);
        hayStack.writeHisValById(standaloneCoolingPreconditioningRateId, TunerConstants.STANDALONE_COOLING_PRECONDITIONING_RATE);

        Point standaloneHeatingPreconditioningRate  = new Point.Builder()
                                                          .setDisplayName(equipDis+"-"+"standaloneHeatingPreconditioningRate")
                                                          .setSiteRef(siteRef)
                                                          .setEquipRef(equipRef).setHisInterpolate("cov")
                                                          .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                                          .addMarker("heating").addMarker("preconditioning").addMarker("rate").addMarker("sp")
                                                          .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                                          .setUnit("\u00B0F")
                                                          .setTz(tz)
                                                          .build();
        String standaloneHeatingPreconditioningRateId = hayStack.addPoint(standaloneHeatingPreconditioningRate);
        hayStack.writePoint(standaloneHeatingPreconditioningRateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_HEATING_PRECONDITIONING_RATE, 0);
        hayStack.writeHisValById(standaloneHeatingPreconditioningRateId, TunerConstants.STANDALONE_HEATING_PRECONDITIONING_RATE);


        Point zoneCO2Target  = new Point.Builder()
                                   .setDisplayName(equipDis+"-standaloneCO2Target")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef).setHisInterpolate("cov")
                                   .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                   .addMarker("zone").addMarker("co2").addMarker("target").addMarker("sp")
                                   .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                   .setUnit("ppm")
                                   .setTz(tz)
                                   .build();
        String zoneCO2TargetId = hayStack.addPoint(zoneCO2Target);
        hayStack.writePoint(zoneCO2TargetId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_CO2_TARGET, 0);
        hayStack.writeHisValById(zoneCO2TargetId, TunerConstants.ZONE_CO2_TARGET);

        Point zoneCO2Threshold  = new Point.Builder()
                                      .setDisplayName(equipDis+"-standaloneCO2Threshold")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                      .addMarker("zone").addMarker("co2").addMarker("threshold").addMarker("sp")
                                      .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                      .setUnit("ppm")
                                      .setTz(tz)
                                      .build();
        String zoneCO2ThresholdId = hayStack.addPoint(zoneCO2Threshold);
        hayStack.writePoint(zoneCO2ThresholdId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_CO2_THRESHOLD, 0);
        hayStack.writeHisValById(zoneCO2ThresholdId, TunerConstants.ZONE_CO2_THRESHOLD);

        Point zoneVOCTarget  = new Point.Builder()
                                   .setDisplayName(equipDis+"-standaloneVOCTarget")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipRef).setHisInterpolate("cov")
                                   .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                   .addMarker("zone").addMarker("voc").addMarker("target").addMarker("sp")
                                   .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                   .setUnit("ppb")
                                   .setTz(tz)
                                   .build();
        String zoneVOCTargetId = hayStack.addPoint(zoneVOCTarget);
        hayStack.writePoint(zoneVOCTargetId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_VOC_TARGET, 0);
        hayStack.writeHisValById(zoneVOCTargetId, TunerConstants.ZONE_VOC_TARGET);

        Point zoneVOCThreshold  = new Point.Builder()
                                      .setDisplayName(equipDis+"-standaloneVOCThreshold")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                      .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
                                      .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                      .setUnit("ppb")
                                      .setTz(tz)
                                      .build();
        String zoneVOCThresholdId = hayStack.addPoint(zoneVOCThreshold);
        hayStack.writePoint(zoneVOCThresholdId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.ZONE_VOC_THRESHOLD, 0);
        hayStack.writeHisValById(zoneVOCThresholdId, TunerConstants.ZONE_VOC_THRESHOLD);


        Point sa2PfcHeatingThreshold = new Point.Builder()
                                           .setDisplayName(equipDis+"-2PipeFancoilHeatingThreshold")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("default").addMarker("base").addMarker("writable").addMarker("his")
                                           .addMarker("standalone").addMarker("heating").addMarker("threshold").addMarker("pipe2").addMarker("fcu").addMarker("sp")
                                           .setMinVal("80").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                           .setUnit("\u00B0F")
                                           .setTz(tz)
                                           .build();
        String sa2PfcHeatingThresholdId = hayStack.addPoint(sa2PfcHeatingThreshold);
        hayStack.writePoint(sa2PfcHeatingThresholdId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_HEATING_THRESHOLD_2PFCU_DEFAULT, 0);
        hayStack.writeHisValById(sa2PfcHeatingThresholdId, TunerConstants.STANDALONE_HEATING_THRESHOLD_2PFCU_DEFAULT);

        Point sa2PfcCoolingThreshold = new Point.Builder()
                                           .setDisplayName(equipDis+"-2PipeFancoilCoolingThreshold")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("default").addMarker("base").addMarker("writable").addMarker("his")
                                           .addMarker("standalone").addMarker("cooling").addMarker("threshold").addMarker("pipe2").addMarker("fcu").addMarker("sp")
                                           .setMinVal("35").setMaxVal("70").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                           .setUnit("\u00B0F")
                                           .setTz(tz)
                                           .build();
        String sa2PfcCoolingThresholdId = hayStack.addPoint(sa2PfcCoolingThreshold);
        hayStack.writePoint(sa2PfcCoolingThresholdId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_COOLING_THRESHOLD_2PFCU_DEFAULT, 0);
        hayStack.writeHisValById(sa2PfcCoolingThresholdId, TunerConstants.STANDALONE_COOLING_THRESHOLD_2PFCU_DEFAULT);
        Point standaloneCoolingAirflowTempLowerOffset = new Point.Builder()
                                                            .setDisplayName(equipDis+"-standaloneStage2CoolingLowerOffset")
                                                            .setSiteRef(siteRef)
                                                            .setEquipRef(equipRef).setHisInterpolate("cov")
                                                            .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                                            .addMarker("cooling").addMarker("airflow").addMarker("lower").addMarker("sp").addMarker("temp").addMarker("offset").addMarker("stage2")
                                                            .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                            .setUnit("\u00B0F")
                                                            .setTz(tz)
                                                            .build();
        String standaloneCoolingAirflowTempLowerOffsetId = hayStack.addPoint(standaloneCoolingAirflowTempLowerOffset);
        hayStack.writePoint(standaloneCoolingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_COOLING_STAGE2_LOWER_OFFSET, 0);
        hayStack.writeHisValById(standaloneCoolingAirflowTempLowerOffsetId, TunerConstants.STANDALONE_COOLING_STAGE2_LOWER_OFFSET);

        Point standaloneCoolingAirflowTempUpperOffset = new Point.Builder()
                                                            .setDisplayName(equipDis+"-standaloneStage2CoolingUpperOffset")
                                                            .setSiteRef(siteRef)
                                                            .setEquipRef(equipRef).setHisInterpolate("cov")
                                                            .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                                            .addMarker("cooling").addMarker("airflow").addMarker("upper").addMarker("sp").addMarker("temp").addMarker("offset").addMarker("stage2")
                                                            .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                            .setUnit("\u00B0F")
                                                            .setTz(tz)
                                                            .build();
        String standaloneCoolingAirflowTempUpperOffsetId = hayStack.addPoint(standaloneCoolingAirflowTempUpperOffset);
        hayStack.writePoint(standaloneCoolingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_COOLING_STAGE2_UPPER_OFFSET, 0);
        hayStack.writeHisValById(standaloneCoolingAirflowTempUpperOffsetId, TunerConstants.STANDALONE_COOLING_STAGE2_UPPER_OFFSET);

        Point standaloneHeatingAirflowTempUpperOffset = new Point.Builder()
                                                            .setDisplayName(equipDis+"-standaloneStage2HeatingUpperOffset")
                                                            .setSiteRef(siteRef)
                                                            .setEquipRef(equipRef).setHisInterpolate("cov")
                                                            .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                                            .addMarker("heating").addMarker("airflow").addMarker("upper").addMarker("sp").addMarker("temp").addMarker("offset").addMarker("stage2")
                                                            .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                            .setUnit("\u00B0F")
                                                            .setTz(tz)
                                                            .build();
        String standaloneHeatingAirflowTempUpperOffsetId = hayStack.addPoint(standaloneHeatingAirflowTempUpperOffset);
        hayStack.writePoint(standaloneHeatingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_HEATING_STAGE2_UPPER_OFFSET, 0);
        hayStack.writeHisValById(standaloneHeatingAirflowTempUpperOffsetId, TunerConstants.STANDALONE_HEATING_STAGE2_UPPER_OFFSET);

        Point standaloneHeatingAirflowTempLowerOffset = new Point.Builder()
                                                            .setDisplayName(equipDis+"-standaloneStage2HeatingLowerOffset")
                                                            .setSiteRef(siteRef)
                                                            .setEquipRef(equipRef).setHisInterpolate("cov")
                                                            .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                                            .addMarker("heating").addMarker("airflow").addMarker("lower").addMarker("sp").addMarker("temp").addMarker("offset").addMarker("stage2")
                                                            .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                            .setUnit("\u00B0F")
                                                            .setTz(tz)
                                                            .build();
        String standaloneHeatingAirflowTempLowerOffsetId = hayStack.addPoint(standaloneHeatingAirflowTempLowerOffset);
        hayStack.writePoint(standaloneHeatingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, "ccu", TunerConstants.STANDALONE_HEATING_STAGE1_LOWER_OFFSET, 0);
        hayStack.writeHisValById(standaloneHeatingAirflowTempLowerOffsetId, TunerConstants.STANDALONE_HEATING_STAGE2_LOWER_OFFSET);
    }
    
    public static void addEquipZoneStandaloneTuners(CCUHsApi hayStack, String siteRef, String equipdis,
                                                    String equipref, String roomRef, String floorRef, String tz) {

        Point saHeatingDeadBand = new Point.Builder()
                                      .setDisplayName(equipdis+"-"+"standaloneHeatingDeadband")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .setRoomRef(roomRef)
                                      .setFloorRef(floorRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("base").addMarker("writable").addMarker("his")
                                      .addMarker("standalone").addMarker("heating").addMarker("deadband").addMarker("sp")
                                      .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                      .setUnit("\u00B0F")
                                      .setTz(tz)
                                      .build();
        String saHeatingDeadBandId = hayStack.addPoint(saHeatingDeadBand);
        HashMap
            saHeatingDeadBandPoint = hayStack.read("point and tuner and default and base and standalone and heating and deadband");
        ArrayList<HashMap> saHeatingDeadBandArr = hayStack.readPoint(saHeatingDeadBandPoint.get("id").toString());
        for (HashMap valMap : saHeatingDeadBandArr) {
            if (valMap.get("val") != null)
            {
                hayStack.pointWrite(HRef.copy(saHeatingDeadBandId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(saHeatingDeadBandId, HSUtil.getPriorityVal(saHeatingDeadBandId));

        Point saCoolingDeadBand = new Point.Builder()
                                      .setDisplayName(equipdis+"-"+"standaloneCoolingDeadband")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .setRoomRef(roomRef)
                                      .setFloorRef(floorRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                      .addMarker("cooling").addMarker("deadband").addMarker("sp")
                                      .setMinVal("0.1").setMaxVal("5.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                      .setUnit("\u00B0F")
                                      .setTz(tz)
                                      .build();
        String saCoolingDeadBandId = hayStack.addPoint(saCoolingDeadBand);
        HashMap saCoolingDeadBandPoint = hayStack.read("point and tuner and default and base and standalone and cooling and deadband");
        ArrayList<HashMap> saCoolingDeadBandArr = hayStack.readPoint(saCoolingDeadBandPoint.get("id").toString());
        for (HashMap valMap : saCoolingDeadBandArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(saCoolingDeadBandId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(saCoolingDeadBandId, HSUtil.getPriorityVal(saCoolingDeadBandId));

        Point saStage1Hysteresis = new Point.Builder()
                                       .setDisplayName(equipdis+"-"+"standaloneStage1Hysteresis")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipref)
                                       .setRoomRef(roomRef)
                                       .setFloorRef(floorRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                       .addMarker("stage1").addMarker("hysteresis").addMarker("sp")
                                       .setMinVal("0.5").setMaxVal("1.0").setIncrementVal("0.1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                       .setUnit("%")
                                       .setTz(tz)
                                       .build();
        String saStage1HysteresisId = hayStack.addPoint(saStage1Hysteresis);
        HashMap saStage1HysteresisPoint = hayStack.read("point and tuner and default and base and standalone and stage1 and hysteresis");
        ArrayList<HashMap> saStage1HysteresisArr = hayStack.readPoint(saStage1HysteresisPoint.get("id").toString());
        for (HashMap valMap : saStage1HysteresisArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(saStage1HysteresisId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(saStage1HysteresisId, HSUtil.getPriorityVal(saStage1HysteresisId));

        Point standaloneCoolingPreconditioningRate = new Point.Builder()
                                                         .setDisplayName(equipdis+"-"+"standaloneCoolingPreconditioningRate")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipref)
                                                         .setRoomRef(roomRef)
                                                         .setFloorRef(floorRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                                         .addMarker("cooling").addMarker("preconditioning").addMarker("rate").addMarker("sp")
                                                         .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String standaloneCoolingPreconditioningRateId = hayStack.addPoint(standaloneCoolingPreconditioningRate);
        HashMap standaloneCoolingPreconditioningRatePoint = hayStack.read("point and tuner and default and base and standalone and cooling and preconditioning and rate");
        ArrayList<HashMap> standaloneCoolingPreconditioningRateArr = hayStack.readPoint(standaloneCoolingPreconditioningRatePoint.get("id").toString());
        for (HashMap valMap : standaloneCoolingPreconditioningRateArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(standaloneCoolingPreconditioningRateId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(standaloneCoolingPreconditioningRateId, HSUtil.getPriorityVal(standaloneCoolingPreconditioningRateId));

        Point standaloneHeatingPreconditioningRate = new Point.Builder()
                                                         .setDisplayName(equipdis+"-"+"standaloneHeatingPreconditioningRate")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipref)
                                                         .setRoomRef(roomRef)
                                                         .setFloorRef(floorRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                                         .addMarker("heating").addMarker("preconditioning").addMarker("rate").addMarker("sp")
                                                         .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                                         .setUnit("\u00B0F")
                                                         .setTz(tz)
                                                         .build();
        String standaloneHeatingPreconditioningRateId = hayStack.addPoint(standaloneHeatingPreconditioningRate);
        HashMap standaloneHeatingPreconditioningRatePoint = hayStack.read("point and tuner and default and base and standalone and heating and preconditioning and rate");
        ArrayList<HashMap> standaloneHeatingPreconditioningRateArr = hayStack.readPoint(standaloneHeatingPreconditioningRatePoint.get("id").toString());
        for (HashMap valMap : standaloneHeatingPreconditioningRateArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(standaloneHeatingPreconditioningRateId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(standaloneHeatingPreconditioningRateId, HSUtil.getPriorityVal(standaloneHeatingPreconditioningRateId));

        Point standaloneCoolingAirflowTempLowerOffset = new Point.Builder()
                                                            .setDisplayName(equipdis+"-"+"standaloneCoolingAirflowTempLowerOffset")
                                                            .setSiteRef(siteRef)
                                                            .setEquipRef(equipref)
                                                            .setRoomRef(roomRef)
                                                            .setFloorRef(floorRef).setHisInterpolate("cov")
                                                            .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                                            .addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                                                            .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                            .setUnit("\u00B0F")
                                                            .setTz(tz)
                                                            .build();
        String standaloneCoolingAirflowTempLowerOffsetId = hayStack.addPoint(standaloneCoolingAirflowTempLowerOffset);
        HashMap standaloneCoolingAirflowTempLowerOffsetPoint = hayStack.read("point and tuner and default and base and standalone and cooling and airflow and temp and lower and offset");
        ArrayList<HashMap> standaloneCoolingAirflowTempLowerOffsetArr = hayStack.readPoint(standaloneCoolingAirflowTempLowerOffsetPoint.get("id").toString());
        for (HashMap valMap : standaloneCoolingAirflowTempLowerOffsetArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(standaloneCoolingAirflowTempLowerOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(standaloneCoolingAirflowTempLowerOffsetId, HSUtil.getPriorityVal(standaloneCoolingAirflowTempLowerOffsetId));

        Point standaloneCoolingAirflowTempUpperOffset = new Point.Builder()
                                                            .setDisplayName(equipdis+"-"+"standaloneCoolingAirflowTempUpperOffset")
                                                            .setSiteRef(siteRef)
                                                            .setEquipRef(equipref)
                                                            .setRoomRef(roomRef)
                                                            .setFloorRef(floorRef).setHisInterpolate("cov")
                                                            .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                                            .addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                                                            .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                            .setUnit("\u00B0F")
                                                            .setTz(tz)
                                                            .build();
        String standaloneCoolingAirflowTempUpperOffsetId = hayStack.addPoint(standaloneCoolingAirflowTempUpperOffset);
        HashMap standaloneCoolingAirflowTempUpperOffsetPoint = hayStack.read("point and tuner and default and base and standalone and cooling and airflow and temp and upper and offset");
        ArrayList<HashMap> standaloneCoolingAirflowTempUpperOffsetArr = hayStack.readPoint(standaloneCoolingAirflowTempUpperOffsetPoint.get("id").toString());
        for (HashMap valMap : standaloneCoolingAirflowTempUpperOffsetArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(standaloneCoolingAirflowTempUpperOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(standaloneCoolingAirflowTempUpperOffsetId, HSUtil.getPriorityVal(standaloneCoolingAirflowTempUpperOffsetId));

        Point standaloneHeatingAirflowTempLowerOffset = new Point.Builder()
                                                            .setDisplayName(equipdis+"-"+"standaloneHeatingAirflowTempLowerOffset")
                                                            .setSiteRef(siteRef)
                                                            .setEquipRef(equipref)
                                                            .setRoomRef(roomRef)
                                                            .setFloorRef(floorRef).setHisInterpolate("cov")
                                                            .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                                            .addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                                                            .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                            .setUnit("\u00B0F")
                                                            .setTz(tz)
                                                            .build();
        String standaloneHeatingAirflowTempLowerOffsetId = hayStack.addPoint(standaloneHeatingAirflowTempLowerOffset);
        HashMap standaloneHeatingAirflowTempLowerOffsetPoint = hayStack.read("point and tuner and default and base and standalone and heating and airflow and temp and lower and offset");
        ArrayList<HashMap> standaloneHeatingAirflowTempLowerOffsetArr = hayStack.readPoint(standaloneHeatingAirflowTempLowerOffsetPoint.get("id").toString());
        for (HashMap valMap : standaloneHeatingAirflowTempLowerOffsetArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(standaloneHeatingAirflowTempLowerOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(standaloneHeatingAirflowTempLowerOffsetId, HSUtil.getPriorityVal(standaloneHeatingAirflowTempLowerOffsetId));

        Point standaloneHeatingAirflowTempUpperOffset = new Point.Builder()
                                                            .setDisplayName(equipdis+"-"+"standaloneHeatingAirflowTempUpperOffset")
                                                            .setSiteRef(siteRef)
                                                            .setEquipRef(equipref)
                                                            .setRoomRef(roomRef)
                                                            .setFloorRef(floorRef).setHisInterpolate("cov")
                                                            .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                                            .addMarker("heating").addMarker("airflow").addMarker("temp").addMarker("upper").addMarker("offset").addMarker("sp")
                                                            .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                            .setUnit("\u00B0F")
                                                            .setTz(tz)
                                                            .build();
        String standaloneHeatingAirflowTempUpperOffsetId = hayStack.addPoint(standaloneHeatingAirflowTempUpperOffset);
        HashMap standaloneHeatingAirflowTempUpperOffsetPoint = hayStack.read("point and tuner and default and base and standalone and heating and airflow and temp and upper and offset");
        ArrayList<HashMap> standaloneHeatingAirflowTempUpperOffsetArr = hayStack.readPoint(standaloneHeatingAirflowTempUpperOffsetPoint.get("id").toString());
        for (HashMap valMap : standaloneHeatingAirflowTempUpperOffsetArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(standaloneHeatingAirflowTempUpperOffsetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(standaloneHeatingAirflowTempUpperOffsetId, HSUtil.getPriorityVal(standaloneHeatingAirflowTempUpperOffsetId));

        Point standaloneAirflowSampleWaitTime = new Point.Builder()
                                                    .setDisplayName(equipdis+"-"+"standaloneAirflowSampleWaitTime")
                                                    .setSiteRef(siteRef)
                                                    .setEquipRef(equipref)
                                                    .setRoomRef(roomRef)
                                                    .setFloorRef(floorRef).setHisInterpolate("cov")
                                                    .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                                    .addMarker("airflow").addMarker("sample").addMarker("wait").addMarker("time").addMarker("sp")
                                                    .setMinVal("0").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                    .setUnit("m")
                                                    .setTz(tz)
                                                    .build();
        String standaloneAirflowSampleWaitTimeId = hayStack.addPoint(standaloneAirflowSampleWaitTime);
        HashMap standaloneAirflowSampleWaitTimePoint = hayStack.read("point and tuner and default and base and standalone and airflow and sample and wait and time");
        ArrayList<HashMap> standaloneAirflowSampleWaitTimeArr = hayStack.readPoint(standaloneAirflowSampleWaitTimePoint.get("id").toString());
        for (HashMap valMap : standaloneAirflowSampleWaitTimeArr) {
            if (valMap.get("val") != null)
            {
                System.out.println(valMap);
                hayStack.pointWrite(HRef.copy(standaloneAirflowSampleWaitTimeId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
            }
        }
        hayStack.writeHisValById(standaloneAirflowSampleWaitTimeId, HSUtil.getPriorityVal(standaloneAirflowSampleWaitTimeId));


        Point zoneCO2Target  = new Point.Builder()
                                   .setDisplayName(equipdis+"-"+"standaloneCO2Target")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipref)
                                   .setRoomRef(roomRef)
                                   .setFloorRef(floorRef).setHisInterpolate("cov")
                                   .addMarker("tuner").addMarker("standalone").addMarker("writable").addMarker("his")
                                   .addMarker("zone").addMarker("co2").addMarker("target").addMarker("sp")
                                   .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                   .setUnit("ppm")
                                   .setTz(tz)
                                   .build();
        String zoneCO2TargetId = hayStack.addPoint(zoneCO2Target);

        HashMap zoneCO2TargetIdPoint = hayStack.read("point and tuner and default and base and standalone and zone and co2 and target and sp");
        if(zoneCO2TargetIdPoint != null && zoneCO2TargetIdPoint.get("id") != null) {
            ArrayList<HashMap> zoneCO2TargetIdArr = hayStack.readPoint(zoneCO2TargetIdPoint.get("id").toString());
            for (HashMap valMap : zoneCO2TargetIdArr) {
                if (valMap.get("val") != null) {
                    System.out.println(valMap);
                    hayStack.pointWrite(HRef.copy(zoneCO2TargetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(zoneCO2TargetId, HSUtil.getPriorityVal(zoneCO2TargetId));
        }
        Point zoneCO2Threshold  = new Point.Builder()
                                      .setDisplayName(equipdis+"-"+"standaloneCO2Threshold")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .setRoomRef(roomRef)
                                      .setFloorRef(floorRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("standalone").addMarker("writable").addMarker("his")
                                      .addMarker("zone").addMarker("co2").addMarker("threshold").addMarker("sp")
                                      .setMinVal("0").setMaxVal("2000").setIncrementVal("10").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                      .setUnit("ppm")
                                      .setTz(tz)
                                      .build();
        String zoneCO2ThresholdId = hayStack.addPoint(zoneCO2Threshold);

        HashMap zoneCO2ThresholdIdPoint = hayStack.read("point and tuner and default and base and standalone and zone and co2 and threshold and sp");
        if(zoneCO2ThresholdIdPoint != null && zoneCO2ThresholdIdPoint.get("id") != null) {
            ArrayList<HashMap> zoneCO2ThresholdIdArr = hayStack.readPoint(zoneCO2ThresholdIdPoint.get("id").toString());
            for (HashMap valMap : zoneCO2ThresholdIdArr) {
                if (valMap.get("val") != null) {
                    System.out.println(valMap);
                    hayStack.pointWrite(HRef.copy(zoneCO2ThresholdId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(zoneCO2ThresholdId, HSUtil.getPriorityVal(zoneCO2ThresholdId));
        }

        Point zoneVOCTarget  = new Point.Builder()
                                   .setDisplayName(equipdis+"-"+"standaloneVOCTarget")
                                   .setSiteRef(siteRef)
                                   .setEquipRef(equipref)
                                   .setRoomRef(roomRef)
                                   .setFloorRef(floorRef).setHisInterpolate("cov")
                                   .addMarker("tuner").addMarker("standalone").addMarker("writable").addMarker("his")
                                   .addMarker("zone").addMarker("voc").addMarker("target").addMarker("sp")
                                   .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                   .setUnit("ppb")
                                   .setTz(tz)
                                   .build();
        String zoneVOCTargetId = hayStack.addPoint(zoneVOCTarget);

        HashMap zoneVOCTargetIdPoint = hayStack.read("point and tuner and default and base and standalone and zone and voc and target and sp");
        if (zoneVOCTargetIdPoint != null && zoneVOCTargetIdPoint.get("id") != null) {
            ArrayList<HashMap> zoneVOCTargetIdArr = hayStack.readPoint(zoneVOCTargetIdPoint.get("id").toString());
            for (HashMap valMap : zoneVOCTargetIdArr) {
                if (valMap.get("val") != null) {
                    System.out.println(valMap);
                    hayStack.pointWrite(HRef.copy(zoneVOCTargetId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(zoneVOCTargetId, HSUtil.getPriorityVal(zoneVOCTargetId));
        }

        Point zoneVOCThreshold  = new Point.Builder()
                                      .setDisplayName(equipdis+"-"+"standaloneVOCThreshold")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .setRoomRef(roomRef)
                                      .setFloorRef(floorRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("standalone").addMarker("writable").addMarker("his")
                                      .addMarker("zone").addMarker("voc").addMarker("threshold").addMarker("sp")
                                      .setMinVal("0").setMaxVal("1000").setIncrementVal("10").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                      .setUnit("ppb")
                                      .setTz(tz)
                                      .build();
        String zoneVOCThresholdId = hayStack.addPoint(zoneVOCThreshold);

        HashMap zoneVOCThresholdIdPoint = hayStack.read("point and tuner and default and base and standalone and zone and voc and target and sp");
        if (zoneVOCThresholdIdPoint != null && zoneVOCThresholdIdPoint.get("id") != null) {
            ArrayList<HashMap> zoneVOCThresholdIdArr = hayStack.readPoint(zoneVOCThresholdIdPoint.get("id").toString());
            for (HashMap valMap : zoneVOCThresholdIdArr) {
                if (valMap.get("val") != null) {
                    System.out.println(valMap);
                    hayStack.pointWrite(HRef.copy(zoneVOCThresholdId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(zoneVOCThresholdId, HSUtil.getPriorityVal(zoneVOCThresholdId));
        }
    }
    
    public static void addTwoPipeFanEquipStandaloneTuners(CCUHsApi hayStack, String siteRef, String equipDis,
                                                          String equipRef, String roomRef, String floorRef, String tz) {

        Point sa2PfcHeatingThreshold = new Point.Builder()
                                           .setDisplayName(equipDis+"-2PipeFancoilHeatingThreshold")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("writable").addMarker("his")
                                           .addMarker("standalone").addMarker("heating").addMarker("threshold").addMarker("pipe2").addMarker("fcu").addMarker("sp")
                                           .setMinVal("80").setMaxVal("150").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                           .setUnit("\u00B0F")
                                           .setTz(tz)
                                           .build();
        String sa2PfcHeatingThresholdId = hayStack.addPoint(sa2PfcHeatingThreshold);
        HashMap sa2PfcHeatingThresholdIdPoint = hayStack.read("point and tuner and default and base and standalone and heating and threshold and pipe2 and fcu");
        if (sa2PfcHeatingThresholdIdPoint != null && sa2PfcHeatingThresholdIdPoint.get("id") != null) {
            ArrayList<HashMap> sa2PfcHeatingThresholdIdPointArr = hayStack.readPoint(sa2PfcHeatingThresholdIdPoint.get("id").toString());
            for (HashMap valMap : sa2PfcHeatingThresholdIdPointArr) {
                if (valMap.get("val") != null) {
                    System.out.println(valMap);
                    hayStack.pointWrite(HRef.copy(sa2PfcHeatingThresholdId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(sa2PfcHeatingThresholdId, HSUtil.getPriorityVal(sa2PfcHeatingThresholdId));
        }

        Point sa2PfcCoolingThreshold = new Point.Builder()
                                           .setDisplayName(equipDis+"-2PipeFancoilCoolingThreshold")
                                           .setSiteRef(siteRef)
                                           .setEquipRef(equipRef)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef).setHisInterpolate("cov")
                                           .addMarker("tuner").addMarker("writable").addMarker("his")
                                           .addMarker("standalone").addMarker("cooling").addMarker("threshold").addMarker("pipe2").addMarker("fcu").addMarker("sp")
                                           .setMinVal("35").setMaxVal("70").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                           .setUnit("\u00B0F")
                                           .setTz(tz)
                                           .build();
        String sa2PfcCoolingThresholdId = hayStack.addPoint(sa2PfcCoolingThreshold);
        HashMap sa2PfcCoolingThresholdIdPoint = hayStack.read("point and tuner and default and base and standalone and cooling and threshold and pipe2 and fcu");
        if (sa2PfcCoolingThresholdIdPoint != null && sa2PfcCoolingThresholdIdPoint.get("id") != null) {
            ArrayList<HashMap> sa2PfcCoolingThresholdIdPointArr = hayStack.readPoint(sa2PfcCoolingThresholdIdPoint.get("id").toString());
            for (HashMap valMap : sa2PfcCoolingThresholdIdPointArr) {
                if (valMap.get("val") != null) {
                    System.out.println(valMap);
                    hayStack.pointWrite(HRef.copy(sa2PfcCoolingThresholdId), (int) Double.parseDouble(valMap.get("level").toString()), valMap.get("who").toString(), HNum.make(Double.parseDouble(valMap.get("val").toString())), HNum.make(0));
                }
            }
            hayStack.writeHisValById(sa2PfcCoolingThresholdId, HSUtil.getPriorityVal(sa2PfcCoolingThresholdId));
        }
    }
    
    public static void addEquipStandaloneTuners(CCUHsApi hayStack, String siteRef, String equipdis, String equipref,
                                                String roomRef,
                                                String floorRef, String tz){
        ZoneTuners.addZoneTunersForEquip(hayStack, siteRef, equipdis,equipref, roomRef, floorRef, tz);
        addEquipZoneStandaloneTuners(hayStack, siteRef, equipdis,equipref, roomRef, floorRef, tz);
    }
    
}
