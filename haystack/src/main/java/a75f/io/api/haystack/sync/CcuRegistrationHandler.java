package a75f.io.api.haystack.sync;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Objects;

import a75f.io.api.haystack.BuildConfig;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.constants.CcuFieldConstants;
import a75f.io.constants.HttpConstants;
import a75f.io.logger.CcuLog;

class CcuRegistrationHandler {
    public static final String TAG = "CCU_HS_CcuSyncHandler";
    
    public boolean doSync() {
        HashMap ccu = CCUHsApi.getInstance().read("device and ccu");
        
        String id = Objects.toString(ccu.get(CcuFieldConstants.ID), "");
    
        if (!CCUHsApi.getInstance().isEligibleForSync(id)) {
            CcuLog.d(TAG, "CcuRegistrationHandler CCU Sync not required "+id);
            return true;
        }
    
        HashMap site = CCUHsApi.getInstance().read("site");
    
        String siteRef = site.get("id").toString();
    
        String dis = Objects.toString(ccu.get(CcuFieldConstants.DESCRIPTION),"");
    
        String ahuRef = Objects.toString(ccu.get(CcuFieldConstants.AHUREF),"");
    
        String gatewayRef = Objects.toString(ccu.get(CcuFieldConstants.GATEWAYREF),"");
    
        String equipRef = Objects.toString(ccu.get(CcuFieldConstants.EQUIPREF),"");
    
        String facilityManagerEmail = Objects.toString(ccu.get(CcuFieldConstants.FACILITY_MANAGER_EMAIL));
        String installerEmail = Objects.toString(ccu.get(CcuFieldConstants.INSTALLER_EMAIL));
    
        if (StringUtils.isNotBlank(id)
            && StringUtils.isNotBlank(dis)
            && StringUtils.isNotBlank(ahuRef)
            && StringUtils.isNotBlank(gatewayRef)
            && StringUtils.isNotBlank(equipRef)
            && StringUtils.isNotBlank(facilityManagerEmail)
            && StringUtils.isNotBlank(installerEmail)) {
        
            JSONObject ccuUpdateJson = CCUHsApi.getInstance().getCcuRegisterJson(
                id,
                siteRef,
                dis,
                ahuRef,
                gatewayRef,
                equipRef,
                ccu.get(CcuFieldConstants.FACILITY_MANAGER_EMAIL).toString(),
                ccu.get(CcuFieldConstants.INSTALLER_EMAIL).toString()
            );
        
            if (ccuUpdateJson != null) {
                // TODO Matt Rudd - Add the ability to call put in order to sync the CCU
                CcuLog.d(TAG, "Attempting to sync the CCU information");
                String ccuRegistrationResponse = HttpUtil.executeJson(
                    CCUHsApi.getInstance().getAuthenticationUrl()+"devices/" + id,
                    ccuUpdateJson.toString(),
                    BuildConfig.CARETAKER_API_KEY,
                    true, // TODO Matt Rudd - Hate to do this, but the HttpUtils are a mess
                    HttpConstants.HTTP_METHOD_PUT
                );
            
                if (StringUtils.isNotBlank(ccuRegistrationResponse)) {
                    CCUHsApi.getInstance().setEntitySynced(id);
                } else {
                    return false;
                }
            }
        }
        
        return true;
    }
}
