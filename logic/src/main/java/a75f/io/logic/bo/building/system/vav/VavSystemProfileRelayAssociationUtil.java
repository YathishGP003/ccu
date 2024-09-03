package a75f.io.logic.bo.building.system.vav;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Stage;
import a75f.io.logic.bo.building.system.SystemProfile;
import a75f.io.logic.bo.util.TemperatureMode;

public class VavSystemProfileRelayAssociationUtil {
    public static boolean getDesiredTempDisplayMode(TemperatureMode modeType){
        HashMap<Object, Object> equips = CCUHsApi.getInstance().readEntity("equip and system and not modbus and not connectModule");
        Equip equip = new Equip.Builder().setHashMap(equips).build();
        ProfileType profileType = ProfileType.getProfileTypeForName(equip.getProfile());
        //This will be needed for profiles not migrated to DM.
        if (profileType == null) {
            profileType = ProfileType.valueOf(equip.getProfile());
        }
        SystemProfile profileInstance = L.ccu().systemProfile;
        if (profileInstance == null) {
            return false;
        }
        VavSystemProfile systemProfile = getSystemProfileInstance(profileInstance);


        switch (profileType) {
            case SYSTEM_VAV_STAGED_RTU:
            case SYSTEM_VAV_STAGED_VFD_RTU:
            case SYSTEM_VAV_ANALOG_RTU:
                return modeType == TemperatureMode.COOLING ?
                        systemProfile.isCoolingAvailable() : systemProfile.isHeatingAvailable();
            case SYSTEM_VAV_HYBRID_RTU:
                return getAnyVavStagedRtuRelayAssociation((VavStagedRtu) systemProfile, true, modeType);
        }
        return false;
    }

    private static VavSystemProfile getSystemProfileInstance(SystemProfile systemProfile) {

        if (systemProfile instanceof  VavStagedRtu) {
            return (VavStagedRtu)systemProfile;
        } else if (systemProfile instanceof VavStagedRtuWithVfd) {
            return (VavStagedRtuWithVfd)systemProfile;
        } else if (systemProfile instanceof VavFullyModulatingRtu) {
            return  (VavFullyModulatingRtu)systemProfile;
        } else if (systemProfile instanceof VavAdvancedHybridRtu) {
            return  (VavAdvancedHybridRtu)systemProfile;
        }
        return (VavSystemProfile) systemProfile;
    }

    private static boolean getAnyVavStagedRtuRelayAssociation(VavStagedRtu vavStagedRtu, Boolean isVavHybridRtu, TemperatureMode modeType){
        int numberOfRelaysInVavStagedRtu = 7;
        if(modeType == TemperatureMode.COOLING) {
            for (int i = 1; i <= numberOfRelaysInVavStagedRtu; i++) {
                if (vavStagedRtu.getConfigEnabled("relay" + i) > 0 && (isRelayAssociatedToCoolingStage((int) vavStagedRtu.getConfigAssociation("relay" + i)))
                        || getAnalogAssociationForCooling(isVavHybridRtu)) {
                    return true;
                }
            }
            return false;
        }
        if(modeType == TemperatureMode.HEATING){
            for (int i = 1; i <= numberOfRelaysInVavStagedRtu; i++) {
                if (vavStagedRtu.getConfigEnabled("relay" + i) > 0 && (isRelayAssociatedToHeatingStage((int) vavStagedRtu.getConfigAssociation("relay" + i)))
                || getAnalogAssociationForHeating(isVavHybridRtu)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private static boolean getAnalogAssociationForCooling(Boolean isVavHybridRtu) {
        if(isVavHybridRtu) {
            return getConfigVal("system and analog1 and enabled", CCUHsApi.getInstance()) > 0;
        }
        return false;
    }

    private static boolean getAnalogAssociationForHeating(Boolean isVavHybridRtu) {
        if(isVavHybridRtu) {
            return getConfigVal("system and analog3 and enabled", CCUHsApi.getInstance()) > 0;
        }
        return false;
    }

    private static double getConfigVal(String tags,CCUHsApi ccuHsApi) {
        HashMap<Object, Object> configPoint = ccuHsApi.readEntity(tags);
        if (configPoint.isEmpty()) {
            CcuLog.e(L.TAG_CCU_SYSTEM," !!!  System config point does not exist !!! - "+tags);
            return 0;
        }
        return ccuHsApi.readPointPriorityVal(configPoint.get("id").toString());
    }

    private static boolean isRelayAssociatedToCoolingStage(Integer state) {
        return (state == Stage.COOLING_1.ordinal() || state == Stage.COOLING_2.ordinal() ||
                state == Stage.COOLING_3.ordinal() || state == Stage.COOLING_4.ordinal() ||
                state == Stage.COOLING_5.ordinal());
    }

    private static boolean isRelayAssociatedToHeatingStage(Integer state) {
        return (state == Stage.HEATING_1.ordinal() || state == Stage.HEATING_2.ordinal() ||
                state == Stage.HEATING_3.ordinal() || state == Stage.HEATING_4.ordinal() ||
                state == Stage.HEATING_5.ordinal());
    }
}