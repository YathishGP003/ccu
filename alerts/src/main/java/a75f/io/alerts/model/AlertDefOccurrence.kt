package a75f.io.alerts.model

import a75f.io.alerts.AlertBuilder
import a75f.io.alerts.AlertDefinition
import a75f.io.alerts.AlertFormatter
import a75f.io.api.haystack.Alert
import a75f.io.api.haystack.CCUHsApi

/**
 * @author tcase@75f.io
 * Created on 4/22/21.
 */
/** An instance of an alert def being tested, for the entire system or a specific zone containing the noted point*/
data class AlertDefOccurrence(
   val alertDef: AlertDefinition,
   val isMuted: Boolean,
   val testPositive: Boolean,
   val evaluationString: String,
   val pointId: String? = null,
   val equipRef: String? = null,
) {
   val key = AlertsDefStateKey(alertDef.alert.mTitle, equipRef)

   fun toAlert(haystack: CCUHsApi): Alert {
      return if (pointId != null)
         AlertBuilder.build(alertDef, AlertFormatter.getFormattedMessage(alertDef, pointId), haystack,
            equipRef,
            pointId)
      else
         AlertBuilder.build(alertDef, AlertFormatter.getFormattedMessage(alertDef), haystack)
   }
}

/** Current state (progress) in the system of one alert def occurrance */
data class AlertDefOccurrenceState(
   val occurrence: AlertDefOccurrence,
   val progress: AlertDefProgress
)

/**
 * Alert def occurrences must have a unique alert def title + equipId (possibly) null, to be separate
 * from an existing alert def occurence.
 */
data class AlertsDefStateKey(
   val title: String,
   val equipId: String?
)
