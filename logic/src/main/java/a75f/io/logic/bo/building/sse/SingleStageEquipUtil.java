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
            CcuLog.d(L.TAG_CCU_ZONE, "SSE updateRelay1Config - No Action required.");
            return;
        }
        CcuLog.d(L.TAG_CCU_ZONE, "SSE updateRelay1Config : "+configVal);
        SSEStage configStage = SSEStage.values()[configVal];
        switch (configStage) {
            case COOLING:
            
                HashMap heatingPt = CCUHsApi.getInstance().read("point and standalone and heating and stage1 and " +
                                                                " sse and equipRef== \"" + configPoint.getEquipRef() + "\"");
                if (!heatingPt.isEmpty())
                    CCUHsApi.getInstance().deleteEntity(heatingPt.get("id").toString());
            
                createCoolingStagePoint(equip );
                CcuLog.d(L.TAG_CCU_ZONE, "SSE Config Update - createCoolingStagePoint");
                break;
            case HEATING:
                HashMap coolingPt = CCUHsApi.getInstance().read("point and standalone and cooling and stage1 and " +
                                                                " sse and equipRef== \"" + configPoint.getEquipRef() + "\"");
                if (!coolingPt.isEmpty())
                    CCUHsApi.getInstance().deleteEntity(coolingPt.get("id").toString());
            
                createHeatingStagePoint(equip );
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
    
    public static void updateRelay2Config(int configVal, Point configPoint) {
        
        HashMap equipMap = CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        String nodeAddr = equip.getGroup();
        double curConfig = getConfigNumVal("enable and relay2", nodeAddr);
        if (configVal == curConfig) {
            CcuLog.d(L.TAG_CCU_ZONE, "SSE updateRelay2Config - No Action required.");
            return;
        }
        CcuLog.d(L.TAG_CCU_ZONE, "SSE updateRelay2Config : " + configVal);
        HashMap fanPt = CCUHsApi.getInstance().read("point and standalone and fan and stage1 and " +
                                                    " sse and equipRef== \"" + configPoint.getEquipRef() + "\"");
        if (configVal > 0) {
            if (fanPt.isEmpty()) {
                createFanStagePoint(equip);
            }
        } else {
            if (!fanPt.isEmpty())
                CCUHsApi.getInstance().deleteEntity(fanPt.get("id").toString());
        }
        CCUHsApi.getInstance().syncPointEntityTree();
    }
    
    private static void createCoolingStagePoint(Equip equip) {
        
        Point coolingStage = new Point.Builder()
                                 .setDisplayName(equip.getDisplayName() + "-coolingStage1")
                                 .setEquipRef(equip.getId())
                                 .setSiteRef(equip.getSiteRef())
                                 .setRoomRef(equip.getRoomRef())
                                 .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                 .addMarker("standalone").addMarker("cooling").addMarker("stage1").addMarker("his").addMarker("zone")
                                 .addMarker("logical").addMarker("sse").addMarker("cmd")
                                 .setEnums("off,on")
                                 .setGroup(equip.getGroup())
                                 .setTz(CCUHsApi.getInstance().getTimeZone())
                                 .build();
        String r1coolID = CCUHsApi.getInstance().addPoint(coolingStage);
        CCUHsApi.getInstance().writeHisValById(r1coolID, 0.0);
        SmartNode.updatePhysicalPointRef(Integer.parseInt(equip.getGroup()), Port.RELAY_ONE.name(), r1coolID);
    }
    
    private static void createHeatingStagePoint(Equip equip) {
        Point heatingStage = new Point.Builder()
                                 .setDisplayName(equip.getDisplayName() + "-heatingStage1")
                                 .setEquipRef(equip.getId())
                                 .setSiteRef(equip.getSiteRef())
                                 .setRoomRef(equip.getRoomRef())
                                 .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                 .addMarker("standalone").addMarker("heating").addMarker("stage1").addMarker("his").addMarker("zone")
                                 .addMarker("logical").addMarker("sse").addMarker("cmd")
                                 .setEnums("off,on")
                                 .setGroup(String.valueOf(equip.getGroup()))
                                 .setTz(CCUHsApi.getInstance().getTimeZone())
                                 .build();
        String r1heatID = CCUHsApi.getInstance().addPoint(heatingStage);
        CCUHsApi.getInstance().writeHisValById(r1heatID, 0.0);
        SmartNode.updatePhysicalPointRef(Integer.parseInt(equip.getGroup()),Port.RELAY_ONE.name(),r1heatID);
    }
    
    private static void createFanStagePoint(Equip equip) {
        Point fanStage1 = new Point.Builder()
                              .setDisplayName(equip.getDisplayName()+"-fanStage1")
                              .setEquipRef(equip.getId())
                              .setSiteRef(equip.getSiteRef())
                              .setRoomRef(equip.getRoomRef())
                              .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                              .addMarker("standalone").addMarker("fan").addMarker("stage1").addMarker("his").addMarker("zone")
                              .addMarker("logical").addMarker("sse").addMarker("cmd")
                              .setEnums("off,on")
                              .setGroup(String.valueOf(equip.getGroup()))
                              .setTz(CCUHsApi.getInstance().getTimeZone())
                              .build();
        String r2ID = CCUHsApi.getInstance().addPoint(fanStage1);
        CCUHsApi.getInstance().writeHisValById(r2ID, 0.0);
        SmartNode.updatePhysicalPointRef(Integer.parseInt(equip.getGroup()),Port.RELAY_TWO.name(),r2ID);
    }
    
    public static double getConfigNumVal(String tags, String nodeAddr) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and sse and "+tags+" and group == \""+nodeAddr+"\"");
    }
}
