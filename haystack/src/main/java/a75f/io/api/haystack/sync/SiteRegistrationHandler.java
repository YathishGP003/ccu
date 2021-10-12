package a75f.io.api.haystack.sync;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.projecthaystack.HDict;

import a75f.io.api.haystack.BuildConfig;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.constants.HttpConstants;
import a75f.io.constants.SiteFieldConstants;
import a75f.io.logger.CcuLog;

class SiteRegistrationHandler {
    
    public static final String TAG = "CCU_HS_SiteSyncHandler";
    
    public boolean doSync() {
    
        CcuLog.d(TAG, "SiteRegistrationHandler doSync");
        
        HDict siteDict =  CCUHsApi.getInstance().readHDict("site");
    
        String siteId = siteDict.get("id").toString();
        if (!CCUHsApi.getInstance().isEligibleForSync(siteId)) {
            CcuLog.d(TAG, "SiteRegistrationHandler Site Sync not required : "+siteId);
            return true;
        }
    
        boolean synced = false;
        
        JSONObject siteCreationRequestJson = getSiteJsonRequest(siteDict);
    
        if (siteCreationRequestJson != null) {
            
            boolean siteUpdate = CCUHsApi.getInstance().hasEntitySynced(siteId);
            String siteUrl = siteUpdate ? "sites/"+siteId : "sites";
            try {
                Log.d(TAG, "Sending Site registration request: " + siteCreationRequestJson.toString());
                String response = HttpUtil.executeJson(
                    CCUHsApi.getInstance().getAuthenticationUrl() + siteUrl,
                    siteCreationRequestJson.toString(),
                    BuildConfig.CARETAKER_API_KEY,
                    true, // TODO Matt Rudd - Hate to do this, but the HttpUtils are a mess
                    siteUpdate ? HttpConstants.HTTP_METHOD_PUT : HttpConstants.HTTP_METHOD_POST
                );
            
                if (response != null) {
                    JSONObject siteCreationResponseJson = new JSONObject(response);
                    String siteGuidFromResponse = siteCreationResponseJson.getString("id");
                
                    if (StringUtils.isNotBlank(siteGuidFromResponse) && StringUtils.equals(siteGuidFromResponse, siteId)) {
                        synced = true;
                    }
                
                    CCUHsApi.getInstance().setEntitySynced(siteId);
                
                }
            } catch (JSONException e) {
                e.printStackTrace();
                CcuLog.d(TAG, "Unable to sync site due to JSON exception. This is likely unrecoverable.");
            }
        }
        return synced;
    }
    
    private JSONObject getSiteJsonRequest(HDict siteDict) {
        JSONObject siteCreationRequestJson = new JSONObject();
        
        try {
            siteCreationRequestJson.put(SiteFieldConstants.ID, siteDict.get(SiteFieldConstants.ID));
            siteCreationRequestJson.put(SiteFieldConstants.AREA, siteDict.get(SiteFieldConstants.AREA));
            siteCreationRequestJson.put(SiteFieldConstants.DESCRIPTION, siteDict.dis());
            siteCreationRequestJson.put(SiteFieldConstants.FACILITY_MANAGER_EMAIL, siteDict.get(SiteFieldConstants.FACILITY_MANAGER_EMAIL));
            siteCreationRequestJson.put(SiteFieldConstants.GEOADDRESS, siteDict.get(SiteFieldConstants.GEOADDRESS));
            siteCreationRequestJson.put(SiteFieldConstants.GEOCITY, siteDict.get(SiteFieldConstants.GEOCITY));
            siteCreationRequestJson.put(SiteFieldConstants.GEOCOORDINATES, siteDict.get(SiteFieldConstants.GEOCOORDINATES, false));
            siteCreationRequestJson.put(SiteFieldConstants.GEOCOUNTRY, siteDict.get(SiteFieldConstants.GEOCOUNTRY));
            siteCreationRequestJson.put(SiteFieldConstants.GEOPOSTALCODE, siteDict.get(SiteFieldConstants.GEOPOSTALCODE));
            siteCreationRequestJson.put(SiteFieldConstants.GEOSTATE, siteDict.get(SiteFieldConstants.GEOSTATE));
            siteCreationRequestJson.put(SiteFieldConstants.INSTALLER_EMAIL, siteDict.get(SiteFieldConstants.INSTALLER_EMAIL));
            siteCreationRequestJson.put(SiteFieldConstants.ORGANIZATION, siteDict.get(SiteFieldConstants.ORGANIZATION));
            siteCreationRequestJson.put(SiteFieldConstants.TIMEZONE, siteDict.get(SiteFieldConstants.TIMEZONE));
            siteCreationRequestJson.put(SiteFieldConstants.WEATHERREF, siteDict.get(SiteFieldConstants.WEATHERREF, false));
        } catch (JSONException e) {
            e.printStackTrace();
            CcuLog.d(TAG, "Unable to sync site due to JSON exception. This is likely unrecoverable.");
            siteCreationRequestJson = null;
        }
        
        return siteCreationRequestJson;
    }
}
