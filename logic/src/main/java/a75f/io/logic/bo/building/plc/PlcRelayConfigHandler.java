package a75f.io.logic.bo.building.plc;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
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
                                    .addMarker("sp").addMarker("pid").addMarker("zone").addMarker("writable").addMarker("logical")
                                    .addMarker("config").addMarker(relayType).addMarker("enabled")
                                    .setUnit("%")
                                    .setGroup(equip.getGroup())
                                    .setTz(hayStack.getTimeZone())
                                    .build();
        String relayConfigId = hayStack.addPoint(relayConfig);
    
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
    
        Point relayOnThreshold = new Point.Builder()
                                 .setDisplayName(equip.getDisplayName() + "-"+relayType+"OnThreshold")
                                 .setEquipRef(equip.getId())
                                 .setSiteRef(equip.getSiteRef())
                                 .setRoomRef(equip.getRoomRef())
                                 .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                 .addMarker("sp").addMarker("pid").addMarker("zone").addMarker("writable").addMarker("logical")
                                 .addMarker("config").addMarker(relayType).addMarker("on").addMarker("threshold")
                                 .setUnit("%")
                                 .setGroup(equip.getGroup())
                                 .setTz(hayStack.getTimeZone())
                                 .build();
        String relayOnThresholdId = hayStack.addPoint(relayOnThreshold);
        
        Point relayOffThreshold = new Point.Builder()
                                      .setDisplayName(equip.getDisplayName() + "-"+relayType+"OffThreshold")
                                      .setEquipRef(equip.getId())
                                      .setSiteRef(equip.getSiteRef())
                                      .setRoomRef(equip.getRoomRef())
                                      .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                      .addMarker("sp").addMarker("pid").addMarker("zone").addMarker("writable").addMarker("logical")
                                      .addMarker("config").addMarker(relayType).addMarker("off").addMarker("threshold")
                                      .setUnit("%")
                                      .setGroup(equip.getGroup())
                                      .setTz(hayStack.getTimeZone())
                                      .build();
        String relayOffThresholdId = hayStack.addPoint(relayOffThreshold);
    
        if (relayType.contains(Tags.RELAY1)) {
            device.relay1.setPointRef(relayCmdId);
            device.relay1.setEnabled(config.relay1ConfigEnabled);
            hayStack.writeDefaultValById(relayConfigId, config.relay1ConfigEnabled ? 1.0 : 0);
            hayStack.writeDefaultValById(relayOnThresholdId, config.relay1OnThresholdVal);
            hayStack.writeDefaultValById(relayOffThresholdId, config.relay1OffThresholdVal);
        } else if (relayType.contains(Tags.RELAY2)) {
            device.relay2.setPointRef(relayCmdId);
            device.relay2.setEnabled(config.relay2ConfigEnabled);
            hayStack.writeDefaultValById(relayConfigId, config.relay2ConfigEnabled ? 1.0 : 0);
            hayStack.writeDefaultValById(relayOnThresholdId, config.relay2OnThresholdVal);
            hayStack.writeDefaultValById(relayOffThresholdId, config.relay2OffThresholdVal);
        }
    }
    
}
