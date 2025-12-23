package a75f.io.device.mesh

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.Zone
import a75f.io.device.connect.parseFloatFromFourBytes
import a75f.io.device.connect.parseFloatFromFourBytesLittle
import a75f.io.device.connect.parseFloatPcn
import a75f.io.device.connect.parseIntFromTwoBytes
import a75f.io.device.mesh.hyperstat.HyperStatMessageSender.writeMessageBytesToUsb
import a75f.io.device.pcn
import a75f.io.device.pcn.PCN_Settings_t
import a75f.io.device.serial.CcuToCmOverUsbDatabaseSeedPcnMessage_t
import a75f.io.device.serial.CcuToCmOverUsbPcnSettingsMessage_t
import a75f.io.device.serial.MessageType
import a75f.io.device.serial.PCNDeviceTypes
import a75f.io.device.serial.ProfileMap_t
import a75f.io.device.serial.SmartNodeSettings2_t
import a75f.io.device.serial.SmartNodeSettings_t
import a75f.io.device.util.DeviceConfigurationUtil.Companion.getUserConfiguration
import a75f.io.domain.api.Domain.hayStack
import a75f.io.domain.api.DomainName
import a75f.io.domain.util.ModelNames
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.ZoneState
import a75f.io.logic.bo.building.pcn.PCNUtil.Companion.getConnectNodeEquip
import a75f.io.logic.bo.building.pcn.PCNUtil.Companion.getPCNForZone
import a75f.io.logic.bo.building.pcn.PCNUtil.Companion.getPcnByNodeAddress
import a75f.io.logic.bo.building.pcn.PCNUtil.Companion.getPcnNodeDevice
import a75f.io.logic.tuners.TunerUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays
import java.util.Calendar

class LPcn {

    companion object {
        const val HYPERSTAT_MESSAGE_ADDR_START_INDEX: Int = 1
        const val HYPERSTAT_MESSAGE_ADDR_END_INDEX: Int = 5
        const val HYPERSTAT_MESSAGE_TYPE_INDEX: Int = 13
        const val HYPERSTAT_SERIALIZED_MESSAGE_START_INDEX: Int = 17
        val settingsMessage1 = mutableMapOf<Short, CcuToCmOverUsbPcnSettingsMessage_t>()
        val settingsMessage2 = mutableMapOf<Short, CcuToCmOverUsbPcnSettingsMessage_t>()

        /**
         * Builds and returns a seed message (`CcuToCmOverUsbDatabaseSeedPcnMessage_t`)
         * that initializes or synchronizes smart node configuration with the CM.
         *
         * Steps performed:
         * 1. Create a new `CcuToCmOverUsbDatabaseSeedPcnMessage_t` object.
         * 2. Set the message type to `CCU_TO_CM_OVER_USB_DATABASE_SEED_SN`.
         * 3. Assign the smart node address from the provided `address` parameter.
         * 4. Attempt to set the encryption key retrieved from `L.getEncryptionKey()`.
         *    Any exceptions encountered while doing so are caught and printed
         *    to the error stream but do not prevent message creation.
         * 5. Populate the message’s settings block by calling
         *    `fillSmartNodeSettings(...)` with details like zone,
         *    equipment ID, and node profile.
         * 6. Return the constructed seed message.
         *
         * @param zone The logical zone information where the smart node is assigned (nullable).
         * @param address The short address of the target smart node.
         * @param equipId An optional equipment identifier tied to the smart node.
         * @param profile An optional node profile string containing configuration data.
         * @return A fully constructed `CcuToCmOverUsbDatabaseSeedPcnMessage_t` ready to be sent.
         */
        @JvmStatic
        fun getSeedMessage(
            zone: Zone?,
            address: Short,
            equipId: String?,
            profile: String?
        ): CcuToCmOverUsbDatabaseSeedPcnMessage_t {
            val seedMessage =
                CcuToCmOverUsbDatabaseSeedPcnMessage_t()
            seedMessage.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN)
            seedMessage.smartNodeAddress.set(address.toInt())
            try {
                seedMessage.putEncrptionKey(L.getEncryptionKey())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            fillSmartNodeSettings(seedMessage.settings, zone, address, equipId)
            fillSmartNodeSettings2(seedMessage.settings2, zone, address, equipId)
            return seedMessage
        }

        @JvmStatic
        fun sendPcnSettingsMessage(zoneId: String, address: Short) {
            val settingsMessage:PCN_Settings_t = getPcnSettingsMessage(zoneId);
            CcuLog.i(
                L.TAG_CCU_SERIAL,
                "Send Pcn Proto Buf Message "
            )
            CcuLog.i(L.TAG_CCU_SERIAL, settingsMessage.toString())

            writeMessageBytesToUsb(
                address.toInt(),
                MessageType.CM_TO_DEVICE_OVER_AIR_PCN_SETTINGS,
                settingsMessage.toByteArray()
            )
        }

        fun fillSmartNodeSettings2(settings2: SmartNodeSettings2_t, zone: Zone?, address: Short, equipId: String?) {
            try {
                settings2.profileMap2.set(ProfileMap_t.PROFILE_MAP_VAV_NO_FAN)
            } catch (e: java.lang.Exception) {
                CcuLog.d(L.TAG_CCU_SERIAL, "Error in setting profile map 2: ${e.message}")
            }
        }

        /**
         * Populates the given `SmartNodeSettings_t` object with configuration values
         * derived from user, zone, device, and system APIs. This ensures that the smart
         * node is initialized with correct temperature limits, damper settings, offsets,
         * tuner parameters, airflow conditions, and user preferences.
         *
         * Steps performed:
         * 1. Retrieve device information for the given zone and build an equipment reference.
         * 2. Attempt to set user temperature limits:
         *    - Read cooling and heating deadband values using CCUHsApi queries.
         *    - If unavailable, fallback defaults (75°F max, 69°F min) are applied.
         * 3. Configure damper settings (max/min open) depending on the zone's current
         *    heating/cooling state.
         * 4. Apply temperature offset values retrieved from `LSmartNode`.
         * 5. Configure PI (Proportional–Integral) tuner constants:
         *    - If values are available, scale them into `Short` form and set accordingly.
         *    - If missing, fallback defaults are applied (50, 50, 15, 30).
         * 6. Read airflow heating and cooling temperature setpoints using `CCUHsApi`
         *    against device reference equipment.
         * 7. Apply UI-related settings such as centigrade preference, hold mode, and
         *    time format defaults.
         * 8. Attempt to configure occupancy detection from `LSmartNode`, or set to
         *    disabled if unavailable.
         *
         * @param settings  The `SmartNodeSettings_t` object to populate with values.
         * @param zone      The `Zone` object representing the logical group for this node (must not be null).
         * @param address   The short address of the node for which the settings are filled.
         * @param equipId   Optional equipment identifier used for mapping tuner parameters.
         */
        private fun fillSmartNodeSettings(settings: SmartNodeSettings_t, zone: Zone?, address: Short, equipId: String?) {
            val ccuHsApi = CCUHsApi.getInstance()

            val deviceMap = getPCNForZone(zone!!.id, CCUHsApi.getInstance())
            val deviceRef = Equip.Builder().setHashMap(deviceMap).build()

            try {
                val coolingDeadband =
                    ccuHsApi.readPointPriorityValByQuery("cooling and deadband and schedulable and roomRef == \"" + zone.id + "\"")

                val heatingDeadband =
                    ccuHsApi.readPointPriorityValByQuery("heating and deadband and schedulable and roomRef == \"" + zone.id + "\"")
                settings.minUserTemp.set((DeviceUtil.getMinUserTempLimits(zone.id, true)
                        - heatingDeadband).toInt().toShort())
                settings.maxUserTemp.set((DeviceUtil.getMaxUserTempLimits(zone.id, false)
                        + coolingDeadband).toInt().toShort())
            } catch (e: java.lang.Exception) {
                //Equips not having user temps are bound to throw exception
                settings.maxUserTemp.set(75.toShort())
                settings.minUserTemp.set(69.toShort())
            }

            if (LSmartNode.getStatus(address) == ZoneState.HEATING.ordinal.toDouble()) {
                settings.maxDamperOpen.set(
                    LSmartNode.getDamperLimit("heating", "max", address).toInt().toShort()
                )
                settings.minDamperOpen.set(
                    LSmartNode.getDamperLimit("heating", "min", address).toInt().toShort()
                )
            } else {
                settings.maxDamperOpen.set(
                    LSmartNode.getDamperLimit("cooling", "max", address).toInt().toShort()
                )
                settings.minDamperOpen.set(
                    LSmartNode.getDamperLimit("cooling", "min", address).toInt().toShort()
                )
            }

            settings.temperatureOffset.set(
                (LSmartNode.getTempOffset(address)).toInt().toShort()
            )

            try {
                settings.proportionalConstant.set(
                    (TunerUtil.getProportionalGain(equipId) * 100).toInt().toShort()
                )
                settings.integralConstant.set(
                    (TunerUtil.getIntegralGain(equipId) * 100).toInt().toShort()
                )
                settings.proportionalTemperatureRange.set(
                    (TunerUtil.getProportionalSpread(
                        equipId
                    ) * 10).toInt().toShort()
                )
                settings.integrationTime.set(
                    TunerUtil.getIntegralTimeout(equipId).toInt().toShort()
                )
            } catch (e: java.lang.Exception) {
                //Equips not having PI tuners are bound to throw exception
                settings.proportionalConstant.set(50.toShort())
                settings.integralConstant.set(50.toShort())
                settings.proportionalTemperatureRange.set(15.toShort())
                settings.integrationTime.set(30.toShort())
            }

            settings.airflowHeatingTemperature.set(
                ccuHsApi.readPointPriorityValByQuery("zone and ( domainName==\"" + DomainName.vavHeatingAirflowTemp + "\" or domainName == \"" + DomainName.dabHeatingAirflowTemp + "\") and equipRef==\"" + deviceRef + "\"")
                    .toInt()
                    .toShort()
            )
            settings.airflowCoolingTemperature.set(
                ccuHsApi.readPointPriorityValByQuery("zone and ( domainName==\"" + DomainName.vavCoolingAirflowTemp + "\" or domainName == \"" + DomainName.dabCoolingAirflowTemp + "\") and equipRef==\"" + deviceRef + "\"")
                    .toInt()
                    .toShort()
            )
            settings.showCentigrade.set((getUserConfiguration()).toInt().toShort())
            settings.displayHold.set(0.toShort())
            settings.militaryTime.set(0.toShort())
            try {
                settings.enableOccupationDetection.set(
                    LSmartNode.getConfigNumVal(
                        "enable and occupancy",
                        address
                    ).toInt().toShort()
                )
            } catch (e: java.lang.Exception) {
                settings.enableOccupationDetection.set(0.toShort())
            }
        }

        /**
         * Builds a `CcuToCmOverUsbPcnSettingsMessage_t` that contains register
         * configuration information for a given PCN smart node, its connected
         * modules, and external equipment devices. This message is used
         * for regular PCN settings updates over USB/Modbus communication.
         *
         * Steps performed:
         *
         * 1. **Initialize Settings Message**
         *    - Clear any existing configurations.
         *    - Set the message type to `CCU_TO_CM_MODBUS_SERVER_REGULAR_UPDATE_SETTINGS`.
         *    - Assign node address.
         *    - Set the interval for updates (40 seconds).
         *
         * 2. **PCN Native Device Configuration**
         *    - Resolve the PCN device ID via Haystack query by address.
         *    - Retrieve all non-writable "writable points" with register numbers
         *      for the given zone.
         *    - If points exist:
         *      - Add a slave config (slave ID `100`) for the PCN device.
         *      - Set device type to `PCN_NATIVE_DEVICE`.
         *      - Populate register numbers and point IDs.
         *
         * 3. **Connected Modules Configuration (per PCN device)**
         *    - Query Haystack for all connect module devices linked to the PCN device.
         *    - For each connect module device:
         *      - Fetch all register-based writable points belonging to that slave.
         *      - Add a slave config with the given `slaveId` if points exist.
         *      - Assign device type `PCN_CONNECT_MODULE`.
         *      - Populate register-val mappings and point IDs into `settingsMessage1`.
         *
         * 4. **External Equipment Devices Configuration**
         *    - Query Haystack for all external Modbus devices referencing the PCN device.
         *    - For each such device:
         *      - Retrieve all non-writable register-based points in the given zone.
         *      - If points are found, add a slave config with proper slave ID.
         *      - Assign device type `PCN_EXTERNAL_EQUIP`.
         *      - Populate register addresses and related point IDs.
         *
         * 5. **Return Built Settings Message**
         *
         * @param zone    The zone object specifying the logical grouping of the node.
         * @param address The node address (short) representing the target PCN node.
         * @return A fully populated `CcuToCmOverUsbPcnSettingsMessage_t` including
         *         settings for the PCN device, any connected modules, and external equipment.
         */
        @JvmStatic
        fun getPcnSettings1Message(
            zone: Zone?,
            address: Short,
        ): CcuToCmOverUsbPcnSettingsMessage_t? {
            settingsMessage1.put(address, CcuToCmOverUsbPcnSettingsMessage_t())
            settingsMessage1.get(address)?.let { settingsMessage1 ->
                settingsMessage1.clearConfigs()
                settingsMessage1.messageType = MessageType.CCU_TO_CM_MODBUS_SERVER_REGULAR_UPDATE_SETTINGS.ordinal
                settingsMessage1.nodeAddress = address.toInt()
                settingsMessage1.intervalInSecs = 40 // 40 seconds
                // Step 1: Get the PCN device id for the given address
                var pcnDeviceId = hayStack.readEntity("domainName == \"${ModelNames.pcnDevice}\" and addr == \"$address\"").get("id").toString()
                val writablePoints = hayStack.readAllEntities("point and not writable and pcn and registerNumber and roomRef == \"${zone?.id}\"")
                var curRegIndex = 0
                var writablePointsSize = 0
                writablePoints.forEachIndexed{ index, writablePoint->
                    writablePointsSize += if(writablePoint.get(Tags.PARAMETER_DEFINITION_TYPE).toString() == "float") 2 else 1
                }
                // Step 1.a: Add a slave config for PCN device if writable points are present
                if(writablePoints.size > 0) {
                    settingsMessage1.addSlaveConfig(100, writablePointsSize)
                    settingsMessage1.getSlaveConfig(0).deviceType = PCNDeviceTypes.PCN_NATIVE_DEVICE // PCN device
                }

                // Step 1.b: Populate the register values for PCN device
                writablePoints.forEachIndexed{ index, writablePoint->
                    val addr = writablePoint[Tags.REG_NUM].toString().toInt()
                    settingsMessage1.getSlaveConfig(0).setRegisterValue(curRegIndex, addr)
                    settingsMessage1.getSlaveConfig(0).pointId[curRegIndex] = writablePoints[index][Tags.ID].toString()
                    // Update the register type first, then update the register value
                    updateRegisterType(writablePoint, settingsMessage1,0,  curRegIndex)
                    curRegIndex++
                    if(writablePoint.get(Tags.PARAMETER_DEFINITION_TYPE).toString() == "float") {
                        // Each float occupies 2 registers
                        settingsMessage1.getSlaveConfig(0).setRegisterValue(curRegIndex, addr+1)
                        settingsMessage1.getSlaveConfig(0).pointId[curRegIndex] = writablePoints[index][Tags.ID].toString()
                        updateRegisterType(writablePoint, settingsMessage1,0,  curRegIndex)
                        curRegIndex++
                    }
                }
                if (writablePointsSize > 0) settingsMessage1.getSlaveConfig(0).sortSlaveConfigsByRegisters()

                // Step 2: Get all connect module devices for the given PCN device
                val connectModuleDevices = hayStack.readAllEntities("device and domainName == \"${ModelNames.connectNodeDevice}\" and deviceRef == \"$pcnDeviceId\"")
                connectModuleDevices.forEachIndexed {index, device ->
                    val slaveId = device[Tags.ADDR].toString().toInt()
                    val writablePoints = hayStack.readAllEntities("point and not writable and connectModule and registerNumber and roomRef == \"${zone?.id}\" and group == \"$slaveId\"")
                    curRegIndex = 0
                    writablePointsSize = 0
                    writablePoints.forEachIndexed{ index, writablePoint->
                        writablePointsSize += if(writablePoint.get(Tags.PARAMETER_DEFINITION_TYPE).toString() == "float") 2 else 1
                    }

                    // Just fetch the slave id from first writable point
                    val modifiedIndex = settingsMessage1.getAllConfigs().size + 1 // Since 0 is used by PCN device
                    if(writablePoints.size > 0) {
                        val slaveId = writablePoints[0][Tags.GROUP].toString().toInt()
                        settingsMessage1.addSlaveConfig(slaveId, writablePointsSize)
                        settingsMessage1.getSlaveConfig(modifiedIndex - 1).deviceType = PCNDeviceTypes.PCN_CONNECT_MODULE // PCN device
                    }
                    // Step 2 : Update register values for connect module
                    writablePoints.forEachIndexed{ innerIndex, writablePoint->
                        val addr = writablePoint[Tags.REG_NUM].toString().toInt()
                        settingsMessage1.getSlaveConfig(modifiedIndex - 1).setRegisterValue(curRegIndex, addr)
                        settingsMessage1.getSlaveConfig(modifiedIndex - 1).pointId[curRegIndex] = writablePoints[innerIndex][Tags.ID].toString()
                        // Update the register type first, then update the register value
                        updateRegisterType(writablePoint, settingsMessage1, modifiedIndex - 1, curRegIndex)
                        curRegIndex++
                        if(writablePoint.get(Tags.PARAMETER_DEFINITION_TYPE).toString() == "float") {
                            // Each float occupies 2 registers
                            settingsMessage1.getSlaveConfig(modifiedIndex - 1).setRegisterValue(curRegIndex, addr+1)
                            settingsMessage1.getSlaveConfig(modifiedIndex - 1).pointId[curRegIndex] = writablePoints[index][Tags.ID].toString()
                            updateRegisterType(writablePoint, settingsMessage1, modifiedIndex - 1, curRegIndex)
                            curRegIndex++
                        }
                    }
                    if (writablePointsSize > 0) settingsMessage1.getSlaveConfig(modifiedIndex - 1).sortSlaveConfigsByRegisters()
                }

                // Step 3: Get all external  devices for the given PCN device
                val externalEquipDevices = hayStack.readAllEntities("modbus and connectModule and deviceRef == \"$pcnDeviceId\"")
                externalEquipDevices.forEachIndexed {index, device ->
                    val writablePoints = hayStack.readAllEntities("point and not writable and modbus and registerNumber and roomRef == \"${zone?.id}\"")
                    curRegIndex = 0
                    writablePointsSize = 0
                    writablePoints.forEachIndexed{ index, writablePoint->
                        writablePointsSize += if(writablePoint.get(Tags.PARAMETER_DEFINITION_TYPE).toString() == "float") 2 else 1
                    }
                    // Just fetch the slave id from first writable point
                    val modifiedIndex = settingsMessage1.getAllConfigs().size + 1 // Since 0 is used by PCN device
                    if(writablePoints.size > 0) {
                        val slaveId = writablePoints[0][Tags.GROUP].toString().toInt()
                        settingsMessage1.addSlaveConfig(slaveId, writablePointsSize)
                        settingsMessage1.getSlaveConfig(modifiedIndex - 1).deviceType = PCNDeviceTypes.PCN_EXTERNAL_EQUIP // PCN device
                    }
                    // Step 3.a : Get the connect module address
                    writablePoints.forEachIndexed{ innerIndex, writablePoint->
                        val addr = writablePoint[Tags.REG_NUM].toString().toInt()
                        settingsMessage1.getSlaveConfig(modifiedIndex - 1).setRegisterValue(curRegIndex, addr)
                        settingsMessage1.getSlaveConfig(modifiedIndex - 1).pointId[curRegIndex] = writablePoints[innerIndex][Tags.ID].toString()
                        // Update the register type first, then update the register value
                        updateRegisterType(writablePoint, settingsMessage1, modifiedIndex - 1, curRegIndex)
                        curRegIndex++
                        if(writablePoint.get(Tags.PARAMETER_DEFINITION_TYPE).toString() == "float") {
                            // Each float occupies 2 registers
                            settingsMessage1.getSlaveConfig(modifiedIndex - 1).setRegisterValue(curRegIndex, addr+1)
                            settingsMessage1.getSlaveConfig(modifiedIndex - 1).pointId[curRegIndex] = writablePoints[index][Tags.ID].toString()
                            updateRegisterType(writablePoint, settingsMessage1, modifiedIndex - 1, curRegIndex)
                            curRegIndex++
                        }
                    }
                    if (writablePointsSize > 0) settingsMessage1.getSlaveConfig(modifiedIndex - 1).sortSlaveConfigsByRegisters()
                }
            }
            return settingsMessage1[address]
        }

        @JvmStatic
        fun getWritePcnRegularUpdate(
            zone: Zone?,
            address: Short,
        ): ByteArray {
            CcuLog.i(L.TAG_CCU_SERIAL, "Send MODBUS_SERVER_WRITE_REGISTER_1 for slave $address ")

            // Compute buffer size: message type + node address + all registers (2 bytes each)
            val totalSize = 1 + 2 + 2 + settingsMessage2[address]?.slaveConfigs?.sumOf { it.numRegs * 2 }!!
            val buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN)

            // Write message type
            buf.put((MessageType.CCU_TO_CM_OVER_USB_MODBUS_SERVER_WRITE_REGISTER.ordinal and 0xFF).toByte())

            // Write node address
            buf.putShort(address)

            // Helper function to process writable points and put values in buffer
            fun putWritablePoints(query: String, buffer: ByteBuffer) {
                val writablePoints = hayStack.readAllEntities(query)
                writablePoints.forEach { writablePoint ->
                    val paramType = writablePoint[Tags.PARAMETER_DEFINITION_TYPE].toString()
                    val pointId = writablePoint[Tags.ID].toString()
                    if (paramType == "float") {
                        val floatVal = hayStack.readDefaultValById(pointId).toFloat()
                        val intBits = java.lang.Float.floatToIntBits(floatVal)
                        buffer.putShort(((intBits shr 16) and 0xFFFF).toShort()) // Upper 2 bytes
                        buffer.putShort((intBits and 0xFFFF).toShort())           // Lower 2 bytes
                    } else {
                        val intVal = hayStack.readDefaultValById(pointId).toInt()
                        buffer.putShort((intVal and 0xFFFF).toShort())           // 2 bytes
                    }
                }
                // Add CRC byte as well
                buffer.put((settingsMessage2[address]?.crcLo?.toByte()!!).toByte())
                buffer.put((settingsMessage2[address]?.crcHi?.toByte()!!).toByte())
            }

            // Iterate configs and delegate per device type
            for (cfg in settingsMessage2[address]?.slaveConfigs!!) {
                when (cfg.deviceType) {
                    PCNDeviceTypes.PCN_NATIVE_DEVICE -> {
                        putWritablePoints("point and writable and pcn and registerNumber and roomRef == \"${zone?.id}\"", buf)
                    }
                    PCNDeviceTypes.PCN_CONNECT_MODULE -> {
                        putWritablePoints("point and writable and connectModule and registerNumber and roomRef == \"${zone?.id}\"" +
                                "and group==\"${cfg.slaveId}\"", buf)
                    }
                    PCNDeviceTypes.PCN_EXTERNAL_EQUIP -> {
                        putWritablePoints("point and writable and modbus and registerNumber and roomRef == \"${zone?.id}\"" +
                                "and group==\"${cfg.slaveId}\"", buf)
                    }
                    PCNDeviceTypes.PCN_DEVICE_NONE -> { /* No action needed */ }
                }
            }
            return buf.array()
        }

        /**
         * Builds a `CcuToCmOverUsbPcnSettingsMessage_t` that defines **writable** register
         * configurations for a PCN node, its connected modules, and external equipment.
         * This is used for writing values into device registers (unlike
         * `getPcnSettings1Message` which covers non-writable points for monitoring).
         *
         * Steps performed:
         *
         * 1. **Initialize Settings Message**
         *    - Clear prior configs.
         *    - Set message type to `CCU_TO_CM_MODBUS_SERVER_WRITE_REGISTER_SETTINGS`.
         *    - Assign node address.
         *    - Define update interval (40 seconds).
         *
         * 2. **PCN Native Device Writable Registers**
         *    - Resolve the PCN device ID using Haystack query.
         *    - Fetch all **writable points** belonging to this PCN in the zone.
         *    - If writable points exist:
         *      - Add a slave config with fixed ID `100`.
         *      - Mark the device type as `PCN_NATIVE_DEVICE`.
         *      - Populate register values with the writable point register numbers.
         *
         * 3. **Connected Module Devices Writable Registers**
         *    - Read all connect modules linked to this PCN device.
         *    - For each one:
         *      - Fetch all **writable** points using the module’s slave ID.
         *      - Add a slave config for the connect module if points are present.
         *      - Assign the device type `PCN_CONNECT_MODULE`.
         *      - Populate register numbers for each writable point.
         *
         * 4. **External Equipment Writable Registers**
         *    - Query all Modbus-based external equipment linked to the PCN.
         *    - Retrieve all **writable points** in the given zone.
         *    - Add a new slave config for external equipment if points are found.
         *    - Assign device type `PCN_EXTERNAL_EQUIP`.
         *    - Populate the register addresses for each external equipment writable point.
         *
         * 5. **Return Fully Configured Settings Message**
         *
         * @param zone    The zone object representing the logical area.
         * @param address The node address for the PCN being targeted.
         * @return A `CcuToCmOverUsbPcnSettingsMessage_t` configured with writable
         *         register information across PCN device, its connect modules,
         *         and external equipment.
         */
        @JvmStatic
        fun getPcnSettings2Message(
            zone: Zone?,
            address: Short,
        ): CcuToCmOverUsbPcnSettingsMessage_t? {

            settingsMessage2.put(address, CcuToCmOverUsbPcnSettingsMessage_t())
            settingsMessage2.get(address)?.let { settingsMessage2 ->
                settingsMessage2.clearConfigs()
                settingsMessage2.messageType = MessageType.CCU_TO_CM_MODBUS_SERVER_WRITE_REGISTER_SETTINGS.ordinal
                settingsMessage2.nodeAddress = address.toInt()
                settingsMessage2.intervalInSecs = 40 // 40 seconds
                // Step 1: Get the PCN device id for the given address
                var pcnDeviceId = hayStack.readEntity("domainName == \"${ModelNames.pcnDevice}\" and addr == \"$address\"").get("id").toString()
                val writablePoints = hayStack.readAllEntities("point and writable and pcn and registerNumber and roomRef == \"${zone?.id}\"")
                var curRegIndex = 0
                var writablePointsSize = 0
                writablePoints.forEachIndexed{ index, writablePoint->
                    writablePointsSize += if(writablePoint.get(Tags.PARAMETER_DEFINITION_TYPE).toString() == "float") 2 else 1
                }
                // Step 1.a: Add a slave config for PCN device if writable points are present
                if(writablePoints.size > 0) {
                    settingsMessage2.addSlaveConfig(100, writablePointsSize)
                    settingsMessage2.getSlaveConfig(0).deviceType = PCNDeviceTypes.PCN_NATIVE_DEVICE // PCN device
                }

                // Step 1.b: Populate the register values for PCN device
                writablePoints.forEachIndexed{ index, writablePoint->
                    val addr = writablePoint[Tags.REG_NUM].toString().toInt()
                    settingsMessage2.getSlaveConfig(0).setRegisterValue(curRegIndex, addr)
                    // Update the register type first, then update the register value
                    updateRegisterType(writablePoint, settingsMessage2, 0, curRegIndex)
                    curRegIndex++
                    if(writablePoint.get(Tags.PARAMETER_DEFINITION_TYPE).toString() == "float") {
                        // Each float occupies 2 registers
                        settingsMessage2.getSlaveConfig(0).setRegisterValue(curRegIndex, addr+1)
                        updateRegisterType(writablePoint, settingsMessage2, 0, curRegIndex)
                        curRegIndex++
                    }
                }
                if (writablePointsSize > 0) settingsMessage2.getSlaveConfig(0).sortSlaveConfigsByRegisters()

                // Step 2: Get all connect module devices for the given PCN device
                val connectModuleDevices = hayStack.readAllEntities("device and domainName == \"${ModelNames.connectNodeDevice}\" and deviceRef == \"$pcnDeviceId\"")
                connectModuleDevices.forEachIndexed {index, device ->
                    val slaveId = device[Tags.ADDR].toString().toInt()
                    val writablePoints = hayStack.readAllEntities("point and writable and connectModule and registerNumber and roomRef == \"${zone?.id}\" and group == \"$slaveId\"")
                    curRegIndex = 0
                    writablePointsSize = 0
                    writablePoints.forEachIndexed{ index, writablePoint->
                        writablePointsSize += if(writablePoint.get(Tags.PARAMETER_DEFINITION_TYPE).toString() == "float") 2 else 1
                    }
                    // Just fetch the slave id from first writable point
                    val modifiedIndex = settingsMessage2.getAllConfigs().size + 1 // Since 0 is used by PCN device
                    if(writablePoints.size > 0) {
                        val slaveId = writablePoints[0][Tags.GROUP].toString().toInt()
                        settingsMessage2.addSlaveConfig(slaveId, writablePointsSize)
                        settingsMessage2.getSlaveConfig(modifiedIndex - 1).deviceType = PCNDeviceTypes.PCN_CONNECT_MODULE // PCN device
                    }
                    // Step 2.a : Get the connect module address
                    writablePoints.forEachIndexed{ innerIndex, writablePoint->
                        val addr = writablePoint[Tags.REG_NUM].toString().toInt()
                        settingsMessage2.getSlaveConfig(modifiedIndex - 1).setRegisterValue(curRegIndex, addr)
                        // Update the register type first, then update the register value
                        updateRegisterType(writablePoint, settingsMessage2, modifiedIndex - 1, curRegIndex)
                        curRegIndex++
                        if(writablePoint.get(Tags.PARAMETER_DEFINITION_TYPE).toString() == "float") {
                            // Each float occupies 2 registers
                            settingsMessage2.getSlaveConfig(modifiedIndex - 1).setRegisterValue(curRegIndex, addr+1)
                            updateRegisterType(writablePoint, settingsMessage2, modifiedIndex - 1, curRegIndex)
                            curRegIndex++
                        }
                    }
                    if (writablePointsSize > 0) settingsMessage2.getSlaveConfig(modifiedIndex - 1).sortSlaveConfigsByRegisters()
                }

                // Step 3: Get all external  devices for the given PCN device
                val externalEquipDevices = hayStack.readAllEntities("modbus and connectModule and deviceRef == \"$pcnDeviceId\"")
                externalEquipDevices.forEachIndexed {index, device ->
                    val writablePoints = hayStack.readAllEntities("point and writable and modbus and registerNumber and roomRef == \"${zone?.id}\"")
                    curRegIndex = 0
                    writablePointsSize = 0
                    writablePoints.forEachIndexed{ index, writablePoint->
                        writablePointsSize += if(writablePoint.get(Tags.PARAMETER_DEFINITION_TYPE).toString() == "float") 2 else 1
                    }
                    // Just fetch the slave id from first writable point
                    val modifiedIndex = settingsMessage2.getAllConfigs().size + 1 // Since 0 is used by PCN device
                    if(writablePoints.size > 0) {
                        val slaveId = writablePoints[0][Tags.GROUP].toString().toInt()
                        settingsMessage2.addSlaveConfig(slaveId, writablePoints.size)
                        settingsMessage2.getSlaveConfig(modifiedIndex - 1).deviceType = PCNDeviceTypes.PCN_EXTERNAL_EQUIP // PCN device
                    }
                    // Step 3.a : Get the connect module address
                    writablePoints.forEachIndexed{ innerIndex, writablePoint->
                        val addr = writablePoint[Tags.REG_NUM].toString().toInt()
                        settingsMessage2.getSlaveConfig(modifiedIndex - 1).setRegisterValue(curRegIndex, addr)
                        // Update the register type first, then update the register value
                        updateRegisterType(writablePoint, settingsMessage2, modifiedIndex - 1, curRegIndex)
                        curRegIndex++
                        if(writablePoint.get(Tags.PARAMETER_DEFINITION_TYPE).toString() == "float") {
                            // Each float occupies 2 registers
                            settingsMessage2.getSlaveConfig(modifiedIndex - 1).setRegisterValue(curRegIndex, addr+1)
                            updateRegisterType(writablePoint, settingsMessage2, modifiedIndex - 1, curRegIndex)
                            curRegIndex++
                        }
                    }
                    if (writablePointsSize > 0) settingsMessage2.getSlaveConfig(modifiedIndex - 1).sortSlaveConfigsByRegisters()
                }
            }

            return settingsMessage2[address]
        }

        private fun updateRegisterType(
            writablePoint: HashMap<Any, Any>,
            settingsMessage: CcuToCmOverUsbPcnSettingsMessage_t,
            slaveConfigIndex: Int,
            registerIndex: Int
        ) {
            val type = writablePoint[Tags.REGISTER_TYPE]?.toString()

            // Map registerType string to an integer code
            val registerTypeCode = when (type) {
                Tags.INPUT_REGISTER  -> 0  // 0 for inputRegister
                Tags.COIL            -> 2  // 2 for coil register
                Tags.DISCRETE_INPUT  -> 3  // 3 for discreteInput register
                else                 -> 1  // 1 for holding/default
            }

            settingsMessage
                .getSlaveConfig(slaveConfigIndex)
                .setRegisterType(registerIndex, registerTypeCode)
        }

        @JvmStatic
        fun handlePcnRegularUpdateSettings(data: ByteArray) {
            val fullData = data.copyOfRange(0, data.size)
            val tempData: ByteArray = data.copyOfRange(3, data.size) // Skip message type and node address
            val deviceCrcBytes = data.copyOfRange( data.size -4, data.size -2) // Last 2 bytes are CRC
            var i = 0

            CcuLog.e(L.TAG_CCU_SERIAL_CONNECT, "PCN Regular update data: $fullData")

            // Get the PCN device address from data and update heartbeat points of PCN device and connect node devices connected to it
            val myData = data.copyOfRange(1, 3);
            val address = ByteBuffer.wrap(myData).order(ByteOrder.LITTLE_ENDIAN).short
            val registersTriple = settingsMessage1[address]?.getAllRegInfo()
            // If the deviceCRC for regular update settings1 mismatch, then log and return(to avoid incorrect portal updates)
            val ccuSettingsCrc = settingsMessage1[address]?.getCrc()
            val deviceCrc = ByteBuffer.wrap(deviceCrcBytes).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF
            if(ccuSettingsCrc != deviceCrc) {
                CcuLog.e(L.TAG_CCU_SERIAL_CONNECT, "PCN Regular update CRC mismatch: CCU Device CRC $ccuSettingsCrc , Device CRC $deviceCrc")
                return
            }
            val pcnNodeDevice = getPcnNodeDevice(address.toInt())
            pcnNodeDevice.heartBeat.writeHisValueByIdWithoutCOV(1.0)
            val connectNodeDevices = CCUHsApi.getInstance().readAllEntities("device and connectModule and deviceRef==\"${pcnNodeDevice.getId()}\"")
            connectNodeDevices.forEach() { connectNodeDevice ->
                val heartBeatPoint = CCUHsApi.getInstance().readEntity("heartbeat and deviceRef==\"${connectNodeDevice.get("id")}\"")
                CCUHsApi.getInstance().writeHisValueByIdWithoutCOV(heartBeatPoint.get("id").toString(), 1.0)
            }

            while (i < tempData.size) {
                val regIndex = i / 2
                val regInfo = registersTriple?.get(regIndex)
                val pointId = regInfo?.second
                val register = regInfo?.third
                val deviceType = regInfo?.first

                CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "Data[$i]: ${tempData[i]}")

                // Read point metadata
                val ptEntity = hayStack.readEntity("id==$pointId")
                val paramType = ptEntity[Tags.PARAMETER_DEFINITION_TYPE].toString()

                if (paramType == "float") {
                    // Float -> 4 bytes
                    val floatVal = parseFloatPcn(tempData.copyOfRange(i, i + 4))
                    CcuLog.i(L.TAG_CCU_SERIAL_CONNECT,
                        "  Register $register: $floatVal for PointId $pointId"
                    )
                    CCUHsApi.getInstance().writeHisValById(pointId, floatVal.toDouble())
                    i += 4
                } else {
                    // Int -> 2 bytes
                    val intVal = parseIntFromTwoBytes(tempData.copyOfRange(i, i + 2))
                    CcuLog.i(L.TAG_CCU_SERIAL_CONNECT,
                        "  Register $register: ${tempData[i].toInt() + (tempData[i + 1].toInt() shl 8)} for PointId $pointId"
                    )
                    CCUHsApi.getInstance().writeHisValById(pointId, intVal.toDouble())
                    i += 2
                }
            }
            CcuLog.i(L.TAG_CCU_SERIAL_CONNECT, "PCN regular update")
        }

        /**
         * Processes a decoded PCN diagnostic message and updates device state
         * or diagnostic information for the given node address.
         *
         * This function is responsible for:
         * 1. Logging the diagnostic update if debug logging is enabled.
         * 2. Updating the last-seen timestamp of the device in `Pulse.mDeviceUpdate`.
         * 3. Iterating over the list of diagnostic info entries contained within
         *    the `PCN_ConnectDiagnosticMessage_t`.
         * 4. For each diagnostic info entry, resolving the associated PCN device ID
         *    via `getPcnByNodeAddress(...)`.
         * 5. Depending on the `slaveID`:
         *    - If `slaveID == 100`: Direct diagnostic update for the PCN device is handled
         *      by `handleDiagnosticUpdatePcn`.
         *    - Otherwise: Diagnostic update is handled for a connected node
         *      via `handleDiagnosticUpdateConnectNode`.
         *
         * @param regularUpdateMessage The decoded PCN diagnostic message containing
         *        one or more diagnostic info entries for devices/nodes.
         * @param nodeAddress The integer address of the node from which the update originated.
         */
        private fun handlePcnDiagnosticDecoded(
            regularUpdateMessage: pcn.PCN_ConnectDiagnosticMessage_t,
            nodeAddress: Int
        ) {
            if (DLog.isLoggingEnabled()) {
                CcuLog.i(L.TAG_CCU_DEVICE, "handleRegularUpdate: $regularUpdateMessage")
            }

            Pulse.mDeviceUpdate[nodeAddress.toShort()] = Calendar.getInstance().timeInMillis

            // Step 1: Get the pcn device id for the given address
            regularUpdateMessage.connectDiagnosticInfoList.forEachIndexed { index, connectDiagnosticInfo ->
                // Step 1: Get the PCN device details and load the register values
                val pcnDeviceId = getPcnByNodeAddress(nodeAddress.toString(), CCUHsApi.getInstance())
                if(connectDiagnosticInfo.slaveID == 100) {
                    handleDiagnosticUpdatePcn(
                        regularUpdateMessage.connectDiagnosticInfoList[index],
                        pcnDeviceId as HashMap<Any, Any>,
                        nodeAddress
                    )
                } else {
                    handleDiagnosticUpdateConnectNode(
                        regularUpdateMessage.connectDiagnosticInfoList[index],
                        pcnDeviceId as HashMap<Any, Any>,
                        connectDiagnosticInfo.slaveID
                    )
                }
            }
        }

        fun handleDiagnosticUpdatePcn(regularUpdateMessage: pcn.ConnectDiagnosticInformation_t, device: HashMap<Any, Any>, nodeAddress: Int) {
            CcuLog.d(L.TAG_CCU_DEVICE, "HyperStat RegularUpdate: nodeAddress $nodeAddress :  $regularUpdateMessage")

            // Step 1: Get the connect node device reference based on slave address
            val pcnDevice = getPcnNodeDevice(nodeAddress)

            pcnDevice.heartBeat.writeHisValueByIdWithoutCOV(1.0)
            pcnDevice.sequenceLastRunTime.writeHisVal(regularUpdateMessage.seqLastRunTime.toDouble())
            pcnDevice.sequenceLongRunTime.writeHisVal(regularUpdateMessage.seqMaxRunTime.toDouble())
            pcnDevice.sequenceRunCount.writeHisVal(regularUpdateMessage.seqRunCount.toDouble())
            pcnDevice.sequenceStatus.writeHisVal(regularUpdateMessage.seqStatus.toDouble())
            pcnDevice.sequenceErrorCode.writeHisVal(regularUpdateMessage.seqErrorCode.toDouble())

            CcuLog.d(L.TAG_PCN, "HyperStat Pcn Update completed")

        }

        fun handleDiagnosticUpdateConnectNode(
            regularUpdateMessage: pcn.ConnectDiagnosticInformation_t,
            device: HashMap<Any, Any>,
            slaveId: Int
        ) {
            CcuLog.d(L.TAG_CCU_DEVICE, "PCN Diagnostic message update: nodeAddress $slaveId :  $regularUpdateMessage")

            // Step 1: Get the connect node device reference based on slave address
            val connectNodeEquip = getConnectNodeEquip(device, slaveId)

            connectNodeEquip.heartBeat.writeHisValueByIdWithoutCOV(1.0)
            connectNodeEquip.sequenceLastRunTime.writeHisVal(regularUpdateMessage.seqLastRunTime.toDouble())
            connectNodeEquip.sequenceLongRunTime.writeHisVal(regularUpdateMessage.seqMaxRunTime.toDouble())
            connectNodeEquip.sequenceRunCount.writeHisVal(regularUpdateMessage.seqRunCount.toDouble())
            connectNodeEquip.sequenceStatus.writeHisVal(regularUpdateMessage.seqStatus.toDouble())
            connectNodeEquip.sequenceErrorCode.writeHisVal(regularUpdateMessage.seqErrorCode.toDouble())

            CcuLog.d(L.TAG_PCN, "PCN Diagnostic update Connect Node Update completed")
        }

        /**
         * Handles a PCN diagnostic message received from the device.
         *
         * This function extracts the device address and the diagnostic payload
         * from the provided byte array, decodes the diagnostic message into a
         * `PCN_ConnectDiagnosticMessage_t` object, and forwards the result to
         * the decoding handler for further processing.
         *
         * Steps performed:
         * 1. Capture the current system time (for possible logging or timestamping usage).
         * 2. Extract the device address bytes from `data` using the predefined
         *    indices `HYPERSTAT_MESSAGE_ADDR_START_INDEX` and
         *    `HYPERSTAT_MESSAGE_ADDR_END_INDEX`.
         * 3. Convert the extracted bytes into an integer address,
         *    interpreting the bytes in little-endian order.
         * 4. Extract the serialized diagnostic message bytes from
         *    `HYPERSTAT_SERIALIZED_MESSAGE_START_INDEX` until the end of the array.
         * 5. Parse the serialized bytes into a `PCN_ConnectDiagnosticMessage_t` object.
         * 6. Pass the decoded diagnostic message along with the device address to
         *    `handlePcnDiagonasticDecoded`.
         *
         * @param data The raw byte array containing the diagnostic message and address.
         */
        @JvmStatic
        fun handlePcnDiagonasticMessage(data: ByteArray) {
            val time = System.currentTimeMillis()
            val addrArray = Arrays.copyOfRange(
                data, HYPERSTAT_MESSAGE_ADDR_START_INDEX,
                HYPERSTAT_MESSAGE_ADDR_END_INDEX
            )
            val address = ByteBuffer.wrap(addrArray)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt()

            val messageArray = Arrays.copyOfRange(
                data,
                HYPERSTAT_SERIALIZED_MESSAGE_START_INDEX,
                data.size
            )
            val regularUpdate =
                pcn.PCN_ConnectDiagnosticMessage_t.parseFrom(messageArray)
            handlePcnDiagnosticDecoded(regularUpdate, address)
        }

        /**
         * Processes an incoming message from the device by checking its message type
         * and delegating to the appropriate handler.
         *
         * The function extracts the message type from the given byte array using
         * the predefined index constant `HYPERSTAT_MESSAGE_TYPE_INDEX`.
         *
         * If the message type is not recognized, a warning log is created with the
         * unknown message type value.
         *
         * @param data The raw byte array containing the incoming message.
         */
        @JvmStatic
        fun processMessage(data: ByteArray) {
            val msgType = data[HYPERSTAT_MESSAGE_TYPE_INDEX].toInt()
            when (msgType) {
                MessageType.DEVICE_TO_CM_PCN_MODBUS_SERVER_DIAGNOSTIC_MESSAGE.ordinal -> {
                    handlePcnDiagonasticMessage(data)
                }
                else -> {
                    CcuLog.w(L.TAG_CCU_SERIAL, "Unknown message type: $msgType")
                }
            }
        }
    }
}
