package a75f.io.domain.logic

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.BypassDamperEquip
import a75f.io.domain.HyperStatSplitEquip
import a75f.io.domain.OAOEquip
import a75f.io.domain.VavAcbEquip
import a75f.io.domain.api.Device
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.Equip
import a75f.io.domain.api.Point
import a75f.io.domain.api.Site
import a75f.io.domain.devices.CCUDevice
import a75f.io.domain.devices.CmBoardDevice
import a75f.io.domain.devices.ConnectDevice
import a75f.io.domain.devices.HyperStatDevice
import a75f.io.domain.devices.MyStatDevice
import a75f.io.domain.equips.BuildingEquip
import a75f.io.domain.equips.CCUDiagEquip
import a75f.io.domain.equips.CCUEquip
import a75f.io.domain.equips.DabAdvancedHybridSystemEquip
import a75f.io.domain.equips.DabEquip
import a75f.io.domain.equips.DabModulatingRtuSystemEquip
import a75f.io.domain.equips.DabStagedSystemEquip
import a75f.io.domain.equips.DabStagedVfdSystemEquip
import a75f.io.domain.equips.DefaultSystemEquip
import a75f.io.domain.equips.DomainEquip
import a75f.io.domain.equips.OtnEquip
import a75f.io.domain.equips.SseEquip
import a75f.io.domain.equips.PlcEquip
import a75f.io.domain.equips.VavAdvancedHybridSystemEquip
import a75f.io.domain.equips.VavEquip
import a75f.io.domain.equips.VavModulatingRtuSystemEquip
import a75f.io.domain.equips.VavStagedSystemEquip
import a75f.io.domain.equips.VavStagedVfdSystemEquip
import a75f.io.domain.equips.hyperstat.CpuV2Equip
import a75f.io.domain.equips.hyperstat.HpuV2Equip
import a75f.io.domain.equips.hyperstat.MonitoringEquip
import a75f.io.domain.equips.hyperstat.Pipe2V2Equip
import a75f.io.domain.equips.mystat.MyStatCpuEquip
import a75f.io.domain.equips.mystat.MyStatHpuEquip
import a75f.io.domain.equips.mystat.MyStatPipe2Equip
import a75f.io.domain.util.CommonQueries
import a75f.io.logger.CcuLog
import io.seventyfivef.ph.core.Tags


/**
 *
 */
object DomainManager {

    fun buildDomain(hayStack : CCUHsApi) {
        val site = hayStack.site
        val tunerEquip = hayStack.readEntity("tuner and equip")
        if (tunerEquip.isNotEmpty()) {
            Domain.buildingEquip = BuildingEquip(tunerEquip[Tags.ID].toString())
        }
        site?.let { Domain.site = Site(site.displayName, site.id) }

        val ccu =  hayStack.ccu
        ccu?.let {
            Domain.site?.addCcu(it)
            addSystemEquip(hayStack, it["id"].toString())
            addSystemDevice(hayStack, it["id"].toString())
            addBypassEquip(hayStack, it["id"].toString())
            addOaoEquip(hayStack, it["id"].toString())
            addOaoDevice(hayStack, it["id"].toString())
            addEquipToDomain(hayStack, it["id"].toString(), DomainName.ccuConfiguration)
            addEquipToDomain(hayStack, it["id"].toString(), DomainName.diagEquip)
        }

        val floors = hayStack.readAllEntities("floor")
        floors.forEach{ floor ->
            val floorId = floor["id"]
            floorId?.let {
                Domain.site?.addFloor(floor)
                val rooms = hayStack.readAllEntities(
                    "room and floorRef == \"$floorId\"")

                rooms.forEach { room ->
                    val roomId = room["id"]
                    roomId?.let {
                        Domain.site?.floors?.get(floorId.toString())?.addRoom(room)
                        addEquips(hayStack, floorId.toString(), roomId.toString())
                        addDevices(hayStack, floorId.toString(), roomId.toString())
                    }
                }
            }

        }
        addDomainEquips(hayStack)
        addSystemDomainEquip(hayStack)
        addCmBoardDevice(hayStack)
        addDomainDevices(hayStack)
        addCCUDevice(hayStack)
        addDiagEquip(hayStack)
        addCCUEquip(hayStack)
    }

    fun addCCUEquip(hayStack: CCUHsApi) {
        val ccuEquip = hayStack.readEntityByDomainName(DomainName.ccuConfiguration)
        if (ccuEquip.isNotEmpty()) {
            CcuLog.e(Domain.LOG_TAG, "Added CCU Equip to domain")
                Domain.ccuEquip = CCUEquip(ccuEquip["id"].toString())
        }
    }

    fun addDomainEquips(hayStack: CCUHsApi) {
        hayStack.readAllEntities("zone and equip")
            .forEach {
                CcuLog.i(Domain.LOG_TAG, "Build domain $it")

                when{
                    it["domainName"]?.toString().equals("smartnodeActiveChilledBeam", true) -> {
                        Domain.equips[it["id"].toString()] = VavAcbEquip(it["id"].toString())
                    }
                    it.contains("vav") -> {
                        Domain.equips[it["id"].toString()] = VavEquip(it["id"].toString())
                        CcuLog.i(Domain.LOG_TAG, "Build domain vavEquip $it")
                    }
                    it.contains("dab") -> Domain.equips[it["id"].toString()] = DabEquip(it["id"].toString())
                    it.contains("sse") -> Domain.equips[it["id"].toString()] = SseEquip(it["id"].toString())
                    (it.contains("hyperstat") && it.contains("cpu")) -> Domain.equips[it["id"].toString()] = CpuV2Equip(it["id"].toString())
                    (it.contains("hyperstat") && it.contains("hpu")) -> Domain.equips[it["id"].toString()] = HpuV2Equip(it["id"].toString())
                    (it.contains("hyperstat") && it.contains("pipe2")) -> Domain.equips[it["id"].toString()] = Pipe2V2Equip(it["id"].toString())
                    (it.contains("domainName") && it["domainName"].toString() == DomainName.hyperstatMonitoring) -> Domain.equips[it["id"].toString()] = MonitoringEquip(it["id"].toString())
                    it.contains("otn") -> Domain.equips[it["id"].toString()] = OtnEquip(it["id"].toString())
                    it.contains("pid") -> Domain.equips[it["id"].toString()] = PlcEquip(it["id"].toString())
                    (it.contains("mystat") && it.contains("cpu")) -> Domain.equips[it["id"].toString()] = MyStatCpuEquip(it["id"].toString())
                    (it.contains("mystat") && it.contains("hpu")) -> Domain.equips[it["id"].toString()] = MyStatHpuEquip(it["id"].toString())
                    (it.contains("mystat") && it.contains("pipe2")) -> Domain.equips[it["id"].toString()] = MyStatPipe2Equip(it["id"].toString())
                    it.contains("hyperstatsplit") -> {
                        CcuLog.i(Domain.LOG_TAG, "Build domain hyperstatsplit is addded $it")
                        Domain.equips[it["id"].toString()] = HyperStatSplitEquip(it["id"].toString())
                    }

                }
            }

        hayStack.readAllEntities("bypassDamper and equip")
            .forEach {
                CcuLog.i(Domain.LOG_TAG, "Build domain $it")
                Domain.equips[it["id"].toString()] = BypassDamperEquip(it["id"].toString())
            }

        hayStack.readAllEntities("otn and equip")
            .forEach {
                CcuLog.i(Domain.LOG_TAG, "Build domain $it")
                Domain.equips[it["id"].toString()] = OtnEquip(it["id"].toString())
            }

        hayStack.readAllEntities("oao and equip")
            .forEach {
                CcuLog.i(Domain.LOG_TAG, "Build domain $it")
                Domain.equips[it["id"].toString()] = OAOEquip(it["id"].toString())
            }

        Domain.equips.forEach {
            CcuLog.i(Domain.LOG_TAG, "Added equip to domain ${it.key}")
        }
    }

    fun addSystemDomainEquip(hayStack: CCUHsApi) {
        val systemEquip = hayStack.readEntity(CommonQueries.SYSTEM_PROFILE)
        if (systemEquip.isNotEmpty()) {
            if (systemEquip["domainName"] != null) {
                    Domain.systemEquip = when(systemEquip["domainName"].toString()) {
                    "vavStagedRtu" -> {
                        CcuLog.i(Domain.LOG_TAG, "Add vavStagedRtu systemEquip to domain ")
                        VavStagedSystemEquip(systemEquip["id"].toString())
                    }

                    "vavStagedRtuVfdFan" -> {
                        CcuLog.i(Domain.LOG_TAG, "Add vavStagedRtuVfdFan systemEquip to domain ")
                        VavStagedVfdSystemEquip(systemEquip["id"].toString())
                    }

                    "vavAdvancedHybridAhuV2" -> {
                        val connectEquip = hayStack.readEntity("system and equip and connectModule")
                        CcuLog.i(Domain.LOG_TAG, "Add vavAdvancedHybridAhuV2 systemEquip to domain  : ConnectEquip ${connectEquip["id"]}")
                        VavAdvancedHybridSystemEquip(systemEquip["id"].toString(), connectEquip["id"].toString())
                    }
                    "dabAdvancedHybridAhuV2" -> {
                        val connectEquip = hayStack.readEntity("system and equip and connectModule")
                        CcuLog.i(Domain.LOG_TAG, "Add dabAdvancedHybridAhuV2 systemEquip to domain  : ConnectEquip ${connectEquip["id"]}")
                        DabAdvancedHybridSystemEquip(systemEquip["id"].toString(), connectEquip["id"].toString())
                    }

                    "vavFullyModulatingAhu" -> {
                        CcuLog.i(Domain.LOG_TAG, "Add vavFullyModulatingAhu systemEquip to domain ")
                        VavModulatingRtuSystemEquip(systemEquip["id"].toString())
                    }

                    "dabStagedRtu" -> {
                        CcuLog.i(Domain.LOG_TAG, "Add dabStagedRtu systemEquip to domain ")
                        DabStagedSystemEquip(systemEquip["id"].toString())
                    }

                    "dabStagedRtuVfdFan" -> {
                        CcuLog.i(Domain.LOG_TAG, "Add dabStagedRtuVfdFan systemEquip to domain ")
                        DabStagedVfdSystemEquip(systemEquip["id"].toString())
                    }

                    "dabFullyModulatingAhu" -> {
                        CcuLog.i(Domain.LOG_TAG, "Add dabFullyModulatingAhu systemEquip to domain ")
                        DabModulatingRtuSystemEquip(systemEquip["id"].toString())
                    }

                     DomainName.defaultSystemEquip -> {
                        CcuLog.i(Domain.LOG_TAG, "Add defaultSystemEquip systemEquip to domain ")
                            DefaultSystemEquip(systemEquip["id"].toString())
                     }

                    else -> {
                        CcuLog.e(Domain.LOG_TAG, "Unknown system equip ${systemEquip["domainName"]}")
                        DomainEquip(systemEquip["id"].toString())
                    }
                }
            } else  {
                CcuLog.e(Domain.LOG_TAG, "Unknown system (${systemEquip["profile"]}) equip does not contains domainName ")
            }
        } else {
            CcuLog.e(Domain.LOG_TAG, "No system equip found")
            Domain.systemEquip = DomainEquip(systemEquip["id"].toString())
        }
    }

    fun addCmBoardDevice(hayStack: CCUHsApi) {
        val cmBoardDevice = hayStack.readEntity("device and cm")
        if (cmBoardDevice.isNotEmpty()) {
            CcuLog.e(Domain.LOG_TAG, "Added CM device to domain")
            Domain.cmBoardDevice = CmBoardDevice(cmBoardDevice["id"].toString())
            if (Domain.systemEquip is VavAdvancedHybridSystemEquip
                    || Domain.systemEquip is DabAdvancedHybridSystemEquip) {
                val connect1Device = hayStack.readEntity("device and connectModule")
                if (connect1Device.isNotEmpty()) {
                    CcuLog.d(Domain.LOG_TAG, "Added Connect1 device to domain")
                    Domain.connect1Device = ConnectDevice(connect1Device["id"].toString())
                } else {
                    CcuLog.e(Domain.LOG_TAG, "-------- Connect1 device not added to to domain -")
                }
            }
        }


    }

    private fun addDomainDevices(hayStack: CCUHsApi) {
        hayStack.readAllEntities("zone and equip").forEach { equip ->

            val deviceMap = hayStack.readEntity("domainName and device and equipRef == \"${equip["id"]}\"")
            if (deviceMap.isNotEmpty()) {
                when {
                    (equip.contains("hyperstat") && (equip.contains("cpu") || equip.contains("hpu") || equip.contains("pipe2") ||
                            equip.contains(a75f.io.api.haystack.Tags.MONITORING))) -> Domain.devices[equip["id"].toString()] = HyperStatDevice(deviceMap["id"].toString())
                    (equip.contains("mystat") && (equip.contains("cpu") || equip.contains("hpu") || equip.contains("pipe2"))) -> Domain.devices[equip["id"].toString()] = MyStatDevice(deviceMap["id"].toString())
                }
            }
        }
    }

    fun addDiagEquip(hayStack: CCUHsApi) {
        val diagEquip = hayStack.readEntity("diag and equip")
        if (diagEquip.isNotEmpty()) {
            CcuLog.e(Domain.LOG_TAG, "Added Diag to domain")
            Domain.diagEquip = CCUDiagEquip(diagEquip["id"].toString())
        }
    }

    fun addCCUDevice(hayStack: CCUHsApi) {
        val ccuDevice = hayStack.readEntity("device and ccu")
        if (ccuDevice.isNotEmpty()) {
            CcuLog.e(Domain.LOG_TAG, "Added CCU device to domain")
            Domain.ccuDevice = CCUDevice(ccuDevice["id"].toString())
        }
    }
    fun updateDomainEquip(equip: a75f.io.api.haystack.Equip) {
        when {
            equip.domainName.contains("smartnodeActiveChilledBeam") ->  {
                Domain.equips[equip.id] = VavAcbEquip(equip.id)
            }
            equip.markers.contains("vav") -> Domain.equips[equip.id] = VavEquip(equip.id)
            equip.markers.contains("dab") -> Domain.equips[equip.id] = DabEquip(equip.id)
            equip.markers.contains("pid") -> Domain.equips[equip.id] = PlcEquip(equip.id)

            equip.markers.contains("sse") -> Domain.equips[equip.id] = SseEquip(equip.id)
            equip.markers.contains("otn") -> Domain.equips[equip.id] = OtnEquip(equip.id)
            equip.markers.contains("hyperstat") -> {
                when {
                    equip.markers.contains("cpu") -> Domain.equips[equip.id] = CpuV2Equip(equip.id)
                    equip.markers.contains("hpu") -> Domain.equips[equip.id] = HpuV2Equip(equip.id)
                    equip.markers.contains("pipe2") -> Domain.equips[equip.id] = Pipe2V2Equip(equip.id)
                    equip.markers.contains(a75f.io.api.haystack.Tags.MONITORING) ->
                        Domain.equips[equip.id] = MonitoringEquip(equip.id)
                }
            }

            equip.markers.contains("mystat") -> {
                when {
                    equip.markers.contains("cpu") -> Domain.equips[equip.id] = MyStatCpuEquip(equip.id)
                    equip.markers.contains("hpu") -> Domain.equips[equip.id] = MyStatHpuEquip(equip.id)
                    equip.markers.contains("pipe2") -> Domain.equips[equip.id] = MyStatPipe2Equip(equip.id)
                }
            }
            equip.markers.contains("hyperstatsplit") -> Domain.equips[equip.id] = HyperStatSplitEquip(equip.id)
        }
    }

    fun addSystemEquip(hayStack: CCUHsApi, ccuId: String) {
        val systemEquip = hayStack.readAllEntities("system and equip")
        systemEquip.forEach { equip->
            val equipId = equip["id"]
            equipId?.let {
                Domain.site?.ccus?.get(ccuId)?.addEquip(equip)
                val points =
                    hayStack.readAllEntities("point and equipRef == \"$equipId\"")
                points.forEach { point ->
                    val domainName = point["domainName"]
                    domainName?.let {
                        Domain.site?.ccus?.get(ccuId)?.equips?.get(equipId.toString())?.addPoint(point)

                    }
                }
            }
        }
    }

    private fun addSystemDevice(hayStack: CCUHsApi, ccuId: String) {
        val devices = hayStack.readAllEntities("device and roomRef == \"SYSTEM\" and not smartnode") // this query should not pick up OAO or Bypass Smart Nodes
        devices.forEach { device ->
            val deviceId = device["id"]
            deviceId?.let {
                Domain.site?.ccus?.get(ccuId)?.addDevice(device)
                val points =
                    hayStack.readAllEntities("point and deviceRef == \"$deviceId\"")
                points.forEach { point ->
                    val domainName = point["domainName"]
                    domainName?.let {
                        Domain.site?.ccus?.get(ccuId)?.devices?.get(deviceId)?.addPoint(point)
                    }
                }
            }
        }
    }

    fun addBypassEquip(hayStack: CCUHsApi, ccuId: String) {
        val bypassEquip = hayStack.readAllEntities("bypassDamper and equip")
        bypassEquip.forEach { equip->
            val equipId = equip["id"]
            equipId?.let {
                Domain.site?.ccus?.get(ccuId)?.addBypassEquip(equip)
                val points =
                    hayStack.readAllEntities("point and equipRef == \"$equipId\"")
                points.forEach { point ->
                    val domainName = point["domainName"]
                    domainName?.let {
                        Domain.site?.ccus?.get(ccuId)?.bypassEquips?.get(equipId.toString())?.addPoint(point)
                    }
                }
            }
        }
    }

    fun addOaoEquip(hayStack: CCUHsApi, ccuId: String) {
        val oaoEquip = hayStack.readAllEntities("oao and equip")
        oaoEquip.forEach { equip->
            val equipId = equip["id"]
            equipId?.let {
                Domain.site?.ccus?.get(ccuId)?.addDomainEquip(equip)
                val points =
                    hayStack.readAllEntities("point and equipRef == \"$equipId\"")
                points.forEach { point ->
                    val domainName = point["domainName"]
                    domainName?.let {
                        Domain.site?.ccus?.get(ccuId)?.equips?.get(equipId.toString())?.addPoint(point)
                    }
                }
            }
        }
    }
    fun addEquipToDomain(hayStack: CCUHsApi, ccuId: String, domainName : String) {
        val equip = hayStack.readEntity("domainName == \"$domainName\"")
        equip["id"]?.let { equipId ->
            Domain.site?.ccus?.get(ccuId)?.addDomainEquip(equip)
            val points =
                hayStack.readAllEntities("point and equipRef == \"$equipId\"")
            points.forEach { point ->
                val pointDomainName = point["domainName"]
                pointDomainName?.let {
                    Domain.site?.ccus?.get(ccuId)?.equips?.get(equipId.toString())?.addPoint(point)
                }
            }
        }
    }

    private fun addOaoDevice(hayStack: CCUHsApi, ccuId: String) {
        val oaoEquip = hayStack.readEntity("oao and equip")
        if(!oaoEquip.contains("id"))
            return
        val oaoEquipId = oaoEquip["id"].toString()
        val devices = hayStack.readAllEntities("point and equipRef == \"$oaoEquipId\"")
        devices.forEach { device ->
            val deviceId = device["id"]
            deviceId?.let {
                Domain.site?.ccus?.get(ccuId)?.addDevice(device)
                val points =
                    hayStack.readAllEntities("point and deviceRef == \"$deviceId\"")
                points.forEach { point ->
                    val domainName = point["domainName"]
                    domainName?.let {
                        Domain.site?.ccus?.get(ccuId)?.devices?.get(deviceId)?.addPoint(point)
                    }
                }
            }
        }
    }

    private fun addEquips(hayStack: CCUHsApi, floorId : String, roomId : String) {
        val equips = hayStack.readAllEntities("equip and roomRef == \"$roomId\"")
        equips.forEach { equip ->
            val equipId = equip["id"]
            equipId?.let {
                Domain.site?.floors?.get(floorId)?.rooms?.get(roomId)?.addEquip(equip)
                val points =
                    hayStack.readAllEntities("point and equipRef == \"$equipId\"")
                points.forEach {point ->
                    val domainName = point["domainName"]
                    domainName?.let {
                        Domain.site?.floors?.get(floorId)?.rooms
                            ?.get(roomId)?.equips?.get(equipId.toString())?.addPoint(point)
                    }
                }
            }
        }
    }

    private fun addDevices(hayStack: CCUHsApi, floorId : String, roomId : String) {
        val devices = hayStack.readAllEntities("device and roomRef == \"$roomId\"")
        devices.forEach { device ->
            val deviceId = device["id"]
            deviceId?.let {
                Domain.site?.floors?.get(floorId)?.rooms?.get(roomId)?.addDevice(device)
                val points =
                    hayStack.readAllEntities("point and deviceRef == \"$deviceId\"")
                points.forEach { point ->
                    val domainName = point["domainName"]
                    domainName?.let {
                        Domain.site?.floors?.get(floorId)?.rooms?.get(roomId)?.devices?.get(deviceId)
                            ?.addPoint(point)
                    }

                }
            }
        }
    }

    fun addEquip(hayStackEquip : a75f.io.api.haystack.Equip) {
        if (Domain.site == null){
            return
        }
        Domain.site?.floors?.get(hayStackEquip.floorRef)?.
        rooms?.get(hayStackEquip.roomRef)?.equips?.put(hayStackEquip.id, Equip(hayStackEquip.domainName, hayStackEquip.id))
    }
    fun addPoint(hayStackPoint : a75f.io.api.haystack.Point) {
        if (Domain.site == null) {
            return
        }
        Domain.site?.floors?.get(hayStackPoint.floorRef)?.
                rooms?.get(hayStackPoint.roomRef)?.equips?.get(hayStackPoint.equipRef)?.
                points?.put(hayStackPoint.domainName, Point(hayStackPoint.domainName, hayStackPoint.equipRef))
    }

    fun addDevice(hayStackDevice : a75f.io.api.haystack.Device) {
        if (Domain.site == null) {
            return
        }
        Domain.site?.floors?.get(hayStackDevice.floorRef)?.
        rooms?.get(hayStackDevice.roomRef)?.devices?.put(hayStackDevice.id, Device(hayStackDevice.domainName, hayStackDevice.id))

        val floor = Domain.site?.floors?.get(hayStackDevice.floorRef)
        val room = Domain.site?.floors?.get(hayStackDevice.floorRef)?.
        rooms?.get(hayStackDevice.roomRef)
        val device = Domain.site?.floors?.get(hayStackDevice.floorRef)?.
        rooms?.get(hayStackDevice.roomRef)?.equips?.get(hayStackDevice.equipRef)
    }

    fun addRawPoint(hayStackPoint : a75f.io.api.haystack.RawPoint) {
        if (Domain.site == null) {
            return
        }
        Domain.site?.floors?.get(hayStackPoint.floorRef)?.
        rooms?.get(hayStackPoint.roomRef)?.devices?.get(hayStackPoint.deviceRef)?.
        points?.put(hayStackPoint.domainName, Point(hayStackPoint.domainName, hayStackPoint.id))
    }

    fun removePoint(hayStackPoint : a75f.io.api.haystack.Point) {
        if (Domain.site == null) {
            return
        }
        Domain.site?.floors?.get(hayStackPoint.floorRef)?.
        rooms?.get(hayStackPoint.roomRef)?.equips?.get(hayStackPoint.equipRef)?.
        points?.remove(hayStackPoint.domainName, Point(hayStackPoint.domainName, hayStackPoint.equipRef))
    }
    fun removeDeviceRawPoint(hayStackPoint : a75f.io.api.haystack.RawPoint) {
        if (Domain.site == null) {
            return
        }
        Domain.site?.floors?.get(hayStackPoint.floorRef)?.
        rooms?.get(hayStackPoint.roomRef)?.devices?.get(hayStackPoint.deviceRef)?.
        points?.remove(hayStackPoint.domainName, Point(hayStackPoint.domainName, hayStackPoint.deviceRef))
    }
}

