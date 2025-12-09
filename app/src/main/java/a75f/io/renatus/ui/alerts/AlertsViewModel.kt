package a75f.io.renatus.ui.alerts

import a75f.io.alerts.AlertManager
import a75f.io.alerts.AlertProcessor.TAG_CCU_ALERTS
import a75f.io.api.haystack.Alert
import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsiusTwoDecimal
import a75f.io.logic.tuners.TunerConstants
import a75f.io.renatus.R
import a75f.io.renatus.util.CCUUiUtil
import a75f.io.renatus.views.MasterControl.MasterControlView
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AlertsViewModel : ViewModel() {

    var alertList = mutableStateListOf<Alert>()
    var useCelsius: HashMap<Any, Any>? = null

    fun setAlertList() {
        alertList.clear()
        AlertManager.getInstance().getAllAlertsNotInternal().forEach { alert ->
            if (alert.mAlertType.equals("CUSTOMER VISIBLE", ignoreCase = true)) {
                alertList.add(alert)
            }
        }
    }

    private fun formatMessageToCelsius(alertMessage: String): String {
        val sb = StringBuilder()
        val strings = alertMessage.split(" ").toTypedArray()
        try {
            for (i in strings.indices) {
                if (strings[i].contains("\u00B0")) {
                    if (useCelsius?.containsKey("id") == true &&
                        MasterControlView.getTuner(useCelsius!!["id"].toString()) ==
                        TunerConstants.USE_CELSIUS_FLAG_ENABLED
                    ) {
                        strings[i] = "\u00B0C"
                        strings[i - 1] =
                            fahrenheitToCelsiusTwoDecimal(strings[i - 1].toDouble()).toString()
                    } else {
                        val df = DecimalFormat("#.#")
                        strings[i - 1] = df.format(strings[i - 1].toDouble())
                    }
                }
            }
            for (string in strings) {
                sb.append(string).append(" ")
            }
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            CcuLog.e(TAG_CCU_ALERTS, "Failed to format units in alert message", e)
            return alertMessage
        }
        return sb.toString()
    }


    private fun getFormattedDate(millis: Long): String {
        if (millis == 0L) return ""
        val sdf = SimpleDateFormat("MMM d, yyyy | h:mm a", Locale.getDefault())
        val date = Date(millis)
        val calendar = Calendar.getInstance()
        calendar.time = date
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val suffix = when {
            day in 11..13 -> "th"
            else -> when (day % 10) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }
        }
        return sdf.format(date).replace(",", "$suffix,")
    }



    fun onClicked(position: Int, a: Alert, context: Context) {
        var message = a.mMessage

        if (message.contains("75F")) {
            var replacement = "75F"
            when {
                CCUUiUtil.isDaikinThemeEnabled(context) -> replacement = "SiteLine™"
                CCUUiUtil.isCarrierThemeEnabled(context) -> replacement = "ClimaVision"
                CCUUiUtil.isAiroverseThemeEnabled(context) -> replacement =
                    "Airoverse for Facilities"
            }
            message = message.replace("75F", replacement)
        }

        useCelsius = CCUHsApi.getInstance().readEntity("displayUnit")

        if (message.contains("\u00B0")) {
            message = formatMessageToCelsius(message)
        }

        val builder = AlertDialog.Builder(context)
        builder.setMessage(
            a.mTitle + "\n\n" + message + "\n" +
                    context.getString(R.string.alert_generated_at) + getFormattedDate(a.startTime) +
                    context.getString(R.string.alert_fixed_at) + getFormattedDate(a.endTime)
        )
            .setCancelable(false)
            .setIcon(R.drawable.ic_dialog_alert)
            .setPositiveButton(context.getString(R.string.ok_button)) { _, _ -> }

        if (!a.isFixed) {
            builder.setNegativeButton(context.getString(R.string.mark_fixed)) { _, _ ->
                AlertManager.getInstance().fixAlert(a)
                refreshAlerts(alertList)
            }
        }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    @SuppressLint("CheckResult")
    fun onLongClicked(position: Int, alert: Alert, context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setMessage("Delete ?")
            .setCancelable(true)
            .setIcon(R.drawable.ic_dialog_alert)
            .setPositiveButton("OK") { _, _ ->
                val a = alertList[position]
                alertList.removeAt(position)
                AlertManager.getInstance()
                    .deleteAlert(a)
                    .subscribeOn(Schedulers.io())                // <-- background thread
                    .observeOn(AndroidSchedulers.mainThread())   // <-- UI updates on main thread
                    .subscribe(
                        {
                            CcuLog.i(TAG_CCU_ALERTS, "delete success")
                            refreshAlerts(alertList)
                        },
                        { throwable ->
                            CcuLog.w(TAG_CCU_ALERTS, "delete failure", throwable)
                        }
                    )
            }
            .setNegativeButton("Cancel") { _, _ -> }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun refreshAlerts(alerts: List<Alert>) {
        val newAlerts = alerts.toList()
        alertList.clear()
        alertList.addAll(newAlerts)
    }
}