package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.RawPoint
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.HyperStatSplitEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.allStandaloneProfileConditions
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.hvac.StandaloneFanStage
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HSSplitHaystackUtil.Companion.getHssProfileConditioningMode
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.common.HyperStatSplitAssociationUtil.Companion.getHssProfileFanLevel
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.CpuEconSensorBusTempAssociation
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.HyperStatSplitControlType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UVConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UVRelayControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UvAnalogOutControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVConfiguration
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UVRelayControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UvAnalogOutControls
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.getPossibleFanMode
import a75f.io.logic.bo.building.statprofiles.util.getSplitConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getSplitDomainEquipByEquipRef
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.util.modifyConditioningMode
import a75f.io.logic.util.modifyFanMode
import a75f.io.messaging.handler.MessageUtil.Companion.returnDurationDiff
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
                            val cache = FanModeCacheStorage.getHyperStatSplitFanModeCache()
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
                    val cache = FanModeCacheStorage.getHyperStatSplitFanModeCache()
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
            val splitDomainEquip = getSplitDomainEquipByEquipRef(configPoint.equipRef)
            if (configPoint.markers.contains(Tags.USERINTENT) && configPoint.markers.contains(Tags.CONDITIONING)) {
                DesiredTempDisplayMode.setModeTypeOnUserIntentChange(configPoint.roomRef, CCUHsApi.getInstance())
            } else {
                DesiredTempDisplayMode.setModeType(configPoint.roomRef, CCUHsApi.getInstance())
            }
            if (isDynamicConfigPoint(configPoint)) handleDynamicConfig(configPoint, hayStack)
            // Update fan/conditioning mode enums for Split Domain Equip
                splitDomainEquip?.let { equip ->
                    val config = getSplitConfiguration(configPoint.equipRef)
                    config.apply {
                        val possibleConditioningMode = getHssProfileConditioningMode(this)
                        val possibleFanMode = getPossibleFanMode(equip)
                        modifyFanMode(possibleFanMode.ordinal, equip.fanOpMode)
                        modifyConditioningMode(
                            possibleConditioningMode.ordinal,
                            equip.conditioningMode,
                            allStandaloneProfileConditions
                        )

                        CcuLog.i(L.TAG_CCU_PUBNUB, "updatedConfigPoint for HS Split fan/cond mode -> $this")
                    }
                }


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
                    configPoint.domainName.equals(DomainName.universalIn8Association) ||
                    isAnalogMinMaxConfig(configPoint.domainName)
        }

        private fun isAnalogMinMaxConfig(domainName : String) : Boolean {
            val regex = Regex("analog\\d+(Min|Max)")
            return regex.containsMatchIn(domainName)
        }

        private fun handleDynamicConfig (configPoint: Point, hayStack: CCUHsApi) {
            CcuLog.i(L.TAG_CCU_PUBNUB, "Point " + configPoint.domainName + " would trigger a reconfig")

            val equipModel = ModelLoader.getHyperStatSplitCpuModel() as SeventyFiveFProfileDirective
            val deviceModel = ModelLoader.getHyperStatSplitDeviceModel() as SeventyFiveFDeviceDirective
            val profileConfiguration = HyperStatSplitCpuConfiguration(
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

            correctSensorBusTempPoints(profileConfiguration, hayStack)
            mapSensorBusPressureLogicalPoint(profileConfiguration, equipId, hayStack)
            setOutputTypes(profileConfiguration, hayStack)

            handleNonDefaultConditioningMode(profileConfiguration, hayStack)
            handleNonDefaultFanMode(profileConfiguration, hayStack)

            initializePrePurgeStatus(profileConfiguration, hayStack, 1.0)
            initializePrePurgeStatus(profileConfiguration, hayStack, 0.0)

            hayStack.syncEntityTree()

        }

        private fun pointUpdateOwner(configPoint: Point, msgObject: JsonObject, hayStack: CCUHsApi) {
            try {
                val who = msgObject[HayStackConstants.WRITABLE_ARRAY_WHO].asString
                val level = msgObject[HayStackConstants.WRITABLE_ARRAY_LEVEL].asInt
                val value = msgObject[HayStackConstants.WRITABLE_ARRAY_VAL].asDouble
                val durationDiff = returnDurationDiff(msgObject);
                CcuLog.d(
                    L.TAG_CCU_PUBNUB,
                    "HS Split: writePointFromJson - level: $level who: $who val: $value  durationDiff: $durationDiff"
                )
                hayStack.writePointLocal(configPoint.id, level, who, value, durationDiff)
            } catch (e: Exception) {
                e.printStackTrace()
                CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : " + msgObject + " ; " + e.message)
            }
        }

        fun mapSensorBusPressureLogicalPoint(
            config: HyperStatSplitConfiguration,
            equipRef: String,
            hayStack: CCUHsApi
        ) {
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

        fun correctSensorBusTempPoints(config: HyperStatSplitConfiguration, hayStack: CCUHsApi) {
            val device = hayStack.read("device and addr == \"" + config.nodeAddress + "\"")

            if (!isAnySensorBusMapped(config, CpuEconSensorBusTempAssociation.OUTSIDE_AIR_TEMPERATURE_HUMIDITY)) {
                val deviceOATMap = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.outsideAirTempSensor + "\"")
                val deviceOATPoint = RawPoint.Builder().setHDict(deviceOATMap).setEnabled(false).setPointRef(null)
                hayStack.updatePoint(deviceOATPoint.build(), deviceOATMap[Tags.ID].toString())

                val deviceOAHMap = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.outsideAirHumiditySensor + "\"")
                val deviceOAHPoint = RawPoint.Builder().setHDict(deviceOAHMap).setEnabled(false).setPointRef(null)
                hayStack.updatePoint(deviceOAHPoint.build(), deviceOAHMap[Tags.ID].toString())
            }

            if (!isAnySensorBusMapped(config, CpuEconSensorBusTempAssociation.MIXED_AIR_TEMPERATURE_HUMIDITY)) {
                val deviceMATMap = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.mixedAirTempSensor + "\"")
                val deviceMATPoint = RawPoint.Builder().setHDict(deviceMATMap).setEnabled(false).setPointRef(null)
                hayStack.updatePoint(deviceMATPoint.build(), deviceMATMap[Tags.ID].toString())

                val deviceMAHMap = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.mixedAirHumiditySensor + "\"")
                val deviceMAHPoint = RawPoint.Builder().setHDict(deviceMAHMap).setEnabled(false).setPointRef(null)
                hayStack.updatePoint(deviceMAHPoint.build(), deviceMAHMap[Tags.ID].toString())
            }

            if (!isAnySensorBusMapped(config, CpuEconSensorBusTempAssociation.SUPPLY_AIR_TEMPERATURE_HUMIDITY)) {
                val deviceSATMap = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.supplyAirTemperature + "\"")
                val deviceSATPoint = RawPoint.Builder().setHDict(deviceSATMap).setEnabled(false).setPointRef(null)
                hayStack.updatePoint(deviceSATPoint.build(), deviceSATMap[Tags.ID].toString())

                val deviceSAHMap = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.supplyAirHumiditySensor + "\"")
                val deviceSAHPoint = RawPoint.Builder().setHDict(deviceSAHMap).setEnabled(false).setPointRef(null)
                hayStack.updatePoint(deviceSAHPoint.build(), deviceSAHMap[Tags.ID].toString())
            }

        }

        private fun isAnySensorBusMapped(config: HyperStatSplitConfiguration, type: CpuEconSensorBusTempAssociation): Boolean {
            return (config.address0Enabled.enabled && config.address0SensorAssociation.temperatureAssociation.associationVal == type.ordinal) ||
                    (config.address1Enabled.enabled && config.address1SensorAssociation.temperatureAssociation.associationVal == type.ordinal) ||
                    (config.address2Enabled.enabled && config.address2SensorAssociation.temperatureAssociation.associationVal == type.ordinal)
        }

        fun setOutputTypes(config: HyperStatSplitConfiguration, hayStack: CCUHsApi) {
            val device = hayStack.read("device and addr == \"" + config.nodeAddress + "\"")

            val relay1 = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.relay1 + "\"")
            val relay1Point = RawPoint.Builder().setHDict(relay1).setType("Relay N/O").setEnabled(config.relay1Enabled.enabled)
            if (config.relay1Enabled.enabled && config.relay1Association.associationVal ==config.getProfileBasedEnumValueRelayType(HyperStatSplitControlType.EXTERNALLY_MAPPED.name,config)) {
                relay1Point.addMarker(Tags.WRITABLE)
                relay1Point.addMarker(Tags.UNUSED)
            } else if(relay1.has(Tags.UNUSED)) {
                relay1Point.removeMarkerIfExists(Tags.WRITABLE)
                hayStack.clearAllAvailableLevelsInPoint(relay1[Tags.ID].toString())
            }
            hayStack.updatePoint(relay1Point.build(), relay1[Tags.ID].toString())

            val relay2 = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.relay2 + "\"")
            val relay2Point = RawPoint.Builder().setHDict(relay2).setType("Relay N/O").setEnabled(config.relay2Enabled.enabled)
            if (config.relay2Enabled.enabled && config.relay2Association.associationVal == config.getProfileBasedEnumValueRelayType(HyperStatSplitControlType.EXTERNALLY_MAPPED.name,config)) {
                relay2Point.addMarker(Tags.WRITABLE)
                relay2Point.addMarker(Tags.UNUSED)
            } else if(relay2.has(Tags.UNUSED)) {
                relay2Point.removeMarkerIfExists(Tags.WRITABLE)
                relay2Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(relay2[Tags.ID].toString())
            }
            hayStack.updatePoint(relay2Point.build(), relay2[Tags.ID].toString())

            val relay3 = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.relay3 + "\"")
            val relay3Point = RawPoint.Builder().setHDict(relay3).setType("Relay N/O").setEnabled(config.relay3Enabled.enabled)
            if (config.relay3Enabled.enabled && config.relay3Association.associationVal == config.getProfileBasedEnumValueRelayType(HyperStatSplitControlType.EXTERNALLY_MAPPED.name,config)) {
                relay3Point.addMarker(Tags.WRITABLE)
                relay3Point.addMarker(Tags.UNUSED)
            } else if(relay3.has(Tags.UNUSED)) {
                relay3Point.removeMarkerIfExists(Tags.WRITABLE)
                relay3Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(relay3[Tags.ID].toString())
            }
            hayStack.updatePoint(relay3Point.build(), relay3[Tags.ID].toString())

            val relay4 = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.relay4 + "\"")
            val relay4Point = RawPoint.Builder().setHDict(relay4).setType("Relay N/O").setEnabled(config.relay4Enabled.enabled)
            if (config.relay4Enabled.enabled && config.relay4Association.associationVal ==config.getProfileBasedEnumValueRelayType(HyperStatSplitControlType.EXTERNALLY_MAPPED.name,config)) {
                relay4Point.addMarker(Tags.WRITABLE)
                relay4Point.addMarker(Tags.UNUSED)
            } else if(relay4.has(Tags.UNUSED)) {
                relay4Point.removeMarkerIfExists(Tags.WRITABLE)
                relay4Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(relay4[Tags.ID].toString())
            }
            hayStack.updatePoint(relay4Point.build(), relay4[Tags.ID].toString())

            val relay5 = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.relay5 + "\"")
            val relay5Point = RawPoint.Builder().setHDict(relay5).setType("Relay N/O").setEnabled(config.relay5Enabled.enabled)
            if (config.relay5Enabled.enabled && config.relay5Association.associationVal == config.getProfileBasedEnumValueRelayType(HyperStatSplitControlType.EXTERNALLY_MAPPED.name,config)) {
                relay5Point.addMarker(Tags.WRITABLE)
                relay5Point.addMarker(Tags.UNUSED)
            } else if(relay5.has(Tags.UNUSED)) {
                relay5Point.removeMarkerIfExists(Tags.WRITABLE)
                relay5Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(relay5[Tags.ID].toString())
            }
            hayStack.updatePoint(relay5Point.build(), relay5[Tags.ID].toString())

            val relay6 = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.relay6 + "\"")
            val relay6Point = RawPoint.Builder().setHDict(relay6).setType("Relay N/O").setEnabled(config.relay6Enabled.enabled)
            if (config.relay6Enabled.enabled && config.relay6Association.associationVal == config.getProfileBasedEnumValueRelayType(HyperStatSplitControlType.EXTERNALLY_MAPPED.name,config)) {
                relay6Point.addMarker(Tags.WRITABLE)
                relay6Point.addMarker(Tags.UNUSED)
            } else if(relay6.has(Tags.UNUSED)) {
                relay6Point.removeMarkerIfExists(Tags.WRITABLE)
                relay6Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(relay6[Tags.ID].toString())
            }
            hayStack.updatePoint(relay6Point.build(), relay6[Tags.ID].toString())

            val relay7 = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.relay7 + "\"")
            val relay7Point = RawPoint.Builder().setHDict(relay7).setType("Relay N/O").setEnabled(config.relay7Enabled.enabled)
            if (config.relay7Enabled.enabled && config.relay7Association.associationVal ==config.getProfileBasedEnumValueRelayType(HyperStatSplitControlType.EXTERNALLY_MAPPED.name,config)) {
                relay7Point.addMarker(Tags.WRITABLE)
                relay7Point.addMarker(Tags.UNUSED)
            } else if(relay7.has(Tags.UNUSED)) {
                relay7Point.removeMarkerIfExists(Tags.WRITABLE)
                relay7Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(relay7[Tags.ID].toString())
            }
            hayStack.updatePoint(relay7Point.build(), relay7[Tags.ID].toString())

            val relay8 = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.relay8 + "\"")
            val relay8Point = RawPoint.Builder().setHDict(relay8).setType("Relay N/O").setEnabled(config.relay8Enabled.enabled)
            if (config.relay8Enabled.enabled && config.relay8Association.associationVal == config.getProfileBasedEnumValueRelayType(HyperStatSplitControlType.EXTERNALLY_MAPPED.name,config)) {
                relay8Point.addMarker(Tags.WRITABLE)
                relay8Point.addMarker(Tags.UNUSED)
            } else if(relay8.has(Tags.UNUSED)) {
                relay8Point.removeMarkerIfExists(Tags.WRITABLE)
                relay8Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(relay8[Tags.ID].toString())
            }
            hayStack.updatePoint(relay8Point.build(), relay8[Tags.ID].toString())

            val analogOut1 = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.analog1Out + "\"")
            val analogOut1Point = RawPoint.Builder().setHDict(analogOut1).setType(config.analogOut1TypeToString()).setEnabled(config.analogOut1Enabled.enabled)
            if (config.analogOut1Enabled.enabled && config.analogOut1Association.associationVal == config.getProfileBasedEnumValueAnalogType(HyperStatSplitControlType.EXTERNALLY_MAPPED.name,config)) {
                analogOut1Point.addMarker(Tags.WRITABLE)
                analogOut1Point.addMarker(Tags.UNUSED)
            } else if(analogOut1.has(Tags.UNUSED)) {
                analogOut1Point.removeMarkerIfExists(Tags.WRITABLE)
                analogOut1Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(analogOut1[Tags.ID].toString())
            }
            hayStack.updatePoint(analogOut1Point.build(), analogOut1[Tags.ID].toString())

            val analogOut2 = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.analog2Out + "\"")
            val analogOut2Point = RawPoint.Builder().setHDict(analogOut2).setType(config.analogOut2TypeToString()).setEnabled(config.analogOut2Enabled.enabled)
            if (config.analogOut2Enabled.enabled && config.analogOut2Association.associationVal == config.getProfileBasedEnumValueAnalogType(HyperStatSplitControlType.EXTERNALLY_MAPPED.name,config)) {
                analogOut2Point.addMarker(Tags.WRITABLE)
                analogOut2Point.addMarker(Tags.UNUSED)
            } else if(analogOut2.has(Tags.UNUSED)) {
                analogOut2Point.removeMarkerIfExists(Tags.WRITABLE)
                analogOut2Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(analogOut2[Tags.ID].toString())
            }
            hayStack.updatePoint(analogOut2Point.build(), analogOut2[Tags.ID].toString())

            val analogOut3 = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.analog3Out + "\"")
            val analogOut3Point = RawPoint.Builder().setHDict(analogOut3).setType(config.analogOut3TypeToString()).setEnabled(config.analogOut3Enabled.enabled)
            if (config.analogOut3Enabled.enabled && config.analogOut3Association.associationVal == config.getProfileBasedEnumValueAnalogType(HyperStatSplitControlType.EXTERNALLY_MAPPED.name,config)) {
                analogOut3Point.addMarker(Tags.WRITABLE)
                analogOut3Point.addMarker(Tags.UNUSED)
            } else if(analogOut3.has(Tags.UNUSED)) {
                analogOut3Point.removeMarkerIfExists(Tags.WRITABLE)
                analogOut3Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(analogOut3[Tags.ID].toString())
            }
            hayStack.updatePoint(analogOut3Point.build(), analogOut3[Tags.ID].toString())

            val analogOut4 = hayStack.readHDict("point and deviceRef == \""+ device["id"] +"\" and domainName == \"" + DomainName.analog4Out + "\"")
            val analogOut4Point = RawPoint.Builder().setHDict(analogOut4).setType(config.analogOut4TypeToString()).setEnabled(config.analogOut4Enabled.enabled)
            if (config.analogOut4Enabled.enabled && config.analogOut4Association.associationVal == config.getProfileBasedEnumValueAnalogType(HyperStatSplitControlType.EXTERNALLY_MAPPED.name,config)) {
                analogOut4Point.addMarker(Tags.WRITABLE)
                analogOut4Point.addMarker(Tags.UNUSED)
            } else if(analogOut4.has(Tags.UNUSED)) {
                analogOut4Point.removeMarkerIfExists(Tags.WRITABLE)
                analogOut4Point.removeMarkerIfExists(Tags.UNUSED)
                hayStack.clearAllAvailableLevelsInPoint(analogOut4[Tags.ID].toString())
            }
            hayStack.updatePoint(analogOut4Point.build(), analogOut4[Tags.ID].toString())

        }

        fun handleNonDefaultConditioningMode(config: HyperStatSplitConfiguration, hayStack: CCUHsApi) {

            fun isCoolingExist(): Boolean {

                return when (config) {

                    is Pipe4UVConfiguration -> return config.isAnyAnalogEnabledAndMapped(
                        Pipe4UvAnalogOutControls.COOLING_WATER_MODULATING_VALVE)||
                            config.isAnyRelayEnabledAndMapped(Pipe4UVRelayControls.COOLING_WATER_VALVE)

                    is HyperStatSplitCpuConfiguration -> return config.isAnalogCoolingEnabled() || config.isCoolStage1Enabled() ||
                            config.isCoolStage2Enabled() || config.isCoolStage3Enabled() ||
                            config.isCompressorStagesAvailable() || config.isAnalogCompressorEnabled()
                    is Pipe2UVConfiguration -> return  true

                    else -> true
                }
            }

            fun isHeatingExist(): Boolean {

                return  when(config){

                    is Pipe4UVConfiguration -> return (config.isAnyAnalogEnabledAndMapped(Pipe4UvAnalogOutControls.HEATING_WATER_MODULATING_VALVE) ||
                            config.isAnyRelayEnabledAndMapped(Pipe4UVRelayControls.HEATING_WATER_VALVE)||(config.isAnyRelayEnabledAndMapped(Pipe4UVRelayControls.AUX_HEATING_STAGE1)||config.isAnyRelayEnabledAndMapped(Pipe4UVRelayControls.AUX_HEATING_STAGE2)))

                    is HyperStatSplitCpuConfiguration -> return config.isAnalogHeatingEnabled() || config.isHeatStage1Enabled() ||
                            config.isHeatStage2Enabled() || config.isHeatStage3Enabled() ||
                            config.isCompressorStagesAvailable() || config.isAnalogCompressorEnabled()
                    is Pipe2UVConfiguration -> return true

                    else -> true
                }
            }


            hayStack.readEntity("point and domainName == \"" + DomainName.conditioningMode + "\" and group == \"" + config.nodeAddress + "\"")["id"]?.let { conditioningModeIdObject ->
                val conditioningModeId = conditioningModeIdObject.toString()
                val isCoolingAvailable = isCoolingExist()
                val isHeatingAvailable = isHeatingExist()

                if (!isCoolingAvailable && !isHeatingAvailable) {
                    // If there are no cooling or heating outputs mapped, set the Conditioning Mode to OFF
                    hayStack.writeDefaultValById(conditioningModeId, StandaloneConditioningMode.OFF.ordinal.toDouble())
                    hayStack.writeHisValById(conditioningModeId, StandaloneConditioningMode.OFF.ordinal.toDouble())
                } else if (!isCoolingAvailable) {
                    // If there are only heating outputs mapped, set the Conditioning Mode to HEAT-ONLY
                    hayStack.writeDefaultValById(conditioningModeId, StandaloneConditioningMode.HEAT_ONLY.ordinal.toDouble())
                    hayStack.writeHisValById(conditioningModeId, StandaloneConditioningMode.HEAT_ONLY.ordinal.toDouble())
                } else if (!isHeatingAvailable) {
                    // If there are only cooling outputs mapped, set the Conditioning Mode to COOL-ONLY
                    hayStack.writeDefaultValById(conditioningModeId, StandaloneConditioningMode.COOL_ONLY.ordinal.toDouble())
                    hayStack.writeHisValById(conditioningModeId, StandaloneConditioningMode.COOL_ONLY.ordinal.toDouble())
                }
                // If both cooling and heating outputs are mapped, keep the default value of AUTO
            }
        }

        fun handleNonDefaultFanMode(config: HyperStatSplitConfiguration, hayStack: CCUHsApi) {
            val fanModePoint = hayStack.readEntity("point and domainName == \"" + DomainName.fanOpMode + "\" and group == \"" + config.nodeAddress + "\"")
            val fanModeId = fanModePoint["id"].toString()

            val isFanEnabledWithAllOptions = returnProfileBasedFanModeEnabledStatus(config)

            if (config.isFanEnabled(config,HyperStatSplitControlType.FAN_ENABLED.name) && !isFanEnabledWithAllOptions) {
                hayStack.writeDefaultValById(fanModeId, 1.0)
                hayStack.writeHisValById(fanModeId, 0.0)
                FanModeCacheStorage.getHyperStatSplitFanModeCache().removeFanModeFromCache(fanModePoint["equipRef"].toString())
            }
            else if (!isFanEnabledWithAllOptions)  {
                hayStack.writeDefaultValById(fanModeId, 0.0)
                hayStack.writeHisValById(fanModeId, 0.0)
                FanModeCacheStorage.getHyperStatSplitFanModeCache().removeFanModeFromCache(fanModePoint["equipRef"].toString())
            }
        }
        private fun returnProfileBasedFanModeEnabledStatus(
            config: HyperStatSplitConfiguration
        ): Boolean = when (config) {

            is Pipe4UVConfiguration -> with(config) {
                //Analog mapping
                isAnyAnalogEnabledAndMapped(Pipe4UvAnalogOutControls.FAN_SPEED) ||
                        // Relay mapping
                        isAnyRelayEnabledAndMapped(Pipe4UVRelayControls.FAN_LOW_SPEED) ||
                        isAnyRelayEnabledAndMapped(Pipe4UVRelayControls.FAN_MEDIUM_SPEED)||
                        isAnyRelayEnabledAndMapped(Pipe4UVRelayControls.FAN_HIGH_SPEED)
            }

            is HyperStatSplitCpuConfiguration -> with(config) {
                isLinearFanEnabled() || isStagedFanEnabled() ||
                        isFanLowEnabled() || isFanMediumEnabled() || isFanHighEnabled()
            }

            is Pipe2UVConfiguration -> with(config) {
                isAnyRelayMappedAndEnabled(Pipe2UVRelayControls.FAN_LOW_SPEED)||
                       isAnyRelayMappedAndEnabled(Pipe2UVRelayControls.FAN_MEDIUM_SPEED) ||
                        isAnyRelayMappedAndEnabled(Pipe2UVRelayControls.FAN_HIGH_SPEED) ||
                        isAnyAnalogMappedAndEnabled(Pipe2UvAnalogOutControls.FAN_SPEED)
            }

            else -> false
        }

        fun initializePrePurgeStatus(config: HyperStatSplitConfiguration, hayStack: CCUHsApi, value: Double) {
            val prePurgeStatusPoint =
                hayStack.readEntity("point and domainName == \"" + DomainName.prePurgeStatus + "\" and group == \"" + config.nodeAddress + "\"")
            val prePurgeStatusId = prePurgeStatusPoint["id"].toString()
            if (config.prePurge.enabled) {
                hayStack.writeHisValById(prePurgeStatusId, value)
            } else {
                hayStack.writeHisValById(prePurgeStatusId, 0.0)
            }
        }

    }

}