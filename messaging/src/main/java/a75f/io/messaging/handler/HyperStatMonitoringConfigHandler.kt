package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import a75f.io.domain.api.Domain
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.ModelLoader.getModelForDomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.hyperstat.profiles.util.getConfiguration
import a75f.io.logic.bo.building.hyperstatmonitoring.HyperStatV2MonitoringProfile
import com.google.gson.JsonObject
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective


/*
* created by spoorthidev on 20-July-2021
*/
internal object HyperStatMonitoringConfigHandler {
    @JvmStatic
    fun reconfigureMonitoring(msgObject: JsonObject, configPoint: Point) {
        val hayStack = CCUHsApi.getInstance()
        val hyperStatMonitoringEquip = hayStack.readEntity("equip and id == "+configPoint.equipRef)

        val profileModel = ModelLoader.getHyperStatMonitoringModel() as SeventyFiveFProfileDirective
        val deviceModel = ModelLoader.getHyperStatDeviceModel() as SeventyFiveFDeviceDirective

        val equipBuilder = ProfileEquipBuilder(hayStack)
        val entityMapper = EntityMapper(profileModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val config = getConfiguration(configPoint.equipRef)?.getActiveConfiguration()//make it common for all

        val  deviceDis = "${hayStack.siteName}-${deviceModel.name}-${config!!.nodeAddress}"

        val pointNewValue = msgObject["val"].asDouble
        updateConfiguration(configPoint.domainName, pointNewValue, config!!)

        equipBuilder.updateEquipAndPoints(config!!, profileModel
            , hayStack.getSite()!!.id, hyperStatMonitoringEquip["dis"].toString(), true)
        CcuLog.i(Domain.LOG_TAG, " updated Monitoring Equip and Points")

        deviceBuilder.updateDeviceAndPoints(config, deviceModel, hyperStatMonitoringEquip["id"].toString(), hayStack.site!!.id, deviceDis)
        CcuLog.i(L.TAG_CCU_PUBNUB, "updated Monitoring Device and Points $config")
    }
}
