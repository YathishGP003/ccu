package a75f.io.logic.tuners;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HisItem;
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
        hayStack.writePointForCcuUser(saHeatingDeadBandId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,TunerConstants.STANDALONE_HEATING_DEADBAND_DEFAULT, 0);
        hayStack.writeHisValById(saHeatingDeadBandId, TunerConstants.STANDALONE_HEATING_DEADBAND_DEFAULT);

        Point saCoolingDeadBand = new Point.Builder()
                                      .setDisplayName(equipDis+"-standaloneCoolingDeadband")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                      .addMarker("cooling").addMarker("deadband").addMarker("sp")
                                      .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                      .setUnit("\u00B0F")
                                      .setTz(tz)
                                      .build();
        String saCoolingDeadBandId = hayStack.addPoint(saCoolingDeadBand);
        hayStack.writePointForCcuUser(saCoolingDeadBandId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,TunerConstants.STANDALONE_COOLING_DEADBAND_DEFAULT, 0);
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
        hayStack.writePointForCcuUser(saStage1HysteresisId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,TunerConstants.STANDALONE_STAGE1_HYSTERESIS_DEFAULT, 0);
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
        hayStack.writePointForCcuUser(saAirflowSampleWaitTimeId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,TunerConstants.STANDALONE_AIRFLOW_SAMPLE_WAIT_TIME, 0);
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
        hayStack.writePointForCcuUser(saStage1CoolingLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,TunerConstants.STANDALONE_COOLING_STAGE1_LOWER_OFFSET, 0);
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
        hayStack.writePointForCcuUser(saStage1CoolingUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.STANDALONE_COOLING_STAGE1_UPPER_OFFSET, 0);
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
        hayStack.writePointForCcuUser(saStage1HeatingLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,TunerConstants.STANDALONE_HEATING_STAGE1_LOWER_OFFSET, 0);
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
        hayStack.writePointForCcuUser(saStage1HeatingUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,TunerConstants.STANDALONE_HEATING_STAGE1_UPPER_OFFSET, 0);
        hayStack.writeHisValById(saStage1HeatingUpperOffsetId, TunerConstants.STANDALONE_HEATING_STAGE1_UPPER_OFFSET);

        Point standaloneCoolingPreconditioningRate  = new Point.Builder()
                                                          .setDisplayName(equipDis+"-"+"standaloneCoolingPreconditioningRate")
                                                          .setSiteRef(siteRef)
                                                          .setEquipRef(equipRef).setHisInterpolate("cov")
                                                          .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                                          .addMarker("cooling").addMarker("preconditioning").addMarker("rate").addMarker("sp")
                                                          .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                                          .setUnit("minute")
                                                          .setTz(tz)
                                                          .build();
        String standaloneCoolingPreconditioningRateId = hayStack.addPoint(standaloneCoolingPreconditioningRate);
        hayStack.writePointForCcuUser(standaloneCoolingPreconditioningRateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,TunerConstants.STANDALONE_COOLING_PRECONDITIONING_RATE, 0);
        hayStack.writeHisValById(standaloneCoolingPreconditioningRateId, TunerConstants.STANDALONE_COOLING_PRECONDITIONING_RATE);

        Point standaloneHeatingPreconditioningRate  = new Point.Builder()
                                                          .setDisplayName(equipDis+"-"+"standaloneHeatingPreconditioningRate")
                                                          .setSiteRef(siteRef)
                                                          .setEquipRef(equipRef).setHisInterpolate("cov")
                                                          .addMarker("tuner").addMarker("default").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                                          .addMarker("heating").addMarker("preconditioning").addMarker("rate").addMarker("sp")
                                                          .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                                          .setUnit("minute")
                                                          .setTz(tz)
                                                          .build();
        String standaloneHeatingPreconditioningRateId = hayStack.addPoint(standaloneHeatingPreconditioningRate);
        hayStack.writePointForCcuUser(standaloneHeatingPreconditioningRateId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL, TunerConstants.STANDALONE_HEATING_PRECONDITIONING_RATE, 0);
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
        hayStack.writePointForCcuUser(zoneCO2TargetId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.ZONE_CO2_TARGET, 0);
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
        hayStack.writePointForCcuUser(zoneCO2ThresholdId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.ZONE_CO2_THRESHOLD, 0);
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
        hayStack.writePointForCcuUser(zoneVOCTargetId, TunerConstants.VAV_DEFAULT_VAL_LEVEL,TunerConstants.ZONE_VOC_TARGET, 0);
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
        hayStack.writePointForCcuUser(zoneVOCThresholdId, TunerConstants.VAV_DEFAULT_VAL_LEVEL, TunerConstants.ZONE_VOC_THRESHOLD, 0);
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
        hayStack.writePointForCcuUser(sa2PfcHeatingThresholdId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,TunerConstants.STANDALONE_HEATING_THRESHOLD_2PFCU_DEFAULT, 0);
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
        hayStack.writePointForCcuUser(sa2PfcCoolingThresholdId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,TunerConstants.STANDALONE_COOLING_THRESHOLD_2PFCU_DEFAULT, 0);
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
        hayStack.writePointForCcuUser(standaloneCoolingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,TunerConstants.STANDALONE_COOLING_STAGE2_LOWER_OFFSET, 0);
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
        hayStack.writePointForCcuUser(standaloneCoolingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,TunerConstants.STANDALONE_COOLING_STAGE2_UPPER_OFFSET, 0);
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
        hayStack.writePointForCcuUser(standaloneHeatingAirflowTempUpperOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,TunerConstants.STANDALONE_HEATING_STAGE2_UPPER_OFFSET, 0);
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
        hayStack.writePointForCcuUser(standaloneHeatingAirflowTempLowerOffsetId, TunerConstants.SYSTEM_DEFAULT_VAL_LEVEL,TunerConstants.STANDALONE_HEATING_STAGE1_LOWER_OFFSET, 0);
        hayStack.writeHisValById(standaloneHeatingAirflowTempLowerOffsetId, TunerConstants.STANDALONE_HEATING_STAGE2_LOWER_OFFSET);
    }
    
    public static void addEquipZoneStandaloneTuners(CCUHsApi hayStack, String siteRef, String equipdis,
                                                    String equipref, String roomRef, String floorRef, String tz) {
    
        List<HisItem> hisItems = new ArrayList<>();
        Point saHeatingDeadBand = new Point.Builder()
                                      .setDisplayName(equipdis+"-"+"standaloneHeatingDeadband")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .setRoomRef(roomRef)
                                      .setFloorRef(floorRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("base").addMarker("writable").addMarker("his")
                                      .addMarker("standalone").addMarker("heating").addMarker("deadband").addMarker("sp")
                                      .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                      .setUnit("\u00B0F")
                                      .setTz(tz)
                                      .build();
        String saHeatingDeadBandId = hayStack.addPoint(saHeatingDeadBand);
        BuildingTunerUtil.updateTunerLevels(saHeatingDeadBandId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(saHeatingDeadBandId));

        Point saCoolingDeadBand = new Point.Builder()
                                      .setDisplayName(equipdis+"-"+"standaloneCoolingDeadband")
                                      .setSiteRef(siteRef)
                                      .setEquipRef(equipref)
                                      .setRoomRef(roomRef)
                                      .setFloorRef(floorRef).setHisInterpolate("cov")
                                      .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                      .addMarker("cooling").addMarker("deadband").addMarker("sp")
                                      .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                      .setUnit("\u00B0F")
                                      .setTz(tz)
                                      .build();
        String saCoolingDeadBandId = hayStack.addPoint(saCoolingDeadBand);
        BuildingTunerUtil.updateTunerLevels(saCoolingDeadBandId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(saCoolingDeadBandId));

        Point saStage1Hysteresis = new Point.Builder()
                                       .setDisplayName(equipdis+"-"+"standaloneStage1Hysteresis")
                                       .setSiteRef(siteRef)
                                       .setEquipRef(equipref)
                                       .setRoomRef(roomRef)
                                       .setFloorRef(floorRef).setHisInterpolate("cov")
                                       .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                       .addMarker("stage1").addMarker("hysteresis").addMarker("sp")
                                       .setMinVal("0").setMaxVal("10.0").setIncrementVal("0.5").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                       .setUnit("%")
                                       .setTz(tz)
                                       .build();
        String saStage1HysteresisId = hayStack.addPoint(saStage1Hysteresis);
        BuildingTunerUtil.updateTunerLevels(saStage1HysteresisId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(saStage1HysteresisId));

        Point standaloneCoolingPreconditioningRate = new Point.Builder()
                                                         .setDisplayName(equipdis+"-"+"standaloneCoolingPreconditioningRate")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipref)
                                                         .setRoomRef(roomRef)
                                                         .setFloorRef(floorRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                                         .addMarker("cooling").addMarker("preconditioning").addMarker("rate").addMarker("sp")
                                                         .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                                         .setUnit("minute")
                                                         .setTz(tz)
                                                         .build();
        String standaloneCoolingPreconditioningRateId = hayStack.addPoint(standaloneCoolingPreconditioningRate);
        BuildingTunerUtil.updateTunerLevels(standaloneCoolingPreconditioningRateId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(standaloneCoolingPreconditioningRateId));
        
        Point standaloneHeatingPreconditioningRate = new Point.Builder()
                                                         .setDisplayName(equipdis+"-"+"standaloneHeatingPreconditioningRate")
                                                         .setSiteRef(siteRef)
                                                         .setEquipRef(equipref)
                                                         .setRoomRef(roomRef)
                                                         .setFloorRef(floorRef).setHisInterpolate("cov")
                                                         .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                                         .addMarker("heating").addMarker("preconditioning").addMarker("rate").addMarker("sp")
                                                         .setMinVal("0").setMaxVal("60").setIncrementVal("1").setTunerGroup(TunerConstants.GENERIC_TUNER_GROUP)
                                                         .setUnit("minute")
                                                         .setTz(tz)
                                                         .build();
        String standaloneHeatingPreconditioningRateId = hayStack.addPoint(standaloneHeatingPreconditioningRate);
        BuildingTunerUtil.updateTunerLevels(standaloneHeatingPreconditioningRateId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(standaloneHeatingPreconditioningRateId));
        
        Point standaloneCoolingAirflowTempLowerOffset = new Point.Builder()
                                                            .setDisplayName(equipdis+"-"+"standaloneCoolingAirflowTempLowerOffset")
                                                            .setSiteRef(siteRef)
                                                            .setEquipRef(equipref)
                                                            .setRoomRef(roomRef)
                                                            .setFloorRef(floorRef).setHisInterpolate("cov")
                                                            .addMarker("tuner").addMarker("base").addMarker("standalone").addMarker("writable").addMarker("his")
                                                            .addMarker("cooling").addMarker("airflow").addMarker("temp").addMarker("lower").addMarker("offset").addMarker("sp")
                                                            .setMinVal("-150").setMaxVal("0").setIncrementVal("1").setTunerGroup(TunerConstants.ALERT_TUNER)
                                                            .setUnit("minute")
                                                            .setTz(tz)
                                                            .build();
        String standaloneCoolingAirflowTempLowerOffsetId = hayStack.addPoint(standaloneCoolingAirflowTempLowerOffset);
        BuildingTunerUtil.updateTunerLevels(standaloneCoolingAirflowTempLowerOffsetId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(standaloneCoolingAirflowTempLowerOffsetId));
        
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
        BuildingTunerUtil.updateTunerLevels(standaloneCoolingAirflowTempUpperOffsetId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(standaloneCoolingAirflowTempUpperOffsetId));
        
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
        BuildingTunerUtil.updateTunerLevels(standaloneHeatingAirflowTempLowerOffsetId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(standaloneHeatingAirflowTempLowerOffsetId));
        
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
        BuildingTunerUtil.updateTunerLevels(standaloneHeatingAirflowTempUpperOffsetId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(standaloneHeatingAirflowTempUpperOffsetId));
        
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
        BuildingTunerUtil.updateTunerLevels(standaloneAirflowSampleWaitTimeId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(standaloneAirflowSampleWaitTimeId));

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
        BuildingTunerUtil.updateTunerLevels(zoneCO2TargetId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(zoneCO2TargetId));

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
        BuildingTunerUtil.updateTunerLevels(zoneCO2ThresholdId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(zoneCO2ThresholdId));

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
        BuildingTunerUtil.updateTunerLevels(zoneVOCTargetId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(zoneVOCTargetId));
    
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
        BuildingTunerUtil.updateTunerLevels(zoneVOCThresholdId, roomRef, hayStack);
        hisItems.add(HSUtil.getHisItemForWritable(zoneVOCThresholdId));
    
        hayStack.writeHisValueByIdWithoutCOV(hisItems);
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
        BuildingTunerUtil.updateTunerLevels(sa2PfcHeatingThresholdId, roomRef, hayStack);
        hayStack.writeHisValueByIdWithoutCOV(sa2PfcHeatingThresholdId, HSUtil.getPriorityVal(sa2PfcHeatingThresholdId));
    
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
        BuildingTunerUtil.updateTunerLevels(sa2PfcCoolingThresholdId, roomRef, hayStack);
        hayStack.writeHisValueByIdWithoutCOV(sa2PfcCoolingThresholdId, HSUtil.getPriorityVal(sa2PfcCoolingThresholdId));
    }
    
    public static void addEquipStandaloneTuners(CCUHsApi hayStack, String siteRef, String equipdis, String equipref,
                                                String roomRef,
                                                String floorRef, String tz){
        ZoneTuners.addZoneTunersForEquip(hayStack, siteRef, equipdis,equipref, roomRef, floorRef, tz);
        addEquipZoneStandaloneTuners(hayStack, siteRef, equipdis,equipref, roomRef, floorRef, tz);
    }
    
}
