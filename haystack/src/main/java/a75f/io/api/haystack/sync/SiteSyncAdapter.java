package a75f.io.api.haystack.sync;

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

    private static final String ID_FIELD = "id";
    private static final String TIMEZONE_FIELD = "tz";
    private static final String DESCRIPTION_FIELD = "dis";
    private static final String ORGANIZATION_FIELD = "organization";
    private static final String GEOADDRESS_FIELD = "geoAddr";
    private static final String GEOCITY_FIELD = "geoCity";
    private static final String GEOSTATE_FIELD = "geoState";
    private static final String GEOCOUNTRY_FIELD = "geoCountry";
    private static final String GEOPOSTALCODE_FIELD = "geoPostalCode";
    private static final String GEOCOORDINATES_FIELD = "geoCoord";
    private static final String AREA_FIELD = "area";

    @Override
    public boolean onSync() {
        CcuLog.i(LOG_PREFIX, "onSync Site");

        boolean synced = false;

        HDict sDict =  CCUHsApi.getInstance().readHDict("site");
        HDictBuilder b = new HDictBuilder().add(sDict);
        String siteLuid = b.get("id").toString();
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
        String siteLuid = b.get("id").toString();
        JSONObject siteCreationRequestJson = getSiteJsonRequest(siteDict);
        String siteGuid = null;

        if (siteCreationRequestJson != null) {
            try {
                // TODO Matt Rudd - Need to add the api-key header and make the HTTP util support it
                String response = HttpUtil.executePost(CCUHsApi.getInstance().getAuthenticationUrl() + "sites", siteCreationRequestJson.toString());

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
                // TODO Matt Rudd - Need to add the api-key header and make the HTTP util support it; this needs to be a PUT
                String response = HttpUtil.executePost(CCUHsApi.getInstance().getAuthenticationUrl() + "sites", siteCreationRequestJson.toString());

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
            siteCreationRequestJson.put(ID_FIELD, siteDict.get(ID_FIELD));
            siteCreationRequestJson.put(TIMEZONE_FIELD, siteDict.get(TIMEZONE_FIELD));
            siteCreationRequestJson.put(DESCRIPTION_FIELD, siteDict.dis());
            siteCreationRequestJson.put(ORGANIZATION_FIELD, siteDict.get(ORGANIZATION_FIELD));
            siteCreationRequestJson.put(GEOADDRESS_FIELD, siteDict.get(GEOADDRESS_FIELD));
            siteCreationRequestJson.put(GEOCITY_FIELD, siteDict.get(GEOCITY_FIELD));
            siteCreationRequestJson.put(GEOSTATE_FIELD, siteDict.get(GEOSTATE_FIELD));
            siteCreationRequestJson.put(GEOCOUNTRY_FIELD, siteDict.get(GEOCOUNTRY_FIELD));
            siteCreationRequestJson.put(GEOPOSTALCODE_FIELD, siteDict.get(GEOPOSTALCODE_FIELD));
            siteCreationRequestJson.put(GEOCOORDINATES_FIELD, siteDict.get(GEOCOORDINATES_FIELD));
            siteCreationRequestJson.put(AREA_FIELD, siteDict.get(AREA_FIELD));
        } catch (JSONException e) {
            e.printStackTrace();
            CcuLog.d(LOG_PREFIX, "Unable to sync site due to JSON exception. This is likely unrecoverable.");
            siteCreationRequestJson = null;
        }

        return siteCreationRequestJson;
    }
}
