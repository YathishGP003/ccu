package a75f.io.logic.bo.building.statprofiles.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.devices.HyperStatDevice
import a75f.io.domain.devices.HyperStatSplitDevice
import a75f.io.domain.equips.HyperStatSplitEquip
import a75f.io.domain.equips.unitVentilator.HsSplitCpuEquip
import a75f.io.domain.equips.unitVentilator.Pipe4UVEquip
import a75f.io.domain.util.ModelLoader
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVRelayControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UvAnalogOutControls
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

fun getHyperStatSplitDevice(nodeAddress: Int): HashMap<Any, Any> {
    return CCUHsApi.getInstance()
        .readEntity("domainName == \"${DomainName.hyperstatSplitDevice}\" and addr == \"$nodeAddress\"")
}

fun getHyperStatSplitDeviceByEquipRef(equipRef: String): HashMap<Any, Any> {
    return CCUHsApi.getInstance()
        .readEntity("domainName == \"${DomainName.hyperstatSplitDevice}\" and equipRef == \"$equipRef\"")
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

fun getUvPossibleFanMode(config: UnitVentilatorConfiguration): Int {
    return when (config) {
        is Pipe4UVConfiguration -> getPipe4UvPossibleFanModeSettings(config)
        else -> getPipe4UvPossibleFanModeSettings(config)
    }
}

fun getUvPossibleConditioningMode(config: UnitVentilatorConfiguration):PossibleConditioningMode{
    return when (config) {
        is Pipe4UVConfiguration -> getPipe4UvPossibleConditioningModeSettings(config)
        else -> getPipe4UvPossibleConditioningModeSettings(config as Pipe4UVConfiguration)
    }
}

fun getPipe4UvPossibleConditioningModeSettings(config: Pipe4UVConfiguration): PossibleConditioningMode {

    val enabledRelays = config.getRelayEnabledAssociations()
        .map { it.second }
    val analogouts = config.getAnalogEnabledAssociations().map { it.second }
    if (enabledRelays.isEmpty() && analogouts.isEmpty()) return PossibleConditioningMode.OFF
    return when {
        (((Pipe4UVRelayControls.HEATING_WATER_VALVE.ordinal in enabledRelays || Pipe4UvAnalogOutControls.HEATING_WATER_MODULATING_VALVE.ordinal in analogouts )||( Pipe4UVRelayControls.AUX_HEATING_STAGE1.ordinal in enabledRelays || Pipe4UVRelayControls.AUX_HEATING_STAGE2.ordinal in enabledRelays)) && (
                Pipe4UvAnalogOutControls.COOLING_WATER_MODULATING_VALVE.ordinal in analogouts || Pipe4UVRelayControls.COOLING_WATER_VALVE.ordinal in enabledRelays)) -> PossibleConditioningMode.BOTH

        (Pipe4UVRelayControls.COOLING_WATER_VALVE.ordinal in enabledRelays ||
                Pipe4UvAnalogOutControls.COOLING_WATER_MODULATING_VALVE.ordinal in analogouts) -> PossibleConditioningMode.COOLONLY

        ((Pipe4UVRelayControls.HEATING_WATER_VALVE.ordinal in enabledRelays ||
                Pipe4UvAnalogOutControls.HEATING_WATER_MODULATING_VALVE.ordinal in analogouts)|| (Pipe4UVRelayControls.AUX_HEATING_STAGE1.ordinal in enabledRelays || Pipe4UVRelayControls.AUX_HEATING_STAGE2.ordinal in enabledRelays)) -> PossibleConditioningMode.HEATONLY

        else -> PossibleConditioningMode.OFF
    }
}

fun getPipe4UvPossibleFanModeSettings(config: UnitVentilatorConfiguration): Int {

    val enabledRelays = config.getRelayEnabledAssociations()
        .map { it.second }
    var fanLevel = 0

    val analogouts = config.getAnalogEnabledAssociations().map { it.second }


    if (Pipe4UvAnalogOutControls.FAN_SPEED.ordinal in analogouts) {
        return HsFanConstants.LOW_MED_HIGH
    }

    fanLevel = buildList {
        if ((Pipe4UVRelayControls.FAN_LOW_SPEED_VENTILATION.ordinal in enabledRelays || Pipe4UVRelayControls.FAN_LOW_SPEED.ordinal in enabledRelays)) add(
            HsFanConstants.LOW
        )
        if (Pipe4UVRelayControls.FAN_MEDIUM_SPEED.ordinal in enabledRelays) add(HsFanConstants.MED)
        if (Pipe4UVRelayControls.FAN_HIGH_SPEED.ordinal in enabledRelays) add(HsFanConstants.HIGH)
    }.sum()

    return if (Pipe4UVRelayControls.FAN_ENABLED.ordinal in enabledRelays && fanLevel == 0)
         fanLevel + HsFanConstants.AUTO
    else
        fanLevel
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








