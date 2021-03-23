@file:JvmName("AlertDefinitionHandler")

package a75f.io.logic.pubnub

import a75f.io.alerts.AlertManager
import a75f.io.alerts.cloud.ServiceGenerator
import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import com.google.gson.JsonObject
import io.reactivex.rxjava3.schedulers.Schedulers

const val CMD = "alertDefinition"

/**
 * Handles the alert definition pubnub msg, which has one function, to
 * get the alert definition from the service and save it if it is valid.
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
fun handleMessage(msgObject: JsonObject) {

   val alertGUID = msgObject["alert_def_id"].asString
   val siteId = CCUHsApi.getInstance().globalSiteId?.removePrefix("@")
                  ?: return CcuLog.w(L.TAG_CCU_PUBNUB, "No siteId in handle alert def message")

   // Alert service will have been initialized at startup
   val alertsService = ServiceGenerator.instance.alertsService

   // Note: Following existing logic, we are not saving the ref to the disposable here.  We should, if
   // we were to follow best practice to a T, keep the disposable and dispose it if the app needs to exit
   // while this logic is in flight.
   alertsService.getCustomDefinition(siteId, alertGUID)
      .subscribeOn(Schedulers.io())
      .subscribe(
         { alertDef ->
            if (alertDef.validate()) {
               AlertManager.getInstance().addAlertDefinition(alertDef)
            } else {
               CcuLog.d(L.TAG_CCU_PUBNUB, "Invalid Alert Definition $alertDef")
            }
         },
         { error -> CcuLog.w(L.TAG_CCU_PUBNUB, "Error fetching alert def from server", error) }
      )
}
