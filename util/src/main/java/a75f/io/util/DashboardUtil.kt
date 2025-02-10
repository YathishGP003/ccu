package a75f.io.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Created by Manjunath K on 29-01-2025.
 */
const val DASHBOARD = "CCU_DASHBOARD"
const val IS_DASHBOARD_CONFIGURED = "isDashboardConfigured"
fun getPreferences(context: Context): SharedPreferences = context.getSharedPreferences("dashboard", Context.MODE_PRIVATE)

fun saveConfigs(jsonObject: String, context: Context) {
    try {
        val sharedPreferences = getPreferences(context)
        sharedPreferences.edit().putString("config", jsonObject).apply()
        Log.i(DASHBOARD, "Dashboard JSON saved to file")
    } catch (e: Exception) {
        e.printStackTrace()
        Log.e(DASHBOARD, "Error saving Dashboard JSON to file")
    }
}

fun isDashboardConfig(context: Context) = getPreferences(context).getBoolean(IS_DASHBOARD_CONFIGURED, false)

fun setDashboardConfig(isConfigured: Boolean, context: Context) {
    getPreferences(context).edit().putBoolean(IS_DASHBOARD_CONFIGURED, isConfigured).apply()
}