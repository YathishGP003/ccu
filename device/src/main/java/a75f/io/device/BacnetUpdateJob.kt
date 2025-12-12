package a75f.io.device

import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.util.bacnet.BacnetConfigConstants.IS_BACNET_MSTP_INITIALIZED
import a75f.io.logic.util.bacnet.checkBacnetHealth
import a75f.io.logic.util.bacnet.updateBacnetMstpLinearAndCovSubscription
import a75f.io.usbserial.UsbDisconnectReceiver
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.widget.Toast
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

internal class BacnetUpdateJob : BaseJob () {
    private val jobLock: Lock = ReentrantLock()

    override fun doJob() {
        if (jobLock.tryLock()) {
            try {
                CcuLog.d(L.TAG_CCU_JOB, "BacnetUpdateJob -> ")

                val mstpDisconnectTime = UsbDisconnectReceiver.getMstpDisconnectTime(Globals.getInstance().getApplicationContext())
                if (mstpDisconnectTime != 0L) {
                    val elapsed = System.currentTimeMillis() - mstpDisconnectTime
                    if (elapsed >= 15 * 60 * 1000) {
                        CcuLog.d(L.TAG_CCU_JOB, "MSTP adapter disconnected for more than 15 minutes, Disabling mstp")
                        PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().getApplicationContext())
                            .edit().putBoolean(IS_BACNET_MSTP_INITIALIZED, false).apply()

                        PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().getApplicationContext())
                            .edit().putBoolean("bacnetMstpForceDisabled", true).apply()

                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(Globals.getInstance().applicationContext,
                                "MSTP disabled (adapter disconnected for 15 minutes)", Toast.LENGTH_LONG).show()
                        }
                        val intent = Intent("MSTP_STOP")
                        Globals.getInstance().applicationContext.sendBroadcast(intent)
                        UsbDisconnectReceiver.clearDisconnectTime(Globals.getInstance().getApplicationContext())
                    } else {
                        CcuLog.d(L.TAG_CCU_JOB, "MSTP adapter disconnected for "+elapsed+" ms, not disabling mstp yet.")
                    }
                }
                checkBacnetHealth()
                updateBacnetMstpLinearAndCovSubscription(false)
                CcuLog.d(L.TAG_CCU_JOB, "<- BacnetUpdateJob ")
            } catch (e: Exception) {
                CcuLog.e(L.TAG_CCU_JOB, "Exception in BacnetUpdateJob: ${e.message}", e)
            } finally {
                jobLock.unlock()
            }
        } else {
            CcuLog.d(L.TAG_CCU_JOB, "BacnetUpdateJob is already running, skipping this execution.")
            return
        }

    }
}