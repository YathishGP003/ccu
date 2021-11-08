package a75f.io.alerts.model

import a75f.io.alerts.AlertDefinition
import a75f.io.api.haystack.Alert
import a75f.io.logger.CcuLog
import java.lang.StringBuilder

/**
 * Miscellaneious data classes, typealiases, extensions, and utility functions for Alerts Kt
 *
 * @author tcase@75f.io
 * Created on 4/15/21.
 */

/** Map of alert def titles to alert defe */
typealias AlertDefsMap = MutableMap<String, AlertDefinition>

/** Map of keys (title+equipId) to full AlertDef occurrence state (alert def, occurrence, progress) */
typealias AlertDefsState = MutableMap<AlertsDefStateKey, AlertDefOccurrenceState>

/** Nice print out for debugging */
fun AlertDefsState.niceString(): String {
   val active = values.count { it.progress.isActive() }
   val fixed = values.count { it.progress.isFixed() }
   val inProgress = values.count { it.progress is AlertDefProgress.Partial }

   val sb = StringBuilder()
   sb.append("size: ").append(size)
      .append(",   active: ").append(active)
   this.filter { it.value.progress.isActive() }
      .forEach { (key, _) -> sb.append(" ").append(key.title) }
   sb.append(",   partial: ").append(inProgress)
   this.filter { it.value.progress is AlertDefProgress.Partial}
      .forEach { (key, value) -> sb.append(" ").append((value.progress as AlertDefProgress.Partial).occurrenceCount).append("  ").append(key.title) }
   sb.append(",   fixed: ").append(fixed)
   return sb.toString()
}

fun AlertDefsState.remove(alert: Alert) {
   val key = AlertsDefStateKey(alert.mTitle, alert.equipId)
   remove(key)
}

   /**
 * Call when an alert definition is to be removed from the system.
 * This removes all instances of it from AlertDefsState.
 */
fun AlertDefsState.removeAll(alertDefinition: AlertDefinition) {

   val matchingKeys = keys.filter { it.title == alertDefinition.alert.mTitle }
   matchingKeys.forEach { remove(it) }
}


/** Represents change (delta) in Alerts state resulting from one alerts def processing to another */
data class AlertsStateChange(
   val newAlerts: List<AlertDefOccurrence>,
   val newlyFixedAlerts: List<Alert>
)

/** Calculates and returns the difference between current alerts state (this) and would-be alerts state
   based on state of positive alert def occurrances  */
operator fun List<Alert>.minus(alertDefsState: AlertDefsState): AlertsStateChange {

   val newAlerts = alertDefsState.getActiveAlerts()
      .filter { ! this.contains(it) }
   val fixedAlerts = alertDefsState.getFixedAlerts()
      .filter { this.contains(it) } .map { this.find(it)!! }

   return AlertsStateChange(newAlerts = newAlerts, newlyFixedAlerts = fixedAlerts)
}

/** Finds a matching alert, if any, for an alert def occurrence*/
fun List<Alert>.find(alertDefOccurrence: AlertDefOccurrence): Alert? {
   val alertToCheck = alertDefOccurrence.alertDef.alert

   forEach { alert ->
      if (alert.mTitle == alertToCheck.mTitle
          && alert.equipId == alertDefOccurrence.equipRef?.replaceFirst("@","")) {
            return alert
      }
   }
   return null
}

/** Returns whether there is a matching alert for this alert def occurrence */
operator fun List<Alert>.contains(alertDefOccurrence: AlertDefOccurrence) =
   this.find(alertDefOccurrence) != null

/** Returns all alert def occurrences marked as fixed, i.e not occurring */
fun AlertDefsState.getFixedAlerts() = filter { it.value.progress.isFixed() } .map { it.value.occurrence }

/** Returns all alert def occurrences that have reach the state of active alert */
fun AlertDefsState.getActiveAlerts(): List<AlertDefOccurrence> = filter { it.value.progress.isActive() } .map { it.value.occurrence }

/** Calculates new alert definitions state by adding list of new occurrences to this current state.*/
operator fun AlertDefsState.plusAssign(occurrences: List<AlertDefOccurrence>) {

   // all the positive occurrences this time around, and their keys
   val positives = occurrences.filter { it.testPositive }
   val positiveKeys = positives.map { it.key }

   // initialize collections for use internal to this function
   val updates: AlertDefsState = mutableMapOf()
   val deletions: MutableList<AlertsDefStateKey> = mutableListOf()

   fun handleNewAlertOccurrence(alertDefOccurrence: AlertDefOccurrence) {
      val offset = alertDefOccurrence.alertDef.offset?.toInt() ?: 0
      if (offset <= 1) {
         updates.put(alertDefOccurrence.key, AlertDefOccurrenceState(alertDefOccurrence, AlertDefProgress.Raised()))
      } else {
         updates.put(alertDefOccurrence.key, AlertDefOccurrenceState(alertDefOccurrence, AlertDefProgress.Partial()))
      }
   }

   // for existing alert defs, i.e. in *this* collection
   forEach { (key, value) ->
      val alertDefOccurrence = value.occurrence
      val progress = value.progress

      // if the existing key is still among the positive occurrences...
      if ( key in positiveKeys ) {
         when {
            progress is AlertDefProgress.Partial -> {
               val offset = alertDefOccurrence.alertDef.offset.toInt()
               val count = progress.occurrenceCount + 1
               if (count >= offset) {
                  updates.put(key, value.copy(progress = progress.toRaised()))
               } else {
                  updates.put(key, value.copy(progress = progress.inc()))
               }
            }
            // it was fixed, but now it's occurring again
            progress.isFixed() -> handleNewAlertOccurrence(alertDefOccurrence)
            // was active, remains active
            else -> {} //ignore active alerts
         }
      } else {
         when {
            // was a raised alert, is no longer present:  Delete partial and fixed alerts
            progress is AlertDefProgress.Partial -> deletions.add(key)
            progress.isFixed() -> deletions.add(key)
            // And "resolve" active alert.
            progress.isActive() -> updates.put(key, value.copy(progress = progress.toFixed()))
         }
      }
   }

   // For newly raised alert defs
   positives.forEach { alertDefOccurrence ->
      if ( alertDefOccurrence.key !in keys) {
         handleNewAlertOccurrence(alertDefOccurrence)
      }
   }

   putAll(updates)
   deletions.forEach { remove(it) }
}

/** Convert any List to an ArrayList, simply returning this if it already an ArrayList.*/
fun <T> List<T>.toArrayList(): ArrayList<T> = if (this is ArrayList) this else ArrayList(this)

/* For future refactoring of Conditional
data class ConditionalDto(
   val order: Int,
   val operator: String,
   val key: String,
   val value: String,
   val condition: String,
   val grpOperation: String
)
*/





