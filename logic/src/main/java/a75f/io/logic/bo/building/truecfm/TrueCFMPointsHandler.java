package a75f.io.logic.bo.building.truecfm;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.Units;
import a75f.io.logic.BacnetIdKt;
import a75f.io.logic.BacnetUtilKt;

public class TrueCFMPointsHandler {
    
    public static void createTrueCFMControlPoint(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal, String fanMarker) {
        
        Point enableCFMControl = new Point.Builder()
                                     .setDisplayName(equip.getDisplayName() + "-enableCFMControl")
                                     .setEquipRef(equip.getId())
                                     .setSiteRef(equip.getSiteRef())
                                     .setRoomRef(equip.getRoomRef())
                                     .addMarker("control").setEnums("false,true")
                                     .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov").addMarker(fanMarker)
                                     .addMarker("config").addMarker(profileTag).addMarker("writable").addMarker("zone")
                                     .addMarker("enable").addMarker("trueCfm").addMarker("sp").addMarker("his")
                                     .setGroup(equip.getGroup())
                                     .setTz(equip.getTz())
                                     .build();
        String enableCFMControlId = hayStack.addPoint(enableCFMControl);
        hayStack.writeDefaultValById(enableCFMControlId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(enableCFMControlId, initialVal);
    }
    
    private static void createTrueCFMKFactorPoint(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal, String fanMarker) {
        Point kFactor = new Point.Builder()
                            .setDisplayName(equip.getDisplayName() + "-kFactor")
                            .setEquipRef(equip.getId())
                            .setFloorRef(equip.getFloorRef())
                            .setRoomRef(equip.getRoomRef())
                            .setSiteRef(equip.getSiteRef()).setHisInterpolate("cov")
                            .addMarker("config").addMarker(profileTag).addMarker("trueCfm").addMarker(fanMarker)
                            .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his").addMarker("kfactor")
                            .setMinVal("1.00").setMaxVal("3.00").setIncrementVal("0.01")
                            .setGroup(equip.getGroup())
                            .setTz(equip.getTz())
                            .build();
        String kFactorId = hayStack.addPoint(kFactor);
        hayStack.writeDefaultValById(kFactorId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(kFactorId, initialVal);
    }
    
    private static void createTrueCFMCoolingMin(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal,
                                                String fanType, String maxValueForMinCfmCooling) {
        Point numMinCFMCooling = new Point.Builder()
                                     .setDisplayName(equip.getDisplayName() + "-minCFMCooling")
                                     .setEquipRef(equip.getId())
                                     .setSiteRef(equip.getSiteRef())
                                     .setRoomRef(equip.getRoomRef())
                                     .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                     .addMarker("config").addMarker(profileTag).addMarker("min")
                                     .addMarker("trueCfm").addMarker("cooling").addMarker(fanType)
                                     .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                     .setMinVal("0").setMaxVal(maxValueForMinCfmCooling).setIncrementVal("5")
                                     .setGroup(equip.getGroup())
                                     .setTz(equip.getTz())
                                     .setUnit(Units.CFM)
                                     .build();
        String numMinCFMCoolingId = hayStack.addPoint(numMinCFMCooling);
        hayStack.writeDefaultValById(numMinCFMCoolingId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(numMinCFMCoolingId, initialVal);
    }
    
    
    private static void createTrueCFMCoolingMax(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal,
                                                String fanType) {
        Point numMaxCFMCooling = new Point.Builder()
                                     .setDisplayName(equip.getDisplayName() + "-maxCFMCooling")
                                     .setEquipRef(equip.getId())
                                     .setSiteRef(equip.getSiteRef())
                                     .setRoomRef(equip.getRoomRef())
                                     .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                     .addMarker("config").addMarker(profileTag).addMarker("max")
                                     .addMarker("trueCfm").addMarker("cooling").addMarker(fanType)
                                     .setMinVal("0").setMaxVal("5000").setIncrementVal("5")
                                     .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                     .setGroup(equip.getGroup())
                                     .setTz(equip.getTz())
                                     .setUnit(Units.CFM)
                                     .build();
        String numMaxCFMCoolingId = hayStack.addPoint(numMaxCFMCooling);
        hayStack.writeDefaultValById(numMaxCFMCoolingId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(numMaxCFMCoolingId, initialVal);
    }
    
    private static void createTrueCFMReheatMin(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal,
                                               String fanType, String maxValueForMinCfmHeating) {
        Point numMinCFMReheating = new Point.Builder()
                                       .setDisplayName(equip.getDisplayName() + "-minCFMReheating")
                                       .setEquipRef(equip.getId())
                                       .setSiteRef(equip.getSiteRef())
                                       .setRoomRef(equip.getRoomRef())
                                       .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                       .addMarker("config").addMarker(profileTag).addMarker("min")
                                       .addMarker("trueCfm").addMarker("heating").addMarker(fanType)
                                       .setMinVal("0").setMaxVal(maxValueForMinCfmHeating).setIncrementVal("10")
                                       .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                       .setGroup(equip.getGroup())
                                       .setTz(equip.getTz())
                                       .setUnit(Units.CFM)
                                       .build();
        String numMinCFMReheatingId = hayStack.addPoint(numMinCFMReheating);
        hayStack.writeDefaultValById(numMinCFMReheatingId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(numMinCFMReheatingId, initialVal);
    }
    
    private static void createTrueCFMReheatMax(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal,
                                               String fanType) {
        Point numMaxCFMReheating = new Point.Builder()
                                       .setDisplayName(equip.getDisplayName() + "-maxCFMReheating")
                                       .setEquipRef(equip.getId())
                                       .setSiteRef(equip.getSiteRef())
                                       .setRoomRef(equip.getRoomRef())
                                       .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                       .addMarker("config").addMarker(profileTag).addMarker("max")
                                       .addMarker("trueCfm").addMarker("heating").addMarker(fanType)
                                       .setMinVal("0").setMaxVal("1500").setIncrementVal("10")
                                       .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                       .setGroup(equip.getGroup())
                                       .setTz(equip.getTz())
                                       .setUnit(Units.CFM)
                                       .build();
        String numMaxCFMReheatingId = hayStack.addPoint(numMaxCFMReheating);
        hayStack.writeDefaultValById(numMaxCFMReheatingId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(numMaxCFMReheatingId, initialVal);
    }

    private static void createTrueCFMIaqMin(CCUHsApi hayStack, Equip equip, String profileTag,  double minCFMForIaq) {
        Point minCFMIAQ = new Point.Builder()
                .setDisplayName(equip.getDisplayName() + "-minCFMIAQ")
                .setEquipRef(equip.getId())
                .setSiteRef(equip.getSiteRef())
                .setRoomRef(equip.getRoomRef())
                .setFloorRef(equip.getFloorRef()).setHisInterpolate(Tags.COV)
                .addMarker(Tags.CONFIG).addMarker(profileTag).addMarker(Tags.MIN)
                .addMarker(Tags.CFM).addMarker(Tags.IAQ)
                .setMinVal("0").setMaxVal("1500").setIncrementVal("10")
                .addMarker(Tags.WRITABLE).addMarker(Tags.ZONE).addMarker(Tags.HIS)
                .setGroup(equip.getGroup())
                .setTz(equip.getTz())
                .setUnit(Units.CFM)
                .build();
        String minCFMIAQId = hayStack.addPoint(minCFMIAQ);
        hayStack.writeDefaultValById(minCFMIAQId, minCFMForIaq);
        hayStack.writeHisValueByIdWithoutCOV(minCFMIAQId, minCFMForIaq);
    }
    
    public static void createTrueCfmSpPoints(CCUHsApi hayStack, Equip equip, String profileTag, String fanType) {
        
        Point airflowCfm = new Point.Builder()
                               .setDisplayName(equip.getDisplayName()+"-airflow")
                               .setEquipRef(equip.getId())
                               .setSiteRef(equip.getSiteRef())
                               .setRoomRef(equip.getRoomRef())
                               .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                               .addMarker(profileTag).addMarker("sp").addMarker("trueCfm")
                               .addMarker("air").addMarker("flow").addMarker("his").addMarker(fanType)
                               .setGroup(equip.getGroup())
                               .setTz(equip.getTz())
                               .setUnit(Units.CFM)
                               .build();
        BacnetUtilKt.addBacnetTags(airflowCfm,BacnetIdKt.AIRFLOWID,BacnetUtilKt.ANALOG_VALUE,Integer.parseInt(equip.getGroup()));
        String airflowCfmId = hayStack.addPoint(airflowCfm);

        hayStack.writeHisValueByIdWithoutCOV(airflowCfmId, 0.0);
        
        Point airVelocity = new Point.Builder()
                                 .setDisplayName(equip.getDisplayName()+"-airVelocity")
                                 .setEquipRef(equip.getId())
                                 .setSiteRef(equip.getSiteRef())
                                 .setRoomRef(equip.getRoomRef())
                                 .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                 .addMarker(profileTag).addMarker("air").addMarker("velocity").addMarker("trueCfm")
                                 .addMarker("sp").addMarker("his").addMarker(fanType)
                                 .setGroup(equip.getGroup())
                                 .setTz(equip.getTz())
                                 .setUnit(Units.FT_PER_MIN)
                                 .build();
        BacnetUtilKt.addBacnetTags(airVelocity,BacnetIdKt.AIRVELOCITYID,BacnetUtilKt.ANALOG_VALUE,Integer.parseInt(equip.getGroup()));
        String airVelocityId = hayStack.addPoint(airVelocity);

        hayStack.writeHisValById(airVelocityId, 0.0);
    }

    
    /**
     * Deletes all true cfm related points, including the tuners.
     */
    public static void deleteTrueCFMPoints(CCUHsApi hayStack, String equipRef) {
        List<HashMap<Object, Object >>
            allCFMPoints =  hayStack.readAllEntities("point and trueCfm and not enable and equipRef== \""
                                                     + equipRef + "\"");
        for (HashMap<Object, Object> cfmPoint : allCFMPoints) {
            hayStack.deleteEntity(Objects.requireNonNull(cfmPoint.get("id")).toString());
        }
    }

    public static void deleteNonCfmDamperPoints(CCUHsApi hayStack, String equipRef) {
        HashMap<Object, Object> damperMinCooling = hayStack.readEntity("config and min and damper and pos and " +
                "cooling and equipRef == \""+equipRef+"\"");
        if (!damperMinCooling.isEmpty()) {
            hayStack.deleteWritablePoint(damperMinCooling.get("id").toString());
        }

        HashMap<Object, Object> damperMaxCooling = hayStack.readEntity("config and max and damper and pos and " +
                "cooling and equipRef == \""+equipRef+"\"");
        if (!damperMaxCooling.isEmpty()) {
            hayStack.deleteWritablePoint(damperMaxCooling.get("id").toString());
        }

        HashMap<Object, Object> damperMinHeating = hayStack.readEntity("config and min and damper and pos and " +
                "heating and equipRef == \""+equipRef+"\"");
        if (!damperMinHeating.isEmpty()) {
            hayStack.deleteWritablePoint(damperMinHeating.get("id").toString());
        }
    }
}
