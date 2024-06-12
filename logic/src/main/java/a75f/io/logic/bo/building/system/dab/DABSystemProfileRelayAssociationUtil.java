package a75f.io.logic.bo.building.system.dab;

import android.util.Log;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Stage;
import a75f.io.logic.bo.util.TemperatureMode;

public class DABSystemProfileRelayAssociationUtil {
    public static boolean getDesiredTempDisplayMode(TemperatureMode modeType){
        HashMap<Object, Object> equip = CCUHsApi.getInstance().readEntity("equip and system and not modbus and not connectModule");
        Equip eq = new Equip.Builder().setHashMap(equip).build();
        switch (ProfileType.valueOf(eq.getProfile())) {
            case SYSTEM_DAB_STAGED_RTU:
                DabStagedRtu dabStagedRtu = new DabStagedRtu();
                return getAnyDabStagedRtuRelayAssociation(dabStagedRtu, false,
                        modeType);

            case SYSTEM_DAB_ANALOG_RTU:
                DabFullyModulatingRtu dabFullyModulatingRtu = new DabFullyModulatingRtu();
                return modeType == TemperatureMode.COOLING ? dabFullyModulatingRtu.getConfigEnabled
                        ("analog1") == 1.0 : dabFullyModulatingRtu.getConfigEnabled("analog3") == 1.0;

            case SYSTEM_DAB_STAGED_VFD_RTU:
                DabStagedRtuWithVfd dabStagedRtuWithVfd = new DabStagedRtuWithVfd();
                return getAnyDabStagedRtuRelayAssociation(dabStagedRtuWithVfd,
                        false, modeType);

            case SYSTEM_DAB_HYBRID_RTU:
                DabAdvancedHybridRtu dabAdvancedHybridRtu = new DabAdvancedHybridRtu();
                return getAnyDabStagedRtuRelayAssociation(dabAdvancedHybridRtu,
                        true, modeType);

        }
        return false;
    }

    private static boolean getAnyDabStagedRtuRelayAssociation(DabStagedRtu dabStagedRtu,
                                                              Boolean isDavHybridRtu,
                                                              TemperatureMode modeType){
        int numberOfRelaysInVavStagedRtu = 7;
        if(modeType == TemperatureMode.COOLING) {
            for (int i = 1; i <= numberOfRelaysInVavStagedRtu; i++) {
                if ((dabStagedRtu.getConfigEnabled("relay" + i) > 0 && (isRelayAssociatedToCoolingStage((int)
                        dabStagedRtu.getConfigAssociation("relay" + i))))
                        || getAnalogAssociationForCooling(isDavHybridRtu)) {
                    return true;
                }
            }
            return false;
        }
        if(modeType == TemperatureMode.HEATING){
            for (int i = 1; i <= numberOfRelaysInVavStagedRtu; i++) {
                if (dabStagedRtu.getConfigEnabled("relay" + i) > 0 && (isRelayAssociatedToHeatingStage((int)
                        dabStagedRtu.getConfigAssociation("relay" + i)))
                        || getAnalogAssociationForHeating(isDavHybridRtu)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private static boolean isRelayAssociatedToHeatingStage(int state) {
        return (state == Stage.HEATING_1.ordinal() || state == Stage.HEATING_2.ordinal() ||
                state == Stage.HEATING_3.ordinal() || state == Stage.HEATING_4.ordinal() ||
                state == Stage.HEATING_5.ordinal());
    }

    private static boolean isRelayAssociatedToCoolingStage(Integer state) {
        return (state == Stage.COOLING_1.ordinal() || state == Stage.COOLING_2.ordinal() ||
                state == Stage.COOLING_3.ordinal() || state == Stage.COOLING_4.ordinal() ||
                state == Stage.COOLING_5.ordinal());
    }

    private static boolean getAnalogAssociationForCooling(boolean isDabHybridRtu) {
        if(isDabHybridRtu) {
            return getConfigVal("system and analog1 and enabled", CCUHsApi.getInstance()) > 0;
        }
        return false;
    }

    private static boolean getAnalogAssociationForHeating(boolean isDabHybridRtu) {
        if(isDabHybridRtu) {
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

}
