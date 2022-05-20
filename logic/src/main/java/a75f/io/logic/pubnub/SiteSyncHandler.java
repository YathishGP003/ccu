package a75f.io.logic.pubnub;

import android.content.Context;
import android.content.Intent;
import com.google.gson.JsonObject;

import org.projecthaystack.client.HClient;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Site;
import a75f.io.logic.bo.util.RenatusLogicIntentActions;

public class SiteSyncHandler
{
    public static final String CMD = "sync";
    
    public static void handleMessage(JsonObject msgObject, Context context) {
        
        String siteGuid = msgObject.get("siteId") != null ? msgObject.get("siteId").getAsString(): "";
        
        if (!siteGuid.isEmpty()) {
            
            CCUHsApi hayStack = CCUHsApi.getInstance();
            Site remoteSite = hayStack.getRemoteSiteEntity(siteGuid);

            if(!remoteSite.getOrganization().equals(hayStack.getSite().getOrganization())){
                hayStack.removeAllNamedSchedule();
                updateSiteOrganisationwithNamedSchedules(hayStack,remoteSite.getOrganization());
            }
            //"sync" pubnubs are generated for other entities in a Site too.
            //SiteSyncHandler should take care of only the Site entity updates.
            if (remoteSite != null && !remoteSite.equals(hayStack.getSite())) {
                hayStack.updateSiteLocal(remoteSite, hayStack.getSiteIdRef().toString());
                Intent locationUpdateIntent = new Intent(RenatusLogicIntentActions.ACTION_SITE_LOCATION_UPDATED);
                context.sendBroadcast(locationUpdateIntent);
            }
        }
    
    }

    private static void updateSiteOrganisationwithNamedSchedules(CCUHsApi hsApi, String organization) {
        HClient hClient = new HClient(hsApi.getHSUrl(), HayStackConstants.USER, HayStackConstants.PASS);
        hsApi.importNamedScheduleWithOrg(hClient, organization);
    }
}
