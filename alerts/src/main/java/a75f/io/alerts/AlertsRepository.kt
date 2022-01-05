package a75f.io.alerts

import a75f.io.alerts.cloud.AlertsService
import a75f.io.alerts.model.*
import a75f.io.alerts.model.AlertDefProgress.Partial
import a75f.io.api.haystack.Alert
import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.joda.time.DateTime
import java.util.concurrent.TimeUnit

/**
 * Gateway to Alerts and AlertDefinitions data, business logic processing, and backend alerservice.
 *
 * @author tcase@75f.io
 * Created on 4/13/21.
 */
class AlertsRepository(
   private val dataStore: AlertsDataStore,
   private val alertProcessor: AlertProcessor,
   private val alertsService: AlertsService,
   private val alertSyncHandler: AlertSyncHandler,
   private val haystack: CCUHsApi
) {

   // cache of alert definitions
   private var _alertDefsMap: AlertDefsMap =
      dataStore.getAlertDefinitions()
         .associateBy { it.alert.mTitle }
         .toMutableMap()

   // provide synchronized access to our in-memory store of alert defs.  Use this to read alert defs.
   private val alertDefsMap: AlertDefsMap
      get() = synchronized(_alertDefsMap) { _alertDefsMap }

   // current state of alert definition instances (i.e. positive tests)
   private val alertDefsState : AlertDefsState = mutableMapOf()

   // Rx disposable for clearing
   private var fetchDisposable: Disposable? = null
   // results of AlertProcessor evaluation saved to report state for debugging screen.
   private var alertDefOccurrences: List<AlertDefOccurrence> = emptyList()

   init {
      fetchAlertsDefinitions()
   }

   /////   Alert Definitions   /////

   fun getAlertDefinitions(): List<AlertDefinition> = alertDefsMap.values.toList()

   fun deleteAlertDefinition(id: String) {
      val title = alertDefsMap.findTitleById(id)
      if (title == null) {
         CcuLog.w("CCU_ALERTS", "Could not find alert definition for id to delete: $id")
         return
      }
      val alertDef = alertDefsMap.remove(title)
      alertDef?.let {
         alertDefsState.removeAll(alertDef)
         dataStore.deleteAlertsForDef(alertDef)
      }
      saveDefs()
   }

   fun fetchAlertsDefinitions() {
      if (! haystack.siteSynced()) {
         return
      }
      val siteId = haystack.siteIdRef.toVal()

      fetchDisposable = alertsService.getSiteDefinitions(siteId)
         .subscribeOn(Schedulers.io())
         .map { it.data }
         .subscribe(
            { alertDefs -> handleRetrievedDefsAlerts(alertDefs) },
            { error -> CcuLog.e("CCU_ALERTS", "Unexpected error fetching or parsing site definitions.", error) }
         )
   }

   fun fetchAlertDefssIfEmpty() {
      if (alertDefsMap.isEmpty()) {
         fetchAlertsDefinitions()
      }
   }

   fun addAlertDefinition(alertDef: AlertDefinition) {
      alertDefsMap[alertDef.alert.mTitle] = alertDef
      saveDefs()
   }

   private fun AlertDefsMap.findTitleById(id: String) = values.find { it._id == id }?.alert?.mTitle


   /////  Alerts   /////

   fun getActiveAlerts() = dataStore.getActiveAlerts()

   fun getAllAlertsOldestFirst() = dataStore.getAllAlertsOldestFirst()

   fun getActiveCMDeadAlerts() = dataStore.getActiveCMDeadAlerts()

   fun getActiveDeviceDeadAlerts() = dataStore.getActiveDeviceDeadAlerts()

   fun getUnsyncedAlerts() = dataStore.getUnSyncedAlerts()

   /**
    * @return Looks like this returns all alerts with severity not equal to an INTERNAL status
    */
   fun getAllAlertsNotInternal(): List<Alert> = dataStore.getAllAlertsNotInternal()

   fun addAlert(alert: Alert) {
      CcuLog.d("CCU_ALERTS", "Add Alert!  $alert")
      dataStore.addAlertIfUnique(alert)
   }

   // "Resolve" an alert
   fun fixAlert(alert: Alert) {
      CcuLog.d("CCU_ALERTS", "Fix Alert!")
      alert.setEndTime(DateTime().millis)
      alert.setFixed(true)
      alert.setSyncStatus(false)
      dataStore.updateAlert(alert)
      alertDefsState.remove(alert)

      // special handling in preferences for "CCU RESTART" alert.  Is that restart alert, or that it was restarted?
      if (alert.mTitle.equals("CCU RESTART", ignoreCase = true)) {
         dataStore.cancelAppRestarted()
      }
   }

   fun deleteAlert(alert: Alert): Completable? {
      CcuLog.d("CCU_ALERTS", "Delete alert! $alert")

      //_id is empty if the alert is not synced to backend.
      return if (alert._id == "") {
         CcuLog.w("CCU_ALERTS", "empty global Id; just remove in alertBox")
         removeAlert(alert)
         Completable.complete()
      } else {
         alertSyncHandler.delete(alert._id)
            .doOnComplete { removeAlert(alert) }
            .doOnError { throwable: Throwable? -> CcuLog.w("CCU_ALERTS", "Delete alert failed " + alert._id, throwable) }
      }
   }

   private fun removeAlert(alert: Alert) {
      dataStore.deleteAlert(alert)
      alertDefsState.remove(alert)
   }


   fun deleteAlertInternal(id: String) {
      val alert = dataStore.getAlert(id)
      alert?.let { dataStore.deleteAlert(alert._id) }

   }

   /**
    * Generates an alert and puts it in our data store IF
    *  -- there is an existing alert definition with matching title
    *        -- that is enabled, and
    *        -- does NOT have a matching message.
    *
    * I.e. This will only send one alert for any message, until restart.  (existing logic).  Used for interal alerts.
    */
   fun generateAlert(title: String, msg: String) {
      val alertDef = alertDefsMap[title]

      if (alertDef == null) {
         CcuLog.w("CCU_ALERTS", "In generateAlert(), no alert definition found for $title")
         return
      }
      val ccuId = haystack.ccuRef.toVal()

      if (alertDef.alert.ismEnabled() && !alertDef.isMuted(ccuId, null)) {
         alertDef.alert.setmMessage(msg)
         alertDef.alert.setmNotificationMsg(msg)
         addAlert(AlertBuilder.build(alertDef, AlertFormatter.getFormattedMessage(alertDef), haystack))
      }
   }

   fun generateCMDeadAlert(title: String, msg: String?) {
      if (dataStore.getActiveCMDeadAlerts().isNotEmpty()) {
         return
      }
      generateAlert(title, msg ?: "")
   }


   /////   Processing   /////

   fun processAlertDefs() {

      val alertDefs = getAlertDefinitions()

      // evaluate and raise alert conditions from alert defs
      alertDefOccurrences = alertProcessor.evaluateAlertDefinitions(alertDefs)

      // update overall state of raised alerts
      alertDefsState += alertDefOccurrences

      CcuLog.d("CCU_ALERTS", "New AlertDefsState = ${alertDefsState.niceString()}")

      // update active alerts with new state
      val alertsStateChange = dataStore.getActiveAlerts() - alertDefsState

      CcuLog.d("CCU_ALERTS", "New AlertsState = $alertsStateChange")

      // commit changes
      alertsStateChange.newAlerts
         .map { occurrence ->  occurrence.toAlert(haystack) }
         .forEach { alert -> addAlert(alert)
      }
      alertsStateChange.newlyFixedAlerts.forEach { alert -> fixAlert(alert) }

      // check for alert time-out
      clearElapsedAlerts()

      // sync
      syncAlerts()
   }

   // data structure used by AlertDefs dev settings fragment
   fun getOffsetCounter(): Map<AlertDefOccurrence, Int> {
      val mapping = mutableMapOf<AlertDefOccurrence, Int>()

      alertDefsState
         .filter { it.value.progress is Partial }
         .forEach { mapping[it.value.occurrence] = (it.value.progress as Partial).occurrenceCount }

      return mapping
   }

   // also used by AlertDefs dev settings fragment
   fun getCurrentOccurrences() = alertDefOccurrences


   /////   private   /////

   /**
    * Called from processAlerts.
    * Clears all alerts older than 24 hours
    * Clears mcError alerts after 1 hour.
    */
   private fun clearElapsedAlerts() {
      val alertList = dataStore.getAllAlertsNotInternal()
      for (a in alertList) {
         //clear alerts after 24 hours if alert is synced
         val alertIsSynced = a.syncStatus
         val alertIsFixed = a.isFixed
         val alertIsStale = System.currentTimeMillis() - a.getEndTime() >= TimeUnit.DAYS.toMillis(1)

         if (alertIsFixed && alertIsStale && alertIsSynced) {
            removeAlert(a)
         }
      }

      val cmErrorAlertList = dataStore.getCmErrorAlerts()
      for (a in cmErrorAlertList) {
         //clear alerts after every hours
         if (System.currentTimeMillis() - a.getStartTime() >= 3600000) {
            removeAlert(a)
         }
      }
   }

   // syncs all alerts in data store with syneced == false
   private fun syncAlerts() {
      // safety check
      if (!CCUHsApi.getInstance().isCCURegistered) {
         return
      }

      val unsyncedAlerts: List<Alert> = getUnsyncedAlerts()
      CcuLog.d("CCU_ALERTS", "${unsyncedAlerts.size} alerts to sync")

      if (unsyncedAlerts.isNotEmpty()) {
         alertSyncHandler.sync(unsyncedAlerts, dataStore)
      }
   }

   private fun handleRetrievedDefsAlerts(retrievedAlertDefs: List<AlertDefinition>) {

      synchronized(_alertDefsMap) {
         _alertDefsMap.clear()
         _alertDefsMap.putAll(retrievedAlertDefs.associateBy { it.alert.mTitle })

         //log
         CcuLog.d("CCU_ALERTS", "Fetched ${_alertDefsMap.size} Predefined alerts")
         _alertDefsMap.values.forEach {
            CcuLog.d("CCU_ALERTS", "Predefined alertDef Fetched: $it")
         }

         saveDefs()
      }
   }

   private fun saveDefs() {
      dataStore.saveAlertDefinitions(getAlertDefinitions().toArrayList())
   }

   fun close() {
      fetchDisposable?.dispose()
   }
}
