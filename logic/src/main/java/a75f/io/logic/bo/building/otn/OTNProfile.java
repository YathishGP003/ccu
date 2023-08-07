package a75f.io.logic.bo.building.otn;

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
import a75f.io.constants.WhoFiledConstants;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.schedules.ScheduleUtil;
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
public class OTNProfile extends ZoneProfile {
    OTNEquip mOTNEquip;
    public HashMap<Integer, OTNEquip> mOTNDeviceMap;

    public OTNProfile(){
        mOTNDeviceMap = new HashMap<>();
    }

    public void addOTNEquip(ProfileType type, int node, OTNConfiguration config,
                            String floorRef, String roomRef) {
        mOTNEquip = new OTNEquip(type, node);
        mOTNEquip.createEntities(config, floorRef, roomRef);
        mOTNDeviceMap.put(node, mOTNEquip);
        mOTNEquip.init();
    }

    public void addOTNEquip(int node) {
        mOTNEquip = new OTNEquip(ProfileType.OTN, node);
        mOTNDeviceMap.put(node, mOTNEquip);
        mOTNEquip.init();
    }

    public void updateOTN(ProfileType type, int node, OTNConfiguration config, String floorRef,
                          String roomRef) {
        mOTNEquip.update(type, node, config, floorRef, roomRef);
        mOTNEquip.init();
        updateOccupancyDetPoint();
    }

    @Override
    public void updateZonePoints() {

        if (isZoneDead()) {
            state = TEMPDEAD;
            String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and " +
                    "message and writable and group == \"" + mOTNEquip.mNodeAddr + "\"");
            if (!curStatus.equals("Zone Temp Dead")) {
                CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable" +
                        " and" +
                        " group == \"" + mOTNEquip.mNodeAddr + "\"", "Zone Temp Dead");
            }
            CCUHsApi.getInstance().writeHisValByQuery("point and not ota and status and his and group == \"" +
                    mOTNEquip.mNodeAddr + "\"", (double) TEMPDEAD.ordinal());
            return;
        }



        boolean isAutoforceoccupiedenabled = CCUHsApi.getInstance().readDefaultVal("point and " +
                "auto and forced and occupied and config and equipRef == \"" + mOTNEquip.mEquipRef + "\"") > 0;
        /*boolean isAutoforceoccupiedenabled = CCUHsApi.getInstance().readDefaultVal("point and " +
                "auto and forced and occupied and config and equipRef == \"" + mBPOSEquip.mEquipRef + "\"") > 0;
        boolean isAutoawayenabled = CCUHsApi.getInstance().readDefaultVal("point and " +
                "auto and forced and away and config and equipRef == \"" + mOTNEquip.mEquipRef + "\"") > 0;
        HashMap<Object,Object> occupancy = CCUHsApi.getInstance().readEntity("point and occupancy and mode and " +
                "equipRef == \"" + mOTNEquip.mEquipRef + "\"");
        double occupancyvalue =
                CCUHsApi.getInstance().readHisValById(Objects.requireNonNull(occupancy.get("id")).toString());


        double forcedOccupiedMinutes = TunerUtil.readTunerValByQuery("forced and occupied and time",
                mOTNEquip.mEquipRef);
        //We don't need to update forced occupancy if the tuner is 0.
        if (isAutoforceoccupiedenabled && forcedOccupiedMinutes > 0) {
            runAutoForceOccupyOperation(mOTNEquip.mEquipRef);
        } else if (occupancyvalue == Occupancy.AUTOFORCEOCCUPIED.ordinal()) {
            resetForceOccupy();
        }

        if (isAutoawayenabled) runAutoAwayOperation();
        else if (occupancyvalue == Occupancy.AUTOAWAY.ordinal()) {
            resetForceOccupy();
        }*/

        double desiredTempCooling =
                SystemTemperatureUtil.getDesiredTempCooling(mOTNEquip.mEquipRef);
        double desiredTempHeating =
                SystemTemperatureUtil.getDesiredTempHeating(mOTNEquip.mEquipRef);
        double tempMidPoint = (desiredTempCooling + desiredTempHeating) / 2;
        mOTNEquip.setDesiredTemp(tempMidPoint);

        String zoneId = HSUtil.getZoneIdFromEquipId(mOTNEquip.mEquipRef);
        Occupied occ = ScheduleManager.getInstance().getOccupiedModeCache(zoneId);
        boolean occupied = (occ != null && occ.isOccupied());

        HashMap<Object,Object> occDethashmap = CCUHsApi.getInstance().readEntity("point and occupancy and " +
                "detection and his and equipRef == \"" + mOTNEquip.mEquipRef + "\"");
        double occDetPoint =
                CCUHsApi.getInstance().readHisValById(Objects.requireNonNull(occDethashmap.get(
                        "id")).toString());
        double setTempCooling = mOTNEquip.getDesiredTempCooling();
        double setTempHeating = mOTNEquip.getDesiredTempHeating();
        double roomTemp = mOTNEquip.getCurrentTemp();
        double systemDefaultTemp = 72.0;


        /*Log.d(L.TAG_BPOS, "occupied = " + occupied
                + "isAutoforceoccupiedenabled = " + isAutoforceoccupiedenabled
                + "isAutoawayenabled = " + isAutoawayenabled
                + "occupancyvalue = " + occupancyvalue
                + "occDetPoint = " + occDetPoint);*/

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
        if (mOTNEquip.getStatus() != state.ordinal()) {
            mOTNEquip.setStatus(state.ordinal());
        }


        mOTNEquip.setStatus(state.ordinal(),
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
        CcuLog.d(L.TAG_CCU_ZONE, " roomTemp : " + mOTNEquip.getCurrentTemp() + " " +
                "buildingLimitMax:" + buildingLimitMax + " tempDead:" + tempDeadLeeway);
        CcuLog.d(L.TAG_CCU_ZONE, " roomTemp : " + mOTNEquip.getCurrentTemp() + " " +
                "buildingLimitMin:" + buildingLimitMin + " tempDead:" + tempDeadLeeway);
        return mOTNEquip.getCurrentTemp() > (buildingLimitMax + tempDeadLeeway)
                || mOTNEquip.getCurrentTemp() < (buildingLimitMin - tempDeadLeeway);
    }

    @Override
    public ProfileType getProfileType() {
        return ProfileType.OTN;
    }

    @Override
    public OTNConfiguration getProfileConfiguration(short address) {
        return mOTNEquip.getOTNconfiguration();
    }

    @Override
    public Set<Short> getNodeAddresses() {
        return new HashSet<Short>() {{
            add((short) mOTNEquip.mNodeAddr);
        }};
    }


    private void runAutoForceOccupyOperation(String equipRef) {
        double occupancyModeval = CCUHsApi.getInstance().readHisValByQuery(
                "point and  otn and occupancy  and his and " +
                        "mode and equipRef  == \"" + mOTNEquip.mEquipRef + "\"");
        boolean occupancysensor = CCUHsApi.getInstance().readHisValByQuery(
                "point and  otn and occupancy  and his and " +
                        "sensor and equipRef  == \"" + mOTNEquip.mEquipRef + "\"") > 0;

        Occupied occuStatus =
            ScheduleManager.getInstance().getOccupiedModeCache(HSUtil.getZoneIdFromEquipId(equipRef));

        Log.d(L.TAG_OTN, "occupied = " + occuStatus
                + "occupancyModeval = " + occupancyModeval
                + "occupancysensor = " + occupancysensor);

        assert occuStatus != null;
        if ((!occuStatus.isOccupied() || occuStatus.getVacation() != null)
                && occupancyModeval != Occupancy.AUTOAWAY.ordinal()) {
            long temporaryHoldTime =
                    ScheduleUtil.getTemporaryHoldExpiry(HSUtil.getEquipInfo(mOTNEquip.mEquipRef));
            long differenceInMinutes = findDifference(temporaryHoldTime, true);
            double desiredAvgTemp = mOTNEquip.getDesiredTemp();

            /*
            If systemOccupancy is preconditioning then zone level profile will also be in
            preconditioning.
             */
            if (ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.PRECONDITIONING){
                CCUHsApi.getInstance().writeHisValByQuery(
                        "point and otn and occupancy and mode and " +
                                "equipRef == \"" + mOTNEquip.mEquipRef + "\"",
                        (double) Occupancy.PRECONDITIONING.ordinal());
            }else if (occupancysensor) {
                // If we are not is in force occupy then fall into force occupy
                if (occupancyModeval != Occupancy.AUTOFORCEOCCUPIED.ordinal()) {
                    CCUHsApi.getInstance().writeHisValByQuery(
                            "point and otn and occupancy and mode and " +
                                    "equipRef == \"" + mOTNEquip.mEquipRef + "\"",
                            (double) Occupancy.AUTOFORCEOCCUPIED.ordinal());

                    updateDesiredtemp(desiredAvgTemp,true);
                    Log.i(L.TAG_OTN, "Falling in FORCE Occupy mode");
                } else {
                    Log.i(L.TAG_OTN, "We are already in force occupy");
                    // We are already in force occupy
                    // Just update with latest
                    updateDesiredtemp(desiredAvgTemp,false);
                }
            } else {
                Log.d(L.TAG_OTN, "Occupant not detected in unoccupied mode");
                if (occupancyModeval == Occupancy.AUTOFORCEOCCUPIED.ordinal() && differenceInMinutes <= 0) {
                    resetForceOccupy();
                }
            }
        } else {
            // Reset everything if there was force occupied condition
            Log.d(L.TAG_OTN, "We are back to occupied");
            Log.d(L.TAG_OTN, "occupancyModeval = "+occupancyModeval);
            if (occupancyModeval == Occupancy.AUTOFORCEOCCUPIED.ordinal()) {
                Log.d(L.TAG_OTN, "Move to to occupied status");
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
        Log.d(L.TAG_OTN, "Resetting the resetForceOccupy: ");
        CCUHsApi.getInstance().writeHisValByQuery("point and  otn and occupancy  and his and " +
                "mode and equipRef == \"" + mOTNEquip.mEquipRef + "\"", 0.0);
        CCUHsApi.getInstance().writeHisValByQuery("point and  otn and occupancy  and his and " +
                        "mode and equipRef == \"" + mOTNEquip.mEquipRef + "\"",
                (double) Occupancy.UNOCCUPIED.ordinal());

        HashMap<Object,Object> coolDT = CCUHsApi.getInstance().readEntity("point and desired and cooling and equipref " +
                "== \"" + mOTNEquip.mEquipRef + "\"");
        HashMap<Object,Object> heatDT = CCUHsApi.getInstance().readEntity("point and desired and heating and equipref " +
                "== \"" + mOTNEquip.mEquipRef + "\"");

        SystemScheduleUtil.clearOverrides(Objects.requireNonNull(heatDT.get("id")).toString());
        SystemScheduleUtil.clearOverrides(Objects.requireNonNull(coolDT.get("id")).toString());
    }

    private void updateDesiredtemp(Double avgDesiredTemp,boolean isFirstTime) {
        Log.d(L.TAG_OTN, "  updateDesiredtemp ++");
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap<Object,Object> equipMap =
                hayStack.readEntity("equip and group == \"" + mOTNEquip.mNodeAddr + "\"");
        Equip q = new Equip.Builder().setHashMap(equipMap).build();

        String zoneId = HSUtil.getZoneIdFromEquipId(q.getId());
        Occupied occ = ScheduleManager.getInstance().getOccupiedModeCache(zoneId);
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
                    avgDesiredTemp,
                    WhoFiledConstants.OTN_WHO);
        }



    }

    private void runAutoAwayOperation() {
        HashMap<Object,Object> equipMap =
                CCUHsApi.getInstance().readEntity("equip and group == \"" + mOTNEquip.mNodeAddr + "\"");
        Equip q = new Equip.Builder().setHashMap(equipMap).build();
        String zoneId = HSUtil.getZoneIdFromEquipId(mOTNEquip.mEquipRef);
        Occupied occ = ScheduleManager.getInstance().getOccupiedModeCache(zoneId);
        boolean occupied = (occ != null && occ.isOccupied());
        HashMap<Object,Object> occupancymode = CCUHsApi.getInstance().readEntity(
                "point and occupancy  and mode and equipRef == \"" + mOTNEquip.mEquipRef + "\"");
        double occupancyModeval = CCUHsApi.getInstance().readHisValByQuery(
                "point and  otn and occupancy  and his and " +
                        "mode and equipRef  == \"" + mOTNEquip.mEquipRef + "\"");
        boolean occupancysensor = CCUHsApi.getInstance().readHisValByQuery(
                "point and  otn and occupancy  and his and " +
                        "sensor and equipRef  == \"" + mOTNEquip.mEquipRef + "\"") > 0;
        HashMap<Object,Object> ocupancyDetection = CCUHsApi.getInstance().readEntity(
                "point and  otn and occupancy and detection and his and equipRef  == \"" + mOTNEquip.mEquipRef + "\"");

        if (occupied && occupancyModeval != Occupancy.AUTOFORCEOCCUPIED.ordinal()) {
            Log.d(L.TAG_OTN,"  auto away handle");
            //if the oocupantnotdetected for autoAwayTimer
            // then enter autoaway
            if (!occupancysensor) {
                Log.d(L.TAG_OTN, "  auto away case -  occDetPoint <= 0");
                //check if there was no detection from past autoawaytimer
                HisItem hisItem =
                        CCUHsApi.getInstance().curRead(Objects.requireNonNull(ocupancyDetection.get("id")).toString());
                long lastupdatedtime = (hisItem == null) ? null : hisItem.getDate().getTime();
                long min = findDifference(lastupdatedtime, false);
                // read autoawaytimer tuner
                double autoawaytime = TunerUtil.readTunerValByQuery(" point and tuner and auto " +
                        "and away and " +
                        "time ", mOTNEquip.mEquipRef);

                Log.d(L.TAG_OTN, "  auto away case -  min = " + min + " autoawaytime ="
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
            Log.d(L.TAG_OTN, "  auto away case -  reset to occupied");
            clearlevel3Override();

        }
    }

    private void clearlevel3Override() {
        HashMap<Object,Object> coolDT = CCUHsApi.getInstance().readEntity("point and desired and cooling and temp and equipRef == \"" + mOTNEquip.mEquipRef + "\"");
        HashMap<Object,Object> heatDT = CCUHsApi.getInstance().readEntity("point and desired and heating and temp and equipRef == \"" + mOTNEquip.mEquipRef + "\"");
        HashMap<Object,Object> averageDT = CCUHsApi.getInstance().readEntity("point and desired and average and temp and equipRef == \"" + mOTNEquip.mEquipRef + "\"");
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
        HashMap<Object,Object> equip = CCUHsApi.getInstance().readEntity("equip and group == \""+ mOTNEquip.mNodeAddr+"\"");
        return new Equip.Builder().setHashMap(equip).build();
    }


    private void updateOccupancyDetPoint(){
        boolean isAutoawayenabled = CCUHsApi.getInstance().readDefaultVal("point and " +
                "auto and forced and away and config and equipRef == \"" + mOTNEquip.mEquipRef + "\"") > 0;
        Occupied occuStatus =
                ScheduleManager.getInstance().getOccupiedModeCache(HSUtil.getZoneIdFromEquipId(mOTNEquip.mEquipRef ));
        if(isAutoawayenabled && occuStatus != null) {
            if (occuStatus.isOccupied()) {
                HashMap<Object,Object> ocupancyDetection = CCUHsApi.getInstance().readEntity(
                        "point and  otn and occupancy and detection and his and equipRef  ==" +
                                " \"" + mOTNEquip.mEquipRef + "\"");
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
