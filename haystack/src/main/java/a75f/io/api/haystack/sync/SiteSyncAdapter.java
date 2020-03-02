package a75f.io.api.haystack.sync;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HRow;
import org.projecthaystack.io.HZincReader;
import org.projecthaystack.io.HZincWriter;

import java.util.ArrayList;
import java.util.Iterator;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;

/**
 * Created by samjithsadasivan on 12/31/18.
 */

public class SiteSyncAdapter extends EntitySyncAdapter
{
    @Override
    public boolean onSync() {
        CcuLog.i("CCU_HS_SYNC", "onSync Site");

        if (!CCUHsApi.getInstance().isCCURegistered()){
            return false;
        }

        HDict sDict =  CCUHsApi.getInstance().readHDict("site");
        HDictBuilder b = new HDictBuilder().add(sDict);
        String siteLUID = b.remove("id").toString();
        String siteGUID = CCUHsApi.getInstance().getGUID(siteLUID);
        if (siteGUID == null)
        {
            ArrayList<HDict> entities = new ArrayList<>();
            entities.add(b.toDict());
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(CCUHsApi.getInstance().getHSUrl() + "addEntity", HZincWriter.gridToString(grid));
            CcuLog.i("CCU_HS_SYNC", "Response : "+response);
            if (response == null) {
                return false;
            }
            HZincReader zReader = new HZincReader(response);
            Iterator it = zReader.readGrid().iterator();
            while (it.hasNext())
            {
                HRow row = (HRow) it.next();
                siteGUID = row.get("id").toString();
            }
            if (siteGUID != null && siteGUID != "")
            {
                CCUHsApi.getInstance().putUIDMap(siteLUID, siteGUID);
            } else {
                return false;
            }
        }
    
        /*if (siteGUID != null && siteGUID != "")
        {
            CCUHsApi.getInstance().putUIDMap(siteLUID, siteGUID);
            if (!doSyncFloors(siteLUID)) {
                //Abort Sync as equips and points need valid floorRef and zoneRef
                CcuLog.i("CCU", "Floor Sync failed : abort ");
                return;
            }
            if (!doSyncEquips(siteLUID)) {
                CcuLog.i("CCU", "Equip Sync failed : abort ");
                return;
            }
            doSyncDevices(siteLUID);
            doSyncSchedules(siteLUID);
        
        }*/
    
        CcuLog.i("CCU_HS_SYNC", "<- doSyncSite");
        return true;
    }
}
