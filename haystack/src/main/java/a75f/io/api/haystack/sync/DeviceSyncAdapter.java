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
        CcuLog.i("CCU_HS_SYNC", "onSync Devices");
        HashMap site = CCUHsApi.getInstance().read("site");
        String siteLUID = site.get("id").toString();
        ArrayList<HashMap> devices = CCUHsApi.getInstance().readAll("device");
        ArrayList<String> deviceLUIDList = new ArrayList();
        ArrayList<HDict> entities = new ArrayList<>();
        for (Map m: devices)
        {
            CcuLog.i("CCU_HS_SYNC", m.toString());
            String luid = m.remove("id").toString();
            if (CCUHsApi.getInstance().getGUID(luid) == null) {
                deviceLUIDList.add(luid);
                m.put("siteRef", HRef.copy(CCUHsApi.getInstance().getGUID(siteLUID)));
                //Crash happens here because device not paired but tries to sync, 
					
				if (m.get("floorRef") != null && !m.get("floorRef").toString().equals("SYSTEM"))
                {
                    String guid = CCUHsApi.getInstance().getGUID(m.get("floorRef").toString());
                    if(guid == null) {
                        return false;
                    }
                    m.put("floorRef", HRef.copy(guid));
                }
                if (m.get("roomRef") != null && !m.get("roomRef").toString().equals("SYSTEM"))
                {
                    String guid = CCUHsApi.getInstance().getGUID(m.get("roomRef").toString());
                    if(guid == null) {
                        return false;
                    }
                    m.put("roomRef", HRef.copy(guid));
                }
                if (m.get("equipRef") != null)
                {
                    String guid = CCUHsApi.getInstance().getGUID(m.get("equipRef").toString());
                    if(guid == null) {
                        return false;
                    }
                    m.put("equipRef", HRef.copy(guid));
                }
                if (m.get("ahuRef") != null)
                {
					String guid = CCUHsApi.getInstance().getGUID(m.get("ahuRef").toString());
                    if(guid == null) {
                        return false;
                    }
                    m.put("ahuRef", HRef.copy(guid));
                }
                entities.add(HSUtil.mapToHDict(m));
            }
        }
    
        if (deviceLUIDList.size() > 0)
        {
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(HttpUtil.HAYSTACK_URL + "addEntity", HZincWriter.gridToString(grid));
            CcuLog.i("CCU_HS_SYNC", "Response: \n" + response);
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
