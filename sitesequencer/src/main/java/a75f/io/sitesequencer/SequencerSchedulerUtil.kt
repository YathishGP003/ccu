package a75f.io.sitesequencer

import a75f.io.alerts.AlertDefinition
import a75f.io.api.haystack.Alert
import a75f.io.logger.CcuLog
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.Calendar
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit


class SequencerSchedulerUtil {

    companion object {
        private val TAG = SequencerParser.TAG_CCU_SITE_SEQUENCER
        private const val PREFS_NAME = "ccu_sequences"
        private const val KEY_HASHMAP = "job_ids_hashmap"

        fun createJob(context: Context, siteSequencerDefinition: SiteSequencerDefinition) {
            if (context != null) {
                var isJobCreated = isAlarmScheduled(context, siteSequencerDefinition.seqId.hashCode()) //isJobExists(siteSequencerDefinition.seqId, context)
                CcuLog.d(
                    TAG,
                    "isJobCreated: $isJobCreated for seq id ->: ${siteSequencerDefinition.seqId} -isEnabled-${siteSequencerDefinition.enabled}")
                //if (!isJobCreated) {
                    scheduleJob(context, siteSequencerDefinition)
                //}
            }
        }

        private fun isJobExists(seqId: String, context: Context): Boolean {
            val workManager = WorkManager.getInstance(context)
            workManager.getWorkInfosByTag(seqId).get().forEach {
                if (it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING) {
                    println("Work request with tag $seqId already exists.")
                    return true
                }
            }
            return false
        }

        fun isAlarmScheduled(context: Context, requestCode: Int): Boolean {
            // Create the PendingIntent with the same request code
            val intent = Intent(context, SequencerAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Check if the PendingIntent exists
            return pendingIntent != null
        }

        private fun schedulePeriodicWork(
            interval: Long,
            timeUnit: TimeUnit,
            context: Context,
            siteSequencerDefinition: SiteSequencerDefinition
        ) {
            val periodicWorkRequest =
                PeriodicWorkRequest.Builder(SequenceWorker::class.java, interval, timeUnit)
                    .addTag(siteSequencerDefinition.seqId)
                    .build()

            // Enqueue the periodic work request
            WorkManager.getInstance(context).enqueue(periodicWorkRequest)
        }

        fun scheduleJob(
            context: Context,
            siteSequencerDefinition: SiteSequencerDefinition
        ) {
            CcuLog.d(
                TAG,
                "scheduleJob for seq id ->: ${siteSequencerDefinition.seqId} for frequency: ${siteSequencerDefinition.quartzCronRequest.frequency}"
            )
            if (context != null) {
                siteSequencerDefinition.quartzCronRequest.frequency?.let {
                    when (it) {
                        "EVERY_MINUTE" -> {
                            scheduleOneMinTask(context, siteSequencerDefinition)
                        }
                        "HOURLY" -> {
                            scheduleHourlyTask(context, siteSequencerDefinition)
                        }
                        "DAILY" -> {
                            scheduleDailyTask(context, siteSequencerDefinition)
                        }
                        "WEEKLY" -> {
                            scheduleWeeklyTaskExact(context, siteSequencerDefinition)
                        }
                        "EVERY_MONTH" -> {
                            scheduleMonthlyTaskExact(context, siteSequencerDefinition)
                        }
                    }
                }
                CcuLog.d(
                    TAG,
                    "created new job, job id for this seq id not exists seq id: ${siteSequencerDefinition.seqId}"
                )
            }
        }

        private fun scheduleHourlyTask(context: Context, siteSequencerDefinition: SiteSequencerDefinition) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerTime = getCustomCalendarHourlyFrequency(siteSequencerDefinition).timeInMillis
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                AlarmManager.INTERVAL_HOUR,  // 1 hour in milliseconds
                createIntentForScheduler(context, siteSequencerDefinition)
            )

            val triggerDate = Date(triggerTime)
            CcuLog.d(
                TAG,
                "scheduleHourlyTask for seq id ->: ${siteSequencerDefinition.seqId} <--seq name-->${siteSequencerDefinition.seqName}  at time: $triggerDate")
        }

        private fun scheduleDailyTask(context: Context, siteSequencerDefinition: SiteSequencerDefinition) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerTime = getCustomCalendarDailyFrequency(siteSequencerDefinition).timeInMillis
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                AlarmManager.INTERVAL_DAY,  // 1 day in milliseconds
                createIntentForScheduler(context, siteSequencerDefinition)
            )

            val triggerDate = Date(triggerTime)
            CcuLog.d(
                TAG,
                "scheduleDailyTask for seq id ->: ${siteSequencerDefinition.seqId} <--seq name-->${siteSequencerDefinition.seqName}  at time: $triggerDate")
        }

        private fun scheduleWeeklyTask(context: Context, siteSequencerDefinition: SiteSequencerDefinition) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerTime = getCalendarWeeklyFrequency(siteSequencerDefinition).timeInMillis
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                AlarmManager.INTERVAL_DAY * 7,  // 7 days in milliseconds
                createIntentForScheduler(context, siteSequencerDefinition)
            )

            val triggerDate = Date(triggerTime)
            CcuLog.d(
                TAG,
                "scheduleWeeklyTask for seq id ->: ${siteSequencerDefinition.seqId} <--seq name-->${siteSequencerDefinition.seqName}  at time: $triggerDate")
        }

        fun scheduleWeeklyTaskExact(context: Context, siteSequencerDefinition: SiteSequencerDefinition) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerTime = getCalendarWeeklyFrequencyExact(siteSequencerDefinition).timeInMillis

            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime,
                createIntentForScheduler(context, siteSequencerDefinition))
            val triggerDate = Date(triggerTime)
            CcuLog.d(
                TAG,
                "scheduleMonthlyTask for seq id ->: ${siteSequencerDefinition.seqId} <--seq name-->${siteSequencerDefinition.seqName}  at time: $triggerDate")
        }

        private fun scheduleMonthlyTask(context: Context, siteSequencerDefinition: SiteSequencerDefinition) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerTime = getCalendarMonthlyFrequency(siteSequencerDefinition).timeInMillis
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                AlarmManager.INTERVAL_DAY * 30,
                createIntentForScheduler(context, siteSequencerDefinition)
            )
            val triggerDate = Date(triggerTime)
            CcuLog.d(
                TAG,
                "scheduleMonthlyTask for seq id ->: ${siteSequencerDefinition.seqId} <--seq name-->${siteSequencerDefinition.seqName}  at time: $triggerDate")
        }

        fun scheduleMonthlyTaskExact(context: Context, siteSequencerDefinition: SiteSequencerDefinition) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val triggerTime = getCalendarMonthlyFrequencyExact(siteSequencerDefinition).timeInMillis

            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, createIntentForScheduler(context, siteSequencerDefinition))
            val triggerDate = Date(triggerTime)
            CcuLog.d(
                TAG,
                "scheduleMonthlyTask for seq id ->: ${siteSequencerDefinition.seqId} <--seq name-->${siteSequencerDefinition.seqName}  at time: $triggerDate")
        }

        private fun getCalendarWeeklyFrequencyExact(siteSequencerDefinition: SiteSequencerDefinition): Calendar {
            // Create a Calendar object and set it to the specified day and time
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()

                // Set the day of the week (e.g., Calendar.MONDAY)
                set(Calendar.DAY_OF_WEEK, getDay(siteSequencerDefinition))
                set(Calendar.HOUR_OF_DAY, siteSequencerDefinition.quartzCronRequest.hour ?: 0)
                set(Calendar.MINUTE, siteSequencerDefinition.quartzCronRequest.minute ?: 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // If the scheduled time is in the past for the current week, move to the next week
                if (before(Calendar.getInstance())) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }
            return calendar
        }

        private fun getCalendarMonthlyFrequencyExact(siteSequencerDefinition: SiteSequencerDefinition): Calendar{
            val hour = siteSequencerDefinition.quartzCronRequest.hour ?: 0
            val minute = siteSequencerDefinition.quartzCronRequest.minute ?: 0
            // Set calendar to the 1st day of the next month at the specified time
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()

                // If it's the 1st already and time has passed for today, move to the next month
                if (get(Calendar.DAY_OF_MONTH) > 1 || (get(Calendar.DAY_OF_MONTH) == 1 && (get(Calendar.HOUR_OF_DAY) > hour || (get(Calendar.HOUR_OF_DAY) == hour && get(Calendar.MINUTE) >= minute)))) {
                    add(Calendar.MONTH, 1)
                }

                // Set to the 1st of the month
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return calendar
        }

        private fun getCalendarMonthlyFrequency(siteSequencerDefinition: SiteSequencerDefinition): Calendar{
            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, siteSequencerDefinition.quartzCronRequest.hour ?: 0)
                set(Calendar.MINUTE, siteSequencerDefinition.quartzCronRequest.minute ?: 0)
                set(Calendar.SECOND, 0)

                // Check if the time has already passed for this month, if so, schedule for next month
                if (before(Calendar.getInstance())) {
                    add(Calendar.MONTH, 1)
                }
            }
            return calendar
        }

        private fun getCalendarWeeklyFrequency(siteSequencerDefinition: SiteSequencerDefinition): Calendar{
            val calendar = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, getDay(siteSequencerDefinition))
                set(Calendar.HOUR_OF_DAY, siteSequencerDefinition.quartzCronRequest.hour ?: 0)
                set(Calendar.MINUTE, siteSequencerDefinition.quartzCronRequest.minute ?: 0)
                set(Calendar.SECOND, 0)
            }
            return calendar
        }

        private fun getDay(siteSequencerDefinition: SiteSequencerDefinition): Int {
            return when (siteSequencerDefinition.quartzCronRequest.dayOfWeek) {
                "SUN" -> Calendar.SUNDAY
                "MON" -> Calendar.MONDAY
                "TUE" -> Calendar.TUESDAY
                "WED" -> Calendar.WEDNESDAY
                "THU" -> Calendar.THURSDAY
                "FRI" -> Calendar.FRIDAY
                "SAT" -> Calendar.SATURDAY
                else -> Calendar.MONDAY // Default case
            }
        }

        private fun getCustomCalendarDailyFrequency(siteSequencerDefinition: SiteSequencerDefinition) : Calendar {
            val calendar = Calendar.getInstance().apply {
                siteSequencerDefinition.quartzCronRequest.hour?.let {
                    set(Calendar.HOUR_OF_DAY,
                        it
                    )
                }
                siteSequencerDefinition.quartzCronRequest.minute?.let {
                    set(Calendar.MINUTE,
                        it
                    )
                }
                set(Calendar.SECOND, 0)
                // Ensure the alarm starts tomorrow if the current time is past 8:00 a.m.
                if (timeInMillis < System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            return calendar
        }

        private fun getCustomCalendarHourlyFrequency(siteSequencerDefinition: SiteSequencerDefinition) : Calendar {
            val calendar = Calendar.getInstance().apply {
                set(
                    Calendar.HOUR_OF_DAY,
                    1
                )
                siteSequencerDefinition.quartzCronRequest.minute?.let {
                    set(Calendar.MINUTE,
                        it
                    )
                }
                set(Calendar.SECOND, 0)
            }
            return calendar
        }

        private fun scheduleOneMinTask(context: Context, siteSequencerDefinition: SiteSequencerDefinition) {
            CcuLog.d(
                TAG,
                "scheduleOneMinTask for seq id ->: ${siteSequencerDefinition.seqId}")
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            // Schedule the task to run every minute
            alarmManager.setRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 60 * 1000, // Start after 1 minute
                60 * 1000, // Repeat interval: 1 minute
                createIntentForScheduler(context, siteSequencerDefinition)
            )
        }

        private val interval = "interval"
        private val seqId = "seqId"

        private fun createIntentForScheduler(
            context: Context,
            siteSequencerDefinition: SiteSequencerDefinition
        ): PendingIntent {
            val intent = Intent(context, SequencerAlarmReceiver::class.java)
            intent.putExtra(interval, siteSequencerDefinition.quartzCronRequest.frequency)
            intent.putExtra(seqId, siteSequencerDefinition.seqId)
            return PendingIntent.getBroadcast(
                context,
                siteSequencerDefinition.seqId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun cancelIntentsForSeqId(context: Context, seqId: String) {
            CcuLog.d(
                TAG,
                "cancelIntentsForSeqId for seq id ->: $seqId"
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, SequencerAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                seqId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }

        private fun generateJobId(): Int {
            return UUID.randomUUID().hashCode()
        }

        fun cancelJob(seqId: String, context: Context) {
            CcuLog.d(
                TAG,
                "cancelJob for seq id ->: $seqId"
            )
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWorkByTag(seqId)
            workManager.pruneWork()
        }

        fun cancelAllJobs(context: Context) {
            val workManager = WorkManager.getInstance(context)
            workManager.cancelAllWork()
        }

        fun findAlertByBlockId(def: SiteSequencerDefinition?, blockId: String?): SequenceAlert? {
            if(def != null){
                def.seqAlerts?.forEach {sequenceAlert ->
                    if(sequenceAlert.alertBlockId == blockId){
                        return sequenceAlert
                    }
                }
            }
            return null
        }

        fun findAlertsInSequence(def: SiteSequencerDefinition?): ArrayList<SequenceAlert>? {
            if(def != null){
                return def.seqAlerts
            }
            return null
        }

        fun createAlertDefinition(sequenceAlert : SequenceAlert): AlertDefinition {
            val alertDefinition = AlertDefinition()
            val testAlert = Alert()
            testAlert.mTitle = sequenceAlert.title
            testAlert.mMessage = sequenceAlert.message
            testAlert.mEnabled = sequenceAlert.enabled
            testAlert.mNotificationMsg = sequenceAlert.notificationMessage
            testAlert.mSeverity = Alert.AlertSeverity.valueOf(sequenceAlert.severity.uppercase())
            testAlert.mAlertType = sequenceAlert.alertType.uppercase()
            testAlert.alertDefId = sequenceAlert.alertDefinitionId
            alertDefinition.alert = testAlert
            alertDefinition._id = sequenceAlert.alertDefinitionId
            return alertDefinition
        }
    }
}
