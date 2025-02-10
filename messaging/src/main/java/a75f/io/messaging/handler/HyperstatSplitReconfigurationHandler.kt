package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.Tags
import a75f.io.domain.HyperStatSplitEquip
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.PointBuilderConfig
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.hyperstatsplit.common.FanModeCacheStorage
import a75f.io.logic.bo.building.hyperstatsplit.profiles.HyperStatSplitProfileConfiguration
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.CpuEconSensorBusTempAssociation
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconProfile
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuProfileConfiguration
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import android.util.Log
import com.google.gson.JsonObject
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

/**
 * Created by Manjunath K on 14-10-2021.
 */
class HyperstatSplitReconfigurationHandler {

    companion object {

        fun handleHyperStatSplitConfigChange(msgObject: JsonObject, configPoint: Point, hayStack: CCUHsApi) {
            try {

                CcuLog.i(
                    L.TAG_CCU_PUBNUB, "\n **** Reconfiguration ****${L.TAG_CCU_HSSPLIT_CPUECON} " + configPoint + " " + msgObject.toString()
                            + " Markers =" + configPoint.markers +"\n "
                )
                val pointVal = msgObject[HayStackConstants.WRITABLE_ARRAY_VAL].asString
                if (pointVal.isEmpty()) {
                    val level = msgObject[HayStackConstants.WRITABLE_ARRAY_LEVEL].asInt
                    //When a level is deleted, it currently generates a message with empty value.
                    //Handle it here.
                    hayStack.clearPointArrayLevel(configPoint.id, level, true)
                    hayStack.writeHisValById(configPoint.id, HSUtil.getPriorityVal(configPoint.id))
                    if (configPoint.domainName.equals(DomainName.conditioningMode) ||
                            configPoint.domainName.equals(DomainName.fanOpMode)) {
                        DesiredTempDisplayMode.setModeTypeOnUserIntentChange(configPoint.roomRef, CCUHsApi.getInstance())

                        /*
                        - If we do reconfiguration from portal for fanMode, level 10 updated as ( val = 9)
                        - Now if user change the fanMode from CCU, it will update the level 8 as 1
                        - Now if we do reconfiguration from portal for fanMode, message will receive for only removing level 8 not for level 10
                        - Because level 10 does not have change of value, so silo will never send update entity for level 10
                        - Due to this fanMode will not update in CCU's shared preference
                         */
                        if (configPoint.domainName.equals(DomainName.fanOpMode)) {
                            val configVal = HSUtil.getPriorityVal(configPoint.id).toInt() // After clearing the level, update the preference with priority value
                            val cache = FanModeCacheStorage()
                            if (configVal != 0 && (configVal % 3 == 0 || isFanModeCurrentOccupied(configVal) ) ) //Save only Fan occupied period or current Occupied mode alone, else no need.
                                cache.saveFanModeInCache(configPoint.equipRef, configVal)
                            else cache.removeFanModeFromCache(configPoint.equipRef)
                        }

                        return
                    }
                    return
                }

                if (configPoint.domainName.equals(DomainName.fanOpMode)) {
                    val configVal = msgObject["val"].asInt
                    val cache = FanModeCacheStorage()
                    if (configVal != 0 && (configVal % 3 == 0 || isFanModeCurrentOccupied(configVal) )) //Save only Fan occupied period or current Occupied mode alone, else no need.
                        cache.saveFanModeInCache(
                            configPoint.equipRef,
                            configVal
                        ) else cache.removeFanModeFromCache(configPoint.equipRef)
                }
            } catch (e: NullPointerException){
            e.printStackTrace()
                Log.i(L.TAG_CCU_HSSPLIT_CPUECON, "updateConfigPoint: ${e.localizedMessage}")
            }
            pointUpdateOwner(configPoint, msgObject, hayStack)
            if (configPoint.markers.contains(Tags.USERINTENT) && configPoint.markers.contains(Tags.CONDITIONING)) {
                DesiredTempDisplayMode.setModeTypeOnUserIntentChange(configPoint.roomRef, CCUHsApi.getInstance())
            } else {
                DesiredTempDisplayMode.setModeType(configPoint.roomRef, CCUHsApi.getInstance())
            }

            if (isDynamicConfigPoint(configPoint)) handleDynamicConfig(configPoint, hayStack)

            (L.getProfile(configPoint.group.toShort()) as HyperStatSplitCpuEconProfile).refreshEquip()
            hayStack.scheduleSync()

        }

        private fun isFanModeCurrentOccupied(value : Int): Boolean {
            val basicSettings = StandaloneFanStage.values()[value]
            return (basicSettings == StandaloneFanStage.LOW_CUR_OCC || basicSettings == StandaloneFanStage.MEDIUM_CUR_OCC || basicSettings == StandaloneFanStage.HIGH_CUR_OCC)
        }

        private fun isDynamicConfigPoint(configPoint: Point): Boolean {
            return configPoint.domainName.equals(DomainName.enableOutsideAirOptimization) ||
                    configPoint.domainName.equals(DomainName.prePurgeEnable) ||

                    configPoint.domainName.equals(DomainName.sensorBusAddress0Enable) ||
                    configPoint.domainName.equals(DomainName.sensorBusAddress1Enable) ||
                    configPoint.domainName.equals(DomainName.sensorBusAddress2Enable) ||
                    configPoint.domainName.equals(DomainName.sensorBusPressureEnable) ||
                    configPoint.domainName.equals(DomainName.temperatureSensorBusAdd0) ||
                    configPoint.domainName.equals(DomainName.temperatureSensorBusAdd1) ||
                    configPoint.domainName.equals(DomainName.temperatureSensorBusAdd2) ||
                    configPoint.domainName.equals(DomainName.pressureSensorBusAdd0) ||

                    configPoint.domainName.equals(DomainName.relay1OutputEnable) ||
                    configPoint.domainName.equals(DomainName.relay2OutputEnable) ||
                    configPoint.domainName.equals(DomainName.relay3OutputEnable) ||
                    configPoint.domainName.equals(DomainName.relay4OutputEnable) ||
                    configPoint.domainName.equals(DomainName.relay5OutputEnable) ||
                    configPoint.domainName.equals(DomainName.relay6OutputEnable) ||
                    configPoint.domainName.equals(DomainName.relay7OutputEnable) ||
                    configPoint.domainName.equals(DomainName.relay8OutputEnable) ||
                    configPoint.domainName.equals(DomainName.relay1OutputAssociation) ||
                    configPoint.domainName.equals(DomainName.relay2OutputAssociation) ||
                    configPoint.domainName.equals(DomainName.relay3OutputAssociation) ||
                    configPoint.domainName.equals(DomainName.relay4OutputAssociation) ||
                    configPoint.domainName.equals(DomainName.relay5OutputAssociation) ||
                    configPoint.domainName.equals(DomainName.relay6OutputAssociation) ||
                    configPoint.domainName.equals(DomainName.relay7OutputAssociation) ||
                    configPoint.domainName.equals(DomainName.relay8OutputAssociation) ||

                    configPoint.domainName.equals(DomainName.analog1OutputEnable) ||
                    configPoint.domainName.equals(DomainName.analog2OutputEnable) ||
                    configPoint.domainName.equals(DomainName.analog3OutputEnable) ||
                    configPoint.domainName.equals(DomainName.analog4OutputEnable) ||
                    configPoint.domainName.equals(DomainName.analog1OutputAssociation) ||
                    configPoint.domainName.equals(DomainName.analog2OutputAssociation) ||
                    configPoint.domainName.equals(DomainName.analog3OutputAssociation) ||
                    configPoint.domainName.equals(DomainName.analog4OutputAssociation) ||

                    configPoint.domainName.equals(DomainName.universalIn1Enable) ||
                    configPoint.domainName.equals(DomainName.universalIn2Enable) ||
                    configPoint.domainName.equals(DomainName.universalIn3Enable) ||
                    configPoint.domainName.equals(DomainName.universalIn4Enable) ||
                    configPoint.domainName.equals(DomainName.universalIn5Enable) ||
                    configPoint.domainName.equals(DomainName.universalIn6Enable) ||
                    configPoint.domainName.equals(DomainName.universalIn7Enable) ||
                    configPoint.domainName.equals(DomainName.universalIn8Enable) ||
                    configPoint.domainName.equals(DomainName.universalIn1Association) ||
                    configPoint.domainName.equals(DomainName.universalIn2Association) ||
                    configPoint.domainName.equals(DomainName.universalIn3Association) ||
                    configPoint.domainName.equals(DomainName.universalIn4Association) ||
                    configPoint.domainName.equals(DomainName.universalIn5Association) ||
                    configPoint.domainName.equals(DomainName.universalIn6Association) ||
                    configPoint.domainName.equals(DomainName.universalIn7Association) ||
                    configPoint.domainName.equals(DomainName.universalIn8Association)
        }

        private fun handleDynamicConfig (configPoint: Point, hayStack: CCUHsApi) {
            CcuLog.i(L.TAG_CCU_PUBNUB, "Point " + configPoint.domainName + " would trigger a reconfig");

            val equipModel = ModelLoader.getHyperStatSplitCpuModel() as SeventyFiveFProfileDirective
            val deviceModel = ModelLoader.getHyperStatSplitDeviceModel() as SeventyFiveFDeviceDirective
            val profileConfiguration = HyperStatSplitCpuProfileConfiguration(
                configPoint.group.toInt(),
                NodeType.HYPERSTATSPLIT.name,
                0,
                configPoint.roomRef,
                configPoint.floorRef,
                ProfileType.HYPERSTATSPLIT_CPU,
                equipModel
            ).getActiveConfiguration()

            val equipBuilder = ProfileEquipBuilder(hayStack)
            val equipDis = hayStack.siteName + "-cpuecon-" + profileConfiguration.nodeAddress

            val equipId = equipBuilder.updateEquipAndPoints(profileConfiguration, equipModel, hayStack.site!!.id, equipDis, true)

            val entityMapper = EntityMapper(equipModel)
            val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
            val deviceDis = hayStack.siteName +  "-HSS-" + profileConfiguration.nodeAddress
            CcuLog.i(Domain.LOG_TAG, " updateDeviceAndPoints")
            deviceBuilder.updateDeviceAndPoints(
                profileConfiguration,
                deviceModel,
                equipId,
                hayStack.site!!.id,
                deviceDis
            )

            addLinearFanLowMedHighPoints(equipId, hayStack.site!!.id, equipDis, hayStack, profileConfiguration as HyperStatSplitCpuProfileConfiguration, equipModel)
            correctSensorBusTempPoints(profileConfiguration, hayStack)
            mapSensorBusPressureLogicalPoint(profileConfiguration, equipId, hayStack, equipModel)
            setOutputTypes(profileConfiguration, hayStack)

            handleNonDefaultConditioningMode(profileConfiguration as HyperStatSplitCpuProfileConfiguration, hayStack)
            handleNonDefaultFanMode(profileConfiguration as HyperStatSplitCpuProfileConfiguration, hayStack)

            initializePrePurgeStatus(profileConfiguration, hayStack, 1.0)
            initializePrePurgeStatus(profileConfiguration, hayStack, 0.0)

            hayStack.syncEntityTree()

        }

        private fun pointUpdateOwner(configPoint: Point, msgObject: JsonObject, hayStack: CCUHsApi) {
            try {
                val who = msgObject[HayStackConstants.WRITABLE_ARRAY_WHO].asString
                val level = msgObject[HayStackConstants.WRITABLE_ARRAY_LEVEL].asInt
                val value = msgObject[HayStackConstants.WRITABLE_ARRAY_VAL].asDouble
                val duration =
                    if (msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION] != null)
                        msgObject[HayStackConstants.WRITABLE_ARRAY_DURATION].asInt else 0
                hayStack.writePointLocal(configPoint.id, level, who, value, duration)
            } catch (e: Exception) {
                e.printStackTrace()
                CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : " + msgObject + " ; " + e.message)
            }
        }

        /*
            analog1FanLow, etc. points require a multi-dependency. They need to be created when the
            corresponding analog output is mapped to either Linear Fan (1) or Staged Fan (4).

            Framework doesn't support this yet. So these points are configured in framework to create only
            when AO is mapped to Staged Fan. If an AO mapped to Linear Fan, we create the point manually here.
         */
        fun addLinearFanLowMedHighPoints(
            equipId : String,
            siteRef : String,
            equipDis : String,
            hayStack: CCUHsApi,
            profileConfiguration: HyperStatSplitCpuProfileConfiguration,
            equipModel: SeventyFiveFProfileDirective
        ) {
            if (profileConfiguration.analogOut1Enabled.enabled &&
                profileConfiguration.analogOut1Association.associationVal.equals(CpuControlType.LINEAR_FAN.ordinal)) {

                requireNotNull(equipModel)
                val equipBuilder = ProfileEquipBuilder(hayStack)

                val fanLowDef = equipModel.points.find { it.domainName == DomainName.analog1FanLow }
                fanLowDef?.run {
                    val fanLowId = equipBuilder.createPoint(
                        PointBuilderConfig(
                            this, profileConfiguration, equipId, siteRef, hayStack.timeZone, equipDis
                        )
                    )
                    hayStack.writeDefaultValById(fanLowId, (profileConfiguration as HyperStatSplitCpuProfileConfiguration).analogOut1Voltage.linearFanAtFanLow.currentVal)
                }

                val fanMediumDef = equipModel.points.find { it.domainName == DomainName.analog1FanMedium }
                fanMediumDef?.run {
                    val fanMediumId = equipBuilder.createPoint(
                        PointBuilderConfig(
                            this, profileConfiguration, equipId, siteRef, hayStack.timeZone, equipDis
                        )
                    )
                    hayStack.writeDefaultValById(fanMediumId, (profileConfiguration as HyperStatSplitCpuProfileConfiguration).analogOut1Voltage.linearFanAtFanMedium.currentVal)
                }

                val fanHighDef = equipModel.points.find { it.domainName == DomainName.analog1FanHigh }
                fanHighDef?.run {
                    val fanHighId = equipBuilder.createPoint(
                        PointBuilderConfig(
                            this, profileConfiguration, equipId, siteRef, hayStack.timeZone, equipDis
                        )
                    )
                    hayStack.writeDefaultValById(fanHighId, (profileConfiguration as HyperStatSplitCpuProfileConfiguration).analogOut1Voltage.linearFanAtFanHigh.currentVal)
                }

            }

            if (profileConfiguration.analogOut2Enabled.enabled &&
                profileConfiguration.analogOut2Association.associationVal.equals(CpuControlType.LINEAR_FAN.ordinal)) {

                requireNotNull(equipModel)
                val equipBuilder = ProfileEquipBuilder(hayStack)

                val fanLowDef = equipModel.points.find { it.domainName == DomainName.analog2FanLow }
                fanLowDef?.run {
                    val fanLowId = equipBuilder.createPoint(
                        PointBuilderConfig(
                            this, profileConfiguration, equipId, siteRef, hayStack.timeZone, equipDis
                        )
                    )
                    hayStack.writeDefaultValById(fanLowId, (profileConfiguration as HyperStatSplitCpuProfileConfiguration).analogOut2Voltage.linearFanAtFanLow.currentVal)
                }

                val fanMediumDef = equipModel.points.find { it.domainName == DomainName.analog2FanMedium }
                fanMediumDef?.run {
                    val fanMediumId = equipBuilder.createPoint(
                        PointBuilderConfig(
                            this, profileConfiguration, equipId, siteRef, hayStack.timeZone, equipDis
                        )
                    )
                    hayStack.writeDefaultValById(fanMediumId, (profileConfiguration as HyperStatSplitCpuProfileConfiguration).analogOut2Voltage.linearFanAtFanMedium.currentVal)
                }

                val fanHighDef = equipModel.points.find { it.domainName == DomainName.analog2FanHigh }
                fanHighDef?.run {
                    val fanHighId = equipBuilder.createPoint(
                        PointBuilderConfig(
                            this, profileConfiguration, equipId, siteRef, hayStack.timeZone, equipDis
                        )
                    )
                    hayStack.writeDefaultValById(fanHighId, (profileConfiguration as HyperStatSplitCpuProfileConfiguration).analogOut2Voltage.linearFanAtFanHigh.currentVal)
                }

            }

            if (profileConfiguration.analogOut3Enabled.enabled &&
                profileConfiguration.analogOut3Association.associationVal.equals(CpuControlType.LINEAR_FAN.ordinal)) {

                requireNotNull(equipModel)
                val equipBuilder = ProfileEquipBuilder(hayStack)

                val fanLowDef = equipModel.points.find { it.domainName == DomainName.analog3FanLow }
                fanLowDef?.run {
                    val fanLowId = equipBuilder.createPoint(
                        PointBuilderConfig(
                            this, profileConfiguration, equipId, siteRef, hayStack.timeZone, equipDis
                        )
                    )
                    val defVal = (profileConfiguration as HyperStatSplitCpuProfileConfiguration).analogOut3Voltage.linearFanAtFanLow.currentVal
                    hayStack.writeDefaultValById(fanLowId, defVal)
                }

                val fanMediumDef = equipModel.points.find { it.domainName == DomainName.analog3FanMedium }
                fanMediumDef?.run {
                    val fanMediumId = equipBuilder.createPoint(
                        PointBuilderConfig(
                            this, profileConfiguration, equipId, siteRef, hayStack.timeZone, equipDis
                        )
                    )
                    val defVal = (profileConfiguration as HyperStatSplitCpuProfileConfiguration).analogOut3Voltage.linearFanAtFanMedium.currentVal
                    hayStack.writeDefaultValById(fanMediumId, defVal)
                }

                val fanHighDef = equipModel.points.find { it.domainName == DomainName.analog3FanHigh }
                fanHighDef?.run {
                    val fanHighId = equipBuilder.createPoint(
                        PointBuilderConfig(
                            this, profileConfiguration, equipId, siteRef, hayStack.timeZone, equipDis
                        )
                    )
                    val defVal = (profileConfiguration as HyperStatSplitCpuProfileConfiguration).analogOut3Voltage.linearFanAtFanHigh.currentVal
                    hayStack.writeDefaultValById(fanHighId, defVal)
                }

            }

            if (profileConfiguration.analogOut4Enabled.enabled &&
                profileConfiguration.analogOut4Association.associationVal.equals(CpuControlType.LINEAR_FAN.ordinal)) {

                requireNotNull(equipModel)
                val equipBuilder = ProfileEquipBuilder(hayStack)

                val fanLowDef = equipModel.points.find { it.domainName == DomainName.analog4FanLow }
                fanLowDef?.run {
                    val fanLowId = equipBuilder.createPoint(
                        PointBuilderConfig(
                            this, profileConfiguration, equipId, siteRef, hayStack.timeZone, equipDis
                        )
                    )
                    hayStack.writeDefaultValById(fanLowId, (profileConfiguration as HyperStatSplitCpuProfileConfiguration).analogOut4Voltage.linearFanAtFanLow.currentVal)
                }

                val fanMediumDef = equipModel.points.find { it.domainName == DomainName.analog4FanMedium }
                fanMediumDef?.run {
                    val fanMediumId = equipBuilder.createPoint(
                        PointBuilderConfig(
                            this, profileConfiguration, equipId, siteRef, hayStack.timeZone, equipDis
                        )
                    )
                    hayStack.writeDefaultValById(fanMediumId, (profileConfiguration as HyperStatSplitCpuProfileConfiguration).analogOut4Voltage.linearFanAtFanMedium.currentVal)
                }

                val fanHighDef = equipModel.points.find { it.domainName == DomainName.analog4FanHigh }
                fanHighDef?.run {
                    val fanHighId = equipBuilder.createPoint(
                        PointBuilderConfig(
                            this, profileConfiguration, equipId, siteRef, hayStack.timeZone, equipDis
                        )
                    )
                    hayStack.writeDefaultValById(fanHighId, (profileConfiguration as HyperStatSplitCpuProfileConfiguration).analogOut4Voltage.linearFanAtFanHigh.currentVal)
                }

            }

        }

        fun mapSensorBusPressureLogicalPoint(config: HyperStatSplitProfileConfiguration, equipRef: String, hayStack: CCUHsApi, equipModel: SeventyFiveFProfileDirective) {
            if (config.sensorBusPressureEnable.enabled && config.pressureAddress0SensorAssociation.associationVal > 0) {
                val hssEquip = HyperStatSplitEquip(equipRef)
                if (hssEquip.ductStaticPressureSensor1_2.pointExists()) {
                    hssEquip.ductStaticPressureSensor1_2.id.let {
                        val deviceMap = hayStack.readEntity("device and addr == \"" + config.nodeAddress + "\"")
                        val pressureSensorPhysMap = hayStack.readEntity("point and domainName == \"" + DomainName.ductStaticPressureSensor + "\" and deviceRef == \"" + deviceMap["id"].toString() + "\"")
                        val pressureSensorPhysPoint = RawPoint.Builder().setHashMap(pressureSensorPhysMap).setEnabled(true).setPointRef(it).build()
                        hayStack.updatePoint(pressureSensorPhysPoint, pressureSensorPhysMap["id"].toString())
                    }
                }
            }
        }

        fun correctSensorBusTempPoints(config: HyperStatSplitProfileConfiguration, hayStack: CCUHsApi) {
            val device = hayStack.read("device and addr == \"" + config.nodeAddress + "\"")

            if (!isAnySensorBusMapped(config, CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY)) {
                val deviceOATMap = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.outsideAirTempSensor + "\"")
                val deviceOATPoint = RawPoint.Builder().setHDict(deviceOATMap).setEnabled(false).setPointRef(null)
                hayStack.updatePoint(deviceOATPoint.build(), deviceOATMap[Tags.ID].toString())

                val deviceOAHMap = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.outsideAirHumiditySensor + "\"")
                val deviceOAHPoint = RawPoint.Builder().setHDict(deviceOAHMap).setEnabled(false).setPointRef(null)
                hayStack.updatePoint(deviceOAHPoint.build(), deviceOAHMap[Tags.ID].toString())
            }

            if (!isAnySensorBusMapped(config, CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY)) {
                val deviceMATMap = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.mixedAirTempSensor + "\"")
                val deviceMATPoint = RawPoint.Builder().setHDict(deviceMATMap).setEnabled(false).setPointRef(null)
                hayStack.updatePoint(deviceMATPoint.build(), deviceMATMap[Tags.ID].toString())

                val deviceMAHMap = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.mixedAirHumiditySensor + "\"")
                val deviceMAHPoint = RawPoint.Builder().setHDict(deviceMAHMap).setEnabled(false).setPointRef(null)
                hayStack.updatePoint(deviceMAHPoint.build(), deviceMAHMap[Tags.ID].toString())
            }

            if (!isAnySensorBusMapped(config, CpuEconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY)) {
                val deviceSATMap = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.supplyAirTemperature + "\"")
                val deviceSATPoint = RawPoint.Builder().setHDict(deviceSATMap).setEnabled(false).setPointRef(null)
                hayStack.updatePoint(deviceSATPoint.build(), deviceSATMap[Tags.ID].toString())

                val deviceSAHMap = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.supplyAirHumiditySensor + "\"")
                val deviceSAHPoint = RawPoint.Builder().setHDict(deviceSAHMap).setEnabled(false).setPointRef(null)
                hayStack.updatePoint(deviceSAHPoint.build(), deviceSAHMap[Tags.ID].toString())
            }

        }

        private fun isAnySensorBusMapped(config: HyperStatSplitProfileConfiguration, type: CpuEconSensorBusTempAssociation): Boolean {
            return (config.address0Enabled.enabled && config.address0SensorAssociation.temperatureAssociation.associationVal == type.ordinal) ||
                    (config.address1Enabled.enabled && config.address1SensorAssociation.temperatureAssociation.associationVal == type.ordinal) ||
                    (config.address2Enabled.enabled && config.address2SensorAssociation.temperatureAssociation.associationVal == type.ordinal)
        }

        fun setOutputTypes(config: HyperStatSplitProfileConfiguration, hayStack: CCUHsApi) {
            val device = hayStack.read("device and addr == \"" + config.nodeAddress + "\"")

            val relay1 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.relay1 + "\"")
            var relay1Point = RawPoint.Builder().setHDict(relay1).setType("Relay N/O").setEnabled(config.relay1Enabled.enabled)
            if (config.relay1Enabled.enabled && config.relay1Association.associationVal == CpuRelayType.EXTERNALLY_MAPPED.ordinal) {
                relay1Point.addMarker(Tags.WRITABLE);
                relay1Point.addMarker(Tags.UNUSED)
            } else {
                relay1Point.removeMarkerIfExists(Tags.WRITABLE)
                hayStack.clearAllAvailableLevelsInPoint(relay1[Tags.ID].toString())
            }
            hayStack.updatePoint(relay1Point.build(), relay1[Tags.ID].toString())

            val relay2 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.relay2 + "\"")
            var relay2Point = RawPoint.Builder().setHDict(relay2).setType("Relay N/O").setEnabled(config.relay2Enabled.enabled)
            if (config.relay2Enabled.enabled && config.relay2Association.associationVal == CpuRelayType.EXTERNALLY_MAPPED.ordinal) {
                relay2Point.addMarker(Tags.WRITABLE)
                relay2Point.addMarker(Tags.UNUSED)
            } else {
                relay2Point.removeMarkerIfExists(Tags.WRITABLE)
                relay2Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(relay2[Tags.ID].toString())
            }
            hayStack.updatePoint(relay2Point.build(), relay2[Tags.ID].toString())

            val relay3 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.relay3 + "\"")
            var relay3Point = RawPoint.Builder().setHDict(relay3).setType("Relay N/O").setEnabled(config.relay3Enabled.enabled)
            if (config.relay3Enabled.enabled && config.relay3Association.associationVal == CpuRelayType.EXTERNALLY_MAPPED.ordinal) {
                relay3Point.addMarker(Tags.WRITABLE)
                relay3Point.addMarker(Tags.UNUSED)
            } else {
                relay3Point.removeMarkerIfExists(Tags.WRITABLE)
                relay3Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(relay3[Tags.ID].toString())
            }
            hayStack.updatePoint(relay3Point.build(), relay3[Tags.ID].toString())

            val relay4 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.relay4 + "\"")
            var relay4Point = RawPoint.Builder().setHDict(relay4).setType("Relay N/O").setEnabled(config.relay4Enabled.enabled)
            if (config.relay4Enabled.enabled && config.relay4Association.associationVal == CpuRelayType.EXTERNALLY_MAPPED.ordinal) {
                relay4Point.addMarker(Tags.WRITABLE)
                relay4Point.addMarker(Tags.UNUSED)
            } else {
                relay4Point.removeMarkerIfExists(Tags.WRITABLE)
                relay4Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(relay4[Tags.ID].toString())
            }
            hayStack.updatePoint(relay4Point.build(), relay4[Tags.ID].toString())

            val relay5 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.relay5 + "\"")
            var relay5Point = RawPoint.Builder().setHDict(relay5).setType("Relay N/O").setEnabled(config.relay5Enabled.enabled)
            if (config.relay5Enabled.enabled && config.relay5Association.associationVal == CpuRelayType.EXTERNALLY_MAPPED.ordinal) {
                relay5Point.addMarker(Tags.WRITABLE)
                relay5Point.addMarker(Tags.UNUSED)
            } else {
                relay5Point.removeMarkerIfExists(Tags.WRITABLE)
                relay5Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(relay5[Tags.ID].toString())
            }
            hayStack.updatePoint(relay5Point.build(), relay5[Tags.ID].toString())

            val relay6 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.relay6 + "\"")
            var relay6Point = RawPoint.Builder().setHDict(relay6).setType("Relay N/O").setEnabled(config.relay6Enabled.enabled)
            if (config.relay6Enabled.enabled && config.relay6Association.associationVal == CpuRelayType.EXTERNALLY_MAPPED.ordinal) {
                relay6Point.addMarker(Tags.WRITABLE)
                relay6Point.addMarker(Tags.UNUSED)
            } else {
                relay6Point.removeMarkerIfExists(Tags.WRITABLE)
                relay6Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(relay6[Tags.ID].toString())
            }
            hayStack.updatePoint(relay6Point.build(), relay6[Tags.ID].toString())

            val relay7 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.relay7 + "\"")
            var relay7Point = RawPoint.Builder().setHDict(relay7).setType("Relay N/O").setEnabled(config.relay7Enabled.enabled)
            if (config.relay7Enabled.enabled && config.relay7Association.associationVal == CpuRelayType.EXTERNALLY_MAPPED.ordinal) {
                relay7Point.addMarker(Tags.WRITABLE)
                relay7Point.addMarker(Tags.UNUSED)
            } else {
                relay7Point.removeMarkerIfExists(Tags.WRITABLE)
                relay7Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(relay7[Tags.ID].toString())
            }
            hayStack.updatePoint(relay7Point.build(), relay7[Tags.ID].toString())

            val relay8 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.relay8 + "\"")
            var relay8Point = RawPoint.Builder().setHDict(relay8).setType("Relay N/O").setEnabled(config.relay8Enabled.enabled)
            if (config.relay8Enabled.enabled && config.relay8Association.associationVal == CpuRelayType.EXTERNALLY_MAPPED.ordinal) {
                relay8Point.addMarker(Tags.WRITABLE)
                relay8Point.addMarker(Tags.UNUSED)
            } else {
                relay8Point.removeMarkerIfExists(Tags.WRITABLE)
                relay8Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(relay8[Tags.ID].toString())
            }
            hayStack.updatePoint(relay8Point.build(), relay8[Tags.ID].toString())

            val analogOut1 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.analog1Out + "\"")
            var analogOut1Point = RawPoint.Builder().setHDict(analogOut1).setType(config.analogOut1TypeToString()).setEnabled(config.analogOut1Enabled.enabled)
            if (config.analogOut1Enabled.enabled && config.analogOut1Association.associationVal == CpuControlType.EXTERNALLY_MAPPED.ordinal) {
                analogOut1Point.addMarker(Tags.WRITABLE)
                analogOut1Point.addMarker(Tags.UNUSED)
            } else {
                analogOut1Point.removeMarkerIfExists(Tags.WRITABLE)
                analogOut1Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(analogOut1[Tags.ID].toString())
            }
            hayStack.updatePoint(analogOut1Point.build(), analogOut1[Tags.ID].toString())

            val analogOut2 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.analog2Out + "\"")
            var analogOut2Point = RawPoint.Builder().setHDict(analogOut2).setType(config.analogOut2TypeToString()).setEnabled(config.analogOut2Enabled.enabled)
            if (config.analogOut2Enabled.enabled && config.analogOut2Association.associationVal == CpuControlType.EXTERNALLY_MAPPED.ordinal) {
                analogOut2Point.addMarker(Tags.WRITABLE)
                analogOut2Point.addMarker(Tags.UNUSED)
            } else {
                analogOut2Point.removeMarkerIfExists(Tags.WRITABLE)
                analogOut2Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(analogOut2[Tags.ID].toString())
            }
            hayStack.updatePoint(analogOut2Point.build(), analogOut2[Tags.ID].toString())

            val analogOut3 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.analog3Out + "\"")
            var analogOut3Point = RawPoint.Builder().setHDict(analogOut3).setType(config.analogOut3TypeToString()).setEnabled(config.analogOut3Enabled.enabled)
            if (config.analogOut3Enabled.enabled && config.analogOut3Association.associationVal == CpuControlType.EXTERNALLY_MAPPED.ordinal) {
                analogOut3Point.addMarker(Tags.WRITABLE)
                analogOut3Point.addMarker(Tags.UNUSED)
            } else {
                analogOut3Point.removeMarkerIfExists(Tags.WRITABLE)
                analogOut3Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(analogOut3[Tags.ID].toString())
            }
            hayStack.updatePoint(analogOut3Point.build(), analogOut3[Tags.ID].toString())

            val analogOut4 = hayStack.readHDict("point and deviceRef == \""+device.get("id")+"\" and domainName == \"" + DomainName.analog4Out + "\"")
            var analogOut4Point = RawPoint.Builder().setHDict(analogOut4).setType(config.analogOut4TypeToString()).setEnabled(config.analogOut4Enabled.enabled)
            if (config.analogOut4Enabled.enabled && config.analogOut4Association.associationVal == CpuControlType.EXTERNALLY_MAPPED.ordinal) {
                analogOut4Point.addMarker(Tags.WRITABLE)
                analogOut4Point.addMarker(Tags.UNUSED)
            } else {
                analogOut4Point.removeMarkerIfExists(Tags.WRITABLE)
                analogOut4Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(analogOut4[Tags.ID].toString())
            }
            hayStack.updatePoint(analogOut4Point.build(), analogOut4[Tags.ID].toString())

        }

        fun handleNonDefaultConditioningMode(config: HyperStatSplitCpuProfileConfiguration, hayStack: CCUHsApi) {
            hayStack.readEntity("point and domainName == \"" + DomainName.conditioningMode + "\" and group == \"" + config.nodeAddress + "\"")["id"]?.let { conditioningModeIdObject ->
                val conditioningModeId = conditioningModeIdObject.toString()
                val isCoolingAvailable = config.isAnalogCoolingEnabled() || config.isCoolStage1Enabled() || config.isCoolStage2Enabled() || config.isCoolStage3Enabled()
                val isHeatingAvailable = config.isAnalogHeatingEnabled() || config.isHeatStage1Enabled() || config.isHeatStage2Enabled() || config.isHeatStage3Enabled()

                if (!isCoolingAvailable && !isHeatingAvailable) {
                    // If there are no cooling or heating outputs mapped, set the Conditioning Mode to OFF
                    hayStack.writeDefaultValById(conditioningModeId, StandaloneConditioningMode.OFF.ordinal.toDouble())
                } else if (!isCoolingAvailable) {
                    // If there are only heating outputs mapped, set the Conditioning Mode to HEAT-ONLY
                    hayStack.writeDefaultValById(conditioningModeId, StandaloneConditioningMode.HEAT_ONLY.ordinal.toDouble())
                } else if (!isHeatingAvailable) {
                    // If there are only cooling outputs mapped, set the Conditioning Mode to COOL-ONLY
                    hayStack.writeDefaultValById(conditioningModeId, StandaloneConditioningMode.COOL_ONLY.ordinal.toDouble())
                }
                // If both cooling and heating outputs are mapped, keep the default value of AUTO
            }
        }

        fun handleNonDefaultFanMode(config: HyperStatSplitCpuProfileConfiguration, hayStack: CCUHsApi) {
            val fanModePoint = hayStack.readEntity("point and domainName == \"" + DomainName.fanOpMode + "\" and group == \"" + config.nodeAddress + "\"")
            val fanModeId = fanModePoint.get("id").toString()

            val isFanEnabled = config.isLinearFanEnabled() || config.isStagedFanEnabled() || config.isFanLowRelayEnabled() || config.isFanMediumRelayEnabled() || config.isFanHighRelayEnabled()

            if (!isFanEnabled) {
                hayStack.writeDefaultValById(fanModeId, 0.0)
            }
        }

        fun initializePrePurgeStatus(config: HyperStatSplitProfileConfiguration, hayStack: CCUHsApi, value: Double) {
            if (config.prePurge.enabled) {
                val prePurgeStatusPoint =
                    hayStack.readEntity("point and domainName == \"" + DomainName.prePurgeStatus + "\" and group == \"" + config.nodeAddress + "\"")
                val prePurgeStatusId = prePurgeStatusPoint.get("id").toString()

                hayStack.writeHisValById(prePurgeStatusId, value)
            }
        }

        /**
         * Following enum class is used to define the control type for the HyperStat Split CPU Profile
         * This enum list is picked from the model & Need to update when any changes in the model for this enum
         */
        enum class CpuControlType {
            COOLING, LINEAR_FAN, HEATING, OAO_DAMPER, STAGED_FAN, RETURN_DAMPER, EXTERNALLY_MAPPED
        }

        enum class CpuRelayType {
            COOLING_STAGE1, COOLING_STAGE2, COOLING_STAGE3, HEATING_STAGE1, HEATING_STAGE2, HEATING_STAGE3, FAN_LOW_SPEED, FAN_MEDIUM_SPEED, FAN_HIGH_SPEED, FAN_ENABLED, OCCUPIED_ENABLED, HUMIDIFIER, DEHUMIDIFIER, EX_FAN_STAGE1, EX_FAN_STAGE2, DCV_DAMPER, EXTERNALLY_MAPPED
        }

    }

}