package a75f.io.logic.preconfig

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

object PreConfigZoneUtil {
    private val nodeAddressCounter = AtomicInteger(1000) // Start from 1000

    suspend fun createAllZones(
        preConfigData: PreconfigurationData,
        floorId: String,
        siteId: String,
        ccuHsApi: CCUHsApi
    ) = coroutineScope {
        val semaphore = Semaphore(4) // limit concurrency

        val jobs = preConfigData.zones.map { zone ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    val nodeAddress = nodeAddressCounter.incrementAndGet()
                    createZones(zone, floorId, siteId, ccuHsApi, nodeAddress)
                }
            }
        }
        jobs.awaitAll()
    }

    // Your existing zone creation logic
    private fun createZones(
        zone: String,
        floorId: String,
        siteId: String,
        ccuHsApi: CCUHsApi,
        nodeAddress: Int
    ) {
        val zoneId = createZone(zone, floorId, siteId, ccuHsApi)
        val equipId = createTerminalEquip(
            "dabSmartNode",
            floorId,
            zoneId,
            nodeAddress,
            NodeType.SMART_NODE,
            ccuHsApi
        )
        DesiredTempDisplayMode.setModeType(zoneId, ccuHsApi)
        CcuLog.i(L.TAG_PRECONFIGURATION, "Created zone $zoneId and equip $equipId")
    }
}
