package a75f.io.logic.preconfig

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Floor
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Site
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.Zone
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.logic.CCUBaseConfigurationBuilder
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.DiagEquipConfigurationBuilder
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.EntityMapper
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.util.ModelLoader
import a75f.io.domain.util.ModelLoader.getCCUBaseConfigurationModel
import a75f.io.logger.CcuLog
import a75f.io.logic.DefaultSchedules
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.bypassdamper.BypassDamperProfile
import a75f.io.logic.bo.building.bypassdamper.BypassDamperProfileConfiguration
import a75f.io.logic.bo.building.dab.DabProfile
import a75f.io.logic.bo.building.dab.DabProfileConfiguration
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.definitions.ScheduleType
import a75f.io.logic.bo.building.hvac.Stage
import a75f.io.logic.bo.building.system.dab.DabStagedRtuWithVfd
import a75f.io.logic.bo.building.system.vav.config.StagedRtuProfileConfig
import a75f.io.logic.bo.building.system.vav.config.StagedVfdRtuProfileConfig
import a75f.io.logic.limits.SchedulabeLimits.Companion.addSchedulableLimits
import a75f.io.logic.util.TimeZoneUtil.getTimeZoneIdFromString
import a75f.io.logic.util.bacnet.addBacnetTags
import a75f.io.logic.util.bacnet.generateBacnetIdForRoom
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import org.projecthaystack.HRef

fun createSite (
    siteName: String,
    organisationName: String,
    timeZone: String,
    streetAddress: String,
    city: String,
    state: String,
    country: String,
    zipCode: String,
    fmEmail: String,
    installerEmail: String,
    ccuHsApi: CCUHsApi
): String {

    val tzID = getTimeZoneIdFromString(timeZone)
    val s75f = Site.Builder()
        .setDisplayName(siteName)
        .addMarker("site")
        .setGeoCity(city)
        .setGeoState(state)
        .setTz(tzID.substring(tzID.lastIndexOf("/") + 1))
        .setGeoZip(zipCode)
        .setGeoCountry(country)
        .setOrgnization(organisationName)
        .setInstaller(installerEmail)
        .setFcManager(fmEmail)
        .setGeoAddress(streetAddress)
        .setGeoFence("2.0")
        .setArea(10000).build()

    return ccuHsApi.addSite(s75f)
}

fun createCcuDevice(
    ccuName: String,
    siteId: String,
    installerEmail: String,
    facilityManagerEmail: String,
    ccuHsApi: CCUHsApi
): String {

    val diagEquipConfigurationBuilder = DiagEquipConfigurationBuilder(CCUHsApi.getInstance())
    val ccuBaseConfigurationBuilder = CCUBaseConfigurationBuilder(CCUHsApi.getInstance())

    val ccuBaseConfigurationModel = getCCUBaseConfigurationModel()
    val diagEquipId = diagEquipConfigurationBuilder.createDiagEquipAndPoints(ccuName)

    val ccuRef = ccuBaseConfigurationBuilder.createCCUBaseConfiguration(
        ccuName,
        installerEmail, facilityManagerEmail, diagEquipId, ccuBaseConfigurationModel
    )
    Domain.ccuEquip.addressBand.writeDefaultVal("1000") // Default value`for address band
    L.ccu().addressBand = "1000".toShort()

    //L.ccu().ccuName = ccuName
    ccuHsApi.addOrUpdateConfigProperty(HayStackConstants.CUR_CCU, HRef.make(ccuRef))
    return ccuRef;
}

fun createFloor(
    floorName: String,
    siteId: String,
    ccuHsApi: CCUHsApi
): String {
    val floor = Floor.Builder()
        .setDisplayName(floorName.trim())
        .setSiteRef(siteId)
        .build()

    return ccuHsApi.addFloor(floor)
}

fun createZone(
    zoneName: String,
    floorId: String,
    siteId: String,
    ccuHsApi: CCUHsApi
): String {
    val zone = Zone.Builder()
        .setDisplayName(zoneName.trim())
        .setFloorRef(floorId)
        .setSiteRef(siteId)
        .build()
    zone.id = ccuHsApi.addZone(zone)
    doZoneConfig(zone)
    return zone.id
}

fun createSystemEquip(
    enabledStages: List<String>,
    hayStack: CCUHsApi
): String {
    val equipModel = ModelLoader.getDabStagedVfdRtuModelDef() as SeventyFiveFProfileDirective
    val profileConfiguration = StagedVfdRtuProfileConfig(equipModel).getDefaultConfiguration()

    CcuLog.i("CCU_PRECONFIGURATION", "applyStagesToConfig")
    applyStagesToConfig(
        profileConfiguration,
        enabledStages
    )
    CcuLog.i("CCU_PRECONFIGURATION", "applyStagesToConfig Done")
    val deviceModel = ModelLoader.getCMDeviceModel() as SeventyFiveFDeviceDirective

    val equipBuilder = ProfileEquipBuilder(hayStack)
    val entityMapper = EntityMapper(equipModel)
    val deviceBuilder = DeviceBuilder(hayStack, entityMapper)

    val equipDis = "${hayStack.siteName}-${equipModel.name}"
    val equipId = equipBuilder.buildEquipAndPoints(profileConfiguration, equipModel, hayStack.site!!.id, equipDis)

    val deviceDis = hayStack.siteName +"-"+ deviceModel.name
    CcuLog.i("CCU_PRECONFIGURATION", "buildDeviceAndPoints")
    deviceBuilder.buildDeviceAndPoints(
        profileConfiguration,
        deviceModel,
        equipId,
        hayStack.site!!.id,
        deviceDis
    )

    CcuLog.i("CCU_PRECONFIGURATION", "addSystemDomainEquip")
    DomainManager.addSystemDomainEquip(hayStack)
    DomainManager.addCmBoardDevice(hayStack)
    L.ccu().systemProfile = DabStagedRtuWithVfd()
    L.ccu().systemProfile.addSystemEquip()
    L.ccu().systemProfile.updateAhuRef(equipId)
    return equipId
}

private fun applyStagesToConfig(
    profileConfiguration: StagedRtuProfileConfig,
    enabledStages: List<String>
) {
    enabledStages.filterNotNull().forEach { stage ->
        CcuLog.i("CCU_PRECONFIGURATION", "applyStagesToConfig $stage")
        when (stage) {
            DomainName.coolingStage1 ->  {
                profileConfiguration.relay1Enabled.enabled = true
                profileConfiguration.relay1Association.associationVal = Stage.COOLING_1.ordinal
            }
            DomainName.coolingStage2 -> {
                profileConfiguration.relay2Enabled.enabled = true
                profileConfiguration.relay2Association.associationVal = Stage.COOLING_2.ordinal
            }

            DomainName.fanStage1 -> {
                profileConfiguration.relay3Enabled.enabled = true
                profileConfiguration.relay3Association.associationVal = Stage.FAN_1.ordinal
            }

            DomainName.fanStage2 -> {
                profileConfiguration.relay6Enabled.enabled = true
                profileConfiguration.relay6Association.associationVal = Stage.FAN_2.ordinal
            }

            DomainName.heatingStage1 -> {
                profileConfiguration.relay4Enabled.enabled = true
                profileConfiguration.relay4Association.associationVal = Stage.HEATING_1.ordinal
            }

            DomainName.heatingStage2 -> {
                profileConfiguration.relay5Enabled.enabled = true
                profileConfiguration.relay5Association.associationVal = Stage.HEATING_2.ordinal
            }

            else -> {
                CcuLog.i("CCU_PRECONFIGURATION", "applyStagesToConfig $stage not supported")
            }
        }
    }
}
fun createTerminalEquip(
    equipDomainName: String,
    floorId: String,
    roomId: String,
    deviceAddress: Int,
    nodeType: NodeType,
    hayStack: CCUHsApi
): String {

    if (equipDomainName != "dabSmartNode" && equipDomainName != "dabHelioNode") {
        throw UnsupportedPreconfigurationException("Unsupported system equip domain name: $equipDomainName")
    }

    val equipModel = ModelLoader.getSmartNodeDabModel() as SeventyFiveFProfileDirective
    val deviceModel = ModelLoader.getSmartNodeDevice() as SeventyFiveFDeviceDirective

    val profileConfiguration = DabProfileConfiguration(
        deviceAddress, nodeType.name, 0,
        roomId, floorId, ProfileType.DAB, equipModel
    ).getDefaultConfiguration()

    val equipBuilder = ProfileEquipBuilder(hayStack)
    val equipDis = hayStack.siteName + "-VVT-C-"+deviceAddress
    val equipId = equipBuilder.buildEquipAndPoints(
        profileConfiguration, equipModel, hayStack.site!!
            .id, equipDis
    )
    val deviceName = when (nodeType) {
        NodeType.HELIO_NODE -> "-HN-"
        else -> "-SN-"
    }
    val entityMapper = EntityMapper(equipModel)
    val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
    val deviceDis = hayStack.siteName + deviceName + deviceAddress
    CcuLog.i(Domain.LOG_TAG, " buildDeviceAndPoints")
    deviceBuilder.buildDeviceAndPoints(
        profileConfiguration,
        deviceModel,
        equipId,
        hayStack.site!!.id,
        deviceDis
    )
    L.ccu().zoneProfiles.add(DabProfile(deviceAddress.toShort()))
    return equipId
}

fun createBypassDamperEquip(deviceAddress: Int, floorRef: String, zoneRef: String, nodeType: NodeType, hayStack: CCUHsApi) {

    CcuLog.i(L.TAG_PRECONFIGURATION, "createBypassDamperEquip")
    val model = ModelLoader.getSmartNodeBypassDamperModelDef() as SeventyFiveFProfileDirective
    val deviceModel = ModelLoader.getSmartNodeDevice() as SeventyFiveFDeviceDirective
    val profileConfiguration = BypassDamperProfileConfiguration(deviceAddress, nodeType.name, 0,
        zoneRef, floorRef , ProfileType.BYPASS_DAMPER, model ).getDefaultConfiguration()

    val equipBuilder = ProfileEquipBuilder(hayStack)
    val equipDis = hayStack.siteName + "-BYPASS-" + profileConfiguration.nodeAddress

    val equipId = equipBuilder.buildEquipAndPoints(
        profileConfiguration, model, hayStack.site!!.id, equipDis
    )

    val entityMapper = EntityMapper(model)
    val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
    val deviceName = when(nodeType) { NodeType.HELIO_NODE -> "-HN-" else -> "-SN-"}
    val deviceDis = hayStack.siteName + deviceName + profileConfiguration.nodeAddress
    CcuLog.i(L.TAG_PRECONFIGURATION, " buildDeviceAndPoints")
    deviceBuilder.buildDeviceAndPoints(
        profileConfiguration,
        deviceModel,
        equipId,
        hayStack.site!!.id,
        deviceDis
    )
    CcuLog.i(L.TAG_PRECONFIGURATION, " add Profile")
    overrideTunersForBypassDamper(hayStack)
    setPressureSensorRef(profileConfiguration, hayStack)
    setOutputTypes(profileConfiguration, hayStack)
    setDamperFeedback(profileConfiguration, hayStack)
    L.ccu().bypassDamperProfile = BypassDamperProfile(equipId, deviceAddress.toShort())
    CcuLog.i(L.TAG_PRECONFIGURATION, " Created Bypass Damper Equip with ID: $equipId")
}

fun doZoneConfig(hsZone: Zone ) {
    addSchedulableLimits(false, hsZone.id, hsZone.displayName)
    hsZone.setId(hsZone.getId())
    DefaultSchedules.setDefaultCoolingHeatingTemp()
    val zoneSchedule = DefaultSchedules.generateDefaultSchedule(true, hsZone.id)
    hsZone.scheduleRef = zoneSchedule

    val bacnetId = generateBacnetIdForRoom(hsZone.getId())
    hsZone.setBacnetId(bacnetId)
    hsZone.setBacnetType(Tags.DEVICE)
    CCUHsApi.getInstance().updateZone(hsZone, hsZone.id)
    addBacnetTags(null, hsZone.getFloorRef(), hsZone.getId())
    CcuLog.i(L.TAG_PRECONFIGURATION, "Completed schedule configuration for zone ${hsZone.displayName}")
}

fun updateScheduleTypeToZone(ccuHsApi: CCUHsApi) {
    CcuLog.i(L.TAG_PRECONFIGURATION, "Updating schedule type to zone")
    val scheduleTypes = ccuHsApi.readAllEntities("point and domainName == \"" + DomainName.scheduleType + "\"")
    scheduleTypes.mapNotNull { it["id"] }
            .map { it.toString() }
            .forEach {
                ccuHsApi.writePointForCcuUser(
                    it,
                    8,
                    ScheduleType.ZONE.ordinal.toDouble(),
                    0,
                    "Schedule Type Updated"
                )
                ccuHsApi.writeHisValById(it, ScheduleType.ZONE.ordinal.toDouble())
            }
}

