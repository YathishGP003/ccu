package a75f.io.logic.bo.building.ccu;


import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.RFDEAD;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.domain.equips.TIEquip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.schedules.Occupancy;
import a75f.io.logic.bo.building.schedules.ScheduleManager;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.dab.DabSystemController;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.tuners.TunerUtil;

//CCU As a Zone Profile
public class TIProfile extends ZoneProfile {
    TIEquip tiEquip;
    Short mNodeAddr;

    public TIProfile(String equipRef, Short addr) {
        mNodeAddr = addr;
        tiEquip = new TIEquip(equipRef);
    }



    @Override
    public ProfileType getProfileType() {
        return ProfileType.TEMP_INFLUENCE;
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
    public Equip getEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \"" + mNodeAddr + "\"");
        return new Equip.Builder().setHashMap(equip).build();
    }

    @Override
    public void updateZonePoints() {
        if(isRFDead()){
            state = RFDEAD;
            String curStatus = tiEquip.getEquipStatus().readDefaultStrVal();
            if (!curStatus.equals(RFDead)) {
                tiEquip.getEquipStatus().writeDefaultVal(RFDead);
                /*CCUHsApi.getInstance().writeDefaultVal("point and status and message and" +
                        " writable and group == \"" + mNodeAddr + "\"", RFDead);*/
            }
            tiEquip.getEquipStatus().writeHisVal(RFDEAD.ordinal());
            /*CCUHsApi.getInstance().writeHisValByQuery("point and not ota and status and" +
                    " his and group == \"" + mNodeAddr + "\"", (double) RFDEAD.ordinal());*/
            return;
        } else if (isZoneDead()) {
            state = TEMPDEAD;
            String curStatus = tiEquip.getEquipStatusMessage().readDefaultStrVal();
                   // CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \"" + mNodeAddr + "\"");
            if (!curStatus.equals("Zone Temp Dead")) {
                tiEquip.getEquipStatusMessage().writeDefaultVal("Zone Temp Dead");
               // CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + mNodeAddr + "\"", "Zone Temp Dead");
            }
            tiEquip.getEquipStatus().writeHisVal(TEMPDEAD.ordinal());
            //CCUHsApi.getInstance().writeHisValByQuery("point and not ota and status and his and group == \"" + mNodeAddr+ "\"", (double) TEMPDEAD.ordinal());
            return;
        }

        double setTempCooling = tiEquip.getDesiredTempCooling().readPriorityVal();
        double setTempHeating = tiEquip.getDesiredTempHeating().readPriorityVal();
        double roomTemp = tiEquip.getCurrentTemp().readHisVal();
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
            CcuLog.d(L.TAG_CCU_ZONE, " TIEquip : systemMode-" + systemMode + " roomTemp:" + roomTemp);
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
        setStatus(state.ordinal(), (DabSystemController.getInstance().isEmergencyMode() || VavSystemController.getInstance().isEmergencyMode()) && (state == HEATING ? buildingLimitMinBreached()
                : state == COOLING ? buildingLimitMaxBreached() : false));

    }

    private void setStatus(double status, boolean emergency) {
        tiEquip.getEquipStatus().writeHisVal(status);
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

        String curStatus = tiEquip.getEquipStatusMessage().readDefaultStrVal();
        if (!curStatus.equals(message)) {
            tiEquip.getEquipStatusMessage().writeDefaultVal(message);
        }
    }


    @Override
    public double getCurrentTemp() {
        return tiEquip.getCurrentTemp().readHisVal();
    }
}
