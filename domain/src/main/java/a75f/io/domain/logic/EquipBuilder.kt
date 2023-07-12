package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.domain.api.Domain
import a75f.io.domain.config.EnableConfig
import a75f.io.domain.config.EntityConfiguration
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.config.containsConfig
import a75f.io.domain.config.getConfig
import a75f.io.domain.util.TagsUtil
import a75f.io.domain.util.TunerUtil
import io.seventyfivef.domainmodeler.client.ModelDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.ph.core.TagType
import org.projecthaystack.HBool
import org.projecthaystack.HStr

class EquipBuilder(private val hayStack : CCUHsApi) {

    /**
     * Creates a new haystack equip and all the points.
     * configuration - Updated profile configuration.
     * modelDef - Model instance for profile.
     */
    fun buildEquipAndPoints(configuration: ProfileConfiguration, modelDef: ModelDirective) : String{
        val entityMapper = EntityMapper(modelDef as SeventyFiveFProfileDirective)
        val entityConfiguration = entityMapper.getEntityConfiguration(configuration)

        val hayStackEquip = buildEquip(modelDef, configuration)
        val equipId = hayStack.addEquip(hayStackEquip)
        hayStackEquip.id = equipId
        DomainManager.addEquip(hayStackEquip)
        createPoints(modelDef, configuration, entityConfiguration, equipId)

        return equipId
    }

    /**
     * Updates and existing haystack equip and it points.
     * configuration - Updated profile configuration.
     * modelDef - Model instance for profile.
     */
    fun updateEquipAndPoints(configuration: ProfileConfiguration, modelDef: ModelDirective) : String{
        val entityMapper = EntityMapper(modelDef as SeventyFiveFProfileDirective)
        val entityConfiguration = ReconfigHandler
            .getEntityReconfiguration(configuration.nodeAddress, hayStack, entityMapper.getEntityConfiguration(configuration))

        val equip = hayStack.readEntity(
            "equip and group == \"${configuration.nodeAddress}\"")

        val equipId =  equip["id"].toString()
        val hayStackEquip = buildEquip(modelDef, configuration)
        hayStack.updateEquip(hayStackEquip, equipId)

        DomainManager.addEquip(hayStackEquip)
        createPoints(modelDef, configuration, entityConfiguration, equipId)
        updatePoints(modelDef, configuration, entityConfiguration, equipId)
        deletePoints(entityConfiguration, equipId)
        return equipId
    }

    fun buildEquip(modelDef: SeventyFiveFProfileDirective, profileConfiguration: ProfileConfiguration) : Equip{

        val equipBuilder = Equip.Builder().setDisplayName(modelDef.name)
            .setDomainName(modelDef.domainName)
            .setFloorRef(profileConfiguration.floorRef)
            .setGroup(profileConfiguration.nodeAddress.toString())

        if (profileConfiguration.roomRef != null) {
            equipBuilder.setRoomRef(profileConfiguration.roomRef)
        }

        modelDef.tags.filter { it.kind == TagType.MARKER }.forEach{ tag -> equipBuilder.addMarker(tag.name)}
        modelDef.tags.filter { it.kind == TagType.STR }.forEach{ tag ->
            tag.defaultValue?.let {
                equipBuilder.addTag(tag.name, HStr.make(tag.defaultValue.toString()))
            }
        }
        modelDef.tags.filter { it.kind == TagType.NUMBER }.forEach{ tag ->
            TagsUtil.getTagDefHVal(tag)?.let { equipBuilder.addTag(tag.name, it) }
        }
        modelDef.tags.filter { it.kind == TagType.BOOL }.forEach{ tag ->
            tag.defaultValue?.let {
                equipBuilder.addTag(tag.name, HBool.make(tag.defaultValue as Boolean))
            }
        }

        return equipBuilder.build()
    }

    private fun createPoints(modelDef: SeventyFiveFProfileDirective, profileConfiguration: ProfileConfiguration, entityConfiguration: EntityConfiguration, equipRef: String) {
        entityConfiguration.tobeAdded.forEach { point ->
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                val hayStackPoint = buildPoint(modelPointDef, profileConfiguration, equipRef)
                val pointId = hayStack.addPoint(hayStackPoint)
                hayStackPoint.id = pointId
                val enableConfig = profileConfiguration.getEnableConfigs().getConfig(point.domainName)
                if (enableConfig != null) {
                    initializeDefaultVal(hayStackPoint, enableConfig.enabled.toInt() )
                } else if (modelPointDef.tagNames.contains("writable") && modelPointDef.defaultValue is Number) {
                    initializeDefaultVal(hayStackPoint, modelPointDef.defaultValue as Number)
                }
                DomainManager.addPoint(hayStackPoint)
            }

        }
    }
    fun buildPoint(modelDef: SeventyFiveFProfilePointDef, configuration: ProfileConfiguration, equipRef : String) : Point{

    private fun updatePoints(modelDef: SeventyFiveFProfileDirective, profileConfiguration: ProfileConfiguration,
                             entityConfiguration: EntityConfiguration, equipRef: String) {
        entityConfiguration.tobeUpdated.forEach { point ->
            val existingPoint = hayStack.readEntity("domainName == \""+point.domainName+"\" and equipRef == \""+equipRef+"\"")
            val modelPointDef = modelDef.points.find { it.domainName == point.domainName }
            modelPointDef?.run {
                val hayStackPoint = buildPoint(modelPointDef, profileConfiguration, equipRef)
                hayStack.updatePoint(hayStackPoint, existingPoint["id"].toString())
                hayStackPoint.id = existingPoint["id"].toString()
                val enableConfig = profileConfiguration.getEnableConfigs().getConfig(point.domainName)
                if (enableConfig != null) {
                    initializeDefaultVal(hayStackPoint, enableConfig.enabled.toInt() )
                } else if (modelPointDef.tagNames.contains("writable") && modelPointDef.defaultValue is Number) {
                    initializeDefaultVal(hayStackPoint, modelPointDef.defaultValue as Number)
                }
                DomainManager.addPoint(hayStackPoint)
            }

        }
    }

    private fun deletePoints(entityConfiguration: EntityConfiguration, equipRef: String) {
        entityConfiguration.tobeDeleted.forEach { point ->
            val existingPoint = hayStack.readEntity("domainName == \""+point.domainName+"\" and equipRef == \""+equipRef+"\"")
            hayStack.deleteEntity(existingPoint["id"].toString())
        }
    }

    private fun buildPoint(modelDef: SeventyFiveFProfilePointDef, configuration: ProfileConfiguration, equipRef : String) : Point{

        //TODO - Ref validation, zone/system equip differentiator.
        val pointBuilder = Point.Builder().setDisplayName(modelDef.name)
            .setDomainName(modelDef.domainName)
            .setEquipRef(equipRef)
            .setFloorRef(configuration.floorRef)
            .setKind(Kind.parsePointType(modelDef.kind.name))
            .setUnit(modelDef.defaultUnit)
            .setGroup(configuration.nodeAddress.toString())

        if (configuration.roomRef != null) {
            pointBuilder.setRoomRef(configuration.roomRef)
        }

        //TODO - Support added for currently used tag types. Might need updates in future.
        modelDef.tags.filter { it.kind == TagType.MARKER }.forEach{ pointBuilder.addMarker(it.name)}
        modelDef.tags.filter { it.kind == TagType.NUMBER }.forEach{ tag ->
            TagsUtil.getTagDefHVal(tag)?.let { pointBuilder.addTag(tag.name, it) }
        }

        modelDef.tags.filter { it.kind == TagType.STR }.forEach{ tag ->
            tag.defaultValue?.let {
                pointBuilder.addTag(tag.name, HStr.make(tag.defaultValue.toString()))
            }
        }
        modelDef.tags.filter { it.kind == TagType.BOOL }.forEach{ tag ->
            tag.defaultValue?.let {
                pointBuilder.addTag(tag.name, HBool.make(tag.defaultValue as Boolean))
            }
        }

        return pointBuilder.build()
    }

    private fun initializeDefaultVal(point : Point, defaultVal : Number) {
        when{
            point.markers.contains("config") /*&& point.defaultVal is String*/-> hayStack.writeDefaultValById(point.id, defaultVal.toDouble())
            point.markers.contains("tuner") -> TunerUtil.updateTunerLevels(point.id, point.roomRef,  point.domainName, hayStack)
        }
    }

    fun getEquipDetailsByDomain(domainName: String): List<a75f.io.domain.api.Equip>{
        DomainManager.buildDomain(hayStack)
        val equips = mutableListOf<a75f.io.domain.api.Equip>()
        assert(Domain.site?.floors?.size  == 1)
        Domain.site?.floors?.entries?.forEach{
            val floor = it.value
            assert(floor.rooms.size == 1)
            floor.rooms.entries.forEach { r ->
                val room =  r.value
                room.equips.forEach { (equipDomainName, equip) ->
                    if (equip.domainName == domainName){
                        equips.add(equip)
                    }
                }
            }
        }
        return equips
    }

}