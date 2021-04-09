package a75f.io.logic.bo.building.plc;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.haystack.device.SmartNode;

/**
 * Util class that handles create/update of points for PI equip relays and initialize them.
 */
public class PlcRelayConfigHandler {
    
    /**
     * Create Relay points if they are enabled during the first time pairing.
     * Device instance is directly used here, since it is not added to the database yet.
     * @param equip
     * @param config
     * @param device
     * @param relayType
     * @param hayStack
     */
    public static void createRelayConfigPoints(Equip equip, PlcProfileConfiguration config, SmartNode device,
                                               String relayType, CCUHsApi hayStack) {
    
        
        String relayConfigId = addRelayConfigPoint(equip, relayType, hayStack);
        
        String relayCmdId = addRelayCmdPoint(equip, relayType, hayStack);
        hayStack.writeHisValById(relayCmdId, 0.0);
    
        
        String relayOnThresholdId = addRelayOnThresholdPoint(equip, relayType, hayStack);
        String relayOffThresholdId = addRelayOffThresholdPoint(equip, relayType, hayStack);
    
        if (relayType.contains(Tags.RELAY1)) {
            device.relay1.setPointRef(relayCmdId);
            device.relay1.setEnabled(config.relay1ConfigEnabled);
            device.relay1.setType(OutputRelayActuatorType.NormallyClose.displayName);
            hayStack.writeDefaultValById(relayConfigId, config.relay1ConfigEnabled ? 1.0 : 0);
            hayStack.writeDefaultValById(relayOnThresholdId, config.relay1OnThresholdVal);
            hayStack.writeDefaultValById(relayOffThresholdId, config.relay1OffThresholdVal);
        } else if (relayType.contains(Tags.RELAY2)) {
            device.relay2.setPointRef(relayCmdId);
            device.relay2.setEnabled(config.relay2ConfigEnabled);
            device.relay2.setType(OutputRelayActuatorType.NormallyClose.displayName);
            hayStack.writeDefaultValById(relayConfigId, config.relay2ConfigEnabled ? 1.0 : 0);
            hayStack.writeDefaultValById(relayOnThresholdId, config.relay2OnThresholdVal);
            hayStack.writeDefaultValById(relayOffThresholdId, config.relay2OffThresholdVal);
        }
    }
    
    /**
     * Update the relay config points when relay status is changed during a profile update.
     * Here we use static method to update relay configurations on the device.
     * @param equip
     * @param config
     * @param relayType
     * @param hayStack
     */
    public static void updateRelayConfigPoints(Equip equip, PlcProfileConfiguration config,
                                               String relayType, CCUHsApi hayStack) {
    
        String relayConfigId = addRelayConfigPoint(equip, relayType, hayStack);
    
        String relayCmdId = addRelayCmdPoint(equip, relayType, hayStack);
        hayStack.writeHisValById(relayCmdId, 0.0);
    
    
        String relayOnThresholdId = addRelayOnThresholdPoint(equip, relayType, hayStack);
        String relayOffThresholdId = addRelayOffThresholdPoint(equip, relayType, hayStack);
    
        int nodeAddress = Integer.parseInt(equip.getGroup());
        if (relayType.contains(Tags.RELAY1)) {
            
            SmartNode.updatePhysicalPointRef(nodeAddress, Port.RELAY_ONE.name(), relayCmdId);
            SmartNode.setPointEnabled(nodeAddress, Port.RELAY_ONE.name(),
                                      config.relay1ConfigEnabled);
            SmartNode.updatePhysicalPointType(nodeAddress, Port.RELAY_ONE.name(), OutputRelayActuatorType.NormallyClose.displayName);
            hayStack.writeDefaultValById(relayConfigId, config.relay1ConfigEnabled ? 1.0 : 0);
            hayStack.writeDefaultValById(relayOnThresholdId, config.relay1OnThresholdVal);
            hayStack.writeDefaultValById(relayOffThresholdId, config.relay1OffThresholdVal);
        } else if (relayType.contains(Tags.RELAY2)) {
            SmartNode.updatePhysicalPointRef(nodeAddress, Port.RELAY_TWO.name(), relayCmdId);
            SmartNode.setPointEnabled(nodeAddress, Port.RELAY_TWO.name(), config.relay2ConfigEnabled);
            SmartNode.updatePhysicalPointType(nodeAddress, Port.RELAY_TWO.name(),
                                              OutputRelayActuatorType.NormallyClose.displayName);
            hayStack.writeDefaultValById(relayConfigId, config.relay2ConfigEnabled ? 1.0 : 0);
            hayStack.writeDefaultValById(relayOnThresholdId, config.relay2OnThresholdVal);
            hayStack.writeDefaultValById(relayOffThresholdId, config.relay2OffThresholdVal);
        }
    }
    
    
    private static String addRelayConfigPoint(Equip equip, String relayType, CCUHsApi hayStack) {
        Point relayConfig = new Point.Builder()
                                .setDisplayName(equip.getDisplayName() + "-"+relayType+"ConfigEnabled")
                                .setEquipRef(equip.getId())
                                .setSiteRef(equip.getSiteRef())
                                .setRoomRef(equip.getRoomRef())
                                .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                .addMarker("sp").addMarker("pid").addMarker("zone").addMarker("writable").addMarker("logical")
                                .addMarker("config").addMarker(relayType).addMarker("enabled")
                                .setGroup(equip.getGroup())
                                .setTz(hayStack.getTimeZone())
                                .build();
        return hayStack.addPoint(relayConfig);
    }
    
    private static String addRelayCmdPoint(Equip equip, String relayType, CCUHsApi hayStack) {
        Point relayCmd = new Point.Builder()
                             .setDisplayName(equip.getDisplayName() + "-"+relayType+"Cmd")
                             .setEquipRef(equip.getId())
                             .setSiteRef(equip.getSiteRef())
                             .setRoomRef(equip.getRoomRef())
                             .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                             .addMarker("pid").addMarker("zone").addMarker("his").addMarker("logical")
                             .addMarker("cmd").addMarker(relayType)
                             .setGroup(equip.getGroup())
                             .setTz(hayStack.getTimeZone())
                             .build();
        return hayStack.addPoint(relayCmd);
    }
    
    private static String addRelayOnThresholdPoint(Equip equip, String relayType, CCUHsApi hayStack) {
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
        return hayStack.addPoint(relayOnThreshold);
    }
    
    private static String addRelayOffThresholdPoint(Equip equip, String relayType, CCUHsApi hayStack) {
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
        return hayStack.addPoint(relayOffThreshold);
    }
    
}
