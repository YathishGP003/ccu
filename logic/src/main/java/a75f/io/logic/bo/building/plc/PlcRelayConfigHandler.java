package a75f.io.logic.bo.building.plc;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.logic.bo.haystack.device.SmartNode;

public class PlcRelayConfigHandler {
    
    public static void createRelayConfigPoints(Equip equip, PlcProfileConfiguration config, SmartNode device,
                                               String relayType, CCUHsApi hayStack) {
    
        Point relayConfig = new Point.Builder()
                                    .setDisplayName(equip.getDisplayName() + "-"+relayType+"ConfigEnabled")
                                    .setEquipRef(equip.getId())
                                    .setSiteRef(equip.getSiteRef())
                                    .setRoomRef(equip.getRoomRef())
                                    .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                    .addMarker("sp").addMarker("pid").addMarker("zone").addMarker("his").addMarker("logical")
                                    .addMarker("config").addMarker(relayType).addMarker("enabled")
                                    .setUnit("%")
                                    .setGroup(equip.getGroup())
                                    .setTz(hayStack.getTimeZone())
                                    .build();
        String relayConfigId = hayStack.addPoint(relayConfig);
        hayStack.writeDefaultValById(relayConfigId, 0.0);
    
    
        Point relayCmd = new Point.Builder()
                                 .setDisplayName(equip.getDisplayName() + "-"+relayType+"Cmd")
                                 .setEquipRef(equip.getId())
                                 .setSiteRef(equip.getSiteRef())
                                 .setRoomRef(equip.getRoomRef())
                                 .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                 .addMarker("pid").addMarker("zone").addMarker("his").addMarker("logical")
                                 .addMarker("cmd").addMarker(relayType)
                                 .setUnit("%")
                                 .setGroup(equip.getGroup())
                                 .setTz(hayStack.getTimeZone())
                                 .build();
        String relayCmdId = hayStack.addPoint(relayCmd);
        hayStack.writeHisValById(relayCmdId, 0.0);
        
        if (relayType.contains("relay1")) {
            device.relay1.setPointRef(relayCmdId);
            device.relay1.setEnabled(true);
        } else if (relayType.contains("relay2")) {
            device.relay2.setPointRef(relayCmdId);
            device.relay2.setEnabled(true);
        }
    
        Point relayOnThreshold = new Point.Builder()
                                 .setDisplayName(equip.getDisplayName() + "-"+relayType+"OnThreshold")
                                 .setEquipRef(equip.getId())
                                 .setSiteRef(equip.getSiteRef())
                                 .setRoomRef(equip.getRoomRef())
                                 .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                 .addMarker("sp").addMarker("pid").addMarker("zone").addMarker("his").addMarker("logical")
                                 .addMarker("config").addMarker(relayType).addMarker("on").addMarker("threshold")
                                 .setUnit("%")
                                 .setGroup(equip.getGroup())
                                 .setTz(hayStack.getTimeZone())
                                 .build();
        String relayOnThresholdId = hayStack.addPoint(relayOnThreshold);
        hayStack.writeDefaultValById(relayOnThresholdId, 0.0);
        
        Point relayOffThreshold = new Point.Builder()
                                      .setDisplayName(equip.getDisplayName() + "-"+relayType+"OffThreshold")
                                      .setEquipRef(equip.getId())
                                      .setSiteRef(equip.getSiteRef())
                                      .setRoomRef(equip.getRoomRef())
                                      .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                      .addMarker("sp").addMarker("pid").addMarker("zone").addMarker("his").addMarker("logical")
                                      .addMarker("config").addMarker(relayType).addMarker("off").addMarker("threshold")
                                      .setUnit("%")
                                      .setGroup(equip.getGroup())
                                      .setTz(hayStack.getTimeZone())
                                      .build();
        String relayOffThresholdId = hayStack.addPoint(relayOffThreshold);
        hayStack.writeDefaultValById(relayOffThresholdId, 0.0);
    }
    
}
