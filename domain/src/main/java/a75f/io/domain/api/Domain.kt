package a75f.io.domain.api

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.CCUTagsDb
import a75f.io.domain.config.ProfileConfiguration
import a75f.io.domain.devices.CCUDevice
import a75f.io.domain.devices.CmBoardDevice
import a75f.io.domain.devices.ConnectDevice
import a75f.io.domain.devices.DomainDevice
import a75f.io.domain.equips.BuildingEquip
import a75f.io.domain.equips.CCUDiagEquip
import a75f.io.domain.equips.CCUEquip
import a75f.io.domain.equips.DomainEquip
import a75f.io.domain.logic.DeviceBuilder
import a75f.io.domain.logic.DomainManager
import a75f.io.domain.logic.PointBuilderConfig
import a75f.io.domain.logic.ProfileEquipBuilder
import a75f.io.domain.logic.DomainManager.addDomainEquips
import a75f.io.domain.logic.EntityMapper
import a75f.io.logger.CcuLog
import android.annotation.SuppressLint
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFDeviceDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import io.seventyfivef.domainmodeler.common.point.NumericConstraint
import io.seventyfivef.ph.core.Tags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import org.projecthaystack.HDict
import org.projecthaystack.HNum
import org.projecthaystack.HRef

@SuppressLint("StaticFieldLeak")
object Domain {

    const val LOG_TAG = "CCU_DOMAIN"
    const val LOG_TAG_TUNER = "CCU_DOMAIN_TUNER"

    val domainScope = CoroutineScope(Dispatchers.IO + Job())
    var hayStack: CCUHsApi = CCUHsApi.getInstance()
    var site: Site? = null
    lateinit var buildingEquip : BuildingEquip
    lateinit var systemEquip : DomainEquip
    var equips = mutableMapOf<String, DomainEquip>()
    lateinit var cmBoardDevice: CmBoardDevice
    lateinit var connect1Device: ConnectDevice //This would be preset only when advanced ahu is configured
    lateinit var diagEquip: CCUDiagEquip
    lateinit var ccuDevice : CCUDevice // This is physical entity, this will be deleted and added when ccu is registered and unregistered
    lateinit var ccuEquip: CCUEquip

    val devices = mutableMapOf<String, DomainDevice>()

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

    fun getDeviceEntityByDomain(domainName: String, newModelVersion: String) : List<Device> {
        val deviceEntities = hayStack.readAllEntities("device and domainName == \"$domainName\"  and sourceModelVersion!=\"$newModelVersion\"")
        val deviceEntityList = mutableListOf<Device>()
        deviceEntities.forEach { device ->
            domainName.let {
                val deviceEntity = Device(domainName, device["id"].toString())
                deviceEntityList.add(deviceEntity)
            }
        }
        return deviceEntityList
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

    fun getBypassEquipByDomainName(domainName: String): Equip? {
        DomainManager.buildDomain(CCUHsApi.getInstance())
        site?.ccus?.entries?.forEach {
            it.value.bypassEquips.forEach { (_,equip)->
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
    fun getEquip(equipRef: String): DomainEquip? {
        return equips[equipRef]
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
    fun readEquipDict(domainName: String) : HDict {
        return hayStack.readHDict("equip and domainName == \"$domainName\"")
    }


    @JvmStatic
    fun readDictOnEquip(domainName: String, equipRef: String) : HDict {
        return hayStack.readHDict("point and domainName == \"$domainName\" and equipRef == \"$equipRef\"")
    }

    @JvmStatic
    fun readDictNullableOnEquip(domainName: String, equipRef: String) : HDict? {
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

            (point.valueConstraint as MultiStateConstraint).allowedValues.forEach{ state ->
                valuesList.add(state.value)
            }

        } else if (point?.valueConstraint is NumericConstraint) {

            val minVal = (point.valueConstraint as NumericConstraint).minValue
            val maxVal = (point.valueConstraint as NumericConstraint).maxValue
            val incVal = point.presentationData?.get("tagValueIncrement").toString().toDouble()

            var it = minVal
            while (it <= maxVal && incVal > 0.0) {
                valuesList.add(getStringFormat(it, incVal))
                it += incVal
            }

        }
        return valuesList
    }

    /**
     * Retrieves the minimum value, maximum value, and increment value for a given domain name
     * from the specified model.
     *
     * This function searches for the point in the model that matches the provided domain name.
     * It determines whether the point's value constraint is numeric or multi-state and extracts
     * the appropriate minimum, maximum, and increment values accordingly.
     *
     * - For numeric constraints: Uses `minValue`, `maxValue`, and `tagValueIncrement` from the point.
     * - For multi-state constraints: Sets the minimum value to 0, the maximum value to the size of
     *   the allowed values, and the increment value to 1.
     * - If no matching point or constraint type is found, it returns default values of 0.0, 0.0, and 0.0.
     *
     * @param domainName The domain name used to find the point in the model.
     * @param model The profile directive model containing the points to search.
     * @return A Triple containing the minimum value, maximum value, and increment value.
     */
    fun getMinMaxIncValuesByDomainName(domainName: String, model: SeventyFiveFProfileDirective) : Triple<Double, Double, Double> {
        val point = model.points.find { it.domainName == domainName }

        if (point?.valueConstraint is NumericConstraint) {
            val minVal = (point.valueConstraint as NumericConstraint).minValue
            val maxVal = (point.valueConstraint as NumericConstraint).maxValue
            val incVal = point.presentationData?.get("tagValueIncrement").toString().toDouble()
            return Triple(minVal, maxVal, incVal)
        } else if (point?.valueConstraint is MultiStateConstraint) {
            val minVal = 0.0 // Consider enum first value as zero
            val maxVal = (point.valueConstraint as MultiStateConstraint).allowedValues.size.toDouble() - 1 //  // Maximum valid index - (allowedValues list size - 1)
            val incVal = 1.0
            return Triple(minVal, maxVal, incVal)
        }
        return Triple(0.0, 0.0, 0.0)
    }

    fun getListByDomainNameWithCustomMaxVal(domainName: String, model: SeventyFiveFProfileDirective, maxVal: Double) : List<String> {
        val valuesList : MutableList<String> = mutableListOf()
        val point = model.points.find { it.domainName == domainName }

        if (point?.valueConstraint is MultiStateConstraint) {

            (point.valueConstraint as MultiStateConstraint).allowedValues.forEach{ state ->
                valuesList.add(state.value)
            }

        } else if (point?.valueConstraint is NumericConstraint) {

            val effectiveMaxVal = if (maxVal > 0.0) maxVal else (point.valueConstraint as NumericConstraint).maxValue
            val minVal = (point.valueConstraint as NumericConstraint).minValue
            val incVal = point.presentationData?.get("tagValueIncrement").toString().toDouble()

            var it = minVal
            while (it <= effectiveMaxVal && incVal > 0.0) {
                valuesList.add(getStringFormat(it, incVal))
                it += incVal
            }

        }
        return valuesList
    }

    fun getStringFormat(itVal: Double, incVal: Double): String {
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
        if (equipId.isEmpty()) {
            addDomainEquips(hayStack)
        }
        return equips[equipId]
    }

    fun readStrPointValueByDomainName(domainName: String, equipRef : String): String {
        return hayStack.readDefaultStrVal("point and domainName == \"$domainName\" and equipRef == \"$equipRef\"")
    }
    fun readStrPointValueByDomainName(domainName: String): String {
        return hayStack.readDefaultStrVal("point and domainName == \"$domainName\"")
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
    @JvmStatic
    fun writeDefaultValByDomain(domainName: String, value: String) {
        return hayStack.writeDefaultVal("point and domainName == \"$domainName\"", value)
    }
    @JvmStatic
    fun readHisValByDomain(domainName: String) : Double {
        return hayStack.readHisValByQuery("point and domainName == \"$domainName\"")
    }
    fun readEquip(modelId: String) : Map<Any,Any> {
        return hayStack.readEntity("equip and sourceModel==\"$modelId\" or modelId == \"$modelId\"")
    }
    fun readPointById(id : String) : HashMap<Any,Any> {
        return hayStack.readMapById(id)
    }

    /* using new model version to fetch the device  which is not migrated to new model version
      specifically for bypass damper and DAB devices
    */
    fun readNonDmDevice(modelId: String, newModelVersion: String) : Map<Any,Any> {
        return hayStack.readEntity("device and sourceModelVersion!=\"$newModelVersion\" and sourceModel==\"$modelId\" or modelId == \"$modelId\"")
    }
    @JvmStatic
    fun readValAtLevelByDomain(domainName: String, level: Int) : Double {
        val point: HashMap<Any, Any> = hayStack.readEntity("point and domainName == \"$domainName\"")
        if (point.isNotEmpty()) {
            val id = point["id"].toString()
            return hayStack.readDefaultValByLevel(id, level)
        } else {
            CcuLog.d(CCUTagsDb.TAG_CCU_HS, "Invalid point read attempt: $domainName")
            return 0.0
        }
    }
    fun writeValAtLevelByDomain(domainName: String, level: Int, value: Double) {
        val point: HashMap<Any, Any> = hayStack.readEntity("point and domainName == \"$domainName\"")
        if (point.isNotEmpty()) {
            val id = point["id"].toString()
            hayStack.writePoint(id, level, hayStack.ccuUserName, value, 0)
        } else {
            CcuLog.d(CCUTagsDb.TAG_CCU_HS, "Invalid point write attempt: $domainName")
        }
    }

    fun getListOfDisNameByDomainName(domainName: String, model: SeventyFiveFProfileDirective) : List<String> {
        val valuesList: MutableList<String> = mutableListOf()
        val point = model.points.find { it.domainName == domainName }

        if (point?.valueConstraint is MultiStateConstraint) {

            (point.valueConstraint as MultiStateConstraint).allowedValues.forEach { state ->
                valuesList.add(state.dis!!)
            }
        }
        return valuesList
    }

    @JvmStatic
    fun getEquipDevices() : Map<String,DomainDevice> {
        return devices
    }

    /*we should make sure domain equip's are initialised before accessing Domain equips
    * If we are accessing while creating new site its better to access with safe check */
    fun checkSystemEquipInitialisedAndGetId() : String {
        return if(Domain::systemEquip.isInitialized) {
            systemEquip.getId()
        } else {
            ""
        }
    }
    fun checkCCUDeviceInitialisedAndGet() : CCUDevice? {
        return if(Domain::ccuDevice.isInitialized) {
            ccuDevice
        } else {
            null
        }
    }

    fun getDomainDiagEquip() : CCUDiagEquip?{
        return if (Domain::diagEquip.isInitialized) {
            diagEquip
        } else {
            null
        }
    }

    fun getDomainCCUEquip() : CCUEquip? {
        return if (Domain::ccuEquip.isInitialized) {
            ccuEquip
        } else {
            null
        }
    }

    fun isDiagEquipInitialised() : Boolean {
        return Domain::diagEquip.isInitialized
    }

    fun createDomainPoint(
        model: SeventyFiveFProfileDirective, profileConfiguration: ProfileConfiguration,
        equipRef: String, siteRef: String, tz: String, equipDis: String, domainName: String
    ) {
        val equipBuilder = ProfileEquipBuilder(CCUHsApi.getInstance())
        val modelPointDef = model.points.find { it.domainName == domainName }
        modelPointDef?.run {
            CcuLog.d(
                LOG_TAG, "Creating point for domainName: $domainName ")
            equipBuilder.createPoint(
                PointBuilderConfig(
                    modelPointDef,
                    profileConfiguration,
                    equipRef,
                    siteRef,
                    tz,
                    equipDis
                )
            )
        }
    }

    fun createDomainDevicePoint(
        deviceModel: SeventyFiveFDeviceDirective,
        equipModel: SeventyFiveFProfileDirective,
        profileConfiguration: ProfileConfiguration,
        device: a75f.io.api.haystack.Device,
        deviceDis: String,
        domainName: String,
    ) {
        val entityMapper = EntityMapper(equipModel)
        val deviceBuilder = DeviceBuilder(hayStack, entityMapper)
        val modelPointDef = deviceModel.points.find { it.domainName == domainName }
        modelPointDef?.run {
            CcuLog.d(LOG_TAG, "Creating point for domainName: $domainName ")
            deviceBuilder.createPoint(
                modelPointDef,
                profileConfiguration,
                device,
                deviceDis
            )
        }
    }

    @JvmStatic
    fun pointWriteAndHisWriteById(id: String, value: Double, who: String) {
        val pointDetails = a75f.io.api.haystack.Point.Builder()
            .setHashMap(hayStack.readMapById(id))
            .build()

        val markers = pointDetails.markers

        // Handle writable points
        if (markers.contains("writable")) {
            hayStack.pointWrite(
                HRef.copy(id),
                8,
                who,
                HNum.make(value),
                HNum.make(0)
            )
            CcuLog.d(LOG_TAG, "Updated point write for ID: $id, Value: $value")
        }

        // Handle historical value writes
        if (markers.contains("his")) {
            val priorityVal = if (markers.contains(Tags.WRITABLE)) {
                hayStack.readPointPriorityVal(id)
            } else {
                value
            }
            hayStack.writeHisValById(id, priorityVal)
            CcuLog.d(LOG_TAG, "Updated historical value write for ID: $id, Value: $priorityVal")
        }
    }

}