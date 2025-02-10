package a75f.io.logic.bo.building.definitions;

/**
 * Created by Yinten on 9/20/2017.
 */

public enum ProfileType
{
    LIGHT, TEST, SSE, HMP,PLC, EMR, DAB, VAV_REHEAT, VAV_SERIES_FAN, VAV_PARALLEL_FAN, OAO,
    SYSTEM_DEFAULT,SYSTEM_VAV_ANALOG_RTU, SYSTEM_VAV_STAGED_RTU,SYSTEM_VAV_HYBRID_RTU, SYSTEM_VAV_STAGED_VFD_RTU,SYSTEM_VAV_IE_RTU, SYSTEM_VAV_BACNET_RTU,
    SYSTEM_DAB_ANALOG_RTU,SYSTEM_DAB_STAGED_RTU,SYSTEM_DAB_HYBRID_RTU, SYSTEM_DAB_STAGED_VFD_RTU,
    SMARTSTAT_CONVENTIONAL_PACK_UNIT, SMARTSTAT_COMMERCIAL_PACK_UNIT, SMARTSTAT_HEAT_PUMP_UNIT, SMARTSTAT_TWO_PIPE_FCU, SMARTSTAT_FOUR_PIPE_FCU,
    TEMP_MONITOR, TEMP_INFLUENCE,DUAL_DUCT,MODBUS_UPS30,MODBUS_UPS400,MODBUS_UPS80,MODBUS_VRF,MODBUS_PAC, MODBUS_WLD,MODBUS_RRS,MODBUS_EM,MODBUS_EMS,MODBUS_ATS,MODBUS_UPS150,MODBUS_BTU,MODBUS_EMR,MODBUS_EMR_ZONE,
    MODBUS_UPS40K,MODBUS_UPSL,MODBUS_UPSV,MODBUS_UPSVL,MODBUS_VAV_BACnet, HYPERSTAT_SENSE, MODBUS_DEFAULT, OTN,
    HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT,HYPERSTAT_HEAT_PUMP_UNIT, HYPERSTAT_TWO_PIPE_FCU, HYPERSTAT_FOUR_PIPE_FCU,
    HYPERSTAT_VRV, HYPERSTAT_MONITORING, HYPERSTATSPLIT_CPU, VAV_ACB, dabExternalAHUController, vavExternalAHUController, BYPASS_DAMPER,
    SYSTEM_VAV_ADVANCED_AHU, SYSTEM_DAB_ADVANCED_AHU, BACNET_DEFAULT;


    public static ProfileType getProfileTypeForName(String name){
        switch (name) {
            case "vavStagedRtu":
                return SYSTEM_VAV_STAGED_RTU;
            case "vavStagedRtuVfdFan":
                return SYSTEM_VAV_STAGED_VFD_RTU;
            case "vavAdvancedHybridAhuV2":
                return SYSTEM_VAV_ADVANCED_AHU;
            case "vavFullyModulatingAhu":
                return SYSTEM_VAV_ANALOG_RTU;
            case "dabAdvancedHybridAhuV2":
                return SYSTEM_DAB_ADVANCED_AHU;
            case "dabStagedRtu":
                return SYSTEM_DAB_STAGED_RTU;
            case "dabStagedRtuVfdFan":
                return SYSTEM_DAB_STAGED_VFD_RTU;
            case "dabFullyModulatingAhu":
                return SYSTEM_DAB_ANALOG_RTU;
            case "SYSTEM_DAB_HYBRID_RTU":
                return SYSTEM_DAB_HYBRID_RTU;
            case "OAO":
                return OAO;
        }
        return null;
    }

    public static String getProfileDescription(ProfileType profileType) {
        if (profileType == null) {
            return "UnknownProfile";
        }

        switch (profileType) {
            case DAB:
                return "DAB";
            case VAV_REHEAT:
                return "VAVReheatNoFan";
            case VAV_ACB:
                return "ActiveChilledBeams + DOAS";
            case VAV_SERIES_FAN:
                return "VAVReheatSeries";
            case VAV_PARALLEL_FAN:
                return "VAVReheatParallel";
            case SSE:
                return "SingleStageEquipment";
            case PLC:
                return "PILoopController";
            case HYPERSTATSPLIT_CPU:
                return "ConventionalPackageUnit & Economizer";
            case HYPERSTAT_MONITORING:
                return "Monitoring";
            case HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT:
                return "ConventionalPackageUnit";
            case HYPERSTAT_HEAT_PUMP_UNIT:
                return "HeatPumpUnit";
            case HYPERSTAT_TWO_PIPE_FCU:
                return "2PipeFCU";
            case OTN:
                return "TemperatureInfluencing";
            default:
                return "UnknownProfile";
        }
    }
}
