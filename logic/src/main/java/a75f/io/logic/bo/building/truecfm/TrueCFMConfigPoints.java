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
    
    public static void createTrueCFMControlPoint(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal, String fanMarker) {
        
        Point enableCFMControl = new Point.Builder()
                                     .setDisplayName(equip.getDisplayName() + "-enableCFMControl")
                                     .setEquipRef(equip.getId())
                                     .setSiteRef(equip.getSiteRef())
                                     .setRoomRef(equip.getRoomRef())
                                     .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov").addMarker(fanMarker)
                                     .addMarker("config").addMarker(profileTag).addMarker("writable").addMarker("zone")
                                     .addMarker("enabled").addMarker("cfm").addMarker("sp").addMarker("his")
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
                            .setRoomRef(equip.getRoomRef()).addMarker(fanMarker)
                            .setSiteRef(equip.getSiteRef()).setHisInterpolate("cov")
                            .addMarker("config").addMarker(profileTag).addMarker("cfm").addMarker("pos").addMarker(fanMarker)
                            .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his").addMarker("kfactor")
                            .setGroup(equip.getGroup())
                            .setTz(equip.getTz())
                            .build();
        String kFactorId = hayStack.addPoint(kFactor);
        hayStack.writeDefaultValById(kFactorId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(kFactorId, initialVal);
    }
    
    private static void createTrueCFMCoolingMin(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal, String fanMarker) {
    
        Point numMinCFMCooling = new Point.Builder()
                                     .setDisplayName(equip.getDisplayName() + "-minCFMCooling")
                                     .setEquipRef(equip.getId())
                                     .setSiteRef(equip.getSiteRef())
                                     .setRoomRef(equip.getRoomRef())
                                     .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                     .addMarker("config").addMarker(profileTag).addMarker("min")
                                     .addMarker("cfm").addMarker("cooling").addMarker(fanMarker)
                                     .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                     .setGroup(equip.getGroup())
                                     .setTz(equip.getTz())
                                     .build();
        String numMinCFMCoolingId = hayStack.addPoint(numMinCFMCooling);
        hayStack.writeDefaultValById(numMinCFMCoolingId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(numMinCFMCoolingId, initialVal);
    }
    
    
    private static void createTrueCFMCoolingMax(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal, String fanMarker) {
    
        Point numMaxCFMCooling = new Point.Builder()
                                     .setDisplayName(equip.getDisplayName() + "-maxCFMCooling")
                                     .setEquipRef(equip.getId())
                                     .setSiteRef(equip.getSiteRef())
                                     .setRoomRef(equip.getRoomRef())
                                     .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                     .addMarker("config").addMarker(profileTag).addMarker("max")
                                     .addMarker("cfm").addMarker("cooling").addMarker(fanMarker)
                                     .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                     .setGroup(equip.getGroup())
                                     .setTz(equip.getTz())
                                     .build();
        String numMaxCFMCoolingId = hayStack.addPoint(numMaxCFMCooling);
        hayStack.writeDefaultValById(numMaxCFMCoolingId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(numMaxCFMCoolingId, initialVal);
    }
    
    private static void createTrueCFMReheatMin(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal, String fanMarker) {
    
        Point numMinCFMReheating = new Point.Builder()
                                       .setDisplayName(equip.getDisplayName() + "-minCFMReheating")
                                       .setEquipRef(equip.getId())
                                       .setSiteRef(equip.getSiteRef())
                                       .setRoomRef(equip.getRoomRef())
                                       .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                       .addMarker("config").addMarker(profileTag).addMarker("min")
                                       .addMarker("cfm").addMarker("heating").addMarker(fanMarker)
                                       .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                       .setGroup(equip.getGroup())
                                       .setTz(equip.getTz())
                                       .build();
        String numMinCFMReheatingId = hayStack.addPoint(numMinCFMReheating);
        hayStack.writeDefaultValById(numMinCFMReheatingId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(numMinCFMReheatingId, initialVal);
    }
    
    private static void createTrueCFMReheatMax(CCUHsApi hayStack, Equip equip, String profileTag, double initialVal, String fanMarker) {
        Point numMaxCFMReheating = new Point.Builder()
                                       .setDisplayName(equip.getDisplayName() + "-maxCFMReheating")
                                       .setEquipRef(equip.getId())
                                       .setSiteRef(equip.getSiteRef())
                                       .setRoomRef(equip.getRoomRef())
                                       .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                                       .addMarker("config").addMarker(profileTag).addMarker("max")
                                       .addMarker("cfm").addMarker("heating").addMarker(fanMarker)
                                       .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                                       .setGroup(equip.getGroup())
                                       .setTz(equip.getTz())
                                       .build();
        String numMaxCFMReheatingId = hayStack.addPoint(numMaxCFMReheating);
        hayStack.writeDefaultValById(numMaxCFMReheatingId, initialVal);
        hayStack.writeHisValueByIdWithoutCOV(numMaxCFMReheatingId, initialVal);
    }

    private static void createAirflowCfmPoint(CCUHsApi hayStack, Equip equip, String profileTag, String fanMarker) {
        Point airflowCfm = new Point.Builder()
                .setDisplayName(equip.getDisplayName() +"-airflowCfm")
                .setEquipRef(equip.getId())
                .setSiteRef(equip.getSiteRef())
                .setRoomRef(equip.getRoomRef())
                .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                .addMarker("vav").addMarker("cmd").addMarker("cfm").addMarker(fanMarker)
                .addMarker("airflow").addMarker("his").addMarker(profileTag)
                .setGroup(equip.getGroup())
                .build();
        hayStack.addPoint(airflowCfm);
    }

    private static void createFlowVelocityPoint(CCUHsApi hayStack, Equip equip, String profileTag, String fanMarker) {

        Point flowVelocity = new Point.Builder()
                .setDisplayName(equip.getDisplayName() +"-flowVelocity")
                .setEquipRef(equip.getId())
                .setSiteRef(equip.getSiteRef())
                .setRoomRef(equip.getRoomRef())
                .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                .addMarker("vav").addMarker("flow").addMarker("velocity").addMarker(fanMarker)
                .addMarker("sp").addMarker("his").addMarker(profileTag)
                .setGroup(equip.getGroup())
                .build();
         hayStack.addPoint(flowVelocity);

    }

    private static void createPressurePoint(CCUHsApi hayStack, Equip equip, String profileTag, String fanMarker) {

        Point pressure = new Point.Builder()
                .setDisplayName(equip.getDisplayName() +"-pressure")
                .setEquipRef(equip.getId())
                .setSiteRef(equip.getSiteRef())
                .setRoomRef(equip.getRoomRef())
                .setFloorRef(equip.getFloorRef()).setHisInterpolate("cov")
                .addMarker("pressure").addMarker("his").addMarker("sensor").addMarker("vav")
                .addMarker(fanMarker).addMarker(profileTag)
                .setGroup(equip.getGroup())
                .build();
         hayStack.addPoint(pressure);

    }
    
    public static void createTrueCFMVavConfigPoints(CCUHsApi hayStack, Equip equip,
                                                    VavProfileConfiguration vavProfileConfiguration, String fanMarker) {
        createTrueCFMKFactorPoint(hayStack, equip, Tags.VAV, vavProfileConfiguration.kFactor, fanMarker);
    
        createTrueCFMCoolingMin(hayStack, equip, Tags.VAV, vavProfileConfiguration.numMinCFMCooling, fanMarker );
    
        createTrueCFMCoolingMax(hayStack, equip, Tags.VAV, vavProfileConfiguration.nuMaxCFMCooling, fanMarker );
    
        createTrueCFMReheatMin(hayStack, equip, Tags.VAV, vavProfileConfiguration.numMinCFMReheating, fanMarker );
    
        createTrueCFMReheatMax(hayStack, equip, Tags.VAV, vavProfileConfiguration.numMaxCFMReheating, fanMarker );

        createAirflowCfmPoint(hayStack, equip, Tags.VAV, fanMarker );

        createFlowVelocityPoint(hayStack, equip, Tags.VAV, fanMarker );

        createPressurePoint(hayStack, equip, Tags.VAV, fanMarker );
    }
    
    /**
     * Deletes all true cfm related points, including the tuners.
     */
    public static void deleteTrueCFMPoints(CCUHsApi hayStack, String equipRef) {
        List<HashMap<Object, Object >>
                allCFMPoints =  hayStack.readAllEntities("point and cfm and not enabled and equipRef== \"" + equipRef + "\"");
        allCFMPoints.add(hayStack.readEntity("point and vav and flow and velocity and equipRef== \"" + equipRef + "\""));
        allCFMPoints.add(hayStack.readEntity("point and vav and pressure and sensor and equipRef== \"" + equipRef + "\""));
        for (HashMap<Object, Object> cfmPoint : allCFMPoints) {
            hayStack.deleteEntity(Objects.requireNonNull(cfmPoint.get("id")).toString());
        }
    }
}
