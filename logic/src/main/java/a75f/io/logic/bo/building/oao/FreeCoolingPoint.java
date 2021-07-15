package a75f.io.logic.bo.building.oao;

import a75f.io.api.haystack.Point;

public class FreeCoolingPoint {
    private Point freeCooling;

    private FreeCoolingPoint(String pointName, String siteDis, String siteRef, String roomRef, String equipRef,
                            int nodeAddr, String floorRef, String tz, String coolingCause){
        freeCooling = new Point.Builder()
                .setDisplayName(siteDis+"-OAO-"+nodeAddr+"-"+pointName)
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .setHisInterpolate("cov")
                .addMarker("oao").addMarker(coolingCause)
                .addMarker("available").addMarker("his").addMarker("sp")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
    }

    public static Point getFreeCoolingPoint(String pointName, String siteDis, String siteRef, String roomRef,
                                            String equipRef, int nodeAddr, String floorRef, String tz,
                                            String coolingCause){
        return new FreeCoolingPoint(pointName, siteDis, siteRef, roomRef, equipRef, nodeAddr, floorRef,
                tz, coolingCause).freeCooling;
    }
}
