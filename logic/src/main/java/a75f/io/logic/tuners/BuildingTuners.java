package a75f.io.logic.tuners;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.util.PreferenceUtil;

/**
 * Created by samjithsadasivan on 10/5/18.
 */

public class BuildingTuners
{
    
    private String equipRef;
    private String equipDis;
    private String siteRef;
    private String tz;
    CCUHsApi hayStack;
    
    private static BuildingTuners instance = null;
    private BuildingTuners(){
        addBuildingTunerEquip();
    }
    
    public static BuildingTuners getInstance() {
        if (instance == null) {
            instance = new BuildingTuners();
        }
        return instance;
    }
    
    public void addBuildingTunerEquip() {
        hayStack = CCUHsApi.getInstance();
        HashMap tuner = CCUHsApi.getInstance().read("equip and tuner");
        if (tuner != null && !tuner.isEmpty()) {
            equipRef = tuner.get("id").toString();
            equipDis = tuner.get("dis").toString();
            HashMap siteMap = hayStack.read(Tags.SITE);
            siteRef = siteMap.get(Tags.ID).toString();
            tz = siteMap.get("tz").toString();
            CcuLog.d(L.TAG_CCU_SYSTEM,"BuildingTuner equip already present");
            return;
        }
        CcuLog.d(L.TAG_CCU_SYSTEM,"BuildingTuner Equip does not exist. Create Now");
        HashMap siteMap = hayStack.read(Tags.SITE);
        siteRef = siteMap.get(Tags.ID).toString();
        String siteDis = siteMap.get("dis").toString();
        Equip tunerEquip= new Equip.Builder()
                          .setSiteRef(siteRef)
                          .setDisplayName(siteDis+"-BuildingTuner")
                          .addMarker("equip").addMarker("tuner")
                          .setTz(siteMap.get("tz").toString())
                          .build();
        equipRef = hayStack.addEquip(tunerEquip);
        equipDis = siteDis+"-BuildingTuner";
        tz = siteMap.get("tz").toString();

        addDefaultBuildingTuners();
        CCUHsApi.getInstance().syncEntityTree();
    }
    
    /**
     * All the new tuners with are being added here.
     * This should be done neatly.
     */
    public void updateBuildingTuners() {
        TITuners.addDefaultTiTuners(hayStack, siteRef, equipRef, equipDis, tz);
        DualDuctTuners.addDefaultTuners(hayStack, siteRef, equipRef, equipDis, tz);
        OAOTuners.updateNewTuners(hayStack, siteRef,equipRef, equipDis,tz,false);
        VrvTuners.addDefaultVrvTuners(hayStack, siteRef,equipRef, equipDis,tz);
        checkForTunerMigration();
    }

    private void checkForTunerMigration() {
        CcuLog.i(L.TAG_CCU_TUNER, "checkForTunerMigration: ");
        PackageManager manager = Globals.getInstance().getApplicationContext().getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(Globals.getInstance().getApplicationContext().getPackageName(), 0);
            String tunerVersion = info.versionName + "." + info.versionCode;

            CcuLog.i(L.TAG_CCU_TUNER, " PreferenceUtil.getTunerVersion(): "+ PreferenceUtil.getTunerVersion()+
                    " Version "+tunerVersion);
            if (!PreferenceUtil.getTunerVersion().equals(tunerVersion)) {
                TunerUpgrades.handleTunerUpgrades(CCUHsApi.getInstance());
                PreferenceUtil.setTunerVersion(tunerVersion);
            }
        } catch (PackageManager.NameNotFoundException e) {
            CcuLog.e(L.TAG_CCU_TUNER, "checkForTunerMigration: "+e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    public void addDefaultBuildingTuners() {
    
        GenericTuners.addDefaultGenericTuners(hayStack, siteRef, equipRef, equipDis, tz);
        VavTuners.addDefaultVavTuners(hayStack, siteRef, equipRef, equipDis, tz);
        PlcTuners.addDefaultPlcTuners(hayStack, siteRef, equipRef, equipDis, tz);
        StandAloneTuners.addDefaultStandaloneTuners(hayStack, siteRef, equipRef, equipDis, tz);
        DabTuners.addDefaultDabTuners(hayStack, siteRef, equipRef, equipDis, tz);
        TITuners.addDefaultTiTuners(hayStack, siteRef, equipRef, equipDis, tz);
        OAOTuners.addDefaultTuners(hayStack, siteRef, equipRef, equipDis, tz);
        DualDuctTuners.addDefaultTuners(hayStack, siteRef, equipRef, equipDis, tz);
        AlertTuners.addDefaultAlertTuners(hayStack, siteRef, equipRef, equipDis, tz);
        TemperatureLimitTuners.addDefaultTempLimitTuners(hayStack, siteRef, equipRef, equipDis, tz);
        TimerTuners.addDefaultTimerTuners(hayStack, siteRef, equipRef, equipDis, tz);
        OTNTuners.addDefaultOTNTuners(hayStack, siteRef, equipRef, equipDis, tz);
        HyperstatCpuTuners.Companion.addHyperstatDefaultTuners(hayStack, siteRef, equipRef, equipDis, tz);
        Equip buildingTunerEquip = new Equip.Builder().setHashMap(hayStack.readEntity("equip and tuner")).build();
        DabReheatTunersKt.createDefaultReheatTuners(hayStack, buildingTunerEquip);
        HyperStat2PipeTuners.Companion.addPipe2BuildingTuner(hayStack, siteRef, equipRef, equipDis, tz);
    }
}
