package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.sync.HttpUtil
import a75f.io.constants.HttpConstants
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.messaging.BuildConfig
import a75f.io.messaging.MessageHandler
import a75f.io.util.DASHBOARD
import a75f.io.util.DashboardListener
import a75f.io.util.ExecutorTask
import a75f.io.util.isDashboardConfig
import a75f.io.util.saveConfigs
import a75f.io.util.setDashboardConfig
import android.content.Context
import com.google.gson.JsonObject
import org.json.JSONObject


/**
 * Created by Manjunath K on 16-01-2025.
 */

class DashboardHandler: MessageHandler {

    companion object {
        var dashboardListener: DashboardListener? = null
    }

    override val command: List<String> = listOf("updateDashboard")

    override fun handleMessage(jsonObject: JsonObject, context: Context) {
        CcuLog.d(DASHBOARD, "Handle dashboard $jsonObject")
        getDashboardConfiguration()
    }

    override fun ignoreMessage(jsonObject: JsonObject, context: Context): Boolean {
        return false
    }
}

fun getDashboardConfiguration() {
    val requestUrl = "${BuildConfig.TABLE_MAKER_API_BASE}customgraphicsCcu/details"
    var isConfigured = isDashboardConfig(Globals.getInstance().applicationContext)
    ExecutorTask.executeAsync({
        val response = HttpUtil.executeJson(
            requestUrl,
            null,
            CCUHsApi.getInstance().getJwt(),
            false,
            HttpConstants.HTTP_METHOD_GET
        )
        if (response.isNullOrEmpty()) {
            CcuLog.e(DASHBOARD, " no response found")
        } else {

            try {
                val responseObject = JSONObject(response)
                val config = responseObject.get("customGraphicsCcuView") as JSONObject
                val configString = config.getString("contentType")
                if (configString.isNullOrEmpty() || configString.contentEquals("NONE")) {
                    CcuLog.e(DASHBOARD, " Dashboard is not configured")
                    setDashboardConfig(false, Globals.getInstance().applicationContext)
                    isConfigured = false
                } else {
                    CcuLog.e(DASHBOARD, " Dashboard is configured")
                    setDashboardConfig(true, Globals.getInstance().applicationContext)
                    isConfigured = true
                }

                val dashboardConfiguration = JSONObject()
                dashboardConfiguration.put("environment", BuildConfig.BUILD_TYPE)
                dashboardConfiguration.put("config", responseObject)

                CcuLog.e(DASHBOARD, "Dashboard JSON response: $dashboardConfiguration")
                saveConfigs(dashboardConfiguration.toString(), Globals.getInstance().applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
                CcuLog.e(DASHBOARD, "Error parsing Dashboard JSON")
            }
        }
    }, {
        DashboardHandler.dashboardListener?.onDashboardConfigured(isConfigured)
    })
}



