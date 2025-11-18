package a75f.io.logic.bo.building.statprofiles.util

import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.devices.HyperStatSplitDevice
import a75f.io.domain.equips.HyperStatSplitEquip
import a75f.io.domain.equips.unitVentilator.HsSplitCpuEquip
import a75f.io.domain.equips.unitVentilator.Pipe2UVEquip
import a75f.io.domain.equips.unitVentilator.Pipe4UVEquip
import a75f.io.domain.util.ModelLoader
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.ZonePriority
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.getEconSelectedFanLevel
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.EconSensorBusTempAssociation
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UVConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.UnitVentilatorConfiguration
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Author: Manjunath Kundaragi
 * Created on: 22-07-2025
 */


fun mapSensorBusPressureLogicalPoint(
    config: HyperStatSplitConfiguration, equipRef: String
) {
    if (config.sensorBusPressureEnable.enabled && config.pressureAddress0SensorAssociation.associationVal > 0) {
        val hssEquip = HyperStatSplitEquip(equipRef)
        if (hssEquip.ductStaticPressureSensor1_2.pointExists()) {
            hssEquip.ductStaticPressureSensor1_2.id.let {
                val deviceMap = Domain.hayStack.readEntity("device and addr == \"" + config.nodeAddress + "\"")
                val pressureSensorPhysMap = Domain.hayStack.readEntity("domainName == \"" + DomainName.ductStaticPressureSensor + "\" and deviceRef == \"" + deviceMap["id"].toString() + "\"")
                val pressureSensorPhysPoint = RawPoint.Builder().setHashMap(pressureSensorPhysMap).setEnabled(true).setPointRef(it).build()
                Domain.hayStack.updatePoint(pressureSensorPhysPoint, pressureSensorPhysMap["id"].toString())
            }
        }
    }
}

fun correctSensorBusTempPoints(config: HyperStatSplitConfiguration) {
    val hayStack = Domain.hayStack
    val device = hayStack.readEntity("device and addr == \"" + config.nodeAddress + "\"")

    if (!config.isAnySensorBusMapped(config, EconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY)) {
        val deviceOATMap =
            hayStack.readHDict("deviceRef == \"" + device[Tags.ID] + "\" and domainName == \"" + DomainName.outsideAirTempSensor + "\"")
        val deviceOATPoint =
            RawPoint.Builder().setHDict(deviceOATMap).setEnabled(false).setPointRef(null)
        hayStack.updatePoint(deviceOATPoint.build(), deviceOATMap[Tags.ID].toString())

        val deviceOAHMap =
            hayStack.readHDict("deviceRef == \"" + device[Tags.ID] + "\" and domainName == \"" + DomainName.outsideAirHumiditySensor + "\"")
        val deviceOAHPoint =
            RawPoint.Builder().setHDict(deviceOAHMap).setEnabled(false).setPointRef(null)
        hayStack.updatePoint(deviceOAHPoint.build(), deviceOAHMap[Tags.ID].toString())
    }

    if (!config.isAnySensorBusMapped(config, EconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY)) {
        val deviceMATMap =
            hayStack.readHDict("deviceRef == \"" + device[Tags.ID] + "\" and domainName == \"" + DomainName.mixedAirTempSensor + "\"")
        val deviceMATPoint =
            RawPoint.Builder().setHDict(deviceMATMap).setEnabled(false).setPointRef(null)
        hayStack.updatePoint(deviceMATPoint.build(), deviceMATMap[Tags.ID].toString())

        val deviceMAHMap =
            hayStack.readHDict("deviceRef == \"" + device[Tags.ID] + "\" and domainName == \"" + DomainName.mixedAirHumiditySensor + "\"")
        val deviceMAHPoint =
            RawPoint.Builder().setHDict(deviceMAHMap).setEnabled(false).setPointRef(null)
        hayStack.updatePoint(deviceMAHPoint.build(), deviceMAHMap[Tags.ID].toString())
    }

    if (!config.isAnySensorBusMapped(config, EconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY)) {
        val deviceSATMap =
            hayStack.readHDict("deviceRef == \"" + device[Tags.ID] + "\" and domainName == \"" + DomainName.supplyAirTemperature + "\"")
        val deviceSATPoint =
            RawPoint.Builder().setHDict(deviceSATMap).setEnabled(false).setPointRef(null)
        hayStack.updatePoint(deviceSATPoint.build(), deviceSATMap[Tags.ID].toString())

        val deviceSAHMap =
            hayStack.readHDict("deviceRef == \"" + device["id"] + "\" and domainName == \"" + DomainName.supplyAirHumiditySensor + "\"")
        val deviceSAHPoint =
            RawPoint.Builder().setHDict(deviceSAHMap).setEnabled(false).setPointRef(null)
        hayStack.updatePoint(deviceSAHPoint.build(), deviceSAHMap[Tags.ID].toString())
    }
}

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


        is Pipe2UVEquip -> {
            val pipe2Model = ModelLoader.getSplitPipe2Model() as SeventyFiveFProfileDirective
            return Pipe2UVConfiguration(
                equip.nodeAddress,
                NodeType.HYPERSTATSPLIT.toString(),
                ZonePriority.NONE.ordinal,
                equip.roomRef!!,
                equip.floorRef!!,
                ProfileType.HYPERSTATSPLIT_2PIPE_UV,
                pipe2Model
            ).getActiveConfiguration()
        }

        else -> {
            return null
        }
    }
}

fun getPossibleFanModeSettings(fanLevel: Int): PossibleFanMode {
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

fun getPossibleFanMode(equip: HyperStatSplitEquip): PossibleFanMode {
    return getPossibleFanModeSettings(getEconSelectedFanLevel(equip))
}

fun getUvPossibleConditioningMode(config: UnitVentilatorConfiguration): PossibleConditioningMode {
    return when (config) {
        is Pipe4UVConfiguration -> getPipe4UvPossibleConditioningModeSettings(config)
        is Pipe2UVConfiguration -> PossibleConditioningMode.BOTH
        else -> PossibleConditioningMode.OFF
    }
}

fun getPipe4UvPossibleConditioningModeSettings(config: Pipe4UVConfiguration): PossibleConditioningMode {
    return if (config.isCoolingAvailable() && config.isHeatingAvailable()) {
        PossibleConditioningMode.BOTH
    } else if (config.isCoolingAvailable()) {
        PossibleConditioningMode.COOLONLY
    } else if (config.isHeatingAvailable()) {
        PossibleConditioningMode.HEATONLY
    } else {
        PossibleConditioningMode.OFF
    }
}

fun getCpuPossibleConditioningModeSettings(config: HyperStatSplitCpuConfiguration): PossibleConditioningMode {
    return if (config.isCompressorAvailable() || (config.isCoolingAvailable() && config.isHeatingAvailable())) {
        PossibleConditioningMode.BOTH
    } else if (config.isCoolingAvailable()) {
        PossibleConditioningMode.COOLONLY
    } else if (config.isHeatingAvailable()) {
        PossibleConditioningMode.HEATONLY
    } else {
        PossibleConditioningMode.OFF
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

fun getSplitDomainEquipByEquipRef(equipRef: String):HyperStatSplitEquip?{
    return when(val equip = Domain.getEquip(equipRef)){
        is HsSplitCpuEquip -> equip
        is Pipe2UVEquip -> equip
        is Pipe4UVEquip -> equip
        else -> null
    }
}








