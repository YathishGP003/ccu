package a75f.io.logic.bo.building.statprofiles.hyperstat.v2.configs

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.PhysicalPoint
import a75f.io.domain.api.Point
import a75f.io.domain.devices.HyperStatDevice
import a75f.io.domain.equips.hyperstat.HyperStatEquip
import a75f.io.logic.bo.building.definitions.OutputRelayActuatorType
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.StatProfileConfiguration
import a75f.io.logic.bo.building.statprofiles.util.FanConfig
import a75f.io.logic.bo.building.statprofiles.util.getHyperStatDevice
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import org.projecthaystack.HDict

/**
 * Created by Manjunath K on 26-09-2024.
 */

abstract class HyperStatConfiguration(
        nodeAddress: Int, nodeType: String, priority: Int, roomRef: String, floorRef: String, profileType: ProfileType,  model: SeventyFiveFProfileDirective)
    : StatProfileConfiguration(nodeAddress = nodeAddress, nodeType = nodeType, priority = priority, roomRef = roomRef, floorRef = floorRef, profileType = profileType, model) {

    lateinit var analogOut1FanSpeedConfig: FanConfig
    lateinit var analogOut2FanSpeedConfig: FanConfig
    lateinit var analogOut3FanSpeedConfig: FanConfig

    abstract fun getActiveConfiguration(): HyperStatConfiguration

    // Utility functions

    fun getRelayEnabledAssociations(): List<Pair<Boolean, Int>> {
        return mutableListOf<Pair<Boolean, Int>>().apply {
            if (relay1Enabled.enabled) add(Pair(true, relay1Association.associationVal))
            if (relay2Enabled.enabled) add(Pair(true, relay2Association.associationVal))
            if (relay3Enabled.enabled) add(Pair(true, relay3Association.associationVal))
            if (relay4Enabled.enabled) add(Pair(true, relay4Association.associationVal))
            if (relay5Enabled.enabled) add(Pair(true, relay5Association.associationVal))
            if (relay6Enabled.enabled) add(Pair(true, relay6Association.associationVal))
        }
    }

    private fun getAnalogOutEnabledAssociations(): List<Pair<Boolean, Int>> {
        return mutableListOf<Pair<Boolean, Int>>().apply {
            if (analogOut1Enabled.enabled) add(Pair(true, analogOut1Association.associationVal))
            if (analogOut2Enabled.enabled) add(Pair(true, analogOut2Association.associationVal))
            if (analogOut3Enabled.enabled) add(Pair(true, analogOut3Association.associationVal))
        }
    }

    // This method is expected to be called when the equip and device are already present in the Domain
    fun getRelayLogicalPhysicalMap(equipId: String): Map<Point, PhysicalPoint> {
        val map: MutableMap<Point, PhysicalPoint> = HashMap()
        val equip =  Domain.equips[equipId] as HyperStatEquip
        val device = Domain.devices[equipId] as HyperStatDevice
        map[equip.relay1OutputEnable] = device.relay1
        map[equip.relay2OutputEnable] = device.relay2
        map[equip.relay3OutputEnable] = device.relay3
        map[equip.relay4OutputEnable] = device.relay4
        map[equip.relay5OutputEnable] = device.relay5
        map[equip.relay6OutputEnable] = device.relay6
        return map
    }

    // This method is expected to be called when the equip and device are already present in the Domain
    fun getAnalogOutLogicalPhysicalMap(equipId: String): Map<Point, PhysicalPoint> {
        val map: MutableMap<Point, PhysicalPoint> = HashMap()
        val equip = Domain.equips[equipId] as HyperStatEquip
        val device = Domain.devices[equipId] as HyperStatDevice
        map[equip.analog1OutputEnable] = device.analog1Out
        map[equip.analog2OutputEnable] = device.analog2Out
        map[equip.analog3OutputEnable] = device.analog3Out
        return map
    }

    fun isAnyRelayEnabledAssociated(relays: List<Pair<Boolean, Int>> = emptyList(), association: Int): Boolean {
        fun isMapped(list: List<Pair<Boolean, Int>>) = list.any { it.first && it.second == association }
        if (relays.isNotEmpty()) return isMapped(relays)
        return isMapped(getRelayEnabledAssociations())
    }

    fun isAnyAnalogOutEnabledAssociated(analogOuts: List<Pair<Boolean, Int>> = emptyList(), association: Int): Boolean {
        fun isMapped(list: List<Pair<Boolean, Int>>) = list.any { it.first && it.second == association }
        if (analogOuts.isNotEmpty()) return isMapped(analogOuts)
        return isMapped(getAnalogOutEnabledAssociations())
    }

    private fun availableHighestStages(stage1: Int, stage2: Int, stage3: Int): Triple<Boolean, Boolean, Boolean> {
        var isStage1Selected = false
        var isStage2Selected = false
        var isStage3Selected = false

        getRelayEnabledAssociations().forEach { (enabled, associated) ->
            if (enabled) {
                if (associated == stage1) isStage1Selected = true
                if (associated == stage2) isStage2Selected = true
                if (associated == stage3) isStage3Selected = true
            }
        }
        return Triple(isStage1Selected, isStage2Selected, isStage3Selected)
    }

    protected fun getHighestStage(stage1: Int, stage2: Int, stage3: Int): Int {
        val availableStages = availableHighestStages(stage1, stage2, stage3)
        return if (availableStages.third) stage3
        else if (availableStages.second) stage2
        else if (availableStages.first) stage1
        else -1
    }

    protected fun getLowestStage(stage1: Int, stage2: Int, stage3: Int): Int {
        val availableStages = availableHighestStages(stage1, stage2, stage3)
        return if (availableStages.first) stage1
        else if (availableStages.second) stage2
        else if (availableStages.third) stage3
        else -1
    }

    fun getAnalogOutsConfigurationMapping(): List<Triple<Boolean, Int, Port>> {
        return listOf(
            Triple(
                analogOut1Enabled.enabled,
                analogOut1Association.associationVal,
                Port.ANALOG_OUT_ONE
            ),
            Triple(
                analogOut2Enabled.enabled,
                analogOut2Association.associationVal,
                Port.ANALOG_OUT_TWO
            ),
            Triple(
                analogOut3Enabled.enabled,
                analogOut3Association.associationVal,
                Port.ANALOG_OUT_THREE
            ),
        )
    }

    fun getFanConfiguration(): HashMap<Port, FanConfig> {
        return hashMapOf(
            Port.ANALOG_OUT_ONE to analogOut1FanSpeedConfig,
            Port.ANALOG_OUT_TWO to analogOut2FanSpeedConfig,
            Port.ANALOG_OUT_THREE to analogOut3FanSpeedConfig
        )
    }

    private fun isEnabledAndAssociated(enabled: Boolean, association: Int, mapping: Int) = enabled && association == mapping

    abstract fun getHighestFanStageCount(): Int

    fun setPortConfiguration(nodeAddress: Int, relays: Map<String, Boolean>, analogOuts: Map<String, Pair<Boolean, String>>) {

        val hayStack = CCUHsApi.getInstance()
        val device = getHyperStatDevice(nodeAddress)
        val deviceRef = device[Tags.ID].toString()

        fun getPortDict(portName: String): HDict? {
            return hayStack.readHDict("point and deviceRef == \"$deviceRef\" and domainName == \"$portName\"")
        }

        fun updatePort(portDict: HDict, type: String, isWritable: Boolean) {
            val port = RawPoint.Builder().setHDict(portDict)
            port.setType(type)
            if (isWritable) {
                port.addMarker(Tags.WRITABLE)
                port.addMarker(Tags.UNUSED)
            } else if(portDict.has(Tags.UNUSED)) {
                port.removeMarkerIfExists(Tags.WRITABLE)
                port.removeMarkerIfExists(Tags.UNUSED)
            }
            val buildPoint = port.build()
            hayStack.updatePoint(buildPoint, buildPoint.id)
        }

        relays.forEach { (relayName, externallyMapped) ->
            val portDict = getPortDict(relayName)
            if (portDict != null && !portDict.isEmpty) {
                updatePort(portDict, OutputRelayActuatorType.NormallyOpen.displayName, externallyMapped)
            }
        }

        analogOuts.forEach { (analogName, config) ->
            val portDict = getPortDict(analogName)
            if (portDict != null && !portDict.isEmpty) {
                updatePort(portDict, config.second, config.first)
            }
        }
    }
}


