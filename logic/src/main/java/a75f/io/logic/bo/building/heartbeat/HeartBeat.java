package a75f.io.logic.bo.building.heartbeat;

import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.logic.bo.building.definitions.Port;

public class HeartBeat {
    private Point heartBeat;
    private RawPoint rssi;

    private HeartBeat(String equipDis, String equipRef, String siteRef, String room, String floor, int nodeAddr,
                      String profile, String tz, String module){
        heartBeat = new Point.Builder()
                .setDisplayName(equipDis+"-heartBeat")
                .setEquipRef(equipRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor).setHisInterpolate("cov")
                .addMarker("heartbeat").addMarker(module)
                .addMarker("zone").addMarker("standalone")
                .addMarker(profile).addMarker("sensor")
                .addMarker("current").addMarker("his")
                .addMarker("cur").addMarker("logical")
                .setGroup(String.valueOf(nodeAddr))
                .setTz(tz)
                .build();
    }

    private HeartBeat(int nodeAddr,String deviceRef, String siteRef, String room, String floor, String tz) {
        rssi = new RawPoint.Builder()
                .setDisplayName("rssi-" + nodeAddr)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setRoomRef(room)
                .setFloorRef(floor)
                .addMarker("rssi")
                .addMarker("sensor").addMarker("his")
                .setPort(Port.RSSI.toString())
                .setTz(tz)
                .build();
    }

    public static Point getHeartBeatPoint(String equipDis, String equipRef, String siteRef, String room, String floor,
                                          int nodeAddr, String profile, String tz, String module){
        return new HeartBeat(equipDis, equipRef, siteRef, room, floor, nodeAddr, profile, tz, module).heartBeat;
    }

    public static RawPoint getHeartBeatRawPoint(int nodeAddr,String deviceRef, String siteRef, String room, String floor, String tz){
        return new HeartBeat(nodeAddr, deviceRef, siteRef, room, floor, tz).rssi;
    }

}