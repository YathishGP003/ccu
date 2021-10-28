package a75f.io.logic.bo.building.sse;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.hvac.SSEStage;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.bo.haystack.device.SmartStat;

public class SingleStageEquipUtil {
    
    /**
     * Create Heating/Cooling point based on Relay1 Selection
     * When it is 0 ( Not Installed) - Both Cooling & Heating points are not required.
     * When it is 1 - Heating point should exist.
     * When it is 2 - Cooling point should exist.
     * @param configVal
     * @param configPoint
     */
    public static void createRelay1Config(int configVal, Point configPoint) {
    
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        
        CcuLog.d(L.TAG_CCU_ZONE, "SSE createRelay1Config : "+configVal);
        SSEStage configStage = SSEStage.values()[configVal];
        
        if (configStage == SSEStage.HEATING) {
            String heatingStageId = createHeatingStagePoint(equip);
            SmartNode.updatePhysicalPointRef(Integer.parseInt(equip.getGroup()), Port.RELAY_ONE.name(), heatingStageId);
        } else if (configStage == SSEStage.COOLING) {
            String coolingStageId = createCoolingStagePoint(equip);
            SmartNode.updatePhysicalPointRef(Integer.parseInt(equip.getGroup()), Port.RELAY_ONE.name(), coolingStageId);
        }
    }
    
    public static void updateRelay1Config(int configVal, Point configPoint) {
    
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        String nodeAddr = equip.getGroup();
        double curConfig = getConfigNumVal("enable and relay1", nodeAddr);
        
        if (configVal == curConfig) {
            CcuLog.d(L.TAG_CCU_ZONE, "SSE updateRelay1Config - No Action required : configVal "+configVal);
            return;
        }
        CcuLog.d(L.TAG_CCU_ZONE, "SSE updateRelay1Config : "+configVal);
        SSEStage configStage = SSEStage.values()[configVal];
        switch (configStage) {
    
            case NOT_INSTALLED:
                HashMap heatPt = CCUHsApi.getInstance().read("point and standalone and heating and stage1 and  " +
                                                             "sse and equipRef== \"" + configPoint.getEquipRef() + "\"");
                if (!heatPt.isEmpty())
                    CCUHsApi.getInstance().deleteEntity(heatPt.get("id").toString());
        
                HashMap coolPt = CCUHsApi.getInstance().read("point and standalone and cooling and stage1 and  " +
                                                             "sse and equipRef== \"" + configPoint.getEquipRef() + "\"");
                if (!coolPt.isEmpty())
                    CCUHsApi.getInstance().deleteEntity(coolPt.get("id").toString());
                break;
            case HEATING:
                HashMap coolingPt = CCUHsApi.getInstance().read("point and standalone and cooling and stage1 and " +
                                                                " sse and equipRef== \"" + configPoint.getEquipRef() + "\"");
                if (!coolingPt.isEmpty())
                    CCUHsApi.getInstance().deleteEntity(coolingPt.get("id").toString());
        
                String heatingStageId = createHeatingStagePoint(equip);
                SmartNode.updatePhysicalPointRef(Integer.parseInt(equip.getGroup()), Port.RELAY_ONE.name(), heatingStageId);
                break;
            case COOLING:
            
                HashMap heatingPt = CCUHsApi.getInstance().read("point and standalone and heating and stage1 and " +
                                                                " sse and equipRef== \"" + configPoint.getEquipRef() + "\"");
                if (!heatingPt.isEmpty())
                    CCUHsApi.getInstance().deleteEntity(heatingPt.get("id").toString());
            
                String coolingStageId = createCoolingStagePoint(equip);
                SmartNode.updatePhysicalPointRef(Integer.parseInt(equip.getGroup()), Port.RELAY_ONE.name(), coolingStageId);
                break;
                
        }
        SmartNode.setPointEnabled(Integer.valueOf(nodeAddr), Port.RELAY_ONE.name(), configVal > 0 ? true : false );
        CCUHsApi.getInstance().scheduleSync();
    }
    
    /**
     * Create Fan point if Relay 2 is enabled.
     * @param configVal
     * @param configPoint
     */
    public static void createRelay2Config(int configVal, Point configPoint) {
    
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        
        CcuLog.d(L.TAG_CCU_ZONE, "SSE createRelay2Config : " + configVal);
        
        if (configVal > 0) {
            String fanStageId = createFanStagePoint(equip);
            SmartNode.updatePhysicalPointRef(Integer.parseInt(equip.getGroup()), Port.RELAY_TWO.name(), fanStageId);
        }
        CCUHsApi.getInstance().scheduleSync();
    }
    
    public static void updateRelay2Config(int configVal, Point configPoint) {
        
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        String nodeAddr = equip.getGroup();
        double curConfig = getConfigNumVal("enable and relay2", nodeAddr);
        if (configVal == curConfig) {
            CcuLog.d(L.TAG_CCU_ZONE, "SSE updateRelay2Config - No Action required : configVal "+configVal);
            return;
        }
        CcuLog.d(L.TAG_CCU_ZONE, "SSE updateRelay2Config : " + configVal);
        HashMap fanPt = CCUHsApi.getInstance().read("point and standalone and fan and stage1 and " +
                                                    " sse and equipRef== \"" + configPoint.getEquipRef() + "\"");
        if (configVal > 0) {
            if (fanPt.isEmpty()) {
                String fanStageId = createFanStagePoint(equip);
                SmartNode.updatePhysicalPointRef(Integer.parseInt(equip.getGroup()), Port.RELAY_TWO.name(), fanStageId);
            }
        } else {
            if (!fanPt.isEmpty())
                CCUHsApi.getInstance().deleteEntity(fanPt.get("id").toString());
        }
        SmartNode.setPointEnabled(Integer.valueOf(nodeAddr), Port.RELAY_TWO.name(), configVal > 0 ? true : false );
        CCUHsApi.getInstance().scheduleSync();
    }
    
    public static void updateThermistorConfig(int configVal, Point configPoint) {
    
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        String nodeAddr = equip.getGroup();
        if (configPoint.getMarkers().contains(Tags.TH1)) {
            SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.TH1_IN.name(),
                                      configVal > 0 ? true : false);
        } else if (configPoint.getMarkers().contains(Tags.TH2)) {
            SmartStat.setPointEnabled(Integer.parseInt(nodeAddr), Port.TH2_IN.name(),
                                      configVal > 0 ? true : false);
        }
    }
    
    public static String createCoolingStagePoint(Equip equip) {
    
        CcuLog.d(L.TAG_CCU_ZONE, "SSE createCoolingStagePoint");
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
        String coolingStageId = CCUHsApi.getInstance().addPoint(coolingStage);
        CCUHsApi.getInstance().writeHisValById(coolingStageId, 0.0);
        return coolingStageId;
    }
    
    public static String createHeatingStagePoint(Equip equip) {
        CcuLog.d(L.TAG_CCU_ZONE, "SSE createHeatingStagePoint");
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
        String heatingStageId = CCUHsApi.getInstance().addPoint(heatingStage);
        CCUHsApi.getInstance().writeHisValById(heatingStageId, 0.0);
        return heatingStageId;
    }
    
    public static String createFanStagePoint(Equip equip) {
        CcuLog.d(L.TAG_CCU_ZONE, "SSE createFanStagePoint");
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
        String fanStage1Id = CCUHsApi.getInstance().addPoint(fanStage1);
        CCUHsApi.getInstance().writeHisValById(fanStage1Id, 0.0);
        return fanStage1Id;
    }
    
    private static double getConfigNumVal(String tags, String nodeAddr) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and sse and "+tags+" and group == \""+nodeAddr+"\"");
    }
}
