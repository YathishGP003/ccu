package a75f.io.renatus.dabextahu

import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.RadioButtonCompose
import a75f.io.renatus.compose.SetPointConfig
import a75f.io.renatus.compose.SetPointControlCompose
import a75f.io.renatus.compose.TextViewWithClick
import a75f.io.renatus.compose.TitleTextView
import a75f.io.renatus.modbus.ModelSelectionFragment
import a75f.io.renatus.modbus.util.EQUIP_TYPE
import a75f.io.renatus.modbus.util.MODBUS
import a75f.io.renatus.modbus.util.SEARCH_MODEL
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider

/**
 * Created by Manjunath K on 06-06-2023.
 */

class DabExternalAhuControlConfigFragment : Fragment() {
    private lateinit var viewModel: AhuControlViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext())
        viewModel = ViewModelProvider(this)[AhuControlViewModel::class.java]
        viewModel.configModelDefinition(NodeType.SMART_NODE, ProfileType.DAB_EXTERNAL_AHU,requireContext())
        rootView.apply {
            setContent {
                Box(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Row{
                            SetPointControlCompose(SET_POINT_CONTROL,state = viewModel.setPointControl) {
                                viewModel.setPointControl = it
                                if (!viewModel.setPointControl)
                                    viewModel.dualSetPointControl = false
                            }
                            if (viewModel.setPointControl) {
                                SetPointControlCompose(DUAL_SET_POINT_CONTROL,state = viewModel.dualSetPointControl) {
                                    viewModel.dualSetPointControl = it
                                }
                            }
                        }
                        Row{
                            if (viewModel.setPointControl && !viewModel.dualSetPointControl) {
                                Row {
                                    val items = viewModel.getOptions()
                                    Column {
                                        Row( modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                        ) {
                                            SetPointConfig(SAT_SP_MIN,viewModel.getIndexFromVal(viewModel.heatingMinSp), items)
                                            { selected -> viewModel.heatingMinSp = selected }

                                            SetPointConfig(SAT_SP_MAX,viewModel.getIndexFromVal(viewModel.heatingMaxSp), items)
                                            { selected -> viewModel.heatingMaxSp = selected }
                                        }
                                    }
                                }
                            }
                        }
                        Row{
                            if (viewModel.dualSetPointControl) {
                                Row {
                                    val items = viewModel.getOptions()
                                    Column {
                                        Row( modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()) {

                                            SetPointConfig(SAT_HEATING_SP_MIN,viewModel.getIndexFromVal(viewModel.heatingMinSp), items)
                                            { selected -> viewModel.heatingMinSp = selected }

                                            SetPointConfig(SAT_HEATING_SP_MAX,viewModel.getIndexFromVal(viewModel.heatingMaxSp), items)
                                            { selected -> viewModel.heatingMaxSp = selected }
                                        }
                                        Row( modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()) {

                                            SetPointConfig(SAT_COOLING_SP_MIN,viewModel.getIndexFromVal(viewModel.heatingMinSp), items)
                                            { selected -> viewModel.heatingMinSp = selected }

                                            SetPointConfig(SAT_COOLING_SP_MAX,viewModel.getIndexFromVal(viewModel.heatingMaxSp), items)
                                            { selected -> viewModel.heatingMaxSp = selected }
                                        }
                                    }
                                }
                            }
                        }
                        Row{
                            SetPointControlCompose(
                                FAN_SP_CONTROL,
                                state = viewModel.fanStaticSetPointControl
                            ) {
                                viewModel.fanStaticSetPointControl = it
                            }
                        }
                        Row{
                            if (viewModel.fanStaticSetPointControl) {
                                Row {
                                    val items = viewModel.getOptions()
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                        ) {
                                            SetPointConfig(FAN_SP_MIN,viewModel.getIndexFromVal(viewModel.heatingMinSp), items)
                                            { selected -> viewModel.heatingMinSp = selected }

                                            SetPointConfig(FAN_SP_MAX,viewModel.getIndexFromVal(viewModel.heatingMaxSp), items)
                                            { selected -> viewModel.heatingMaxSp = selected }

                                        }
                                    }
                                }
                            }
                        }
                        Row{
                            SetPointControlCompose(
                                DCV_CONTROL_LABEL,
                                state = viewModel.dcvControl
                            ) {
                                viewModel.dcvControl = it
                            }
                        }
                        Row{
                            if (viewModel.dcvControl) {
                                Row {
                                    val items = viewModel.getOptions()
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                        ) {
                                            SetPointConfig(DCV_CONTROL_MIN,viewModel.getIndexFromVal(viewModel.heatingMinSp), items)
                                            { selected -> viewModel.heatingMinSp = selected }

                                            SetPointConfig(DCV_CONTROL_MAX,viewModel.getIndexFromVal(viewModel.heatingMaxSp), items)
                                            { selected -> viewModel.heatingMaxSp = selected }
                                        }
                                    }
                                }
                            }
                        }
                        Row{
                            SetPointControlCompose(
                                OCCUPANCY_CONTROL_LABEL,
                                state = viewModel.occupancyMode
                            ) {
                                viewModel.occupancyMode = it
                            }
                        }
                        Row{
                            SetPointControlCompose(
                                HUMIDIFIER_CONTROL_LABEL,
                                state = viewModel.humidifierControl
                            ) {
                                viewModel.humidifierControl = it
                            }
                        }
                        Row{
                            if (viewModel.humidifierControl) {
                                Row {
                                    val items = viewModel.getOptions()
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                        ) {
                                            SetPointConfig(TARGET_HUMIDIFIER,viewModel.getIndexFromVal(viewModel.heatingMinSp), items)
                                            { selected -> viewModel.heatingMinSp = selected }
                                        }
                                    }
                                }
                            }
                        }
                        Row{
                            SetPointControlCompose(
                                DEHUMIDIFIER_CONTROL_LABEL,
                                state = viewModel.dehumidifierControl
                            ) {
                                viewModel.dehumidifierControl = it
                            }
                        }
                        Row{
                            if (viewModel.dehumidifierControl) {
                                Row {
                                    val items = viewModel.getOptions()
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                        ) {
                                            SetPointConfig(TARGET_DEHUMIDIFIER,viewModel.getIndexFromVal(viewModel.heatingMinSp), items)
                                            { selected -> viewModel.heatingMinSp = selected }
                                        }
                                    }
                                }
                            }
                        }
                        Row {
                            Row {
                                val radioOptions = listOf("BACnet", "Modbus")
                                RadioButtonCompose(radioOptions)

                                /*if (viewModel.equipModel.value.isDevicePaired) {
                                    viewModel.modelName.value =
                                        getName(viewModel.equipModel.value.equipDevice.value.name)
                                    TextViewWithClick(
                                        text = viewModel.modelName,
                                        onClick = { },
                                        enableClick = false,
                                        isCompress = false
                                    )
                                    HeaderTextView(viewModel.equipModel.value.equipDevice.value.modbusEquipIdId)
                                } else {
                                    TextViewWithClick(
                                        text = viewModel.modelName,
                                        onClick = {
                                            showDialogFragment(
                                                ModelSelectionFragment.newInstance(
                                                    viewModel.deviceList,
                                                    viewModel.onItemSelect, SEARCH_MODEL
                                                ), ModelSelectionFragment.ID
                                            )
                                        },
                                        enableClick = true, isCompress = false
                                    )
                                    HeaderTextView(viewModel.equipModel.value.version.value)
                                }*/
                            }
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) { HeaderTextView(EQUIP_TYPE) }
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Row {
                                    val radioOptions = listOf("Mangoes", "Apple", "Melons")
                                    RadioButtonCompose(radioOptions)

                                    /*if (viewModel.equipModel.value.isDevicePaired) {
                                        viewModel.modelName.value =
                                            getName(viewModel.equipModel.value.equipDevice.value.name)
                                        TextViewWithClick(
                                            text = viewModel.modelName,
                                            onClick = { },
                                            enableClick = false,
                                            isCompress = false
                                        )
                                        HeaderTextView(viewModel.equipModel.value.equipDevice.value.modbusEquipIdId)
                                    } else {
                                        TextViewWithClick(
                                            text = viewModel.modelName,
                                            onClick = {
                                                showDialogFragment(
                                                    ModelSelectionFragment.newInstance(
                                                        viewModel.deviceList,
                                                        viewModel.onItemSelect, SEARCH_MODEL
                                                    ), ModelSelectionFragment.ID
                                                )
                                            },
                                            enableClick = true, isCompress = false
                                        )
                                        HeaderTextView(viewModel.equipModel.value.version.value)
                                    }*/
                                }
                            }
                        }

                        Row{

                            Button(
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(android.graphics.Color.parseColor("#E24301")),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color.Gray,
                                    disabledContentColor = Color.LightGray
                                ),
                                onClick = { viewModel.saveConfiguration() }
                            ) {
                                Text(
                                    style = TextStyle(
                                        fontFamily = FontFamily.SansSerif,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 18.sp,
                                        color = Color.Black
                                    ),
                                    text = "Save"
                                )
                            }
                        }
                    }
                }
            }
        }
        return rootView
    }

}


