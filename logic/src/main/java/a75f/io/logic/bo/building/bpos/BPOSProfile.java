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
import a75f.io.logic.tuners.StandaloneTunerUtil;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
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
        boolean isAutoforceoccupiedenabled = CCUHsApi.getInstance().readDefaultVal("point and " +
                "autoforceoccupied and config and equipRef == \"" + mBPOSEquip.mEquipRef + "\"") > 0;
        boolean isAutoawayenabled = CCUHsApi.getInstance().readDefaultVal("point and " +
                "autoforceaway and config and equipRef == \"" + mBPOSEquip.mEquipRef + "\"") > 0;
        HashMap occupancy = CCUHsApi.getInstance().read("point and occupancy and sensor and " +
                "equipRef == \"" + mBPOSEquip.mEquipRef + "\"");
        double occupancyvalue = CCUHsApi.getInstance().readHisValByQuery("point and occupancy and" +
                " sensor and " +
                "equipRef == \"" + mBPOSEquip.mEquipRef + "\"");
        double zonedynamicpriority =
                DabSystemController.getInstance().getEquipDynamicPriority(zoneCoolingLoad != 0 ?
                        zoneCoolingLoad : zoneHeatingLoad, mBPOSEquip.mEquipRef);

        ArrayList<HisItem> fortimer =
                CCUHsApi.getInstance().hisRead(occupancy.get("id").toString(), "today");
        double occDetPoint = CCUHsApi.getInstance().readDefaultVal("point and occupancy and " +
                "detection " +
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

        //use writeHisValueByIdWithoutCOV and use getTemporaryHoldExpiry and use curread to get timer


        if (!occupied && isAutoforceoccupiedenabled &&
                occupancyvalue != (double) Occupancy.AUTOAWAY.ordinal()) {
            if (occDetPoint > 0)
                CCUHsApi.getInstance().writeHisValById(occupancy.get("id").toString(),
                        (double) Occupancy.AUTOFORCEOCCUPIED.ordinal());

            /*check if its already in forced occupy or check if its in auto force occupy
            if(occupancyvalue != (double) Occupancy.FORCEDOCCUPIED.ordinal() ||
                   occupancyvalue != (double) Occupancy.AUTOFORCEOCCUPIED.ordinal()){}

                   This case is commented as of now

             */

            //check the timer
            HisItem hisItem = CCUHsApi.getInstance().curRead(occupancy.get("id").toString());
            Date  lastupdatedtime = (hisItem == null) ? null : hisItem.getDate();


            long th = ScheduleProcessJob.getTemporaryHoldExpiry(HSUtil.getEquipFromZone(String.valueOf(mBPOSEquip.mNodeAddr)));
            if (th > 0) {
                DateTime et = new DateTime(th);
                int min = et.getMinuteOfHour();

                //present time - curread = diff
                //expiry
                //when diff is within 2 hours , I will check the expiry is less than 30 min then i again update the duration
                if(min < 30){
                    int curmin = new Time(System.currentTimeMillis()).getMinutes();
                    int lastupdated = lastupdatedtime.getMinutes();
                    if(curmin-lastupdated < 120){
                        // enable autoforce occupied for ForcedOccupiedTimer
                        double dt = CCUHsApi.getInstance().readHisValByQuery("point and air and temp and " +
                                "desired and average and sp and equipRef == \"" + mBPOSEquip.mEquipRef + "\"");
                        updateDesiredtemp(dt);
                    }

                }


            }
        }
        if (occupied && isAutoawayenabled && occupancyvalue != Occupancy.AUTOFORCEOCCUPIED.ordinal()) {
            //if the oocupantnotdetected for autoAwayTimer
            // then enter autoaway
            if (occDetPoint <= 0) {
                CCUHsApi.getInstance().writeDefaultValById(occupancy.get("id").toString(),
                        (double) Occupancy.AUTOAWAY.ordinal());
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

    private void updateDesiredtemp(Double dt) {
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
                        setHashMap(singleDtPoint).build(), coolingDesiredTemp, heatingDesiredTemp, dt);

    }

}
