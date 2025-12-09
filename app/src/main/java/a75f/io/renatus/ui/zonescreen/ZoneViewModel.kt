package a75f.io.renatus.ui.zonescreen

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil
import a75f.io.logic.bo.building.modbus.buildModbusModel
import a75f.io.logic.bo.building.pcn.PCNUtil
import a75f.io.logic.bo.util.CCUUtils
import a75f.io.renatus.ZoneEquip
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.observer.PointSubscriber
import a75f.io.renatus.ui.zonescreen.tempprofiles.updateDesiredCurrentTemp
import a75f.io.renatus.util.HeartBeatUtil
import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap


@SuppressLint("StaticFieldLeak")
class ZoneViewModel : ViewModel() {

    private var job: Job? = null
    private var roomEquips = ConcurrentHashMap<String, ZoneEquip>()

    companion object {
        const val ZONE_HEARTBEAT_UPDATE = "ZONE_HEARTBEAT_UPDATE"
        private const val HEARTBEAT_DELAY = 60_000L
    }

    fun observeZoneHealth() {
        stopObservingZoneHealth()
        CcuLog.d(ZONE_HEARTBEAT_UPDATE, "Started observing zone heartbeat.")

        job = viewModelScope.launch(Dispatchers.IO) {
            val api = CCUHsApi.getInstance()
            while (isActive) {
                roomEquips.entries.forEach { (roomId, zoneEquip) ->
                    updateZoneHealth(roomId, zoneEquip, api)
                }
                delay(HEARTBEAT_DELAY)
            }
        }
    }

    private suspend fun updateZoneHealth(
        roomId: String,
        zoneEquip: ZoneEquip,
        api: CCUHsApi
    ) {
        try {
            val isZoneAlive: Boolean
            if (zoneEquip.isTemperatureProfile) {
                val equips = CCUHsApi.getInstance()
                    .readAllEntities("equip and zone and roomRef ==\"$roomId\"")

                isZoneAlive = HeartBeatUtil.isZoneAlive(equips)
                CcuLog.d(
                    ZONE_HEARTBEAT_UPDATE,
                    "Updating 75f profile's zone status → alive: $isZoneAlive, room: $roomId"
                )
            } else {
                isZoneAlive = when {
                    ConnectNodeUtil.isConnectNodePaired(roomId) ->
                        isConnectNodeAlive(roomId, api)

                    PCNUtil.getPCNForZone(roomId, api).isNotEmpty() ->
                        isPCNAlive(roomId, api)

                    CCUUtils.isModbusEquip(roomId) ->
                        isModbusAlive(roomId)

                    CCUUtils.isPIDEquip(roomId) ->
                        isModuleAlive(roomId)

                    CCUUtils.isEMREquip(roomId) ->
                        isModuleAlive(roomId)

                    else ->
                        isBacnetAlive(roomId, api)
                }
                CcuLog.d(
                    ZONE_HEARTBEAT_UPDATE,
                    "Updating zone status → alive: $isZoneAlive, room: $roomId"
                )
            }

            pushUIUpdate(zoneEquip, isZoneAlive)
        } catch (e: Exception) {
            e.printStackTrace()
            CcuLog.d(ZONE_HEARTBEAT_UPDATE, "Exception while updating heartbeat")
        }
    }

    private fun isConnectNodeAlive(roomId: String, api: CCUHsApi): Boolean {
        val connectNode = ConnectNodeUtil.getConnectNodeForZone(roomId, api)
        val id = connectNode["id"]?.toString()
        val alive = CCUUtils.isLowCodeDeviceAlive(id)
        CcuLog.d(ZONE_HEARTBEAT_UPDATE, "ConnectNode heartbeat updated for $roomId")
        return alive
    }

    private fun isPCNAlive(roomId: String, api: CCUHsApi): Boolean {
        val pcn = PCNUtil.getPCNForZone(roomId, api)
        val id = pcn["id"]?.toString()
        val alive = CCUUtils.isLowCodeDeviceAlive(id)
        CcuLog.d(ZONE_HEARTBEAT_UPDATE, "PCN heartbeat updated for $roomId")
        return alive
    }

    private fun isModbusAlive(roomId: String): Boolean {
        val modbusList = buildModbusModel(roomId)

        for (equip in modbusList) {
            val equipId = equip.deviceEquipRef
            val deviceId = equip.slaveId.toString()

            if (!HeartBeatUtil.isModbusModuleAlive(deviceId, equipId)) {
                CcuLog.d(ZONE_HEARTBEAT_UPDATE, "Modbus OFF for $roomId")
                return false
            }
        }

        CcuLog.d(ZONE_HEARTBEAT_UPDATE, "Modbus heartbeat updated for $roomId")
        return true
    }

    private fun isBacnetAlive(roomId: String, api: CCUHsApi): Boolean {
        val equips = api.readAllEntities("equip and roomRef == \"$roomId\"")

        equips.forEach { equip ->
            if (equip.containsKey("bacnet")) {
                val device = api.readEntity(
                    "device and equipRef == \"${equip["id"]}\""
                )
                val address = device["addr"].toString()
                val alive =
                    HeartBeatUtil.isModbusModuleAlive(address, equip["id"].toString())

                if (!alive) {
                    CcuLog.d(ZONE_HEARTBEAT_UPDATE, "Bacnet OFF for $roomId")
                    return false
                }
            }
        }

        CcuLog.d(ZONE_HEARTBEAT_UPDATE, "Bacnet heartbeat updated for $roomId\n")
        return true
    }

    private fun isModuleAlive(roomId: String): Boolean {
        val equips = CCUHsApi.getInstance()
            .readAllEntities("equip and roomRef ==\"$roomId\"")

        return HeartBeatUtil.isZoneAlive(equips)
    }

    private suspend fun pushUIUpdate(zoneEquip: ZoneEquip, isAlive: Boolean) {
        withContext(Dispatchers.Main) {
            HeartBeatUtil.zoneStatus(zoneEquip.textViewModule, isAlive)
        }
    }

    fun stopObservingZoneHealth() {
        job?.cancel()
        job = null
        CcuLog.d(ZONE_HEARTBEAT_UPDATE, "Stopped observing zone heartbeat.")
    }

    fun setRoomEquips(map: Map<String, ZoneEquip>) {
        roomEquips.clear()
        roomEquips.putAll(map)
    }
}