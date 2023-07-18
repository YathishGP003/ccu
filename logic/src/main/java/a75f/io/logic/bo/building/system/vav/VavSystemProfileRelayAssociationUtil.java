package a75f.io.logic.bo.building.system.vav;

import java.util.HashMap;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Stage;
import a75f.io.logic.bo.util.TemperatureMode;

public class VavSystemProfileRelayAssociationUtil {
    public static boolean getDesiredTempDisplayMode(TemperatureMode modeType){
        HashMap<Object, Object> equips = CCUHsApi.getInstance().readEntity("equip and system");
        Equip equip = new Equip.Builder().setHashMap(equips).build();
        switch (ProfileType.valueOf(equip.getProfile())) {
            case SYSTEM_VAV_STAGED_RTU:
                VavStagedRtu vavStagedRtu = new VavStagedRtu();
                return getAnyVavStagedRtuRelayAssociation(vavStagedRtu, false, modeType);

            case SYSTEM_VAV_ANALOG_RTU:
                VavFullyModulatingRtu vavFullyModulatingRtu = new VavFullyModulatingRtu();
                return modeType == TemperatureMode.COOLING ? vavFullyModulatingRtu.getConfigEnabled
                        ("analog1") == 1.0 : vavFullyModulatingRtu.getConfigEnabled("analog3") == 1.0;

                case SYSTEM_VAV_STAGED_VFD_RTU:
                VavStagedRtuWithVfd vavStagedRtuWithVfd = new VavStagedRtuWithVfd();
                return getAnyVavStagedRtuRelayAssociation(vavStagedRtuWithVfd, false, modeType);


            case SYSTEM_VAV_HYBRID_RTU:
                VavAdvancedHybridRtu vavAdvancedHybridRtu = new VavAdvancedHybridRtu();
                return getAnyVavStagedRtuRelayAssociation(vavAdvancedHybridRtu, true, modeType);
        }
        return false;
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