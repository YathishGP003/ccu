package a75f.io.logic.migration.firmware;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Tags;
import a75f.io.logic.bo.building.firmware.FirmwareVersion;
import a75f.io.logic.util.PreferenceUtil;

public class FirmwareVersionPointMigration {
    private final static String CCU_FIRMWARE_VERSION_MIGRATION = "CCU_FIRMWARE_VERSION_MIGRATION";

    public static void initFirmwareVersionPointMigration() {
        new FirmwareVersionPointMigration().checkForFirmwarePointMigration();
    }

    private void checkForFirmwarePointMigration(){
        if (!PreferenceUtil.isFirmwareVersionPointMigrationDone()) {
            Log.i(CCU_FIRMWARE_VERSION_MIGRATION,"Firmware version point migration started ");
            upgradeEquipsWithFirmwareVersionPoints(CCUHsApi.getInstance());
            PreferenceUtil.setFirmwareVersionPointMigrationStatus(true);
        }
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
            HashMap firmwarePoint =
                    hayStack.read("point and physical and firmware and version and deviceRef == \"" + deviceInfo.getId() + "\"");
            if(firmwarePoint.isEmpty() && deviceType.equals("cm")){
               hayStack.addPoint(FirmwareVersion.getFirmwareVersion(site.getDisplayName()+"-CM-"+"firmwareVersion",
                        deviceInfo.getId(), deviceInfo.getSiteRef(), site.getTz()));
            }
            else if(firmwarePoint.isEmpty() && !deviceType.equals("cm")){
                hayStack.addPoint(FirmwareVersion.getFirmwareVersion(Integer.parseInt(deviceInfo.getAddr()), deviceInfo.getId(),
                        site.getId(), deviceInfo.getFloorRef(), deviceInfo.getRoomRef(), site.getTz()));

            }


        }

    }
}
