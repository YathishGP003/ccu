/**
 * AlertCauses.kt
 * This file is part of the Alert
 * Created by kkmukesh on 10/10/2023.
 */

package a75f.io.alerts.model

import a75f.io.logger.CcuLog
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.util.Log

class AlertCauses {

    companion object {

        const val CCU_RESTART = "CCU_RESTART"
        const val CCU_ANR = "CCU_ANR"
        const val CCU_CRASH = "CCU_CRASH"
        private const val DEVICE_RESTART = "DEVICE_RESTART"
        const val CCU_UPDATE = "CCU_UPDATE"
        const val BUNDLE_UPDATE_DEVICE_REBOOT = "BUNDLE_UPDATE_DEVICE_REBOOT"
        const val REMOTE_COMMAND_RESTART = "REMOTE_COMMAND_RESTART"
        const val CCU_EXIT_SAFE_MODE = "CCU_EXIT_SAFE_MODE"
        private const val CCU_RESTART_MESSAGE = "CCU application has been restarted either remotely or manually by a user."
        private const val CCU_ANR_MESSAGE = "CCU application became unresponsive and has been automatically restarted."
        private const val CCU_CRASH_MESSAGE = "CCU application crashed unexpectedly and has been restarted."
        private const val DEVICE_REBOOT_MESSAGE = "CCU tablet device restarted."
        private const val CCU_EXIT_SAFE_MODE_MESSAGE = "CCU has exited safe mode and restarted successfully."
        private const val BUNDLE_UPDATE_DEVICE_REBOOT_MESSAGE = "App bundle has been updated and restarted"
        private const val CCU_UPDATE_MESSAGE = "CCU application has been updated and restarted."

        fun getCauses(sharePref: SharedPreferences): String {
            val causes = sharePref.getString("app_restart_cause", "CCU_RESTART")
            val exitSafe = sharePref.getBoolean("SafeModeExit", false)
            CcuLog.d("AlertCauses", "getCauses: causes = $causes, exitSafe = $exitSafe")

            //for exit safe mode
            if (exitSafe) {
                sharePref.edit().putBoolean("SafeModeExit", false).apply()
                return CCU_EXIT_SAFE_MODE_MESSAGE
            }

            //for device reboot check
            fun isDeviceRecentlyRebooted(): Boolean {
                if (!getSystemProperty("CCU_REBOOT")) {
                    setSystemProperty("CCU_REBOOT", "true")
                    CcuLog.d("AlertCauses", "Boot time: device rebooted")
                    return true
                }
                CcuLog.d("AlertCauses", "Boot time: device not rebooted")
                return false
            }

            //for device reboot
            if (isDeviceRecentlyRebooted() ||
                causes.equals(DEVICE_RESTART, ignoreCase = true) ||
                causes.equals(BUNDLE_UPDATE_DEVICE_REBOOT, ignoreCase = true) ||
                causes.equals("DEVICE RESTART COMMAND", ignoreCase = true)
            ) {
                return if (causes.equals(BUNDLE_UPDATE_DEVICE_REBOOT, ignoreCase = true)) {
                    CcuLog.d("AlertCauses", "Boot time: rebooted by remote Bundle command")
                    BUNDLE_UPDATE_DEVICE_REBOOT_MESSAGE
                } else {
                    CcuLog.d("AlertCauses", "Boot time: rebooted")
                    DEVICE_REBOOT_MESSAGE
                }
            }

            return when (causes) {
                CCU_RESTART -> CCU_RESTART_MESSAGE
                CCU_ANR -> CCU_ANR_MESSAGE
                CCU_CRASH -> CCU_CRASH_MESSAGE
                DEVICE_RESTART -> DEVICE_REBOOT_MESSAGE
                CCU_UPDATE -> CCU_UPDATE_MESSAGE
                else -> CCU_RESTART_MESSAGE
            }
        }

        // set system property to check if the device is rebooted
        @SuppressLint("PrivateApi")
        private fun setSystemProperty(propertyName: String, propertyValue: String) {
            try {
                val systemPropertiesClass = Class.forName("android.os.SystemProperties")
                val setMethod =
                    systemPropertiesClass.getMethod("set", String::class.java, String::class.java)
                setMethod.invoke(null, propertyName, propertyValue)
                Log.i("CCU_PROPERTY", "setSystemProperty: $propertyName = $propertyValue")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        @SuppressLint("PrivateApi")
        // if the tablet reboots the property will be remove we will get null pointer exception , is a tablet rebooted
        private fun getSystemProperty(propertyName: String): Boolean {
            return try {
                val systemPropertiesClass = Class.forName("android.os.SystemProperties")
                val getMethod = systemPropertiesClass.getMethod("get", String::class.java)
                val result = getMethod.invoke(null, propertyName) as String
                result.equals("true", ignoreCase = true)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }

        }
    }
}