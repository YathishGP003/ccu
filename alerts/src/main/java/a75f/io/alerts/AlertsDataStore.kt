package a75f.io.alerts

import a75f.io.api.haystack.Alert
import a75f.io.api.haystack.Alert_
import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import android.content.Context
import org.apache.commons.lang3.StringUtils
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList

private const val PREFS_ALERT_DEFS = "ccu_alerts"
private const val PREFS_ALERTS_PREDEFINED = "predef_alerts"
private const val PREFS_ALERTS_APP_RESTART = "app_restart_alerts"


/**
 * This class manages all data storage for the Alerts subsystem, storing both Alerts and Alert
 * definitions to disc.  It hides the implementation (currently, prefs and object box).
 *
 * Currently provides only a synchronous interface, which is currently the norm across the app for
 * disc storage.
 *
 * @author tcase@75f.io
 * Created on 4/13/21.
 */
class AlertsDataStore @JvmOverloads constructor(
   private val context: Context,
   private val haystack: CCUHsApi = CCUHsApi.getInstance(),
   private val parser: AlertParser = AlertParser()
) {

   private val alertsSharedPrefs = context.getSharedPreferences(PREFS_ALERT_DEFS, android.content.Context.MODE_PRIVATE)

   private val boxStore = haystack.tagsDb.boxStore
   private val alertBox = boxStore.boxFor(Alert::class.java)


   fun clearAlert(alert: Alert) {
      alertBox.remove(alert.id)
   }

   fun getActiveAlerts(): List<Alert> {
      val alertQuery = alertBox.query()
      alertQuery.equal(Alert_.isFixed, false)
         .orderDesc(Alert_.startTime)
      return alertQuery.build().find()
   }

   fun getAlertDefinitions(): List<AlertDefinition> {
      val alertsDefString = alertsSharedPrefs.getString(PREFS_ALERTS_PREDEFINED, "")
      CcuLog.d("CCU_ALERTS", "Parsing Alerts:  $alertsDefString, with $parser")

      try {
         return if (StringUtils.isNotBlank(alertsDefString)) parser.parseAlertsString(alertsDefString) else ArrayList()
      } catch (ex: Exception) {
         CcuLog.w("CCU_ALERTS", "Unable to parsing alerts:  $alertsDefString")
         return ArrayList()
      }
   }

   fun saveAlertDefinitions(alertDefs: ArrayList<AlertDefinition>) {
      val alerts = parser.alertDefsToString(alertDefs)
      CcuLog.d("CCU_ALERTS", "Saving Alerts:  $alerts")
      alertsSharedPrefs.edit().putString(PREFS_ALERTS_PREDEFINED, alerts).apply()
   }

   fun appRestarted() {
      alertsSharedPrefs.edit().putBoolean(PREFS_ALERTS_APP_RESTART, true).apply()
   }

   fun deleteAlert(id: String) {
      val a = getAlert(id)
      if (a != null) {
         alertBox.remove(a.id)
      }
   }

   fun deleteAlert(alert: Alert) {
      alertBox.remove(alert)
   }

   fun cancelAppRestarted() {
      alertsSharedPrefs.edit().putBoolean(PREFS_ALERTS_APP_RESTART, false).apply()
   }

   fun isAppRestarted() =
      alertsSharedPrefs.getBoolean(PREFS_ALERTS_APP_RESTART, false)

   fun getActiveCMDeadAlerts(): List<Alert> {
      val alertQuery = alertBox.query()
      alertQuery.equal(Alert_.isFixed, false)
         .equal(Alert_.mTitle, "CM DEAD")
         .orderDesc(Alert_.startTime)
      return alertQuery.build().find()
   }

   fun getActiveDeviceDeadAlerts(): List<Alert> {
      val alertQuery = alertBox.query()
      alertQuery.equal(Alert_.isFixed, false)
         .equal(Alert_.mTitle, "DEVICE DEAD")
         .orderDesc(Alert_.startTime)
      return alertQuery.build().find()
   }


   /**
    * Database query for alerts with syncStatus false
    */
   fun getUnSyncedAlerts(): List<Alert> {
      val alertQuery = alertBox.query()
      alertQuery.equal(Alert_.syncStatus, false)
         .orderDesc(Alert_.startTime)
      return alertQuery.build().find()
   }

   /**
    * @return Looks like this returns all alerts with severity not equal to an INTERNAL status
    */
   fun getAllAlertsNotInternal(): List<Alert> {
      val alertQuery = alertBox.query()
      alertQuery.notEqual(Alert_.mSeverity, Alert.AlertSeverity.INTERNAL_INFO.ordinal.toLong())
      alertQuery.notEqual(Alert_.mSeverity, Alert.AlertSeverity.INTERNAL_LOW.ordinal.toLong())
      alertQuery.notEqual(Alert_.mSeverity, Alert.AlertSeverity.INTERNAL_MODERATE.ordinal.toLong())
      alertQuery.notEqual(Alert_.mSeverity, Alert.AlertSeverity.INTERNAL_SEVERE.ordinal.toLong())
      alertQuery.notEqual(Alert_.mSeverity, Alert.AlertSeverity.INTERNAL_ERROR.ordinal.toLong())
      alertQuery.notEqual(Alert_.mSeverity, Alert.AlertSeverity.INTERNAL_WARN.ordinal.toLong())
      alertQuery.orderDesc(Alert_.startTime)
      return alertQuery.build().find()
   }

   fun getAllAlerts(): List<Alert> {
      return alertBox.all
   }

   fun getAllAlertsOldestFirst(): List<Alert> {
      val alertQuery = alertBox.query()
      alertQuery.order(Alert_.startTime)
      return alertQuery.build().find()
   }

   fun getCmErrorAlerts(): List<Alert> {
      val alertQuery = alertBox.query()
      alertQuery.contains(Alert_.mTitle, "CM ERROR REPORT")
         .orderDesc(Alert_.startTime)
      return alertQuery.build().find()
   }

   fun getAlert(id: String): Alert? {
      val alertQuery = alertBox.query()
      alertQuery.equal(Alert_._id, id)
      return alertQuery.build().findFirst()
   }

   fun getAllAlerts(message: String): List<Alert> {
      val alertQuery = alertBox.query()
      alertQuery.equal(Alert_.mMessage, message)
         .orderDesc(Alert_.startTime)
      return alertQuery.build().find()
   }

   fun addAlertIfUnique(alert: Alert) {
      for (a in getActiveAlerts()) {
         if (a.mTitle == alert.mTitle && a.ref != null && a.ref == alert.ref && a.mMessage != null && a.mMessage == alert.mMessage) {
            return
         }
      }
      alertBox.put(alert)
   }

   fun updateAlert(alert: Alert) {
      alertBox.put(alert)
   }
}
