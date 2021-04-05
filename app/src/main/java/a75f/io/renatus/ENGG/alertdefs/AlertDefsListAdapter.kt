package a75f.io.renatus.ENGG.alertdefs

import a75f.io.renatus.R
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for RecyclerView to convert AlertDefinition objects to line item views.
 *
 * @author tcase@75f.io
 * Created on 3/2/21.
 */
class AlertDefsListAdapter: RecyclerView.Adapter<AlertDefViewHolder>() {

   private val alertDefList: MutableList<AlertDefRow> = mutableListOf()

   /**
    * Simple setting of all alerts.
    * We will want to convert this to a DiffUtil approach for better performance.
    */
   fun setAlertDefs(alertDefs: List<AlertDefRow>) {
      if (alertDefs == alertDefList) {
         return
      }

      alertDefList.clear()
      alertDefList.addAll(alertDefs)
      notifyDataSetChanged()
   }

   override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertDefViewHolder {
      val view = LayoutInflater.from(parent.context).inflate(R.layout.alert_def_list_item, parent, false)
      return AlertDefViewHolder(view)
   }

   override fun onBindViewHolder(holder: AlertDefViewHolder, position: Int) {
      val alertDef = alertDefList[position]
      holder.bind(alertDef)
   }

   override fun getItemCount() = alertDefList.size
}

class AlertDefViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

   private val titleView: TextView = itemView.findViewById(R.id.titleText)
   private val severityView: TextView = itemView.findViewById(R.id.severityText)
   private val conditionalView: TextView = itemView.findViewById(R.id.conditionalText)
   private val statusView: TextView = itemView.findViewById(R.id.stateText)

   fun bind(alertDef: AlertDefRow) {

      with (alertDef) {
         val color = ContextCompat.getColor(itemView.context, colorRes)
         titleView.text = title
         severityView.text = severity
         severityView.setTextColor(color)
         conditionalView.text =conditional
         statusView.text = status

         if (isActive) {
            statusView.setTextColor(color)
            statusView.setTypeface(null, Typeface.BOLD)
            severityView.setTypeface(null, Typeface.BOLD)
            if (isCustom) {
               titleView.setTextColor(Color.DKGRAY)
               titleView.setTypeface(null, Typeface.BOLD_ITALIC )
            } else {
               titleView.setTypeface(null, Typeface.BOLD)
               titleView.setTextColor(Color.BLACK)
            }
         } else {
            statusView.setTextColor(Color.DKGRAY)
            statusView.setTypeface(null, Typeface.NORMAL)
            if (isCustom) {
               titleView.setTextColor(Color.DKGRAY)
               titleView.setTypeface(null, Typeface.ITALIC )
            } else {
               titleView.setTypeface(null, Typeface.NORMAL)
               titleView.setTextColor(Color.BLACK)
            }
            severityView.setTypeface(null, Typeface.NORMAL)
         }

         itemView.setOnClickListener {
            AlertDialog.Builder(itemView.context)
               .setTitle(title)
               .setMessage(notificationMsg)
               .show()
         }
      }
   }
}
