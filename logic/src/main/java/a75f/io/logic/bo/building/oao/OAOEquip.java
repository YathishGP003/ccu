package a75f.io.logic.bo.building.oao;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.definitions.ProfileType;
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
        Equip.Builder b = new Equip.Builder().setSiteRef(siteRef).setDisplayName(equipDis).setRoomRef(roomRef).setFloorRef(floorRef).setProfile(profileType.name()).addMarker("equip").addMarker("oao").addMarker("equipHis").setAhuRef(ahuRef).setTz(tz).setGroup(String.valueOf(nodeAddr));
        equipRef = hayStack.addEquip(b.build());
        
        OAOTuners.addEquipTuners(siteDis + "-OAO-" + nodeAddr, siteRef, equipRef, tz);
        
        createConfigPoints(config, equipRef);
    
        Point insideEnthalpy = new Point.Builder()
                                   .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-insideEnthalpy")
                                   .setEquipRef(equipRef)
                                   .setSiteRef(siteRef)
                                   .setRoomRef(roomRef)
                                   .setFloorRef(floorRef)
                                   .addMarker("oao").addMarker("inside").addMarker("enthalpy").addMarker("his").addMarker("equipHis")
                                   .setGroup(String.valueOf(nodeAddr))
                                   .setTz(tz)
                                   .build();
        hayStack.addPoint(insideEnthalpy);
    
        Point outsideEnthalpy = new Point.Builder()
                                       .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-outsideEnthalpy")
                                       .setEquipRef(equipRef)
                                       .setSiteRef(siteRef)
                                       .setRoomRef(roomRef)
                                       .setFloorRef(floorRef)
                                       .addMarker("oao").addMarker("outside").addMarker("enthalpy").addMarker("his").addMarker("equipHis")
                                       .setGroup(String.valueOf(nodeAddr))
                                       .setTz(tz)
                                       .build();
        hayStack.addPoint(outsideEnthalpy);
    
        Point economizingAvailable = new Point.Builder()
                                        .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-economizingAvailable")
                                        .setEquipRef(equipRef)
                                        .setSiteRef(siteRef)
                                        .setRoomRef(roomRef)
                                        .setFloorRef(floorRef)
                                        .addMarker("oao").addMarker("economizing").addMarker("available").addMarker("his").addMarker("equipHis")
                                        .setGroup(String.valueOf(nodeAddr))
                                        .setTz(tz)
                                        .build();
        hayStack.addPoint(economizingAvailable);
    
        Point economizingLoopOutput = new Point.Builder()
                                             .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-economizingLoopOutput")
                                             .setEquipRef(equipRef)
                                             .setSiteRef(siteRef)
                                             .setRoomRef(roomRef)
                                             .setFloorRef(floorRef)
                                             .addMarker("oao").addMarker("economizing").addMarker("loop").addMarker("output").addMarker("his").addMarker("equipHis")
                                             .setGroup(String.valueOf(nodeAddr))
                                             .setTz(tz)
                                             .build();
        hayStack.addPoint(economizingLoopOutput);
    
        Point outsideAirCalculatedMinDamper = new Point.Builder()
                                             .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-outsideAirCalculatedMinDamper")
                                             .setEquipRef(equipRef)
                                             .setSiteRef(siteRef)
                                             .setRoomRef(roomRef)
                                             .setFloorRef(floorRef)
                                             .addMarker("oao").addMarker("outside").addMarker("air").addMarker("calculated").addMarker("min").addMarker("damper").addMarker("his").addMarker("equipHis")
                                             .setGroup(String.valueOf(nodeAddr))
                                             .setTz(tz)
                                             .build();
        hayStack.addPoint(outsideAirCalculatedMinDamper);
        
        Point outsideAirLoopOutput = new Point.Builder()
                                              .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-outsideAirLoopOutput")
                                              .setEquipRef(equipRef)
                                              .setSiteRef(siteRef)
                                              .setRoomRef(roomRef)
                                              .setFloorRef(floorRef)
                                              .addMarker("oao").addMarker("outside").addMarker("air").addMarker("loop").addMarker("output").addMarker("intermediate").addMarker("his").addMarker("equipHis")
                                              .setGroup(String.valueOf(nodeAddr))
                                              .setTz(tz)
                                              .build();
        hayStack.addPoint(outsideAirLoopOutput);
    
        Point outsideAirFinalLoopOutput = new Point.Builder()
                                             .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-outsideAirFinalLoopOutput")
                                             .setEquipRef(equipRef)
                                             .setSiteRef(siteRef)
                                             .setRoomRef(roomRef)
                                             .setFloorRef(floorRef)
                                             .addMarker("oao").addMarker("outside").addMarker("air").addMarker("loop").addMarker("output").addMarker("final").addMarker("his").addMarker("equipHis")
                                             .setGroup(String.valueOf(nodeAddr))
                                             .setTz(tz)
                                             .build();
        String outsideAirFinalLoopOutputId = hayStack.addPoint(outsideAirFinalLoopOutput);
        
    }
    
    public void createConfigPoints(OAOProfileConfiguration config, String equipRef)
    {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String equipDis = siteDis + "-DAB-" + nodeAddr;
        String tz = siteMap.get("tz").toString();
        Point outsideDamperAtMinDrive  = new Point.Builder().setDisplayName(equipDis + "-outsideDamperAtMinDrive")
                                               .setEquipRef(equipRef)
                                               .setSiteRef(siteRef)
                                               .addMarker("config").addMarker("oao").addMarker("writable").addMarker("outside").addMarker("damper").addMarker("min").addMarker("drive").addMarker("sp")
                                               .setGroup(String.valueOf(nodeAddr))
                                               .setTz(tz).build();
        String outsideDamperAtMinDriveId = CCUHsApi.getInstance().addPoint(outsideDamperAtMinDrive );
        hayStack.writeDefaultValById(outsideDamperAtMinDriveId, config.outsideDamperAtMinDrive);
    
        Point outsideDamperAtMaxDrive  = new Point.Builder().setDisplayName(equipDis + "-outsideDamperAtMaxDrive")
                                                            .setEquipRef(equipRef)
                                                            .setSiteRef(siteRef)
                                                            .addMarker("config").addMarker("oao").addMarker("writable").addMarker("outside").addMarker("damper").addMarker("max").addMarker("drive").addMarker("sp")
                                                            .setGroup(String.valueOf(nodeAddr))
                                                            .setTz(tz).build();
        String outsideDamperAtMaxDriveId = CCUHsApi.getInstance().addPoint(outsideDamperAtMaxDrive );
        hayStack.writeDefaultValById(outsideDamperAtMaxDriveId, config.outsideDamperAtMaxDrive);
    
    
        Point outsideDamperMinOpen   = new Point.Builder().setDisplayName(equipDis + "-outsideDamperMinOpen")
                                                            .setEquipRef(equipRef)
                                                            .setSiteRef(siteRef)
                                                            .addMarker("config").addMarker("oao").addMarker("writable").addMarker("outside").addMarker("damper").addMarker("min").addMarker("open").addMarker("sp")
                                                            .setGroup(String.valueOf(nodeAddr))
                                                            .setTz(tz).build();
        String outsideDamperMinOpenId = CCUHsApi.getInstance().addPoint(outsideDamperMinOpen  );
        hayStack.writeDefaultValById(outsideDamperMinOpenId, config.outsideDamperMinOpen);
        
    
        Point returnDamperAtMinDrive  = new Point.Builder().setDisplayName(equipDis + "-returnDamperAtMinDrive")
                                                            .setEquipRef(equipRef)
                                                            .setSiteRef(siteRef)
                                                            .addMarker("config").addMarker("oao").addMarker("writable").addMarker("return").addMarker("damper").addMarker("min").addMarker("drive").addMarker("sp")
                                                            .setGroup(String.valueOf(nodeAddr))
                                                            .setTz(tz).build();
        String returnDamperAtMinDriveId = CCUHsApi.getInstance().addPoint(returnDamperAtMinDrive );
        hayStack.writeDefaultValById(returnDamperAtMinDriveId, config.returnDamperAtMinDrive);
    
        Point returnDamperAtMaxDrive  = new Point.Builder().setDisplayName(equipDis + "-returnDamperAtMaxDrive")
                                                            .setEquipRef(equipRef)
                                                            .setSiteRef(siteRef)
                                                            .addMarker("config").addMarker("oao").addMarker("writable").addMarker("return").addMarker("damper").addMarker("max").addMarker("drive").addMarker("sp")
                                                            .setGroup(String.valueOf(nodeAddr))
                                                            .setTz(tz).build();
        String returnDamperAtMaxDriveId = CCUHsApi.getInstance().addPoint(returnDamperAtMaxDrive);
        hayStack.writeDefaultValById(returnDamperAtMaxDriveId, config.returnDamperAtMaxDrive);
    
        Point returnDamperMinOpen  = new Point.Builder().setDisplayName(equipDis + "-returnDamperMinOpen")
                                                           .setEquipRef(equipRef)
                                                           .setSiteRef(siteRef)
                                                           .addMarker("config").addMarker("oao").addMarker("writable").addMarker("return").addMarker("damper").addMarker("min").addMarker("open").addMarker("sp")
                                                           .setGroup(String.valueOf(nodeAddr))
                                                           .setTz(tz).build();
        String returnDamperMinOpenId = CCUHsApi.getInstance().addPoint(returnDamperMinOpen );
        hayStack.writeDefaultValById(returnDamperMinOpenId, config.returnDamperMinOpen);
    
        Point exhaustFanStage1Threshold  = new Point.Builder().setDisplayName(equipDis + "-exhaustFanStage1Threshold")
                                                        .setEquipRef(equipRef)
                                                        .setSiteRef(siteRef)
                                                        .addMarker("config").addMarker("oao").addMarker("writable").addMarker("exhaust").addMarker("fan").addMarker("stage1").addMarker("threshold").addMarker("sp")
                                                        .setGroup(String.valueOf(nodeAddr))
                                                        .setTz(tz).build();
        String exhaustFanStage1ThresholdId = CCUHsApi.getInstance().addPoint(exhaustFanStage1Threshold );
        hayStack.writeDefaultValById(exhaustFanStage1ThresholdId, config.exhaustFanStage1Threshold);
    
        Point exhaustFanStage2Threshold  = new Point.Builder().setDisplayName(equipDis + "-exhaustFanStage2Threshold")
                                                              .setEquipRef(equipRef)
                                                              .setSiteRef(siteRef)
                                                              .addMarker("config").addMarker("oao").addMarker("writable").addMarker("exhaust").addMarker("fan").addMarker("stage2").addMarker("threshold").addMarker("sp")
                                                              .setGroup(String.valueOf(nodeAddr))
                                                              .setTz(tz).build();
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
                                                          .setSiteRef(siteRef)
                                                          .addMarker("config").addMarker("oao").addMarker("writable").addMarker("co2").addMarker("threshold").addMarker("sp")
                                                          .setGroup(String.valueOf(nodeAddr))
                                                          .setTz(tz).build();
        String co2ThresholdId = CCUHsApi.getInstance().addPoint(co2Threshold );
        hayStack.writeDefaultValById(co2ThresholdId, config.co2Threshold);
    
        Point exhaustFanHysteresis  = new Point.Builder().setDisplayName(equipDis + "-exhaustFanHysteresis")
                                                          .setEquipRef(equipRef)
                                                          .setSiteRef(siteRef)
                                                          .addMarker("config").addMarker("oao").addMarker("writable").addMarker("exhaust").addMarker("fan").addMarker("hysteresis").addMarker("sp")
                                                          .setGroup(String.valueOf(nodeAddr))
                                                          .setTz(tz).build();
        String exhaustFanHysteresisId = CCUHsApi.getInstance().addPoint(exhaustFanHysteresis );
        hayStack.writeDefaultValById(exhaustFanHysteresisId, config.exhaustFanHysteresis);
    
        Point usePerRoomCO2Sensing  = new Point.Builder().setDisplayName(equipDis + "-usePerRoomCO2Sensing")
                                                         .setEquipRef(equipRef)
                                                         .setSiteRef(siteRef)
                                                         .addMarker("config").addMarker("oao").addMarker("writable").addMarker("room").addMarker("co2").addMarker("sensing").addMarker("sp")
                                                         .setGroup(String.valueOf(nodeAddr))
                                                         .setTz(tz).build();
        String usePerRoomCO2SensingId = CCUHsApi.getInstance().addPoint(usePerRoomCO2Sensing );
        hayStack.writeDefaultValById(usePerRoomCO2SensingId, config.usePerRoomCO2Sensing ? 1.0 :0);
    }
    
    public void setConfigNumVal(String tags,double val) {
        hayStack.writeDefaultVal("point and config and oao and "+tags+" and group == \""+nodeAddr+"\"", val);
    }
    
    public double getConfigNumVal(String tags) {
        return hayStack.readDefaultVal("point and config and oao and "+tags+" and group == \""+nodeAddr+"\"");
    }
}
