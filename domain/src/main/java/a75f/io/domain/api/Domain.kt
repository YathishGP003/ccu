package a75f.io.domain.api

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.BuildingEquip
import a75f.io.domain.DomainEquip
import a75f.io.domain.VavEquip
import a75f.io.domain.logic.DomainManager
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import io.seventyfivef.domainmodeler.common.point.NumericConstraint
import android.annotation.SuppressLint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.projecthaystack.HDict

@SuppressLint("StaticFieldLeak")
object Domain {

    const val LOG_TAG = "CCU_DOMAIN"
    const val LOG_TAG_TUNER = "CCU_DOMAIN_TUNER"

    val domainScope = CoroutineScope(Dispatchers.IO + Job())
    val hayStack: CCUHsApi = CCUHsApi.getInstance()
    var site: Site? = null
    lateinit var buildingEquip : BuildingEquip
    var equips = mutableMapOf<String, DomainEquip>()
    /**
     * Retrieve the domain object of a point by it id and equipRef.
     */
    fun getEquipPoint(pointId : String, parentRef : String) : Point? {
        site?.floors?.entries?.forEach{
            val floor = it.value
            floor.rooms.entries.forEach { room ->
                val equip = room.value.equips.values.find { entry ->
                    entry.id == parentRef
                }
                val point = equip?.points?.values?.find { p ->
                    p.id == pointId
                }
                if (point != null) {
                    return point
                }
            }
        }
        return null
    }

    /**
     * Retrieve the domain object of a point by it id and deviceRef.
     */
    fun getDevicePoint(pointId : String, deviceRef : String) : Point? {
        site?.floors?.entries?.forEach{
            val floor = it.value
            floor.rooms.entries.forEach { room ->
                val device = room.value.devices.values.find { entry ->
                    entry.id == deviceRef
                }
                val point = device?.points?.values?.find { p ->
                    p.id == pointId
                }
                if (point != null) {
                    return point
                }
            }
        }
        return null
    }


    fun getEquipDetailsByDomain(domainName: String): List<Equip> {
        DomainManager.buildDomain(CCUHsApi.getInstance())
        val equips = mutableListOf<Equip>()
        site?.floors?.entries?.forEach{
            val floor = it.value
            floor.rooms.entries.forEach { r ->
                val room =  r.value
                room.equips.forEach { (_, equip) ->
                    if (equip.domainName == domainName){
                        equips.add(equip)
                    }
                }
            }
        }
        return equips
    }

    fun getSystemEquipByDomainName(domainName: String): Equip? {
        DomainManager.buildDomain(CCUHsApi.getInstance())
        site?.ccus?.entries?.forEach {
            it.value.equips.forEach { (_,equip)->
                if (equip.domainName == domainName){
                    return equip
                }
            }
        }
        return null
    }


    fun getPointByDomain(equip: Equip, domainName: String): Double {
        val point = equip.points.entries.find { it.key.contentEquals(domainName) }?.value
        point?.let { return point.readDefaultVal() }
        return 0.0
    }

    fun getPointPriorityValByDomain(equip: Equip, domainName: String): Double {
        val point = equip.points.entries.find { it.key.contentEquals(domainName) }?.value
        point?.let { return point.readPriorityVal() }
        return 0.0
    }

    fun getHisByDomain(equip: Equip, domainName: String): Double {
        val point = equip.points.entries.find { it.key.contentEquals(domainName) }?.value
        point?.let { return point.readHisVal() }
        return 0.0
    }

    private fun getIdByDomain(equip: Equip, domainName: String): String? {
        val point = equip.points.entries.find { it.key.contentEquals(domainName) }?.value
        return point?.id
    }

    fun writePointByDomain(equip: Equip, domainName: String, value: Any) {
        val point = equip.points.entries.find { it.key.contentEquals(domainName) }?.value
        point?.let {
            it.writeDefaultVal(value)
            if (value is Double) {
                it.writeHisVal(value)
            }
        }
    }

    @JvmStatic
    fun readPoint(domainName: String) : Map<Any,Any> {
        return hayStack.readEntity("point and domainName == \"$domainName\"")
    }

    @JvmStatic
    fun readDict(domainName: String) : HDict {
        return hayStack.readHDict("point and domainName == \"$domainName\"")
    }

    @JvmStatic
    fun readDictOnEquip(domainName: String, equipRef: String) : HDict {
        return hayStack.readHDict("point and domainName == \"$domainName\" and equipRef == \"$equipRef\"")
    }

    @JvmStatic
    fun readPointForEquip(domainName: String, equipRef : String) : Map<Any,Any> {
        return hayStack.readEntity("point and domainName == \"$domainName\" and equipRef == \"$equipRef\"")
    }

    /*
        For Numeric points, this method re-loads some values from the model (minVal, maxVal, incrementVal)
        that are already present in the ValueConfig for the point.

        This is because for Enum points, the enum range is only present in the PointDef. So the Numeric points
        pull their range directly from the PointDef as well for consistency.

        There is a performance penalty, but this design is cleaner and (in my opinion) is what is called for in ADR-0043.
    */
    fun getListByDomainName(domainName: String, model: SeventyFiveFProfileDirective) : List<String> {
        val valuesList : MutableList<String> = mutableListOf()
        val point = model.points.find { it.domainName == domainName }

        if (point?.valueConstraint is MultiStateConstraint) {

            (point?.valueConstraint as MultiStateConstraint).allowedValues.forEach{ state ->
                valuesList.add(state.value)
            }

        } else if (point?.valueConstraint is NumericConstraint) {

            val minVal = (point?.valueConstraint as NumericConstraint).minValue
            val maxVal = (point?.valueConstraint as NumericConstraint).maxValue
            val incVal = point?.presentationData?.get("tagValueIncrement").toString().toDouble()

            var it = minVal
            while (it <= maxVal && incVal > 0.0) {
                valuesList.add(getStringFormat(it, incVal))
                it += incVal
            }

        }
        return valuesList
    }

    private fun getStringFormat(itVal: Double, incVal: Double): String {
        var decimalPlaces = 0
        var i : Double = incVal
        while (i < 1) {
            i *= 10
            decimalPlaces += 1
        }
        val formattedString = ("%." + decimalPlaces.toString() + "f").format(itVal)
        return if (formattedString.toDouble() != 0.0) formattedString else ("%." + decimalPlaces.toString() + "f").format(0.0)
    }

    fun getDomainEquip(equipId : String) : DomainEquip? {
        return equips[equipId]
    }
    fun readPointValueByDomainName(domainName: String, equipRef : String): Double {
        return hayStack.readDefaultVal("point and domainName == \"$domainName\" and equipRef == \"$equipRef\"")
    }
    fun readStrPointValueByDomainName(domainName: String, equipRef : String): String {
        return hayStack.readDefaultStrVal("point and domainName == \"$domainName\" and equipRef == \"$equipRef\"")
    }
    @JvmStatic
    fun readDefaultValByDomain(domainName: String): Double {
        return hayStack.readDefaultVal("point and domainName == \"$domainName\"")
    }
    @JvmStatic
    fun writeDefaultValByDomain(domainName: String, value: Double) {
        return hayStack.writeDefaultVal("point and domainName == \"$domainName\"", value)
    }
    @JvmStatic
    fun writeHisValByDomain(domainName: String, value: Double) {
        return hayStack.writeHisValByQuery("point and domainName == \"$domainName\"", value)
    }

    @JvmStatic
    fun writeHisValByDomain(domainName: String, value: Double, equipRef: String) {
        return hayStack.writeHisValByQuery("point and domainName == \"$domainName\" and equipRef == \"$equipRef\"", value)
    }

    @JvmStatic
    fun writeDefaultValByDomain(domainName: String, value: String, equipRef: String) {
        return hayStack.writeDefaultVal("point and domainName == \"$domainName\" and equipRef == \"$equipRef\"", value)
    }

    fun readEquip(modelId: String) : Map<Any,Any> {
        return hayStack.readEntity("equip and sourceModel==\"$modelId\" or modelId == \"$modelId\"")
    }
}