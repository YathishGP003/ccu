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

public class ZoneSyncAdapter extends EntitySyncAdapter
{
    @Override
    public boolean onSync() {
        CcuLog.i("CCU", "doSyncZones ->");
        ArrayList<HashMap> floors = CCUHsApi.getInstance().readAll("floor");
        ArrayList<String> zoneLUIDList = new ArrayList();
        ArrayList<HDict> entities = new ArrayList<>();
        HashMap site = CCUHsApi.getInstance().read("site");
        for (Map f: floors)
        {
            ArrayList<HashMap> zones = CCUHsApi.getInstance().readAll("room and floorRef == \""+f.get("id")+"\"");
            for (Map m : zones)
            {
                CcuLog.i("CCU", m.toString());
                String luid = m.remove("id").toString();
                if (CCUHsApi.getInstance().getGUID(luid) == null)
                {
                    zoneLUIDList.add(luid);

                    m.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(site.get("id").toString())));
                    m.put("floorRef", HRef.copy(CCUHsApi.getInstance().getGUID(m.get("floorRef").toString())));
                    if(m.get("scheduleRef") != null ) {
                        String guid = CCUHsApi.getInstance().getGUID(m.get("scheduleRef").toString());
                        if(guid != null) m.put("scheduleRef", HRef.copy(guid));
                    }
                    entities.add(HSUtil.mapToHDict(m));
                }
            }
        }
    
        if (zoneLUIDList.size() > 0)
        {
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
            CcuLog.i("CCU", "Response: \n" + response);
            if (response == null)
            {
                CcuLog.i("CCU", "Aborting Zone Sync");
                return false;
            }
            HZincReader zReader = new HZincReader(response);
            Iterator it = zReader.readGrid().iterator();
            int index = 0;
            while (it.hasNext())
            {
                HRow row = (HRow) it.next();
                String zoneGUID = row.get("id").toString();
                if (zoneGUID != null && zoneGUID != "")
                {
                    CCUHsApi.getInstance().putUIDMap(zoneLUIDList.get(index++), zoneGUID);
                } else {
                    return false;
                }
            }
        }
        return true;
    }
}
