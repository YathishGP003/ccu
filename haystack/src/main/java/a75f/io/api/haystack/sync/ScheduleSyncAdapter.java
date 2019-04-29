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

public class ScheduleSyncAdapter extends EntitySyncAdapter
{
    @Override
    public boolean onSync() {
        ArrayList<HashMap> schedules = CCUHsApi.getInstance().readAll("schedule");
        HashMap site = CCUHsApi.getInstance().read("site");
        String siteLUID = site.get("id").toString();
        ArrayList<String> scheduleLUIDList = new ArrayList<String>();
        ArrayList<HDict> entities = new ArrayList<>();
        for (Map m: schedules)
        {
            String luid = m.remove("id").toString();
            if (CCUHsApi.getInstance().getGUID(luid) == null) {
                scheduleLUIDList.add(luid);
                m.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(siteLUID)));
                if (m.get("roomRef") != null && !m.get("roomRef").toString().equals("SYSTEM"))
                {
                    String guid = CCUHsApi.getInstance().getGUID(m.get("roomRef").toString());
                    if(guid == null)  {
                        CcuLog.i("CCU_HS", "Room not synced for "+m);
                        return false;
                    }
                    m.put("roomRef", HRef.copy(guid));
                }
                entities.add(HSUtil.mapToHDict(m));
            }
        }
    
        if (scheduleLUIDList.size() > 0)
        {
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
            CcuLog.i("CCU_HS", "Response: \n" + response);
            if (response == null)
            {
                CcuLog.i("CCU_HS", "Aborting Schedule Sync");
                return false;
            }
            HZincReader zReader = new HZincReader(response);
            Iterator it = zReader.readGrid().iterator();
            int index = 0;
            while (it.hasNext())
            {
                HRow row = (HRow) it.next();
                String floorGUID = row.get("id").toString();
                if (floorGUID != "")
                {
                    CCUHsApi.getInstance().putUIDMap(scheduleLUIDList.get(index++), floorGUID);
                }
            }
        }
    
        return true;
    }
}
