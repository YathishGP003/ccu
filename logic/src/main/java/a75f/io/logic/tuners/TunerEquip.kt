package a75f.io.logic.tuners

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HayStackConstants
import a75f.io.api.haystack.Point
import a75f.io.domain.BuildingEquip
import a75f.io.domain.api.Domain
import a75f.io.domain.logic.TunerEquipBuilder
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.TunerSyncFailed
import io.seventyfivef.ph.core.Tags
import kotlinx.coroutines.launch
import org.projecthaystack.HDict
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HGridBuilder
import org.projecthaystack.HList
import org.projecthaystack.HNum
import org.projecthaystack.HRef
import org.projecthaystack.HRow
import org.projecthaystack.UnknownRecException
import org.projecthaystack.client.HClient

object TunerEquip : CCUHsApi.OnCcuRegistrationCompletedListener {

    /**
     * Must be called during every app restart.
     * It creates/updates TunerEquip and then syncs during first time registration.
     * Subsequent restart, this just triggers a tuner sync for level 16.
     */
    fun initialize(haystack : CCUHsApi) {
        CcuLog.i(L.TAG_CCU_TUNER, "Initialize Building Equip")
        val tunerEquip = haystack.readEntity("equip and tuner")
        //Building Equip does not exist, create new one.
        if (tunerEquip.isEmpty()) {
            CcuLog.i(L.TAG_CCU_TUNER, "Create Building Equip")
            if (haystack.site == null) {
                CcuLog.i(L.TAG_CCU_TUNER, "Site does not exist , cant initialize tuner equip")
                return
            }
            val equipBuilder = TunerEquipBuilder(haystack)
            equipBuilder.buildEquipAndPoints(haystack.site!!.id)
            CcuLog.i(L.TAG_CCU_TUNER, " registerOnCcuRegistrationCompletedListener ")
            haystack.registerOnCcuRegistrationCompletedListener(this)
        } else {
            CcuLog.i(L.TAG_CCU_TUNER, "Tuner equip already exists.")
            if (!doCutOverMigrationIfRequired(haystack)) {
                syncBuildingTuners(haystack)
            }
            //TODO- Test backend migration.
            //val equipBuilder = TunerEquipBuilder(haystack)
            //equipBuilder.updateBackendBuildingTuner(haystack.site!!.id, haystack)
        }
    }

    private fun syncBuildingTuners(siteId: String, hClient: HClient, haystack: CCUHsApi) {
        CcuLog.i(L.TAG_CCU_TUNER, "syncBuildingTuners $siteId")
        val remotePoints = mutableListOf<Point>()
        try {
            val tunerPointsDict = HDictBuilder().add(
                "filter",
                "tuner and point and default and siteRef == $siteId"
            ).toDict();
            val tunerPointsGrid = hClient.call ("read", HGridBuilder.dictToGrid(tunerPointsDict))
            tunerPointsGrid?.dump()

            val pointMaps = haystack.HGridToList(tunerPointsGrid)
            pointMaps.forEach {remotePoints.add(Point.Builder().setHashMap(it).build())}

        } catch (e: UnknownRecException) {
            e.printStackTrace();
        }

        remotePoints.forEach {
            if (it.domainName.isNullOrEmpty()) {
                CcuLog.e(L.TAG_CCU_TUNER, "Invalid domain name for $it")
            } else {
                CcuLog.i(
                    L.TAG_CCU_TUNER,
                    "Copy UUID of Building equip remote point ${it.domainName}"
                )
                try {
                    val localPointDict = Domain.readDict(it.domainName)
                    if (!localPointDict.isEmpty) {
                        val localPoint = Point.Builder().setHDict(localPointDict).build()
                        val defaultVal = haystack.readDefaultValByLevel(localPoint.id, 17)
                        if (localPoint.id != it.id) {
                            CcuLog.i(
                                L.TAG_CCU_TUNER,
                                "Update UUID of local Building equip point ${localPoint.domainName}"
                            )
                            haystack.removeEntity(localPoint.id)
                            localPoint.id = it.id
                            haystack.addRemotePoint(localPoint, localPoint.id)
                            haystack.writeDefaultTunerValById(localPoint.id, defaultVal)
                        } else {
                            CcuLog.i(
                                L.TAG_CCU_TUNER,
                                "UUID already synced - Building equip point ${localPoint.domainName}"
                            )
                        }
                    }
                } catch ( e : Exception) {
                    CcuLog.e(L.TAG_CCU_TUNER, "Point not found in local DB for domainName ${it.domainName}", e)
                }


            }
        }
        val pointDictTobeSynced = remotePoints.filter { !it.domainName.isNullOrEmpty()}
            .filter { Domain.readPoint(it.domainName).isNotEmpty() }
            .map { HDictBuilder().add("id", HRef.copy(it.id)).toDict() }
        syncPointArrays(pointDictTobeSynced, hClient, haystack)

        //Re-initialize cached Ids in building equip singleton.
        val tunerEquip = haystack.readEntity("equip and tuner")
        if (tunerEquip.isNotEmpty()) {
            Domain.buildingEquip = BuildingEquip(tunerEquip[Tags.ID].toString())
        }

        CcuLog.i(L.TAG_CCU_TUNER, "syncBuildingTuners Completed")
    }

    private fun syncPointArrays(points: List<HDict>, hClient: HClient, haystack: CCUHsApi) {
        CcuLog.i(L.TAG_CCU_TUNER, "syncPointArrays Size ${points.size}")
        val partitionSize = 25
        val partitions: MutableList<List<HDict>> = ArrayList()
        var limit = 0
        while (limit < points.size) {
            partitions.add(points.subList(limit, (limit + partitionSize).coerceAtMost(points.size)))
            limit += partitionSize
        }
        for (sublist in partitions) {
            val writableArrayPoints = hClient.call(
                "pointWriteMany",
                HGridBuilder.dictsToGrid(sublist.toTypedArray())
            )

            //We cannot proceed adding new CCU to existing Site without fetching all the point array values.
            if (writableArrayPoints == null) {
                CcuLog.e(
                    L.TAG_CCU_TUNER,
                    "Failed to fetch point array values while importing existing tuner data."
                )
                throw TunerSyncFailed("Failed to fetch tuners")
            }
            val hDictList = ArrayList<HDict>()
            val rowIterator = writableArrayPoints.iterator()
            while (rowIterator.hasNext()) {
                val row = rowIterator.next() as HRow
                val id = row["id"].toString()
                val kind = row["kind"].toString()
                val data = row["data"]
                CcuLog.i(L.TAG_CCU_TUNER, "Imported point array $data")
                if (data is HList && data.size() > 0) {
                    for ( index in 0 until data.size()) {
                        val dataElement = data[index] as HDict
                        val level = dataElement["level"].toString()
                        if (level.toInt() == 16) {
                            CcuLog.i(L.TAG_CCU_TUNER, "Sync Level 16 to CCU")
                            val who = dataElement.getStr("who")
                            val value = dataElement["val"]
                            /*val lastModifiedTimeTag: Any? = dataElement["lastModifiedDateTime", false]
                    val lastModifiedDateTime = if (lastModifiedTimeTag != null) {
                        lastModifiedTimeTag as HDateTime?
                    } else {
                        HDateTime.make(System.currentTimeMillis())
                    }*/
                            haystack.pointWrite(
                                HRef.copy(id),
                                level.toInt(),
                                who,
                                value,
                                HNum.make(0)
                            )
                            //TODO- lastModifiedTime required: Update later by changing pointWrite method.
                            haystack.writeHisValById(id, value.toString().toDouble())
                        }
                    }
                } else {
                    CcuLog.i(L.TAG_CCU_TUNER, "Point array does not exist for $row")
                }
            }
        //TODO - why this was needed
        //hClient.call("pointWriteMany", HGridBuilder.dictsToGrid(hDictList.toTypedArray()))
        }
    }

    override fun onRegistrationCompleted(haystack: CCUHsApi) {
        CcuLog.i(L.TAG_CCU_TUNER, "Building Equip onRegistrationCompleted received")
        haystack.unRegisterOnCcuRegistrationCompletedListener { this }
        syncBuildingTuners(haystack)
    }
    fun syncBuildingTuners(haystack: CCUHsApi) {
        CcuLog.i(L.TAG_CCU_TUNER, "Sync building Tuners");
        val hClient = HClient(haystack.hsUrl, HayStackConstants.USER, HayStackConstants.PASS)
        val siteId = haystack.siteIdRef.toString()
        Domain.domainScope.launch {
            syncBuildingTuners(siteId, hClient, haystack)
            //TODO- Propagate level 16 to system and zone tuner copies
        }
    }

    private fun doCutOverMigrationIfRequired(haystack: CCUHsApi) : Boolean {
        val buildingEquip = haystack.readEntity("equip and tuner");
        if (buildingEquip["domainName"]?.toString()?.isNotEmpty() == true) {
            CcuLog.i(Domain.LOG_TAG, "Building equip cut-over migration complete.")
        } else {
            CcuLog.i(Domain.LOG_TAG, "Building equip cut-over migration start.")
            val equipBuilder = TunerEquipBuilder(haystack)
            val equipId = buildingEquip["id"].toString()
            equipBuilder.migrateBuildingTunerPointsForCutOver(equipId, haystack.site!!)
            Domain.buildingEquip = BuildingEquip(equipId)
            return true
        }
        return false
    }
}