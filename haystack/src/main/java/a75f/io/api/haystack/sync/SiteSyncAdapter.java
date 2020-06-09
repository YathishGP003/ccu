package a75f.io.api.haystack.sync;

import a75f.io.api.haystack.BuildConfig;
import a75f.io.api.haystack.Site;
import a75f.io.constants.HttpConstants;
import a75f.io.constants.SiteFieldConstants;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
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

public class SiteSyncAdapter extends EntitySyncAdapter {

    private static final String LOG_PREFIX = "CCU_HS_SITESYNC";

    @Override
    public synchronized boolean onSync() {
        CcuLog.i(LOG_PREFIX, "onSync Site");

        boolean synced = false;

        HDict sDict =  CCUHsApi.getInstance().readHDict("site");
        HDictBuilder b = new HDictBuilder().add(sDict);
        String siteLuid = b.get(SiteFieldConstants.ID).toString();
        String siteGuid = CCUHsApi.getInstance().getGUID(siteLuid);

        if (StringUtils.isBlank(siteGuid)) {
            synced = siteCreationSync(sDict);
        } else {
            synced = siteUpdateSync(sDict);
        }

        CcuLog.i(LOG_PREFIX, "<- doSyncSite");
        return synced;
    }

    private boolean siteCreationSync(HDict siteDict) {
        boolean synced = false;

        HDictBuilder b = new HDictBuilder().add(siteDict);
        String siteLuid = b.get(SiteFieldConstants.ID).toString();
        JSONObject siteCreationRequestJson = getSiteJsonRequest(siteDict);
        String siteGuid = null;

        if (siteCreationRequestJson != null) {
            try {
                String response = HttpUtil.executeJson(
                        CCUHsApi.getInstance().getAuthenticationUrl() + "sites",
                        siteCreationRequestJson.toString(),
                        BuildConfig.CARETAKER_API_KEY,
                        true, // TODO Matt Rudd - Hate to do this, but the HttpUtils are a mess
                        HttpConstants.HTTP_METHOD_POST
                );

                if (response != null) {
                    JSONObject siteCreationResponseJson = new JSONObject(response);
                    siteGuid = siteCreationResponseJson.getString("id");

                    if (StringUtils.isNotBlank(siteGuid)) {
                        CCUHsApi.getInstance().putUIDMap(siteLuid, siteGuid);
                        synced = true;
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
                CcuLog.d(LOG_PREFIX, "Unable to sync site due to JSON exception. This is likely unrecoverable.");
            }
        }
        return synced;
    }

    private boolean siteUpdateSync(HDict siteDict) {
        boolean synced = false;

        HDictBuilder b = new HDictBuilder().add(siteDict);
        String siteLuid = b.get("id").toString();
        JSONObject siteCreationRequestJson = getSiteJsonRequest(siteDict);
        String existingSiteGuid = CCUHsApi.getInstance().getGUID(siteLuid);

        if (siteCreationRequestJson != null) {
            try {
                String response = HttpUtil.executeJson(
                        CCUHsApi.getInstance().getAuthenticationUrl() + "sites/" + existingSiteGuid,
                        siteCreationRequestJson.toString(),
                        BuildConfig.CARETAKER_API_KEY,
                        true, // TODO Matt Rudd - Hate to do this, but the HttpUtils are a mess
                        HttpConstants.HTTP_METHOD_PUT
                );

                if (response != null) {
                    JSONObject siteCreationResponseJson = new JSONObject(response);
                    String siteGuidFromResponse = siteCreationResponseJson.getString("id");

                    if (StringUtils.isNotBlank(siteGuidFromResponse) && StringUtils.equals(siteGuidFromResponse, existingSiteGuid)) {
                        synced = true;
                    }

                }
            } catch (JSONException e) {
                e.printStackTrace();
                CcuLog.d(LOG_PREFIX, "Unable to sync site due to JSON exception. This is likely unrecoverable.");
            }
        }
        return synced;
    }

    private JSONObject getSiteJsonRequest(HDict siteDict) {
        JSONObject siteCreationRequestJson = new JSONObject();

        try {
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
            CcuLog.d(LOG_PREFIX, "Unable to sync site due to JSON exception. This is likely unrecoverable.");
            siteCreationRequestJson = null;
        }

        return siteCreationRequestJson;
    }
}
