package a75f.io.renatus.profiles.mystat

import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.common.HSZoneStatus
import a75f.io.logic.bo.util.UnitUtils
import a75f.io.renatus.R
import a75f.io.renatus.profiles.system.advancedahu.Option
import a75f.io.renatus.util.HeartBeatUtil
import a75f.io.renatus.views.CustomSpinnerDropDownAdapter
import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.common.point.Constraint
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint

/**
 * Created by Manjunath K on 16-01-2025.
 */


const val MYSTAT: String = "MyStat"
const val CPU: String = "Conventional Package Unit"
const val PIPE2: String = "2 Pipe FCU"
const val PIPE4: String ="4 Pipe FCU"
const val HPU: String = "Heat Pump Unit"

var minMaxVoltage = List(11) { Option(it, it.toString()) }
var testVoltage = List(101) { Option(it, it.toString()) }
var damperOpeningRate = (10..100 step 10).toList().map { Option(it, it.toString()) }

fun getPointByDomainName(
    modelDefinition: SeventyFiveFProfileDirective,
    domainName: String
): SeventyFiveFProfilePointDef? {
    return modelDefinition.points.find { (it.domainName.contentEquals(domainName)) }
}

fun getAllowedValues(domainName: String, model: SeventyFiveFProfileDirective): List<Option> {
    val pointDef = getPointByDomainName(model, domainName) ?: return emptyList()
    return if (pointDef.valueConstraint.constraintType == Constraint.ConstraintType.MULTI_STATE) {
        val constraint = pointDef.valueConstraint as MultiStateConstraint
        val enums = mutableListOf<Option>()
        constraint.allowedValues.forEach {
            enums.add(Option(it.index, it.value, it.dis))
        }
        enums
    } else {
        emptyList()
    }
}

fun showTextView(viewId: Int, rootView: View, text: String) {
    val textView = rootView.findViewById<TextView>(viewId)
    textView.text = text
}

 fun setMyStatTitleStatusConfig(
    viewTitle: View, viewStatus: View, nodeAddress: String, status: String, profileName: String) {
    val textViewModule = viewTitle.findViewById<TextView>(R.id.module_status)
    HeartBeatUtil.moduleStatus(textViewModule, nodeAddress)
    showTextView(
        R.id.textProfile,
        viewTitle,
        "$MYSTAT - $profileName ( $nodeAddress )"
    )
    showTextView(R.id.text_status, viewStatus, status)
    showTextView(R.id.last_updated_status,
        viewStatus,
        HeartBeatUtil.getLastUpdatedTime(nodeAddress)
    )
}

@SuppressLint("DefaultLocale")
fun showMyStatDischargeConfigIfRequired(dischargeView: View, pointsList: HashMap<String,Any>, rootView: LinearLayout) {
    if (pointsList.containsKey(HSZoneStatus.DISCHARGE_AIRFLOW.name)) {
        var dischargeValue = pointsList[HSZoneStatus.DISCHARGE_AIRFLOW.name].toString() + " ℉"
        if (UnitUtils.isCelsiusTunerAvailableStatus()) {
            val converted = UnitUtils.fahrenheitToCelsiusTwoDecimal(pointsList[HSZoneStatus.DISCHARGE_AIRFLOW.name] as Double)
            dischargeValue = "${String.format("%.2f", converted)} °C"
        }
        showTextView(R.id.text_airflowValue, dischargeView, dischargeValue)
        rootView.removeView(dischargeView)
        rootView.addView(dischargeView)
    } else {
        rootView.removeView(dischargeView)
    }
}

@SuppressLint("DefaultLocale")
fun showMyStatSupplyTemp(dischargeView: View, pointsList: HashMap<String,Any>, rootView: LinearLayout) {
    val profileType = pointsList[HSZoneStatus.PROFILE_TYPE.name] as ProfileType
    val textView = dischargeView.findViewById<TextView>(R.id.text_discharge_airflow)
    textView.text = "Supply Water Temperature: "
    if (profileType == ProfileType.MYSTAT_PIPE2) {
        var supplyTemp = pointsList[HSZoneStatus.SUPPLY_TEMP.name].toString() + " ℉"
        if (UnitUtils.isCelsiusTunerAvailableStatus()) {
            val converted =
                UnitUtils.fahrenheitToCelsiusTwoDecimal(pointsList[HSZoneStatus.SUPPLY_TEMP.name] as Double)
            supplyTemp = "${String.format("%.2f", converted)} °C"
        }
        showTextView(R.id.text_airflowValue, dischargeView, supplyTemp)
        rootView.removeView(dischargeView)
        rootView.addView(dischargeView)
    }
}

fun getMyStatAdapterValue(context : Context, itemArray : Int) = CustomSpinnerDropDownAdapter( context, R.layout.spinner_zone_item, context.resources.getStringArray(itemArray).toMutableList())
