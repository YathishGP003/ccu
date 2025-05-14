package a75f.io.logic.preconfig

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Queries
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.schedule.BuildingOccupancy
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.system.dab.DabStagedRtuWithVfd
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.tuners.TunerEquip.initialize
import org.projecthaystack.client.HClient

class PreconfigurationHandler {
    @Throws(UnsupportedTimeZoneException::class, InvalidStagesException::class)
    fun handlePreconfiguration(preconfigData : PreconfigurationData, ccuHsApi: CCUHsApi) : String {

        CcuLog.i(L.TAG_PRECONFIGURATION,"Handling preconfiguration data: $preconfigData")

        //validatePreconfigData(preconfigData)


        val siteId = createSite(
            preconfigData.siteName,
            preconfigData.orgName,
            preconfigData.timeZone,
            preconfigData.siteAddress.geoAddr,
            preconfigData.siteAddress.geoCity,
            preconfigData.siteAddress.geoState,
            preconfigData.siteAddress.geoCountry,
            preconfigData.siteAddress.geoPostalCode,
            preconfigData.fmEmail,
            preconfigData.installerEmail,
            ccuHsApi
        )
        CcuLog.i(L.TAG_PRECONFIGURATION,"Created Site with ID: $siteId")

        val ccuId = createCcuDevice(
            preconfigData.ccuName,
            siteId,
            preconfigData.installerEmail,
            preconfigData.fmEmail,
            ccuHsApi
        )
        CcuLog.i(L.TAG_PRECONFIGURATION,"Created CCU with ID: $ccuId")

        initialize(ccuHsApi, false)
        CcuLog.i(L.TAG_PRECONFIGURATION,"Initialised Building Equipment")
        if (ccuHsApi.readEntity(Queries.BUILDING_OCCUPANCY).isEmpty()) {
            BuildingOccupancy.buildDefaultBuildingOccupancy()
        }

        CcuLog.i(L.TAG_PRECONFIGURATION,"Building Occupancy created")
        ccuHsApi.importNamedScheduleWithOrg(
            HClient(ccuHsApi.hsUrl, HayStackConstants.USER, HayStackConstants.PASS),
            preconfigData.orgName
        )

        val systemEquipId = createSystemEquip(
            preconfigData.relayMappingSet,
            ccuHsApi
        )
        CcuLog.i(L.TAG_PRECONFIGURATION,"Created System Equip with ID: $systemEquipId")

        createBypassDamperEquip(
            L.generateSmartNodeAddress().toInt(),
            "SYSTEM",
            "SYSTEM",
            NodeType.SMART_NODE,
            ccuHsApi
        )

        val floorName = preconfigData.floor
        if (floorName.trim().isNotBlank()) {
            val floorId = createFloor(
                floorName,
                siteId,
                ccuHsApi
            )
            CcuLog.i(L.TAG_PRECONFIGURATION, "Created Floor with ID: $floorId")

            for (zone in preconfigData.zones) {
                val zoneId = createZone(
                    zone,
                    floorId,
                    siteId,
                    ccuHsApi
                )
                CcuLog.i(L.TAG_PRECONFIGURATION, "Created Zone with ID: $zoneId")
                val equipId = createTerminalEquip(
                    "dabSmartNode", //TODO
                    floorId,
                    zoneId,
                    L.generateSmartNodeAddress().toInt(),
                    NodeType.SMART_NODE,
                    ccuHsApi
                )
                CcuLog.i(L.TAG_PRECONFIGURATION, "Created Terminal Equip with ID: $equipId")

                DesiredTempDisplayMode.setModeType(zoneId, ccuHsApi)
            }
        }
        else
        {
            CcuLog.d(L.TAG_PRECONFIGURATION, "Floor name is empty, skipping floor and zone  creation")

        }

        L.ccu().systemProfile = DabStagedRtuWithVfd()
        L.ccu().systemProfile.removeSystemEquipModbus()
        L.ccu().systemProfile.addSystemEquip()
        
        updateMigrationVersionPoint(ccuHsApi)
        CcuLog.i(L.TAG_PRECONFIGURATION,"Imported Named Schedule with Org")
        ccuHsApi.syncEntityTree()
        CcuLog.i(L.TAG_PRECONFIGURATION,"Synced Entity with Point Write")
        return siteId
    }

    fun cleanUpPreconfigurationData(ccuHsApi: CCUHsApi) {
        CcuLog.i(L.TAG_PRECONFIGURATION,"Cleaning up preconfiguration data")

        ccuHsApi.readAllEntities("point")
            .forEach {ccuHsApi.deleteEntity(it.get("id").toString())}
        ccuHsApi.readAllEntities("equip")
            .forEach {ccuHsApi.deleteEntity(it.get("id").toString())}
        ccuHsApi.readAllEntities("device")
            .forEach {ccuHsApi.deleteEntity(it.get("id").toString())}
        ccuHsApi.readAllEntities("zone")
            .forEach {ccuHsApi.deleteEntity(it.get("id").toString())}
        ccuHsApi.readAllEntities("floor")
            .forEach {ccuHsApi.deleteEntity(it.get("id").toString())}
        ccuHsApi.deleteEntity(Tags.SITE)
    }

    @Throws(InvalidPreconfigurationDataException::class)
    fun validatePreconfigurationData(preconfigData: PreconfigurationData) {
        with(preconfigData) {
            val validations = listOf(
                siteName to "Site name",
                orgName to "Organization name",
                timeZone to "Time zone",
                siteAddress.geoAddr to "Site address",
                siteAddress.geoCity to "Site city",
                siteAddress.geoState to "Site state",
                siteAddress.geoCountry to "Site country",
                siteAddress.geoPostalCode to "Site postal code",
                fmEmail to "FM email",
                installerEmail to "Installer email",
                ccuName to "CCU name"
            )

            validations.forEach { (value, name) ->
                if (value.isNullOrBlank()) {
                    throw InvalidPreconfigurationDataException("$name cannot be empty")
                }
            }
        }
    }
}