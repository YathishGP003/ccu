package a75f.io.logic.bo.building.truecfm;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.vav.VavProfileConfiguration;

public class TrueCFMPointsHandler {
    
    public static void createTrueCFMControlPoint(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal) {
        
        Point enableCFMControl = new Point.Builder()
                                     .setDisplayName(equip.getDisplayName() + "-enableCFMControl")
                                     .setEquipRef(equip.getId())
                                     .setSiteRef(equip.getSiteRef())
                                     .setRoomRef(equip.getRoomRef())
                                     .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                     .addMarker("config").addMarker(profileTag).addMarker("writable").addMarker("zone")
                                     .addMarker("enabled").addMarker("cfm").addMarker("sp").addMarker("his")
                                     .setGroup(equip.getGroup())
                                     .setTz(equip.getTz())
                                     .build();
        String enableCFMControlId = hayStack.addPoint(enableCFMControl);
        hayStack.writeDefaultValById(enableCFMControlId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(enableCFMControlId, initialVal);
    }
    
    private static void createTrueCFMKFactorPoint(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal) {
        Point kFactor = new Point.Builder()
                            .setDisplayName(equip.getDisplayName() + "-kFactor")
                            .setEquipRef(equip.getId())
                            .setFloorRef(equip.getFloorRef())
                            .setRoomRef(equip.getRoomRef())
                            .setSiteRef(equip.getSiteRef()).setHisInterpolate("cov")
                            .addMarker("config").addMarker(profileTag).addMarker("cfm").addMarker("pos")
                            .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his").addMarker("kfactor")
                            .setGroup(equip.getGroup())
                            .setTz(equip.getTz())
                            .build();
        String kFactorId = hayStack.addPoint(kFactor);
        hayStack.writeDefaultValById(kFactorId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(kFactorId, initialVal);
    }
    
    private static void createTrueCFMCoolingMin(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal,
                                                String fanType) {
    
        Point numMinCFMCooling = new Point.Builder()
                                     .setDisplayName(equip.getDisplayName() + "-minCFMCooling")
                                     .setEquipRef(equip.getId())
                                     .setSiteRef(equip.getSiteRef())
                                     .setRoomRef(equip.getRoomRef())
                                     .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                     .addMarker("config").addMarker(profileTag).addMarker("min")
                                     .addMarker("cfm").addMarker("cooling").addMarker(fanType)
                                     .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                     .setGroup(equip.getGroup())
                                     .setTz(equip.getTz())
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
                                     .addMarker("cfm").addMarker("cooling").addMarker(fanType)
                                     .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                     .setGroup(equip.getGroup())
                                     .setTz(equip.getTz())
                                     .build();
        String numMaxCFMCoolingId = hayStack.addPoint(numMaxCFMCooling);
        hayStack.writeDefaultValById(numMaxCFMCoolingId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(numMaxCFMCoolingId, initialVal);
    }
    
    private static void createTrueCFMReheatMin(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal,
                                               String fanType) {
    
        Point numMinCFMReheating = new Point.Builder()
                                       .setDisplayName(equip.getDisplayName() + "-minCFMReheating")
                                       .setEquipRef(equip.getId())
                                       .setSiteRef(equip.getSiteRef())
                                       .setRoomRef(equip.getRoomRef())
                                       .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                       .addMarker("config").addMarker(profileTag).addMarker("min")
                                       .addMarker("cfm").addMarker("heating").addMarker(fanType)
                                       .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                       .setGroup(equip.getGroup())
                                       .setTz(equip.getTz())
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
                                       .addMarker("cfm").addMarker("heating").addMarker(fanType)
                                       .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                       .setGroup(equip.getGroup())
                                       .setTz(equip.getTz())
                                       .build();
        String numMaxCFMReheatingId = hayStack.addPoint(numMaxCFMReheating);
        hayStack.writeDefaultValById(numMaxCFMReheatingId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(numMaxCFMReheatingId, initialVal);
    }
    
    public static void createTrueCfmSpPoints(CCUHsApi hayStack, Equip equip, String profileTag, String fanType) {
        
        Point airflowCfm = new Point.Builder()
                               .setDisplayName(equip.getDisplayName()+"-airflowCfm")
                               .setEquipRef(equip.getId())
                               .setSiteRef(equip.getSiteRef())
                               .setRoomRef(equip.getRoomRef())
                               .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                               .addMarker(profileTag).addMarker("sp").addMarker("cfm")
                               .addMarker("airflow").addMarker("his").addMarker(fanType)
                               .setGroup(equip.getGroup())
                               .setTz(equip.getTz())
                               .build();
        String airflowCfmId = hayStack.addPoint(airflowCfm);
        hayStack.writeHisValueByIdWithoutCOV(airflowCfmId, 0.0);
        
        Point flowVelocity = new Point.Builder()
                                 .setDisplayName(equip.getDisplayName()+"-flowVelocity")
                                 .setEquipRef(equip.getId())
                                 .setSiteRef(equip.getSiteRef())
                                 .setRoomRef(equip.getRoomRef())
                                 .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                 .addMarker(profileTag).addMarker("flow").addMarker("velocity").addMarker("cfm")
                                 .addMarker("sp").addMarker("his").addMarker(fanType)
                                 .setGroup(equip.getGroup())
                                 .setTz(equip.getTz())
                                 .build();
        String flowVelocityId = hayStack.addPoint(flowVelocity);
        hayStack.writeHisValById(flowVelocityId, 0.0);
    }
    
    public static void createTrueCFMVavPoints(CCUHsApi hayStack, String equipRef,
                                                    VavProfileConfiguration vavProfileConfiguration, String fanType) {
        HashMap<Object, Object> equipMap = hayStack.readMapById(equipRef);
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        
        createTrueCFMKFactorPoint(hayStack, equip, Tags.VAV, vavProfileConfiguration.kFactor);
    
        createTrueCFMCoolingMin(hayStack, equip, Tags.VAV, vavProfileConfiguration.numMinCFMCooling, fanType);
    
        createTrueCFMCoolingMax(hayStack, equip, Tags.VAV, vavProfileConfiguration.nuMaxCFMCooling, fanType);
    
        createTrueCFMReheatMin(hayStack, equip, Tags.VAV, vavProfileConfiguration.numMinCFMReheating, fanType);
    
        createTrueCFMReheatMax(hayStack, equip, Tags.VAV, vavProfileConfiguration.numMaxCFMReheating, fanType);
    
        createTrueCfmSpPoints(hayStack, equip, Tags.VAV, fanType);
    }
    
    /**
     * Deletes all true cfm related points, including the tuners.
     */
    public static void deleteTrueCFMPoints(CCUHsApi hayStack, String equipRef) {
        List<HashMap<Object, Object >>
            allCFMPoints =  hayStack.readAllEntities("point and cfm and not enabled and equipRef== \""
                                                     + equipRef + "\"");
        for (HashMap<Object, Object> cfmPoint : allCFMPoints) {
            hayStack.deleteEntity(Objects.requireNonNull(cfmPoint.get("id")).toString());
        }
    }
}
