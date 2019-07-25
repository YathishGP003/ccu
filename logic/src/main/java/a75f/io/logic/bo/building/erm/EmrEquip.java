package a75f.io.logic.bo.building.erm;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.haystack.device.SmartNode;

public class EmrEquip
{
    ProfileType profileType ;
    int         nodeAddr;
    String equipRef = null;
    
    CCUHsApi hayStack = CCUHsApi.getInstance();
    public EmrEquip(ProfileType type, int node) {
        profileType = type;
        nodeAddr = node;
    }
    
    public void init() {
        if (equipRef == null) {
            HashMap equip = hayStack.read("equip and emr and group == \"" + nodeAddr + "\"");
            equipRef = equip.get("id").toString();
        }
    }
    
    public void createEntities(String floorRef, String roomRef)
    {
        HashMap siteMap = hayStack.read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String tz = siteMap.get("tz").toString();
        String equipDis = siteDis + "-EMR-" + nodeAddr;
        String ahuRef = null;
        HashMap systemEquip = hayStack.read("equip and system");
        if (systemEquip != null && systemEquip.size() > 0)
        {
            ahuRef = systemEquip.get("id").toString();
        }
        Equip b = new Equip.Builder().setSiteRef(siteRef)
                                     .setDisplayName(equipDis)
                                     .setRoomRef(roomRef)
                                     .setFloorRef(floorRef)
                                     .setProfile(profileType.name())
                                     .addMarker("equip").addMarker("emr").addMarker("zone")
                                     .addMarker("equipHis").setGatewayRef(ahuRef).setTz(tz).setGroup(String.valueOf(nodeAddr)).build();
        equipRef = hayStack.addEquip(b);
    
        Point emrReading  = new Point.Builder()
                                          .setDisplayName(equipDis+"-energyMeterReading")
                                          .setEquipRef(equipRef)
                                          .setSiteRef(siteRef)
                                          .setRoomRef(roomRef)
                                          .setFloorRef(floorRef)
                                          .addMarker("sp").addMarker("emr").addMarker("sensor").addMarker("zone").addMarker("his")
                                          .addMarker("cur").addMarker("logical").addMarker("equipHis")
                                          .setGroup(String.valueOf(nodeAddr))
                                          .setTz(tz)
                                          .build();
        String emrReadingId  = hayStack.addPoint(emrReading );
        hayStack.writeHisValById(emrReadingId, 0.0);
    
        Point currentRate  = new Point.Builder()
                                    .setDisplayName(equipDis+"-currentRate")
                                    .setEquipRef(equipRef)
                                    .setSiteRef(siteRef)
                                    .setRoomRef(roomRef)
                                    .setFloorRef(floorRef)
                                    .addMarker("sp").addMarker("emr").addMarker("zone").addMarker("his")
                                    .addMarker("current").addMarker("rate").addMarker("logical").addMarker("equipHis").addMarker("sp")
                                    .setGroup(String.valueOf(nodeAddr))
                                    .setTz(tz)
                                    .build();
        String currentRateId  = hayStack.addPoint(currentRate );
        hayStack.writeHisValById(currentRateId, 0.0);
    
        Point equipStatusMessage = new Point.Builder()
                                           .setDisplayName(equipDis+"-equipStatusMessage")
                                           .setEquipRef(equipRef)
                                           .setSiteRef(siteRef)
                                           .setRoomRef(roomRef)
                                           .setFloorRef(floorRef)
                                           .addMarker("status").addMarker("message").addMarker("pid").addMarker("writable").addMarker("logical").addMarker("zone").addMarker("his").addMarker("equipHis")
                                           .setGroup(String.valueOf(nodeAddr))
                                           .setTz(tz)
                                           .setKind("string")
                                           .build();
        String equipStatusMessageLd = CCUHsApi.getInstance().addPoint(equipStatusMessage);
        hayStack.writeDefaultValById(equipStatusMessageLd, "Output Loop Signal is 0%");
    
        Point equipScheduleType = new Point.Builder()
                                          .setDisplayName(equipDis+"-scheduleType")
                                          .setEquipRef(equipRef)
                                          .setSiteRef(siteRef)
                                          .setRoomRef(roomRef)
                                          .setFloorRef(floorRef)
                                          .addMarker("zone").addMarker("emr").addMarker("scheduleType").addMarker("writable").addMarker("his").addMarker("equipHis")
                                          .setGroup(String.valueOf(nodeAddr))
                                          .setTz(tz)
                                          .build();
        String equipScheduleTypeId = CCUHsApi.getInstance().addPoint(equipScheduleType);
        CCUHsApi.getInstance().writeDefaultValById(equipScheduleTypeId, 0.0);
        CCUHsApi.getInstance().writeHisValById(equipScheduleTypeId, 0.0);
    
        SmartNode device = new SmartNode(nodeAddr, siteRef, floorRef, roomRef, equipRef);
        device.addPointsToDb();
        
        device.addSensor(Port.SENSOR_ENERGY_METER, emrReadingId);
        hayStack.syncEntityTree();
    }
    
    public void setEquipStatus(String status)
    {
        hayStack.writeDefaultVal("point and status and message and equipRef == \""+equipRef+"\"", status);
        
    }
    
    public void setHisVal(String query, double val)
    {
        hayStack.writeHisValByQuery( query+" and equipRef == \""+equipRef+"\"", val);
    }
}
