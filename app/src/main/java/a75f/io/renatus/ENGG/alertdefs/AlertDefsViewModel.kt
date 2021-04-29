package a75f.io.renatus.ENGG.alertdefs

import a75f.io.alerts.*
import a75f.io.alerts.model.AlertDefOccurrence
import a75f.io.api.haystack.Alert
import a75f.io.api.haystack.Alert.AlertSeverity.*
import a75f.io.renatus.R
import android.text.SpannableStringBuilder
import androidx.annotation.ColorRes
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.subjects.BehaviorSubject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for AlertDefinitions screen.
 * Gets all the AlertDef data and formats it for its presentation.
 *
 * @author tcase@75f.io
 * Created on 3/2/21.
 */
class AlertDefsViewModel: ViewModel() {

   val viewState: BehaviorSubject<AlertDefsViewState> = BehaviorSubject.create()

   private lateinit var alertManager: AlertManager

   fun injectDependencies(alertMgr: AlertManager) {

      this.alertManager = alertMgr
      retrieveAlertDefs()
   }

   fun retrieveAlertDefs() {
      viewState.onNext(AlertDefsViewState(emptyList()))

      // Initialize empty map of AlertDefs to all their Alerts & Offset Status
      val occurrences = alertManager.alertDefOccurrences
      val alertDefsMap = occurrences
         .associateWithTo(mutableMapOf<AlertDefOccurrence, AlertDefStatus>(), { AlertDefStatus() })

      // Add current alerts to alertDefsMap, first to last so that older alerts are overwritten.
      alertManager.allAlertsOldestFirst
         .forEach { alert ->
            val alertDef = occurrences.findOccurrenceForAlert(alert)
            alertDef?.let {
               alertDefsMap[it] = alertDefsMap[it]!!.copy(latestAlert = alert)
            }
         }

      // Add offset counter values, if any, to alertDefsMap.
      alertManager.offsetCounter
         .forEach { (occurrence, count) ->
            val alertDef = occurrence.alertDef
            val offsetStatusStr = "Cond met: " + count.toString() + " of " + alertDef.offset + " min"
            val existingOccurrence = alertDefsMap[occurrence]
            if (existingOccurrence != null) {
               alertDefsMap[occurrence] = existingOccurrence.copy(offsetStatus = offsetStatusStr)
            } else {
               alertDefsMap[occurrence] = AlertDefStatus(offsetStatus = offsetStatusStr)
            }

            alertDefsMap[occurrence] = alertDefsMap[occurrence]!!.copy(offsetStatus = offsetStatusStr)
         }

      viewState.onNext(AlertDefsViewState(
         alertDefsMap.toList()
            .map { createViewRow(it) }
      ))
   }

   private fun List<AlertDefOccurrence>.findOccurrenceForAlert(alert: Alert): AlertDefOccurrence? {
      forEach {
         if ( it.alertDef.alert.mTitle == alert.mTitle &&
            (it.pointId == alert.ref || it.pointId == null)) {
            return it
         }
      }
      return null
   }

   private fun createViewRow(pair: Pair<AlertDefOccurrence, AlertDefStatus>): AlertDefRow {
      val occurrence = pair.first
      val status = pair.second
      val latestAlert = status.latestAlert
      val alertDef = occurrence.alertDef
      val severity = alertDef.alert.mSeverity
      val title = if (alertDef.custom) {
         "(c) " + alertDef.alert.mTitle
      } else {
         alertDef.alert.mTitle
      }

      var statusString = status.statusString()
      if (statusString.isBlank() || statusString == "Resolved") {
         if (occurrence.isMuted) statusString = "MUTED"
      }

      return AlertDefRow(
         title = title,
         notificationMsg = alertDef.alert.mNotificationMsg,
         isActive = latestAlert?.isActive ?: false,
         isEnabled = alertDef.alert.mEnabled,
         isFixed = latestAlert?.isFixed ?: false,
         alertPopup = status.latestAlert?.formatTimes(),
         isCustom = alertDef.custom,
         severity = severity.name.replace("INTERNAL_", "I/"),
         conditional = alertDef.conditionEvaluationText(),
         colorRes = severity.color(),
         status = statusString,
         evalString = occurrence.evaluationString
      )
   }

   private fun Alert.formatTimes(): String {
      return "Alert Generated at " + getFormattedDate(getStartTime()) +
            "\nAlert Fixed at "+getFormattedDate(getEndTime())
   }

   private fun getFormattedDate(millis: Long): String? {
      if (millis == 0L) {
         return ""
      }
      val sdf: DateFormat = SimpleDateFormat("dd MMM yyyy HH:mm")
      val date = Date(millis)
      return sdf.format(date)
   }
}

data class AlertDefStatus(
   val latestAlert: Alert? = null,
   val offsetStatus: String = ""
)

private fun AlertDefStatus.statusString(): String {
   if (latestAlert == null) return offsetStatus

   return if (latestAlert.isFixed) {
     if (offsetStatus.isNotEmpty()) {
        offsetStatus
     } else {
        "Resolved"
     }
   } else {
      "Active"
   }
}


@ColorRes fun Alert.AlertSeverity.color(): Int {
   return when (this) {
      SEVERE -> R.color.severity_severe
      ERROR -> R.color.severity_severe
      MODERATE -> R.color.severity_moderate
      WARN -> R.color.severity_moderate
      LOW -> R.color.severity_low
      INFO -> R.color.severity_info
      INTERNAL_SEVERE -> R.color.severity_severe
      INTERNAL_ERROR -> R.color.severity_severe
      INTERNAL_MODERATE -> R.color.severity_moderate
      INTERNAL_WARN -> R.color.severity_moderate
      INTERNAL_LOW -> R.color.severity_low
      INTERNAL_INFO -> R.color.severity_info
   }
}

data class AlertDefsViewState(
   val alertDefRows: List<AlertDefRow>
) {
   val totalDefs = alertDefRows.size.toString()
   val activeCount = alertDefRows.count { it.isActive }.toString()
   val fixedCount = alertDefRows.count { it.isFixed }.toString()
   val inProcessCount = alertDefRows.count { it.status.isNotEmpty() && !it.isActive && it.status != "Resolved" }.toString()
}

data class AlertDefRow(
   val title: String,
   val notificationMsg: String = "",
   val isEnabled: Boolean = true,
   val isMuted: Boolean = false,
   val isCustom: Boolean = false,
   val isActive: Boolean = false,
   val isFixed: Boolean = false,
   val alertPopup: String? = null,
   val severity: String,
   val conditional: SpannableStringBuilder,
   @ColorRes val colorRes: Int,
   val status: String,
   val evalString: String
)
