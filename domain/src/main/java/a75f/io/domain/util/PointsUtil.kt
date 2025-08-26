package a75f.io.domain.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.PointBuilderConfig
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

class PointsUtil(private val hayStack : CCUHsApi) {

    fun createDynamicSensorEquipPoint(equip: Equip, domainName: String, config: ProfileConfiguration): String? {

        val tz = hayStack.timeZone
        CcuLog.i(Domain.LOG_TAG, "add Dynamic Sensor Equip point - $domainName")
        val profileEquipBuilder = ProfileEquipBuilder(hayStack)
        val sensor = hayStack.readEntity("point and domainName == \"$domainName\" and equipRef == \"${equip.id}\"")
        if (sensor.isNotEmpty()) {
            CcuLog.i(Domain.LOG_TAG, "$domainName is already present for equip ${equip.equipRef}")
            return sensor[Tags.ID].toString()
        }
        val modelDef = getModelFromEquip(equip)
        val modelPointDef = modelDef?.points?.find { it.domainName == domainName }
        modelPointDef?.run {
            val hayStackPoint = profileEquipBuilder.buildPoint(PointBuilderConfig(modelPointDef, config, equip.id, equip.siteRef, tz, equip.displayName))
            val pointId = hayStack.addPoint(hayStackPoint)
            hayStackPoint.id = pointId
            DomainManager.addPoint(hayStackPoint)
            return hayStackPoint.id
        }
        return null
    }

    fun getModelFromEquip(equip: Equip): SeventyFiveFProfileDirective? {
        return when (equip.domainName) {
            DomainName.smartnodeDAB -> ModelLoader.getSmartNodeDabModel() as SeventyFiveFProfileDirective
            DomainName.helionodeDAB -> ModelLoader.getHelioNodeDabModel() as SeventyFiveFProfileDirective
            DomainName.smartnodeVAVReheatNoFan -> ModelLoader.getSmartNodeVavNoFanModelDef() as SeventyFiveFProfileDirective
            DomainName.smartnodeVAVReheatParallelFan -> ModelLoader.getSmartNodeVavParallelFanModelDef() as SeventyFiveFProfileDirective
            DomainName.smartnodeVAVReheatSeriesFan -> ModelLoader.getSmartNodeVavSeriesModelDef() as SeventyFiveFProfileDirective
            DomainName.smartnodeActiveChilledBeam -> ModelLoader.getSmartNodeVavAcbModelDef() as SeventyFiveFProfileDirective
            DomainName.helionodeVAVReheatNoFan -> ModelLoader.getHelioNodeVavNoFanModelDef() as SeventyFiveFProfileDirective
            DomainName.helionodeVAVReheatParallelFan -> ModelLoader.getHelioNodeVavParallelFanModelDef() as SeventyFiveFProfileDirective
            DomainName.helionodeVAVReheatSeriesFan -> ModelLoader.getHelioNodeVavSeriesModelDef() as SeventyFiveFProfileDirective
            DomainName.helionodeActiveChilledBeam -> ModelLoader.getHelioNodeVavAcbModelDef() as SeventyFiveFProfileDirective
            DomainName.smartnodeBypassDamper -> ModelLoader.getSmartNodeBypassDamperModelDef() as SeventyFiveFProfileDirective
            DomainName.hyperstatSplitCPU -> ModelLoader.getHyperStatSplitCpuModel() as SeventyFiveFProfileDirective
            DomainName.hyperstatCPU -> ModelLoader.getHyperStatCpuModel() as SeventyFiveFProfileDirective
            DomainName.smartnodeOAO -> ModelLoader.getSmartNodeOAOModelDef() as SeventyFiveFProfileDirective
            DomainName.helionodeSSE -> ModelLoader.getHelioNodeSSEModel() as SeventyFiveFProfileDirective
            DomainName.smartnodeSSE -> ModelLoader.getSmartNodeSSEModel() as SeventyFiveFProfileDirective
            DomainName.hyperstatHPU -> ModelLoader.getHyperStatHpuModel() as SeventyFiveFProfileDirective
            DomainName.hyperstat2PFCU -> ModelLoader.getHyperStatPipe2Model() as SeventyFiveFProfileDirective
            DomainName.smartnodePID -> ModelLoader.getSmartNodePidModel() as SeventyFiveFProfileDirective
            DomainName.helionodePID -> ModelLoader.getHelioNodePidModel() as SeventyFiveFProfileDirective
            DomainName.hyperstatMonitoring -> ModelLoader.getHyperStatMonitoringModel() as SeventyFiveFProfileDirective
            else -> null
        }
    }

    /**
     * Overwrites a point's current value with its default value from the associated equipment model.
     *
     * This function:
     * 1. Retrieves the equipment reference for the point
     * 2. Gets the model ID from the equipment
     * 3. Fetches the model definition from cache
     * 4. Finds the matching point definition by domain name
     * 5. Writes the default value to the point via CCUHsApi
     *
     * @param point The target point to overwrite.
     *
     */
    fun overWriteDefaultValGeneric(point: Point) {
        val localPoint = hayStack.readMapById(point.equipRef)
        val modelId = localPoint.get("sourceModel")
        val model = ModelCache.getModelById(modelId.toString())
        val pointDef = model.points.find { it.domainName == point.domainName}
        val defaultval = pointDef?.defaultValue as Number
        CCUHsApi.getInstance().writeDefaultTunerValById(point.id,defaultval.toDouble())
        CcuLog.d(Domain.LOG_TAG, "Default value written for point: ${point.displayName} with value: $defaultval")
    }
}