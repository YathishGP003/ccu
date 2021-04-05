package a75f.io.renatus.ENGG.alertdefs

import a75f.io.alerts.AlertDefinition
import a75f.io.alerts.AlertManager
import a75f.io.alerts.conditionEvaluationText
import a75f.io.api.haystack.Alert
import a75f.io.api.haystack.Alert.AlertSeverity.*
import a75f.io.renatus.R
import android.text.SpannableStringBuilder
import androidx.annotation.ColorRes
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.subjects.BehaviorSubject

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
      // title Mapping
      viewState.onNext(AlertDefsViewState(emptyList()))

      val titleMapping = alertManager.alertDefinitions
         .associateBy { it.alert.mTitle }

      // Initialize empty map of AlertDefs to all their Alerts & Offset Status
      val alertDefsMap = alertManager.alertDefinitions
         .associateWithTo(mutableMapOf<AlertDefinition, AlertDefStatus>(), { AlertDefStatus() })

      // Add current alerts to alertDefsMap, first to last so that older alerts are overwritten.
      alertManager.allAlertsOldestFirst
         .forEach { alert ->
            val alertDef = titleMapping[ alert.mTitle ]
            alertDef?.let {
               alertDefsMap[it] = alertDefsMap[it]!!.copy(latestAlert = alert)
            }
         }

      // Add offset counter values, if any, to alertDefsMap.
      alertManager.offsetCounter
         .forEach { keyValue ->
            val title = keyValue.key
            var alertDef = titleMapping[ title ]

            if (alertDef == null) {
               val key = titleMapping.keys.firstOrNull { title.startsWith(it) } ?: return@forEach
               alertDef = titleMapping[ key ]
            }

            alertDef?.let {
               val status = alertDefsMap[it]
               val offsetStatusStr = "Cond met: " + keyValue.value.toString() + " of " + it.offset + " min"
               alertDefsMap[it] = status!!.copy(offsetStatus = offsetStatusStr)
            }
         }

      viewState.onNext(AlertDefsViewState(
         alertDefsMap
            .toList()
            .map { createViewRow(it)  }
         )
      )
   }

   private fun createViewRow(pair: Pair<AlertDefinition, AlertDefStatus>): AlertDefRow {
      val alertDef = pair.first
      val severity = alertDef.alert.mSeverity
      val title = if (alertDef.custom) {
         "(c) " + alertDef.alert.mTitle
      } else {
         alertDef.alert.mTitle
      }

      return AlertDefRow(
         title = title,
         notificationMsg = alertDef.alert.mNotificationMsg,
         isActive = pair.second.latestAlert?.isActive ?: false,
         isFixed = pair.second.latestAlert?.isFixed ?: false,
         isCustom = alertDef.custom,
         severity = severity.name.replace("INTERNAL_","I/"),
         conditional = alertDef.conditionEvaluationText(),
         colorRes = severity.color(),
         status = pair.second.statusString()
      )
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
   val severity: String,
   val conditional: SpannableStringBuilder,
   @ColorRes val colorRes: Int,
   val status: String
)
