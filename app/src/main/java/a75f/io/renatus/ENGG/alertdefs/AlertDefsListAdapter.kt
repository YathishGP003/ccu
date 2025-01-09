package a75f.io.renatus.ENGG.alertdefs

import a75f.io.logger.CcuLog
import a75f.io.renatus.R
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter for RecyclerView to convert AlertDefinition objects to line item views.
 *
 * @author tcase@75f.io
 * Created on 3/2/21.
 */
class AlertDefsListAdapter(private val callback: AdapterCallback): RecyclerView.Adapter<AlertDefViewHolder>() {

   private val alertDefList: MutableList<AlertDefRow> = mutableListOf()
   private val TAG = "CCU_ALERTS"

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
      return AlertDefViewHolder(view, callback)
   }

   override fun onBindViewHolder(holder: AlertDefViewHolder, position: Int) {
      val alertDef = alertDefList[position]
      holder.bind(alertDef)
   }

   override fun getItemCount() = alertDefList.size
}

class AlertDefViewHolder(itemView: View, val callback: AdapterCallback): RecyclerView.ViewHolder(itemView) {

   private val titleView: TextView = itemView.findViewById(R.id.titleText)
   private val severityView: TextView = itemView.findViewById(R.id.severityText)
   private val conditionalView: TextView = itemView.findViewById(R.id.conditionalText)
   private val statusView: TextView = itemView.findViewById(R.id.stateText)

   fun bind(alertDefRow: AlertDefRow) {

      with (alertDefRow) {
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
               titleView.setTypeface(null, Typeface.BOLD )
            } else {
               titleView.setTypeface(null, Typeface.BOLD)
               titleView.setTextColor(Color.BLACK)
            }
         } else {
            statusView.setTextColor(Color.DKGRAY)
            statusView.setTypeface(null, Typeface.NORMAL)
            if (isCustom) {
               titleView.setTextColor(Color.DKGRAY)
               titleView.setTypeface(null, Typeface.NORMAL)
            } else {
               titleView.setTypeface(null, Typeface.NORMAL)
               titleView.setTextColor(Color.BLACK)
            }
            severityView.setTypeface(null, Typeface.NORMAL)
         }

         conditionalView.setOnClickListener {
            AlertDialog.Builder(itemView.context)
               .setTitle("Evaluation")
               .setMessage(evalString)
               .show()
         }

         itemView.setOnClickListener {

            if (creator != null && (creator == "blockly" || creator == "sequencer")) {
               CcuLog.d("CCU_ALERTS", "this is blockly alert")
               //kamal launch new fragment from here
               callback.onItemClicked(alertDefId)
            } else {
               CcuLog.d("CCU_ALERTS", "this is not a blockly alert")
               AlertDialog.Builder(itemView.context)
                  .setTitle(title)
                  .setMessage(notificationMsg)
                  .show()
            }
         }


         if (alertPopup == null) {
            statusView.setOnClickListener(null)
         } else {
            statusView.setOnClickListener {
               AlertDialog.Builder(itemView.context)
                  .setTitle("Alert!")
                  .setMessage(alertPopup)
                  .show()
            }
         }
      }
   }
}
