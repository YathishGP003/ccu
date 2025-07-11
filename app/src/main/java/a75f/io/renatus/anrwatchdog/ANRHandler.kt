package a75f.io.renatus.anrwatchdog

import a75f.io.alerts.model.AlertCauses.Companion.CCU_ANR
import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.renatus.RenatusApp
import a75f.io.renatus.UtilityApplication
import a75f.io.renatus.util.CCUUiUtil.UpdateAppRestartCause
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.common.base.Throwables
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object ANRHandler {
    private const val TAG = "ANRHandler"
    private const val ANR_WATCHDOG_ENABLED = "anr_reporting_enabled"
    private const val CCU_ANR_TRACES = "ccu_anr_traces"

    /**
     * Default ANR timeout is given as 10 seconds as most of the time framework recovers without
     * showing an ANR dialog while 5 seconds delay is reported.
     */
    @JvmStatic
    fun configureANRWatchdog() {
        CcuLog.i(TAG, "configureANRWatchdog")
        ANRWatchDog(10000)
            .setANRListener { error: ANRError, uiHandler: Handler ->
                CcuLog.e(TAG, "ANR detected " + error.message + " , Sleep for 10 sec")
                CcuLog.e(TAG, "ANR Pending messages " + uiHandler.hasMessages(0))
                try {
                    //Check if UI thread is still active
                    val handler = Handler(Looper.getMainLooper())
                    val uiThreadStatus =
                        AtomicBoolean(false)
                    handler.post {
                        uiThreadStatus.set(true)
                        CcuLog.e(TAG, "UI thread still active - ANR recovered ")
                    }
                    Thread.sleep(10000)
                    if (!uiThreadStatus.get()) {
                        error.printStackTrace()
                        dumpANRTraces(UtilityApplication.context, error)
                        CcuLog.e(TAG, "ANR Triggered, Restarting App")
                        UpdateAppRestartCause(CCU_ANR)
                        RenatusApp.restartApp()
                    }
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            } /*.setANRInterceptor(duration -> {
                long ret = 20000 - duration;
                if (ret > 0) {
                    CcuLog.e(L.TAG_CCU, "Intercepted ANR that is too short (" + duration + " ms), postponing for " + ret + " ms.");
                }
                return ret;
            })*/
            .start()
    }

    @SuppressLint("ApplySharedPref")
    private fun dumpANRTraces(context: Context, error: ANRError) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentTime: String = sdf.format(Date())
        CcuLog.e(TAG, "Dump ANR traces $currentTime")
        context.getSharedPreferences("ccu_anr_traces", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("reported", false)
            .putString("time", currentTime+ "\n")
            .putString("traces", Throwables.getStackTraceAsString(error.fillInStackTrace()))
            .commit()
    }

    fun updateANRReportingStatus(context: Context, status: Boolean) {
        context.getSharedPreferences("ccu_anr_traces", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("reported", status)
            .apply()
    }

    fun isANRPendingTobeReported(context: Context): Boolean {
        return context.getSharedPreferences(CCU_ANR_TRACES, Context.MODE_PRIVATE)
            .getBoolean("reported", true).not()
    }

    fun getAnrAlertMessage(hayStack : CCUHsApi): String {
        val siteName = hayStack.siteName
        val ccuName = hayStack.ccuName
        return String.format("%s %s  has just been restarted due to a ANR", siteName, ccuName)
    }

    @JvmStatic
    fun isAnrWatchdogEnabled() :Boolean {
        return Globals.getInstance().applicationContext.getSharedPreferences(
            "ccu_devsetting",
            Context.MODE_PRIVATE
        ).getBoolean(ANR_WATCHDOG_ENABLED, false)
    }
}