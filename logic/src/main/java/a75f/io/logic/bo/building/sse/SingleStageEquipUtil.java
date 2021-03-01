package a75f.io.logic.bo.building.sse;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.hvac.SSEStage;
import a75f.io.logic.bo.haystack.device.SmartNode;

public class SingleStageEquipUtil {
    
    public static void updateRelay1Config(int configVal, Point configPoint) {
    
        HashMap equipMap = CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        String nodeAddr = equip.getGroup();
        double curConfig = getConfigNumVal("enable and relay1", nodeAddr);
        
        if (configVal == curConfig) {
            CcuLog.d(L.TAG_CCU_ZONE, "SSE Config Update - No Action required.");
            return;
        }
        SSEStage configStage = SSEStage.values()[(int)curConfig];
        switch (configStage) {
            case COOLING:
            
                HashMap heatingPt = CCUHsApi.getInstance().read("point and standalone and heating and stage1 and " +
                                                                " sse and equipRef== \"" + configPoint.getEquipRef() + "\"");
                if (!heatingPt.isEmpty())
                    CCUHsApi.getInstance().deleteEntity(heatingPt.get("id").toString());
            
                createCoolingStagePoint(configPoint, equip, nodeAddr);
                CcuLog.d(L.TAG_CCU_ZONE, "SSE Config Update - createCoolingStagePoint");
                break;
            case HEATING:
                HashMap coolingPt = CCUHsApi.getInstance().read("point and standalone and cooling and stage1 and " +
                                                                " sse and equipRef== \"" + configPoint.getEquipRef() + "\"");
                if (!coolingPt.isEmpty())
                    CCUHsApi.getInstance().deleteEntity(coolingPt.get("id").toString());
            
                createHeatingStagePoint(configPoint, equip, nodeAddr);
                CcuLog.d(L.TAG_CCU_ZONE, "SSE Config Update - createHeatingStagePoint");
                break;
            case NOT_INSTALLED:
                HashMap heatPt = CCUHsApi.getInstance().read("point and standalone and heating and stage1 and  " +
                                                             "sse and equipRef== \"" + configPoint.getEquipRef() + "\"");
                if (!heatPt.isEmpty())
                    CCUHsApi.getInstance().deleteEntity(heatPt.get("id").toString());
            
                HashMap coolPt = CCUHsApi.getInstance().read("point and standalone and cooling and stage1 and  " +
                                                             "sse and equipRef== \"" + configPoint.getEquipRef() + "\"");
                if (!coolPt.isEmpty())
                    CCUHsApi.getInstance().deleteEntity(coolPt.get("id").toString());
                CcuLog.d(L.TAG_CCU_ZONE, "SSE Config Update - Not Installed");
                break;
        
        }
        CCUHsApi.getInstance().syncPointEntityTree();
    }
    private static void createCoolingStagePoint(Point configPoint, Equip equip, String nodeAddr) {
        
        Point coolingStage = new Point.Builder()
                                 .setDisplayName(equip.getDisplayName() + "-coolingStage1")
                                 .setEquipRef(configPoint.getEquipRef())
                                 .setSiteRef(configPoint.getSiteRef())
                                 .setRoomRef(equip.getRoomRef())
                                 .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                 .addMarker("standalone").addMarker("cooling").addMarker("stage1").addMarker("his").addMarker("zone")
                                 .addMarker("logical").addMarker("sse").addMarker("cmd")
                                 .setEnums("off,on")
                                 .setGroup(nodeAddr)
                                 .setTz(configPoint.getTz())
                                 .build();
        String r1coolID = CCUHsApi.getInstance().addPoint(coolingStage);
        CCUHsApi.getInstance().writeHisValById(r1coolID, 0.0);
        SmartNode.updatePhysicalPointRef(Integer.parseInt(nodeAddr), Port.RELAY_ONE.name(), r1coolID);
    }
    
    private static void createHeatingStagePoint(Point configPoint, Equip equip, String nodeAddr) {
        Point heatingStage = new Point.Builder()
                                 .setDisplayName(equip.getDisplayName() + "-heatingStage1")
                                 .setEquipRef(equip.getId())
                                 .setSiteRef(equip.getSiteRef())
                                 .setRoomRef(equip.getRoomRef())
                                 .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                 .addMarker("standalone").addMarker("heating").addMarker("stage1").addMarker("his").addMarker("zone")
                                 .addMarker("logical").addMarker("sse").addMarker("cmd")
                                 .setEnums("off,on")
                                 .setGroup(String.valueOf(nodeAddr))
                                 .setTz(configPoint.getTz())
                                 .build();
        String r1heatID = CCUHsApi.getInstance().addPoint(heatingStage);
        CCUHsApi.getInstance().writeHisValById(r1heatID, 0.0);
        SmartNode.updatePhysicalPointRef(Integer.parseInt(nodeAddr),Port.RELAY_ONE.name(),r1heatID);
    }
    
    public static double getConfigNumVal(String tags, String nodeAddr) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and sse and "+tags+" and group == \""+nodeAddr+"\"");
    }
}
