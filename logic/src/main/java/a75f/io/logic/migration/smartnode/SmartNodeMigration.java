package a75f.io.logic.migration.smartnode;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.util.PreferenceUtil;

public class SmartNodeMigration {

    private static final String CCU_SN_MIGRATION = "CCU_SN_MIGRATION";

    public static void init() {
        new SmartNodeMigration().checkForMigration();
    }

    private void checkForMigration() {
        if (!PreferenceUtil.getSNMigration()) {
            Log.i(CCU_SN_MIGRATION,"SN migration started ");
            updateSNPoints(CCUHsApi.getInstance());
            PreferenceUtil.setSmartNodeMigration();
        }
    }

    private void updateSNPoints(CCUHsApi instance) {
        ArrayList<HashMap<Object, Object>> smartNodeDevices = instance.readAllEntities
                ("device and smartnode");
        if (smartNodeDevices.size() > 0) {
            for (HashMap<Object, Object> sndevice : smartNodeDevices) {
                HashMap<Object, Object> snEquip = instance.readEntity("equip and group" +
                        " == \"" + sndevice.get("addr") + "\"");
                if (snEquip != null) {
                    Equip snPoint =
                            new Equip.Builder().setHashMap(snEquip).addMarker("smartnode").build();
                    CcuLog.d(L.TAG_CCU_SYSTEM, "updateSNPoint successfull ");
                    CCUHsApi.getInstance().updateEquip(snPoint, snEquip.get("id").toString());
                }
            }
        }
    }
}
