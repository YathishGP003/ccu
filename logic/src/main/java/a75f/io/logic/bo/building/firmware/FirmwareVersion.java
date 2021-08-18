package a75f.io.logic.bo.building.firmware;

import a75f.io.api.haystack.RawPoint;

public class FirmwareVersion {
    private RawPoint firmWareVersion;

    private FirmwareVersion(int smartNodeAddress, String deviceRef, String siteRef, String floorRef, String roomRef,
                            String tz){
        firmWareVersion = new RawPoint.Builder()
                .setDisplayName("firmwareVersion-" + smartNodeAddress)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .setRoomRef(roomRef)
                .setFloorRef(floorRef)
                .addMarker("writable")
                .addMarker("firmware")
                .addMarker("version")
                .setTz(tz)
                .build();

    }

    private FirmwareVersion(String displayName, String deviceRef, String siteRef,  String tz){
        firmWareVersion = new RawPoint.Builder()
                .setDisplayName(displayName)
                .setDeviceRef(deviceRef)
                .setSiteRef(siteRef)
                .addMarker("writable")
                .addMarker("firmware")
                .addMarker("version")
                .setTz(tz)
                .build();

    }

    public static RawPoint getFirmwareVersion(int smartNodeAddress, String deviceRef ,String siteRef, String floorRef
            , String roomRef, String tz){
        return new FirmwareVersion(smartNodeAddress, deviceRef , siteRef, floorRef, roomRef, tz).firmWareVersion;
    }

    public static RawPoint getFirmwareVersion(String displayName, String deviceRef ,String siteRef, String tz){
        return new FirmwareVersion(displayName, deviceRef , siteRef, tz).firmWareVersion;
    }
}
