package a75f.io.logic.pubnub;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;

/**
 * Created by mahesh on 11-02-2021.
 */
public class BuildingTunerUpdateHandler {

    public static void updateZoneModuleSystemPoints(String id) {

        //update dualDuct building tuners
        HashMap newTunerValueItem = CCUHsApi.getInstance().readMapById(id);
        String buildingTunerDis = newTunerValueItem.get("dis").toString();

        ArrayList<HashMap> dualDuctBuildingTuners = CCUHsApi.getInstance().readAll("tuner and tunerGroup and dualDuct");
        for (HashMap hashMap : dualDuctBuildingTuners) {
            String hashMapDis = hashMap.get("dis").toString();
            if (!newTunerValueItem.get("id").toString().equals(hashMap.get("id").toString()) && hashMapDis.contains("Building") && newTunerValueItem.get("tunerGroup").toString().equalsIgnoreCase(hashMap.get("tunerGroup").toString()) && buildingTunerDis.substring(buildingTunerDis.lastIndexOf("-") + 1).equalsIgnoreCase(hashMapDis.substring(hashMapDis.lastIndexOf("-") + 1))) {
                setTuner(hashMap.get("id").toString(), 16, getBuildingTunerValue(newTunerValueItem.get("id").toString()));
            }
        }

        //Update linked system tuner
        ArrayList<HashMap> systemTuners = CCUHsApi.getInstance().readAll("tuner and tunerGroup and system and roomRef == \"" + "SYSTEM" + "\"");
        for (HashMap systemTunersMap : systemTuners) {
            String systemTunerDis = systemTunersMap.get("dis").toString();
            if (!newTunerValueItem.get("id").toString().equals(systemTunersMap.get("id").toString()) && newTunerValueItem.get("tunerGroup").toString().equalsIgnoreCase(systemTunersMap.get("tunerGroup").toString()) && buildingTunerDis.substring(buildingTunerDis.lastIndexOf("-") + 1).equalsIgnoreCase(systemTunerDis.substring(systemTunerDis.lastIndexOf("-") + 1))) {
                setTuner(systemTunersMap.get("id").toString(), 16, getBuildingTunerValue(newTunerValueItem.get("id").toString()));
            }
        }

        //Update linked zone tuner
        ArrayList<Zone> zoneArrayList = new ArrayList<>();
        ArrayList<Equip> equipArrayList = new ArrayList<>();

        for (Floor f : HSUtil.getFloors()) {
            zoneArrayList.addAll(HSUtil.getZones(f.getId()));
        }

        for (Zone z : zoneArrayList) {
            equipArrayList.addAll(HSUtil.getEquips(z.getId()));
        }

        //Update linked module tuner
        for (Equip e : equipArrayList) {
            ArrayList<HashMap> moduleTuners = CCUHsApi.getInstance().readAll("tuner and equipRef == \"" + e.getId() + "\"");

            for (HashMap moduleTunerMap : moduleTuners) {
                if (!moduleTunerMap.get("roomRef").toString().equals("SYSTEM")) {
                    String moduleTunerDis = moduleTunerMap.get("dis").toString();
                    if (!newTunerValueItem.get("id").toString().equals(moduleTunerMap.get("id").toString()) && newTunerValueItem.get("tunerGroup").toString().equalsIgnoreCase(moduleTunerMap.get("tunerGroup").toString()) && buildingTunerDis.substring(buildingTunerDis.lastIndexOf("-") + 1).equalsIgnoreCase(moduleTunerDis.substring(moduleTunerDis.lastIndexOf("-") + 1))) {
                        setTuner(moduleTunerMap.get("id").toString(), 16, getBuildingTunerValue(newTunerValueItem.get("id").toString()));
                    }
                }
            }
        }
    }

    public static void setTuner(String id, int level, Double val) {

        new AsyncTask<String, Void, Void>() {
            @Override
            protected Void doInBackground(final String... params) {
                if (val != null) {
                    CCUHsApi.getInstance().writePointForCcuUser(id, level, val, 0);
                    CCUHsApi.getInstance().writeHisValById(id, val);
                }
                return null;
            }

            @Override
            protected void onPostExecute(final Void result) {
                // continue what you are doing...
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");
    }

    public static Double getBuildingTunerValue(String id) {
        CCUHsApi hayStack = CCUHsApi.getInstance();
        ArrayList values = hayStack.readPoint(id);
        if (values != null && values.size() > 0) {
            for (int l = 1; l <= values.size(); l++) {
                HashMap valMap = ((HashMap) values.get(l - 1));
                if (valMap.get("level") != null && valMap.get("level").toString().equals("16") && valMap.get("val") != null) {
                    return Double.parseDouble(valMap.get("val").toString());
                }
            }
        }
        return null;
    }
}
