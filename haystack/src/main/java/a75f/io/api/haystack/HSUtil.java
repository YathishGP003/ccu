package a75f.io.api.haystack;

import android.util.Log;

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
    public static Zone getZone(String roomRef, String floorRef) {

        ArrayList<HashMap> zones = CCUHsApi.getInstance().readAll("room and floorRef == \""+floorRef+"\"");
        for (HashMap m : zones)
        {
            if((m.get("id")).toString().equals(roomRef))
                return new Zone.Builder().setHashMap(m).build();
        }
        return null;
    }
    public static ArrayList<Equip> getEquips(String roomRef) {
        
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and roomRef == \""+roomRef+"\"");
        ArrayList<Equip> equipList = new ArrayList<>();
        for (HashMap m : equips)
        {
            //Log.d("CCU_UI", "Equip in Zone " + roomRef + " : " + m);
            equipList.add(new Equip.Builder().setHashMap(m).build());
        }
        return equipList;
    }
    
    public static ArrayList<Device> getDevices(String roomRef) {
        
        ArrayList<HashMap> devices = CCUHsApi.getInstance().readAll("device and roomRef == \""+roomRef+"\"");
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
    
    public static Equip getEquipFromZone(String roomRef) {
        HashMap equip = CCUHsApi.getInstance().read("equip and roomRef == \""+roomRef+"\"");
        return new Equip.Builder().setHashMap(equip).build();
    }
    
    public static String getZoneIdFromEquipId(String equipId) {
        HashMap equipHashMap = CCUHsApi.getInstance().readMapById(equipId);
        Equip equip = new Equip.Builder().setHashMap(equipHashMap).build();
        return equip.getRoomRef();
    }
    public static Equip getEquipInfo(String equipId) {
        HashMap equipHashMap = CCUHsApi.getInstance().readMapById(equipId);
        Equip equip = new Equip.Builder().setHashMap(equipHashMap).build();
        return equip;
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
    
    public static void printPointArr(String id) {
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size(); l++)
            {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("val") != null)
                {
                    Log.d("CCU_HS", " Override updated point " + id + " , level: " + l + " , val :" + Double.parseDouble(valMap.get("val").toString())
                                    +" duration: "+Double.parseDouble(valMap.get("duration").toString()));
                }
            }
        }
    }
    
    public static void printPointArr(Point p) {
        ArrayList values = CCUHsApi.getInstance().readPoint(p.getId());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size(); l++)
            {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("val") != null)
                {
                    Log.d("CCU_HS", "Updated point " + p.getDisplayName() + " , level: " + l + " , val :" + Double.parseDouble(valMap.get("val").toString())
                                                        +" duration: "+Double.parseDouble(valMap.get("duration").toString()));
                }
            }
        }
    }
    
    public static double getPriorityVal(String id) {
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size() ; l++ ) {
                HashMap valMap = ((HashMap) values.get(l-1));
                if (valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }
    
    public static double getPriorityLevelVal(String id, int level) {
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            HashMap valMap = ((HashMap) values.get(level-1));
            if (valMap.get("val") != null) {
                return Double.parseDouble(valMap.get("val").toString());
            }
        }
        return 0;
    }
    
    public static HashMap getPriorityLevel(String id, int level) {
        ArrayList values = CCUHsApi.getInstance().readPoint(id);
        if (values != null && values.size() > 0)
        {
            HashMap valMap = ((HashMap) values.get(level-1));
            return valMap;
        }
        return null;
    }
}
