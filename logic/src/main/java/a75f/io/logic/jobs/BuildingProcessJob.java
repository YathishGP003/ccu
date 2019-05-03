package a75f.io.logic.jobs;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.BaseJob;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZoneProfile;

/**
 * Created by samjithsadasivan on 9/14/18.
 */

public class BuildingProcessJob extends BaseJob
{
    HashMap<String, String> tsData;
    
    
    @Override
    public void doJob() {
        CcuLog.d(L.TAG_CCU_JOB,"BuildingProcessJob -> "+CCUHsApi.getInstance());
    
        HashMap site = CCUHsApi.getInstance().read("site");
        if (site.size() == 0) {
            CcuLog.d(L.TAG_CCU_JOB,"No Site Registered ! <-BuildingProcessJob ");
            return;
        }
    
        HashMap ccu = CCUHsApi.getInstance().read("ccu");
        if (ccu.size() == 0) {
            CcuLog.d(L.TAG_CCU_JOB,"No CCU Registered ! <-BuildingProcessJob ");
            return;
        }
    
        tsData = new HashMap();
        
		//Revoking since crash happens due to iterations
        for (ZoneProfile profile : L.ccu().zoneProfiles) {
            CcuLog.d(L.TAG_CCU_JOB, "updateZonePoints -> "+profile.getProfileType());
            profile.updateZonePoints();
        }
        
        if (!Globals.getInstance().isPubnubSubscribed())
        {
            CCUHsApi.getInstance().syncEntityTree();
            String siteLUID = site.get("id").toString();
            String siteGUID = CCUHsApi.getInstance().getGUID(siteLUID);
            if (siteGUID != null && siteGUID != "") {
                Globals.getInstance().registerSiteToPubNub(siteGUID);
            }
        }
    
        L.ccu().systemProfile.doSystemControl();
        L.saveCCUState();
    
        new Thread()
        {
            @Override
            public void run()
            {
                super.run();
                CCUHsApi.getInstance().syncHisData();
            }
        }.start();
        CcuLog.d(L.TAG_CCU_JOB,"<- BuildingProcessJob");
    }
}
