package a75f.io.logic.bo.haystack.device;

import java.util.HashMap;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Tags;

public class HelioNode extends SmartNode{

    public HelioNode(int address, String site, String floor, String room, String equipRef) {
            Device d = new Device.Builder()
                    .setDisplayName("HN-"+address)
                    .addMarker("network").addMarker("node").addMarker(Tags.HELIO_NODE).addMarker("his")
                    .setAddr(address)
                    .setSiteRef(site)
                    .setFloorRef(floor)
                    .setEquipRef(equipRef)
                    .setRoomRef(room)
                    .build();
            deviceRef = CCUHsApi.getInstance().addDevice(d);
            smartNodeAddress = address;
            siteRef = site;
            floorRef = floor;
            roomRef = room;

            HashMap<Object, Object> siteMap = CCUHsApi.getInstance().readEntity(Tags.SITE);
            tz = Objects.requireNonNull(siteMap.get("tz")).toString();

            createPoints();

    }
}
