package a75f.io.renatus.ENGG.alertdefs

import a75f.io.alerts.log.SequenceMethodLog
import a75f.io.logger.CcuLog
import a75f.io.renatus.ENGG.alertdefs.BundleConstants.LOG_TYPE_ERROR
import a75f.io.renatus.ENGG.alertdefs.BundleConstants.LOG_TYPE_INFO
import a75f.io.renatus.ENGG.alertdefs.BundleConstants.LOG_TYPE_TRACE
import a75f.io.renatus.ENGG.alertdefs.BundleConstants.LOG_TYPE_WARN
import a75f.io.renatus.ENGG.alertdefs.BundleConstants.SORT_BY_EXPIRE_AT
import a75f.io.renatus.ENGG.alertdefs.BundleConstants.SORT_BY_MESSAGE
import a75f.io.renatus.ENGG.alertdefs.BundleConstants.SORT_BY_OPERATION
import a75f.io.renatus.ENGG.alertdefs.BundleConstants.SORT_BY_TIMESTAMP
import a75f.io.renatus.ENGG.alertdefs.BundleConstants.SORT_BY_TYPE
import a75f.io.renatus.ENGG.alertdefs.BundleConstants.TAG
import a75f.io.renatus.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class AlertLogSequenceAdapter(
    private val items: List<SequenceMethodLog>,
    val adapterLogSequenceCallback: AdapterLogSequenceCallback
) : RecyclerView.Adapter<AlertLogSequenceAdapter.ViewHolder>() {
    private var filteredItems: List<SequenceMethodLog> = mutableListOf()
    init {
        CcuLog.d(TAG, "items in adapter-->${items.size}")
        filteredItems = items
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivType: ImageView = view.findViewById(R.id.ivType)
        val tvTimeStamp: TextView = view.findViewById(R.id.tvTimeStamp)
        val tvExpireAt: TextView = view.findViewById(R.id.tvExpireAt)
        val tvOperation: TextView = view.findViewById(R.id.tvOperation)
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val ivResult: ImageView = view.findViewById(R.id.ivResult)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.lyt_alert_log_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredItems[position]
        holder.ivType.setImageResource(getDrawableFromStatus(item.level.name.uppercase(Locale.ENGLISH)))
        holder.tvTimeStamp.text = item.timestamp
        holder.tvExpireAt.text = item.expiresAt
        holder.tvOperation.text = item.operation.name
        holder.tvMessage.text = item.message
        holder.ivResult.setImageResource(R.drawable.img_eye_open)
        holder.ivResult.setOnClickListener(View.OnClickListener {
            if(!item.resultJson.isNullOrBlank()){
                adapterLogSequenceCallback.onItemClicked(item.resultJson)
            }else{
                adapterLogSequenceCallback.onItemClicked(null)
                CcuLog.d(TAG, "there is no result for this")
            }
        })
    }

    private fun getDrawableFromStatus(s: String): Int {
        when (s) {
            LOG_TYPE_ERROR -> {
                return R.drawable.img_error
            }
            LOG_TYPE_TRACE -> {
                return R.drawable.img_trace
            }
            LOG_TYPE_WARN -> {
                return R.drawable.img_warn
            }
            LOG_TYPE_INFO -> {
                return R.drawable.img_info
            }
            else -> return R.drawable.img_trace
        }
    }

    override fun getItemCount(): Int = filteredItems.size


    fun filter(query: String) {
        CcuLog.d(TAG, "filter-->$query")
        filteredItems = if (query.isEmpty()) {
            items
        } else {
            items.filter {
                it.operation.name.contains(query, ignoreCase = true) ||
                        it.message.contains(query, ignoreCase = true) ||
                        it.level.name.contains(query, ignoreCase = true) ||
                        it.expiresAt.contains(query, ignoreCase = true) ||
                        it.timestamp.contains(query, ignoreCase = true)
            }
        }
        CcuLog.d(TAG, "filteredItems after query-->${filteredItems.size}")
        notifyDataSetChanged()
    }

    fun sort(column: String, ascending: Boolean) {
        CcuLog.d(TAG, "Sorting by $column in ${if (ascending) "ascending" else "descending"} order")

        filteredItems = when (column) {
            SORT_BY_OPERATION -> if (ascending) {
                filteredItems.sortedBy { it.operation.name }
            } else {
                filteredItems.sortedByDescending { it.operation.name }
            }
            SORT_BY_TIMESTAMP -> if (ascending) {
                filteredItems.sortedBy { it.timestamp }
            } else {
                filteredItems.sortedByDescending { it.timestamp }
            }
            SORT_BY_MESSAGE -> if (ascending) {
                filteredItems.sortedBy { it.message }
            } else {
                filteredItems.sortedByDescending { it.message }
            }
            SORT_BY_TYPE -> if (ascending) {
                filteredItems.sortedBy { it.message }
            } else {
                filteredItems.sortedByDescending { it.message }
            }
            SORT_BY_EXPIRE_AT -> if (ascending) {
                filteredItems.sortedBy { it.message }
            } else {
                filteredItems.sortedByDescending { it.message }
            }
            else -> {
                CcuLog.w(TAG, "Unknown column for sorting: $column")
                filteredItems
            }
        }

        CcuLog.d(TAG, "filteredItems after sorting-->${filteredItems.size}")
        notifyDataSetChanged()
    }

}
