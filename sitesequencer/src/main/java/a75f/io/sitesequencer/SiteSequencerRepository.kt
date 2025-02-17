package a75f.io.sitesequencer

import a75f.io.alerts.AlertManager
import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import a75f.io.sitesequencer.SequencerSchedulerUtil.Companion.createAlertDefinition
import a75f.io.sitesequencer.cloud.SiteSequencerService
import a75f.io.sitesequencer.model.SiteSequencerDefsMap
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Objects
import java.util.function.Consumer


class SiteSequencerRepository(
    private val dataStore: SequenceDataStore,
    private val sequenceProcessor: SequencerParser,
    private val siteSequencerService: SiteSequencerService,
    private val haystack: CCUHsApi
) {

    // cache of sequencer definitions
    private var _sequencerDefsMap: SiteSequencerDefsMap =
        dataStore.getSiteSequencerDefinitions().associateBy { it.seqId }.toMutableMap()

    // provide synchronized access to our in-memory store of sequencer defs.  Use this to read sequencer defs.
    private val sequencerDefsMap: SiteSequencerDefsMap
        get() = synchronized(_sequencerDefsMap) { _sequencerDefsMap }

    // current state of sequencer definition instances (i.e. positive tests)

    private fun getSequencerDefinitions(): List<SiteSequencerDefinition> =
        sequencerDefsMap.values.toList()

    fun deleteSequencerDefinition(id: String) {
        val title = sequencerDefsMap.findTitleById(id)
        if (title == null) {
            CcuLog.w(
                SequencerParser.TAG_CCU_SITE_SEQUENCER,
                "Could not find seq definition for id to delete: $id"
            )
            return
        }
        sequencerDefsMap.remove(title)
        saveDefs()
    }

    private fun fetchSequencerDefinitions() {
        CcuLog.d(
            SequencerParser.TAG_CCU_SITE_SEQUENCER,
            "fetchSequencerDefinitions !haystack.siteSynced()-->" + !haystack.siteSynced() + " !haystack.authorised-->" + !haystack.authorised
        )
        if (!haystack.siteSynced() || !haystack.authorised) {
            return
        }
        val siteId = haystack.siteIdRef.toVal()
        CcuLog.d(
            SequencerParser.TAG_CCU_SITE_SEQUENCER,
            "fetchSequencerDefinitions siteId-->$siteId"
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Switch to the IO context for the network call
                val response = siteSequencerService.getSiteDefinitions(siteId)
                handleRetrievedDefsSequences(haystack.context, response)
            } catch (e: Exception) {
                // Log and handle any errors that occur during the network request
                CcuLog.e(
                    SequencerParser.TAG_CCU_SITE_SEQUENCER,
                    "##Error fetching site definitions, check network connection and try again later",
                    e
                )
                scheduleSequencerJobs("offline stored definitions")
            }
        }
    }

    fun fetchSequencerDefinitionsForCleanup() {
        CcuLog.d(
            SequencerParser.TAG_CCU_SITE_SEQUENCER,
            "fetchSequencerDefinitionsForCleanup !haystack.siteSynced()-->" + !haystack.siteSynced() + " !haystack.authorised-->" + !haystack.authorised
        )
        if (!haystack.siteSynced() || !haystack.authorised) {
            return
        }
        val siteId = haystack.siteIdRef.toVal()
        CcuLog.d(
            SequencerParser.TAG_CCU_SITE_SEQUENCER,
            "fetchSequencerDefinitionsForCleanup siteId-->$siteId"
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Switch to the IO context for the network call
                val response = siteSequencerService.getSiteDefinitions(siteId)
                handleRetrievedDefsSequencesForCleanup(haystack.context, response)
            } catch (e: Exception) {
                // Log and handle any errors that occur during the network request
                CcuLog.e(
                    SequencerParser.TAG_CCU_SITE_SEQUENCER,
                    "##Error fetching site definitions can not do cleanup, check network connection and try again later",
                    e
                )
            }
        }
    }

    fun fetchSequencerDefssIfEmpty() {
        CcuLog.d(
            SequencerParser.TAG_CCU_SITE_SEQUENCER,
            "fetchSequencerDefssIfEmpty sequencerDefsMap.isEmpty()()-->" + sequencerDefsMap.isEmpty()
        )
        fetchSequencerDefinitions()
    }

    fun addSequencerDefinition(sequenceDef: SiteSequencerDefinition) {
        sequencerDefsMap[sequenceDef.seqId] = sequenceDef
        saveDefs()
        if(sequenceDef.enabled) {
            SequencerSchedulerUtil.createJob(haystack.context, sequenceDef)
        }
    }

    fun getSequenceById(seqId: String): SiteSequencerDefinition? {
        return sequencerDefsMap[seqId]
    }

    private fun SiteSequencerDefsMap.findTitleById(id: String) =
        values.find { it.seqId == id }?.seqName

    private fun handleRetrievedDefsSequences(
        context: Context,
        retrievedSeqDefs: List<SiteSequencerDefinition>
    ) {
        removeAllPendingIntents()
        cleanUp(retrievedSeqDefs)
        CcuLog.d(
            SequencerParser.TAG_CCU_SITE_SEQUENCER,
            "handleRetrievedDefsSequences ## retrievedSeqDefs.size-->" + retrievedSeqDefs.size
        )
        synchronized(_sequencerDefsMap) {
            CcuLog.d(
                SequencerParser.TAG_CCU_SITE_SEQUENCER,
                "handleRetrievedDefsSequences ## 1 _sequencerDefsMap.size-->" + _sequencerDefsMap.size
            )
            _sequencerDefsMap.clear()
            _sequencerDefsMap.putAll(retrievedSeqDefs.associateBy { it.seqId })

            //log
            CcuLog.d(
                SequencerParser.TAG_CCU_SITE_SEQUENCER,
                "Fetched ${_sequencerDefsMap.size} Predefined seq defs"
            )
            scheduleSequencerJobs("network fetched definitions")
            saveDefs()
        }
    }

    private fun handleRetrievedDefsSequencesForCleanup(
        context: Context,
        retrievedSeqDefs: List<SiteSequencerDefinition>
    ) {
        dailyCleanUp(retrievedSeqDefs)
        CcuLog.d(
            SequencerParser.TAG_CCU_SITE_SEQUENCER,
            "handleRetrievedDefsSequencesForCleanup ## retrievedSeqDefs.size-->" + retrievedSeqDefs.size
        )
        synchronized(_sequencerDefsMap) {
            CcuLog.d(
                SequencerParser.TAG_CCU_SITE_SEQUENCER,
                "handleRetrievedDefsSequencesForCleanup ## 1 _sequencerDefsMap.size-->" + _sequencerDefsMap.size
            )
            _sequencerDefsMap.clear()
            _sequencerDefsMap.putAll(retrievedSeqDefs.associateBy { it.seqId })

            CcuLog.d(
                SequencerParser.TAG_CCU_SITE_SEQUENCER,
                "Fetched ${_sequencerDefsMap.size} Predefined seq defs"
            )
            scheduleSequencerJobs("network fetched definitions")
            saveDefs()
        }
    }

    private fun scheduleSequencerJobs(source: String) {
        CcuLog.d(SequencerParser.TAG_CCU_SITE_SEQUENCER, "scheduleSequencerJobs-->source:$source")
        sequencerDefsMap.values.forEach {
            CcuLog.d(
                SequencerParser.TAG_CCU_SITE_SEQUENCER,
                "Predefined sequences Fetched: ${it.seqId} <-->${it.seqName} now scheduling based on frequency: ${it.quartzCronRequest.frequency}\""
            )
            if(it.enabled){
                SequencerSchedulerUtil.createJob(haystack.context, it)
            }
        }
    }

    private fun removeAllPendingIntents() {
        CcuLog.d(SequencerParser.TAG_CCU_SITE_SEQUENCER, "removeAllPendingIntents on app start")
        dataStore.getSiteSequencerDefinitions().associateBy { it.seqId }
            .toMutableMap().values.forEach {
            SequencerSchedulerUtil.cancelIntentsForSeqId(haystack.context, it.seqId)
        }
    }

    private fun cleanUp(retrievedSequencerDefs: List<SiteSequencerDefinition>) {
        CcuLog.d(
            SequencerParser.TAG_CCU_SITE_SEQUENCER,
            "doing cleanUp on app restart or device restart"
        )

        // Convert the data store list to a mutable map with seqId as the key
        val siteSequencerMap =
            dataStore.getSiteSequencerDefinitions().associateBy { it.seqId }.toMutableMap()

        siteSequencerMap.values.forEach { oldSiteSequencerDefinition ->
            val matchingNewDefinition = retrievedSequencerDefs.find { newSiteSequencerDefinition ->
                newSiteSequencerDefinition.seqId == oldSiteSequencerDefinition.seqId
            }

            if (matchingNewDefinition != null) {
                if (matchingNewDefinition.modifiedBy.dateTime != oldSiteSequencerDefinition.modifiedBy.dateTime) {
                    // seq got updated
                    CcuLog.d(
                        SequencerParser.TAG_CCU_SITE_SEQUENCER,
                        "doing cleanUp modified time is different remove alerts"
                    )
                    fixAlerts(oldSiteSequencerDefinition)
                }
            } else {
                // seq got deleted
                // remove log file
                CcuLog.d(
                    SequencerParser.TAG_CCU_SITE_SEQUENCER,
                    "doing cleanUp seq got removed remove alerts"
                )
                fixAlerts(oldSiteSequencerDefinition)
                val fileName =
                    "seq_" + CCUHsApi.getInstance().ccuId + "_" + oldSiteSequencerDefinition.seqId + ".json"
                SequencerLogUtil.deleteJsonFile(CCUHsApi.getInstance().context, fileName)
            }
        }
    }

    private fun dailyCleanUp(retrievedSequencerDefs: List<SiteSequencerDefinition>) {
        CcuLog.d(
            SequencerParser.TAG_CCU_SITE_SEQUENCER,
            "doing daily cleanUp"
        )

        // Convert the data store list to a mutable map with seqId as the key
        val siteSequencerMap =
            dataStore.getSiteSequencerDefinitions().associateBy { it.seqId }.toMutableMap()

        siteSequencerMap.values.forEach { oldSiteSequencerDefinition ->
            val matchingNewDefinition = retrievedSequencerDefs.find { newSiteSequencerDefinition ->
                newSiteSequencerDefinition.seqId == oldSiteSequencerDefinition.seqId
            }

            if (matchingNewDefinition != null) {
                if (matchingNewDefinition.modifiedBy.dateTime != oldSiteSequencerDefinition.modifiedBy.dateTime) {
                    // seq got updated
                    CcuLog.d(
                        SequencerParser.TAG_CCU_SITE_SEQUENCER,
                        "doing daily cleanUp modified time is different remove alerts"
                    )
                    fixAlerts(oldSiteSequencerDefinition)
                }
            } else {
                // seq got deleted
                // remove log file
                CcuLog.d(
                    SequencerParser.TAG_CCU_SITE_SEQUENCER,
                    "doing daily cleanUp seq got removed remove alerts"
                )
                fixAlerts(oldSiteSequencerDefinition)
                val fileName =
                    "seq_" + CCUHsApi.getInstance().ccuId + "_" + oldSiteSequencerDefinition.seqId + ".json"
                SequencerLogUtil.deleteJsonFile(CCUHsApi.getInstance().context, fileName)
            }
            SequencerSchedulerUtil.cancelIntentsForSeqId(haystack.context, oldSiteSequencerDefinition.seqId)
        }
    }

    fun fixAlerts(sequencerDefinition: SiteSequencerDefinition) {
        CcuLog.d(SequencerParser.TAG_CCU_SITE_SEQUENCER, "fix sequencer alerts -- 1")
        Objects.requireNonNull<ArrayList<SequenceAlert>>(sequencerDefinition.seqAlerts).forEach(
            Consumer { sequenceAlert: SequenceAlert? ->
                AlertManager.getInstance()
                    .fixAlertByDef(
                        createAlertDefinition(
                            sequenceAlert!!
                        )
                    )
            })
    }

    fun cleanUpAlerts(sequencerDefinition: SiteSequencerDefinition) {
        CcuLog.d(SequencerParser.TAG_CCU_SITE_SEQUENCER, "removing sequencer alerts -- 1")
        Objects.requireNonNull<ArrayList<SequenceAlert>>(sequencerDefinition.seqAlerts).forEach(
            Consumer { sequenceAlert: SequenceAlert? ->
                AlertManager.getInstance()
                    .deleteAlertsForDef(
                        createAlertDefinition(
                            sequenceAlert!!
                        )
                    )
            })
    }

    fun removePendingIntent(seqId: String) {
        CcuLog.d(SequencerParser.TAG_CCU_SITE_SEQUENCER, "removePendingIntent")
        SequencerSchedulerUtil.cancelIntentsForSeqId(haystack.context, seqId)
    }

    private fun saveDefs() {
        dataStore.saveSiteSequencerDefinitions(getSequencerDefinitions())
    }
}
