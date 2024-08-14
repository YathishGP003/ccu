package a75f.io.renatus.views

import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val REBOOT_DATA_CACHE = "rebootDataCache"
private const val REBOOT_TIMESTAMP = "rebootTimestamp"

class RebootDataCache {
    private var sharedPreferences: SharedPreferences =
        Globals.getInstance().applicationContext.getSharedPreferences(REBOOT_DATA_CACHE, Context.MODE_PRIVATE)


        fun storeRebootTimestamp(isRebootStarted: Boolean) {

            val editor = sharedPreferences.edit()

            val currentTimestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            var formattedDate = dateFormat.format(Date(currentTimestamp))

            val rebootTimestamps = sharedPreferences.getStringSet(REBOOT_TIMESTAMP, HashSet<String>()) ?: HashSet()
            val listOfTimeStamps =  HashSet<String>(rebootTimestamps)
            formattedDate = if (isRebootStarted) "${listOfTimeStamps.size + 1}- Reboot Started: $formattedDate" else "${listOfTimeStamps.size + 1}- Reboot Completed: $formattedDate"
            listOfTimeStamps.add(formattedDate)

            listOfTimeStamps.sortedByDescending { it.substringBefore('-').toIntOrNull() ?: 0  }

            // Store the updated set in SharedPreferences
            editor.putStringSet(REBOOT_TIMESTAMP, listOfTimeStamps)
            editor.apply()

            CcuLog.i(L.TAG_CCU, "RebootHandlerService: Stored reboot timestamp: $formattedDate")
        }

}