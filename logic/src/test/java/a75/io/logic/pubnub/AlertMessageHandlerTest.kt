package a75.io.logic.pubnub

import a75f.io.alerts.AlertDefinition
import a75f.io.alerts.AlertManager
import a75f.io.alerts.cloud.AlertsService
import a75f.io.alerts.cloud.ServiceGenerator
import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.pubnub.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.rxjava3.core.Single
import org.junit.Test


private const val SITE_ID = "604a0244c33dcc5059849ba0"

/**
 * Unit test for AlertMessageHandler.  Most of the test is in verification of behavior.  We are verify that
 * the appropriate AlertManager methods are called, and, in one case, the appropriate AlertService call.
 *
 * Since this test is a bit implementation based, it will be easy to break.  But that's Ok and this is
 * still valid.
 *
 * @author tcase@75f.io
 * Created on 3/16/21.
 */
class AlertMessageHandlerTest {

   // set up alert service & service generator, returning our mock AlertDefinition
   private val retrievedAlertDef = Gson().getAdapter(AlertDefinition::class.java).fromJson(alertDefJson)
   private val mockAlertsService = mockk<AlertsService>() {
      every { getCustomDefinition(any(), any()) } returns Single.just(retrievedAlertDef)
   }
   // a haystack API that returns our site id
   private val haystackApi = mockk<CCUHsApi>() {
      // JJG | 11-18-21
      // Comment'd out the below line. This value no longer exists and I have no idea what it means.
      // Other unit tests could not be ran while this error remained.
//      every { globalSiteIdNoAtSign } returns SITE_ID
   }

   @Test
   fun `handle get custom alert def message`() {

      // Mock alert manager for use in alertService.getCustomDefinition
      val alertManager = mockk<AlertManager>() {
         every { addAlertDefinition(any())} returns Unit
      }

      // Object under test
      val alertMessageHandler = AlertMessageHandler(alertManager, haystackApi)

      // When..
      val message = Gson().fromJson(getAlertDefJson, JsonObject::class.java)
      val defId = message.get(KEY_ALERT_DEF_ID).asString
      alertMessageHandler.handleCustomAlertDefMessage(message)

      // Then..
      verify(exactly = 1) {
         mockAlertsService.getCustomDefinition(SITE_ID, defId)
      }
      verify {
         alertManager.addAlertDefinition(retrievedAlertDef)
      }
   }

   @Test
   fun `handle delete custom alert def message`() {

      // Mock alert manager and we need to specity the deleteAlertDef method since it's called in our handler
      val alertManager = mockk<AlertManager> {
         every { deleteAlertDefinition(any())} returns Unit
      }

      // Object under test
      val alertMessageHandler = AlertMessageHandler(alertManager, haystackApi)

      // When..
      val message = Gson().fromJson(removeAlertDefJson, JsonObject::class.java)
      val defId = message.get(KEY_ALERT_DEF_ID).asString
      alertMessageHandler.handleAlertDefRemoveMessage(DELETE_CUSTOM_ALERT_DEF_CMD, message)

      // Then..
      verify {
         alertManager.deleteAlertDefinition(defId)
      }
   }

   // We just expect alertManager.fetchAllPredefinedAlerts() to be called when any predefinedAlertsDef
   // message comes through.
   @Test
   fun `handle predefined alert def message`() {

      // Mock alert manager for use in alertService.getCustomDefinition
      val alertManager = mockk<AlertManager>() {
         every { fetchPredefinedAlerts()} returns Unit
      }

      // Object under test
      val alertMessageHandler = AlertMessageHandler(alertManager, haystackApi)

      // When..
      val message = Gson().fromJson(getPredefinedAlertDefJson, JsonObject::class.java)
      alertMessageHandler.handlePredefinedAlertDefMessage(message)

      // Then..
      verify {
         alertManager.fetchPredefinedAlerts()
      }
   }

   @Test
   fun `handle remove alert message`() {

      // Mock alert manager for use in alertService.getCustomDefinition
      val alertManager = mockk<AlertManager>() {
         every { deleteAlertInternal(any())} returns Unit
      }

      // Object under test
      val alertMessageHandler = AlertMessageHandler(alertManager, haystackApi)

      // When..
      val message = Gson().fromJson(removeAlertJson, JsonObject::class.java)
      val defId = message.get(KEY_ALERT_ID).asString
      alertMessageHandler.handleAlertRemoveMessage(REMOVE_ALERT_CMD, message)

      // Then..
      verify {
         alertManager.deleteAlertInternal(defId)
      }
   }


   /////     Mock data     /////

   private val getAlertDefJson = """
      {"definitionId":"604a07b078459c12d1bb679a","command":"newCustomAlertDefinition"}
   """

   private val removeAlertDefJson = """
      {"definitionId":"604a07b078459c12d1bb679a","command":"removeCustomAlertDefinition"}
   """

   private val getPredefinedAlertDefJson = """
      {"definitionId":"604a07b078459c12d1bb679a","command":"newPredefinedAlertDefinition"}
   """

   private val removeAlertJson = """
      {"alertId":"60-alertId","command":"removeAlert"}
   """
}

private const val alertDefJson = """
      {
         "_id": "604a07b078459c12d1bb679a",
         "siteRef": "604a0244c33dcc5059849ba0",
         "conditionals": [
             {
                 "order": "1",
                 "key": "zone and current and temp",
                 "value": "system and building and limit and min",
                 "grpOperation": "equip",
                 "condition": "<"
             },
             {
                 "order": "2",
                 "operator": "||"
             },
             {
                 "order": "3",
                 "key": "zone and current and temp",
                 "value": "system and building and limit and max",
                 "grpOperation": "equip",
                 "condition": ">"
             }
         ],
         "offset": "0",
         "alert": {
             "mAlertType": "CUSTOMER VISIBLE",
             "mTitle": "Tony3 temp breached on CCU [Warn]",
             "mMessage": "The battery level of your CCU [%s] has dropped below 75%% and is not charging. Please check that the tablet is secured to it's mount. if it is plugged in, please contact 75F support.",
             "mNotificationMsg": "The battery level of your CCU has dropped below 75% and is not charging. Please check that the tablet is secured to it's mount. If it is plugged in, please contact 75F support.",
             "mSeverity": "LOW",
             "mEnabled": true
         },
         "custom": true
      }
   """


