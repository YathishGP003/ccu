package a75f.io.api.haystack.sync;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.HStr;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;

/**
 * Created by samjithsadasivan on 12/31/18.
 */

public class PointSyncAdapter extends EntitySyncAdapter
{
    @Override
    public boolean onSync() {
        CcuLog.i("CCU_HS_SYNC", "onSync Points");
        HashMap site = CCUHsApi.getInstance().read("site");
        String siteLUID = site.get("id").toString();
        ArrayList<HashMap> equips = CCUHsApi.getInstance().readAll("equip and siteRef == \"" + siteLUID + "\"");
        for (Map q: equips)
        {
        
            String equipLUID = q.remove("id").toString();
        
            ArrayList<HashMap> points = CCUHsApi.getInstance().readAll("point and equipRef == \"" + equipLUID + "\"");
        
            if (points.size() == 0) {
                continue;
            }
            ArrayList<String> pointLUIDList = new ArrayList();
            ArrayList<HDict> entities = new ArrayList<>();
            for (Map m : points)
            {
                //CcuLog.i("CCU", m);
                String luid = m.remove("id").toString();
                if (CCUHsApi.getInstance().getGUID(luid) == null)
                {
                    pointLUIDList.add(luid);
                    m.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(siteLUID)));
                    String gEquipLUID = CCUHsApi.getInstance().getGUID(equipLUID);
                    //Crash here ...due to sync...While no equips no need to sync equip pointers
                    if(gEquipLUID != null) {
                        m.put("equipRef", HRef.copy(gEquipLUID));
                        if (m.get("floorRef") != null && !m.get("floorRef").toString().equals("SYSTEM")) {
                            String guid = CCUHsApi.getInstance().getGUID(m.get("floorRef").toString());
                            if(guid == null) {
                                return false;
                            }
                            m.put("floorRef", HRef.copy(guid));
                        }
                        if (m.get("roomRef") != null && !m.get("roomRef").toString().equals("SYSTEM")) {
                            String guid = CCUHsApi.getInstance().getGUID(m.get("roomRef").toString());
                            if(guid == null) {
                                return false;
                            }
                            m.put("roomRef", HRef.copy(guid));
                        }
                        entities.add(HSUtil.mapToHDict(m));
                    }
                }
            }
        
            if (pointLUIDList.size() > 0)
            {
                HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
                String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "addEntity", HZincWriter.gridToString(grid));
                CcuLog.i("CCU_HS_SYNC", "Response: \n" + response);
                if (response == null)
                {
                    return false;
                }
                HZincReader zReader = new HZincReader(response);
                Iterator it = zReader.readGrid().iterator();
                final ArrayList<String> syncedPoints = new ArrayList();
                int index = 0;
                while (it.hasNext())
                {
                    HRow row = (HRow) it.next();
                    String guid = row.get("id").toString();
                    
                    if (guid != null && guid != "")
                    {
                        String pointLuid = pointLUIDList.get(index++);
                        CCUHsApi.getInstance().putUIDMap(pointLuid, guid);
                        syncedPoints.add(pointLuid);
                    } else {
                        return false;
                    }
                }
    
                new Thread() {
                    @Override
                    public void run() {
                        initWritableRemotePoints(syncedPoints);
                    }
                }.start();
    
            }
        }
        return true;
    }
    
    private void initWritableRemotePoints(ArrayList<String> luidList) {
        for (String luid: luidList)
        {
            if (isWritable(luid))
            {
                writeValRemote(luid, CCUHsApi.getInstance().getGUID(luid));
            }
        }
    }
    
    private boolean isWritable(String id) {
        Point p = new Point.Builder().setHashMap(CCUHsApi.getInstance().readMapById(id)).build();
        
        for (String marker : p.getMarkers()) {
            if (marker.equals("writable")) {
                return true;
            }
        }
        return false;
    }
    
    private void writeValRemote(String luid, String guid) {
        ArrayList<HashMap> pointArr = CCUHsApi.getInstance().readPoint(luid);
        for (HashMap valMap : pointArr) {
            if (valMap.get("val") != null)
            {
                
                boolean isDouble = false;
                Double val = 0.0;
                try
                {
                    val = Double.parseDouble(valMap.get("val").toString());
                    isDouble = true;
                }
                catch (NumberFormatException e)
                {
                    CcuLog.d("CCU_HS", "Writable Val is not Double " + valMap.get("val").toString());
                }
                
                HDictBuilder b = new HDictBuilder()
                                         .add("id", HRef.copy(guid))
                                         .add("level", (int) Double.parseDouble(valMap.get("level").toString()))
                                         .add("who", valMap.get("who").toString())
                                         .add("val", isDouble? HNum.make(val) : HStr.make(valMap.get("val").toString()));
                HDict[] dictArr = {b.toDict()};
                String r = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "pointWrite", HZincWriter.gridToString(HGridBuilder.dictsToGrid(dictArr)));
                CcuLog.d("CCU_HS","Response: \n" + r);
            }
        }
    
    }
}
