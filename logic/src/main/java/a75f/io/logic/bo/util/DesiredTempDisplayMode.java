package a75f.io.logic.bo.util;

import static a75f.io.logic.bo.building.definitions.ProfileType.HYPERSTATSPLIT_2PIPE_UV;
import static a75f.io.logic.bo.building.definitions.ProfileType.HYPERSTATSPLIT_4PIPE_UV;
import static a75f.io.logic.bo.building.definitions.ProfileType.SMARTSTAT_FOUR_PIPE_FCU;
import static a75f.io.logic.bo.building.definitions.ProfileType.SSE;
import static a75f.io.logic.bo.building.hvac.StandaloneConditioningMode.AUTO;
import static a75f.io.logic.bo.building.hvac.StandaloneConditioningMode.COOL_ONLY;
import static a75f.io.logic.bo.building.hvac.StandaloneConditioningMode.HEAT_ONLY;
import static a75f.io.logic.bo.building.hvac.StandaloneConditioningMode.OFF;
import static a75f.io.logic.bo.building.statprofiles.util.HSUtilKt.getHsConfiguration;
import static a75f.io.logic.bo.building.statprofiles.util.MSUtilKt.getMyStatConfiguration;
import static a75f.io.logic.bo.building.statprofiles.util.SplitUtilKt.getSplitConfiguration;
import static a75f.io.logic.bo.building.statprofiles.util.SplitUtilKt.getUvPossibleConditioningMode;
import static a75f.io.logic.bo.util.TemperatureMode.COOLING;
import static a75f.io.logic.bo.util.TemperatureMode.DUAL;
import static a75f.io.logic.bo.util.TemperatureMode.HEATING;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Tags;
import a75f.io.domain.api.DomainName;
import a75f.io.domain.equips.hyperstatsplit.HsSplitCpuEquip;
import a75f.io.domain.util.CommonQueries;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode;
import a75f.io.logic.bo.building.ss4pfcu.FourPipeFanCoilUnitConfigurationUtil;
import a75f.io.logic.bo.building.ss4pfcu.FourPipeFanCoilUnitEquip;
import a75f.io.logic.bo.building.sscpu.ConventionalUnitLogicalMap;
import a75f.io.logic.bo.building.sscpu.SmartStatAssociationUtil;
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.CpuConfiguration;
import a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs.HsPipe4Configuration;
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HyperStatSplitAssociationUtil;
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration;
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.UnitVentilatorConfiguration;
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatCpuConfiguration;
import a75f.io.logic.bo.building.statprofiles.mystat.configs.MyStatPipe4Configuration;
import a75f.io.logic.bo.building.statprofiles.util.PossibleConditioningMode;
import a75f.io.logic.bo.building.system.dab.DABSystemProfileRelayAssociationUtil;
import a75f.io.logic.bo.building.system.vav.VavSystemProfileRelayAssociationUtil;

public class DesiredTempDisplayMode {

    // Below method is called from the places where new profiles configurations are created or updated.
    public static void setModeType(String roomRef, CCUHsApi ccuHsApi) {
        ArrayList<Equip> zoneEquips = HSUtil.getEquips(roomRef);
        TemperatureMode modeType = getDesiredTempDisplayMode(zoneEquips, ccuHsApi);
        if (modeType.equals(DUAL)) {
            TemperatureMode modeToset = modeType;
            //Check if any systemProfile is present in the zone
            if (!hasSystemEquip(zoneEquips)) {
                modeToset = getConditioningModeForZone(roomRef, ccuHsApi);
            }
            CcuLog.d(L.TAG_CCU, "Temperature modeType : " + modeToset + " equip : " + roomRef);
            ccuHsApi.writeHisValByQuery("hvacMode and roomRef == \""
                    + roomRef + "\"", (double) modeToset.ordinal());
        } else {
            CcuLog.d(L.TAG_CCU, "Temperature modeType : " + modeType + " equip : " + roomRef);
            ccuHsApi.writeHisValByQuery("hvacMode and roomRef == \""
                    + roomRef + "\"", (double) modeType.ordinal());
        }
    }

    public static void setModeTypeOnUserIntentChange(String roomRef, CCUHsApi ccuHsApi) {
        ArrayList<Equip> zoneEquips = HSUtil.getEquips(roomRef);
        TemperatureMode modeType = getDesiredTempDisplayMode(zoneEquips, ccuHsApi);
        if (modeType.equals(DUAL)) {
            TemperatureMode modeToset = modeType;
            //Check if any systemProfile is present in the zone
            if (!hasSystemEquip(zoneEquips)) {
                modeToset = getConditioningModeForZone(roomRef, ccuHsApi);
            }
            ccuHsApi.writeHisValByQuery("hvacMode and roomRef == \""
                    + roomRef + "\"", (double) modeToset.ordinal());
        } else {
            ccuHsApi.writeHisValByQuery("hvacMode and roomRef == \""
                    + roomRef + "\"", (double) modeType.ordinal());
        }
    }

    private static boolean hasSystemEquip(ArrayList<Equip> zoneEquips) {
        for (Equip mEquip : zoneEquips) {
            if (mEquip.getMarkers().contains(Tags.VAV) ||
                    mEquip.getMarkers().contains(Tags.DAB)) {
                return true;
            }
        }
        return false;
    }

    private static TemperatureMode getConditioningModeForZone(String roomRef, CCUHsApi ccuHsApi) {
        ArrayList<HashMap<Object, Object>> conditioningMode = ccuHsApi.readAllEntities(
                "conditioning and mode and not access and roomRef == \"" + roomRef + "\"");
        List<StandaloneConditioningMode> standaloneConditioningModes = new ArrayList<>();
        conditioningMode.forEach(conditioningModeMap ->
                standaloneConditioningModes.add(StandaloneConditioningMode.values()[(int)
                        (ccuHsApi.readPointPriorityVal(conditioningModeMap.get("id").toString()))]));
        return conditioningMode(standaloneConditioningModes);
    }

    private static TemperatureMode conditioningMode(List<StandaloneConditioningMode> standaloneConditioningModes) {
        boolean hasAuto = false;
        boolean hasHeatOnly = false;
        boolean hasCoolOnly = false;

        for (StandaloneConditioningMode mode : standaloneConditioningModes) {
            if (mode == AUTO) {
                hasAuto = true;
            } else if (mode == HEAT_ONLY) {
                hasHeatOnly = true;
            } else if (mode == COOL_ONLY) {
                hasCoolOnly = true;
            }
        }

        if (hasAuto || standaloneConditioningModes.contains(OFF)) {
            return DUAL;
        } else if (hasHeatOnly && hasCoolOnly) {
            return DUAL;
        } else if (hasCoolOnly) {
            return COOLING;
        } else if (hasHeatOnly) {
            return HEATING;
        }
        return DUAL;
    }


    public static TemperatureMode getDesiredTempDisplayMode(ArrayList<Equip> zoneEquips, CCUHsApi ccuHsApi) {
        List<TemperatureMode> temperatureModes = new ArrayList<>();
        for (Equip mEquip : zoneEquips) {
            //  As per spec for below profiles need to show both heating and cooling desired temperature
            if (mEquip.getProfile().equalsIgnoreCase(ProfileType.HYPERSTAT_TWO_PIPE_FCU.name()) ||
                    mEquip.getProfile().equalsIgnoreCase(ProfileType.SMARTSTAT_TWO_PIPE_FCU.name()) ||
                    mEquip.getProfile().equalsIgnoreCase(ProfileType.SMARTSTAT_HEAT_PUMP_UNIT.name()) ||
                    mEquip.getProfile().equalsIgnoreCase(ProfileType.HYPERSTAT_HEAT_PUMP_UNIT.name()) ||
                    mEquip.getProfile().equalsIgnoreCase(ProfileType.MYSTAT_PIPE2.name()) ||
                    mEquip.getProfile().equalsIgnoreCase(ProfileType.MYSTAT_HPU.name()) ||
                    mEquip.getProfile().equalsIgnoreCase(HYPERSTATSPLIT_2PIPE_UV.name())) {
                return DUAL;
            } else if (mEquip.getProfile().equalsIgnoreCase(ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT.name())) {
                TemperatureMode temperatureMode = getTemperatureModeForHSCPU(mEquip);
                temperatureModes.add(temperatureMode);
            } else if (mEquip.getProfile().equalsIgnoreCase(ProfileType.HYPERSTAT_FOUR_PIPE_FCU.name())) {
                TemperatureMode temperatureMode = getTemperatureModeForHSPIPE4(mEquip);
                temperatureModes.add(temperatureMode);
            } else if (mEquip.getProfile().equalsIgnoreCase(ProfileType.HYPERSTATSPLIT_CPU.name())) {
                TemperatureMode temperatureMode = getTemperatureModeForHSSplitCPUEcon(mEquip);
                temperatureModes.add(temperatureMode);
            } else if (mEquip.getProfile().equalsIgnoreCase(ProfileType.MYSTAT_CPU.name())) {
                TemperatureMode temperatureMode = getTemperatureModeForMSCPU(mEquip);
                temperatureModes.add(temperatureMode);
            } else if (mEquip.getProfile().equalsIgnoreCase(ProfileType.SMARTSTAT_CONVENTIONAL_PACK_UNIT.name())) {
                TemperatureMode temperatureMode = getTemperatureModeForSSCPU(mEquip);
                temperatureModes.add(temperatureMode);
            } else if (mEquip.getProfile().equalsIgnoreCase(SMARTSTAT_FOUR_PIPE_FCU.name())) {
                TemperatureMode temperatureMode = getTemperatureModeForSSFCU(mEquip);
                temperatureModes.add(temperatureMode);
            } else if (mEquip.getProfile().equalsIgnoreCase(ProfileType.VAV_ACB.name())) {
                TemperatureMode temperatureMode = getTemperatureModeForAcbProfile();
                temperatureModes.add(temperatureMode);
            } else if (mEquip.getMarkers().contains(Tags.VAV)) {
                TemperatureMode temperatureMode = getTemperatureModeForVavProfile(mEquip, ccuHsApi);
                temperatureModes.add(temperatureMode);
            } else if (mEquip.getMarkers().contains(Tags.TI) || mEquip.getMarkers().contains(Tags.OTN)) {
                TemperatureMode temperatureMode = getTemperatureModeForTiOrOtnProfile(ccuHsApi);
                temperatureModes.add(temperatureMode);
            } else if (mEquip.getMarkers().contains(Tags.DAB)) {
                TemperatureMode temperatureMode = getTemperatureModeForDabProfile(mEquip, ccuHsApi);
                temperatureModes.add(temperatureMode);
            } else if (mEquip.getMarkers().contains(Tags.DUALDUCT)) {
                TemperatureMode temperatureMode = getTemperatureModeForDualDuctProfile();
                temperatureModes.add(temperatureMode);
            } else if (mEquip.getProfile().equalsIgnoreCase(SSE.name())) {
                TemperatureMode temperatureMode = getTemperatureModeForSSEProfile(mEquip, ccuHsApi);
                temperatureModes.add(temperatureMode);
            } else if (mEquip.getProfile().equalsIgnoreCase(HYPERSTATSPLIT_4PIPE_UV.name())) {
                TemperatureMode temperatureMode = getTempModeForUnitVentilatorProfile(mEquip);
                temperatureModes.add(temperatureMode);
            } else if (mEquip.getProfile().equalsIgnoreCase(ProfileType.MYSTAT_PIPE4.name())) {
                TemperatureMode temperatureMode = getTemperatureModeForMSPipe4(mEquip);
                temperatureModes.add(temperatureMode);
            } else {
                CcuLog.w(L.TAG_CCU, "Unknown profile for equip: " + mEquip.getId() + ", profile: " + mEquip.getProfile());
            }
        }
        return getZoneTemperatureMode(temperatureModes);
    }

    private static TemperatureMode getTemperatureModeForSSEProfile(Equip mEquip, CCUHsApi ccuHsApi) {
        boolean heating = false;
        boolean cooling = false;
        int relay1OutputAssociationState = ccuHsApi.readDefaultVal("point and domainName == \"" + DomainName.relay1OutputAssociation + "\"" +
                " and equipRef == \"" + mEquip.getId() + "\"").intValue();
        int isRelay1Enabled = ccuHsApi.readDefaultVal("point and domainName == \"" + DomainName.relay1OutputEnable + "\"" +
                " and equipRef == \"" + mEquip.getId() + "\"").intValue();
        if (isRelay1Enabled == 1) {
            if (relay1OutputAssociationState == 0) {
                heating = true;
            } else if (relay1OutputAssociationState == 1) {
                cooling = true;
            }
        } else {
            return DUAL;
        }
        return getTemperatureMode(heating, cooling);
    }

    private static TemperatureMode getTemperatureModeForDualDuctProfile() {
        boolean cooling = DABSystemProfileRelayAssociationUtil.getDesiredTempDisplayMode(TemperatureMode.COOLING);
        boolean heating = (DABSystemProfileRelayAssociationUtil.getDesiredTempDisplayMode(TemperatureMode.HEATING));
        return getTemperatureMode(heating, cooling);
    }

    private static TemperatureMode getTemperatureModeForDabProfile(Equip mEquip, CCUHsApi ccuHsApi) {
        boolean isCooling = DABSystemProfileRelayAssociationUtil.getDesiredTempDisplayMode(TemperatureMode.COOLING);
        boolean isHeating = DABSystemProfileRelayAssociationUtil.getDesiredTempDisplayMode(TemperatureMode.HEATING);
        boolean isReheatEnabled = ccuHsApi.readDefaultVal("reheat and type and equipRef == \"" +
                mEquip.getId() + "\"").intValue() > 0;
        return getTemperatureModeForSystemProfile(isCooling, isHeating, isReheatEnabled);
    }

    private static TemperatureMode getTemperatureModeForTiOrOtnProfile(CCUHsApi ccuHsApi) {
        boolean heating = false;
        boolean cooling = false;
        Equip systemEquip = new Equip.Builder()
                .setHashMap(ccuHsApi.readEntity(CommonQueries.SYSTEM_PROFILE)).build();
        if (systemEquip.getMarkers().contains(Tags.DAB)) {
            if (DABSystemProfileRelayAssociationUtil.getDesiredTempDisplayMode(
                    TemperatureMode.COOLING)) {
                cooling = true;
            }
            if (DABSystemProfileRelayAssociationUtil.getDesiredTempDisplayMode(
                    TemperatureMode.HEATING)) {
                heating = true;
            }
            return getTemperatureMode(heating, cooling);
        } else if (systemEquip.getMarkers().contains(Tags.VAV)) {
            if (VavSystemProfileRelayAssociationUtil.getDesiredTempDisplayMode(
                    TemperatureMode.COOLING)) {
                cooling = true;
            }
            if (VavSystemProfileRelayAssociationUtil.getDesiredTempDisplayMode(
                    TemperatureMode.HEATING)) {
                heating = true;
            }
            return getTemperatureMode(heating, cooling);
        } else {
            // For Default system profile we show both heating and cooling
            return DUAL;
        }
    }

    private static TemperatureMode getTemperatureModeForAcbProfile() {
        boolean isCooling = VavSystemProfileRelayAssociationUtil.getDesiredTempDisplayMode(
                TemperatureMode.COOLING);
        boolean isHeating = VavSystemProfileRelayAssociationUtil.getDesiredTempDisplayMode(
                TemperatureMode.HEATING);
        return getTemperatureModeForSystemProfile(isCooling, isHeating, false);
    }

    private static TemperatureMode getTemperatureModeForVavProfile(Equip mEquip, CCUHsApi ccuHsApi) {
        boolean isCooling = VavSystemProfileRelayAssociationUtil.getDesiredTempDisplayMode(
                TemperatureMode.COOLING);
        boolean isHeating = VavSystemProfileRelayAssociationUtil.getDesiredTempDisplayMode(
                TemperatureMode.HEATING);
        boolean isReheatEnabled = ccuHsApi.readDefaultVal("reheat and type and equipRef == \"" +
                mEquip.getId() + "\"").intValue() > 0;
        return getTemperatureModeForSystemProfile(isCooling, isHeating, isReheatEnabled);
    }

    private static TemperatureMode getTemperatureModeForSystemProfile(boolean isCooling, boolean isHeating,
                                                                      boolean isReheatEnabled) {
        boolean heating = false;
        boolean cooling = false;
        if (!isCooling && !isHeating) {
            heating = true;
            cooling = true;
        }
        if (isHeating && !isCooling) {
            heating = true;
        }
        if (isCooling && !isReheatEnabled) {
            cooling = true;
        }
        if (isCooling && isReheatEnabled) {
            heating = true;
            cooling = true;
        }
        if (isCooling && isHeating) {
            heating = true;
            cooling = true;
        }
        return getTemperatureMode(heating, cooling);
    }

    private static TemperatureMode getTemperatureModeForSSFCU(Equip mEquip) {
        boolean heating = false;
        boolean cooling = false;
        FourPipeFanCoilUnitEquip fourPipeFanCoilUnitConfiguration = new
                FourPipeFanCoilUnitEquip(SMARTSTAT_FOUR_PIPE_FCU, Short.parseShort(mEquip.getGroup()));

        if (FourPipeFanCoilUnitConfigurationUtil.Companion.isAnyRelayEnabledAssociatedToCooling(
                fourPipeFanCoilUnitConfiguration.getWaterValueConfigurations())) {
            cooling = true;
        }
        if (FourPipeFanCoilUnitConfigurationUtil.Companion.isAnyRelayEnabledAssociatedToHeating(
                fourPipeFanCoilUnitConfiguration.getWaterValueConfigurations())) {
            heating = true;
        }
        return getTemperatureForStandaloneBasedOnConditioningMode(getTemperatureMode(heating, cooling), mEquip);
    }

    private static TemperatureMode getTemperatureModeForSSCPU(Equip mEquip) {
        boolean heating = false;
        boolean cooling = false;
        ConventionalUnitLogicalMap conventionalUnitConfiguration = new ConventionalUnitLogicalMap(
                ProfileType.SMARTSTAT_CONVENTIONAL_PACK_UNIT,
                Short.parseShort(mEquip.getGroup()));
        if (SmartStatAssociationUtil.Companion.isAnyRelayEnabledAssociatedToCooling(
                conventionalUnitConfiguration.getRelayConfiguration())) {
            cooling = true;
        }
        if (SmartStatAssociationUtil.Companion.isAnyRelayEnabledAssociatedToHeating(
                conventionalUnitConfiguration.getRelayConfiguration())) {
            heating = true;
        }
        return getTemperatureForStandaloneBasedOnConditioningMode(getTemperatureMode(heating, cooling), mEquip);
    }

    private static TemperatureMode getTemperatureModeForHSCPU(Equip mEquip) {
        CpuConfiguration cpuConfiguration = (CpuConfiguration) getHsConfiguration(mEquip.getId());
        return getTemperatureForStandaloneBasedOnConditioningMode(getTemperatureMode(cpuConfiguration.isHeatingAvailable(), cpuConfiguration.isCoolingAvailable()), mEquip);
    }
    private static TemperatureMode getTemperatureModeForHSPIPE4(Equip mEquip) {
        HsPipe4Configuration pipe4Configuration = (HsPipe4Configuration) getHsConfiguration(mEquip.getId());
        return getTemperatureForStandaloneBasedOnConditioningMode(getTemperatureMode(pipe4Configuration.isHeatingAvailable(), pipe4Configuration.isCoolingAvailable()), mEquip);
    }

    private static TemperatureMode getTemperatureModeForMSCPU(Equip mEquip) {
        MyStatCpuConfiguration cpuConfiguration = (MyStatCpuConfiguration) getMyStatConfiguration(mEquip.getId());
        return getTemperatureForStandaloneBasedOnConditioningMode(getTemperatureMode(cpuConfiguration.isHeatingAvailable(), cpuConfiguration.isCoolingAvailable()), mEquip);
    }

    private static TemperatureMode getTemperatureModeForMSPipe4(Equip mEquip) {
        MyStatPipe4Configuration pipe4Configuration = (MyStatPipe4Configuration) getMyStatConfiguration(mEquip.getId());
        return getTemperatureForStandaloneBasedOnConditioningMode(getTemperatureMode(pipe4Configuration.isHeatingAvailable(), pipe4Configuration.isCoolingAvailable()), mEquip);
    }

    private static TemperatureMode getTemperatureForStandaloneBasedOnConditioningMode(TemperatureMode temperatureMode, Equip equip) {

        HashMap<Object, Object> conditioningModeEntity = CCUHsApi.getInstance().readEntity(
                "conditioning and mode and not access and equipRef == \"" + equip.getId() + "\"");
        StandaloneConditioningMode conditioningMode = StandaloneConditioningMode.values()[(int)
                (CCUHsApi.getInstance().readPointPriorityVal(conditioningModeEntity.get("id").toString()))];
        if ((conditioningMode == AUTO || (conditioningMode == OFF && temperatureMode == DUAL))) {
            return DUAL;
        } else if (temperatureMode == HEATING && (conditioningMode == HEAT_ONLY || conditioningMode == OFF)) {
            return HEATING;
        } else if (temperatureMode == COOLING && (conditioningMode == COOL_ONLY || conditioningMode == OFF)) {
            return COOLING;
        }
        return DUAL;
    }

    private static TemperatureMode getTemperatureModeForHSSplitCPUEcon(Equip mEquip) {
        HyperStatSplitCpuConfiguration config = (HyperStatSplitCpuConfiguration) getSplitConfiguration(mEquip.getId());
        if (config == null) {
            CcuLog.w(L.TAG_CCU, "Configuration is null for equip: " + mEquip.getId());
            return DUAL;
        }
        return getTemperatureForStandaloneBasedOnConditioningMode(getTemperatureMode(config.isHeatingAvailable(), config.isCoolingAvailable()), mEquip);
    }

    public static void setSystemModeForVav(CCUHsApi ccuHsApi) {
        ArrayList<HashMap<Object, Object>> vavEquips = ccuHsApi.readAllEntities(
                "equip and vav and not system");
        if (!vavEquips.isEmpty()) {
            vavEquips.forEach(equip -> {
                Equip actualEquip = new Equip.Builder().setHashMap(equip).build();
                setModeType(actualEquip.getRoomRef(), ccuHsApi);
            });
        }
        setTiAndOtnMode(ccuHsApi);
    }

    public static void setSystemModeForDab(CCUHsApi ccuHsApi) {
        ArrayList<HashMap<Object, Object>> dabEquips = ccuHsApi.readAllEntities(
                "equip and dab and not system");
        if (!dabEquips.isEmpty()) {
            dabEquips.forEach(equip -> {
                Equip actualEquip = new Equip.Builder().setHashMap(equip).build();
                setModeType(actualEquip.getRoomRef(), ccuHsApi);
            });
        }
        setTiAndOtnMode(ccuHsApi);
        setDualDuctMode(ccuHsApi);
    }

    public static void setSystemModeForDefaultSystemProfile(CCUHsApi ccuHsApi) {
        setTiAndOtnMode(ccuHsApi);
        setDualDuctMode(ccuHsApi);
    }

    private static void setTiAndOtnMode(CCUHsApi ccuHsApi) {
        ArrayList<HashMap<Object, Object>> tiProfile = ccuHsApi.readAllEntities(
                "equip and ti");
        tiProfile.forEach(equip -> {
            Equip actualEquip = new Equip.Builder().setHashMap(equip).build();
            setModeType(actualEquip.getRoomRef(), ccuHsApi);
        });

        ArrayList<HashMap<Object, Object>> otnProfile = CCUHsApi.getInstance().readAllEntities(
                "equip and otn");
        otnProfile.forEach(equip -> {
            Equip actualEquip = new Equip.Builder().setHashMap(equip).build();
            setModeType(actualEquip.getRoomRef(), ccuHsApi);
        });
    }

    private static void setDualDuctMode(CCUHsApi ccuHsApi) {
        ArrayList<HashMap<Object, Object>> dualDuctProfile = CCUHsApi.getInstance().readAllEntities(
                "equip and dualDuct");
        dualDuctProfile.forEach(equip -> {
            Equip actualEquip = new Equip.Builder().setHashMap(equip).build();
            setModeType(actualEquip.getRoomRef(), ccuHsApi);
        });
    }

    public static void setSystemModeForStandaloneProfile(CCUHsApi ccuHsApi) {
        ArrayList<HashMap<Object, Object>> standaloneEquips = ccuHsApi.readAllEntities("equip and (hyperstat or smartstat or hyperstatsplit) and roomRef  and not modbus and not bacnetDeviceId");
        List<String> roomRefs = getRoomRefsForAllStandaloneProfiles(standaloneEquips);
        for (String roomRef : roomRefs) {
            setModeTypeOnUserIntentChange(roomRef, ccuHsApi);
        }
    }

    private static List<String> getRoomRefsForAllStandaloneProfiles(ArrayList<HashMap<Object, Object>> standaloneEquips) {
        Set<String> roomRefs = new HashSet<>(); // Using set to avoid duplicate
        standaloneEquips.forEach(standaloneEquip -> roomRefs.add(standaloneEquip.get("roomRef").toString()));
        return new ArrayList<>(roomRefs);
    }

    public static String setPointStatusMessage(String status, TemperatureMode modeType) {
        boolean statusContainsChar;
        if (modeType == DUAL) {
            return status;
        }
        // below code checks whether string has "-" to divide Two units
        statusContainsChar = status.contains("F");
        if (!statusContainsChar) {
            return status;
        }
        String[] statusString = status.split(" ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < statusString.length; i++) {
            if (statusString[i].contains("°")) {
                String[] arrOfStr = statusString[i].split("-");
                if (modeType == TemperatureMode.HEATING) {
                    statusString[i] = arrOfStr[0] + "°F";
                } else {
                    /*This is safe condition when status message contains single Temperature
                    This happens when status message fetched from haystack that CCU sets based on single Temp mode.
                    ex: ,changes to 70F at 8:00*/
                    if (arrOfStr.length < 2) {
                        statusString[i] = arrOfStr[0];
                    } else {
                        statusString[i] = arrOfStr[1];
                    }
                }
            }
        }
        for (String string : statusString) {
            sb.append(string);
            sb.append(" ");
        }
        return sb.toString();
    }

    private static TemperatureMode getTemperatureMode(boolean heating, boolean cooling) {
        if (heating && cooling) {
            return DUAL;
        } else if (cooling) {
            return TemperatureMode.COOLING;
        } else if (heating) {
            return TemperatureMode.HEATING;
        }
        return DUAL;
    }

    private static TemperatureMode getZoneTemperatureMode(List<TemperatureMode> temperatureModes) {
        boolean heating = false;
        boolean cooling = false;
        for (TemperatureMode temperatureMode : temperatureModes) {
            if (temperatureMode == DUAL) {
                return DUAL;
            } else if (temperatureMode == TemperatureMode.HEATING) {
                heating = true;
            } else {
                cooling = true;
            }
        }

        if (heating && cooling) {
            return DUAL;
        } else if (heating) {
            return TemperatureMode.HEATING;
        }
        return TemperatureMode.COOLING;
    }

    public static TemperatureMode getTempModeForUnitVentilatorProfile(Equip equip) {

        UnitVentilatorConfiguration pipeUVConfiguration = (UnitVentilatorConfiguration) getSplitConfiguration(equip.getId());
        assert pipeUVConfiguration != null;
        boolean coolingAvailable = false;
        boolean heatingAvailable = false;
        PossibleConditioningMode possibleConditioningMode = getUvPossibleConditioningMode(pipeUVConfiguration);
        if (PossibleConditioningMode.BOTH == possibleConditioningMode) {
            coolingAvailable = true;
            heatingAvailable = true;

        } else if (possibleConditioningMode == PossibleConditioningMode.HEATONLY) {
           heatingAvailable = true;
        } else if (possibleConditioningMode == PossibleConditioningMode.COOLONLY) {
            coolingAvailable = true;
        }
        return getTemperatureForStandaloneBasedOnConditioningMode(
                getTemperatureMode(heatingAvailable, coolingAvailable), equip);
    }
}
