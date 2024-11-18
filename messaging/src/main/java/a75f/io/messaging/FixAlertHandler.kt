package a75f.io.messaging.handler

import a75f.io.alerts.AlertManager
import a75f.io.logger.CcuLog

import a75f.io.logic.L
import a75f.io.messaging.MessageHandler
import android.content.Context
import com.google.gson.JsonObject

class FixAlertHandler : MessageHandler {

    override val command: List<String> = listOf("fixAlert")

    override fun handleMessage(jsonObject: JsonObject, context: Context) {
        CcuLog.d(L.TAG_CCU_MESSAGING,"Handle fixAlert")

        if (jsonObject.has("id") ){
            val alertId = jsonObject.get("id").asString
            CcuLog.d(L.TAG_CCU_MESSAGING,"alert id: $alertId")
            AlertManager.getInstance().activeAlerts.forEach {
                CcuLog.d(L.TAG_CCU_MESSAGING,"every id: ${it._id}")
                if ((it._id).toString().equals(alertId)){
                    AlertManager.getInstance().fixAlertLocally(it)
                    CcuLog.d(L.TAG_CCU_MESSAGING,"Alert $alertId is fixed")
                }
            }


        }

    }

    override fun ignoreMessage(jsonObject: JsonObject, context: Context): Boolean {
        return false
    }

    companion object {
        const val CMD: String =  "fixAlert"
    }

}
