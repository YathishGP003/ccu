package a75f.io.renatus.views.MasterControl

import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.daikin.fahrenheitToCelsius
import a75f.io.logic.bo.util.UnitUtils
import a75f.io.logic.bo.util.UnitUtils.roundToHalf
import a75f.io.renatus.R
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ZoneTempAdapter(allZones: ArrayList<HashMap<Any, Any>>, tag: String) : RecyclerView.Adapter<ZoneTempAdapter.ViewHolder>() {

    var zones:  ArrayList<HashMap<Any, Any>>
    private var tempTag : String
    private val FOLLOW_BUILDING = "followBuilding"
    private val SAME_AS_BUILDING = "Same as Building"

    init {
        zones = allZones
        tempTag = tag

    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lLayout : LinearLayout = itemView.findViewById(R.id.datalayout)
        val zoneName: TextView = itemView.findViewById(R.id.zoneName)
        val zoneMinTemp: TextView = itemView.findViewById(R.id.minZoneTemp)
        val zoneMaxTemp: TextView = itemView.findViewById(R.id.maxZoneTemp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_zone_temperatures, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val zone: HashMap<Any, Any> = zones[position]

        if(position % 2 != 0){
            holder.lLayout.setBackgroundColor(Color.parseColor("#F8F8F8"))
        }

        holder.zoneName.text = zone["dis"].toString()
        val roomRef = zone["id"].toString()
        if(CCUHsApi.getInstance().getScheduleById(zone["scheduleRef"].toString()).markers.contains(FOLLOW_BUILDING)){
            holder.zoneMaxTemp.text = SAME_AS_BUILDING
            holder.zoneMinTemp.text = SAME_AS_BUILDING
        }else{
            var zoneMaxTempVal = CCUHsApi.getInstance().readPointPriorityValByQuery("point and "+tempTag+
                    " and max and schedulable and limit and user and roomRef == \""+roomRef+"\"")
            var zoneMinTempVal = CCUHsApi.getInstance().readPointPriorityValByQuery("point and "+tempTag+
                    " and min and schedulable and limit and user and roomRef == \""+roomRef+"\"")

            if(UnitUtils.isCelsiusTunerAvailableStatus()){
                zoneMaxTempVal = roundToHalf(fahrenheitToCelsius(zoneMaxTempVal))
                zoneMinTempVal = roundToHalf(fahrenheitToCelsius(zoneMinTempVal))
            }
            holder.zoneMaxTemp.text = zoneMaxTempVal.toString()
            holder.zoneMinTemp.text = zoneMinTempVal.toString()

        }


    }

    override fun getItemCount(): Int {
        return zones.size
    }
}

