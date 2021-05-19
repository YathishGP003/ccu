package a75f.io.logic.bo.building.system.dab;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;

public class DcwbProfileUtil {
    
    public static void createConfigPoints(Equip equip, CCUHsApi hayStack) {
        
        Point analog4OutputEnabled = new Point.Builder()
                                         .setDisplayName(equip.getDisplayName()+"-"+"analog4OutputEnabled")
                                         .setSiteRef(equip.getSiteRef())
                                         .setEquipRef(equip.getId())
                                         .addMarker("system").addMarker("config").addMarker("analog4")
                                         .addMarker("output").addMarker("enabled").addMarker("writable").addMarker("sp")
                                         .setEnums("false,true").setTz(equip.getTz())
                                         .build();
        String analog4OutputEnabledId = hayStack.addPoint(analog4OutputEnabled);
        hayStack.writeDefaultValById(analog4OutputEnabledId, 0.0 );
    
        Point adaptiveDeltaEnabled = new Point.Builder()
                                .setDisplayName(equip.getDisplayName()+"-"+"adaptiveDeltaEnabled")
                                .setSiteRef(equip.getSiteRef())
                                .setEquipRef(equip.getId())
                                .addMarker("system").addMarker("config")
                                .addMarker("adaptive").addMarker("delta").addMarker("enabled").addMarker("writable").addMarker("sp")
                                .setEnums("false,true").setTz(equip.getTz())
                                .build();
        String adaptiveDeltaEnabledId = hayStack.addPoint(adaptiveDeltaEnabled);
        hayStack.writeDefaultValById(adaptiveDeltaEnabledId, 1.0 );
    
        Point maximizedExitWaterTempEnabled = new Point.Builder()
                                         .setDisplayName(equip.getDisplayName()+"-"+"maximizedExitWaterTempEnabled")
                                         .setSiteRef(equip.getSiteRef())
                                         .setEquipRef(equip.getId())
                                         .addMarker("system").addMarker("config")
                                         .addMarker("maximized").addMarker("exit").addMarker("water").addMarker("temp")
                                         .addMarker("enabled").addMarker("writable").addMarker("sp")
                                         .setEnums("false,true").setTz(equip.getTz())
                                         .build();
        String maximizedExitWaterTempEnabledId = hayStack.addPoint(maximizedExitWaterTempEnabled);
        hayStack.writeDefaultValById(maximizedExitWaterTempEnabledId, 0.0 );
    
        Point analog1AtValveClosedPosition = new Point.Builder()
                                             .setDisplayName(equip.getDisplayName()+"-"+"analog1AtValveClosedPosition")
                                             .setSiteRef(equip.getSiteRef())
                                             .setEquipRef(equip.getId())
                                             .addMarker("system").addMarker("config")
                                             .addMarker("analog1").addMarker("valve").addMarker("closed")
                                             .addMarker("position").addMarker("writable").addMarker("sp")
                                             .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTz(equip.getTz())
                                             .build();
        String analog1AtValveClosedPositionId = hayStack.addPoint(analog1AtValveClosedPosition);
        hayStack.writeDefaultValById(analog1AtValveClosedPositionId, 2.0 );
    
        Point analog1AtValveFullPosition = new Point.Builder()
                                                 .setDisplayName(equip.getDisplayName()+"-"+"analog1AtValveFullPosition")
                                                 .setSiteRef(equip.getSiteRef())
                                                 .setEquipRef(equip.getId())
                                                 .addMarker("system").addMarker("config")
                                                 .addMarker("analog1").addMarker("valve").addMarker("full")
                                                 .addMarker("position").addMarker("writable").addMarker("sp")
                                                 .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTz(equip.getTz())
                                                 .build();
        String analog1AtValveFullPositionId = hayStack.addPoint(analog1AtValveFullPosition);
        hayStack.writeDefaultValById(analog1AtValveFullPositionId, 10.0 );
    
        Point analog4LoopOutputType = new Point.Builder()
                                          .setDisplayName(equip.getDisplayName()+"-"+"analog4LoopOutputType")
                                          .setSiteRef(equip.getSiteRef())
                                          .setEquipRef(equip.getId())
                                          .addMarker("system").addMarker("config")
                                          .addMarker("analog4").addMarker("loop").addMarker("output")
                                          .addMarker("type").addMarker("writable").addMarker("sp")
                                          .setMinVal("0").setMaxVal("1").setIncrementVal("0").setTz(equip.getTz())
                                          .build();
        String analog4LoopOutputTypeId = hayStack.addPoint(analog4LoopOutputType);
        hayStack.writeDefaultValById(analog4LoopOutputTypeId, 0.0 );
    }
    
    public static void createAnalog4LoopConfigPoints(String associationType, Equip equip, CCUHsApi hayStack) {
        
        String minLoopName = associationType.contains("cooling") ? "analog4AtMinCoolingLoop" : "analog4AtMinCo2Loop";
        String maxLoopName = associationType.contains("cooling") ? "analog4AtMaxCoolingLoop" : "analog4AtMaxCo2Loop";
    
        Point analog4AtMinCoolingLoop = new Point.Builder()
                                            .setDisplayName(equip.getDisplayName()+"-"+minLoopName)
                                            .setSiteRef(equip.getSiteRef())
                                            .setEquipRef(equip.getId())
                                            .addMarker("system").addMarker("config")
                                            .addMarker("analog4").addMarker("min").addMarker(associationType)
                                            .addMarker("loop").addMarker("writable").addMarker("sp")
                                            .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTz(equip.getTz())
                                            .build();
        String analog4AtMinCoolingLoopId = hayStack.addPoint(analog4AtMinCoolingLoop);
        hayStack.writeDefaultValById(analog4AtMinCoolingLoopId, 2.0 );
    
        Point analog4AtMaxCoolingLoop = new Point.Builder()
                                            .setDisplayName(equip.getDisplayName()+"-"+maxLoopName)
                                            .setSiteRef(equip.getSiteRef())
                                            .setEquipRef(equip.getId())
                                            .addMarker("system").addMarker("config")
                                            .addMarker("analog4").addMarker("max").addMarker(associationType)
                                            .addMarker("loop").addMarker("writable").addMarker("sp")
                                            .setMinVal("0").setMaxVal("10").setIncrementVal("1").setTz(equip.getTz())
                                            .build();
        String analog4AtMaxCoolingLoopId = hayStack.addPoint(analog4AtMaxCoolingLoop);
        hayStack.writeDefaultValById(analog4AtMaxCoolingLoopId, 10.0 );
    }
    
    public static void createChilledWaterConfigPoints( Equip equip, CCUHsApi hayStack) {
        Point chilledWaterTargetDelta = new Point.Builder()
                                            .setDisplayName(equip.getDisplayName()+"-"+"chilledWaterTargetDelta")
                                            .setSiteRef(equip.getSiteRef())
                                            .setEquipRef(equip.getId())
                                            .addMarker("system").addMarker("config")
                                            .addMarker("chilled").addMarker("water").addMarker("target")
                                            .addMarker("delta").addMarker("writable").addMarker("sp")
                                            .setMinVal("0").setMaxVal("30").setIncrementVal("1").setTz(equip.getTz())
                                            .setUnit("\u00B0F")
                                            .build();
        String chilledWaterTargetDeltaId = hayStack.addPoint(chilledWaterTargetDelta);
        hayStack.writeDefaultValById(chilledWaterTargetDeltaId, 15.0 );
    
        Point chilledWaterExitMargin = new Point.Builder()
                                            .setDisplayName(equip.getDisplayName()+"-"+"chilledWaterExitTemperatureMargin")
                                            .setSiteRef(equip.getSiteRef())
                                            .setEquipRef(equip.getId())
                                            .addMarker("system").addMarker("config")
                                            .addMarker("chilled").addMarker("water").addMarker("exit")
                                            .addMarker("temp").addMarker("margin").addMarker("writable").addMarker("sp")
                                            .setMinVal("0").setMaxVal("15").setIncrementVal("1").setTz(equip.getTz())
                                            .setUnit("\u00B0F")
                                            .build();
        String chilledWaterExitMarginId = hayStack.addPoint(chilledWaterExitMargin);
        hayStack.writeDefaultValById(chilledWaterExitMarginId, 4.0 );
    
        Point chilledWaterMaxFlowRate = new Point.Builder()
                                           .setDisplayName(equip.getDisplayName()+"-"+"chilledWaterMaxFlowRate")
                                           .setSiteRef(equip.getSiteRef())
                                           .setEquipRef(equip.getId())
                                           .addMarker("system").addMarker("config")
                                           .addMarker("chilled").addMarker("water").addMarker("max")
                                           .addMarker("flow").addMarker("rate").addMarker("writable").addMarker("sp")
                                           .setMinVal("0").setMaxVal("200").setIncrementVal("10").setTz(equip.getTz())
                                           .setUnit("gpm")
                                           .build();
        String chilledWaterMaxFlowRateId = hayStack.addPoint(chilledWaterMaxFlowRate);
        hayStack.writeDefaultValById(chilledWaterMaxFlowRateId, 100.0 );
        
    }
    
    public static void createLoopPoints( Equip equip, CCUHsApi hayStack) {
        Point systemDCWBValveLoopOutput = new Point.Builder()
                                            .setDisplayName(equip.getDisplayName()+"-"+"systemDCWBValveLoopOutput")
                                            .setSiteRef(equip.getSiteRef())
                                            .setEquipRef(equip.getId())
                                            .addMarker("system").addMarker("dcwb")
                                            .addMarker("valve").addMarker("loop").addMarker("output")
                                            .addMarker("his").addMarker("sp").setHisInterpolate("linear")
                                            .setTz(equip.getTz())
                                            .setUnit("%")
                                            .build();
        String systemDCWBValveLoopOutputId = hayStack.addPoint(systemDCWBValveLoopOutput);
        hayStack.writeHisValById(systemDCWBValveLoopOutputId, 0.0 );
    
        Point chilledWaterTargetExitTemperature  = new Point.Builder()
                                              .setDisplayName(equip.getDisplayName()+"-"+ "chilledWaterExitTemperatureTarget")
                                              .setSiteRef(equip.getSiteRef())
                                              .setEquipRef(equip.getId())
                                              .addMarker("system").addMarker("dcwb")
                                              .addMarker("chilled").addMarker("water").addMarker("exit").addMarker("temp")
                                              .addMarker("target").addMarker("his").addMarker("sp").setHisInterpolate("linear")
                                              .setTz(equip.getTz())
                                              .setUnit("\u00B0F")
                                              .build();
        String chilledWaterTargetExitTemperatureId = hayStack.addPoint(chilledWaterTargetExitTemperature );
        hayStack.writeHisValById(chilledWaterTargetExitTemperatureId, 0.0 );
    }
    
    public static void deleteConfigPoints(CCUHsApi hayStack) {
    
        deleteConfigPoint("analog4 and output and enabled", hayStack);
        deleteConfigPoint("adaptive and delta and enabled", hayStack);
        deleteConfigPoint("maximized and exit and temp and enabled", hayStack);
        deleteConfigPoint("analog1 and valve and closed and position", hayStack);
        deleteConfigPoint("analog1 and valve and full and position", hayStack);
    
        deleteConfigPoint("analog4 and min and loop", hayStack);
        deleteConfigPoint("analog4 and max and loop", hayStack);
    
        deleteConfigPoint("chilled and water and target and delta", hayStack);
        deleteConfigPoint("chilled and water and exit and margin", hayStack);
        deleteConfigPoint("chilled and water and max and flow and rate", hayStack);
        deleteConfigPoint("analog4 and loop and output and type", hayStack);
        
    }
    
    public static void deleteAnalog4LoopConfigPoints(String associationType, CCUHsApi hayStack) {
        String deleteTag = associationType.contains("cooling") ? "cooling" : "co2";
        deleteConfigPoint("analog4 and min and loop and "+deleteTag, hayStack);
        deleteConfigPoint("analog4 and max and loop and "+deleteTag, hayStack);
    }
    
    public static void deleteLoopOutputPoints(CCUHsApi hayStack) {
        deleteLoopPoint("valve and loop and output", hayStack);
        deleteLoopPoint("chilled and water and target and exit and temp", hayStack);
    }
    
    
    private static void deleteConfigPoint(String query, CCUHsApi hayStack) {
        HashMap point = hayStack.read("system and config and "+query);
        if (!point.isEmpty()) {
            hayStack.deleteWritablePoint(point.get("id").toString());
        }
    }
    
    private static void deleteLoopPoint(String query, CCUHsApi hayStack) {
        HashMap point = hayStack.read("system and "+query);
        if (!point.isEmpty()) {
            hayStack.deleteWritablePoint(point.get("id").toString());
        }
    }
    
}
