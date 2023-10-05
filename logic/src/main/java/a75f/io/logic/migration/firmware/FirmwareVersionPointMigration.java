package a75f.io.logic.migration.firmware;

import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.projecthaystack.io.HZincReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.bo.building.firmware.FirmwareVersion;
import a75f.io.logic.util.PreferenceUtil;

public class FirmwareVersionPointMigration {
    private final static String CCU_FIRMWARE_VERSION_MIGRATION = "CCU_FIRMWARE_VERSION_MIGRATION";

    public static boolean initFirmwareVersionPointMigration() {
        if (!CCUHsApi.getInstance().readEntity("site").isEmpty()) {
            return new FirmwareVersionPointMigration().checkForFirmwarePointMigration();
        }
        return true;
    }

    public static boolean initRemoteFirmwareVersionPointMigration() {
        if (!CCUHsApi.getInstance().readEntity("site").isEmpty()) {
            return new FirmwareVersionPointMigration().clearRemoteDuplicateFirmwarePoints();
        }
        return true;
    }

    private boolean checkForFirmwarePointMigration(){
        if (!PreferenceUtil.isFirmwareVersionPointMigrationDone()) {
            Log.i(CCU_FIRMWARE_VERSION_MIGRATION,"Firmware version point migration started ");
            upgradeEquipsWithFirmwareVersionPoints(CCUHsApi.getInstance());
        }
        return true;
    }

    private void upgradeEquipsWithFirmwareVersionPoints(CCUHsApi hayStack){
        String[] deviceTypes = {"smartnode", "smartstat", "hyperstat", "cm"};
        for(String deviceType : deviceTypes){
            addFirmwareVersionToDevice(deviceType, hayStack);
        }
    }

    private void addFirmwareVersionToDevice(String deviceType, CCUHsApi hayStack){
        ArrayList<HashMap> deviceList= hayStack.readAll("device and "+deviceType);
        for(HashMap device : deviceList){
            Device deviceInfo = new Device.Builder().setHashMap(device).build();
            Site site = new Site.Builder().setHashMap(CCUHsApi.getInstance().read(Tags.SITE)).build();
            List<HashMap<Object, Object>> firmwarePointList =
                    hayStack.readAllEntities("point and physical and firmware and version and deviceRef == \"" +
                            deviceInfo.getId() + "\"");
            if(firmwarePointList.size() > 1) { // This says there are duplicate points, hence deleting all firmware
                // points and recreate
                for (HashMap<Object, Object> firmwarePoint : firmwarePointList) {
                    hayStack.deleteEntityTree(firmwarePoint.get("id").toString());
                }
            }
            //if firmware version point is one and kind is string, do nothing
            else if(firmwarePointList.size() == 1 &&
                    firmwarePointList.get(0).get("kind").toString().equalsIgnoreCase("Str")){
                continue;
            }
            //if firmware version point is one and kind is Number, delete the point and recreate
            else if(firmwarePointList.size() == 1 &&
                    firmwarePointList.get(0).get("kind").toString().equalsIgnoreCase("Number")){
                hayStack.deleteEntityTree(firmwarePointList.get(0).toString());
            }

            if(deviceType.equals("cm")){
               hayStack.addPoint(FirmwareVersion.getFirmwareVersion(site.getDisplayName()+"-CM-"+"firmwareVersion",
                        deviceInfo.getId(), deviceInfo.getSiteRef(), site.getTz()));
            }
            else {
                hayStack.addPoint(FirmwareVersion.getFirmwareVersion(Integer.parseInt(deviceInfo.getAddr()), deviceInfo.getId(),
                        site.getId(), deviceInfo.getFloorRef(), deviceInfo.getRoomRef(), site.getTz()));

            }
        }
    }
    private boolean clearRemoteDuplicateFirmwarePoints(){
        if(PreferenceUtil.isFirmwareVersionPointMigrationDone()){
            return true;
        }
        try {
            CcuLog.d(CCU_FIRMWARE_VERSION_MIGRATION,
                    "Allowing data to get saved locally.... ");
            Thread.sleep(1000 * 30);
        } catch (InterruptedException e) {
            CcuLog.d(CCU_FIRMWARE_VERSION_MIGRATION,
                    "InterruptedException occurred while pausing firmware migration to sync data.... ");
        }
        List<Boolean> failedCount = new ArrayList<>();
        CCUHsApi ccuHsApi = CCUHsApi.getInstance();
        String[] deviceTypes = {"smartnode", "smartstat", "hyperstat", "cm"};
        for(String deviceType : deviceTypes) {
            ArrayList<HashMap<Object, Object>> deviceList = ccuHsApi.readAllEntities("device and " + deviceType);
            for (HashMap device : deviceList) {
                Device deviceInfo = new Device.Builder().setHashMap(device).build();
                HashMap<Object, Object> firmwarePoint =
                        ccuHsApi.readEntity("point and physical and firmware and version and deviceRef == \"" +
                                deviceInfo.getId() + "\"");
                String firmwareVersionPointId = StringUtils.prependIfMissing(firmwarePoint.get("id").toString(), "@");
                String query = "point and physical and firmware and " +
                        "deviceRef == " + StringUtils.prependIfMissing(deviceInfo.getId(), "@");
                String response = ccuHsApi.fetchRemoteEntityByQuery(query);
                if(response == null || response.isEmpty()){
                    CcuLog.d(CCU_FIRMWARE_VERSION_MIGRATION,
                            "Failed to read remote entity for the read query : " + query);
                    return false;
                }
                List<String> duplicateFirmwareVersionRemotePointIds = new ArrayList<>();
                List<HashMap> firmwareVersionRemotePointMapList =
                        ccuHsApi.HGridToList(new HZincReader(response).readGrid());
                for(HashMap firmwareVersionRemotePointMap : firmwareVersionRemotePointMapList){
                    String firmwareVersionRemotePointId =
                            StringUtils.prependIfMissing(firmwareVersionRemotePointMap.get("id").toString(), "@");
                    if(!firmwareVersionRemotePointId.equals(firmwareVersionPointId)){
                        duplicateFirmwareVersionRemotePointIds.add(firmwareVersionRemotePointId);
                        CcuLog.d(CCU_FIRMWARE_VERSION_MIGRATION,
                                "Duplicate firmware version point found in Silo with the ID  : "+
                                        firmwareVersionRemotePointId+ " for the device : "+ deviceInfo.getDisplayName());
                    }
                }
                if(duplicateFirmwareVersionRemotePointIds.size() > 0){
                    if(!deleteRemoteFirmwareVersionPoints(duplicateFirmwareVersionRemotePointIds, ccuHsApi)){
                        failedCount.add(false);
                    }
                }
                else{
                    CcuLog.d(CCU_FIRMWARE_VERSION_MIGRATION,
                            "No duplicate firmware version point found in Silo with for the device : "
                                    + deviceInfo.getDisplayName());
                }
            }
        }
        if(failedCount.size() == 0){
            Log.i(CCU_FIRMWARE_VERSION_MIGRATION,"Firmware version point migration completed ");
        }
        return failedCount.size() == 0;
    }
    private boolean deleteRemoteFirmwareVersionPoints(List<String> duplicateFirmwareVersionRemotePointIds,
                                                      CCUHsApi ccuHsApi){
        for(String duplicateFirmwareVersionRemotePointId : duplicateFirmwareVersionRemotePointIds){
            String response = ccuHsApi.deleteRemoteEntity(duplicateFirmwareVersionRemotePointId);
            if(response == null || response.isEmpty()){
                CcuLog.d(CCU_FIRMWARE_VERSION_MIGRATION, "Failed to delete remote entity id : " + duplicateFirmwareVersionRemotePointId);
                return false;
            }
            CcuLog.d(CCU_FIRMWARE_VERSION_MIGRATION, "Remote entity id : " + duplicateFirmwareVersionRemotePointId +
                    " got deleted successfully");
        }
        return true;
    }
}
