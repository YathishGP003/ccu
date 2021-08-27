package a75f.io.renatus.hyperstat.vrv

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.bo.building.vrv.VrvAirflowDirection
import a75f.io.logic.bo.building.vrv.VrvFanSpeed
import a75f.io.logic.bo.building.vrv.VrvOperationMode
import a75f.io.renatus.R
import a75f.io.renatus.util.HeartBeatUtil
import android.app.Activity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

fun loadView(inflater : LayoutInflater,
             layout : LinearLayout,
             equipId : String,
             hayStack: CCUHsApi,
             context: Activity,
             nodeAddress : String) {

    val masterControllerMode = hayStack.readDefaultVal("config and masterController and mode and " +
                                            "equipRef == \"$equipId\"")

    if (masterControllerMode.toInt() == 0) {
        val masterOperationMode: View = inflater.inflate(R.layout.zone_item_type3, null)
        val masterOpModeLabel: TextView = masterOperationMode.findViewById(R.id.text_label)
        val masterOpModeVal: TextView = masterOperationMode.findViewById(R.id.text_value)
        masterOpModeLabel.text = "Master Operation Mode : "

        val masterOpMode = hayStack.readHisValByQuery(
            "point and master and operation and mode and equipRef == \"$equipId\""
        )
        masterOpModeVal.text = VrvOperationMode.values()[masterOpMode.toInt()].toString()

        layout.addView(masterOperationMode)
    }

    val viewTitle: View = inflater.inflate(R.layout.zones_item_title, null)
    val textViewTitle = viewTitle.findViewById<TextView>(R.id.textProfile)
    val textViewModule = viewTitle.findViewById<TextView>(R.id.module_status)
    textViewTitle.text = "Daikin VRV ($nodeAddress)"
    HeartBeatUtil.moduleStatus(textViewModule, nodeAddress)
    layout.addView(viewTitle)

    val viewStatus = inflater.inflate(R.layout.zones_item_status, null)
    val textViewStatus = viewStatus.findViewById<TextView>(R.id.text_status)
    val textViewStatusTitle = viewStatus.findViewById<TextView>(R.id.inner_status_title)
    val textViewUpdatedTime = viewStatus.findViewById<TextView>(R.id.last_updated_status)
    textViewUpdatedTime.text = HeartBeatUtil.getLastUpdatedTime(nodeAddress)
    textViewStatusTitle.visibility = View.GONE
    textViewStatus.visibility = View.GONE
    layout.addView(viewStatus)

    val viewPointRow1 = inflater.inflate(R.layout.zones_item_type2, null)
    viewPointRow1.findViewById<TextView>(R.id.text_point1label).text = "Operation Mode : "
    viewPointRow1.findViewById<TextView>(R.id.text_point2label).text = "Fan Speed : "
    val opModeSp = viewPointRow1.findViewById<Spinner>(R.id.spinnerValue1)
    val fanSpeedSp = viewPointRow1.findViewById<Spinner>(R.id.spinnerValue2)
    layout.addView(viewPointRow1)

    val viewPointRow2 = inflater.inflate(R.layout.zone_item_type4, null)
    viewPointRow2.findViewById<TextView>(R.id.text_point1label).text = "Airflow Direction : "
    viewPointRow2.findViewById<TextView>(R.id.text_point2label).text = "Humidity : "

    val airflowDirSp = viewPointRow2.findViewById<Spinner>(R.id.spinnerValue1)
    val humidityVal: TextView = viewPointRow2.findViewById(R.id.text_point2value)
    humidityVal.text = hayStack.readHisValByQuery("point and humidity and sensor " +
            "and equipRef == \"$equipId\"").toInt().toString()+"% RH"

    layout.addView(viewPointRow2)

    setUpOperationModeSpinner(opModeSp, equipId, hayStack, context)
    setUpFanSpeedSpinner(fanSpeedSp, equipId, hayStack, context)
    setUpAirflowDirectionSpinner(airflowDirSp, equipId, hayStack, context)
    //setUpMasterControllerSpinner(masterControlSp, equipId, hayStack, context)

}

fun canEnableOperation( masterControllerMode : Double,
                        masterOperationMode : Double,
                        opMode : VrvOperationMode) : Boolean{
    if (masterControllerMode.toInt() == 0) {
        if (masterOperationMode.toInt() == VrvOperationMode.FAN.ordinal) {
            if (opMode == VrvOperationMode.HEAT || opMode == VrvOperationMode.COOL){
                return false
            }
        } else if (masterOperationMode.toInt() == VrvOperationMode.COOL.ordinal) {
            if (opMode == VrvOperationMode.HEAT ){
                return false
            }
        } else if (masterOperationMode.toInt() == VrvOperationMode.HEAT.ordinal) {
            if (opMode == VrvOperationMode.COOL){
                return false
            }
        }
    }
    return true
}

private fun setUpOperationModeSpinner(opModeSpinner : Spinner,
                                        equipId: String,
                                        hayStack: CCUHsApi,
                                        context : Activity) {
    val masterControllerMode = hayStack.readDefaultVal("point and masterController and mode " +
                                                "and equipRef == \"$equipId\"")

    val masterOperationMode = hayStack.readHisValByQuery("point and master and operation and mode " +
            "and equipRef == \"$equipId\"")

    val opModeList : MutableList<String> = arrayListOf()

    VrvOperationMode.values().forEach { mode ->
        if (masterControllerMode.toInt() == 1) {
            opModeList.add(mode.toString())
        } else {
            if (mode != VrvOperationMode.AUTO) {
                opModeList.add(mode.toString())
            }
        }
    }

    val adapter:ArrayAdapter<String> = object: ArrayAdapter<String>(
        context,
        R.layout.spinner_zone_item,
        opModeList
    ){
        override fun getDropDownView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {
            val view:TextView = super.getDropDownView(
                position,
                convertView,
                parent
            ) as TextView

            if (!canEnableOperation(masterControllerMode, masterOperationMode, VrvOperationMode.values()[position])) {
                view.setTextColor(Color.LTGRAY)
            }

            return view
        }

        override fun isEnabled(position: Int): Boolean {
            return canEnableOperation(masterControllerMode, masterOperationMode, VrvOperationMode.values()[position])
        }
    }

    opModeSpinner.adapter = adapter

    val curSelection = hayStack.readPointPriorityValByQuery("userIntent and operation and mode and equipRef == \"$equipId\"")
    if (curSelection <= opModeList.size - 1) {
        opModeSpinner.setSelection(curSelection.toInt(), false)
    }
    opModeSpinner.onItemSelectedListener = object :
        AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>,
                                    view: View, position: Int, id: Long) {
            hayStack.writeDefaultVal("userIntent and operation and mode and equipRef == \"$equipId\"", position.toDouble())
            hayStack.writeHisValByQuery("userIntent and operation and mode and equipRef == \"$equipId\"", position.toDouble())

        }

        override fun onNothingSelected(parent: AdapterView<*>) {
            // write code to perform some action
        }
    }
}

private fun setUpAirflowDirectionSpinner(airflowSpinner : Spinner,
                                          equipId: String,
                                          hayStack: CCUHsApi,
                                          context : Activity) {

    val airflowDirectionSupport = hayStack.readHisValByQuery("point and capability and airflowDirection and support " +
            "and equipRef == \"$equipId\"")

    if (airflowDirectionSupport.toInt() == 0) {
        airflowSpinner.isEnabled = false
    }

    val airflowDirectionAuto = hayStack.readHisValByQuery("point and capability and airflowDirection and auto " +
            "and equipRef == \"$equipId\"")

    val airflowDirList : MutableList<String> = arrayListOf()

    VrvAirflowDirection.values().forEach { mode ->
        airflowDirList.add(mode.name)
    }

    val adapter:ArrayAdapter<String> = object: ArrayAdapter<String>(
        context,
        R.layout.spinner_zone_item,
        airflowDirList
    ){
        override fun getDropDownView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {
            val view:TextView = super.getDropDownView(
                position,
                convertView,
                parent
            ) as TextView

            if (airflowDirectionSupport.toInt() == 0 ) {
                view.setTextColor(Color.LTGRAY)
            } else if (airflowDirectionAuto.toInt() == 0 && position == VrvAirflowDirection.Auto.ordinal) {
                view.setTextColor(Color.LTGRAY)
            }
            return view
        }

        override fun isEnabled(position: Int): Boolean {
            if (airflowDirectionSupport.toInt() == 0 ) {
                return false;
            } else if (airflowDirectionAuto.toInt() == 0 && position == VrvAirflowDirection.Auto.ordinal) {
                return false;
            }
            return true
        }
    }

    airflowSpinner.adapter = adapter
    val curSelection = hayStack.readPointPriorityValByQuery("userIntent and airflowDirection and equipRef == \"$equipId\"")
    if (curSelection <= airflowDirList.size - 1) {
        airflowSpinner.setSelection(curSelection.toInt(), false)
    }

    airflowSpinner.onItemSelectedListener = object :
        AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>,
                                    view: View, position: Int, id: Long) {
            hayStack.writeDefaultVal("userIntent and airflowDirection and equipRef == \"$equipId\"", position.toDouble())
            hayStack.writeHisValByQuery("userIntent and airflowDirection and equipRef == \"$equipId\"", position.toDouble())

        }

        override fun onNothingSelected(parent: AdapterView<*>) {
            // write code to perform some action
        }
    }
}

private fun setUpFanSpeedSpinner(fanSpeedSp : Spinner,
                                 equipId: String,
                                 hayStack: CCUHsApi,
                                 context : Activity) {

    val fanSpeedControlLevel = hayStack.readHisValByQuery("point and capability and fanSpeed and controlLevel " +
            "and equipRef == \"$equipId\"")

    val fanSpeedAuto = hayStack.readHisValByQuery("point and capability and fanSpeed and auto " +
            "and equipRef == \"$equipId\"")

    val fanSpeedList : MutableList<String> = arrayListOf()

    when {
        fanSpeedControlLevel.toInt() == 1 -> {
            fanSpeedList.add(VrvFanSpeed.High.name)
        }
        fanSpeedControlLevel.toInt() == 2 -> {
            fanSpeedList.add(VrvFanSpeed.Low.name)
            fanSpeedList.add(VrvFanSpeed.High.name)
        }
        fanSpeedControlLevel.toInt() >= 3 -> {
            fanSpeedList.add(VrvFanSpeed.Low.name)
            fanSpeedList.add(VrvFanSpeed.Medium.name)
            fanSpeedList.add(VrvFanSpeed.High.name)
        }
    }

    if (fanSpeedControlLevel.toInt() > 1) {
        fanSpeedList.add(VrvFanSpeed.Auto.name)
    }

    val adapter:ArrayAdapter<String> = object: ArrayAdapter<String>(
        context,
        R.layout.spinner_zone_item,
        fanSpeedList
    ){
        override fun getDropDownView(
            position: Int,
            convertView: View?,
            parent: ViewGroup
        ): View {
            val view:TextView = super.getDropDownView(
                position,
                convertView,
                parent
            ) as TextView

            if (position == fanSpeedList.size-1  && fanSpeedAuto.toInt() == 0) {
                view.setTextColor(Color.LTGRAY)
            }
            return view
        }

        override fun isEnabled(position: Int): Boolean {
            if (position == fanSpeedList.size-1 && fanSpeedAuto.toInt() == 0) {
                return false
            }
            return true
        }
    }

    fanSpeedSp.adapter = adapter
    val curSelection = hayStack.readPointPriorityValByQuery("userIntent and fanSpeed and equipRef == \"$equipId\"")
    var curFanSpeed = VrvFanSpeed.values()[curSelection.toInt()]
    if (fanSpeedList.indexOf(curFanSpeed.name) <= fanSpeedList.size-1) {
        fanSpeedSp.setSelection(fanSpeedList.indexOf(curFanSpeed.name), false)
    }

    fanSpeedSp.onItemSelectedListener = object :
        AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>,
                                    view: View, position: Int, id: Long) {
            val fanSpeed = VrvFanSpeed.values().find { it.name == fanSpeedList[position] }
            hayStack.writeDefaultVal("userIntent and fanSpeed and equipRef == \"$equipId\"", fanSpeed!!.ordinal.toDouble())
            hayStack.writeHisValByQuery("userIntent and fanSpeed and equipRef == \"$equipId\"", fanSpeed!!.ordinal.toDouble())

        }
        override fun onNothingSelected(parent: AdapterView<*>) {
            // write code to perform some action
        }
    }
}




