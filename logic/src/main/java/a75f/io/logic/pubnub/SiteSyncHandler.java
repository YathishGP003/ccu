package a75f.io.logic.pubnub;

import com.google.gson.JsonObject;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Site;

public class SiteSyncHandler
{
    public static final String CMD = "sync";
    
    public static void handleMessage(JsonObject msgObject) {
        
        String siteGuid = msgObject.get("siteId") != null ? msgObject.get("siteId").getAsString(): "";
        
        if (!siteGuid.isEmpty()) {
            CCUHsApi hayStack = CCUHsApi.getInstance();
            Site remoteSite = hayStack.getRemoteSiteEntity(siteGuid);
            hayStack.updateSiteLocal(remoteSite, hayStack.getSiteId().toString());
        }
    
    }
}
