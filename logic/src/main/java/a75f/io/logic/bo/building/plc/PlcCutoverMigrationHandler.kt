package a75f.io.logic.bo.building.plc

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.cutover.NodeDeviceCutOverMapping
import a75f.io.domain.cutover.PlcCutOverMapping
import a75f.io.domain.cutover.getDomainNameFromDis
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.PointBuilderConfig
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective

fun doPlcDomainModelCutOverMigration(hayStack: CCUHsApi) {
    val plcEquips = hayStack.readAllEntities("equip and zone and pid")
        .filter { it["domainName"] == null }
        .toList()
    if (plcEquips.isEmpty()) {
        CcuLog.i(Domain.LOG_TAG, "PLC DM zone equip migration is complete")
        return
    }
    val equipBuilder = ProfileEquipBuilder(hayStack)
    val site = hayStack.site
    plcEquips.forEach {
        CcuLog.i(Domain.LOG_TAG, "Do DM zone equip migration for $it")

        val equipDis = "${site?.displayName}-PID-${it["group"]}"

        val isHelioNode = it.containsKey("helionode")
        val model = if (isHelioNode) ModelLoader.getHelioNodePidModel() else ModelLoader.getSmartNodePidModel()
        val deviceModel = if (isHelioNode) ModelLoader.getHelioNodeDevice() as SeventyFiveFDeviceDirective else ModelLoader.getSmartNodeDevice() as SeventyFiveFDeviceDirective
        val deviceDis = if (isHelioNode) "${site?.displayName}-HN-${it["group"]}" else "${site?.displayName}-SN-${it["group"]}"
        val deviceBuilder = DeviceBuilder(hayStack, EntityMapper(model as SeventyFiveFProfileDirective))
        val device = hayStack.readEntity("device and addr == \"" + it["group"] + "\"")
        val nativeSensorTypePoint =
            hayStack.readEntity("native and sensor and point and config and equipRef == \"" + it["id"] + "\"");
        val nativeSensorTypeValue =
            hayStack.readDefaultValById(nativeSensorTypePoint["id"].toString())
        val profileConfiguration = PlcProfileConfig(
            Integer.parseInt(it["group"].toString()),
            if (isHelioNode) NodeType.HELIO_NODE.name else NodeType.SMART_NODE.name,
            0,
            it["roomRef"].toString(),
            it["floorRef"].toString(),
            ProfileType.PLC,
            model
        ).getActiveConfiguration()

        doPlcMigration(it["id"].toString(), model, equipDis, profileConfiguration, it, equipBuilder, hayStack)

        deviceBuilder.doCutOverMigration(
            device["id"].toString(),
            deviceModel,
            deviceDis,
            NodeDeviceCutOverMapping.entries,
            profileConfiguration
        )

        val migratedConfiguration = PlcProfileConfig(
            Integer.parseInt(it["group"].toString()),
            if (isHelioNode) NodeType.HELIO_NODE.name else NodeType.SMART_NODE.name,
            0,
            it["roomRef"].toString(),
            it["floorRef"].toString(),
            ProfileType.PLC,
            model
        ).getActiveConfiguration()

        //Update custom configurations which are done outside of the model
        addBaseProfileConfig(DomainName.analog1InputType, migratedConfiguration, model)
        addBaseProfileConfig(DomainName.thermistor1InputType, migratedConfiguration, model)
        addBaseProfileConfig(DomainName.nativeSensorType, migratedConfiguration, model)
        addBaseProfileConfig(DomainName.useAnalogIn2ForSetpoint, migratedConfiguration, model)

        equipBuilder.updateEquipAndPoints(
            migratedConfiguration,
            model,
            it["siteRef"].toString(),
            it["dis"].toString(), true
        )

        CcuLog.i(
            Domain.LOG_TAG,
            "Node: ${it["group"]}, id : ${nativeSensorTypePoint["id"].toString()}, nativeSensorTypeValue  $nativeSensorTypeValue"
        )

        // If user is selected native sensor type as VOC, then we are updating the enum value to index - 1
        if (nativeSensorTypeValue >= 7)
            hayStack.writeDefaultValById(
                nativeSensorTypePoint["id"].toString(),
                nativeSensorTypeValue - 1
            )

        deviceBuilder.updateDeviceAndPoints(
            migratedConfiguration, deviceModel, it["id"].toString(), it["siteRef"].toString(),
            device[Tags.DIS].toString(), model
        )
    }
}

private fun doPlcMigration(equipRef: String, modelDef : SeventyFiveFProfileDirective, equipDis : String,
                            profileConfiguration: ProfileConfiguration, equipHashMap: HashMap<Any, Any> = HashMap(),
                            equipBuilder : ProfileEquipBuilder, hayStack: CCUHsApi) {

    CcuLog.i(Domain.LOG_TAG, "doCutOverMigration for $equipDis")
    val equipPoints =
        hayStack.readAllEntities("point and equipRef == \"$equipRef\"")
    val site = hayStack.site
    val mapping = PlcCutOverMapping.entries
    //TODO-To be removed after testing is complete.
    var update = 0
    var delete = 0
    equipPoints.filter { it["domainName"] == null}
        .forEach { dbPoint ->
            val modelPointName = getDomainNameForModelPoint(dbPoint, mapping)

            if (modelPointName == null) {
                delete++
                //DB point does not exist in model. Should be deleted.
                CcuLog.e(Domain.LOG_TAG, " Cut-Over migration : Redundant Point $dbPoint")
                hayStack.deleteEntityTree(dbPoint["id"].toString())
            } else {
                update++
                CcuLog.e(Domain.LOG_TAG, " Cut-Over migration Update with domainName $modelPointName : $dbPoint")
                //println("Cut-Over migration Update $dbPoint")
                val modelPoint = modelDef.points.find { it.domainName.equals(modelPointName, true)}
                if (modelPoint != null) {
                    equipBuilder.updatePoint(PointBuilderConfig(modelPoint, profileConfiguration, equipRef, site!!.id, site!!.tz, equipDis), dbPoint)
                } else {
                    delete++
                    CcuLog.e(Domain.LOG_TAG, " Model point does not exist for domain name $modelPointName")
                    //hayStack.deleteEntityTree(dbPoint["id"].toString())
                }
            }
        }

    CcuLog.i(
        Domain.LOG_TAG, "CutOver migration for ${modelDef.domainName} Total points DB: ${equipPoints.size} " +
                " Model: ${modelDef.points.size} Map: ${mapping.size} ")
    CcuLog.i(Domain.LOG_TAG, " Deleted $delete Updated $update")
    equipBuilder.updateEquip(equipRef, modelDef, equipDis, false, site!!.id, profileConfiguration, equipHashMap)
    CcuLog.i(Domain.LOG_TAG, " Cut-Over migration completed for Equip $equipHashMap")

}
fun getDomainNameForModelPoint(point : Map<Any, Any>, mapping : Map <String, String>) : String? {
    val displayNme = point["dis"].toString()
    if (displayNme.contains("processVariable")) {
        return DomainName.processVariable
    } else if (displayNme.contains("pidTargetValue")) {
        return DomainName.pidTargetValue
    } else if (displayNme.contains("dynamicTargetValue")) {
        return DomainName.dynamicTargetValue
    }
    return mapping.filterKeys { displayNme.replace("\\s".toRegex(),"").substringAfterLast("-") == it }
        .map { it.value }
        .firstOrNull()
}