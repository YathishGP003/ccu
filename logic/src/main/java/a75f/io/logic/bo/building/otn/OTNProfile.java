package a75f.io.logic.bo.building.otn;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.RFDEAD;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Occupied;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.dab.DabSystemController;
import a75f.io.logic.bo.util.SystemTemperatureUtil;
import a75f.io.logic.tuners.TunerUtil;

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
        CcuLog.d(L.TAG_CCU_ZONE, "updateZonePoints : " + mOTNEquip.mNodeAddr);
        if(isRFDead()){
            handleRFDead(mOTNEquip);
            return;
        } else if (isZoneDead()) {
            handleZoneDead(mOTNEquip);
            return;
        }


        double desiredTempCooling =
                SystemTemperatureUtil.getDesiredTempCooling(mOTNEquip.mEquipRef);
        double desiredTempHeating =
                SystemTemperatureUtil.getDesiredTempHeating(mOTNEquip.mEquipRef);
        double tempMidPoint = (desiredTempCooling + desiredTempHeating) / 2;
        mOTNEquip.setDesiredTemp(tempMidPoint);

        double setTempCooling = mOTNEquip.getDesiredTempCooling();
        double setTempHeating = mOTNEquip.getDesiredTempHeating();
        double roomTemp = mOTNEquip.getCurrentTemp();
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
        if (mOTNEquip.getStatus() != state.ordinal()) {
            mOTNEquip.setStatus(state.ordinal());
        }


        mOTNEquip.setStatus(state.ordinal(),
                DabSystemController.getInstance().isEmergencyMode() && (state == HEATING ?
                        buildingLimitMinBreached()
                        : state == COOLING && buildingLimitMaxBreached()));

    }

    private void handleZoneDead(OTNEquip mOTNEquip) {
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
    }

    private void handleRFDead(OTNEquip mOTNEquip) {
        state = RFDEAD;
        String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and " +
                "message and writable and group == \"" + mOTNEquip.mNodeAddr + "\"");
        if (!curStatus.equals(RFDead)) {
            CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable" +
                    " and" +
                    " group == \"" + mOTNEquip.mNodeAddr + "\"", RFDead);
        }
        CCUHsApi.getInstance().writeHisValByQuery("point and not ota and status and his and group == \"" +
                mOTNEquip.mNodeAddr + "\"", (double) RFDEAD.ordinal());
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
