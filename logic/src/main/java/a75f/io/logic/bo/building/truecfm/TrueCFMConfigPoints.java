package a75f.io.logic.bo.building.truecfm;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.vav.VavProfileConfiguration;

public class TrueCFMConfigPoints {
    
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
    
    private static void createTrueCFMCoolingMin(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal) {
    
        Point numMinCFMCooling = new Point.Builder()
                                     .setDisplayName(equip.getDisplayName() + "-minCFMCooling")
                                     .setEquipRef(equip.getId())
                                     .setSiteRef(equip.getSiteRef())
                                     .setRoomRef(equip.getRoomRef())
                                     .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                     .addMarker("config").addMarker(profileTag).addMarker("min")
                                     .addMarker("cfm").addMarker("cooling")
                                     .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                     .setGroup(equip.getGroup())
                                     .setTz(equip.getTz())
                                     .build();
        String numMinCFMCoolingId = hayStack.addPoint(numMinCFMCooling);
        hayStack.writeDefaultValById(numMinCFMCoolingId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(numMinCFMCoolingId, initialVal);
    }
    
    
    private static void createTrueCFMCoolingMax(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal) {
    
        Point numMaxCFMCooling = new Point.Builder()
                                     .setDisplayName(equip.getDisplayName() + "-maxCFMCooling")
                                     .setEquipRef(equip.getId())
                                     .setSiteRef(equip.getSiteRef())
                                     .setRoomRef(equip.getRoomRef())
                                     .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                     .addMarker("config").addMarker(profileTag).addMarker("max")
                                     .addMarker("cfm").addMarker("cooling")
                                     .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                     .setGroup(equip.getGroup())
                                     .setTz(equip.getTz())
                                     .build();
        String numMaxCFMCoolingId = hayStack.addPoint(numMaxCFMCooling);
        hayStack.writeDefaultValById(numMaxCFMCoolingId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(numMaxCFMCoolingId, initialVal);
    }
    
    private static void createTrueCFMReheatMin(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal) {
    
        Point numMinCFMReheating = new Point.Builder()
                                       .setDisplayName(equip.getDisplayName() + "-minCFMReheating")
                                       .setEquipRef(equip.getId())
                                       .setSiteRef(equip.getSiteRef())
                                       .setRoomRef(equip.getRoomRef())
                                       .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                       .addMarker("config").addMarker(profileTag).addMarker("min")
                                       .addMarker("cfm").addMarker("heating")
                                       .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                       .setGroup(equip.getGroup())
                                       .setTz(equip.getTz())
                                       .build();
        String numMinCFMReheatingId = hayStack.addPoint(numMinCFMReheating);
        hayStack.writeDefaultValById(numMinCFMReheatingId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(numMinCFMReheatingId, initialVal);
    }
    
    private static void createTrueCFMReheatMax(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal) {
        Point numMaxCFMReheating = new Point.Builder()
                                       .setDisplayName(equip.getDisplayName() + "-maxCFMReheating")
                                       .setEquipRef(equip.getId())
                                       .setSiteRef(equip.getSiteRef())
                                       .setRoomRef(equip.getRoomRef())
                                       .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                       .addMarker("config").addMarker(profileTag).addMarker("max")
                                       .addMarker("cfm").addMarker("heating")
                                       .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                       .setGroup(equip.getGroup())
                                       .setTz(equip.getTz())
                                       .build();
        String numMaxCFMReheatingId = hayStack.addPoint(numMaxCFMReheating);
        hayStack.writeDefaultValById(numMaxCFMReheatingId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(numMaxCFMReheatingId, initialVal);
    }
    
    
    public static void createTrueCFMVavConfigPoints(CCUHsApi hayStack, String equipRef,
                                                    VavProfileConfiguration vavProfileConfiguration) {
        HashMap<Object, Object> equipMap = hayStack.readMapById(equipRef);
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        
        createTrueCFMKFactorPoint(hayStack, equip, Tags.VAV, vavProfileConfiguration.kFactor );
    
        createTrueCFMCoolingMin(hayStack, equip, Tags.VAV, vavProfileConfiguration.numMinCFMCooling );
    
        createTrueCFMCoolingMax(hayStack, equip, Tags.VAV, vavProfileConfiguration.nuMaxCFMCooling );
    
        createTrueCFMReheatMin(hayStack, equip, Tags.VAV, vavProfileConfiguration.numMinCFMReheating );
    
        createTrueCFMReheatMax(hayStack, equip, Tags.VAV, vavProfileConfiguration.numMaxCFMReheating );
    }
    
    /**
     * Deletes all true cfm related points, including the tuners.
     */
    public static void deleteTrueCFMPoints(CCUHsApi hayStack, String equipRef) {
        List<HashMap<Object, Object >>
            allCFMPoints =  hayStack.readAllEntities("point and cfm and equipRef== \"" + equipRef + "\"");
        for (HashMap<Object, Object> cfmPoint : allCFMPoints) {
            hayStack.deleteEntity(Objects.requireNonNull(cfmPoint.get("id")).toString());
        }
    }
}
