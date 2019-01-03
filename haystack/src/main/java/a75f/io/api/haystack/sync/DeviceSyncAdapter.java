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

public class DeviceSyncAdapter extends EntitySyncAdapter
{
    @Override
    public boolean onSync() {
        CcuLog.i("CCU", "doSyncDevices ->");
        HashMap site = CCUHsApi.getInstance().read("site");
        String siteLUID = site.get("id").toString();
        ArrayList<HashMap> devices = CCUHsApi.getInstance().readAll("device");
        ArrayList<String> deviceLUIDList = new ArrayList();
        ArrayList<HDict> entities = new ArrayList<>();
        for (Map m: devices)
        {
            CcuLog.i("CCU", m.toString());
            String luid = m.remove("id").toString();
            if (CCUHsApi.getInstance().getGUID(luid) == null) {
                deviceLUIDList.add(luid);
                m.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(siteLUID)));
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
    
        if (deviceLUIDList.size() > 0)
        {
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
            CcuLog.i("CCU", "Response: \n" + response);
            if (response == null) {
                return false;
            }
            HZincReader zReader = new HZincReader(response);
            Iterator it = zReader.readGrid().iterator();
            int index = 0;
        
            while (it.hasNext())
            {
                HRow row = (HRow) it.next();
                String deviceGUID = row.get("id").toString();
                if (deviceGUID != null && deviceGUID != "")
                {
                    CCUHsApi.getInstance().putUIDMap(deviceLUIDList.get(index++), deviceGUID);
                } else {
                    return false;
                }
            }
        }
        return true;
    }
}
