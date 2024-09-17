package a75f.io.renatus.views

import a75f.io.renatus.R
import a75f.io.renatus.util.ScheduleSpillAdapter
import androidx.recyclerview.widget.RecyclerView

import android.app.Dialog
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager

data class AlertDialogData(
    val title: String?,
    val messageHeader: CharSequence,
    val message: List<String>?,
    val positiveButtonText: String?,
    val positiveButtonListener: View.OnClickListener?,
    val negativeButtonText: String,
    val negativeButtonListener: View.OnClickListener,
    val isOnlyNegativeButton: Boolean,
    val isCancelable: Boolean,
    val icon: Int?

)

class AlertDialogAdapter(private val context: Context, private val alertDialogData: AlertDialogData) {

    fun showCustomDialog() {
        val dialogView = Dialog(context)
        dialogView.setContentView(R.layout.custom_alert_box)

        val positiveButton = dialogView.findViewById<Button>(R.id.positive_button)
        val negativeButton = dialogView.findViewById<Button>(R.id.negative_button)
        val divider = dialogView.findViewById<View>(R.id.divider)
        val icon = dialogView.findViewById<View>(R.id.icon)
        val title = dialogView.findViewById<TextView>(R.id.title)
        val message = dialogView.findViewById<TextView>(R.id.message)
        val buttonLayout = dialogView.findViewById<LinearLayout>(R.id.buttons_layout)
        val recyclerView: RecyclerView = dialogView.findViewById(R.id.recycler_view)
        val titleLayout = dialogView.findViewById<LinearLayout>(R.id.title_layout)

        if(alertDialogData.icon != null) {
            icon.setBackgroundResource(alertDialogData.icon)
        } else {
            icon.visibility = View.GONE
        }
        if(alertDialogData.title == null) {
            title.visibility = View.GONE
            titleLayout.visibility = View.GONE
        } else {
            title.text = alertDialogData.title
        }
        message.text = alertDialogData.messageHeader
        negativeButton.text = alertDialogData.negativeButtonText
        positiveButton.text = alertDialogData.positiveButtonText
        recyclerView.layoutManager = LinearLayoutManager(context)


        val adapter = alertDialogData.message?.let { ScheduleSpillAdapter(it) }
        recyclerView.adapter = adapter

        if(alertDialogData.isOnlyNegativeButton) {
            positiveButton.visibility = View.GONE
            divider.visibility = View.GONE
            buttonLayout.gravity = Gravity.CENTER
        }

        negativeButton.setOnClickListener {
            alertDialogData.negativeButtonListener.onClick(it)
            dialogView.dismiss()
        }

        positiveButton.setOnClickListener {
            alertDialogData.positiveButtonListener?.onClick(it)
            dialogView.dismiss()
        }
        dialogView.window?.setLayout(
            740,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialogView.setCancelable(alertDialogData.isCancelable)
        dialogView.show()
    }
}
