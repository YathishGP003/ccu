package a75f.io.api.haystack.sync;

import org.apache.commons.lang3.StringUtils;
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

public class FloorSyncAdapter extends EntitySyncAdapter
{
    @Override
    public boolean onSync() {
    
        CcuLog.i("CCU_HS_SYNC", "onSync Floors");

        String siteRef = CCUHsApi.getInstance().getSiteGuid();

        if (!CCUHsApi.getInstance().isCCURegistered() || StringUtils.isBlank(siteRef)){
            return false;
        }

        ArrayList<HashMap> floors = CCUHsApi.getInstance().readAll("floor");
        ArrayList<String> floorLUIDList = new ArrayList();

        ArrayList<HDict> entities = new ArrayList<>();
        for (Map m: floors)
        {
            String luid = m.remove("id").toString();
            if (CCUHsApi.getInstance().getGUID(luid) == null) {
                floorLUIDList.add(luid);
                m.put("siteRef", HRef.copy(siteRef));
                entities.add(HSUtil.mapToHDict(m));
            }
        }
    
        if (floorLUIDList.size() > 0)
        {
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "addEntity", HZincWriter.gridToString(grid));
            CcuLog.i("CCU_HS_SYNC", "Response: \n" + response);
            if (response == null)
            {
                CcuLog.i("CCU_HS_SYNC", "Aborting Floor Sync");
                return false;
            }
            HZincReader zReader = new HZincReader(response);
            Iterator it = zReader.readGrid().iterator();
            int index = 0;
            while (it.hasNext())
            {
                HRow row = (HRow) it.next();
                String floorGUID = row.get("id").toString();
                if (floorGUID != null && floorGUID != "")
                {
                    CCUHsApi.getInstance().putUIDMap(floorLUIDList.get(index++), floorGUID);
                } else {
                    return false;
                }
            }
        }
        
        return true;
    }
}
