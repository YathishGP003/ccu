package a75f.io.logic.migration.hyperstat

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.logic.bo.building.definitions.Port
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.AnalogInAssociation
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.CpuAnalogOutAssociation
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.CpuRelayAssociation
import a75f.io.logic.bo.haystack.device.DeviceUtil
import a75f.io.logic.migration.hyperstat.MigratePointsUtil.Companion.updateMarkers

/**
 * Created by Manjunath K on 07-10-2022.
 */

class CpuPointsMigration {
    companion object {


        private var migratedRelays = HashSet<Int>()
        var migratedAnalogOut = HashSet<Int>()
        var isAnalogInputMigrated = false

        fun doMigrationForProfilePoints() {
            val haystack: CCUHsApi = CCUHsApi.getInstance()
            val equipList: ArrayList<HashMap<Any, Any>> =
                haystack.readAllEntities("hyperstat and cpu and equip")

            equipList.forEach { equip ->
                migrateTempOffSetPoint(equip[Tags.ID].toString(),equip[Tags.DIS].toString())
                migrateAutoForceOccupyPoint(equip[Tags.ID].toString(),equip[Tags.DIS].toString())
                migrateCo2ConfigPoints(equip[Tags.ID].toString(),equip[Tags.DIS].toString())
                migrateVocConfigPoints(equip[Tags.ID].toString(),equip[Tags.DIS].toString())
                migratePm25ConfigPoints(equip[Tags.ID].toString(),equip[Tags.DIS].toString())
                migrateLoopOutputPoint(equip[Tags.ID].toString(),equip[Tags.DIS].toString())
                migrateUserIntentPoints(equip[Tags.ID].toString())

                // Logical Points migration
                migrateAllRelaysPoint(equip[Tags.ID].toString())
                migrateThermistorSensorPoint(equip[Tags.ID].toString(),equip[Tags.DIS].toString())
                migrateAnalogInSensorPoints(equip[Tags.ID].toString(),equip[Tags.DIS].toString())
                migrateAnalogOutputPoint(equip[Tags.ID].toString())

                migrateSensingInputPoints(equip[Tags.ID].toString())
                migrateRelayPoints(equip[Tags.ID].toString())
                migrateAnalogOutPoints(equip[Tags.ID].toString())
                migrateThermistorPoints(equip[Tags.ID].toString(),equip[Tags.DIS].toString())
                migrateAnalogInPoints(equip[Tags.ID].toString())

                migratedRelays.clear()
                migratedAnalogOut.clear()
                isAnalogInputMigrated = false
            }
            CCUHsApi.getInstance().scheduleSync()
        }



        // Migration of logical points
        private fun migrateAllRelaysPoint(equipRef: String) {
            migrateRelayLogicalPoints(equipRef, "relay1", Port.RELAY_ONE)
            migrateRelayLogicalPoints(equipRef, "relay2", Port.RELAY_TWO)
            migrateRelayLogicalPoints(equipRef, "relay3", Port.RELAY_THREE)
            migrateRelayLogicalPoints(equipRef, "relay4", Port.RELAY_FOUR)
            migrateRelayLogicalPoints(equipRef, "relay5", Port.RELAY_FIVE)
            migrateRelayLogicalPoints(equipRef, "relay6", Port.RELAY_SIX)
        }

        private fun migrateRelayLogicalPoints(equipRef: String, relay: String, port: Port) {

            if (!migratedRelays.contains(port.ordinal)) {
                val config = readDefaultValue("config and enabled and $relay", equipRef)
                if (config == 1) {
                    when (readDefaultValue("config and association and $relay", equipRef)) {
                        CpuRelayAssociation.COOLING_STAGE_1.ordinal -> {
                            migrateCoolingStagePoint(equipRef, relay, port,CpuRelayAssociation.COOLING_STAGE_1)
                        }
                        CpuRelayAssociation.COOLING_STAGE_2.ordinal -> {
                            migrateCoolingStagePoint(equipRef, relay, port,CpuRelayAssociation.COOLING_STAGE_2)
                        }
                        CpuRelayAssociation.COOLING_STAGE_3.ordinal -> {
                            migrateCoolingStagePoint(equipRef, relay, port,CpuRelayAssociation.COOLING_STAGE_3)
                        }
                        CpuRelayAssociation.HEATING_STAGE_1.ordinal -> {
                            migrateHeatingStagePoint(equipRef, relay, port,CpuRelayAssociation.HEATING_STAGE_1)
                        }
                        CpuRelayAssociation.HEATING_STAGE_2.ordinal -> {
                            migrateHeatingStagePoint(equipRef, relay, port,CpuRelayAssociation.HEATING_STAGE_2)
                        }
                        CpuRelayAssociation.HEATING_STAGE_3.ordinal -> {
                            migrateHeatingStagePoint(equipRef, relay, port,CpuRelayAssociation.HEATING_STAGE_3)
                        }
                        CpuRelayAssociation.FAN_LOW_SPEED.ordinal -> {
                            migrateFanLowStagePoint(equipRef, relay, port)
                        }
                        CpuRelayAssociation.FAN_MEDIUM_SPEED.ordinal -> {
                            migrateFanMediumStagePoint(equipRef, relay, port)
                        }
                        CpuRelayAssociation.FAN_HIGH_SPEED.ordinal -> {
                            migrateFanHighStagePoint(equipRef, relay, port)
                        }
                        CpuRelayAssociation.FAN_ENABLED.ordinal -> {
                            migrateFanEnabledPoint(equipRef, relay, port)
                        }
                        CpuRelayAssociation.OCCUPIED_ENABLED.ordinal -> {
                            migrateOccupiedEnabledPoint(equipRef, relay, port)
                        }
                        CpuRelayAssociation.HUMIDIFIER.ordinal,CpuRelayAssociation.DEHUMIDIFIER.ordinal -> {
                            migrateHumidifierEnabledPoint(equipRef, relay, port)
                        }
                    }

                }
            }
        }

        private fun migrateCoolingStagePoint(equipRef: String, relay: String, port: Port, association: CpuRelayAssociation) {
            val logicalPoint = readPoint("$relay and logical", equipRef)
            if (logicalPoint.isNotEmpty()) {
                updateMarkers(
                    logicalPoint,
                    arrayOf("dxCooling", "writable"),
                    arrayOf(relay, "enum"), null
                )
                migratedRelays.add(port.ordinal)
                updateDuplicateLogicalPoint(
                    port,
                    logicalPoint[Tags.ID].toString(),
                    logicalPoint[Tags.GROUP].toString().toInt(),
                    equipRef,
                    association
                )
            }
        }

        private fun migrateHeatingStagePoint(equipRef: String, relay: String, port: Port, association: CpuRelayAssociation) {
            val logicalPoint = readPoint("$relay and logical", equipRef)
            if (logicalPoint.isNotEmpty()) {
                updateMarkers(
                    logicalPoint,
                    arrayOf("dxHeating", "writable"),
                    arrayOf(relay, "enum"), null
                )
                migratedRelays.add(port.ordinal)
                updateDuplicateLogicalPoint(
                    port,
                    logicalPoint[Tags.ID].toString(),
                    logicalPoint[Tags.GROUP].toString().toInt(),
                    equipRef,
                    association
                )
            }
        }

        private fun migrateFanLowStagePoint(equipRef: String, relay: String, port: Port) {
            val logicalPoint = readPoint("$relay and logical", equipRef)
            if (logicalPoint.isNotEmpty()) {
                updateMarkers(
                    logicalPoint,
                    arrayOf("speed", "writable", "low"),
                    arrayOf(relay, "enum", "stage1"), null
                )
                migratedRelays.add(port.ordinal)

                updateDuplicateLogicalPoint(
                    port,
                    logicalPoint[Tags.ID].toString(),
                    logicalPoint[Tags.GROUP].toString().toInt(),
                    equipRef,
                    CpuRelayAssociation.FAN_LOW_SPEED
                )
            }
        }

        private fun migrateFanMediumStagePoint(equipRef: String, relay: String, port: Port) {
            val logicalPoint = readPoint("$relay and logical", equipRef)
            if (logicalPoint.isNotEmpty()) {
                updateMarkers(
                    logicalPoint,
                    arrayOf("speed", "writable", "medium"),
                    arrayOf(relay, "enum", "stage2"), null
                )
                migratedRelays.add(port.ordinal)
                updateDuplicateLogicalPoint(
                    port,
                    logicalPoint[Tags.ID].toString(),
                    logicalPoint[Tags.GROUP].toString().toInt(),
                    equipRef,
                    CpuRelayAssociation.FAN_MEDIUM_SPEED
                )
            }
        }

        private fun migrateFanHighStagePoint(equipRef: String, relay: String, port: Port) {
            val logicalPoint = readPoint("$relay and logical", equipRef)
            if (logicalPoint.isNotEmpty()) {
                updateMarkers(
                    logicalPoint,
                    arrayOf("speed", "writable", "high"),
                    arrayOf(relay, "enum", "stage3"), null
                )
                migratedRelays.add(port.ordinal)
                updateDuplicateLogicalPoint(
                    port,
                    logicalPoint[Tags.ID].toString(),
                    logicalPoint[Tags.GROUP].toString().toInt(),
                    equipRef,
                    CpuRelayAssociation.FAN_HIGH_SPEED
                )
            }
        }

        private fun migrateFanEnabledPoint(equipRef: String, relay: String, port: Port) {
            val logicalPoint = readPoint("$relay and logical", equipRef)
            if (logicalPoint.isNotEmpty()) {
                updateMarkers(
                    logicalPoint,
                    arrayOf("writable", "enabled"),
                    arrayOf(relay, "enum", "enable"), null
                )
                migratedRelays.add(port.ordinal)
                updateDuplicateLogicalPoint(
                    port,
                    logicalPoint[Tags.ID].toString(),
                    logicalPoint[Tags.GROUP].toString().toInt(),
                    equipRef,
                    CpuRelayAssociation.FAN_ENABLED
                )
            }
        }

        private fun migrateOccupiedEnabledPoint(equipRef: String, relay: String, port: Port) {
            val logicalPoint = readPoint("$relay and logical", equipRef)
            if (logicalPoint.isNotEmpty()) {
                updateMarkers(
                    logicalPoint,
                    arrayOf("writable", "enabled","occupied"),
                    arrayOf(relay, "enum", "enable","occupancy"), null
                )
                migratedRelays.add(port.ordinal)
                updateDuplicateLogicalPoint(
                    port,
                    logicalPoint[Tags.ID].toString(),
                    logicalPoint[Tags.GROUP].toString().toInt(),
                    equipRef,
                    CpuRelayAssociation.FAN_ENABLED
                )
            }
        }

        private fun migrateHumidifierEnabledPoint(equipRef: String, relay: String, port: Port) {
            val logicalPoint = readPoint("$relay and logical", equipRef)
            if (logicalPoint.isNotEmpty()) {
                updateMarkers(
                    logicalPoint,
                    arrayOf("writable"),
                    arrayOf(relay, "enum", "fan"), null
                )
                migratedRelays.add(port.ordinal)
                updateDuplicateLogicalPoint(
                    port,
                    logicalPoint[Tags.ID].toString(),
                    logicalPoint[Tags.GROUP].toString().toInt(),
                    equipRef,
                    CpuRelayAssociation.FAN_ENABLED
                )
            }
        }

        private fun migrateThermistorSensorPoint(equipRef: String,equipDis: String) {
            val th1LogicalPoint = readPoint("th1 and logical", equipRef)
            if (th1LogicalPoint.isNotEmpty()) {
                updateMarkers(
                    th1LogicalPoint,
                    arrayOf(),
                    arrayOf("th1", "airflow", "in", "writable", "zone"), "$equipDis-airflowTempSensor"
                )
            }
            val th2LogicalPoint = readPoint("th2 and logical", equipRef)
            if (th2LogicalPoint.isNotEmpty()) {
                updateMarkers(
                    th2LogicalPoint,
                    arrayOf("door", "contact"),
                    arrayOf("th2", "cmd", "in", "writable", "zone"), "$equipDis-doorWindowSensor"
                )
            }
        }

        private fun migrateAnalogInSensorPoints(equipRef: String,equipDis: String) {
            updateAnalogInSensorPoint(equipRef, "analog1", 1,equipDis)
            if (!isAnalogInputMigrated) {
                updateAnalogInSensorPoint(equipRef, "analog2", 2,equipDis)
            }
        }

        private fun updateAnalogInSensorPoint(equipRef: String, analog: String, type: Int,equipDis: String) {
            val configAnalogIn = readDefaultValue("config and enabled and $analog and in", equipRef)
            if (configAnalogIn == 1) {
                when (readDefaultValue("config and association and $analog and in", equipRef)) {
                    AnalogInAssociation.KEY_CARD_SENSOR.ordinal -> analogInKeyCardMigration(
                        type,
                        equipRef,
                        equipDis
                    )
                    AnalogInAssociation.DOOR_WINDOW_SENSOR.ordinal -> analogWindowSensorMigration(
                        type,
                        equipRef,
                        equipDis
                    )
                    AnalogInAssociation.CURRENT_TX_0_10.ordinal -> migrateCurrentTransfer(
                        type,
                        equipRef,
                        analog,
                        10,
                        equipDis
                    )
                    AnalogInAssociation.CURRENT_TX_0_20.ordinal -> migrateCurrentTransfer(
                        type,
                        equipRef,
                        analog,
                        20,
                        equipDis
                    )
                    AnalogInAssociation.CURRENT_TX_0_50.ordinal -> migrateCurrentTransfer(
                        type,
                        equipRef,
                        analog,
                        50,
                        equipDis
                    )
                }
            }
        }

        private fun analogInKeyCardMigration(analogType: Int, equipRef: String,equipDis: String) {
            if (analogType == 1) {
                val logicalPoint = readPoint("analog1 and in and logical", equipRef)
                updateMarkers(
                    logicalPoint,
                    arrayOf(),
                    arrayOf("analog1", "cmd", "in", "enum", "zone"), "$equipDis-keyCardSensor"
                )
                return
            }
            if (analogType == 2) {
                val logicalPoint = readPoint("analog2 and in and logical", equipRef)
                updateMarkers(
                    logicalPoint,
                    arrayOf("keycard2"),
                    arrayOf("analog2", "cmd", "in", "enum", "zone", "keycard"), "$equipDis-keyCardSensor_2"
                )
                return
            }
        }

        private fun analogWindowSensorMigration(analogType: Int, equipRef: String,equipDis: String) {
            if (analogType == 1) {
                val logicalPoint = readPoint("analog1 and in and logical", equipRef)
                updateMarkers(
                    logicalPoint,
                    arrayOf("door", "contact", "window2"),
                    arrayOf("analog1", "cmd", "in", "writable", "zone", "window"), "$equipDis-doorWindowSensor_2"
                )
                return
            }
            if (analogType == 2) {
                val logicalPoint = readPoint("analog2 and in and logical", equipRef)
                updateMarkers(
                    logicalPoint,
                    arrayOf("door", "contact", "window3"),
                    arrayOf("analog2", "cmd", "in", "writable", "zone", "window"), "$equipDis-doorWindowSensor_3"
                )
                return
            }
        }

        private fun migrateCurrentTransfer(
            analogType: Int, equipRef: String, analog: String, currentTx: Int,equipDis: String
        ) {
            val logicalPoint = readPoint("$analog and in and logical", equipRef)
            if (currentTx == 10) {
                updateMarkers(
                    logicalPoint,
                    arrayOf(),
                    arrayOf("$analog", "cmd", "in"), "$equipDis-currentDrawn_10"
                )
                if (analogType == 1 && !isAnalogInputMigrated) {
                    migrateAnalog2(
                        logicalPoint,
                        equipRef,
                        AnalogInAssociation.CURRENT_TX_0_10.ordinal
                    )
                }
                return
            }
            if (currentTx == 20) {
                updateMarkers(
                    logicalPoint,
                    arrayOf("transformer20"),
                    arrayOf("$analog", "cmd", "in", "transformer"), "$equipDis-currentDrawn_20"
                )
                if (analogType == 1 && !isAnalogInputMigrated) {
                    migrateAnalog2(
                        logicalPoint,
                        equipRef,
                        AnalogInAssociation.CURRENT_TX_0_20.ordinal
                    )
                }
                return
            }
            if (currentTx == 50) {
                updateMarkers(
                    logicalPoint,
                    arrayOf("transformer50"),
                    arrayOf("$analog", "cmd", "in", "transformer"), "$equipDis-currentDrawn_50"
                )
                if (analogType == 1 && !isAnalogInputMigrated) {
                    migrateAnalog2(
                        logicalPoint,
                        equipRef,
                        AnalogInAssociation.CURRENT_TX_0_50.ordinal
                    )
                }
            }

        }

        private fun migrateAnalog2(
            logicalPoint: HashMap<Any, Any>,
            equipRef: String,
            analog1Association: Int
        ) {
            val configAnalogIn = readDefaultValue("config and enabled and analog2 and in", equipRef)
            if (configAnalogIn == 1) {
                val association =
                    readDefaultValue("config and association and analog2 and in", equipRef)
                if (association == analog1Association) {
                    DeviceUtil.updatePhysicalPointRef(
                        logicalPoint[Tags.GROUP].toString().toInt(),
                        Port.ANALOG_IN_TWO.name,
                        logicalPoint[Tags.ID].toString()
                    )
                    val ai2LogicalPoint = readPoint("analog2 and in and logical", equipRef)
                    if (ai2LogicalPoint.isNotEmpty()) {
                        CCUHsApi.getInstance().deleteEntity(ai2LogicalPoint[Tags.ID].toString())
                    }
                    isAnalogInputMigrated = true
                }
            } else {
                isAnalogInputMigrated = true
            }
        }

        private fun migrateAnalogOutputPoint(equipRef: String) {
            updateAnalogOutputPoint(equipRef, "analog1", Port.ANALOG_OUT_ONE)
            updateAnalogOutputPoint(equipRef, "analog2", Port.ANALOG_OUT_TWO)
            updateAnalogOutputPoint(equipRef, "analog3", Port.ANALOG_OUT_THREE)
        }

        private fun updateAnalogOutputPoint(equipRef: String, analog: String, port: Port) {
            if (!migratedAnalogOut.contains(port.ordinal)) {
                val analogOutConfig =
                    readDefaultValue("config and enabled and $analog and out", equipRef)
                if (analogOutConfig == 1) {
                    when (readDefaultValue("config and association and $analog and out", equipRef)) {
                        CpuAnalogOutAssociation.COOLING.ordinal -> migrateAnalogOutCooling(
                            equipRef,
                            analog,
                            port
                        )
                        CpuAnalogOutAssociation.FAN_SPEED.ordinal -> {
                            migrateAnalogOutFanSpeed(equipRef, analog, port)
                            migrateAnalogOutFanOptionPoints(equipRef, analog)
                        }
                        CpuAnalogOutAssociation.HEATING.ordinal -> migrateAnalogOutHeating(
                            equipRef,
                            analog,
                            port
                        )
                        CpuAnalogOutAssociation.DCV_DAMPER.ordinal -> migrateAnalogOutDcv(
                            equipRef,
                            analog,
                            port
                        )
                    }
                    migrateAnalogOutMinMaxPoints(equipRef, analog)
                }

            }
        }

        private fun updateAnalogDuplicatePoints(
            originalPort: Port, equipRef: String,
            association: CpuAnalogOutAssociation, nodeAddress: Int, pointId: String
        ) {

            val allPorts = mutableListOf(
                Port.ANALOG_OUT_ONE.ordinal,
                Port.ANALOG_OUT_TWO.ordinal,
                Port.ANALOG_OUT_THREE.ordinal
            )
            allPorts.remove(originalPort.ordinal)

            allPorts.forEach { port ->
                if (!migratedAnalogOut.contains(port)) {
                    val analogName = getAnalogOutName(port)
                    val config =
                        readDefaultValue("config and enabled and $analogName and out", equipRef)
                    if (config == 1) {
                        val mapped = readDefaultValue(
                            "config and association and $analogName and out",
                            equipRef
                        )
                        if (mapped == association.ordinal) {
                            DeviceUtil.updatePhysicalPointRef(
                                nodeAddress,
                                Port.values()[port].name,
                                pointId
                            )
                            migratedAnalogOut.add(port)
                            val logicalPoint =
                                readPoint("$analogName and out and logical", equipRef)
                            if (logicalPoint.isNotEmpty()) {
                                CCUHsApi.getInstance()
                                    .deleteEntity(logicalPoint[Tags.ID].toString())
                            }
                        }
                    }
                }
            }
        }

        private fun migrateAnalogOutCooling(equipRef: String, analog: String, port: Port) {
            val logicalPoint = readPoint("$analog and out and logical", equipRef)
            if (logicalPoint.isNotEmpty()) {
                updateMarkers(
                    logicalPoint,
                    arrayOf("analog", "output", "runtime", "dxCooling", "modulating"),
                    arrayOf(analog, "out"), null
                )
                migratedAnalogOut.add(port.ordinal)
                updateAnalogDuplicatePoints(
                    port,
                    logicalPoint[Tags.ID].toString(),
                    CpuAnalogOutAssociation.COOLING,
                    logicalPoint[Tags.GROUP].toString().toInt(),
                    equipRef,
                )
            }
        }

        private fun migrateAnalogOutHeating(equipRef: String, analog: String, port: Port) {
            val logicalPoint = readPoint("$analog and out and logical", equipRef)
            if (logicalPoint.isNotEmpty()) {
                updateMarkers(
                    logicalPoint,
                    arrayOf("analog", "output", "runtime", "elecHeating", "modulating"),
                    arrayOf(analog, "out"), null
                )
                migratedAnalogOut.add(port.ordinal)
                updateAnalogDuplicatePoints(
                    port,
                    logicalPoint[Tags.ID].toString(),
                    CpuAnalogOutAssociation.HEATING,
                    logicalPoint[Tags.GROUP].toString().toInt(),
                    equipRef,
                )
            }
        }

        private fun migrateAnalogOutFanSpeed(equipRef: String, analog: String, port: Port) {
            val logicalPoint = readPoint("$analog and out and logical", equipRef)
            if (logicalPoint.isNotEmpty()) {
                updateMarkers(
                    logicalPoint,
                    arrayOf("analog", "output", "runtime", "run"),
                    arrayOf(analog, "out"), null
                )
                migratedAnalogOut.add(port.ordinal)
                updateAnalogDuplicatePoints(
                    port,
                    logicalPoint[Tags.ID].toString(),
                    CpuAnalogOutAssociation.FAN_SPEED,
                    logicalPoint[Tags.GROUP].toString().toInt(),
                    equipRef,
                )
            }
        }

        private fun migrateAnalogOutDcv(equipRef: String, analog: String, port: Port) {
            val logicalPoint = readPoint("$analog and out and logical", equipRef)
            if (logicalPoint.isNotEmpty()) {
                updateMarkers(
                    logicalPoint,
                    arrayOf("analog", "output", "runtime", "actuator"),
                    arrayOf(analog, "out"), null
                )
                migratedAnalogOut.add(port.ordinal)
                updateAnalogDuplicatePoints(
                    port,
                    logicalPoint[Tags.ID].toString(),
                    CpuAnalogOutAssociation.DCV_DAMPER,
                    logicalPoint[Tags.GROUP].toString().toInt(),
                    equipRef,
                )
            }
        }

        private fun migrateAnalogOutFanOptionPoints(equipRef: String, analog: String) {
            val lowPoint = readPoint("$analog and out and fan and low", equipRef)
            val mediumPoint = readPoint("$analog and out and fan and medium", equipRef)
            val highPoint = readPoint("$analog and out and fan and high", equipRef)
            if (lowPoint.isNotEmpty()) {
                updateMarkers(
                    lowPoint,
                    arrayOf("output", "speed"),
                    arrayOf("out"), null
                )
            }
            if (mediumPoint.isNotEmpty()) {
                updateMarkers(
                    mediumPoint,
                    arrayOf("output", "speed"),
                    arrayOf("out"), null
                )
            }
            if (highPoint.isNotEmpty()) {
                updateMarkers(
                    highPoint,
                    arrayOf("output", "speed"),
                    arrayOf("out"), null
                )
            }

        }

        private fun migrateAnalogOutMinMaxPoints(equipRef: String, analog: String) {
            val minPoint = readPoint("$analog and out and min", equipRef)
            val maxPoint = readPoint("$analog and out and max", equipRef)
            if (minPoint.isNotEmpty()) {
                updateMarkers(
                    minPoint,
                    arrayOf("output", "actuator"),
                    arrayOf("out", "shortDis"), null
                )
            }
            if (maxPoint.isNotEmpty()) {
                updateMarkers(
                    maxPoint,
                    arrayOf("output", "actuator"),
                    arrayOf("out", "shortDis"), null
                )
            }
        }

        private fun updateDuplicateLogicalPoint(
            originalPort: Port,
            pointId: String,
            nodeAddress: Int,
            equipRef: String,
            association: CpuRelayAssociation
        ) {
            val allPorts = getAllPorts()
            allPorts.remove(originalPort.ordinal)
            allPorts.forEach { port ->
                if (!migratedRelays.contains(port)) {
                    val relayName = getPortRelay(port)
                    val config = readDefaultValue("config and enabled and $relayName", equipRef)
                    if (config == 1) {
                        val mapped =
                            readDefaultValue("config and association and $relayName", equipRef)
                        if (mapped == association.ordinal) {
                            DeviceUtil.updatePhysicalPointRef(
                                nodeAddress,
                                Port.values()[port].name,
                                pointId
                            )
                            migratedRelays.add(port)
                            val logicalPoint = readPoint("$relayName and logical", equipRef)
                            if (logicalPoint.isNotEmpty()) {
                                CCUHsApi.getInstance()
                                    .deleteEntity(logicalPoint[Tags.ID].toString())
                            }
                        }
                    }
                }
            }
        }

        private fun getAllPorts(): MutableList<Int> {
            return mutableListOf(
                Port.RELAY_ONE.ordinal,
                Port.RELAY_TWO.ordinal,
                Port.RELAY_THREE.ordinal,
                Port.RELAY_FOUR.ordinal,
                Port.RELAY_FIVE.ordinal,
                Port.RELAY_SIX.ordinal
            )
        }

        private fun getPortRelay(port: Int): String?{
            return when(port) {
                Port.RELAY_ONE.ordinal -> "relay1"
                Port.RELAY_TWO.ordinal -> "relay2"
                Port.RELAY_THREE.ordinal -> "relay3"
                Port.RELAY_FOUR.ordinal -> "relay4"
                Port.RELAY_FIVE.ordinal -> "relay5"
                Port.RELAY_SIX.ordinal -> "relay6"
                else -> null
            }
        }

        private fun getAnalogOutName(port: Int): String? {
            return when (port) {
                Port.ANALOG_OUT_ONE.ordinal -> "relay1"
                Port.ANALOG_OUT_TWO.ordinal -> "relay2"
                Port.ANALOG_OUT_THREE.ordinal -> "relay3"
                else -> null
            }
        }

        // Configuration points
        private fun migrateTempOffSetPoint(equipRef: String,equipName: String) {
            val tempOffsetPoint = readPoint("config and temperature and offset", equipRef)
            if (tempOffsetPoint.isNotEmpty()) {
                updateMarkers(
                    tempOffsetPoint, arrayOf("temp"), arrayOf("temperature"), "$equipName-temperatureOffset"
                )
            }
        }

        private fun migrateAutoForceOccupyPoint(equipRef: String,equipName: String) {
            val autoForceOccupiedPoint =
                readPoint("config and auto and forced and enabled", equipRef)
            if (autoForceOccupiedPoint.isNotEmpty()) {
                updateMarkers(
                    autoForceOccupiedPoint, arrayOf("occupancy", "cmd"), arrayOf("occupied"), "$equipName-autoForceOccupied"
                )
            }
            val autoAway = readPoint("config and auto and away and enabled", equipRef)
            if (autoAway.isNotEmpty()) {
                updateMarkers(
                    autoAway, arrayOf(), arrayOf(), "$equipName-autoAway"
                )
            }
        }

        private fun migrateCo2ConfigPoints(equipRef: String,equipName: String) {
            val co2DamperOpeningPoint =
                readPoint("config and co2 and damper and opening and rate", equipRef)
            val co2ThresholdPoint = readPoint("config and co2 and threshold and sp", equipRef)
            val co2TargetPoint = readPoint("config and co2 and target and sp", equipRef)

            if (co2DamperOpeningPoint.isNotEmpty()) {
                updateMarkers(
                    co2DamperOpeningPoint, arrayOf("actuator", "sp"), arrayOf(), "$equipName-co2DamperOpeningRate"
                )
            }
            if (co2ThresholdPoint.isNotEmpty()) {
                updateMarkers(
                    co2ThresholdPoint, arrayOf("concentration"), arrayOf(), "$equipName-zoneCO2Threshold"
                )
            }
            if (co2TargetPoint.isNotEmpty()) {
                updateMarkers(
                    co2TargetPoint, arrayOf("concentration"), arrayOf(), "$equipName-zoneCO2Target"
                )
            }
        }

        private fun migrateVocConfigPoints(equipRef: String,equipName: String) {
            val vocThresholdPoint = readPoint("config and voc and threshold", equipRef)
            val vocTargetPoint = readPoint("config and voc and target", equipRef)

            if (vocThresholdPoint.isNotEmpty()) {
                updateMarkers(
                    vocThresholdPoint,
                    arrayOf("concentration", "sp"),
                    arrayOf("air", "cov", "cur", "current", "logical", "sensor"),
                    "$equipName-zoneVOCThreshold"
                )
            }
            if (vocTargetPoint.isNotEmpty()) {
                updateMarkers(
                    vocTargetPoint,
                    arrayOf("concentration", "sp"),
                    arrayOf("air", "cov", "cur", "current", "logical", "sensor"),
                    "$equipName-zoneVOCTarget"
                )
            }
        }

        private fun migratePm25ConfigPoints(equipRef: String,equipName: String) {
            val pm2p5ThresholdPoint = readPoint("config and pm2p5 and threshold", equipRef)
            val pm2p5TargetPoint = readPoint("config and pm2p5 and target", equipRef)

            if (pm2p5ThresholdPoint.isNotEmpty()) {
                updateMarkers(
                    pm2p5ThresholdPoint,
                    arrayOf("concentration", "sp"),
                    arrayOf("air", "cov", "cur", "current", "logical", "sensor"),
                    "$equipName-zonePm2p5Threshold"
                )
            }
            if (pm2p5TargetPoint.isNotEmpty()) {
                updateMarkers(
                    pm2p5TargetPoint,
                    arrayOf("concentration", "sp"),
                    arrayOf("air", "cov", "cur", "current", "logical", "sensor"),
                    "$equipName-zonePm2p5Target"
                )
            }
        }

        private fun migrateLoopOutputPoint(equipRef: String,equipName: String) {
            val coolingLoopOutput = readPoint("cooling and loop and output", equipRef)
            val heatingLoopOutput = readPoint("heating and loop and output", equipRef)
            val fanLoopOutput = readPoint("fan and loop and output", equipRef)

            if (coolingLoopOutput.isNotEmpty()) {
                updateMarkers(
                    coolingLoopOutput,
                    arrayOf("runtime", "writable"), arrayOf("out"), "$equipName-coolingLoopOutput"
                )
            }
            if (heatingLoopOutput.isNotEmpty()) {
                updateMarkers(
                    heatingLoopOutput,
                    arrayOf("runtime", "writable"), arrayOf("out"), "$equipName-heatingLoopOutput"
                )
            }
            if (fanLoopOutput.isNotEmpty()) {
                updateMarkers(
                    fanLoopOutput,
                    arrayOf("runtime", "writable"), arrayOf("out"), "$equipName-fanLoopOutput"
                )
            }
        }


        private fun migrateSensingInputPoints(equipRef: String) {
            val keycardEnabled = readPoint("enabled and sensing and keycard", equipRef)
            val windowEnabled = readPoint("enabled and sensing and window", equipRef)
            val windowInput = readPoint("input and sensor and keycard", equipRef)
            val keycardInput = readPoint("input and sensor and window", equipRef)

            if (keycardEnabled.isNotEmpty()) {
                updateMarkers(keycardEnabled, arrayOf(), arrayOf(),null)
            }
            if (windowEnabled.isNotEmpty()) {
                updateMarkers(windowEnabled, arrayOf(), arrayOf(),null)
            }
            if (windowInput.isNotEmpty()) {
                updateMarkers(windowInput, arrayOf(), arrayOf(),null)
            }
            if (keycardInput.isNotEmpty()) {
                updateMarkers(keycardInput, arrayOf(), arrayOf(),null)
            }
        }

        private fun migrateRelayPoints(equipRef: String) {
            for (i in 1..6) {
                migrateRelayConfigAssociation("relay$i", equipRef)
            }
        }

        private fun migrateRelayConfigAssociation(relay: String, equipRef: String) {
            val config = readPoint("config and enabled and $relay", equipRef)
            val association = readPoint("config and association and $relay", equipRef)
            if (config.isNotEmpty()) {
                updateMarkers(config, arrayOf("cmd"), arrayOf(), null)
            }
            if (association.isNotEmpty()) {
                updateMarkers(association, arrayOf(), arrayOf(), null)
            }
        }

        private fun migrateAnalogOutPoints(equipRef: String) {
            for (i in 1..3) {
                migrateAnalogOutConfigAssociation("analog$i", equipRef)
            }
        }

        private fun migrateAnalogOutConfigAssociation(analog: String, equipRef: String) {
            val config = readPoint("config and enabled and out and $analog", equipRef)
            val association = readPoint("config and association and out and $analog", equipRef)
            if (config.isNotEmpty()) {
                updateMarkers(config, arrayOf("cmd", "output"), arrayOf("out"), null)
            }
            if (association.isNotEmpty()) {
                updateMarkers(association, arrayOf("output"), arrayOf("out"), null)
            }
        }

        private fun migrateAnalogInPoints(equipRef: String) {
            for (i in 1..2) {
                migrateAnalogInConfigAssociation("analog$i", equipRef)
            }
        }

        private fun migrateAnalogInConfigAssociation(analog: String, equipRef: String) {
            val config = readPoint("config and enabled and in and $analog", equipRef)
            val association = readPoint("config and association and in and $analog", equipRef)
            if (config.isNotEmpty()) {
                updateMarkers(config, arrayOf("cmd", "input"), arrayOf("in"), null)
            }
            if (association.isNotEmpty()) {
                updateMarkers(association, arrayOf("input"), arrayOf("in"), null)
            }
        }

        private fun migrateThermistorPoints(equipRef: String,equipName: String) {
            val th1 = readPoint("config and th1 and enabled", equipRef)
            val th2 = readPoint("config and th2 and enabled", equipRef)

            if (th1.isNotEmpty()) {
                updateMarkers(
                    th1,
                    arrayOf("air", "cmd"),
                    arrayOf("airflow", "sensor", "th1"), "$equipName-enableAirflowTempSensor"
                )
            }
            if (th2.isNotEmpty()) {
                updateMarkers(
                    th2,
                    arrayOf("his", "cmd"),
                    arrayOf("sensor", "th2"), "$equipName-enableDoorWindowSensor"
                )
            }

        }

        private fun migrateUserIntentPoints(equipRef: String) {

            val fanMode = readPoint("fan and mode", equipRef)
            val conditioningMode = readPoint("conditioning and mode", equipRef)
            val humidifier = readPoint("humidifier and target", equipRef)
            val dehumidifier = readPoint("dehumidifier and target", equipRef)
            if (fanMode.isNotEmpty()) {
                updateMarkers(
                    fanMode,
                    arrayOf("sp"),
                    arrayOf("userIntent", "control"), null
                )
            }
            if (conditioningMode.isNotEmpty()) {
                updateMarkers(
                    conditioningMode,
                    arrayOf("sp"),
                    arrayOf("userIntent", "control"), null
                )
            }
            if (humidifier.isNotEmpty()) {
                updateMarkers(
                    humidifier,
                    arrayOf(), arrayOf("userIntent"), null
                )
            }
            if (dehumidifier.isNotEmpty()) {
                updateMarkers(
                    dehumidifier,
                    arrayOf(), arrayOf("userIntent"), null
                )
            }
        }

        private fun readPoint(query: String, equipRef: String): HashMap<Any, Any> {
            return CCUHsApi.getInstance()
                .readEntity("zone and $query and equipRef == \"$equipRef\"")
        }

        private fun readDefaultValue(query: String, equipRef: String): Int {
            return CCUHsApi.getInstance()
                .readDefaultVal("zone and $query and equipRef == \"$equipRef\"").toInt()
        }

    }
}