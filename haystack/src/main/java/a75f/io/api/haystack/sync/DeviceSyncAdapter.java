package a75f.io.api.haystack.sync;

import a75f.io.api.haystack.BuildConfig;
import a75f.io.constants.CcuFieldConstants;
import a75f.io.constants.DeviceFieldConstants;
import a75f.io.constants.HttpConstants;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.logger.CcuLog;

public class DeviceSyncAdapter extends EntitySyncAdapter {

    @Override
    public synchronized boolean onSync() {
        CcuLog.i("CCU_HS_SYNC", "onSync Devices");

        boolean synced = false;

        if (CCUHsApi.getInstance().isCCURegistered()) {
            String siteRef = CCUHsApi.getInstance().getGlobalSiteId();

            if (StringUtils.isNotBlank(siteRef)) {
                synced = syncCcuDevice(siteRef);
                synced = syncOtherDevices(siteRef);
            }
        }

        return synced;
    }

    private boolean syncCcuDevice(String siteRef) {
        boolean synced = false;

        if (StringUtils.isBlank(siteRef)) {
            return synced;
        }

        HashMap ccu = CCUHsApi.getInstance().read("device and ccu");

        String id = CCUHsApi.getInstance().getGUID(
                Objects.toString(ccu.get(CcuFieldConstants.ID),"")
        );

        String dis = Objects.toString(ccu.get(CcuFieldConstants.DESCRIPTION),"");

        String ahuRef = CCUHsApi.getInstance().getGUID(
                Objects.toString(ccu.get(CcuFieldConstants.AHUREF),"")
        );

        String gatewayRef = CCUHsApi.getInstance().getGUID(
                Objects.toString(ccu.get(CcuFieldConstants.GATEWAYREF),"")
        );

        String equipRef = CCUHsApi.getInstance().getGUID(
                Objects.toString(ccu.get(CcuFieldConstants.EQUIPREF),"")
        );

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
                CcuLog.d("CCU_DEVICE_SYNC", "Attempting to sync the CCU information");
                String ccuRegistrationResponse = HttpUtil.executeJson(
                        CCUHsApi.getInstance().getAuthenticationUrl()+"devices/" + id,
                        ccuUpdateJson.toString(),
                        BuildConfig.CARETAKER_API_KEY,
                        true, // TODO Matt Rudd - Hate to do this, but the HttpUtils are a mess
                        HttpConstants.HTTP_METHOD_PUT
                );

                if (StringUtils.isNotBlank(ccuRegistrationResponse)) {
                    synced = true;
                }
            }
        }

        return synced;
    }

    // TODO Matt Rudd - This is a candidate for a major refactor
    private boolean syncOtherDevices(String siteRef) {
        boolean synced = false;

        if (StringUtils.isBlank(siteRef)) {
            return synced;
        }

        List<HashMap> nonCcuDeviceList = CCUHsApi.getInstance().readAll("device and not ccu");
        ArrayList<String> deviceLUIDList = new ArrayList();
        ArrayList<HDict> entities = new ArrayList<>();
        CcuLog.d("CCU_DEVICE_SYNC", "Found " + nonCcuDeviceList.size() + " non-CCU devices");

        for (Map nonCcuDevice : nonCcuDeviceList) {

            String luid = nonCcuDevice.remove(DeviceFieldConstants.ID).toString();
            String deviceId = CCUHsApi.getInstance().getGUID(luid);
            if (StringUtils.isBlank(deviceId)) {
                deviceLUIDList.add(luid);
                nonCcuDevice.put(DeviceFieldConstants.SITEREF, HRef.copy(siteRef));

                if (nonCcuDevice.get(DeviceFieldConstants.FLOORREF) != null && !nonCcuDevice.get(DeviceFieldConstants.FLOORREF).toString().equals("SYSTEM")) {
                    String guid = CCUHsApi.getInstance().getGUID(nonCcuDevice.get(DeviceFieldConstants.FLOORREF).toString());
                    CcuLog.d("CCU_DEVICE_SYNC", "Non-CCU device sync with LUID " + luid + "; GUID" + deviceId + "siteRef: " + siteRef + "; description " + nonCcuDevice.get("dis").toString() + "; floorRef: " + guid);
                    if(guid == null) {
                        return false;
                    }
                    nonCcuDevice.put(DeviceFieldConstants.FLOORREF, HRef.copy(guid));
                }

                if (nonCcuDevice.get(DeviceFieldConstants.ROOMREF) != null && !nonCcuDevice.get(DeviceFieldConstants.ROOMREF).toString().equals("SYSTEM")) {
                    String guid = CCUHsApi.getInstance().getGUID(nonCcuDevice.get(DeviceFieldConstants.ROOMREF).toString());
                    CcuLog.d("CCU_DEVICE_SYNC", "Non-CCU device sync with LUID " + luid + "; GUID" + deviceId + "siteRef: " + siteRef + "; description " + nonCcuDevice.get("dis").toString() + "; roomRef: " + guid);
                    if(guid == null) {
                        return false;
                    }
                    nonCcuDevice.put(DeviceFieldConstants.ROOMREF, HRef.copy(guid));
                }

                if (nonCcuDevice.get(DeviceFieldConstants.EQUIPREF) != null) {
                    String guid = CCUHsApi.getInstance().getGUID(nonCcuDevice.get(DeviceFieldConstants.EQUIPREF).toString());
                    CcuLog.d("CCU_DEVICE_SYNC", "Non-CCU device sync with LUID " + luid + "; GUID" + deviceId + "siteRef: " + siteRef + "; description " + nonCcuDevice.get("dis").toString() + "; equipRef: " + guid);
                    if(guid == null) {
                        return false;
                    }
                    nonCcuDevice.put(DeviceFieldConstants.EQUIPREF, HRef.copy(guid));
                }
                CcuLog.i("CCU_DEVICE_SYNC", "Adding the device with GUID " + deviceId + " to be synced: " + nonCcuDevice.toString());
                entities.add(HSUtil.mapToHDict(nonCcuDevice));
            }
        }

        if (deviceLUIDList.isEmpty()) {
            synced = true;
        }

        CcuLog.d("CCU_DEVICE_SYNC", "Found " + deviceLUIDList.size() + " devices that are eligible for syncing");

        if (!deviceLUIDList.isEmpty()) {
            HGrid grid = HGridBuilder.dictsToGrid(entities.toArray(new HDict[entities.size()]));
            String response = HttpUtil.executePost(
                    CCUHsApi.getInstance().getHSUrl() + "addEntity",
                    HZincWriter.gridToString(grid),
                    CCUHsApi.getInstance().getJwt()
            );
            CcuLog.i("CCU_DEVICE_SYNC", "Response: \n" + response);

            if (response == null) {
                return false;
            }
            HZincReader zReader = new HZincReader(response);
            Iterator it = zReader.readGrid().iterator();
            int index = 0;

            while (it.hasNext())
            {
                HRow row = (HRow) it.next();
                String deviceGUID = row.get(DeviceFieldConstants.ID).toString();
                if (StringUtils.isNotBlank(deviceGUID)) {
                    CCUHsApi.getInstance().putUIDMap(deviceLUIDList.get(index++), deviceGUID);
                } else {
                    return false;
                }
            }
        }

        return synced;
    }

}
