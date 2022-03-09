package a75f.io.logic.migration.idupoints;

import android.util.Log;

import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logic.bo.building.vrv.VrvPoints;
import a75f.io.logic.util.PreferenceUtil;

public class IduPointsMigration {

    private static final String CCU_IDU_MIGRATION = "CCU_IDU_MIGRATION";

    public static void init() {
        new IduPointsMigration().checkForMigration();
    }

    private void checkForMigration(){
        if (!PreferenceUtil.isIduPointsMigrationDone()) {
            Log.i(CCU_IDU_MIGRATION,"idu migration started ");
            updateIduPoints(CCUHsApi.getInstance());
            PreferenceUtil.setIduMigrationStatus(true);
        }
    }

    private void updateIduPoints(CCUHsApi instance) {
        List<HashMap<Object,Object>> equips = instance.readAllEntities("equip and vrv and hyperstat");

        for(HashMap<Object,Object> a : equips){
            Equip vrvEquip = new Equip.Builder().setHashMap(a).build();
            Short nodeAddr = Short.valueOf(vrvEquip.getGroup());
            String roomRef = vrvEquip.getRoomRef();
            String floorRef = vrvEquip.getFloorRef();
            VrvPoints.Companion.createThermisterPoints(vrvEquip,roomRef,floorRef,nodeAddr,instance);
            VrvPoints.Companion.createTelecoCheckPoint(vrvEquip,roomRef,floorRef,nodeAddr,instance);
            VrvPoints.Companion.createTestOperationPoint(vrvEquip,roomRef,floorRef,nodeAddr,instance);
        }
        instance.syncEntityTree();

    }
}
