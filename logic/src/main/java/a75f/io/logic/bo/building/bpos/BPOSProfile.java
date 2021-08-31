package a75f.io.logic.bo.building.bpos;

import android.util.Log;

import org.joda.time.DateTime;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HisItem;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.dab.DabSystemController;
import a75f.io.logic.bo.haystack.device.SmartNode;
import a75f.io.logic.bo.util.SystemTemperatureUtil;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.BPOSTuners;
import a75f.io.logic.tuners.StandaloneTunerUtil;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;

/*
 * created by spoorthidev on 3-August-2021
 */

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
                CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable" +
                        " and" +
                        " group == \"" + mBPOSEquip.mNodeAddr + "\"", "Zone Temp Dead");
            }
            CCUHsApi.getInstance().writeHisValByQuery("point and status and his and group == \"" +
                    mBPOSEquip.mNodeAddr + "\"", (double) TEMPDEAD.ordinal());
            return;
        }

        boolean isAutoforceoccupiedenabled = CCUHsApi.getInstance().readDefaultVal("point and " +
                "auto and forced and occupied and config and equipRef == \"" + mBPOSEquip.mEquipRef + "\"") > 0;
        boolean isAutoawayenabled = CCUHsApi.getInstance().readDefaultVal("point and " +
                "auto and forced and away and config and equipRef == \"" + mBPOSEquip.mEquipRef + "\"") > 0;

        if (isAutoforceoccupiedenabled) runAutoForceOccupyOperation(mBPOSEquip.mEquipRef);

        if (isAutoawayenabled) runAutoAwayOperation();

        double zoneCurTemp = mBPOSEquip.getCurrentTemp();
        double desiredTempCooling =
                SystemTemperatureUtil.getDesiredTempCooling(mBPOSEquip.mEquipRef);
        double desiredTempHeating =
                SystemTemperatureUtil.getDesiredTempHeating(mBPOSEquip.mEquipRef);
        double tempMidPoint = (desiredTempCooling + desiredTempHeating) / 2;
        double zoneCoolingLoad = zoneCurTemp > tempMidPoint ? zoneCurTemp - desiredTempCooling : 0;
        double zoneHeatingLoad = zoneCurTemp < tempMidPoint ? desiredTempHeating - zoneCurTemp : 0;

        String zoneId = HSUtil.getZoneIdFromEquipId(mBPOSEquip.mEquipRef);
        Occupied occ = ScheduleProcessJob.getOccupiedModeCache(zoneId);
        boolean occupied = (occ == null ? false : occ.isOccupied());

        HashMap occupancy = CCUHsApi.getInstance().read("point and occupancy and mode and " +
                "equipRef == \"" + mBPOSEquip.mEquipRef + "\"");
        Log.d("BPOSProfile", "id  = " + occupancy.get("id"));
        if (occupancy.get("id") == null) Log.d("BPOSProfile", "id is null" + occupancy.get("id"));

        double occupancyvalue =
                CCUHsApi.getInstance().readHisValById(occupancy.get("id").toString());


        HashMap occDethashmap = CCUHsApi.getInstance().read("point and occupancy and " +
                "detection and his and equipRef == \"" + mBPOSEquip.mEquipRef + "\"");
        double zonedynamicpriority =
                DabSystemController.getInstance().getEquipDynamicPriority(zoneCoolingLoad != 0 ?
                        zoneCoolingLoad : zoneHeatingLoad, mBPOSEquip.mEquipRef);
        double occDetPoint =
                CCUHsApi.getInstance().readHisValById(occDethashmap.get("id").toString());
        double setTempCooling = mBPOSEquip.getDesiredTempCooling();
        double setTempHeating = mBPOSEquip.getDesiredTempHeating();
        double roomTemp = mBPOSEquip.getCurrentTemp();
        double systemDefaultTemp = 72.0;


        Log.d("BPOSProfile", "occupied = " + occupied
                + "isAutoforceoccupiedenabled = " + isAutoforceoccupiedenabled
                + "isAutoawayenabled = " + isAutoawayenabled
                + "occupancyvalue = " + occupancyvalue
                + "occDetPoint = " + occDetPoint);

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
            SystemMode systemMode = SystemMode.values()[(int) TunerUtil.readSystemUserIntentVal(
                    "conditioning and mode")];
            CcuLog.d(L.TAG_CCU_ZONE,
                    " cazEquip : systemMode-" + systemMode + " roomTemp:" + roomTemp);
            if (systemMode == SystemMode.AUTO || systemMode == SystemMode.COOLONLY && roomTemp > systemDefaultTemp) {
                state = COOLING;
            }
            if (systemMode == SystemMode.HEATONLY && roomTemp < systemDefaultTemp) {
                state = HEATING;
            }
            if (systemMode == SystemMode.HEATONLY && roomTemp > systemDefaultTemp) {
                state = COOLING;
            }
        }

        Log.d("BPOSProfile", "status" + state);
        Log.d("BPOSProfile", "mBPOSEquip.getStatus()" + mBPOSEquip.getStatus());
        Log.d("BPOSProfile", "state.ordinal()" + state.ordinal());
        if (mBPOSEquip.getStatus() != state.ordinal()) {
            mBPOSEquip.setStatus(state.ordinal());
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


    private void runAutoForceOccupyOperation(String equipRef) {
        double occupancyModeval = CCUHsApi.getInstance().readHisValByQuery(
                "point and  bpos and occupancy  and his and " +
                        "mode and equipRef  == \"" + mBPOSEquip.mEquipRef + "\"");
        boolean occupancysensor = CCUHsApi.getInstance().readHisValByQuery(
                "point and  bpos and occupancy  and his and " +
                        "sensor and equipRef  == \"" + mBPOSEquip.mEquipRef + "\"") > 0;

        HashMap occupancymode = CCUHsApi.getInstance().read(
                "point and occupancy and mode and equipRef == \"" + mBPOSEquip.mEquipRef + "\"");


        Occupied occuStatus =
                ScheduleProcessJob.getOccupiedModeCache(HSUtil.getZoneIdFromEquipId(equipRef));

        Log.d("BPOSProfile", "occupied = " + occuStatus
                + "occupancyModeval = " + occupancyModeval
                + "occupancysensor = " + occupancysensor);

        if ((!occuStatus.isOccupied() || occuStatus.getVacation() != null)
                && occupancyModeval != Occupancy.AUTOAWAY.ordinal()
        ) {

            long temporaryHoldTime =
                    ScheduleProcessJob.getTemporaryHoldExpiry(HSUtil.getEquipInfo(mBPOSEquip.mEquipRef));
            long differenceInMinutes = findDifference(temporaryHoldTime, true);

            if (occupancysensor) {
                // If we are not is in force occupy then fall into force occupy
                if (occupancyModeval != Occupancy.AUTOFORCEOCCUPIED.ordinal()) {

                    CCUHsApi.getInstance().writeHisValByQuery(
                            "point and bpos and occupancy and mode and " +
                                    "equipRef == \"" + mBPOSEquip.mEquipRef + "\"",
                            (double) Occupancy.AUTOFORCEOCCUPIED.ordinal());
                    double desiredAvgTemp = mBPOSEquip.getDesiredTemp();
                    updateDesiredtemp(desiredAvgTemp);
                    Log.i(Globals.TAG, "Falling in FORCE Occupy mode");
                } else {
                    Log.i(Globals.TAG, "We are already in force occupy");
                    // We are already in force occupy

                    // If the last
                    int quickPeriod = 30;
                    if (differenceInMinutes < quickPeriod) {
                        Log.i(Globals.TAG, "Occupant Detected time is less than 30 min so " +
                                "extending the time");
                        double desiredAvgTemp = mBPOSEquip.getDesiredTemp();
                        updateDesiredtemp(desiredAvgTemp);
                    } else {
                        // Wait for less then 30 then update based on the condition
                        Log.i(Globals.TAG, "No action state waiting for time expiry");
                    }
                }
            } else {
                Log.d("BPOSProfile", "Occupant not detected in unoccupied mode");
                if (occupancyModeval == Occupancy.AUTOFORCEOCCUPIED.ordinal() && differenceInMinutes <= 0) {
                    resetForceOccupy();
                }
            }
        } else {
            // Reset everything if there was force occupied condition
            Log.d("BPOSProfile", "We are back to occupied");
            if (occupancyModeval == Occupancy.AUTOFORCEOCCUPIED.ordinal()) {
                Log.d("BPOSProfile", "Move to to occupied status");
                resetForceOccupy();
            }
        }
    }


    private long findDifference(Long expirytime, Boolean isCurrentGreater) {
        Date expiryTime = new Date(expirytime);
        Date currentTime = new Date(System.currentTimeMillis());
        long diff = (isCurrentGreater ? (expiryTime.getTime() - currentTime.getTime()) :
                (currentTime.getTime() - expiryTime.getTime()));
        return ((diff / 1000) / 60);
    }

    private void resetForceOccupy() {
        Log.i(Globals.TAG, "Resetting the resetForceOccupy: ");
        CCUHsApi.getInstance().writeHisValByQuery("point and  bpos and occupancy  and his and " +
                "mode and equipRef == \"" + mBPOSEquip.mEquipRef + "\"", 0.0);
        CCUHsApi.getInstance().writeHisValByQuery("point and  bpos and occupancy  and his and " +
                        "mode and equipRef == \"" + mBPOSEquip.mEquipRef + "\"",
                (double) Occupancy.UNOCCUPIED.ordinal());

        HashMap coolDT = CCUHsApi.getInstance().read("point and desired and cooling and equipref " +
                "== \"" + mBPOSEquip.mEquipRef + "\"");
        ;
        HashMap heatDT = CCUHsApi.getInstance().read("point and desired and heating and equipref " +
                "== \"" + mBPOSEquip.mEquipRef + "\"");
        ;
        ScheduleProcessJob.clearOverrides(heatDT.get("id").toString());
        ScheduleProcessJob.clearOverrides(coolDT.get("id").toString());
    }

    private void updateDesiredtemp(Double dt) {
        Log.d("BPOSProfile", "  updateDesiredtemp ++");
        HashMap equipMap =
                CCUHsApi.getInstance().read("equip and group == \"" + mBPOSEquip.mNodeAddr + "\"");
        Equip q = new Equip.Builder().setHashMap(equipMap).build();

        double cdb = StandaloneTunerUtil.readTunerValByQuery("deadband and base and cooling and " +
                "equipRef == \"" + q.getId() + "\"");
        double hdb = StandaloneTunerUtil.readTunerValByQuery("deadband and base and heating and " +
                "equipRef == \"" + q.getId() + "\"");
        String zoneId = HSUtil.getZoneIdFromEquipId(q.getId());
        Occupied occ = ScheduleProcessJob.getOccupiedModeCache(zoneId);
        if (occ != null) {
            cdb = occ.getCoolingDeadBand();
            hdb = occ.getHeatingDeadBand();
        }
        double coolingDesiredTemp = dt + cdb;
        double heatingDesiredTemp = dt - hdb;


        HashMap coolingDtPoint = CCUHsApi.getInstance().read("point and air and temp and " +
                "desired and cooling and sp and equipRef == \"" + q.getId() + "\"");
        if (coolingDtPoint == null || coolingDtPoint.size() == 0) {
            throw new IllegalArgumentException();
        }
        try {
            CCUHsApi.getInstance().writeHisValById(coolingDtPoint.get("id").toString(),
                    coolingDesiredTemp);
        } catch (Exception e) {
            e.printStackTrace();
        }

        HashMap heatinDtPoint = CCUHsApi.getInstance().read("point and air and temp and " +
                "desired and heating and sp and equipRef == \"" + q.getId() + "\"");
        if (heatinDtPoint == null || heatinDtPoint.size() == 0) {
            throw new IllegalArgumentException();
        }

        try {
            CCUHsApi.getInstance().writeHisValById(heatinDtPoint.get("id").toString(),
                    heatingDesiredTemp);
        } catch (Exception e) {
            e.printStackTrace();
        }

        HashMap singleDtPoint = CCUHsApi.getInstance().read("point and air and temp and " +
                "desired and average and sp and equipRef == \"" + q.getId() + "\"");
        if (singleDtPoint == null || singleDtPoint.size() == 0) {
            throw new IllegalArgumentException();
        }
        try {
            CCUHsApi.getInstance().writeHisValById(singleDtPoint.get("id").toString(), dt);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ScheduleProcessJob.handleManualDesiredTempUpdate(new Point.Builder().setHashMap(coolingDtPoint)
                        .build(), new Point.Builder().setHashMap(heatinDtPoint).build(),
                new Point.Builder().
                        setHashMap(singleDtPoint).build(), coolingDesiredTemp, heatingDesiredTemp
                , dt);

    }

    private void runAutoAwayOperation() {
        String zoneId = HSUtil.getZoneIdFromEquipId(mBPOSEquip.mEquipRef);
        Occupied occ = ScheduleProcessJob.getOccupiedModeCache(zoneId);
        boolean occupied = (occ == null ? false : occ.isOccupied());
        HashMap occupancymode = CCUHsApi.getInstance().read(
                "point and occupancy  and mode and equipRef == \"" + mBPOSEquip.mEquipRef + "\"");
        double occupancyModeval = CCUHsApi.getInstance().readHisValByQuery(
                "point and  bpos and occupancy  and his and " +
                        "mode and equipRef  == \"" + mBPOSEquip.mEquipRef + "\"");
        boolean occupancysensor = CCUHsApi.getInstance().readHisValByQuery(
                "point and  bpos and occupancy  and his and " +
                        "sensor and equipRef  == \"" + mBPOSEquip.mEquipRef + "\"") > 0;
        HashMap ocupancyDetection = CCUHsApi.getInstance().read(
                "point and  bpos and occupancy and detection and his and equipRef  == \"" + mBPOSEquip.mEquipRef + "\"");

        if (occupied && occupancyModeval != Occupancy.AUTOFORCEOCCUPIED.ordinal()) {
            Log.d("BPOSProfile", "  auto away handle");
            //if the oocupantnotdetected for autoAwayTimer
            // then enter autoaway
            if (!occupancysensor) {
                Log.d("BPOSProfile", "  auto away case -  occDetPoint <= 0");
                //check if there was no detection from past autoawaytimer
                HisItem hisItem =
                        CCUHsApi.getInstance().curRead(ocupancyDetection.get("id").toString());
                long lastupdatedtime = (hisItem == null) ? null : hisItem.getDate().getTime();
                DateTime et = new DateTime(lastupdatedtime);


                long min = findDifference(lastupdatedtime, false);
                // read autoawaytimer tuner
                double autoawaytime = TunerUtil.readTunerValByQuery(" point and tuner and auto " +
                        "and away and " +
                        "time ", mBPOSEquip.mEquipRef);

                Log.d("BPOSProfile", "  auto away case -  min = " + min + " autoawaytime ="
                        + autoawaytime + "lastupdatedtime" + lastupdatedtime);

                if (min >= autoawaytime && occupancyModeval != (double) Occupancy.AUTOAWAY.ordinal()) {
                    CCUHsApi.getInstance().writeHisValById(occupancymode.get("id").toString(),
                            (double) Occupancy.AUTOAWAY.ordinal());
                    CCUHsApi.getInstance().writeHisValById(ocupancyDetection.get("id").toString(),
                            0.0);
                }
            } else {
                if (occupancysensor)
                    CCUHsApi.getInstance().writeHisValById(occupancymode.get("id").toString(),
                            (double) Occupancy.OCCUPIED.ordinal());
            }
        }
        if (!occupied)
            CCUHsApi.getInstance().writeHisValById(occupancymode.get("id").toString(),
                    (double) Occupancy.UNOCCUPIED.ordinal());

    }

}
