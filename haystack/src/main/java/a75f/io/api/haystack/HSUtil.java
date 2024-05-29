package a75f.io.api.haystack;

import static a75f.io.api.haystack.Tags.BACNET_ID;

import com.google.gson.internal.LinkedTreeMap;

import org.apache.commons.lang3.StringUtils;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HVal;
import org.projecthaystack.MapImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    public static ArrayList<Equip> getNonModbusEquips(String roomRef) {

        ArrayList<HashMap<Object, Object>> equips =
                CCUHsApi.getInstance().readAllEntities("equip and not modbus and roomRef == \""+roomRef+"\"");
        ArrayList<Equip> equipList = new ArrayList<>();
        for (HashMap<Object, Object> m : equips)
        {
            equipList.add(new Equip.Builder().setHashMap(m).build());
        }
        return equipList;
    }

    public static List<Equip> getEquipsWithoutSubEquips(String roomRef) {

        List<HashMap<Object, Object>> equips =
                CCUHsApi.getInstance().readAllEntities("equip and not equipRef and roomRef == \""+roomRef+"\"");
        List<Equip> equipList = new ArrayList<>();
        for (HashMap<Object, Object> m : equips)
        {
            equipList.add(new Equip.Builder().setHashMap(m).build());
        }
        return equipList;
    }

    public static boolean isZoneHasSubEquips(String roomRef) {
        return CCUHsApi.getInstance().readAllEntities("equip and equipRef and roomRef == \""+roomRef+"\"")
                .size() > 0;
    }

    public static List<Equip> getSubEquips(String parentEquipId){
        List<HashMap<Object, Object>> equips =
                CCUHsApi.getInstance().readAllEntities("equip and equipRef == \""+parentEquipId+"\"");
        List<Equip> equipList = new ArrayList<>();
        for (HashMap<Object, Object> m : equips) {
            equipList.add(new Equip.Builder().setHashMap(m).build());
        }
        return equipList;
    }

    public static boolean isEquipHasEquipsWithAhuRefOnThisCcu(String parentEquipId) {
        return CCUHsApi.getInstance().readAllEntities("equip and ahuRef == \""+parentEquipId+"\" and ccuRef == \""+CCUHsApi.getInstance().getCcuId()+"\"").size() > 0;
    }

    public static List<Equip> getEquipsWithAhuRefOnThisCcu(String parentEquipId){
        List<HashMap<Object, Object>> equips =
                CCUHsApi.getInstance().readAllEntities("equip and ahuRef == \""+parentEquipId+"\" and ccuRef == \""+CCUHsApi.getInstance().getCcuId()+"\"");
        List<Equip> equipList = new ArrayList<>();
        for (HashMap<Object, Object> m : equips) {
            equipList.add(new Equip.Builder().setHashMap(m).build());
        }
        return equipList;
    }

    public static List<Short> getSubEquipPairingAddr(String parentEquipPairingAddr){
        HashMap<Object, Object> parentEquipMap =
                CCUHsApi.getInstance().readEntity("equip and not equipRef and group == \""+parentEquipPairingAddr+"\"");
        List<Equip> subEquipList = getSubEquips(parentEquipMap.get("id").toString());
        List<Short> subEquipAddrList = new ArrayList<>();
        for(Equip subEquip : subEquipList){
            subEquipAddrList.add(Short.parseShort(subEquip.getGroup()));
        }
        return subEquipAddrList;
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
        HDict equip = CCUHsApi.getInstance().readHDict("equip and roomRef == \""+roomRef+"\"");
        return new Equip.Builder().setHDict(equip).build();
    }
    
    public static String getZoneIdFromEquipId(String equipId) {
        HDict equipDict = CCUHsApi.getInstance().readHDictById(equipId);
        Equip equip = new Equip.Builder().setHDict(equipDict).build();
        return equip.getRoomRef();
    }
    public static Equip getEquipInfo(String equipId) {
        return getEquip( CCUHsApi.getInstance(), equipId);
    }

    public static HDict getEquipDict(CCUHsApi hayStack, String equipId) {
        return hayStack.readHDictById(equipId);
    }
    
    public static Equip getEquip(CCUHsApi hayStack, String equipId) {
        HDict equipDict = CCUHsApi.getInstance().readHDictById(equipId);
        return new Equip.Builder().setHDict(equipDict).build();
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
            else if (entry.getValue() instanceof HRef)
            {
                b.add(entry.getKey(), (HRef) entry.getValue());
            }
            else
            {
                if(entry.getValue().getClass() == com.google.gson.internal.LinkedTreeMap.class) {

                    LinkedTreeMap<String,Object> linkedTreeMap = (LinkedTreeMap<String,Object>) entry.getValue();
                    HDictBuilder dictForMap = new HDictBuilder();
                    Set<String> keySet = linkedTreeMap.keySet();
                    for (String key: keySet) {
                        dictForMap.add(key,linkedTreeMap.get(key).toString());
                    }

                    b.add(entry.getKey(), (HVal) dictForMap.toDict());
                } else{
                    b.add(entry.getKey(),(String) entry.getValue());
                }

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
                    CcuLog.d("CCU_HS", " Override updated point " + id + " , level: " + l + " , val :" + Double.parseDouble(valMap.get("val").toString())
                                    +" duration: "+Double.parseDouble(valMap.get("duration").toString()));
                }
            }
        }
    }
    
    public static void printPointArr(Point p, String tag) {
        ArrayList values = CCUHsApi.getInstance().readPoint(p.getId());
        if (values != null && values.size() > 0)
        {
            for (int l = 1; l <= values.size(); l++)
            {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("val") != null)
                {
                    CcuLog.d(tag,
                          "Updated point " + p.getDisplayName() + " , level: " + l + " , val :" + Double.parseDouble(valMap.get("val").toString())
                                                        +" duration: "+Double.parseDouble(valMap.get("duration").toString()));
                }
            }
        }
    }
    
    public static double getPriorityVal(String id) {
        return getPriorityVal(id, CCUHsApi.getInstance());
    }
    
    public static double getPriorityVal(String id, CCUHsApi hayStack) {
        ArrayList values = hayStack.readPoint(id);
        if (values != null && values.size() > 0) {
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
        return getPriorityLevel(id, level, CCUHsApi.getInstance());
    }
    
    public static HashMap<Object, Object> getPriorityLevel(String id, int level, CCUHsApi hayStack) {
        ArrayList values = hayStack.readPoint(id);
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
        return (tunerEquip.get(Tags.ID).toString()).equals(entityMap.get(Tags.EQUIPREF).toString());
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

    public static boolean isMonitoringConfig(String id, CCUHsApi hayStack) {
        HashMap<Object, Object> pointEntity = hayStack.readMapById(id);
        return pointEntity.containsKey(Tags.MONITORING)
                && pointEntity.containsKey(Tags.HYPERSTAT);
    }

    public static boolean isHyperStatConfig(String id, CCUHsApi hayStack) {
        Point localPoint = new Point.Builder().setHashMap(CCUHsApi.getInstance().readMapById(id)).build();
        HashMap equip = hayStack.readMapById(localPoint.getEquipRef());
        return equip.containsKey(Tags.HYPERSTAT) &&
                ( equip.containsKey(Tags.CPU) ||  equip.containsKey(Tags.PIPE2)
                || equip.containsKey(Tags.PIPE4) ||  equip.containsKey(Tags.HPU) || equip.containsKey(Tags.VRV));
    }

    public static boolean isHyperStatSplitConfig(String id, CCUHsApi hayStack) {
        Point localPoint = new Point.Builder().setHashMap(CCUHsApi.getInstance().readMapById(id)).build();
        HashMap equip = hayStack.readMapById(localPoint.getEquipRef());
        return equip.containsKey(Tags.HYPERSTATSPLIT) && equip.containsKey(Tags.CPU);
    }
    
    public static boolean isPIConfig(String id, CCUHsApi hayStack) {
        HashMap pointEntity = hayStack.readMapById(id);
        return pointEntity.containsKey(Tags.PLC);
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

    public static boolean isVAVTrueCFMConfig(String id, CCUHsApi hayStack) {
        HashMap<Object,Object> pointEntity = hayStack.readMapById(id);
        if (pointEntity.containsKey("domainName")) {
            return pointEntity.get("domainName").equals("enableCFMControl");
        } else {
            return ((pointEntity.containsKey(Tags.ENABLE)) && (pointEntity.containsKey(Tags.CFM) || pointEntity.containsKey("trueCFM")) && (pointEntity.containsKey(Tags.VAV)));
        }
    }

    public static boolean isVAVZonePriorityConfig(String id, CCUHsApi hayStack) {
        HashMap<Object,Object> pointEntity = hayStack.readMapById(id);
        if (pointEntity.containsKey("domainName")) {
            return pointEntity.get("domainName").equals("zonePriority") && pointEntity.containsKey("vav");
        }
        return false;
    }

    public static boolean isDABTrueCFMConfig(String id, CCUHsApi hayStack) {
            HashMap<Object,Object> pointEntity = hayStack.readMapById(id);
            return (pointEntity.containsKey(Tags.ENABLE) && (pointEntity.containsKey(Tags.CFM) || pointEntity.containsKey("trueCFM")) && pointEntity.containsKey(Tags.DAB));
        }

    public static boolean isMaxCFMCoolingConfigPoint(String id, CCUHsApi hayStack) {
        HashMap<Object,Object> pointEntity = hayStack.readMapById(id);
        return ((pointEntity.containsKey(Tags.MAX)) && (pointEntity.containsKey(Tags.CFM) || pointEntity.containsKey("trueCFM")) && (pointEntity.containsKey(Tags.COOLING)));
    }

    public static boolean isMaxCFMReheatingConfigPoint(String id, CCUHsApi hayStack) {
        HashMap<Object,Object> pointEntity = hayStack.readMapById(id);
        return ((pointEntity.containsKey(Tags.MAX)) && (pointEntity.containsKey(Tags.CFM) || pointEntity.containsKey("trueCFM")) && (pointEntity.containsKey(Tags.HEATING)));
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
        markers.remove(Tags.CUR);
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

    public static boolean isTIProfile(String pointUid, CCUHsApi instance) {
        HashMap<Object, Object> pointEntity = instance.readMapById(pointUid);
        return ((pointEntity.containsKey("ti")
                && pointEntity.containsKey("config"))
                && (pointEntity.containsKey("space")
                || pointEntity.containsKey("supply")));
    }

    /**
     * Currently on conditioning mode change is being considered for loop reset.
     * We will have
     * @param pointUid
     * @param instance
     * @return
     */
    public static boolean isPointUpdateNeedsSystemProfileReset(String pointUid, CCUHsApi instance) {
        HashMap<Object, Object> pointEntity = instance.readMapById(pointUid);
        return ((pointEntity.containsKey(Tags.CONDITIONING)
                && pointEntity.containsKey(Tags.MODE)));
    }

    /**
     * Checks a given point is a limit tuner.
     * @param id
     * @param hayStack
     * @return
     */
    public static boolean isBuildingLimitPoint(String id, CCUHsApi hayStack) {
        if (id == null) {
            return false;
        }

        Point tunerPoint = new Point.Builder()
                .setHashMap(hayStack.readMapById(id))
                .build();

        return (tunerPoint.getMarkers().contains("building") && tunerPoint.getMarkers().contains("limit") &&
                !tunerPoint.getMarkers().contains("alert"))
                || (tunerPoint.getMarkers().contains("cooling") && tunerPoint.getMarkers().contains("limit"))
                || (tunerPoint.getMarkers().contains("heating") && tunerPoint.getMarkers().contains("limit"));
    }



    public static double getLevelValueFrom10(String id){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<HashMap> values = hayStack.readPoint(id);
        if (values != null && !values.isEmpty()) {
            HashMap<Object,Object> valMapAtLevel10 =  values.get(9);
            HashMap<Object,Object> valMapAtLevel17 =  values.get(16);
            if (valMapAtLevel10.containsKey("val")) {
                return Double.parseDouble(valMapAtLevel10.get("val").toString());
            }else if(valMapAtLevel17.containsKey("val")){
                return Double.parseDouble(valMapAtLevel17.get("val").toString());
            }
        }
        return 0;
    }

//    /**
//     * Checks a given point is a limit tuner.
//     * @param id
//     * @param hayStack
//     * @return
//     */
//    public static boolean isBuildingLimitPoint(String id, CCUHsApi hayStack) {
//        if (id == null) {
//            return false;
//        }
//
//        Point tunerPoint = new Point.Builder()
//                .setHashMap(hayStack.readMapById(id))
//                .build();
//
//        return (tunerPoint.getMarkers().contains("building") && tunerPoint.getMarkers().contains("limit") &&
//                !tunerPoint.getMarkers().contains("alert"))
//                || (tunerPoint.getMarkers().contains("cooling") && tunerPoint.getMarkers().contains("limit"))
//                || (tunerPoint.getMarkers().contains("heating") && tunerPoint.getMarkers().contains("limit"));
//    }

    public static double getLevelValueFrom16ByQuery(String query){
        HashMap<Object,Object> point = CCUHsApi.getInstance().readEntity(query);
        if(!point.isEmpty())
            return getLevelValueFrom16(point.get("id").toString());
        return 0;
    }

    public static double getLevelValueFrom16(String id){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList<HashMap> values = hayStack.readPoint(id);
        if (values != null && !values.isEmpty()) {
            for (int l = 16; l <= 17; l++) {
                HashMap<Object,Object> valMap =  values.get(l - 1);
                if (valMap.containsKey("val")) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return 0;
    }

    public static void writeValToALLLevel16(ArrayList<HashMap<Object, Object>> points, float val){
        for (HashMap<Object, Object> point:points) {
            writeValtolevel16(point,val);
        }
    }

    public static void writeValtolevel16(HashMap<Object, Object> point, float val) {
        CCUHsApi hsApi = CCUHsApi.getInstance();
        String pointId = point.get("id").toString();
        double level16Val = getPriorityLevelVal(pointId,16);
        if(level16Val == val)
                return;

        hsApi.writePointForCcuUser(pointId, 16, (double) val, 0);
        hsApi.writeHisValById(pointId, (double) val);

    }

    public static void writeValLevel10(HashMap<Object, Object> point, double val) {
        CCUHsApi hsApi = CCUHsApi.getInstance();
        String pointId = point.get("id").toString();
        double existingVal = HSUtil.getPriorityLevelVal(pointId,HayStackConstants.USER_APP_WRITE_LEVEL);
        if(existingVal != val) {
            hsApi.writePointForCcuUser(pointId, 10, val, 0);
            hsApi.writeHisValById(pointId, val);
        }

    }

    public static boolean isSchedulable(String entityId, CCUHsApi hayStack) {
        HashMap<Object, Object> entityMap = hayStack.readMapById(entityId);
        return entityMap.containsKey(Tags.SCHEDULABLE) ||
                (entityMap.containsKey(Tags.BUILDING) && !entityMap.containsKey(Tags.TUNER) &&
                        (entityMap.containsKey(Tags.LIMIT) || entityMap.containsKey(Tags.DIFF)));
    }

    public static void addPointToLocalDB(HDict dict){
        HashMap<Object, Object> map = new HashMap<>();
        Iterator it = dict.iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            map.put(entry.getKey().toString(), entry.getValue().toString());
        }
        Point point = new Point.Builder().setHashMap(map).build();
        CCUHsApi.getInstance().addPoint(point);
    }

    public static String getLevelEntityOfPoint(String id, int level, String value, CCUHsApi hayStack) {
        ArrayList<HashMap> values = hayStack.readPoint(id);
        if (!values.isEmpty()) {
            HashMap valMap = values.get(level - 1);
            if (valMap.containsKey(value)) {
                return valMap.get(value).toString();
            }
        }
        return null;
    }

    public static boolean isPointBackfillConfigPoint(String id, CCUHsApi ccuHsApi) {
        HashMap<Object,Object> pointEntity = ccuHsApi.readMapById(id);
        return ((pointEntity.containsKey(Tags.BACKFILL))&&(pointEntity.containsKey(Tags.DURATION)));
    }

    public static boolean isBypassDamperPresentInSystem(CCUHsApi ccuHsApi) {
        try {
            ArrayList<Equip> sysEquips = getEquips("SYSTEM");
            Optional<Equip> bdEquip = sysEquips.stream().filter(eq -> eq.getDomainName() != null && eq.getDomainName().equals("smartnodeBypassDamper") && eq.getCcuRef().equals(ccuHsApi.getCcuId())).findAny();
            return bdEquip.isPresent();
        } catch (Exception e) {
            return false;
        }
    }

    public static Equip getBypassDamperEquip(CCUHsApi ccuHsApi) {
        try {
            ArrayList<Equip> sysEquips = getEquips("SYSTEM");
            Optional<Equip> bdEquip = sysEquips.stream().filter(eq -> eq.getDomainName() != null && eq.getDomainName().equals("smartnodeBypassDamper") && eq.getCcuRef().equals(ccuHsApi.getCcuId())).findAny();
            if (bdEquip.isPresent()) {
                return bdEquip.get();
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static HDict mapToHDict(HashMap<String, String> m)
    {
        HDictBuilder b = new HDictBuilder();
        for (HashMap.Entry<String, String> entry : m.entrySet())
        {
            b.add(entry.getKey(),  entry.getValue());

        }
        return b.toDict();
    }

    public static boolean isDomainEquip(String equipRef, CCUHsApi hayStack) {
        HashMap equipMap = hayStack.read("equip and id == " + equipRef);
        Equip equip = new Equip.Builder().setHashMap(equipMap).build();
        return equipMap.containsKey("domainName") ? !equip.getDomainName().equals(null) : false;
    }
    public static int generateBacnetId(String zoneID) {
        int bacnetID = 1;
        boolean isBacnetIDUsed = true;
        try {
            HashMap currentRoom = CCUHsApi.getInstance().readMapById(zoneID);
            if (currentRoom!= null && currentRoom.size()>0 && currentRoom.containsKey(BACNET_ID) && (Integer.parseInt(currentRoom.get(BACNET_ID).toString())) != 0) {
                double bacnetID2 = Double.parseDouble(currentRoom.get(BACNET_ID).toString() + "");
                CcuLog.d(Tags.BACNET, "Already have bacnetID $bacnetID2");
                return (int) bacnetID2;
            }
            ArrayList<HashMap<Object, Object>> rooms = CCUHsApi.getInstance().readAllEntities("room");
            ArrayList<HashMap<Object, Object>> equips = CCUHsApi.getInstance().readAllEntities("equip");
            ArrayList<HashMap<Object, Object>> allEntities = new ArrayList<>();
            allEntities.addAll(rooms);
            allEntities.addAll(equips);
            if (allEntities.size() == 0) {
                CcuLog.d(Tags.BACNET, "rooms size : 0 ");
                return bacnetID;
            }
            while (isBacnetIDUsed) {

                for (HashMap<Object, Object> room : allEntities) {
                    if (room.containsKey(BACNET_ID)
                            && Double.parseDouble(room.get(BACNET_ID).toString()) != 0
                            && Double.parseDouble(room.get(BACNET_ID).toString() + "") == bacnetID
                    ) {
                        CcuLog.d(Tags.BACNET, "In looping over - {bacnetID: ${room[BACNET_ID]} ,tempBacnetID: $bacnetID} - room object: $room");
                        bacnetID += 1;
                        isBacnetIDUsed = true;
                        break;
                    } else {
                        isBacnetIDUsed = false;
                    }
                }
            }
            CcuLog.d(Tags.BACNET, "Generated bacnetID: $bacnetID");
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return Integer.parseInt(zoneID + bacnetID);
    }
}
