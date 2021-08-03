package a75f.io.logic.bo.building.oao;

import a75f.io.api.haystack.Point;

public class OAODamperOpenPoint {
    private Point damperOpenPoint;

    private OAODamperOpenPoint(String pointName, String siteDis, String siteRef, String roomRef, String equipRef,
                               int nodeAddr, String floorRef, String tz, String damperOpenReason){
        damperOpenPoint = new Point.Builder()
                .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-"+pointName)
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("oao").addMarker(damperOpenReason)
                .addMarker("available").addMarker("his").addMarker("sp")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
    }

    public static Point getDamperOpenPoint(String pointName, String siteDis, String siteRef, String roomRef,
                                           String equipRef, int nodeAddr, String floorRef, String tz,
                                           String damperOpenReason){
        return new OAODamperOpenPoint(pointName, siteDis, siteRef, roomRef, equipRef, nodeAddr, floorRef,
                tz, damperOpenReason).damperOpenPoint;
    }
}
