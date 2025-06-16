package a75f.io.logic.bo.building.sse;

import org.projecthaystack.HDict;

import java.util.HashMap;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BacnetIdKt;
import a75f.io.logic.BacnetUtilKt;
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
        BacnetUtilKt.addBacnetTags(coolingStage, BacnetIdKt.COOLINGSTAGE1ID,BacnetUtilKt.BINARY_VALUE,Integer.parseInt(equip.getGroup()));
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
        BacnetUtilKt.addBacnetTags(heatingStage, BacnetIdKt.HEATINGSTAGE1ID,BacnetUtilKt.BINARY_VALUE,Integer.parseInt(equip.getGroup()));
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
        BacnetUtilKt.addBacnetTags(fanStage1, BacnetIdKt.FANSTAGE1ID,BacnetUtilKt.BINARY_VALUE,Integer.parseInt(equip.getGroup()));
        String fanStage1Id = CCUHsApi.getInstance().addPoint(fanStage1);
        CCUHsApi.getInstance().writeHisValById(fanStage1Id, 0.0);
        return fanStage1Id;
    }
    
    private static double getConfigNumVal(String tags, String nodeAddr) {
        return CCUHsApi.getInstance().readDefaultVal("point and zone and config and sse and "+tags+" and group == \""+nodeAddr+"\"");
    }

    public static Point createAnalogInLogicalPoints(String equipDis, String siteRef, String equipRef, String roomRef, String floorRef, String tz, int nodeAddr, int analogInAssociation) {

        Point point = null;
        return point;
    }

    public static void updateAnalogIn1Config(InputActuatorType analogInAssociation, Point configPoint, boolean analogIn1Enabled) {

        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readMapById(configPoint.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        String nodeAddr = equip.getGroup();
        double curConfig = getConfigNumVal("input and association", nodeAddr);
        double curAnalogInEnabled = getConfigNumVal("analog1 and input and enabled", nodeAddr);

        //if the selected point is same as the existing, return without doing anything
        if(isCurrentSettingsSameAsNewSettings(curConfig, analogIn1Enabled, analogInAssociation, curAnalogInEnabled)) {
            CcuLog.d(L.TAG_CCU_ZONE, "SSE updateAnalogIn1 - No Action required : configVal "+analogInAssociation);
            return;
        }

        deleteExistingPoint(curConfig, configPoint.getEquipRef());
        createNewPoint(Integer.parseInt(nodeAddr), analogIn1Enabled, equip, analogInAssociation);
        mapLogicalPointWithConfigPoint(equip.getId(), analogInAssociation, Integer.parseInt(nodeAddr));
        CCUHsApi.getInstance().scheduleSync();
    }

    private static void mapLogicalPointWithConfigPoint(String equipId, InputActuatorType analogInAssociation, int nodeAddr) {

        CCUHsApi hayStack = CCUHsApi.getInstance();
        HDict analogInPoint;

        if (analogInAssociation == InputActuatorType.ZERO_TO_50A_CURRENT_TRANSFORMER) {
            analogInPoint = hayStack.readHDict("point and transformer50 and sensor and " +
                    "equipRef == \""+equipId+"\"");
        } else if (analogInAssociation == InputActuatorType.ZERO_TO_20A_CURRENT_TRANSFORMER) {
            analogInPoint = hayStack.readHDict("point and transformer20 and sensor and " +
                    "equipRef == \""+equipId+"\"");
        } else {
            analogInPoint = hayStack.readHDict("point and transformer and sensor and " +
                    "equipRef == \""+equipId+"\"");
        }

        if (!analogInPoint.isEmpty()) {
            updateMarker(analogInPoint);
            SmartNode.updatePhysicalPointRef(nodeAddr, Port.ANALOG_IN_ONE.name(), Objects.requireNonNull(analogInPoint.get("id")).toString());
        }

    }

    private static void createNewPoint(int nodeAddr, boolean analogIn1Enabled, Equip equip, InputActuatorType analogInAssociation) {

        SmartNode.setPointEnabled(nodeAddr, Port.ANALOG_IN_ONE.name(), analogIn1Enabled);

        if (analogIn1Enabled) {

            String analogAssociationId = String.valueOf(createAnalogInLogicalPoints(equip.getDisplayName(),
                    equip.getSiteRef(), equip.getId(), equip.getRoomRef(), equip.getFloorRef(), equip.getTz(),
                    nodeAddr, analogInAssociation.ordinal()));

            SmartNode.updatePhysicalPointRef(Integer.parseInt(equip.getGroup()), Port.ANALOG_IN_ONE.name(), analogAssociationId);

            if (analogInAssociation == InputActuatorType.ZERO_TO_50A_CURRENT_TRANSFORMER) {
                SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_IN_ONE.name(), "10");
            } else if (analogInAssociation == InputActuatorType.ZERO_TO_20A_CURRENT_TRANSFORMER) {
                SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_IN_ONE.name(), "9");
            } else {
                SmartNode.updatePhysicalPointType(nodeAddr, Port.ANALOG_IN_ONE.name(), "8");
            }

            CCUHsApi.getInstance().scheduleSync();
        }

    }

    private static boolean isCurrentSettingsSameAsNewSettings(double curConfig, boolean analogIn1Enabled, InputActuatorType analogInAssociation, double curAnalogInEnabled) {

        int configAnalogIn1Enabled = analogIn1Enabled ? 1 :0;

        return analogInAssociation.ordinal() == curConfig && curAnalogInEnabled == configAnalogIn1Enabled;

    }

    private static void deleteExistingPoint(double curConfig, String equipRef) {

        HashMap<Object, Object> configAnalogInPoint = null;

        String[] queryStrings = {
                "point and logical and transformer and sensor and equipRef== \"" + equipRef + "\"",
                "point and logical and transformer20 and sensor and equipRef== \"" + equipRef + "\"",
                "point and logical and transformer50 and sensor and equipRef== \"" + equipRef + "\""
        };

        if (curConfig < queryStrings.length) {
            configAnalogInPoint = CCUHsApi.getInstance().readEntity(queryStrings[(int) curConfig]);
        }

        if (!Objects.requireNonNull(configAnalogInPoint).isEmpty()) {
            CCUHsApi.getInstance().deleteEntity(Objects.requireNonNull(configAnalogInPoint.get("id")).toString());
        }
    }

    private static void updateMarker(HDict point) {

        if (!point.has("standalone") || !point.has("sse")) {
            Point point1 = new Point.Builder().setHDict(point).addMarker("standalone").addMarker("sse").build();
            CCUHsApi.getInstance().updatePoint(point1, Objects.requireNonNull(point.get("id")).toString());
        }

    }
}