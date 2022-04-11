package a75f.io.logic.bo.building;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.dab.DabProfileConfiguration;
import a75f.io.logic.bo.building.vav.VavProfileConfiguration;

public class TrueCFMConfigPoints {
    public static void createTrueCFMConfigPoints(int node, VavProfileConfiguration vavProfileConfiguration, DabProfileConfiguration dabProfileConfiguration, String equipRef, String floor, String room, String profileName, String tag) {
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        String siteDis = (String) siteMap.get("dis");
        String equipDis = siteDis + "-"+profileName+"-" + node;
        String tz = siteMap.get("tz").toString();

        Point enableCFMControl = new Point.Builder()
                .setDisplayName(equipDis + "-enableCFMControl")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("config").addMarker(tag).addMarker("writable").addMarker("zone")
                .addMarker("enabled").addMarker("cfm").addMarker("sp").addMarker("his")
                .setGroup(String.valueOf(node))
                .setTz(tz)
                .build();
        String enableCFMControlId = CCUHsApi.getInstance().addPoint(enableCFMControl);
        if(vavProfileConfiguration!=null){
        CCUHsApi.getInstance().writeDefaultValById(enableCFMControlId, vavProfileConfiguration.enableCFMControl ? 1.0 : 0);
        CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(enableCFMControlId, vavProfileConfiguration.enableCFMControl ? 1.0 : 0);
        }

        Point kFactor = new Point.Builder()
                .setDisplayName(equipDis + "-kFactor")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef).setHisInterpolate("cov")
                .addMarker("config").addMarker(tag).addMarker("cfm").addMarker("pos")
                .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his").addMarker("kfactor")
                .setGroup(String.valueOf(node))
                .setTz(tz)
                .build();
        String kFactorId = CCUHsApi.getInstance().addPoint(kFactor);
        if(vavProfileConfiguration!=null) {
            CCUHsApi.getInstance().writeDefaultValById(kFactorId, vavProfileConfiguration.kFactor);
            CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(kFactorId, vavProfileConfiguration.kFactor);
        }


        Point numMinCFMCooling = new Point.Builder()
                .setDisplayName(equipDis + "-minCFMCooling")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("config").addMarker(tag).addMarker("min")
                .addMarker("cfm").addMarker("cooling")
                .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                .setGroup(String.valueOf(node))
                .setTz(tz)
                .build();
        String numMinCFMCoolingId = CCUHsApi.getInstance().addPoint(numMinCFMCooling);
        if(vavProfileConfiguration!=null) {
            CCUHsApi.getInstance().writeDefaultValById(numMinCFMCoolingId, (double) vavProfileConfiguration.numMinCFMCooling);
            CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(numMinCFMCoolingId, (double) vavProfileConfiguration.numMinCFMCooling);
        }

        Point numMaxCFMCooling = new Point.Builder()
                .setDisplayName(equipDis + "-maxCFMCooling")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("config").addMarker(tag).addMarker("max")
                .addMarker("cfm").addMarker("cooling")
                .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                .setGroup(String.valueOf(node))
                .setTz(tz)
                .build();
        String numMaxCFMCoolingId = CCUHsApi.getInstance().addPoint(numMaxCFMCooling);
        if(vavProfileConfiguration!=null) {
            CCUHsApi.getInstance().writeDefaultValById(numMaxCFMCoolingId, (double) vavProfileConfiguration.nuMaxCFMCooling);
            CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(numMaxCFMCoolingId, (double) vavProfileConfiguration.nuMaxCFMCooling);
        }

        Point numMinCFMReheating = new Point.Builder()
                .setDisplayName(equipDis + "-minCFMReheating")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("config").addMarker(tag).addMarker("min")
                .addMarker("cfm").addMarker("heating")
                .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                .setGroup(String.valueOf(node))
                .setTz(tz)
                .build();
        String numMinCFMReheatingId = CCUHsApi.getInstance().addPoint(numMinCFMReheating);
        if(vavProfileConfiguration!=null) {
            CCUHsApi.getInstance().writeDefaultValById(numMinCFMReheatingId, (double) vavProfileConfiguration.numMinCFMReheating);
            CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(numMinCFMReheatingId, (double) vavProfileConfiguration.numMinCFMReheating);
        }


        Point numMaxCFMReheating = new Point.Builder()
                .setDisplayName(equipDis + "-maxCFMReheating")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("config").addMarker(tag).addMarker("max")
                .addMarker("cfm").addMarker("heating")
                .addMarker("sp").addMarker("writable").addMarker("zone").addMarker("his")
                .setGroup(String.valueOf(node))
                .setTz(tz)
                .build();
        String numMaxCFMReheatingId = CCUHsApi.getInstance().addPoint(numMaxCFMReheating);
        if(vavProfileConfiguration!=null) {
            CCUHsApi.getInstance().writeDefaultValById(numMaxCFMReheatingId, (double) vavProfileConfiguration.numMaxCFMReheating);
            CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(numMaxCFMReheatingId, (double) vavProfileConfiguration.numMaxCFMReheating);
        }
    }
}
