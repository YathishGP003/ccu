package a75f.io.renatus.util

import a75f.io.renatus.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CloudStatusAdapter(
    private val services: List<CloudStatus.ServiceStatus>
) : RecyclerView.Adapter<CloudStatusAdapter.ServiceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ServiceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cloud_status, parent, false)
        return ServiceViewHolder(view)
    }

    override fun onBindViewHolder(holder: ServiceViewHolder, position: Int) {
        holder.bind(services[position])
    }

    override fun getItemCount() = services.size

    class ServiceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val statusIndicator: View = itemView.findViewById(R.id.statusIndicator)
        private val serviceName: TextView = itemView.findViewById(R.id.serviceName)
        private val serviceVersion: TextView = itemView.findViewById(R.id.serviceVersion)

        fun bind(service: CloudStatus.ServiceStatus) {
            serviceName.text = service.serviceName
            val drawable: Int
            if (service.isNetworkDown) {
                drawable = R.drawable.circle_grey
                serviceVersion.text =  " "
            } else {
                serviceVersion.text = if (service.serviceVersion.isNotEmpty()) {"(${service.serviceVersion})"} else {" "}
                drawable = if (service.isUp) {
                    R.drawable.circle_green
                } else {
                    R.drawable.circle_red
                }
            }
            statusIndicator.setBackgroundResource(drawable)
        }
    }
}
