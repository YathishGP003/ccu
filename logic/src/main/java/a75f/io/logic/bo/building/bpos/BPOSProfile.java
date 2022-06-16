package a75f.io.logic.bo.building.bpos;

import android.util.Log;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.HisItem;
import a75f.io.api.haystack.Occupied;
import a75f.io.api.haystack.Point;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.dab.DabSystemController;
import a75f.io.logic.bo.util.SystemTemperatureUtil;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.jobs.SystemScheduleUtil;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;

/*
 * created by spoorthidev on 3-August-2021
 */
public class BPOSProfile extends ZoneProfile {
    BPOSEquip mBPOSEquip;
    public HashMap<Integer, BPOSEquip> mBposDeviceMap;

    public BPOSProfile(){
        mBposDeviceMap = new HashMap<>();
    }

    public void addBPOSEquip(ProfileType type, int node, BPOSConfiguration config,
                             String floorRef, String roomRef) {
        mBPOSEquip = new BPOSEquip(type, node);
        mBPOSEquip.createEntities(config, floorRef, roomRef);
        mBposDeviceMap.put(node,mBPOSEquip);
        mBPOSEquip.init();
    }

    public void addBPOSEquip(int node) {
        mBPOSEquip = new BPOSEquip(ProfileType.BPOS, node);
        mBposDeviceMap.put(node,mBPOSEquip);
        mBPOSEquip.init();
    }

    public void updateBPOS(ProfileType type, int node, BPOSConfiguration config, String floorRef,
                           String roomRef) {
        mBPOSEquip.update(type, node, config, floorRef, roomRef);
        mBPOSEquip.init();
        updateOccupancyDetPoint();
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
        HashMap<Object,Object> occupancy = CCUHsApi.getInstance().readEntity("point and occupancy and mode and " +
                "equipRef == \"" + mBPOSEquip.mEquipRef + "\"");
        double occupancyvalue =
                CCUHsApi.getInstance().readHisValById(Objects.requireNonNull(occupancy.get("id")).toString());


        double forcedOccupiedMinutes = TunerUtil.readTunerValByQuery("forced and occupied and time",
                mBPOSEquip.mEquipRef);
        //We don't need to update forced occupancy if the tuner is 0.
        if (isAutoforceoccupiedenabled && forcedOccupiedMinutes > 0) {
            runAutoForceOccupyOperation(mBPOSEquip.mEquipRef);
        } else if (occupancyvalue == Occupancy.AUTOFORCEOCCUPIED.ordinal()) {
            resetForceOccupy();
        }

        if (isAutoawayenabled) runAutoAwayOperation();
        else if (occupancyvalue == Occupancy.AUTOAWAY.ordinal()) {
            resetForceOccupy();
        }

        double desiredTempCooling =
                SystemTemperatureUtil.getDesiredTempCooling(mBPOSEquip.mEquipRef);
        double desiredTempHeating =
                SystemTemperatureUtil.getDesiredTempHeating(mBPOSEquip.mEquipRef);
        double tempMidPoint = (desiredTempCooling + desiredTempHeating) / 2;
        mBPOSEquip.setDesiredTemp(tempMidPoint);

        String zoneId = HSUtil.getZoneIdFromEquipId(mBPOSEquip.mEquipRef);
        Occupied occ = ScheduleProcessJob.getOccupiedModeCache(zoneId);
        boolean occupied = (occ != null && occ.isOccupied());




        HashMap<Object,Object> occDethashmap = CCUHsApi.getInstance().readEntity("point and occupancy and " +
                "detection and his and equipRef == \"" + mBPOSEquip.mEquipRef + "\"");
        double occDetPoint =
                CCUHsApi.getInstance().readHisValById(Objects.requireNonNull(occDethashmap.get(
                        "id")).toString());
        double setTempCooling = mBPOSEquip.getDesiredTempCooling();
        double setTempHeating = mBPOSEquip.getDesiredTempHeating();
        double roomTemp = mBPOSEquip.getCurrentTemp();
        double systemDefaultTemp = 72.0;


        Log.d(L.TAG_BPOS, "occupied = " + occupied
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
        if (mBPOSEquip.getStatus() != state.ordinal()) {
            mBPOSEquip.setStatus(state.ordinal());
        }


        mBPOSEquip.setStatus(state.ordinal(),
                DabSystemController.getInstance().isEmergencyMode() && (state == HEATING ?
                        buildingLimitMinBreached()
                        : state == COOLING && buildingLimitMaxBreached()));

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
        return mBPOSEquip.getCurrentTemp() > (buildingLimitMax + tempDeadLeeway)
                || mBPOSEquip.getCurrentTemp() < (buildingLimitMin - tempDeadLeeway);
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

        Occupied occuStatus =
                ScheduleProcessJob.getOccupiedModeCache(HSUtil.getZoneIdFromEquipId(equipRef));

        Log.d(L.TAG_BPOS, "occupied = " + occuStatus
                + "occupancyModeval = " + occupancyModeval
                + "occupancysensor = " + occupancysensor);

        assert occuStatus != null;
        if ((!occuStatus.isOccupied() || occuStatus.getVacation() != null)
                && occupancyModeval != Occupancy.AUTOAWAY.ordinal()) {
            long temporaryHoldTime =
                    ScheduleProcessJob.getTemporaryHoldExpiry(HSUtil.getEquipInfo(mBPOSEquip.mEquipRef));
            long differenceInMinutes = findDifference(temporaryHoldTime, true);
            double desiredAvgTemp = mBPOSEquip.getDesiredTemp();

            /*
            If systemOccupancy is preconditioning then zone level profile will also be in
            preconditioning.
             */
            if (ScheduleProcessJob.getSystemOccupancy() == Occupancy.PRECONDITIONING){
                CCUHsApi.getInstance().writeHisValByQuery(
                        "point and bpos and occupancy and mode and " +
                                "equipRef == \"" + mBPOSEquip.mEquipRef + "\"",
                        (double) Occupancy.PRECONDITIONING.ordinal());
            }else if (occupancysensor) {
                // If we are not is in force occupy then fall into force occupy
                if (occupancyModeval != Occupancy.AUTOFORCEOCCUPIED.ordinal()) {

                    CCUHsApi.getInstance().writeHisValByQuery(
                            "point and bpos and occupancy and mode and " +
                                    "equipRef == \"" + mBPOSEquip.mEquipRef + "\"",
                            (double) Occupancy.AUTOFORCEOCCUPIED.ordinal());

                    updateDesiredtemp(desiredAvgTemp,true);
                    Log.i(L.TAG_BPOS, "Falling in FORCE Occupy mode");
                } else {
                    Log.i(L.TAG_BPOS, "We are already in force occupy");
                    // We are already in force occupy
                    // Just update with latest
                    updateDesiredtemp(desiredAvgTemp,false);
                }
            } else {
                Log.d(L.TAG_BPOS, "Occupant not detected in unoccupied mode");
                if (occupancyModeval == Occupancy.AUTOFORCEOCCUPIED.ordinal() && differenceInMinutes <= 0) {
                    resetForceOccupy();
                }
            }
        } else {
            // Reset everything if there was force occupied condition
            Log.d(L.TAG_BPOS, "We are back to occupied");
            Log.d(L.TAG_BPOS, "occupancyModeval = "+occupancyModeval);
            if (occupancyModeval == Occupancy.AUTOFORCEOCCUPIED.ordinal()) {
                Log.d(L.TAG_BPOS, "Move to to occupied status");
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
        Log.d(L.TAG_BPOS, "Resetting the resetForceOccupy: ");
        CCUHsApi.getInstance().writeHisValByQuery("point and  bpos and occupancy  and his and " +
                "mode and equipRef == \"" + mBPOSEquip.mEquipRef + "\"", 0.0);
        CCUHsApi.getInstance().writeHisValByQuery("point and  bpos and occupancy  and his and " +
                        "mode and equipRef == \"" + mBPOSEquip.mEquipRef + "\"",
                (double) Occupancy.UNOCCUPIED.ordinal());

        HashMap<Object,Object> coolDT = CCUHsApi.getInstance().readEntity("point and desired and cooling and equipref " +
                "== \"" + mBPOSEquip.mEquipRef + "\"");
        HashMap<Object,Object> heatDT = CCUHsApi.getInstance().readEntity("point and desired and heating and equipref " +
                "== \"" + mBPOSEquip.mEquipRef + "\"");

        SystemScheduleUtil.clearOverrides(Objects.requireNonNull(heatDT.get("id")).toString());
        SystemScheduleUtil.clearOverrides(Objects.requireNonNull(coolDT.get("id")).toString());
    }

    private void updateDesiredtemp(Double avgDesiredTemp,boolean isFirstTime) {
        Log.d(L.TAG_BPOS, "  updateDesiredtemp ++");
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap<Object,Object> equipMap =
                hayStack.readEntity("equip and group == \"" + mBPOSEquip.mNodeAddr + "\"");
        Equip q = new Equip.Builder().setHashMap(equipMap).build();

        String zoneId = HSUtil.getZoneIdFromEquipId(q.getId());
        Occupied occ = ScheduleProcessJob.getOccupiedModeCache(zoneId);
        HashMap<Object,Object> coolingDtPoint = hayStack.readEntity("point and air and temp and " +
                "desired and cooling and sp and equipRef == \"" + q.getId() + "\"");
        if (coolingDtPoint == null || coolingDtPoint.size() == 0) {
            throw new IllegalArgumentException();
        }
        HashMap<Object,Object> heatinDtPoint = hayStack.readEntity("point and air and temp and " +
                "desired and heating and sp and equipRef == \"" + q.getId() + "\"");
        if (heatinDtPoint == null || heatinDtPoint.size() == 0) {
            throw new IllegalArgumentException();
        }
        HashMap<Object,Object>  avgDtPoint = CCUHsApi.getInstance().readEntity("point and air and temp and " +
                "desired and average and sp and equipRef == \"" + q.getId() + "\"");
        if (avgDtPoint == null || avgDtPoint.size() == 0) {
            throw new IllegalArgumentException();
        }
        double coolingDesiredTemp;
        double heatingDesiredTemp;
        if (occ != null) {
            if(isFirstTime){
                coolingDesiredTemp = occ.getCoolingVal();
                heatingDesiredTemp = occ.getHeatingVal();
            }else{
                coolingDesiredTemp = hayStack.readPointPriorityVal(Objects.requireNonNull(coolingDtPoint.get("id")).toString());
                heatingDesiredTemp = hayStack.readPointPriorityVal(Objects.requireNonNull(heatinDtPoint.get("id")).toString());
            }

            CCUHsApi.getInstance().writeHisValById(Objects.requireNonNull(coolingDtPoint.get("id")).toString(),
                    coolingDesiredTemp);
            CCUHsApi.getInstance().writeHisValById(Objects.requireNonNull(heatinDtPoint.get("id")).toString(),
                    heatingDesiredTemp);
            CCUHsApi.getInstance().writeHisValById(Objects.requireNonNull(avgDtPoint.get("id")).toString(),
                    avgDesiredTemp);
            SystemScheduleUtil.handleManualDesiredTempUpdate(new Point.Builder().setHashMap(coolingDtPoint).build(),
                    new Point.Builder().setHashMap(heatinDtPoint).build(),
                    new Point.Builder().setHashMap(avgDtPoint).build(),
                    coolingDesiredTemp,
                    heatingDesiredTemp,
                    avgDesiredTemp);
        }



    }

    private void runAutoAwayOperation() {
        HashMap<Object,Object> equipMap =
                CCUHsApi.getInstance().readEntity("equip and group == \"" + mBPOSEquip.mNodeAddr + "\"");
        Equip q = new Equip.Builder().setHashMap(equipMap).build();
        String zoneId = HSUtil.getZoneIdFromEquipId(mBPOSEquip.mEquipRef);
        Occupied occ = ScheduleProcessJob.getOccupiedModeCache(zoneId);
        boolean occupied = (occ != null && occ.isOccupied());
        HashMap<Object,Object> occupancymode = CCUHsApi.getInstance().readEntity(
                "point and occupancy  and mode and equipRef == \"" + mBPOSEquip.mEquipRef + "\"");
        double occupancyModeval = CCUHsApi.getInstance().readHisValByQuery(
                "point and  bpos and occupancy  and his and " +
                        "mode and equipRef  == \"" + mBPOSEquip.mEquipRef + "\"");
        boolean occupancysensor = CCUHsApi.getInstance().readHisValByQuery(
                "point and  bpos and occupancy  and his and " +
                        "sensor and equipRef  == \"" + mBPOSEquip.mEquipRef + "\"") > 0;
        HashMap<Object,Object> ocupancyDetection = CCUHsApi.getInstance().readEntity(
                "point and  bpos and occupancy and detection and his and equipRef  == \"" + mBPOSEquip.mEquipRef + "\"");

        if (occupied && occupancyModeval != Occupancy.AUTOFORCEOCCUPIED.ordinal()) {
            Log.d(L.TAG_BPOS ,"  auto away handle");
            //if the oocupantnotdetected for autoAwayTimer
            // then enter autoaway
            if (!occupancysensor) {
                Log.d(L.TAG_BPOS, "  auto away case -  occDetPoint <= 0");
                //check if there was no detection from past autoawaytimer
                HisItem hisItem =
                        CCUHsApi.getInstance().curRead(Objects.requireNonNull(ocupancyDetection.get("id")).toString());
                long lastupdatedtime = (hisItem == null) ? null : hisItem.getDate().getTime();
                long min = findDifference(lastupdatedtime, false);
                // read autoawaytimer tuner
                double autoawaytime = TunerUtil.readTunerValByQuery(" point and tuner and auto " +
                        "and away and " +
                        "time ", mBPOSEquip.mEquipRef);

                Log.d(L.TAG_BPOS, "  auto away case -  min = " + min + " autoawaytime ="
                        + autoawaytime + "lastupdatedtime" + lastupdatedtime);

                if (min >= autoawaytime && occupancyModeval != (double) Occupancy.AUTOAWAY.ordinal()) {
                    HashMap<Object,Object> coolingDtPoint = CCUHsApi.getInstance().readEntity("point and air and temp and " +
                            "desired and cooling and sp and equipRef == \"" + q.getId() + "\"");
                    if (coolingDtPoint == null || coolingDtPoint.size() == 0) {
                        throw new IllegalArgumentException();
                    }
                    HashMap<Object,Object> heatinDtPoint = CCUHsApi.getInstance().readEntity("point and air and temp and " +
                            "desired and heating and sp and equipRef == \"" + q.getId() + "\"");
                    if (heatinDtPoint == null || heatinDtPoint.size() == 0) {
                        throw new IllegalArgumentException();
                    }
                    CCUHsApi.getInstance().writeHisValById(Objects.requireNonNull(occupancymode.get("id")).toString(),
                            (double) Occupancy.AUTOAWAY.ordinal());
                    CCUHsApi.getInstance().writeHisValById(Objects.requireNonNull(ocupancyDetection.get("id")).toString(),
                            0.0);
                }
            } else {
                if (occupancyModeval != Occupancy.OCCUPIED.ordinal()) {
                    CCUHsApi.getInstance().writeHisValById(Objects.requireNonNull(occupancymode.get(
                            "id")).toString(),
                            (double) Occupancy.OCCUPIED.ordinal());
                    clearlevel3Override();
                }
            }
        }else {
            Log.d(L.TAG_BPOS, "  auto away case -  reset to occupied");
            clearlevel3Override();

        }
    }

    private void clearlevel3Override() {
        HashMap<Object,Object> coolDT = CCUHsApi.getInstance().readEntity("point and desired and cooling and temp and equipRef == \"" + mBPOSEquip.mEquipRef + "\"");
        HashMap<Object,Object> heatDT = CCUHsApi.getInstance().readEntity("point and desired and heating and temp and equipRef == \"" + mBPOSEquip.mEquipRef + "\"");
        HashMap<Object,Object> averageDT = CCUHsApi.getInstance().readEntity("point and desired and average and temp and equipRef == \"" + mBPOSEquip.mEquipRef + "\"");
        double coolDTl3 =  HSUtil.getPriorityLevelVal(Objects.requireNonNull(coolDT.get("id")).toString(),3);
        double heatDTl3 =  HSUtil.getPriorityLevelVal(Objects.requireNonNull(heatDT.get("id")).toString(),3);
        double avgDT =  HSUtil.getPriorityLevelVal(Objects.requireNonNull(averageDT.get("id")).toString(),3);

        if( coolDTl3 != 0.0) {
            CCUHsApi.getInstance().pointWrite(HRef.copy(Objects.requireNonNull(coolDT.get("id")).toString()), 3, "manual", HNum.make(0), HNum.make(1, "ms"));
        }
        if( heatDTl3 != 0.0) {
            CCUHsApi.getInstance().pointWrite(HRef.copy(Objects.requireNonNull(heatDT.get("id")).toString()), 3, "manual"
                    , HNum.make(0), HNum.make(1, "ms"));
        }
        if (avgDT != 0.0) {
            CCUHsApi.getInstance().pointWrite(
                    HRef.copy(Objects.requireNonNull(averageDT.get("id")).toString()),
                    3,
                    "manual",
                    HNum.make(0),
                    HNum.make(1, "ms")
            );
        }


    }

    @Override
    public Equip getEquip()
    {
        HashMap<Object,Object> equip = CCUHsApi.getInstance().readEntity("equip and group == \""+mBPOSEquip.mNodeAddr+"\"");
        return new Equip.Builder().setHashMap(equip).build();
    }


    private void updateOccupancyDetPoint(){
        boolean isAutoawayenabled = CCUHsApi.getInstance().readDefaultVal("point and " +
                "auto and forced and away and config and equipRef == \"" + mBPOSEquip.mEquipRef + "\"") > 0;
        Occupied occuStatus =
                ScheduleProcessJob.getOccupiedModeCache(HSUtil.getZoneIdFromEquipId(mBPOSEquip.mEquipRef ));
        if(isAutoawayenabled && occuStatus != null) {
            if (occuStatus.isOccupied()) {
                HashMap<Object,Object> ocupancyDetection = CCUHsApi.getInstance().readEntity(
                        "point and  bpos and occupancy and detection and his and equipRef  ==" +
                                " \"" + mBPOSEquip.mEquipRef + "\"");
                if (ocupancyDetection.get("id") != null) {
                    double val = CCUHsApi.getInstance().readHisValById(Objects.requireNonNull(ocupancyDetection.get(
                            "id")).toString());
                    CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(Objects.requireNonNull(ocupancyDetection.get(
                            "id")).toString(),
                            val);
                }
            }
        }
    }
}
