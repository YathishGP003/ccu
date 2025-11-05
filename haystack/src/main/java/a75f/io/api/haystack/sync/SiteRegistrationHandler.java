package a75f.io.api.haystack.sync;

import android.preference.PreferenceManager;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.projecthaystack.HDict;
import org.projecthaystack.HStr;

import java.util.HashMap;

import a75f.io.api.haystack.BuildConfig;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.constants.CcuFieldConstants;
import a75f.io.constants.HttpConstants;
import a75f.io.constants.SiteFieldConstants;
import a75f.io.logger.CcuLog;

public class SiteRegistrationHandler {
    
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

        boolean siteUpdate = CCUHsApi.getInstance().hasEntitySynced(siteId);
        JSONObject siteCreationRequestJson = getSiteJsonRequest(siteDict, siteUpdate);
    
        if (siteCreationRequestJson != null) {
            String siteUrl = siteUpdate ? "sites/"+siteId : "sites";
            try {
                CcuLog.d(TAG, "Sending Site registration request: " + siteCreationRequestJson);
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

    /**
     * Used for first time registration of the site
     * @return
     */
    public CareTakerResponse sendSiteData() {
        HDict siteDict =  CCUHsApi.getInstance().readHDict("site");

        String siteId = siteDict.get("id").toString();
        if (!CCUHsApi.getInstance().isEligibleForSync(siteId)) {
            CcuLog.d(TAG, "SiteRegistrationHandler Site Sync not required : "+siteId);
            return new CareTakerResponse(200);
        }

        JSONObject siteCreationRequestJson = getSiteJsonRequest(siteDict, false);

        if (siteCreationRequestJson != null) {
            try {
                CcuLog.d(TAG, "Sending Site registration request: " + siteCreationRequestJson);
                CareTakerResponse response = HttpUtil.executeJsonWithApiKey(
                        CCUHsApi.getInstance().getAuthenticationUrl() + "sites",
                        siteCreationRequestJson.toString(),
                        BuildConfig.CARETAKER_API_KEY,
                        HttpConstants.HTTP_METHOD_POST
                );

                if (response != null && response.getResponseCode() == 200) {
                    JSONObject siteCreationResponseJson = new JSONObject(response.getResponseMessage());
                    String siteGuidFromResponse = siteCreationResponseJson.getString("id");
                    CCUHsApi.getInstance().setEntitySynced(siteId);
                    CcuLog.d(TAG, "Site registration successful: " + siteGuidFromResponse);
                }
                return response;
            } catch (JSONException e) {
                e.printStackTrace();
                CcuLog.d(TAG, "Unable to sync site due to JSON exception. This is likely unrecoverable.");
            }
        }
        return null;
    }
    
    private JSONObject getSiteJsonRequest(HDict siteDict, boolean siteUpdate) {
        HashMap<Object, Object> tunerEquip = CCUHsApi.getInstance().readEntity("equip and tuner");
        if(tunerEquip.isEmpty()) {
            CcuLog.d(TAG, " Tuner equip does not exist. Cant complete site registration");
            return null;
        }
        JSONObject siteCreationRequestJson = new JSONObject();

        try {
            if (!siteUpdate) {
                boolean isPreconfiguration = PreferenceManager.getDefaultSharedPreferences(CCUHsApi.getInstance().getContext())
                        .getString("INSTALL_TYPE","").equals("PRECONFIGCCU");
                if (isPreconfiguration) {
                    String preconfigurationId = PreferenceManager.getDefaultSharedPreferences(CCUHsApi.getInstance().getContext())
                            .getString("sitePreConfigId", "");
                    CcuLog.d(TAG, "Preconfiguration site id: " + preconfigurationId);
                    siteCreationRequestJson.put(SiteFieldConstants.PRECONFIG_ID, preconfigurationId);
                }
            }

            String billingAdminEmail;
            if(siteDict.has(CcuFieldConstants.BILLING_ADMIN_EMAIL)){
                billingAdminEmail = siteDict.get(CcuFieldConstants.BILLING_ADMIN_EMAIL).toString();
            } else {
                billingAdminEmail = siteDict.get(SiteFieldConstants.FACILITY_MANAGER_EMAIL).toString();
            }

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
            siteCreationRequestJson.put(SiteFieldConstants.INSTALLER_EMAIL, siteDict.get(CcuFieldConstants.INSTALLER_EMAIL));
            siteCreationRequestJson.put(SiteFieldConstants.BILLING_ADMIN_EMAIL, HStr.make(billingAdminEmail));
            siteCreationRequestJson.put(SiteFieldConstants.ORGANIZATION, siteDict.get(SiteFieldConstants.ORGANIZATION));
            siteCreationRequestJson.put(SiteFieldConstants.TIMEZONE, siteDict.get(SiteFieldConstants.TIMEZONE));
            siteCreationRequestJson.put(SiteFieldConstants.WEATHERREF, siteDict.get(SiteFieldConstants.WEATHERREF, false));

            if (!siteUpdate) {
                JSONObject tunerFiled = new JSONObject();
                tunerFiled.put(CcuFieldConstants.MODEL_ID, tunerEquip.get(CcuFieldConstants.SOURCE_MODEL));
                tunerFiled.put(CcuFieldConstants.MODEL_VERSION, tunerEquip.get(CcuFieldConstants.SOURCE_MODEL_VERSION));
                siteCreationRequestJson.put(CcuFieldConstants.TUNER, tunerFiled);
            }

        } catch (JSONException e) {
            e.printStackTrace();
            CcuLog.d(TAG, "Unable to sync site due to JSON exception. This is likely unrecoverable.");
            siteCreationRequestJson = null;
        }
        
        return siteCreationRequestJson;
    }
}
