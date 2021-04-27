@file:JvmName("AlertMessageHandlers")

package a75f.io.logic.pubnub

import a75f.io.alerts.AlertManager
import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.cloud.RenatusServicesEnvironment
import com.google.common.annotations.VisibleForTesting
import com.google.gson.JsonObject
import io.reactivex.rxjava3.schedulers.Schedulers

const val CREATE_CUSTOM_ALERT_DEF_CMD = "newCustomAlertDefinition"

// TODO: resolve discrepancy
const val UPDATE_CUSTOM_ALERT_DEF_CMD = "updateCustomAlertDefinition"
const val DELETE_CUSTOM_ALERT_DEF_CMD = "removeCustomAlertDefinition"

const val DELETE_SITE_DEFS_CMD = "removeCustomAlertDefinitions"

const val CREATE_PREDEFINED_ALERT_DEF_CMD = "newPredefinedAlertDefinition"
const val UPDATE_PREDEFINED_ALERT_DEF_CMD = "updatePredefinedAlertDefinition"
const val DELETE_PREDEFINED_ALERT_DEF_CMD = "removePredefinedAlertDefinition"


const val REMOVE_ALERT_CMD = "removeAlert"
const val REMOVE_ALERTS_CMD = "removeAlerts"

@VisibleForTesting
const val KEY_ALERT_DEF_ID = "definitionId"

private const val KEY_ALERT_DEF_IDS = "definitionIds"
@VisibleForTesting
const val KEY_ALERT_ID = "alertId"
private const val KEY_ALERT_IDS = "alertIds"

//@Singleton
class AlertMessageHandler(
   private val alertManager: AlertManager,
   private val haystackApi: CCUHsApi                        // we inject haystack not global site id b/c site could change (during registration).
) {

   companion object {
      // we use this method until we implement dependency injection.
      @JvmStatic
      fun instanceOf() = AlertMessageHandler(
         AlertManager.getInstance(),
         CCUHsApi.getInstance()
      )
   }

   /**
    * Handles the alert definition pubnub messages -- create and update.
    * Gets the alert definition from the service and saved it if valid.
    *
    * Follows the exact logic of previous AlertDefinitionHandler, except:
    *  - uses AlertService (v2) instead of HttpUtil (v1)
    *  - needs siteId for v2
    *  - gets a single alertDef instead of a List
    *  - logs more errors.
    *
    * @author tcase@75f.io
    * Created on 3/10/21.
    */
   fun handleCustomAlertDefMessage(msgObject: JsonObject) {

      val env = RenatusServicesEnvironment.instance
      val alertGUID = msgObject[KEY_ALERT_DEF_ID]?.asString
         ?: return CcuLog.e(L.TAG_CCU_PUBNUB, "No $KEY_ALERT_DEF_ID in $CREATE_CUSTOM_ALERT_DEF_CMD message")

      val siteId = haystackApi.siteIdRef?.toVal()
         ?: return CcuLog.w(L.TAG_CCU_PUBNUB, "No siteId in handle alert def message")

      // We get the alertService each time rather than save it as a member variable because it
      // could change when endpoints base url changes, like when user changes local IP.
      if (!alertManager.hasService()) {
         CcuLog.w(L.TAG_CCU_PUBNUB, "No alerts service, so CCU cannot get new custom definition.")
         return
      }
      val alertsService = alertManager.alertsService

      // Note: Following existing logic, we are not saving the ref to the disposable here.  We should, if
      // we were to follow best practice to a T, keep the disposable and dispose it if the app needs to exit
      // while this logic is in flight.
      alertsService.getCustomDefinition(siteId, alertGUID)
         .subscribeOn(Schedulers.io())
         .subscribe(
            { alertDef ->
               if (alertDef.validate()) {
                  alertManager.addAlertDefinition(alertDef)
                  CcuLog.d(L.TAG_CCU_PUBNUB, "Fetched Alert Definition $alertDef")
               } else {
                  CcuLog.d(L.TAG_CCU_PUBNUB, "Invalid Alert Definition $alertDef")
               }
            },
            { error -> CcuLog.w(L.TAG_CCU_PUBNUB, "Error fetching alert def from server", error) }
         )
   }

   /**
    * Handles the predefined alert definition pubnub messages -- create, update and delete.
    *
    * Delegates to AlertManager, which simply refreshes all predefined alerts.
    */
   fun handlePredefinedAlertDefMessage(msgObject: JsonObject) {
      alertManager.fetchPredefinedAlerts()
      CcuLog.d(L.TAG_CCU_PUBNUB, "Predefined alert definitions fetched.")
   }

   /**
    * Handles the alert removal pubnub messages: one alert or multiple.
    *
    * Removes them from the CCU internally.
    */
   fun handleAlertRemoveMessage(cmd: String, msgObject: JsonObject) {
      try {
         val ids = when (cmd) {
            REMOVE_ALERT_CMD -> listOf(msgObject.getAsJsonPrimitive(KEY_ALERT_ID).asString)
            REMOVE_ALERTS_CMD -> msgObject.getAsJsonArray(KEY_ALERT_IDS).map { it.asString }
            else -> throw IllegalStateException("non Remove Alert cmd in AlertRemoveHandler")
         }
         for (id in ids) {
            alertManager.deleteAlertInternal(id)
            CcuLog.d(L.TAG_CCU_PUBNUB, " Deleted Alert: $id")
         }
      } catch (e: Exception) {
         e.printStackTrace()
         CcuLog.d(L.TAG_CCU_PUBNUB, " Failed to parse removeEntity Json $msgObject")
      }
   }

   fun handleAlertDefRemoveMessage(cmd: String, msgObject: JsonObject) {
      val ids = when (cmd) {
         DELETE_CUSTOM_ALERT_DEF_CMD -> listOf(msgObject.getAsJsonPrimitive(KEY_ALERT_DEF_ID).asString)
         DELETE_SITE_DEFS_CMD -> msgObject.getAsJsonArray(KEY_ALERT_DEF_IDS).map { it.asString }
         else -> throw IllegalStateException("non Remove Alert cmd in AlertRemoveHandler")
      }
      for (id in ids) {
         alertManager.deleteAlertDefinition(id)
         CcuLog.d(L.TAG_CCU_PUBNUB, " Deleted Alert Definition: $id")
      }
   }
}
