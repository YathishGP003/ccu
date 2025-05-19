package a75f.io.renatus.ENGG.bacnet.services

import a75f.io.logic.bo.building.system.client.ItemsViewModel
import a75f.io.renatus.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CustomAdapterMultipleRead(private val mList: List<ItemsViewModel>) :
    RecyclerView.Adapter<CustomAdapterMultipleRead.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.lyt_bac_resp_row_multi_read_item_, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemsViewModel = mList[position]
        holder.textViewType.text = itemsViewModel.textType
        holder.textViewValue.text = itemsViewModel.textValue
        holder.textViewObjectType.text = itemsViewModel.objectType
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewType: TextView = itemView.findViewById(R.id.tv_property_type_text)
        val textViewValue: TextView = itemView.findViewById(R.id.tv_property_value_text)
        val textViewObjectType: TextView = itemView.findViewById(R.id.tv_object_type_text)
    }
}