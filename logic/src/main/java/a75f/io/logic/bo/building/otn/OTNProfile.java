package a75f.io.logic.bo.building.otn;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.RFDEAD;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.domain.equips.OtnEquip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.tuners.TunerUtil;

/*
 * created by spoorthidev on 3-August-2021
 */
public class OTNProfile extends ZoneProfile {

    OtnEquip otnEquip;
    Short mNodeAddr;
    public OTNProfile(String equipRef, Short nodeAddr) {
        otnEquip = new OtnEquip(equipRef);
        mNodeAddr = nodeAddr;

    }

    @Override
    public void updateZonePoints() {
        CcuLog.d(L.TAG_CCU_ZONE, "updateZonePoints : " + mNodeAddr);
        if(isRFDead()){
            handleRFDead();
            return;
        } else if (isZoneDead()) {
            handleZoneDead();
            return;
        }

        double setTempCooling = otnEquip.getDesiredTempCooling().readPriorityVal();
        double setTempHeating = otnEquip.getDesiredTempHeating().readPriorityVal();
        double roomTemp = otnEquip.getCurrentTemp().readHisVal();
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

        Boolean isEmergencyMode = L.ccu().systemProfile.getSystemController().isEmergencyMode();
        CcuLog.d(L.TAG_CCU_ZONE,
                " OtnEquip : state " + state + " roomTemp " + roomTemp+ " setTempCooling "
                        +setTempCooling+ " setTempHeating "+setTempHeating+ " isEmergencyMode "+isEmergencyMode);

        setStatus(state.ordinal(), isEmergencyMode && (state == HEATING ?
                        buildingLimitMinBreached() : state == COOLING && buildingLimitMaxBreached()));

    }

    private void handleZoneDead() {
        state = TEMPDEAD;
        String curStatus = otnEquip.getEquipStatusMessage().readDefaultStrVal();
        if (!curStatus.equals("Zone Temp Dead")) {
            otnEquip.getEquipStatusMessage().writeDefaultVal("Zone Temp Dead");
        }
        otnEquip.getEquipStatus().writeHisVal(TEMPDEAD.ordinal());
    }

    private void handleRFDead() {
        state = RFDEAD;
        String curStatus = otnEquip.getEquipStatusMessage().readDefaultStrVal();
        if (!curStatus.equals(RFDead)) {
            otnEquip.getEquipStatusMessage().writeDefaultVal(RFDead);
        }
        otnEquip.getEquipStatus().writeHisVal(RFDEAD.ordinal());
    }

    @Override
    public ProfileType getProfileType() {
        return ProfileType.OTN;
    }

    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address) {
        return null;
    }

    @Override
    public Set<Short> getNodeAddresses() {
        return new HashSet<Short>() {{
            add(mNodeAddr);
        }};
    }

    @Override
    public double getCurrentTemp() {
        return otnEquip.getCurrentTemp().readHisVal();
    }

    @Override
    public Equip getEquip()
    {
        HashMap<Object,Object> equip = CCUHsApi.getInstance().readEntity("equip and group == \""+ mNodeAddr+"\"");
        return new Equip.Builder().setHashMap(equip).build();
    }

    private void setStatus(double status, boolean emergency) {
        otnEquip.getEquipStatus().writeHisVal(status);
        String message;
        if (emergency) {
            message = (status == 0 ? "Recirculating Air" : status == 1 ? "Emergency Cooling" :
                    "Emergency Heating");
        } else {
            if (ScheduleManager.getInstance().getSystemOccupancy() == Occupancy.PRECONDITIONING) {
                message = "In Preconditioning ";
            } else {
                message = (status == 0 ? "Recirculating Air" : status == 1 ? "Cooling Space" :
                        "Warming Space");
            }
        }

        String curStatus = otnEquip.getEquipStatusMessage().readDefaultStrVal();
        if (!curStatus.equals(message)) {
            otnEquip.getEquipStatusMessage().writeDefaultVal(message);
        }
    }
}
