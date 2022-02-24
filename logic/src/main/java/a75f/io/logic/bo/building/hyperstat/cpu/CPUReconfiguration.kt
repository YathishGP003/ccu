package a75f.io.logic.bo.building.hyperstat.cpu

import a75f.io.api.haystack.*
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hyperstat.common.AnalogOutChanges
import a75f.io.logic.bo.building.hyperstat.common.HSReconfigureUtil
import a75f.io.logic.bo.building.hyperstat.common.HyperStatAssociationUtil
import a75f.io.logic.bo.building.hyperstat.cpu.HyperStatCpuEquip.Companion.getHyperstatEquipRef
import a75f.io.logic.bo.haystack.device.DeviceUtil
import android.util.Log

/**
 * Created by Manjunath K on 26-10-2021.
 */
class CPUReconfiguration {

    companion object {
        // Update Analog actuator type
        fun updateAnalogActuatorType(
            updatePoint: Point, equipRef: String, nodeAddress: Int, haystack:
            CCUHsApi, portType: Port, whichConfig: String
        ) {
            if (updatePoint.markers.contains("out")) {
                val analogOutTag = when (whichConfig) {
                    Queries.ANALOG1_OUT -> "analog1"
                    Queries.ANALOG2_OUT -> "analog2"
                    Queries.ANALOG3_OUT -> "analog3"
                    else -> null
                }
                if (analogOutTag != null) {
                    val minPointValue = haystack.readDefaultVal(
                        "point and hyperstat and ${Tags.CPU} and config and " +
                                "$analogOutTag and out and min and equipRef == \"$equipRef\""
                    ).toInt()
                    val maxPointValue = haystack.readDefaultVal(
                        "point and hyperstat and ${Tags.CPU} and config and " +
                                "$analogOutTag and out and max and equipRef == \"$equipRef\""
                    ).toInt()

                    val pointType = "${minPointValue}-${maxPointValue}v"
                    Log.i(L.TAG_CCU_HSCPU, "updateAnalogActuatorType: ")
                    DeviceUtil.updatePhysicalPointType(nodeAddress, portType.name, pointType)
                }
            }
        }

        // Function to create Logical Points
        fun createLogicalPoint(
            pointsUtil: HyperStatPointsUtil, configType: String, associationPoint:
            Double, updatedConfigValue: Double, nodeAddress: Int
        ) {

            when (configType) {
                Tags.RELAY1 -> {
                    val relay = RelayState(
                        updatedConfigValue == 1.0, CpuRelayAssociation.values()[associationPoint.toInt()]
                    )

                    val pointData: Point = pointsUtil.relayConfiguration(relay, Tags.RELAY1)
                    val pointId = HSReconfigureUtil.createPoint(pointData, pointsUtil, 0.0)

                    DeviceUtil.setPointEnabled(nodeAddress, Port.RELAY_ONE.name, updatedConfigValue == 1.0)
                    DeviceUtil.updatePhysicalPointRef(nodeAddress, Port.RELAY_ONE.name, pointId)
                }
                Tags.RELAY2 -> {
                    val relay =
                        RelayState(updatedConfigValue == 1.0, CpuRelayAssociation.values()[associationPoint.toInt()])
                    val pointData: Point = pointsUtil.relayConfiguration(relay, Tags.RELAY2)
                    val pointId = HSReconfigureUtil.createPoint(pointData, pointsUtil, 0.0)
                    DeviceUtil.setPointEnabled(nodeAddress, Port.RELAY_TWO.name, updatedConfigValue == 1.0)
                    DeviceUtil.updatePhysicalPointRef(nodeAddress, Port.RELAY_TWO.name, pointId)
                }
                Tags.RELAY3 -> {
                    val relay =
                        RelayState(updatedConfigValue == 1.0, CpuRelayAssociation.values()[associationPoint.toInt()])
                    val pointData: Point = pointsUtil.relayConfiguration(relay, Tags.RELAY3)
                    val pointId = HSReconfigureUtil.createPoint(pointData, pointsUtil, 0.0)
                    DeviceUtil.setPointEnabled(nodeAddress, Port.RELAY_THREE.name, updatedConfigValue == 1.0)
                    DeviceUtil.updatePhysicalPointRef(nodeAddress, Port.RELAY_THREE.name, pointId)
                }
                Tags.RELAY4 -> {
                    val relay =
                        RelayState(updatedConfigValue == 1.0, CpuRelayAssociation.values()[associationPoint.toInt()])
                    val pointData: Point = pointsUtil.relayConfiguration(relay, Tags.RELAY4)
                    val pointId = HSReconfigureUtil.createPoint(pointData, pointsUtil, 0.0)
                    DeviceUtil.setPointEnabled(nodeAddress, Port.RELAY_FOUR.name, updatedConfigValue == 1.0)
                    DeviceUtil.updatePhysicalPointRef(nodeAddress, Port.RELAY_FOUR.name, pointId)
                }
                Tags.RELAY5 -> {
                    val relay =
                        RelayState(updatedConfigValue == 1.0, CpuRelayAssociation.values()[associationPoint.toInt()])
                    val pointData: Point = pointsUtil.relayConfiguration(relay, Tags.RELAY5)
                    val pointId = HSReconfigureUtil.createPoint(pointData, pointsUtil, 0.0)
                    DeviceUtil.setPointEnabled(nodeAddress, Port.RELAY_FIVE.name, updatedConfigValue == 1.0)
                    DeviceUtil.updatePhysicalPointRef(nodeAddress, Port.RELAY_FIVE.name, pointId)
                }
                Tags.RELAY6 -> {
                    val relay =
                        RelayState(updatedConfigValue == 1.0, CpuRelayAssociation.values()[associationPoint.toInt()])
                    val pointData: Point = pointsUtil.relayConfiguration(relay, Tags.RELAY6)
                    val pointId = HSReconfigureUtil.createPoint(pointData, pointsUtil, 0.0)
                    DeviceUtil.setPointEnabled(nodeAddress, Port.RELAY_SIX.name, updatedConfigValue == 1.0)
                    DeviceUtil.updatePhysicalPointRef(nodeAddress, Port.RELAY_SIX.name, pointId)
                }
                Queries.ANALOG1_OUT -> {
                    val analogOutState = AnalogOutState(
                        updatedConfigValue == 1.0,
                        CpuAnalogOutAssociation.values()[associationPoint.toInt()],
                        2.0,
                        10.0,
                        30.0,
                        60.0,
                        100.0
                    )
                    val pointData: Triple<Any, Any, Any> = pointsUtil.analogOutConfiguration(
                        analogOutState, Tags.ANALOG1
                    )
                    val minPoint = (pointData.second as Pair<*, *>)
                    val maxPoint = (pointData.third as Pair<*, *>)
                    val pointId = HSReconfigureUtil.createPoint(pointData.first as Point, pointsUtil, 0.0)

                    HSReconfigureUtil.createPoint(minPoint.first as Point, pointsUtil, 2.0)
                    HSReconfigureUtil.createPoint(maxPoint.first as Point, pointsUtil, 10.0)
                    addFanConfiguration(analogOutState, pointsUtil, Tags.ANALOG1)

                    DeviceUtil.setPointEnabled(nodeAddress, Port.ANALOG_OUT_ONE.name, updatedConfigValue == 1.0)
                    DeviceUtil.updatePhysicalPointRef(nodeAddress, Port.ANALOG_OUT_ONE.name, pointId)
                    DeviceUtil.updatePhysicalPointType(nodeAddress, Port.ANALOG_OUT_ONE.name, "2-10v")
                }
                Queries.ANALOG2_OUT -> {
                    val analogOutState = AnalogOutState(
                        updatedConfigValue == 1.0,
                        CpuAnalogOutAssociation.values()[associationPoint.toInt()],
                        2.0,
                        10.0,
                        30.0,
                        60.0,
                        100.0
                    )
                    val pointData: Triple<Any, Any, Any> = pointsUtil.analogOutConfiguration(
                        analogOutState, Tags.ANALOG2
                    )

                    val minPoint = (pointData.second as Pair<*, *>)
                    val maxPoint = (pointData.third as Pair<*, *>)
                    val pointId = HSReconfigureUtil.createPoint(pointData.first as Point, pointsUtil, 0.0)
                    HSReconfigureUtil.createPoint(minPoint.first as Point, pointsUtil, 2.0)
                    HSReconfigureUtil.createPoint(maxPoint.first as Point, pointsUtil, 10.0)
                    addFanConfiguration(analogOutState, pointsUtil, Tags.ANALOG2)
                    DeviceUtil.setPointEnabled(nodeAddress, Port.ANALOG_OUT_TWO.name, updatedConfigValue == 1.0)
                    DeviceUtil.updatePhysicalPointRef(nodeAddress, Port.ANALOG_OUT_TWO.name, pointId)
                    DeviceUtil.updatePhysicalPointType(nodeAddress, Port.ANALOG_OUT_TWO.name, "2-10v")
                }
                Queries.ANALOG3_OUT -> {
                    val analogOutState = AnalogOutState(
                        updatedConfigValue == 1.0,
                        CpuAnalogOutAssociation.values()[associationPoint.toInt()],
                        2.0,
                        10.0,
                        30.0,
                        60.0,
                        100.0
                    )
                    val pointData: Triple<Any, Any, Any> = pointsUtil.analogOutConfiguration(
                        analogOutState, Tags.ANALOG3
                    )

                    val minPoint = (pointData.second as Pair<*, *>)
                    val maxPoint = (pointData.third as Pair<*, *>)
                    val pointId = HSReconfigureUtil.createPoint(pointData.first as Point, pointsUtil, 0.0)
                    HSReconfigureUtil.createPoint(minPoint.first as Point, pointsUtil, 2.0)
                    HSReconfigureUtil.createPoint(maxPoint.first as Point, pointsUtil, 10.0)
                    addFanConfiguration(analogOutState, pointsUtil, Tags.ANALOG3)
                    DeviceUtil.setPointEnabled(nodeAddress, Port.ANALOG_OUT_THREE.name, updatedConfigValue == 1.0)
                    DeviceUtil.updatePhysicalPointRef(nodeAddress, Port.ANALOG_OUT_THREE.name, pointId)
                    DeviceUtil.updatePhysicalPointType(nodeAddress, Port.ANALOG_OUT_THREE.name, "2-10v")
                }

                Queries.ANALOG1_IN -> {
                    val analogInState = AnalogInState(false, CpuAnalogInAssociation.values()[associationPoint.toInt()])

                    val pointData: Point = pointsUtil.analogInConfiguration(
                        analogInState = analogInState,
                        analogTag = Tags.ANALOG1
                    )
                    val pointId = HSReconfigureUtil.createPoint(pointData, pointsUtil, 0.0)
                    DeviceUtil.setPointEnabled(nodeAddress, Port.ANALOG_IN_ONE.name, updatedConfigValue == 1.0)
                    DeviceUtil.updatePhysicalPointRef(nodeAddress, Port.ANALOG_IN_ONE.name, pointId)
                    DeviceUtil.updatePhysicalPointType(
                        nodeAddress,
                        Port.ANALOG_IN_ONE.name,
                        HyperStatAssociationUtil.getSensorNameByType(CpuAnalogInAssociation.values()[associationPoint.toInt()])
                    )

                }

                Queries.ANALOG2_IN -> {
                    val analogInState = AnalogInState(false, CpuAnalogInAssociation.values()[associationPoint.toInt()])
                    val pointData: Point = pointsUtil.analogInConfiguration(
                        analogInState = analogInState,
                        analogTag = Tags.ANALOG2
                    )
                    val pointId = HSReconfigureUtil.createPoint(pointData, pointsUtil, 0.0)
                    DeviceUtil.setPointEnabled(nodeAddress, Port.ANALOG_IN_TWO.name, updatedConfigValue == 1.0)
                    DeviceUtil.updatePhysicalPointRef(nodeAddress, Port.ANALOG_IN_TWO.name, pointId)
                    DeviceUtil.updatePhysicalPointType(
                        nodeAddress,
                        Port.ANALOG_IN_ONE.name,
                        HyperStatAssociationUtil.getSensorNameByType(CpuAnalogInAssociation.values()[associationPoint.toInt()])
                    )
                }

                Tags.TH1 -> {
                    val pointData: Point = pointsUtil.createPointForAirflowTempSensor()
                    val pointId = HSReconfigureUtil.createPoint(pointData, pointsUtil, 0.0)
                    DeviceUtil.setPointEnabled(nodeAddress, Port.TH1_IN.name, updatedConfigValue == 1.0)
                    DeviceUtil.updatePhysicalPointRef(nodeAddress, Port.TH1_IN.name, pointId)
                }
                Tags.TH2 -> {
                    val doorWindowSensorTh2Point = pointsUtil.createPointForDoorWindowSensor(Tags.TH2)
                    val pointId = HSReconfigureUtil.createPoint(doorWindowSensorTh2Point, pointsUtil, 0.0)
                    DeviceUtil.setPointEnabled(nodeAddress, Port.TH2_IN.name, updatedConfigValue == 1.0)
                    DeviceUtil.updatePhysicalPointRef(nodeAddress, Port.TH2_IN.name, pointId)
                }

            }
        }

        // Function to add fan configuration
        private fun addFanConfiguration(
            analogOutState: AnalogOutState, pointsUtil: HyperStatPointsUtil, analogTag:
            String
        ) {
            if (HyperStatAssociationUtil.isAnalogOutAssociatedToFanSpeed(analogOutState)) {
                val pointData: Triple<Point, Point, Point> = pointsUtil.createFanLowMediumHighPoint(analogTag)
                HSReconfigureUtil.createPoint(pointData.first, pointsUtil, analogOutState.perAtFanLow)
                HSReconfigureUtil.createPoint(pointData.second, pointsUtil, analogOutState.perAtFanMedium)
                HSReconfigureUtil.createPoint(pointData.third, pointsUtil, analogOutState.perAtFanHigh)
            }
        }

        private fun deleteLogicalPoint(haystack: CCUHsApi, equipRef: String, configType: String, portType: Port) {
            val logicalPointId: String? = HSReconfigureUtil.readPointID(
                "$configType and logical",
                equipRef, Tags.CPU, haystack
            )
            Log.i(L.TAG_CCU_HSCPU, "Reconfiguration logicalPointId = $logicalPointId ")
            if (logicalPointId != null) {
                Log.i(L.TAG_CCU_HSCPU, "Reconfiguration $logicalPointId logical Deleted")
                haystack.deleteEntityTree(logicalPointId)
            }

            val tag = when (portType) {
                Port.ANALOG_OUT_ONE -> "analog1"
                Port.ANALOG_OUT_TWO -> "analog2"
                else -> "analog3"
            }
            if (configType.contentEquals(Queries.ANALOG1_OUT) ||
                configType.contentEquals(Queries.ANALOG2_OUT) ||
                configType.contentEquals(Queries.ANALOG3_OUT)
            ) {

                val minPointId = HSReconfigureUtil.readPointID("$tag and out and min", equipRef, Tags.CPU, haystack)
                val maxPointId = HSReconfigureUtil.readPointID("$tag and out and max", equipRef, Tags.CPU, haystack)
                val fanLowPointId = HSReconfigureUtil.readPointID("$tag and out and low", equipRef, Tags.CPU, haystack)
                val fanMediumPointId =
                    HSReconfigureUtil.readPointID("$tag and out and medium", equipRef, Tags.CPU, haystack)
                val fanHighPointId =
                    HSReconfigureUtil.readPointID("$tag and out and high", equipRef, Tags.CPU, haystack)

                minPointId.let { haystack.deleteEntityTree(minPointId) }
                maxPointId.let { haystack.deleteEntityTree(maxPointId) }
                fanLowPointId.let { haystack.deleteEntityTree(fanLowPointId) }
                fanMediumPointId.let { haystack.deleteEntityTree(fanMediumPointId) }
                fanHighPointId.let { haystack.deleteEntityTree(fanHighPointId) }

            }
        }

        fun configLogicalPoint(
            updatedConfigValue: Double,
            haystack: CCUHsApi,
            equip: Equip,
            configPoint: Point,
            configType: String,
            portType: Port
        ) {
            if (updatedConfigValue <= 0) {
                Log.i(L.TAG_CCU_HSCPU, "Config Point $configType is disabled so deleting the logical point")
                deleteLogicalPoint(haystack, configPoint.equipRef, configType, portType)

                // Get Existing Configuration
                val config = getHyperstatEquipRef(equip.group.toShort())
                config.updateConditioningMode(config.getConfiguration())
                config.updateFanMode(config.getConfiguration())
            }
            // Create Logical Points only when config is enabled
            if (updatedConfigValue > 0) {
                val logicalPointId: String? = HSReconfigureUtil.readPointID(
                    "$configType and logical",
                    configPoint.equipRef, Tags.CPU, haystack
                )
                Log.i(L.TAG_CCU_HSCPU, "Checking before creating logicalPointId $logicalPointId")
                if (logicalPointId == null) {
                    val currentAssociationValue = HSReconfigureUtil.readAssociationPointValue(
                        configType, configPoint.equipRef, Tags.CPU, haystack
                    )
                    val pointUtil = HSReconfigureUtil.getEquipPointsUtil(equip, haystack)
                    Log.i(
                        L.TAG_CCU_HSCPU,
                        "Creating a logical point for $configType -> $currentAssociationValue"
                    )
                    createLogicalPoint(
                        pointUtil, configType, currentAssociationValue, updatedConfigValue, equip.group.toInt()
                    )
                }
            }
        }


        fun configAssociationPoint(
            whichConfig: String?, portType: Port?, updatedConfigValue: Double, associationPoint:
            Point
        ) {
            if (whichConfig != null && portType != null) {

                if (whichConfig.contentEquals(Tags.RELAY1)
                    || whichConfig.contentEquals(Tags.RELAY2)
                    || whichConfig.contentEquals(Tags.RELAY3)
                    || whichConfig.contentEquals(Tags.RELAY4)
                    || whichConfig.contentEquals(Tags.RELAY5)
                    || whichConfig.contentEquals(Tags.RELAY6)
                ) {
                    val relay = RelayState(true, CpuRelayAssociation.values()[updatedConfigValue.toInt()])
                    val hyperStatCpuEquip = HyperStatCpuEquip.getHyperstatEquipRef(associationPoint.group.toShort())
                    hyperStatCpuEquip.updateRelayDetails(relay, whichConfig, portType,null)
                }

                if (whichConfig.contentEquals(Queries.ANALOG1_OUT)
                    || whichConfig.contentEquals(Queries.ANALOG2_OUT)
                    || whichConfig.contentEquals(Queries.ANALOG3_OUT)
                ) {

                    val analogTag = when {
                        whichConfig.contentEquals(Queries.ANALOG1_OUT) -> Tags.ANALOG1
                        whichConfig.contentEquals(Queries.ANALOG2_OUT) -> Tags.ANALOG2
                        else -> Tags.ANALOG3
                    }
                    val analogPort = when {
                        whichConfig.contentEquals(Queries.ANALOG1_OUT) -> Port.ANALOG_OUT_ONE
                        whichConfig.contentEquals(Queries.ANALOG2_OUT) -> Port.ANALOG_OUT_TWO
                        else -> Port.ANALOG_OUT_THREE
                    }
                    val analogOutState = AnalogOutState(
                        true, CpuAnalogOutAssociation.values()[updatedConfigValue
                            .toInt()], 2.0, 10.0, 30.0, 60.0, 100.0
                    )
                    val hyperStatCpuEquip = HyperStatCpuEquip.getHyperstatEquipRef(associationPoint.group.toShort())
                    Log.i(L.TAG_CCU_HSCPU, "Reconfiguration analogTag  $analogTag $analogPort")
                    hyperStatCpuEquip.updateAnalogOutDetails(
                        analogOutState,
                        analogTag,
                        analogPort,
                        AnalogOutChanges.MAPPING
                    )

                }

                if (whichConfig.contentEquals(Queries.ANALOG1_IN)
                    || whichConfig.contentEquals(Queries.ANALOG2_IN)
                ) {

                    val analogInState = AnalogInState(true, CpuAnalogInAssociation.values()[updatedConfigValue.toInt()])
                    val hyperStatCpuEquip = HyperStatCpuEquip.getHyperstatEquipRef(associationPoint.group.toShort())
                    val tag = if (whichConfig.contentEquals(Queries.ANALOG1_IN)) Tags.ANALOG1 else Tags.ANALOG2
                    val port =
                        if (whichConfig.contentEquals(Queries.ANALOG1_IN)) Port.ANALOG_IN_ONE else Port.ANALOG_IN_TWO
                    hyperStatCpuEquip.updateAnalogInDetails(analogInState, tag, port)
                }

            }
        }
    }
}