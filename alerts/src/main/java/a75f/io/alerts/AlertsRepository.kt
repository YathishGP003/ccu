package a75f.io.alerts

import a75f.io.alerts.AlertProcessor.TAG_CCU_ALERTS
import a75f.io.alerts.AlertProcessor.TAG_CCU_DEV_DEBUG
import a75f.io.alerts.cloud.AlertsService
import a75f.io.alerts.model.AlertDefOccurrence
import a75f.io.alerts.model.AlertDefOccurrenceState
import a75f.io.alerts.model.AlertDefProgress
import a75f.io.alerts.model.AlertDefProgress.Partial
import a75f.io.alerts.model.AlertDefsMap
import a75f.io.alerts.model.AlertDefsState
import a75f.io.alerts.model.AlertsDefStateKey
import a75f.io.alerts.model.contains
import a75f.io.alerts.model.getActiveAlerts
import a75f.io.alerts.model.minus
import a75f.io.alerts.model.plusAssign
import a75f.io.alerts.model.remove
import a75f.io.alerts.model.removeAll
import a75f.io.alerts.model.toArrayList
import a75f.io.api.haystack.Alert
import a75f.io.api.haystack.Alert.AlertSeverity
import a75f.io.api.haystack.Alert_
import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.util.hayStack
import a75f.io.logger.CcuLog
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.joda.time.DateTime
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Gateway to Alerts and AlertDefinitions data, business logic processing, and backend alert service.
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

   private val boxStore = haystack.tagsDb.boxStore
   private val alertBox = boxStore.boxFor(Alert::class.java)

   init {
      fetchAlertsDefinitions()
   }

   /////   Alert Definitions   /////

   private fun getAlertDefinitions(): List<AlertDefinition> = alertDefsMap.values.toList()

   fun deleteAlertDefinition(id: String) {
      val title = alertDefsMap.findTitleById(id)
      if (title == null) {
         CcuLog.w(TAG_CCU_ALERTS, "Could not find alert definition for id to delete: $id")
         return
      }
      val alertDef = alertDefsMap.remove(title)
      alertDef?.let {
         alertDefsState.removeAll(alertDef)
         deleteAlertsDefWithFixingActiveAlerts(alertDef)
      }
      saveDefs()
      setAlertListChanged()
   }

   private fun deleteAlertsDefWithFixingActiveAlerts(alertDef: AlertDefinition) {

      val alertQuery = alertBox.query()
      val matchingAlerts = alertQuery
         .equal(Alert_.mTitle, alertDef.alert.mTitle)
         .build().find()
      var fixedAlertList = mutableListOf<Alert>()
      matchingAlerts.forEach {
            if (!it.isFixed) {
               fixedAlertList.add(it)
               fixAlert(it)
            }
            else {
               dataStore.deleteAlert(it)
            }
      }
      if (fixedAlertList.isNotEmpty()) {
         alertSyncHandler.sync(fixedAlertList, dataStore)
         fixedAlertList.forEach {
            dataStore.deleteAlert(it)
         }
      }
   }

   fun fetchAlertsDefinitions() {
      if (!haystack.siteSynced() || !haystack.authorised) {
         return
      }
      val siteId = haystack.siteIdRef.toVal()

      fetchDisposable = alertsService.getSiteDefinitions(siteId)
         .subscribeOn(Schedulers.io())
         .map { it.data }
         .subscribe(
            { alertDefs -> handleRetrievedDefsAlerts(alertDefs) },
            { error -> CcuLog.e(TAG_CCU_ALERTS, "Unexpected error fetching or parsing site definitions.", error) }
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

   fun getActiveSafeModeAlert() = dataStore.getActiveSafeModeAlert()

   fun getActiveCrashAlert() = dataStore.getActiveCrashAlert()

   fun getAlertsByCreator(creator: String): List<Alert> = dataStore.getAlertsByCreator(creator)

   fun getActiveAlertsByRef(deviceRef: String): List<Alert> = dataStore.getActiveAlertsByRef(deviceRef)

   fun getDeviceRebootActiveAlert(deviceRef: String) = dataStore.getDeviceRebootActiveAlert(deviceRef)

   /**
    * @return Looks like this returns all alerts with severity not equal to an INTERNAL status
    */
   fun getAllAlertsNotInternal(): List<Alert> = dataStore.getAllAlertsNotInternal()

   private fun addAlert(alert: Alert) {
      CcuLog.d(TAG_CCU_ALERTS, "Add Alert!  $alert")
      dataStore.addAlertIfUnique(alert)
   }

   // "Resolve" an alert
   fun fixAlert(alert: Alert) {

      alert.setEndTime(DateTime().millis)
      alert.setFixed(true)
      alert.setSyncStatus(false)
      dataStore.updateAlert(alert)
      alertDefsState.remove(alert)

      // special handling in preferences for "CCU RESTART" alert.  Is that restart alert, or that it was restarted?
      if (alert.mTitle.equals("CCU RESTART", ignoreCase = true)) {
         dataStore.cancelAppRestarted()
      }
      setAlertListChanged()
   }

   fun deleteAlert(alert: Alert): Completable? {
      CcuLog.d(TAG_CCU_ALERTS, "Delete alert! $alert")

      //_id is empty if the alert is not synced to backend.
      return if (alert._id == "") {
         CcuLog.w(TAG_CCU_ALERTS, "empty global Id; just remove in alertBox")
         removeAlert(alert)
         Completable.complete()
      } else {
         alertSyncHandler.delete(alert._id)
            .doOnComplete { removeAlert(alert) }
            .doOnError { throwable: Throwable? -> CcuLog.e(TAG_CCU_ALERTS, "Delete alert failed " + alert._id, throwable) }
      }
      setAlertListChanged()
   }

   private fun removeAlert(alert: Alert) {
      dataStore.deleteAlert(alert)
      alertDefsState.remove(alert)
      setAlertListChanged()
   }


   fun deleteAlertInternal(id: String) {
      val alert = dataStore.getAlert(id)
      alert?.let { dataStore.deleteAlert(alert._id) }
      setAlertListChanged()
   }

   /**
    * Generates an alert and puts it in our data store IF
    *  -- there is an existing alert definition with matching title
    *        -- that is enabled, and
    *        -- does NOT have a matching message.
    *
    * I.e. This will only send one alert for any message, until restart.  (existing logic).  Used for internal alerts.
    */
   fun generateAlert(title: String, msg: String, equipRef: String) {
      val alertDef = alertDefsMap[title]

      if (alertDef == null) {
         CcuLog.w(TAG_CCU_ALERTS, "In generateAlert(), no alert definition found for $title")
         return
      }
      val ccuId = haystack.ccuRef.toVal()

      if (alertDef.alert.ismEnabled() && !alertDef.isMuted(ccuId, null)) {
         alertDef.alert.setmMessage(msg)
         alertDef.alert.setmNotificationMsg(msg)
         val alert = AlertBuilder.build(alertDef, AlertFormatter.getFormattedMessage(alertDef,this), haystack,equipRef,null)
         if (isOtaAlert(title)){
            alert.setFixed(true)
            alert.setEndTime(DateTime().millis)
         }
         addAlert(alert)
      }
      setAlertListChanged()
   }

   fun generateAlertBlockly(title: String, msg: String, equipRef: String, creator: String, blockId: String) {
      val alertDef = alertDefsMap[title]

      if (alertDef == null) {
         CcuLog.w(TAG_CCU_ALERTS, "In generateAlert(), no alert definition found for $title")
         return
      }
      val ccuId = haystack.ccuRef.toVal()

      if (alertDef.alert.ismEnabled() && !alertDef.isMuted(ccuId, null)) {
         alertDef.alert.setmMessage(msg)
         alertDef.alert.setmNotificationMsg(msg)
         val alert = AlertBuilder.build(alertDef, AlertFormatter.getFormattedMessage(alertDef,this), haystack,equipRef,null)
         if (isOtaAlert(title)){
            alert.setFixed(true)
            alert.setEndTime(DateTime().millis)
         }
         alert.setCreator(creator)
         alert.setBlockId(blockId)
         addAlert(alert)
         setAlertListChanged()
      }
   }

   fun isOtaAlert(title: String): Boolean{
      return (title.contentEquals(FIRMWARE_OTA_UPDATE_STARTED) || title.contentEquals(FIRMWARE_OTA_UPDATE_ENDED)  )
   }


   fun generateCMDeadAlert(title: String, msg: String?) {
      if (dataStore.getActiveCMDeadAlerts().isNotEmpty()) {
         return
      }
      generateAlert(title, msg ?: "","")
   }


   /////   Processing   /////

   fun processAlertDefs() {

      val alertDefs = getAlertDefinitions()

      // evaluate and raise alert conditions from alert defs
      alertDefOccurrences = alertProcessor.evaluateAlertDefinitions(alertDefs)

      // update overall state of raised alerts
      alertDefsState += alertDefOccurrences


      // Fix internal event alerts (i.e. "CCU RESTART", "CM_REINIT", etc.) that have be
      //
      // en active for > 1 hour
      fixExpiredEventAlerts()

      // update active alerts with new state
      val alertsStateChange = dataStore.getActiveAlerts() - alertDefsState

      // commit changes
      alertsStateChange.newAlerts
         .map { occurrence ->  occurrence.toAlert(haystack,this) }
         .forEach { alert -> addAlert(alert)
      }
      alertsStateChange.newlyFixedAlerts.forEach { alert ->
         CcuLog.i(TAG_CCU_DEV_DEBUG, "Hard Fix: $alert")
              fixAlert(alert)
      }
      // check for alert time-out
      clearElapsedAlerts()

      // sync
      syncAlerts()
   }

   // data structure used by AlertDefs dev settings fragment0
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

   private fun fixExpiredEventAlerts() {
      val alerts = dataStore.getActiveInternalEventAlerts()
      CcuLog.i(TAG_CCU_ALERTS, "fixExpiredEventAlerts: $alerts")
      for (a in alerts) {
         if (!a.isFixed && (System.currentTimeMillis() - a.startTime) >= 3600000) {
            CcuLog.i(TAG_CCU_ALERTS, "Fixing expired event alert: $a")

            fixAlert(a)
         }
      }
      setAlertListChanged()
   }

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
      setAlertListChanged()
   }

   // syncs all alerts in data store with syneced == false
   private fun syncAlerts() {
      // safety check
      if (!CCUHsApi.getInstance().isCCURegistered) {
         return
      }

      //do not sync in offlineMode
      if (CCUHsApi.getInstance().readDefaultVal("offline and mode") > 0) {
         return
      }

      val unsyncedAlerts: List<Alert> = getUnsyncedAlerts()
      CcuLog.d(TAG_CCU_ALERTS, "${unsyncedAlerts.size} alerts to sync")

      if (unsyncedAlerts.isNotEmpty()) {
         if (CCUHsApi.getInstance().authorised) {
            alertSyncHandler.sync(unsyncedAlerts, dataStore)
         }
      }
   }

   private fun handleRetrievedDefsAlerts(retrievedAlertDefs: List<AlertDefinition>) {

      synchronized(_alertDefsMap) {
         _alertDefsMap.clear()
         _alertDefsMap.putAll(retrievedAlertDefs.associateBy { it.alert.mTitle })

         //log
         CcuLog.d(TAG_CCU_ALERTS, "Fetched ${_alertDefsMap.size} Predefined alerts")
         _alertDefsMap.values.forEach {
            CcuLog.d(TAG_CCU_ALERTS, "Predefined alertDef Fetched: $it")
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

   fun generateCrashAlertWithMessage(title: String, msg: String?) {
      generateAlert(title, msg ?: "","")
   }

   fun removeAlertDefinition(id: String) {
      val titles = mutableListOf<String>()
      for (alertDef in alertDefsMap.values) {
         if (alertDef._id == id) {
            alertDef.alert?.mTitle?.let {
               titles.add(it)
            }
         }
      }
      CcuLog.w(TAG_CCU_ALERTS, "removing alert definitions from map - $titles")
      for (alertTitle in titles) {
         val alertDef = alertDefsMap.remove(alertTitle)
         alertDef?.let {
            alertDefsState.removeAll(alertDef)
         }
      }
      saveDefs()
      setAlertListChanged()
   }
   fun handleAlertBoxItemsExceedingThreshold() {
      if(alertBoxSizeAboveThreshold()) {
         val alertBox = hayStack.tagsDb.boxStore.boxFor(Alert::class.java)
         val query = alertBox.query()
         query.`in`(Alert_.mSeverity, intArrayOf(
            AlertSeverity.INTERNAL_INFO.ordinal,
            AlertSeverity.INTERNAL_LOW.ordinal,
            AlertSeverity.INTERNAL_MODERATE.ordinal,
            AlertSeverity.INTERNAL_SEVERE.ordinal,
            AlertSeverity.LOW.ordinal
         ))
         thread(start = true, name = "clearAlertItems") {
            val alertList = query.build().find()
            alertBox.remove(alertList)
         }
      }
   }

   private fun alertBoxSizeAboveThreshold() : Boolean {
      val threshold = 5000
      return dataStore.getAllAlerts().size > threshold
   }

   fun setRestartAppToTrue() {
      dataStore.appRestarted()
   }

   fun checkIfAppRestarted() : Boolean {
      return dataStore.isAppRestarted()
   }
/// This method is only for dynamically updating the list of alerts when we are in Alerts page
   private fun setAlertListChanged() {
      val instance = AlertManager.getInstance()
      if (instance.alertListListener != null ) {
         instance.alertListListener.onAlertsChanged()
      }
   }

   /*This method is used to put the active predefined and custom alerts stored in the database to the runtime alert list
   * This is called when the app is opened
   * If the alert belongs to the alert definition with groupType - alert, then it is not added to the runtime alert list
   * Such alerts are handled by the CCU only and no conditional is provided in the def.
   * We shall check if the device ref for the alert still exist in the database, if not we shall fix the alert for zone based device alerts
   */
   fun processAlertsOnAppOpen() {
      CcuLog.d(TAG_CCU_ALERTS, "Started processing predefined alerts and legacy custom alerts when app is opened")
      // get all active predefined and legacy custom alerts
      dataStore.getActivePredefinedAndLegacyCustomAlerts().forEach { alert ->
         // if the alert is a zone based device alert, check if the device ref still exists in the database, if not, fix the alert
         if(isZoneBasedDeviceAlert(alert)) {
            if(haystack.readId("id==@${alert.equipId}")==null) {
               CcuLog.d(TAG_CCU_ALERTS, "Device ref ${alert.equipId} does not exist in the database, fixing the alert with Title: ${alert.mTitle}")
               AlertManager.getInstance().fixAlert(alert)
            }
         }
         // otherwise for all the active alerts with groupType != 'alert', fetch them in the runtime variable alertDefsState in Raised State
         else {
            if (alertDefsMap.values.map { it._id }.toSet().contains(alert.alertDefId) && !alertDefsState.getActiveAlerts().contains(alert) && isGroupTypeNotAlert(alert)) {
               alertDefsState[AlertsDefStateKey(alert.mTitle, "@"+alert.equipId)] = AlertDefOccurrenceState(
                  AlertDefOccurrence(alertDefsMap[alert.mTitle]!!,
                     isMuted = false,
                     testPositive = true,
                     evaluationString = alert.mMessage,
                     pointId = alert.ref,
                     equipRef = "@"+alert.equipId
                  )
                  , AlertDefProgress.Raised(
                     timeRaised = DateTime(alert.startTime),
                     timeFixed = null))
            }
         }
      }
      CcuLog.d(TAG_CCU_ALERTS, "Finished processing predefined alerts and legacy custom alerts when app is opened")
   }

   // This method identifies if the alert is device based alert and specific to zones
   private fun isZoneBasedDeviceAlert(alert: Alert): Boolean {
      return alert.mTitle!=null && alert.mTitle in listOf(
         DEVICE_DEAD,
         DEVICE_REBOOT,
         DEVICE_LOW_SIGNAL,
         FIRMWARE_OTA_UPDATE_STARTED,
         FIRMWARE_OTA_UPDATE_ENDED
      )
   }

   // The alertDefinitions that have groupType as "alert" are evaluated directly by CCU based on conditions
   // that are not provided in the alert definitions.
   // Such alerts are not evaluated every minute by the CCU and so we shall not add them to the runtime alert list
   private fun isGroupTypeNotAlert(alert: Alert): Boolean {
         alertDefsMap[alert.mTitle]?.conditionals?.forEach {
         if(it.grpOperation == "alert") {
            return false
         }
      }
      return true
   }
}
