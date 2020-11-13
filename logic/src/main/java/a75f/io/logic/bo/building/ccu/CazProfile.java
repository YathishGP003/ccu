package a75f.io.logic.bo.building.ccu;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemMode;
import a75f.io.logic.bo.building.system.dab.DabSystemController;
import a75f.io.logic.bo.building.system.vav.VavSystemController;
import a75f.io.logic.tuners.TunerUtil;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.HEATING;
import static a75f.io.logic.bo.building.ZoneState.TEMPDEAD;

//CCU As a Zone Profile
public class CazProfile extends ZoneProfile {
    CazEquip cazEquip;

    public void addCcuAsZoneEquip(short addr, CazProfileConfig config, String floorRef, String roomRef) {
        cazEquip = new CazEquip(getProfileType(), addr);
        cazEquip.createEntities(config, floorRef, roomRef);
        cazEquip.init();
    }

    public void addCcuAsZoneEquip(short addr) {
        cazEquip = new CazEquip(getProfileType(), addr);
        cazEquip.init();
    }

    public void updateCcuAsZone(CazProfileConfig config) {
        cazEquip.updateCcuAsZoneConfig(config);
    }

    @Override
    public ProfileType getProfileType() {
        return ProfileType.TEMP_INFLUENCE;
    }

    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address) {
        return cazEquip.getProfileConfiguration();
    }

    @Override
    public Set<Short> getNodeAddresses() {
        return new HashSet<Short>() {{
            add((short) cazEquip.nodeAddr);
        }};
    }

    @Override
    public Equip getEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \"" + cazEquip.nodeAddr + "\"");
        return new Equip.Builder().setHashMap(equip).build();
    }

    @Override
    public boolean isZoneDead() {

        double buildingLimitMax = TunerUtil.readBuildingTunerValByQuery("building and limit and max");
        double buildingLimitMin = TunerUtil.readBuildingTunerValByQuery("building and limit and min");

        double tempDeadLeeway = TunerUtil.readBuildingTunerValByQuery("temp and dead and leeway");
        CcuLog.d(L.TAG_CCU_ZONE, " roomTemp : " + cazEquip.getCurrentTemp() + " buildingLimitMax:" + buildingLimitMax + " tempDead:" + tempDeadLeeway);
        CcuLog.d(L.TAG_CCU_ZONE, " roomTemp : " + cazEquip.getCurrentTemp() + " buildingLimitMin:" + buildingLimitMin + " tempDead:" + tempDeadLeeway);
        if (cazEquip.getCurrentTemp() > (buildingLimitMax + tempDeadLeeway)
                || cazEquip.getCurrentTemp() < (buildingLimitMin - tempDeadLeeway)) {
            return true;
        }

        return false;
    }

    @Override
    public void updateZonePoints() {
        if (isZoneDead()) {
            state = TEMPDEAD;
            String curStatus = CCUHsApi.getInstance().readDefaultStrVal("point and status and message and writable and group == \"" + cazEquip.nodeAddr + "\"");
            if (!curStatus.equals("Zone Temp Dead")) {
                CCUHsApi.getInstance().writeDefaultVal("point and status and message and writable and group == \"" + cazEquip.nodeAddr + "\"", "Zone Temp Dead");
            }
            CCUHsApi.getInstance().writeHisValByQuery("point and status and his and group == \"" + cazEquip.nodeAddr + "\"", (double) TEMPDEAD.ordinal());
            return;
        }

        double setTempCooling = cazEquip.getDesiredTempCooling();
        double setTempHeating = cazEquip.getDesiredTempHeating();
        double roomTemp = cazEquip.getCurrentTemp();
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
           /* if (state != DEADBAND) {
                state = DEADBAND;
            }*/
        }
        cazEquip.setStatus(state.ordinal(), (DabSystemController.getInstance().isEmergencyMode() || VavSystemController.getInstance().isEmergencyMode()) && (state == HEATING ? buildingLimitMinBreached()
                : state == COOLING ? buildingLimitMaxBreached() : false));

    }

    @Override
    public void reset() {
        cazEquip.setCurrentTemp(0);
    }
}
