package a75f.io.logic.bo.building.oao;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.NodeType;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.tuners.OAOTuners;

public class OAOEquip
{
    public int nodeAddr;
    ProfileType profileType;
    CCUHsApi hayStack = CCUHsApi.getInstance();
    String equipRef = null;
    
    public OAOEquip(ProfileType type, int node)
    {
        profileType = type;
        nodeAddr = node;
    }
    
    public void init() {
        HashMap equipMap = CCUHsApi.getInstance().read("equip and group == \"" + nodeAddr + "\"");
        if (equipMap != null && equipMap.size() > 0)
        {
            equipRef = equipMap.get("id").toString();
            OAOTuners.updateOaoSystemTuners(equipMap.get("siteRef").toString(), equipRef, equipMap.get("dis").toString(),equipMap.get("tz").toString(),getSystemProfileType());
            updateNewConfigParams(equipMap.get("siteRef").toString(), equipRef, equipMap.get("dis").toString(),equipMap.get("tz").toString());
        } else {
            throw new IllegalStateException("Equip should be created before init");
        }
    }
    
    
    public void createEntities(OAOProfileConfiguration config, String floorRef, String roomRef)
    {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-OAO-" + nodeAddr;
        String ahuRef = null;
        HashMap systemEquip = hayStack.read("equip and system");
        if (systemEquip != null && systemEquip.size() > 0)
        {
            ahuRef = systemEquip.get("id").toString();
        }
        Equip.Builder b = new Equip.Builder().setSiteRef(siteRef).setDisplayName(equipDis).setRoomRef(roomRef).setFloorRef(floorRef).setProfile(profileType.name()).addMarker("equip").addMarker("oao").setAhuRef(ahuRef).setTz(tz).setGroup(String.valueOf(nodeAddr));
        equipRef = hayStack.addEquip(b.build());
        
        OAOTuners.updateOaoSystemTuners( siteRef, equipRef,siteDis + "-OAO-" + nodeAddr, tz,getSystemProfileType());
        
        createConfigPoints(config, equipRef);
    
        Point insideEnthalpy = new Point.Builder()
                                   .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-insideEnthalpy")
                                   .setEquipRef(equipRef)
                                   .setSiteRef(siteRef)
                                   .setRoomRef(roomRef)
                                   .setFloorRef(floorRef).setHisInterpolate("cov")
                                   .addMarker("oao").addMarker("inside").addMarker("enthalpy").addMarker("his").addMarker("sp")
                                   .setGroup(String.valueOf(nodeAddr))
                                   .setUnit("BTU/lb")
                                   .setTz(tz)
                                   .build();
        hayStack.addPoint(insideEnthalpy);
    
        Point outsideEnthalpy = new Point.Builder()
                                       .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-outsideEnthalpy")
                                       .setEquipRef(equipRef)
                                       .setSiteRef(siteRef)
                                       .setRoomRef(roomRef)
                                       .setFloorRef(floorRef).setHisInterpolate("cov")
                                       .addMarker("oao").addMarker("outside").addMarker("enthalpy").addMarker("his").addMarker("sp")
                                       .setGroup(String.valueOf(nodeAddr))
                                       .setUnit("BTU/lb")
                                       .setTz(tz)
                                       .build();
        hayStack.addPoint(outsideEnthalpy);
    
        Point economizingAvailable = new Point.Builder()
                                        .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-economizingAvailable")
                                        .setEquipRef(equipRef)
                                        .setSiteRef(siteRef)
                                        .setRoomRef(roomRef)
                                        .setFloorRef(floorRef).setHisInterpolate("cov")
                                        .addMarker("oao").addMarker("economizing").addMarker("available").addMarker("his").addMarker("sp")
                                        .setGroup(String.valueOf(nodeAddr))
                                        .setTz(tz)
                                        .build();
        String economizingAvailableId = hayStack.addPoint(economizingAvailable);
    
        Point economizingLoopOutput = new Point.Builder()
                                             .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-economizingLoopOutput")
                                             .setEquipRef(equipRef)
                                             .setSiteRef(siteRef)
                                             .setRoomRef(roomRef)
                                             .setFloorRef(floorRef).setHisInterpolate("cov")
                                             .addMarker("oao").addMarker("economizing").addMarker("loop").addMarker("output").addMarker("his").addMarker("sp")
                                             .setGroup(String.valueOf(nodeAddr))
                                             .setUnit("%")
                                             .setTz(tz)
                                             .build();
        hayStack.addPoint(economizingLoopOutput);
    
        Point outsideAirCalculatedMinDamper = new Point.Builder()
                                             .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-outsideAirCalculatedMinDamper")
                                             .setEquipRef(equipRef)
                                             .setSiteRef(siteRef)
                                             .setRoomRef(roomRef)
                                             .setFloorRef(floorRef).setHisInterpolate("cov")
                                             .addMarker("oao").addMarker("outside").addMarker("air").addMarker("calculated").addMarker("min").addMarker("damper").addMarker("his").addMarker("sp")
                                             .setGroup(String.valueOf(nodeAddr))
                                             .setUnit("%")
                                             .setTz(tz)
                                             .build();
        hayStack.addPoint(outsideAirCalculatedMinDamper);
        
        Point outsideAirFinalLoopOutput = new Point.Builder()
                                             .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-outsideAirFinalLoopOutput")
                                             .setEquipRef(equipRef)
                                             .setSiteRef(siteRef)
                                             .setRoomRef(roomRef)
                                             .setFloorRef(floorRef).setHisInterpolate("cov")
                                             .addMarker("oao").addMarker("outside").addMarker("air").addMarker("loop").addMarker("output").addMarker("final").addMarker("his").addMarker("sp")
                                             .setGroup(String.valueOf(nodeAddr))
                                             .setUnit("%")
                                             .setTz(tz)
                                             .build();
        String outsideAirFinalLoopOutputId = hayStack.addPoint(outsideAirFinalLoopOutput);
    
    
        Point returnAirCO2 = new Point.Builder()
                                              .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-returnAirCO2")
                                              .setEquipRef(equipRef)
                                              .setSiteRef(siteRef)
                                              .setRoomRef(roomRef)
                                              .setFloorRef(floorRef).setHisInterpolate("cov")
                                              .addMarker("oao").addMarker("return").addMarker("air").addMarker("co2").addMarker("sensor").addMarker("his")
                                              .setGroup(String.valueOf(nodeAddr))
                                              .setUnit("ppm")
                                              .setTz(tz)
                                              .build();
        String returnAirCO2Id = hayStack.addPoint(returnAirCO2);
    
        Point rtuCurrentTransformer = new Point.Builder()
                                     .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-rtuCurrentTransformer")
                                     .setEquipRef(equipRef)
                                     .setSiteRef(siteRef)
                                     .setRoomRef(roomRef)
                                     .setFloorRef(floorRef).setHisInterpolate("cov")
                                     .addMarker("oao").addMarker("rtu").addMarker("current").addMarker("transformer").addMarker("sensor").addMarker("his")
                                     .setGroup(String.valueOf(nodeAddr))
                                     .setUnit("A")
                                     .setTz(tz)
                                     .build();
        String rtuCurrentTransformerId = hayStack.addPoint(rtuCurrentTransformer);
    
        Point outsideAirTemperature = new Point.Builder()
                                     .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-outsideAirTemperature")
                                     .setEquipRef(equipRef)
                                     .setSiteRef(siteRef)
                                     .setRoomRef(roomRef)
                                     .setFloorRef(floorRef).setHisInterpolate("cov")
                                     .addMarker("oao").addMarker("outside").addMarker("air").addMarker("temp").addMarker("sensor").addMarker("his")
                                     .setGroup(String.valueOf(nodeAddr))
                                     .setUnit("\u00B0F")
                                     .setTz(tz)
                                     .build();
        String outsideAirTemperatureId = hayStack.addPoint(outsideAirTemperature);
    
        Point weatherOutsideTemp = new Point.Builder()
                                         .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-weatherOutsideTemp")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .setRoomRef(roomRef)
                                         .setFloorRef(floorRef).setHisInterpolate("cov")
                                         .addMarker("oao").addMarker("outsideWeather").addMarker("air").addMarker("temp").addMarker("sensor").addMarker("his")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setUnit("\u00B0F")
                                         .setTz(tz)
                                         .build();
        String weatherOutsideTempId = hayStack.addPoint(weatherOutsideTemp);
    
        Point weatherOutsideHumidity = new Point.Builder()
                                              .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-weatherOutsideHumidity")
                                              .setEquipRef(equipRef)
                                              .setSiteRef(siteRef)
                                              .setRoomRef(roomRef)
                                              .setFloorRef(floorRef).setHisInterpolate("cov")
                                              .addMarker("oao").addMarker("outsideWeather").addMarker("air").addMarker("humidity").addMarker("sensor").addMarker("his")
                                              .setGroup(String.valueOf(nodeAddr))
                                              .setUnit("%")
                                              .setTz(tz)
                                              .build();
        String weatherOutsideHumidityId = hayStack.addPoint(weatherOutsideHumidity);
    
        Point supplyAirTemperature = new Point.Builder()
                                              .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-supplyAirTemperature")
                                              .setEquipRef(equipRef)
                                              .setSiteRef(siteRef)
                                              .setRoomRef(roomRef)
                                              .setFloorRef(floorRef).setHisInterpolate("cov")
                                              .addMarker("oao").addMarker("supply").addMarker("air").addMarker("temp").addMarker("sensor").addMarker("his")
                                              .setGroup(String.valueOf(nodeAddr))
                                              .setUnit("\u00B0F")
                                              .setTz(tz)
                                              .build();
        String supplyAirTemperatureId = hayStack.addPoint(supplyAirTemperature);
    
        Point mixedAirTemperature = new Point.Builder()
                                             .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-mixedAirTemperature")
                                             .setEquipRef(equipRef)
                                             .setSiteRef(siteRef)
                                             .setRoomRef(roomRef)
                                             .setFloorRef(floorRef).setHisInterpolate("cov")
                                             .addMarker("oao").addMarker("mixed").addMarker("air").addMarker("temp").addMarker("sensor").addMarker("his")
                                             .setGroup(String.valueOf(nodeAddr))
                                             .setUnit("\u00B0F")
                                             .setTz(tz)
                                             .build();
        String mixedAirTemperatureId = hayStack.addPoint(mixedAirTemperature);
    
        Point mixedAirHumidity = new Point.Builder()
                                            .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-mixedAirHumidity")
                                            .setEquipRef(equipRef)
                                            .setSiteRef(siteRef)
                                            .setRoomRef(roomRef)
                                            .setFloorRef(floorRef).setHisInterpolate("cov")
                                            .addMarker("oao").addMarker("mixed").addMarker("air").addMarker("humidity").addMarker("sensor").addMarker("his")
                                            .setGroup(String.valueOf(nodeAddr))
                                            .setUnit("%")
                                            .setTz(tz)
                                            .build();
        String mixedAirHumidityId = hayStack.addPoint(mixedAirHumidity);
    
        Point outsideAirDamper = new Point.Builder()
                                         .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-outsideAirDamper")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .setRoomRef(roomRef)
                                         .setFloorRef(floorRef).setHisInterpolate("cov")
                                         .addMarker("oao").addMarker("outside").addMarker("air").addMarker("damper").addMarker("cmd").addMarker("his")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setUnit("%")
                                         .setTz(tz)
                                         .build();
        String outsideAirDamperId = hayStack.addPoint(outsideAirDamper);
        Point returnAirDamper = new Point.Builder()
                                         .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-returnAirDamper")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .setRoomRef(roomRef)
                                         .setFloorRef(floorRef).setHisInterpolate("cov")
                                         .addMarker("oao").addMarker("return").addMarker("air").addMarker("damper").addMarker("cmd").addMarker("his")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setUnit("%")
                                         .setTz(tz)
                                         .build();
        String returnAirDamperId = hayStack.addPoint(returnAirDamper);
    
        Point exhaustFanStage1 = new Point.Builder()
                                        .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-exhaustFanStage1")
                                        .setEquipRef(equipRef)
                                        .setSiteRef(siteRef)
                                        .setRoomRef(roomRef)
                                        .setFloorRef(floorRef).setHisInterpolate("cov")
                                        .addMarker("oao").addMarker("exhaust").addMarker("fan").addMarker("stage1").addMarker("cmd").addMarker("his")
                                        .setGroup(String.valueOf(nodeAddr))
                                        .setTz(tz)
                                        .build();
        String exhaustFanStage1Id = hayStack.addPoint(exhaustFanStage1);
        Point exhaustFanStage2 = new Point.Builder()
                                         .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-exhaustFanStage2")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .setRoomRef(roomRef)
                                         .setFloorRef(floorRef).setHisInterpolate("cov")
                                         .addMarker("oao").addMarker("exhaust").addMarker("fan").addMarker("stage2").addMarker("cmd").addMarker("his")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setTz(tz)
                                         .build();
        String exhaustFanStage2Id = hayStack.addPoint(exhaustFanStage2);
    
        Point co2WA = new Point.Builder()
                                         .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-co2WeightedAverage")
                                         .setEquipRef(equipRef)
                                         .setSiteRef(siteRef)
                                         .setRoomRef(roomRef)
                                         .setFloorRef(floorRef).setHisInterpolate("cov")
                                         .addMarker("oao").addMarker("co2").addMarker("weighted").addMarker("average").addMarker("sp").addMarker("his")
                                         .setGroup(String.valueOf(nodeAddr))
                                         .setUnit("ppm")
                                         .setTz(tz)
                                         .build();
        String co2WAId = hayStack.addPoint(co2WA);
    
    
        SmartNode device = new SmartNode(nodeAddr, siteRef, floorRef, roomRef, equipRef);
        device.analog1In.setPointRef(returnAirCO2Id);
        device.analog1In.setEnabled(true);
        device.analog1In.setType("5");//TODO - Hard coding to CO2 sensor type.
        device.analog2In.setPointRef(rtuCurrentTransformerId);
        device.analog2In.setEnabled(true);
        device.analog2In.setType(String.valueOf(config.currentTranformerType));
    
        device.th1In.setPointRef(outsideAirTemperatureId);
        device.th1In.setEnabled(true);
        device.th2In.setPointRef(supplyAirTemperatureId);
        device.th2In.setEnabled(true);
        
        device.analog1Out.setEnabled(config.isOpConfigured(Port.ANALOG_OUT_ONE));
        device.analog1Out.setPointRef(outsideAirDamperId);
        device.analog1Out.setType(config.outsideDamperAtMinDrive+"-"+config.outsideDamperAtMaxDrive);
        device.analog2Out.setEnabled(config.isOpConfigured(Port.ANALOG_OUT_TWO));
        device.analog2Out.setPointRef(returnAirDamperId);
        device.analog2Out.setType(config.returnDamperAtMinDrive+"-"+config.returnDamperAtMaxDrive);
        
        device.relay1.setEnabled(config.isOpConfigured(Port.RELAY_ONE));
        device.relay1.setPointRef(exhaustFanStage1Id);
        device.relay1.setType(OutputRelayActuatorType.NormallyClose.displayName);
        device.relay2.setEnabled(config.isOpConfigured(Port.RELAY_TWO));
        device.relay2.setPointRef(exhaustFanStage2Id);
        device.relay2.setType(OutputRelayActuatorType.NormallyClose.displayName);
    
        device.currentTemp.setPointRef(mixedAirTemperatureId);
        device.addSensor(Port.SENSOR_RH, mixedAirHumidityId);
        //device.addSensor(Port.SENSOR_VOC, vocId);
    
        device.addPointsToDb();
        
        //init
        hayStack.writeHisValById(returnAirCO2Id,0.0);
        hayStack.writeHisValById(rtuCurrentTransformerId,0.0);
        hayStack.writeHisValById(outsideAirTemperatureId,0.0);
        hayStack.writeHisValById(supplyAirTemperatureId,0.0);
        hayStack.writeHisValById(mixedAirTemperatureId,0.0);
        hayStack.writeHisValById(mixedAirHumidityId,0.0);
        hayStack.writeHisValById(outsideAirDamperId,0.0);
        hayStack.writeHisValById(returnAirDamperId,0.0);
        hayStack.writeHisValById(exhaustFanStage1Id,0.0);
        hayStack.writeHisValById(exhaustFanStage2Id,0.0);
        hayStack.writeHisValById(economizingAvailableId, 0.0);
        hayStack.writeHisValById(weatherOutsideTempId, 0.0);
        hayStack.writeHisValById(weatherOutsideHumidityId, 0.0);
        hayStack.writeHisValById(co2WAId, 0.0);
    
        CCUHsApi.getInstance().syncEntityTree();
    }
    
    public void createConfigPoints(OAOProfileConfiguration config, String equipRef)
    {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String equipDis = siteDis + "-OAO-" + nodeAddr;
        String tz = siteMap.get("tz").toString();
        Point outsideDamperAtMinDrive  = new Point.Builder().setDisplayName(equipDis + "-outsideDamperAtMinDrive")
                                               .setEquipRef(equipRef)
                                               .setSiteRef(siteRef)
                                               .addMarker("config").addMarker("oao").addMarker("writable").addMarker("outside").addMarker("damper").addMarker("min").addMarker("drive").addMarker("sp")
                                               .setGroup(String.valueOf(nodeAddr))
                                               .setUnit("V").setTz(tz).build();
        String outsideDamperAtMinDriveId = CCUHsApi.getInstance().addPoint(outsideDamperAtMinDrive );
        hayStack.writeDefaultValById(outsideDamperAtMinDriveId, config.outsideDamperAtMinDrive);
    
        Point outsideDamperAtMaxDrive  = new Point.Builder().setDisplayName(equipDis + "-outsideDamperAtMaxDrive")
                                                            .setEquipRef(equipRef)
                                                            .setSiteRef(siteRef)
                                                            .addMarker("config").addMarker("oao").addMarker("writable").addMarker("outside").addMarker("damper").addMarker("max").addMarker("drive").addMarker("sp")
                                                            .setGroup(String.valueOf(nodeAddr))
                                                            .setUnit("V").setTz(tz).build();
        String outsideDamperAtMaxDriveId = CCUHsApi.getInstance().addPoint(outsideDamperAtMaxDrive );
        hayStack.writeDefaultValById(outsideDamperAtMaxDriveId, config.outsideDamperAtMaxDrive);
    
    
        Point outsideDamperMinOpen   = new Point.Builder().setDisplayName(equipDis + "-outsideDamperMinOpen")
                                                            .setEquipRef(equipRef)
                                                            .setSiteRef(siteRef)
                                                            .addMarker("config").addMarker("oao").addMarker("writable").addMarker("outside").addMarker("damper").addMarker("min").addMarker("open").addMarker("sp")
                                                            .setGroup(String.valueOf(nodeAddr))
                                                            .setUnit("%").setTz(tz).build();
        String outsideDamperMinOpenId = CCUHsApi.getInstance().addPoint(outsideDamperMinOpen  );
        hayStack.writeDefaultValById(outsideDamperMinOpenId, config.outsideDamperMinOpen);
        
    
        Point returnDamperAtMinDrive  = new Point.Builder().setDisplayName(equipDis + "-returnDamperAtMinDrive")
                                                            .setEquipRef(equipRef)
                                                            .setSiteRef(siteRef)
                                                            .addMarker("config").addMarker("oao").addMarker("writable").addMarker("return").addMarker("damper").addMarker("min").addMarker("drive").addMarker("sp")
                                                            .setGroup(String.valueOf(nodeAddr))
                                                            .setUnit("V").setTz(tz).build();
        String returnDamperAtMinDriveId = CCUHsApi.getInstance().addPoint(returnDamperAtMinDrive );
        hayStack.writeDefaultValById(returnDamperAtMinDriveId, config.returnDamperAtMinDrive);
    
        Point returnDamperAtMaxDrive  = new Point.Builder().setDisplayName(equipDis + "-returnDamperAtMaxDrive")
                                                            .setEquipRef(equipRef)
                                                            .setSiteRef(siteRef)
                                                            .addMarker("config").addMarker("oao").addMarker("writable").addMarker("return").addMarker("damper").addMarker("max").addMarker("drive").addMarker("sp")
                                                            .setGroup(String.valueOf(nodeAddr))
                                                            .setUnit("V").setTz(tz).build();
        String returnDamperAtMaxDriveId = CCUHsApi.getInstance().addPoint(returnDamperAtMaxDrive);
        hayStack.writeDefaultValById(returnDamperAtMaxDriveId, config.returnDamperAtMaxDrive);
    
        Point returnDamperMinOpen  = new Point.Builder().setDisplayName(equipDis + "-returnDamperMinOpen")
                                                           .setEquipRef(equipRef)
                                                           .setSiteRef(siteRef)
                                                           .addMarker("config").addMarker("oao").addMarker("writable").addMarker("return").addMarker("damper").addMarker("min").addMarker("open").addMarker("sp")
                                                           .setGroup(String.valueOf(nodeAddr))
                                                           .setUnit("%").setTz(tz).build();
        String returnDamperMinOpenId = CCUHsApi.getInstance().addPoint(returnDamperMinOpen );
        hayStack.writeDefaultValById(returnDamperMinOpenId, config.returnDamperMinOpen);
    
        Point exhaustFanStage1Threshold  = new Point.Builder().setDisplayName(equipDis + "-exhaustFanStage1Threshold")
                                                        .setEquipRef(equipRef)
                                                        .setSiteRef(siteRef)
                                                        .addMarker("config").addMarker("oao").addMarker("writable").addMarker("exhaust").addMarker("fan").addMarker("stage1").addMarker("threshold").addMarker("sp")
                                                        .setGroup(String.valueOf(nodeAddr))
                                                        .setUnit("%").setTz(tz).build();
        String exhaustFanStage1ThresholdId = CCUHsApi.getInstance().addPoint(exhaustFanStage1Threshold );
        hayStack.writeDefaultValById(exhaustFanStage1ThresholdId, config.exhaustFanStage1Threshold);
    
        Point exhaustFanStage2Threshold  = new Point.Builder().setDisplayName(equipDis + "-exhaustFanStage2Threshold")
                                                              .setEquipRef(equipRef)
                                                              .setSiteRef(siteRef)
                                                              .addMarker("config").addMarker("oao").addMarker("writable").addMarker("exhaust").addMarker("fan").addMarker("stage2").addMarker("threshold").addMarker("sp")
                                                              .setGroup(String.valueOf(nodeAddr))
                                                              .setUnit("%").setTz(tz).build();
        String exhaustFanStage2ThresholdId = CCUHsApi.getInstance().addPoint(exhaustFanStage2Threshold );
        hayStack.writeDefaultValById(exhaustFanStage2ThresholdId, config.exhaustFanStage2Threshold);
    
        Point currentTranformerType  = new Point.Builder().setDisplayName(equipDis + "-currentTranformerType")
                                                              .setEquipRef(equipRef)
                                                              .setSiteRef(siteRef)
                                                              .addMarker("config").addMarker("oao").addMarker("writable").addMarker("current").addMarker("transformer").addMarker("type").addMarker("sp")
                                                              .setGroup(String.valueOf(nodeAddr))
                                                              .setTz(tz).build();
        String currentTranformerTypeId = CCUHsApi.getInstance().addPoint(currentTranformerType );
        hayStack.writeDefaultValById(currentTranformerTypeId, config.currentTranformerType);
    
        Point co2Threshold  = new Point.Builder().setDisplayName(equipDis + "-co2Threshold")
                                                          .setEquipRef(equipRef)
                                                          .setSiteRef(siteRef).setHisInterpolate("cov")
                                                          .addMarker("config").addMarker("oao").addMarker("writable").addMarker("co2").addMarker("threshold").addMarker("sp").addMarker("his")
                                                          .setGroup(String.valueOf(nodeAddr))
                                                          .setUnit("ppm").setTz(tz).build();
        String co2ThresholdId = CCUHsApi.getInstance().addPoint(co2Threshold );
        hayStack.writeDefaultValById(co2ThresholdId, config.co2Threshold);
        hayStack.writeHisValById(co2ThresholdId,config.co2Threshold);
    
        Point exhaustFanHysteresis  = new Point.Builder().setDisplayName(equipDis + "-exhaustFanHysteresis")
                                                          .setEquipRef(equipRef)
                                                          .setSiteRef(siteRef)
                                                          .addMarker("config").addMarker("oao").addMarker("writable").addMarker("exhaust").addMarker("fan").addMarker("hysteresis").addMarker("sp")
                                                          .setGroup(String.valueOf(nodeAddr))
                                                          .setUnit("%").setTz(tz).build();
        String exhaustFanHysteresisId = CCUHsApi.getInstance().addPoint(exhaustFanHysteresis );
        hayStack.writeDefaultValById(exhaustFanHysteresisId, config.exhaustFanHysteresis);
    
        Point usePerRoomCO2Sensing  = new Point.Builder().setDisplayName(equipDis + "-usePerRoomCO2Sensing")
                                                         .setEquipRef(equipRef)
                                                         .setSiteRef(siteRef).setHisInterpolate("cov")
                                                         .addMarker("config").addMarker("oao").addMarker("writable").addMarker("co2").addMarker("sensing").addMarker("sp").addMarker("his")
                                                         .setGroup(String.valueOf(nodeAddr))
                                                         .setTz(tz).build();
        String usePerRoomCO2SensingId = CCUHsApi.getInstance().addPoint(usePerRoomCO2Sensing );
        hayStack.writeDefaultValById(usePerRoomCO2SensingId, config.usePerRoomCO2Sensing ? 1.0 :0);
        hayStack.writeHisValById(usePerRoomCO2SensingId, config.usePerRoomCO2Sensing ? 1.0 :0);


        Point smartPurgeMinDamperOpen  = new Point.Builder().setDisplayName(equipDis + "-systemPurgeOutsideDamperMinPos")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef).setHisInterpolate("cov")
                .addMarker("config").addMarker("oao").addMarker("writable").addMarker("purge").addMarker("damper").addMarker("sp").addMarker("his")
                .addMarker("pos").addMarker("min").addMarker("open").addMarker("outside").addMarker("userIntent")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("%")
                .setTz(tz).build();
        String smartPurgeMinDamperOpenId = CCUHsApi.getInstance().addPoint(smartPurgeMinDamperOpen );
        hayStack.writeDefaultValById(smartPurgeMinDamperOpenId, config.smartPurgeMinDamperOpen);
        hayStack.writeHisValById(smartPurgeMinDamperOpenId, config.smartPurgeMinDamperOpen);


        Point enhancedVentilationMinDamperOpen  = new Point.Builder().setDisplayName(equipDis + "-systemEnhancedVentilationOutsideDamperMinPos")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef).setHisInterpolate("cov")
                .addMarker("config").addMarker("oao").addMarker("writable").addMarker("enhanced").addMarker("ventilation").addMarker("damper").addMarker("sp").addMarker("his")
                .addMarker("pos").addMarker("min").addMarker("open").addMarker("outside").addMarker("userIntent")
                .setGroup(String.valueOf(nodeAddr))
                .setUnit("%")
                .setTz(tz).build();
        String enhancedVentilationMinDamperOpenId = CCUHsApi.getInstance().addPoint(enhancedVentilationMinDamperOpen );
        hayStack.writeDefaultValById(enhancedVentilationMinDamperOpenId, config.enhancedVentilationMinDamperOpen);
        hayStack.writeHisValById(enhancedVentilationMinDamperOpenId, config.enhancedVentilationMinDamperOpen);
    }
    public void updateNewConfigParams(String siteRef,String equipRef, String equipDis, String tz){
        ArrayList<HashMap> purgePoints = CCUHsApi.getInstance().readAll("point and userIntent and oao and purge and outside and damper and pos and min and open and equipRef == \"" + equipRef + "\"");
        if (purgePoints == null || purgePoints.size() == 0) {
            Point smartPurgeMinDamperOpen = new Point.Builder().setDisplayName(equipDis + "-systemPurgeOutsideDamperMinPos")
                    .setEquipRef(equipRef)
                    .setSiteRef(siteRef).setHisInterpolate("cov")
                    .addMarker("config").addMarker("oao").addMarker("writable").addMarker("purge").addMarker("damper").addMarker("sp").addMarker("his")
                    .addMarker("pos").addMarker("min").addMarker("open").addMarker("outside").addMarker("userIntent")
                    .setGroup(String.valueOf(nodeAddr))
                    .setUnit("%")
                    .setTz(tz).build();
            String smartPurgeMinDamperOpenId = CCUHsApi.getInstance().addPoint(smartPurgeMinDamperOpen);
            hayStack.writeDefaultValById(smartPurgeMinDamperOpenId, 100.0);
            hayStack.writeHisValById(smartPurgeMinDamperOpenId, 100.0);
        }
        ArrayList<HashMap> points = CCUHsApi.getInstance().readAll("point and userIntent and oao and enhanced and ventilation and outside and damper and pos and min and open and equipRef == \"" + equipRef + "\"");
        if (points == null || points.size() == 0) {
            Point enhancedVentilationMinDamperOpen = new Point.Builder().setDisplayName(equipDis + "-systemEnhancedVentilationOutsideDamperMinPos")
                    .setEquipRef(equipRef)
                    .setSiteRef(siteRef).setHisInterpolate("cov")
                    .addMarker("config").addMarker("oao").addMarker("writable").addMarker("enhanced").addMarker("ventilation").addMarker("damper").addMarker("sp").addMarker("his")
                    .addMarker("pos").addMarker("min").addMarker("open").addMarker("outside").addMarker("userIntent")
                    .setGroup(String.valueOf(nodeAddr))
                    .setUnit("%")
                    .setTz(tz).build();
            String enhancedVentilationMinDamperOpenId = CCUHsApi.getInstance().addPoint(enhancedVentilationMinDamperOpen);
            hayStack.writeDefaultValById(enhancedVentilationMinDamperOpenId, 50.0);
            hayStack.writeHisValById(enhancedVentilationMinDamperOpenId, 50.0);
        }
    }
    public double getHisVal(String tags) {
        return hayStack.readHisValByQuery("point and oao and "+tags+" and group == \""+nodeAddr+'\"');
    }
    
    public void setHisVal(String tags, double val) {
        hayStack.writeHisValByQuery("point and oao and "+tags+" and group == \""+nodeAddr+'\"', val);
    }
    
    public OAOProfileConfiguration getProfileConfiguration() {
        OAOProfileConfiguration config = new OAOProfileConfiguration();
      
        config.outsideDamperAtMinDrive = getConfigNumVal("outside and damper and min and drive");
        config.outsideDamperAtMaxDrive = getConfigNumVal("outside and damper and max and drive");
        config.returnDamperAtMinDrive =  getConfigNumVal("return and damper and min and drive");
        config.returnDamperAtMaxDrive =  getConfigNumVal("return and damper and max and drive");
        config.outsideDamperMinOpen = getConfigNumVal("outside and damper and min and open");
        config.returnDamperMinOpen = getConfigNumVal("return and damper and min and open");
        
        config.exhaustFanStage1Threshold = getConfigNumVal("exhaust and fan and stage1 and threshold") ;
        config.exhaustFanStage2Threshold = getConfigNumVal("exhaust and fan and stage2 and threshold") ;
        config.currentTranformerType = getConfigNumVal("current and transformer and type") ;
        config.co2Threshold = getConfigNumVal("co2 and threshold");
        config.exhaustFanHysteresis = getConfigNumVal("exhaust and fan and hysteresis");
        config.usePerRoomCO2Sensing = getConfigNumVal("config and oao and co2 and sensing") > 0? true : false;
        config.smartPurgeMinDamperOpen = getConfigNumVal("userIntent and purge and outside and damper and pos and min and open");
        config.enhancedVentilationMinDamperOpen = getConfigNumVal("userIntent and enhanced and ventilation and outside and damper and pos and min and open");
        
        config.setNodeType(NodeType.SMART_NODE);
        
        return config;
    }
    
    public void update(OAOProfileConfiguration config) {
    
        SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_OUT_ONE.name(), config.outsideDamperAtMinDrive+"-"+config.outsideDamperAtMaxDrive);
        SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_OUT_TWO.name(), config.returnDamperAtMinDrive+"-"+config.returnDamperAtMaxDrive);
    
        SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_IN_ONE.name(), "5");
        SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_IN_TWO.name(), String.valueOf(config.currentTranformerType));
        
        setConfigNumVal("outside and damper and min and drive", config.outsideDamperAtMinDrive);
        setConfigNumVal("outside and damper and max and drive", config.outsideDamperAtMaxDrive);
        setConfigNumVal("return and damper and min and drive", config.returnDamperAtMinDrive);
        setConfigNumVal("return and damper and max and drive", config.returnDamperAtMaxDrive);
        setConfigNumVal("outside and damper and min and open", config.outsideDamperMinOpen);
        setConfigNumVal("return and damper and min and open", config.returnDamperMinOpen);
        setConfigNumVal("exhaust and fan and stage1 and threshold", config.exhaustFanStage1Threshold);
        setConfigNumVal("exhaust and fan and stage2 and threshold", config.exhaustFanStage2Threshold);
        setConfigNumVal("current and transformer and type", config.currentTranformerType);
        setConfigNumVal("co2 and threshold", config.co2Threshold);
        setConfigNumVal("exhaust and fan and hysteresis", config.exhaustFanHysteresis);
        setConfigNumVal("co2 and sensing", config.usePerRoomCO2Sensing? 1:0);
        setConfigNumVal("userIntent and purge and outside and damper and pos and min and open",config.smartPurgeMinDamperOpen);
        setConfigNumVal("userIntent and enhanced and ventilation and outside and damper and pos and min and open",config.enhancedVentilationMinDamperOpen);
        CCUHsApi.getInstance().writeHisValByQuery("point and config and oao and co2 and sensing", config.usePerRoomCO2Sensing? 1.0:0);
        CCUHsApi.getInstance().writeHisValByQuery("point and config and oao and co2 and threshold", config.co2Threshold);
        CCUHsApi.getInstance().writeHisValByQuery("point and oao and userIntent and purge and outside and damper and pos and min and open",config.smartPurgeMinDamperOpen);
        CCUHsApi.getInstance().writeHisValByQuery("point and oao and userIntent and enhanced and ventilation and outside and damper and pos and min and open",config.enhancedVentilationMinDamperOpen);
        
    }
    
    public void setConfigNumVal(String tags,double val) {
        hayStack.writeDefaultVal("point and config and oao and "+tags+" and group == \""+nodeAddr+"\"", val);
    }
    
    public double getConfigNumVal(String tags) {
        return hayStack.readDefaultVal("point and config and oao and "+tags+" and group == \""+nodeAddr+"\"");
    }
    private String getSystemProfileType(){
        ProfileType profileType =  L.ccu().systemProfile.getProfileType();
        switch (profileType){
            case SYSTEM_DAB_ANALOG_RTU:
            case SYSTEM_DAB_HYBRID_RTU:
            case SYSTEM_DAB_STAGED_RTU:
            case SYSTEM_DAB_STAGED_VFD_RTU:
                return "dab";
            case SYSTEM_VAV_ANALOG_RTU:
            case SYSTEM_VAV_HYBRID_RTU:
            case SYSTEM_VAV_IE_RTU:
            case SYSTEM_VAV_STAGED_RTU:
            case SYSTEM_VAV_STAGED_VFD_RTU:
                return "vav";
        }
        return "default";
    }
}
