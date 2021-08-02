package a75f.io.logic.bo.building.bpos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.dab.DabSystemController;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;


public class BPOSProfile extends ZoneProfile {
    BPOSEquip mBPOSEquip;


    public void addBPOSEquip(ProfileType type, int node, BPOSConfiguration config,
                             String floorRef, String roomRef) {
        mBPOSEquip = new BPOSEquip(type, node);
        mBPOSEquip.createEntities(config, floorRef, roomRef);
        mBPOSEquip.init();
    }

    public void addBPOSEquip(int node) {
        mBPOSEquip = new BPOSEquip(ProfileType.BPOS, node);
        mBPOSEquip.init();
    }

    public void updateBPOS(ProfileType type, int node, BPOSConfiguration config, String floorRef,
                           String roomRef) {
        mBPOSEquip.update(type, node, config, floorRef, roomRef);
        mBPOSEquip.init();
    }

    @Override
    public void updateZonePoints() {
        if (isZoneDead()) {
            state = TEMPDEAD;
            String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and " +
                    "message and writable and group == \"" + mBPOSEquip.mNodeAddr + "\"");
            if (!curStatus.equals("Zone Temp Dead")) {
                CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and" +
                        " group == \"" +  mBPOSEquip.mNodeAddr+ "\"", "Zone Temp Dead");
            }
            CCUHsApi.getInstance().writeHisValByQuery("point and status and his and group == \"" +
                    mBPOSEquip.mNodeAddr + "\"", (double) TEMPDEAD.ordinal());
            return;
        }
        String zoneId = HSUtil.getZoneIdFromEquipId(mBPOSEquip.mEquipRef);
        Occupied occ = ScheduleProcessJob.getOccupiedModeCache(zoneId);
        boolean occupied = (occ == null ? false : occ.isOccupied());
        boolean isAutoforceoccupiedenabled = CCUHsApi.getInstance().readDefaultVal("point and " +
                "autoforceoccupied and config and equipRef == \"" + mBPOSEquip.mEquipRef + "\"") > 0;
        boolean isAutoawayenabled = CCUHsApi.getInstance().readDefaultVal("point and " +
                "autoforceaway and config and equipRef == \"" + mBPOSEquip.mEquipRef + "\"") > 0;
        HashMap occupancy = CCUHsApi.getInstance().read("point and occupancy and sensor and " +
                "equipRef == \"" + mBPOSEquip.mEquipRef + "\"");



        double occDetPoint = CCUHsApi.getInstance().readDefaultVal("point and occupancy and detection " +
                "and his and equipRef== \"" + mBPOSEquip.mEquipRef + "\"");
        double setTempCooling = mBPOSEquip.getDesiredTempCooling();
        double setTempHeating = mBPOSEquip.getDesiredTempHeating();
        double roomTemp = mBPOSEquip.getCurrentTemp();
        double systemDefaultTemp = 72.0;


        if (roomTemp > setTempCooling) {
            //Zone is in Cooling
            if (state != COOLING) {
                state = COOLING;
            }
        } else if (roomTemp < setTempHeating) {
            //Zone is in heating
            if (state != HEATING) {
                state = HEATING;
            }
        } else {
            SystemMode systemMode = SystemMode.values()[(int)TunerUtil.readSystemUserIntentVal("conditioning and mode")];
            CcuLog.d(L.TAG_CCU_ZONE, " cazEquip : systemMode-" + systemMode + " roomTemp:" + roomTemp);
            if (systemMode == SystemMode.AUTO || systemMode == SystemMode.COOLONLY  && roomTemp > systemDefaultTemp) {
                state = COOLING;
            }
            if (systemMode == SystemMode.HEATONLY && roomTemp < systemDefaultTemp) {
                state = HEATING;
            }
            if (systemMode == SystemMode.HEATONLY && roomTemp > systemDefaultTemp) {
                state = COOLING;
            }
        }


        if (!occupied && isAutoforceoccupiedenabled) {
            // enable autoforce occupied for ForcedOccupiedTimer
            if(occDetPoint>0){
                CCUHsApi.getInstance().writeDefaultValById(occupancy.get("id").toString(),
                        (double) Occupancy.AUTOFORCEOCCUPIED.ordinal());
            }
        }
        if (occupied && isAutoawayenabled) {
            //if the oocupantnotdetected for autoAwayTimer
            // then enter autoaway
            if(occDetPoint<=0){
                CCUHsApi.getInstance().writeDefaultValById(occupancy.get("id").toString(),
                        (double) Occupancy.AUTOAWAY.ordinal());
            }
        }

        mBPOSEquip.setStatus(state.ordinal(),
                DabSystemController.getInstance().isEmergencyMode() && (state == HEATING ?
                        buildingLimitMinBreached()
                : state == COOLING ? buildingLimitMaxBreached() : false));

    }

    @Override
    public boolean isZoneDead() {

        double buildingLimitMax = TunerUtil.readBuildingTunerValByQuery("building and limit and " +
                "max");
        double buildingLimitMin = TunerUtil.readBuildingTunerValByQuery("building and limit and " +
                "min");

        double tempDeadLeeway = TunerUtil.readBuildingTunerValByQuery("temp and dead and leeway");
        CcuLog.d(L.TAG_CCU_ZONE, " roomTemp : " + mBPOSEquip.getCurrentTemp() + " " +
                "buildingLimitMax:" + buildingLimitMax + " tempDead:" + tempDeadLeeway);
        CcuLog.d(L.TAG_CCU_ZONE, " roomTemp : " + mBPOSEquip.getCurrentTemp() + " " +
                "buildingLimitMin:" + buildingLimitMin + " tempDead:" + tempDeadLeeway);
        if (mBPOSEquip.getCurrentTemp() > (buildingLimitMax + tempDeadLeeway)
                || mBPOSEquip.getCurrentTemp() < (buildingLimitMin - tempDeadLeeway)) {
            return true;
        }

        return false;
    }

    @Override
    public ProfileType getProfileType() {
        return ProfileType.BPOS;
    }

    @Override
    public BPOSConfiguration getProfileConfiguration(short address) {
        return mBPOSEquip.getbposconfiguration();
    }

    @Override
    public Set<Short> getNodeAddresses() {
        return new HashSet<Short>() {{
            add((short) mBPOSEquip.mNodeAddr);
        }};
    }


}
