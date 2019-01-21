package a75f.io.api.haystack;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HVal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by samjithsadasivan on 10/12/18.
 */

public class HSUtil
{
    public static ArrayList<Floor> getFloors() {
        ArrayList<HashMap> floors = CCUHsApi.getInstance().readAll("floor");
        ArrayList<Floor> floorList = new ArrayList<>();
        
        for (HashMap m : floors ) {
            floorList.add(new Floor.Builder().setHashMap(m).build());
        }
        return floorList;
    }
    
    public static ArrayList<Zone> getZones(String floorRef) {
        
        ArrayList<HashMap> zones = CCUHsApi.getInstance().readAll("room and floorRef == \""+floorRef+"\"");
        ArrayList<Zone> zoneList = new ArrayList<>();
        for (HashMap m : zones)
        {
            zoneList.add(new Zone.Builder().setHashMap(m).build());
        }
        return zoneList;
    }
    
    public static ArrayList<Equip> getEquips(String zoneRef) {
        
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and zoneRef == \""+zoneRef+"\"");
        ArrayList<Equip> equipList = new ArrayList<>();
        for (HashMap m : equips)
        {
            equipList.add(new Equip.Builder().setHashMap(m).build());
        }
        return equipList;
    }
    
    public static ArrayList<Device> getDevices(String zoneRef) {
        
        ArrayList<HashMap> devices = CCUHsApi.getInstance().readAll("device and zoneRef == \""+zoneRef+"\"");
        ArrayList<Device> deviceList = new ArrayList<>();
        for (HashMap m : devices)
        {
            deviceList.add(new Device.Builder().setHashMap(m).build());
        }
        return deviceList;
    }
    
    public static String getDis(String id) {
        HashMap item = CCUHsApi.getInstance().readMapById(id);
        return item.get("dis").toString() ;
    }
    
    public static Device getDevice(short addr) {
        HashMap device = CCUHsApi.getInstance().read("device and addr == \""+addr+"\"");
        return new Device.Builder().setHashMap(device).build();
    }
    
    public static Equip getEquipFromZone(String zoneRef) {
        HashMap equip = CCUHsApi.getInstance().read("equip and zoneRef == \""+zoneRef+"\"");
        return new Equip.Builder().setHashMap(equip).build();
    }
    
    public static HDict mapToHDict(Map<String, Object> m)
    {
        HDictBuilder b = new HDictBuilder();
        for (Map.Entry<String, Object> entry : m.entrySet())
        {
            if (entry.getValue() instanceof HVal)
            {
                b.add(entry.getKey(), (HVal) entry.getValue());
            }
            else
            {
                b.add(entry.getKey(), (String) entry.getValue());
            }
        }
        return b.toDict();
    }
}
