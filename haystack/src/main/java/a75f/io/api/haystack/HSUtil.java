package a75f.io.api.haystack;

import android.media.audiofx.DynamicsProcessing;
import android.util.Log;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HVal;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import a75f.io.logger.CcuLog;

/**
 * Created by samjithsadasivan on 10/12/18.
 */

public class HSUtil
{
    public static final String QUERY_JOINER = " and ";

    public static ArrayList<Floor> getFloors() {
        ArrayList<HashMap<Object, Object>> floors = CCUHsApi.getInstance().readAllEntities("floor");
        ArrayList<Floor> floorList = new ArrayList<>();
        
        for (HashMap<Object, Object> m : floors ) {
            floorList.add(new Floor.Builder().setHashMap(m).build());
        }
        return floorList;
    }
    
    public static ArrayList<Zone> getZones(String floorRef) {
        
        ArrayList<HashMap<Object, Object>> zones =
                        CCUHsApi.getInstance().readAllEntities("room and floorRef == \""+floorRef+"\"");
        ArrayList<Zone> zoneList = new ArrayList<>();
        for (HashMap<Object, Object> m : zones)
        {
            zoneList.add(new Zone.Builder().setHashMap(m).build());
        }
        return zoneList;
    }
    public static Zone getZone(String roomRef, String floorRef) {

        ArrayList<HashMap<Object, Object>> zones =
                            CCUHsApi.getInstance().readAllEntities("room and floorRef == \""+floorRef+"\"");
        for (HashMap<Object, Object> m : zones)
        {
            CcuLog.i("CCU_DEVICE"," Zone: "+m);
            if((m.get("id")).toString().equals(roomRef))
                return new Zone.Builder().setHashMap(m).build();
        }
        return null;
    }
    public static ArrayList<Equip> getEquips(String roomRef) {
        
        ArrayList<HashMap<Object, Object>> equips =
                        CCUHsApi.getInstance().readAllEntities("equip and roomRef == \""+roomRef+"\"");
        ArrayList<Equip> equipList = new ArrayList<>();
        for (HashMap<Object, Object> m : equips)
        {
            equipList.add(new Equip.Builder().setHashMap(m).build());
        }
        return equipList;
    }
    
    public static ArrayList<Device> getDevices(String roomRef) {
        
        ArrayList<HashMap<Object, Object>> devices = CCUHsApi.getInstance().readAllEntities("device and roomRef == \""+roomRef+"\"");
        ArrayList<Device> deviceList = new ArrayList<>();
        for (HashMap<Object, Object> m : devices)
        {
            deviceList.add(new Device.Builder().setHashMap(m).build());
        }
        return deviceList;
    }
    
    public static String getDis(String id) {
        HashMap<Object, Object> item = CCUHsApi.getInstance().readMapById(id);
        return item.get("dis") == null ? "" : item.get("dis").toString() ;
    }
    
    public static Device getDevice(short addr) {
        HashMap<Object, Object> device = CCUHsApi.getInstance().readEntity("device and addr == \""+addr+"\"");
        return new Device.Builder().setHashMap(device).build();
    }
    
    public static Equip getEquipFromZone(String roomRef) {
        HashMap<Object, Object> equip = CCUHsApi.getInstance().readEntity("equip and roomRef == \""+roomRef+"\"");
        return new Equip.Builder().setHashMap(equip).build();
    }
    
    public static String getZoneIdFromEquipId(String equipId) {
        HashMap<Object, Object> equipHashMap = CCUHsApi.getInstance().readMapById(equipId);
        Equip equip = new Equip.Builder().setHashMap(equipHashMap).build();
        return equip.getRoomRef();
    }
    public static Equip getEquipInfo(String equipId) {
        HashMap<Object, Object> equipHashMap = CCUHsApi.getInstance().readMapById(equipId);
        return new Equip.Builder().setHashMap(equipHashMap).build();
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
        if (values != null && values.size() > 0) {
            return ((HashMap) values.get(level-1));
        }
        return null;
    }

    public static String getQueryFromMarkers(ArrayList<String> markers) {
        StringBuilder builder = new StringBuilder();
        for(String marker : markers) {
            builder.append(marker);
            builder.append(QUERY_JOINER);
        }
        String queryString = builder.toString();
        if (queryString.endsWith(QUERY_JOINER)) {
            int index = queryString.lastIndexOf(QUERY_JOINER);
            return queryString.substring(0, index);
        }
        return queryString;
    }

    public static String appendMarkerToQuery(String query, String marker) {
        return query+QUERY_JOINER+marker;
    }

    public static boolean isBuildingTuner(String entityId, CCUHsApi hayStack) {
        HashMap<Object, Object> entityMap = hayStack.readMapById(entityId);
        if (!entityMap.containsKey(Tags.TUNER)) {
            return false;
        }

        HashMap<Object, Object> tunerEquip = hayStack.readEntity("tuner and equip");
        return tunerEquip.get(Tags.ID).equals(entityMap.get(Tags.EQUIPREF));
    }


    //To update after merging Tuner branch.
    public static boolean isSystemConfigOutputPoint(String id, CCUHsApi hayStack) {
        HashMap<Object, Object> pointEntity = hayStack.readMapById(id);
        return pointEntity.containsKey("system")
               && pointEntity.containsKey("config")
               && pointEntity.containsKey("output");
    }

    public static boolean isSystemConfigHumidifierType(String id, CCUHsApi hayStack) {
        HashMap<Object, Object> pointEntity = hayStack.readMapById(id);
        return pointEntity.containsKey("system")
               && pointEntity.containsKey("config")
               && pointEntity.containsKey("humidifier")
               && pointEntity.containsKey("type");
    }

    public static boolean isSystemConfigIE(String id, CCUHsApi hayStack) {
        HashMap<Object, Object> pointEntity = hayStack.readMapById(id);
        return pointEntity.containsKey("system")
               && pointEntity.containsKey("config")
               && pointEntity.containsKey("ie");
    }
    
    public static boolean isSSEConfig(String id, CCUHsApi hayStack) {
        HashMap<Object, Object> pointEntity = hayStack.readMapById(id);
        return pointEntity.containsKey(Tags.SSE)
               && pointEntity.containsKey(Tags.CONFIG);
    }
    
    public static boolean isDcwbConfig(String id, CCUHsApi hayStack) {
        HashMap<Object, Object> pointEntity = hayStack.readMapById(id);
        return (pointEntity.containsKey(Tags.DCWB) && pointEntity.containsKey(Tags.CONFIG))
                || (pointEntity.containsKey(Tags.ADAPTIVE) && pointEntity.containsKey(Tags.DELTA))
                || (pointEntity.containsKey(Tags.MAXIMIZED) && pointEntity.containsKey(Tags.EXIT));
    }

    public static boolean isSenseConfig(String id, CCUHsApi hayStack) {
        HashMap<Object, Object> pointEntity = hayStack.readMapById(id);
        return pointEntity.containsKey(Tags.SENSE)
                && pointEntity.containsKey(Tags.HYPERSTAT);
    }

    public static boolean isBPOSConfig(String id, CCUHsApi hayStack) {
        HashMap<Object, Object> pointEntity = hayStack.readMapById(id);
        return pointEntity.containsKey(Tags.BPOS);
    }
    
    public static boolean isDamperReheatTypeConfig(String id, CCUHsApi hayStack) {
        HashMap<Object, Object> pointEntity = hayStack.readMapById(id);
        return pointEntity.containsKey(Tags.TYPE)
               && (pointEntity.containsKey(Tags.DAMPER) || pointEntity.containsKey(Tags.REHEAT));
    }
    
    /**
     * Currently checks only FCU type. Will be made generic after other profies
     * support is handled.
     */
    public static boolean isStandaloneConfig(String id, CCUHsApi hayStack) {
        HashMap<Object, Object> pointEntity = hayStack.readMapById(id);
        return pointEntity.containsKey(Tags.STANDALONE);
    }
    
    public static boolean isCPUEquip(String id, CCUHsApi hayStack) {
        HashMap<Object, Object> equipMap = hayStack.readMapById(id);
        return equipMap.containsKey(Tags.CPU);
    }
    
    public static boolean isHPUEquip(String id, CCUHsApi hayStack) {
        HashMap<Object, Object> equipMap = hayStack.readMapById(id);
        return equipMap.containsKey(Tags.HPU);
    }

    public static double getSystemUserIntentVal(String tags)
    {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap<Object, Object> cdb = hayStack.readEntity("point and system and userIntent and " + tags);
        ArrayList values = hayStack.readPoint(cdb.get("id").toString());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size(); l++)
            {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("val") != null)
                {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }
    
    public  static String getEquipTag(ArrayList<String> markers) {
        if (markers.contains(Tags.DAB)) {
            return Tags.DAB;
        } else if (markers.contains(Tags.VAV)) {
            return Tags.VAV;
        } else if (markers.contains(Tags.PID)) {
            return Tags.PID;
        } else if (markers.contains(Tags.OAO)) {
            return Tags.OAO;
        } else if (markers.contains(Tags.STANDALONE)) {
            return Tags.STANDALONE;
        } else if (markers.contains(Tags.DUALDUCT)) {
            return Tags.DUALDUCT;
        } else if (markers.contains(Tags.TI)) {
            return Tags.TI;
        }
        return null;
    }
    
    public static ArrayList<String> removeGenericMarkerTags(ArrayList<String> markers) {
        markers.remove(Tags.ZONE);
        markers.remove(Tags.WRITABLE);
        markers.remove(Tags.HIS);
        markers.remove(Tags.SP);
        markers.remove(Tags.SYSTEM);
        return markers;
    }
    
    public static HisItem getHisItemForWritable(String id) {
        return new HisItem(id, new Date(System.currentTimeMillis()),
                    HSUtil.getPriorityVal(id) );
    }
    
    public static Equip getEquipForModule(Short moduleAddr) {
        HashMap<Object, Object> equipMap = CCUHsApi.getInstance().readEntity("equip and group == \""+moduleAddr+"\"");
        if (equipMap.isEmpty()) {
            return null;
        }
        return new Equip.Builder().setHashMap(equipMap).build();
    }
}
