package a75f.io.renatus.hyperstat.vrv

import a75f.io.renatus.R
import a75f.io.renatus.util.HeartBeatUtil
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView

fun loadView(pointMap : HashMap<String, String>,
             inflater : LayoutInflater,
             layout : LinearLayout,
             equipId : String,
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
    viewPointRow1.findViewById<TextView>(R.id.text_point1label).text = "Airflow Direction : "
    viewPointRow1.findViewById<TextView>(R.id.text_point2label).text = "Master Controller : "

    val airflowDirSp = viewPointRow1.findViewById<Spinner>(R.id.spinnerValue1)
    val masterControlSp = viewPointRow1.findViewById<Spinner>(R.id.spinnerValue2)
    layout.addView(viewPointRow2)

    val humidityView: View = inflater.inflate(R.layout.zone_item_single_eletement_row, null)
    humidityView.findViewById<TextView>(R.id.text_label).text = "Humidity : "
    val humidityVal: TextView = masterOperationMode.findViewById(R.id.text_value)
    humidityVal.text = " "+"% RH" //TODO

    layout.addView(humidityView)
}

