package a75f.io.api.haystack.util

import android.os.Build
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun getCurrentDateTime(): Pair<String, String> {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val currentDate = java.time.LocalDate.now()
        val currentTime = java.time.LocalTime.now()
        Pair(currentDate.toString(), currentTime.toString())
    } else {
        val calendar = Calendar.getInstance()
        val date = dateFormat.format(calendar.time)
        val time = timeFormat.format(calendar.time)
        Pair(date, time)
    }
}