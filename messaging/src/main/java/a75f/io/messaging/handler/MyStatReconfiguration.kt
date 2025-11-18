package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.domain.api.DomainName
import a75f.io.domain.equips.mystat.MyStatEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.allStandaloneProfileConditions
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.L.TAG_CCU_PUBNUB
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode
import a75f.io.logic.bo.building.statprofiles.util.FanModeCacheStorage
import a75f.io.logic.bo.building.statprofiles.util.MyStatDeviceType
import a75f.io.logic.bo.building.statprofiles.util.MyStatFanStages
import a75f.io.logic.bo.building.statprofiles.util.MyStatPossibleFanMode
import a75f.io.logic.bo.building.statprofiles.util.getMyStatConfiguration
import a75f.io.logic.bo.building.statprofiles.util.getMyStatDomainDeviceByEquipRef
import a75f.io.logic.bo.building.statprofiles.util.getMyStatDomainEquipByEquipRef
import a75f.io.logic.bo.building.statprofiles.util.getMyStatFanLevel
import a75f.io.logic.bo.building.statprofiles.util.getMyStatModelByEquipRef
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPossibleConditionMode
import a75f.io.logic.bo.building.statprofiles.util.getMyStatPossibleFanModeSettings
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.util.modifyConditioningMode
import a75f.io.logic.util.modifyFanMode
import a75f.io.messaging.handler.MessageUtil.Companion.returnDurationDiff
import com.google.gson.JsonObject
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective


fun reconfigureMyStat(msgObject: JsonObject, configPoint: Point) {

    val hayStack = CCUHsApi.getInstance()

    val mystatEquip = hayStack.readEntity("equip and id == "+configPoint.equipRef)
    val model = getMyStatModelByEquipRef(configPoint.equipRef)
    val myStatDomainEquip = getMyStatDomainEquipByEquipRef(configPoint.equipRef)
    val myStatDomainDevice = getMyStatDomainDeviceByEquipRef(configPoint.equipRef)
    val myStatDeviceVersion = if(myStatDomainDevice.mystatDeviceVersion.readPointValue().toInt() ==1) MyStatDeviceType.MYSTAT_V2 else MyStatDeviceType.MYSTAT_V1
    CcuLog.d(L.TAG_CCU_PUBNUB,"reconfigureMyStat: myStatDeviceVersion : ${myStatDeviceVersion.name} for equipRef: ${configPoint.group} domain Name : ${configPoint.domainName}")

    if (model == null) {
        CcuLog.e(L.TAG_CCU_PUBNUB, "model is null for $configPoint")
        return
    }

    val config = getMyStatConfiguration(configPoint.equipRef)
    val equipBuilder = ProfileEquipBuilder(hayStack)
    val deviceModel = ModelLoader.getMyStatDeviceModel() as SeventyFiveFDeviceDirective
    val entityMapper = EntityMapper(model as SeventyFiveFProfileDirective)
    val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
    val deviceDis = "${hayStack.siteName}-${deviceModel.name}-${config!!.nodeAddress}"


    val pointNewValue = msgObject["val"]
    if(pointNewValue == null || pointNewValue.asString.isEmpty()){
        CcuLog.e(L.TAG_CCU_PUBNUB, "point is null $config")
    } else {
        // if it is myStat v1 device need to set the UOut 1 default value when UOut 1 was enabled at first time
        if (myStatDeviceVersion.name.equals(MyStatDeviceType.MYSTAT_V1.name) && config.universalOut1.enabled.not() && configPoint.domainName.equals(DomainName.universal1OutputEnable))
        {
            config.getAnalogOutDefaultValueForMyStatV1(config,myStatDeviceVersion.name)
            CcuLog.d(TAG_CCU_PUBNUB,"reconfigureMyStat: set UOut1 default value for MyStat V1 device : ${config.universalOut1.enabled} -  Association ${config.universalOut1Association.associationVal}")
        }
        else
        {
            CcuLog.d(TAG_CCU_PUBNUB,"reconfigureMyStat: UOut1 already enabled : ${config.universalOut1.enabled} -  Association ${config.universalOut1Association.associationVal}")
        }
        updateConfiguration(configPoint.domainName, pointNewValue.asDouble, config)
        equipBuilder.updateEquipAndPoints(config, model , hayStack.getSite()!!.id, mystatEquip["dis"].toString(), true)
        if (configPoint.domainName == DomainName.fanOpMode) {
            myStatupdateFanMode(configPoint.equipRef, pointNewValue.asInt)
        }
        deviceBuilder.updateDeviceAndPoints(config, deviceModel, mystatEquip["id"].toString(), hayStack.site!!.id, deviceDis)
        val equip = MyStatEquip(configPoint.equipRef)
        if (configPoint.domainName != DomainName.fanOpMode) {
            updateMyStatFanMode(equip, getMyStatFanLevel(config))
        }

    }
    writePointFromJson(configPoint, msgObject, hayStack)
    config.apply { setPortConfiguration( nodeAddress, getRelayMap(), getAnalogMap()) }
    DesiredTempDisplayMode.setModeType(configPoint.roomRef, CCUHsApi.getInstance())

    /*
    - If we do reconfiguration from portal for fanMode, level 10 updated as ( val = 9)
    - Now if user change the fanMode from CCU, it will update the level 8 as 1
    - Now if we do reconfiguration from portal for fanMode, message will receive for only removing level 8 not for level 10
    - Because level 10 does not have change of value, so silo will never send update entity for level 10
    - Due to this fanMode will not update in CCU's shared preference
     */
    if ((pointNewValue == null || pointNewValue.asString.isEmpty()) && configPoint.domainName == DomainName.fanOpMode) {
        myStatupdateFanMode(configPoint.equipRef, HSUtil.getPriorityVal(configPoint.id).toInt())
    }
    if(myStatDeviceVersion.name.equals(MyStatDeviceType.MYSTAT_V1.name)){
        config.updateEnumConfigs(myStatDomainEquip, myStatDeviceVersion.name)
        CcuLog.d(TAG_CCU_PUBNUB,"reconfigureMyStat:  MyStat V1 device :  ${myStatDeviceVersion.name}: updated the enum associated configs")

    }

    // updating the fan/con mode eums
        config.apply {

            val possibleConditioningMode = getMyStatPossibleConditionMode(config)
            val possibleFanMode = getMyStatPossibleFanModeSettings(getMyStatFanLevel(config))
            myStatDomainEquip?.let {
                modifyFanMode(possibleFanMode, it.fanOpMode)
                modifyConditioningMode(
                    possibleConditioningMode.ordinal,
                    it.conditioningMode,
                    allStandaloneProfileConditions
                )
            }
            CcuLog.i(L.TAG_CCU_PUBNUB, "updateConfigPoint for MyStat Reconfiguration fan / conditioning mode ")
        }
    
    CcuLog.i(L.TAG_CCU_PUBNUB, "updateConfigPoint for MyStat  Reconfiguration $config")

}

fun myStatupdateFanMode(equipRef: String, fanMode: Int) {
    CcuLog.i(L.TAG_CCU_PUBNUB, "updateFanMode $fanMode")
    val cache = FanModeCacheStorage.getMyStatFanModeCache()
    if (fanMode != 0  && (fanMode % 3 == 0 || isFanModeCurrentOccupied(fanMode)) ) {
        cache.saveFanModeInCache(equipRef, fanMode)
    } else {
        cache.removeFanModeFromCache(equipRef)
    }
}

private fun isFanModeCurrentOccupied(value : Int): Boolean {
    val basicSettings = MyStatFanStages.values()[value]
    return (basicSettings == MyStatFanStages.LOW_CUR_OCC || basicSettings == MyStatFanStages.HIGH_CUR_OCC)
}

private fun writePointFromJson(configPoint: Point, msgObject: JsonObject, hayStack: CCUHsApi) {
    try {
        val who = msgObject[HayStackConstants.WRITABLE_ARRAY_WHO].asString
        val level = msgObject[HayStackConstants.WRITABLE_ARRAY_LEVEL].asInt
        val value = msgObject[HayStackConstants.WRITABLE_ARRAY_VAL].asString
        if (value.isEmpty()) {
            hayStack.clearPointArrayLevel(configPoint.id, level, false)
            hayStack.writeHisValById(configPoint.id, HSUtil.getPriorityVal(configPoint.id))
            return
        }
        val durationDiff = returnDurationDiff(msgObject);
        hayStack.writePointLocal(configPoint.id, level, who, value.toDouble(), durationDiff)
        hayStack.writeHisValById(configPoint.id, value.toDouble())
        CcuLog.d(
            L.TAG_CCU_PUBNUB,
            "MyStat: writePointFromJson - level: $level who: $who val: $value  durationDiff: $durationDiff"
        )
    } catch (e: Exception) {
        CcuLog.e(L.TAG_CCU_PUBNUB, "Failed to parse tuner value : " + msgObject + " ; " + e.message)
    }
}


private fun updateMyStatFanMode(equip: MyStatEquip, fanLevel: Int) {

    val possibleFanMode = getMyStatPossibleFanModeSettings(fanLevel)
    if (possibleFanMode == MyStatPossibleFanMode.OFF) {
        equip.fanOpMode.writePointValue(StandaloneConditioningMode.OFF.ordinal.toDouble())
        return
    }
    if (possibleFanMode == MyStatPossibleFanMode.AUTO) {
        equip.fanOpMode.writePointValue(StandaloneConditioningMode.AUTO.ordinal.toDouble())
        return
    }
    val currentFanMode = MyStatFanStages.values()[equip.fanOpMode.readPriorityVal().toInt()]
    if (currentFanMode != MyStatFanStages.AUTO) {

        if (possibleFanMode == MyStatPossibleFanMode.LOW && currentFanMode.ordinal > MyStatFanStages.LOW_ALL_TIME.ordinal) {
            equip.fanOpMode.writePointValue(MyStatFanStages.OFF.ordinal.toDouble())
        }
        if (possibleFanMode == MyStatPossibleFanMode.HIGH && currentFanMode.ordinal < MyStatFanStages.HIGH_CUR_OCC.ordinal) {
            equip.fanOpMode.writePointValue(MyStatFanStages.OFF.ordinal.toDouble())
        }
    }
}