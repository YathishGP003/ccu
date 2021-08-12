package a75f.io.renatus.hyperstat.vrv

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.Globals
import a75f.io.logic.bo.building.vrv.VrvMasterController
import a75f.io.logic.bo.building.vrv.VrvOperationMode
import a75f.io.renatus.R
import a75f.io.renatus.util.HeartBeatUtil
import android.app.Activity
import android.app.PendingIntent.getActivity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

fun loadView(pointMap : HashMap<String, String>,
             inflater : LayoutInflater,
             layout : LinearLayout,
             equipId : String,
             hayStack: CCUHsApi,
             context: Activity,
             nodeAddress : String) {

    val masterOperationMode: View = inflater.inflate(R.layout.zone_item_type3, null)
    val masterOpModeLabel: TextView = masterOperationMode.findViewById(R.id.text_label)
    val masterOpModeVal: TextView = masterOperationMode.findViewById(R.id.text_value)
    masterOpModeLabel.text = "Master Operation Mode : "
    masterOpModeVal.text = "Cool Only Mode" //TODO

    layout.addView(masterOperationMode)



    val viewTitle: View = inflater.inflate(R.layout.zones_item_title, null)
    val textViewTitle = viewTitle.findViewById<TextView>(R.id.textProfile)
    val textViewModule = viewTitle.findViewById<TextView>(R.id.module_status)
    textViewTitle.text = pointMap["Profile"].toString() + "($nodeAddress)"

    HeartBeatUtil.moduleStatus(textViewModule, nodeAddress)
    layout.addView(viewTitle)

    val viewPointRow1 = inflater.inflate(R.layout.zones_item_type2, null)
    viewPointRow1.findViewById<TextView>(R.id.text_point1label).text = "Operation Mode : "
    viewPointRow1.findViewById<TextView>(R.id.text_point2label).text = "Fan Speed : "

    val opModeSp = viewPointRow1.findViewById<Spinner>(R.id.spinnerValue1)
    val fanSpeedSp = viewPointRow1.findViewById<Spinner>(R.id.spinnerValue2)
    layout.addView(viewPointRow1)

    val viewPointRow2 = inflater.inflate(R.layout.zones_item_type2, null)
    viewPointRow2.findViewById<TextView>(R.id.text_point1label).text = "Airflow Direction : "
    viewPointRow2.findViewById<TextView>(R.id.text_point2label).text = "Master Controller : "

    val airflowDirSp = viewPointRow1.findViewById<Spinner>(R.id.spinnerValue1)
    val masterControlSp = viewPointRow1.findViewById<Spinner>(R.id.spinnerValue2)
    layout.addView(viewPointRow2)

    val humidityView: View = inflater.inflate(R.layout.zone_item_single_eletement_row, null)
    humidityView.findViewById<TextView>(R.id.text_label).text = "Humidity : "
    val humidityVal: TextView = humidityView.findViewById(R.id.text_value)
    humidityVal.text = "0"+"% RH" //TODO

    layout.addView(humidityView)

    setUpOperationModeSpinner(pointMap, opModeSp, equipId, hayStack, context)

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

private fun setUpOperationModeSpinner(pointMap : HashMap<String, String>,
                                opModeSpinner : Spinner,
                                equipId: String,
                                hayStack: CCUHsApi,
                                context : Activity) {
    val masterControllerMode = hayStack.readHisValByQuery("point and masterController and mode " +
                                                "and equipRef == \"$equipId\"")

    val masterOperationMode = hayStack.readHisValByQuery("point and master and operation and mode " +
            "and equipRef == \"$equipId\"")

    val opModeList : MutableList<String> = arrayListOf()

    VrvOperationMode.values().forEach { mode ->
        if (masterControllerMode.toInt() == 1) {
            opModeList.add(mode.name)
        } else {
            if (mode != VrvOperationMode.AUTO) {
                opModeList.add(mode.name)
            }
        }
    }

    val adapter:ArrayAdapter<String> = object: ArrayAdapter<String>(
        context,
        android.R.layout.simple_spinner_dropdown_item,
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
    opModeSpinner.onItemSelectedListener = object :
        AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>,
                                    view: View, position: Int, id: Long) {
            hayStack.writeDefaultVal("userIntent and operation and mode", position.toDouble())
            hayStack.writeHisValByQuery("userIntent and operation and mode", position.toDouble())

        }

        override fun onNothingSelected(parent: AdapterView<*>) {
            // write code to perform some action
        }
    }

}



