package a75f.io.logic.tuners;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.Tags;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;

import static a75f.io.logic.tuners.TunerConstants.DEFAULT_MODE_CHANGEOVER_HYSTERESIS;
import static a75f.io.logic.tuners.TunerConstants.DEFAULT_STAGE_DOWN_TIMER_COUNTER;
import static a75f.io.logic.tuners.TunerConstants.DEFAULT_STAGE_UP_TIMER_COUNTER;

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
        if (tuner != null && tuner.size() > 0) {
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
                          .addMarker("equip").addMarker("tuner").addMarker("his")
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
        checkForTunerMigration();
        //TunerUpgrades.handleTunerUpgrades(CCUHsApi.getInstance());
    }

    private void checkForTunerMigration() {
        PackageManager manager = Globals.getInstance().getApplicationContext().getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(Globals.getInstance().getApplicationContext().getPackageName(), 0);
            String tunerVersion = info.versionName + "." + info.versionCode;

            if (!CCUHsApi.getInstance().getTunerVersion().equals(tunerVersion)) {
                doTunerMigrationJob();
                TunerUpgrades.handleTunerUpgrades(CCUHsApi.getInstance());
                CCUHsApi.getInstance().setTunerVersion(tunerVersion);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void doTunerMigrationJob() {
        ArrayList<HashMap> tunersList = CCUHsApi.getInstance().readAll("tuner and tunerGroup");

        for (HashMap tunerMap : tunersList) {
            String tunerMapDis = tunerMap.get("dis").toString();
            String tunerMapShortDis = tunerMapDis.substring(tunerMapDis.lastIndexOf("-") + 1).trim();
            TunerMigration.migrateTunerIfRequired(tunerMap.get("id").toString(), tunerMapShortDis);
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
    }
}
