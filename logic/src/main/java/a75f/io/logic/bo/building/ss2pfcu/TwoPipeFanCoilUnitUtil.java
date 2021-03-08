package a75f.io.logic.bo.building.ss2pfcu;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.haystack.device.SmartStat;

public class TwoPipeFanCoilUnitUtil {
    
    public static void updateRelayConfig(int configVal, Point configPoint, CCUHsApi hayStack) {
        
        HashMap equipMap = hayStack.readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        String nodeAddr = equip.getGroup();
        try {
            CcuLog.i(L.TAG_CCU_PUBNUB,
                     "updateRelayConfig "+nodeAddr+" "+configPoint);
            if (configPoint.getMarkers().contains(Tags.RELAY1)) {
                SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.RELAY_ONE.name(),
                                          configVal > 0 ? true : false);
            } else if (configPoint.getMarkers().contains(Tags.RELAY2)) {
                SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.RELAY_TWO.name(),
                                          configVal > 0 ? true : false);
            } else if (configPoint.getMarkers().contains(Tags.RELAY3)) {
                SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.RELAY_THREE.name(),
                                          configVal > 0 ? true : false);
            } else if (configPoint.getMarkers().contains(Tags.RELAY4)) {
                SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.RELAY_FOUR.name(),
                                          configVal > 0 ? true : false);
            } else if (configPoint.getMarkers().contains(Tags.RELAY5)) {
                SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.RELAY_FIVE.name(),
                                          configVal > 0 ? true : false);
            } else if (configPoint.getMarkers().contains(Tags.RELAY6)) {
                SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.RELAY_SIX.name(),
                                          configVal > 0 ? true : false);
            } else if (configPoint.getMarkers().contains(Tags.ENABLE)
                        && configPoint.getMarkers().contains(Tags.OCCUPANCY)
                       && configPoint.getMarkers().contains(Tags.CONTROL)) {
                updateOccupancyPoint(configVal, equip, configPoint);
            }
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to update : "+configPoint.getDisplayName()+" ; "+e.getMessage());
        }
        hayStack.syncPointEntityTree();
    }
    
    private static void updateOccupancyPoint(double configVal, Equip equip, Point configPoint) {
        
        HashMap occDetPoint = CCUHsApi.getInstance().read("point and occupancy and detection and fcu and his and " +
                                                          "equipRef== \"" + equip.getId() + "\"");
    
        CcuLog.i(L.TAG_CCU_PUBNUB, "updateOccupancyPoint "+configVal+" "+occDetPoint);
    
        if(configVal > 0){
            //Create occupancy detection point if it does not exisit.
            if (occDetPoint.isEmpty()) {
            String profileTag = configPoint.getMarkers().contains(Tags.PIPE2) ? Tags.PIPE2 : Tags.PIPE4;
            Point occupancyDetection = new Point.Builder()
                                           .setDisplayName(equip.getDisplayName()+"-occupancyDetection")
                                           .setEquipRef(equip.getId())
                                           .setSiteRef(equip.getSiteRef())
                                           .setRoomRef(equip.getRoomRef())
                                           .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                           .addMarker("occupancy").addMarker("detection").addMarker("fcu")
                                           .addMarker(profileTag).addMarker("his").addMarker("zone")
                                           .setGroup(equip.getGroup())
                                           .setEnums("false,true")
                                           .setTz(CCUHsApi.getInstance().getTimeZone())
                                           .build();
            String occupancyDetectionId = CCUHsApi.getInstance().addPoint(occupancyDetection);
            CCUHsApi.getInstance().writeHisValById(occupancyDetectionId, 0.0);
            
        } else {
            if (!occDetPoint.isEmpty())
                CCUHsApi.getInstance().deleteEntityTree(occDetPoint.get("id").toString());
        }
    }
    }
}
