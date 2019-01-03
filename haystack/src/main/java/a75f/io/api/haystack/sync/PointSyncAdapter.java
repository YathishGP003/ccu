package a75f.io.api.haystack.sync;

import org.projecthaystack.HDict;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.logger.CcuLog;

/**
 * Created by samjithsadasivan on 12/31/18.
 */

public class PointSyncAdapter extends EntitySyncAdapter
{
    @Override
    public boolean onSync() {
        CcuLog.i("CCU", "doSyncPoints ->");
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
                    m.put("equipRef", HRef.copy(CCUHsApi.getInstance().getGUID(equipLUID)));
                    if (m.get("floorRef") != null && !m.get("floorRef").toString().equals("SYSTEM"))
                    {
                        m.put("floorRef", HRef.copy(CCUHsApi.getInstance().getGUID(m.get("floorRef").toString())));
                    }
                    if (m.get("zoneRef") != null && !m.get("zoneRef").toString().equals("SYSTEM"))
                    {
                        m.put("zoneRef", HRef.copy(CCUHsApi.getInstance().getGUID(m.get("zoneRef").toString())));
                    }
                    entities.add(HSUtil.mapToHDict(m));
                }
            }
        
            if (pointLUIDList.size() > 0)
            {
                HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
                String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
                CcuLog.i("CCU", "Response: \n" + response);
                if (response == null)
                {
                    return false;
                }
                HZincReader zReader = new HZincReader(response);
                Iterator it = zReader.readGrid().iterator();
                int index = 0;
                while (it.hasNext())
                {
                    HRow row = (HRow) it.next();
                    String guid = row.get("id").toString();
                    if (guid != null && guid != "")
                    {
                        CCUHsApi.getInstance().putUIDMap(pointLUIDList.get(index++), guid);
                    }
                }
            }
        }
        return true;
    }
}
