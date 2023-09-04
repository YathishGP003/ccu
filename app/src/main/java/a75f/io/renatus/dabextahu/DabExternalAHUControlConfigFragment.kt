package a75f.io.renatus.dabextahu

import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.LabelTextView
import a75f.io.renatus.compose.SetPointControlCompose
import a75f.io.renatus.compose.ToggleButton
import a75f.io.renatus.compose.SpinnerElement
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

class DabExternalAHUControlConfigFragment : Fragment() {
    private lateinit var viewModel: AhuControlViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext())
        viewModel = ViewModelProvider(this)[AhuControlViewModel::class.java]
        viewModel.configModelDefinition(NodeType.SMART_NODE,
            ProfileType.DAB_EXTERNAL_AHU,requireContext())
        rootView.apply {
            setContent {
                Box(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp)
                        ) {

                            SetPointControlCompose(SET_POINT_CONTROL,state = viewModel.setPointControl) {
                                viewModel.setPointControl = it
                            }
                            if (viewModel.setPointControl) {
                                SetPointControlCompose(DUAL_SET_POINT_CONTROL,state = viewModel.dualSetPointControl) {
                                    viewModel.dualSetPointControl = it
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.padding(10.dp)
                        ) {

                            if (viewModel.setPointControl) {
                                Row {
                                    val items = viewModel.getOptions()
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .width(1200.dp)
                                                .wrapContentHeight()
                                        ) {
                                            LabelTextView(text = "SAT Heating Setpoint Min")
                                            SpinnerElement(
                                                viewModel.getIndexFromVal(viewModel.heatingMinSp),
                                                items
                                            ) { selected ->
                                                viewModel.heatingMinSp = selected
                                            }
                                            LabelTextView(text = "SAT Heating Setpoint Max")
                                            SpinnerElement(
                                                viewModel.getIndexFromVal(viewModel.heatingMaxSp),
                                                items
                                            ) { selected ->
                                                viewModel.heatingMaxSp = selected
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .width(1200.dp)
                                                .wrapContentHeight()
                                        ) {
                                            LabelTextView(text = "SAT Cooling Setpoint Min")
                                            SpinnerElement(
                                                viewModel.getIndexFromVal(viewModel.coolingMinSp),
                                                items
                                            ) { selected ->
                                                viewModel.coolingMinSp = selected
                                            }
                                            LabelTextView(text = "SAT Cooling Setpoint Max")
                                            SpinnerElement(
                                                viewModel.getIndexFromVal(viewModel.coolingMaxSp),
                                                items
                                            ) { selected ->
                                                viewModel.coolingMaxSp = selected
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.padding(10.dp)
                        ) {

                            SetPointControlCompose(
                                FAN_SP_CONTROL,
                                state = viewModel.fanStaticSetPointControl
                            ) {
                                viewModel.fanStaticSetPointControl = it
                            }

                        }
                        Row(
                            modifier = Modifier.padding(10.dp)
                        ) {

                            SetPointControlCompose(
                                DCV_CONTROL_LABEL,
                                state = viewModel.dcvControl
                            ) {
                                viewModel.dcvControl = it
                            }
                        }
                        Row(
                            modifier = Modifier.padding(10.dp)
                        ) {

                            SetPointControlCompose(
                                OCCUPANCY_CONTROL_LABEL,
                                state = viewModel.occupancyMode
                            ) {
                                viewModel.occupancyMode = it
                            }
                        }
                        Row(
                            modifier = Modifier.padding(10.dp)
                        ) {

                            SetPointControlCompose(
                                HUMIDIFIER_CONTROL_LABEL,
                                state = viewModel.humidifierControl
                            ) {
                                viewModel.humidifierControl = it
                            }
                        }
                        Row(
                            modifier = Modifier.padding(10.dp)
                        ) {

                            SetPointControlCompose(
                                DEHUMIDIFIER_CONTROL_LABEL,
                                state = viewModel.dehumidifierControl
                            ) {
                                viewModel.dehumidifierControl = it
                            }
                        }
                        Row(
                            modifier = Modifier.padding(10.dp)
                        ) {

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


