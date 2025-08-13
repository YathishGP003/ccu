package a75f.io.logic.bo.building.statprofiles.util

import a75f.io.domain.api.Domain
import a75f.io.domain.devices.HyperStatSplitDevice
import a75f.io.domain.equips.HyperStatSplitEquip
import a75f.io.domain.equips.unitVentilator.HsSplitCpuEquip
import a75f.io.domain.equips.unitVentilator.Pipe4UVEquip
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.getEconSelectedFanLevel
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.UnitVentilatorConfiguration
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Author: Manjunath Kundaragi
 * Created on: 22-07-2025
 */

fun getSplitConfiguration(equipRef: String): HyperStatSplitConfiguration? {
    when (val equip = Domain.getDomainEquip(equipRef) as HyperStatSplitEquip) {
        is HsSplitCpuEquip -> {
            val cpuModel = ModelLoader.getHyperStatSplitCpuModel() as SeventyFiveFProfileDirective
            return HyperStatSplitCpuConfiguration(
                equip.nodeAddress,
                NodeType.HYPERSTATSPLIT.toString(),
                ZonePriority.NONE.ordinal,
                equip.roomRef!!,
                equip.floorRef!!,
                ProfileType.HYPERSTATSPLIT_CPU,
                cpuModel
            ).getActiveConfiguration()
        }

        is Pipe4UVEquip -> {
            val pipe4Model = ModelLoader.getSplitPipe4Model() as SeventyFiveFProfileDirective
            return Pipe4UVConfiguration(
                equip.nodeAddress,
                NodeType.HYPERSTATSPLIT.toString(),
                ZonePriority.NONE.ordinal,
                equip.roomRef!!,
                equip.floorRef!!,
                ProfileType.HYPERSTATSPLIT_4PIPE_UV,
                pipe4Model
            ).getActiveConfiguration()
        }

        else -> {
            return null
        }
    }
}

fun getUvPossibleFanModeSettings(fanLevel: Int): PossibleFanMode {
    return when (fanLevel) {
        HsFanConstants.AUTO -> PossibleFanMode.AUTO
        HsFanConstants.LOW -> PossibleFanMode.LOW
        HsFanConstants.MED -> PossibleFanMode.MED
        HsFanConstants.HIGH -> PossibleFanMode.HIGH
        HsFanConstants.LOW_MED -> PossibleFanMode.LOW_MED
        HsFanConstants.LOW_HIGH -> PossibleFanMode.LOW_HIGH
        HsFanConstants.MED_HIGH -> PossibleFanMode.MED_HIGH
        HsFanConstants.LOW_MED_HIGH -> PossibleFanMode.LOW_MED_HIGH
        else -> PossibleFanMode.OFF
    }
}

fun getUvFanModeLevel(equip: HyperStatSplitEquip): PossibleFanMode {
    return getUvPossibleFanModeSettings(getEconSelectedFanLevel(equip))
}

fun getUvPossibleConditioningMode(config: UnitVentilatorConfiguration): PossibleConditioningMode {
    return when (config) {
        is Pipe4UVConfiguration -> getPipe4UvPossibleConditioningModeSettings(config)
        else -> PossibleConditioningMode.OFF
    }
}

fun getPipe4UvPossibleConditioningModeSettings(config: Pipe4UVConfiguration): PossibleConditioningMode {
    return with(config) {
        when {
            isAnyCoolingPortMappedInRelayOrAO() && isAnyHeatingPortMappedInRelayOrAO() -> {
                CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "PossibleConditioningMode : BOTH ")
                PossibleConditioningMode.BOTH
            }

            isAnyCoolingPortMappedInRelayOrAO() -> {
                CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "PossibleConditioningMode : COOL_ONLY ")
                PossibleConditioningMode.COOLONLY
            }

            isAnyHeatingPortMappedInRelayOrAO() -> {
                CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "PossibleConditioningMode : HEAT_ONLY ")
                PossibleConditioningMode.HEATONLY
            }

            else -> {
                CcuLog.i(L.TAG_CCU_HSSPLIT_CPUECON, "PossibleConditioningMode : OFF ")
                PossibleConditioningMode.OFF
            }
        }
    }
}


fun getDomainHyperStatSplitDevice(equipRef: String): HyperStatSplitDevice {
    val device = Domain.getEquipDevices()
    if (device.containsKey(equipRef)) {
        return device[equipRef] as HyperStatSplitDevice
    } else {
        Domain.devices[equipRef] = HyperStatSplitDevice(equipRef)
    }
    return device[equipRef] as HyperStatSplitDevice
}








