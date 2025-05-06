package a75f.io.logic.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.preconfig.UnsupportedTimeZoneException
import android.app.AlarmManager
import android.content.Context
import java.util.TimeZone

object TimeZoneUtil {

    @JvmStatic
    fun getSupportedTimeZones(): ArrayList<String> {
        val tzIds = TimeZone.getAvailableIDs()
        val supportedTimeZones = ArrayList<String>()
        val regions = CCUHsApi.getInstance().supportedRegions

        for (tz in tzIds) {
            val parts = tz.split("/".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            val region = parts[0]
            if (regions.contains(region)) {
                supportedTimeZones.add(tz)
            }
        }

        return supportedTimeZones
    }

    @JvmStatic
    fun getTimeZoneIdFromString(timeZone: String) : String {
        return getSupportedTimeZones().find { it.contains(timeZone) }
            ?: throw UnsupportedTimeZoneException("Unsupported time zone: $timeZone")
    }

    @JvmStatic
    fun setDeviceTimeZone(context: Context, timeZone: String) {
        val tzID = getTimeZoneIdFromString(timeZone)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setTimeZone(tzID)
    }
}