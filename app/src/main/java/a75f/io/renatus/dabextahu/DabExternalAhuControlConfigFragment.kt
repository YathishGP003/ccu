package a75f.io.renatus.dabextahu

import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.compose.HeaderLeftAlignedTextView
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.LabelTextView
import a75f.io.renatus.compose.ParameterLabel
import a75f.io.renatus.compose.RadioButtonCompose
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.SetPointConfig
import a75f.io.renatus.compose.SetPointControlCompose
import a75f.io.renatus.compose.TextViewWithClick
import a75f.io.renatus.compose.TextViewWithClickOption
import a75f.io.renatus.compose.ToggleButton
import a75f.io.renatus.modbus.ModelSelectionFragment
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.modbus.util.CANCEL
import a75f.io.renatus.modbus.util.EQUIP_TYPE
import a75f.io.renatus.modbus.util.LOADING
import a75f.io.renatus.modbus.util.MODBUS
import a75f.io.renatus.modbus.util.OnItemSelect
import a75f.io.renatus.modbus.util.SEARCH_MODEL
import a75f.io.renatus.modbus.util.SEARCH_SLAVE_ID
import a75f.io.renatus.modbus.util.SELECT_ALL
import a75f.io.renatus.modbus.util.SET
import a75f.io.renatus.modbus.util.SLAVE_ID
import a75f.io.renatus.modbus.util.showErrorDialog
import a75f.io.renatus.util.ProgressDialogUtils
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
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
        ProgressDialogUtils.showProgressDialog(requireContext(), LOADING)
        viewModel = ViewModelProvider(this)[AhuControlViewModel::class.java]
        viewModel.configModelDefinition(
            NodeType.SMART_NODE, ProfileType.DAB_EXTERNAL_AHU, requireContext()
        )

        rootView.apply {
            setContent {
                if (viewModel.profileModelDefination != null) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                    ) {
                        item {
                            Row {
                                SetPointControlCompose(
                                    viewModel.configModel.value.controlName(
                                        viewModel.profileModelDefination,
                                        SET_POINT_CONTROL
                                    ), state = viewModel.configModel.value.setPointControl
                                ) {
                                    viewModel.configModel.value.setPointControl = it
                                    if (!viewModel.configModel.value.setPointControl) viewModel.configModel.value.dualSetPointControl =
                                        false
                                }
                                if (viewModel.configModel.value.setPointControl) {
                                    SetPointControlCompose(
                                        viewModel.configModel.value.controlName(
                                            viewModel.profileModelDefination,
                                            DUAL_SET_POINT_CONTROL
                                        ),
                                        state = viewModel.configModel.value.dualSetPointControl
                                    ) {
                                        viewModel.configModel.value.dualSetPointControl = it
                                    }
                                }
                            }
                            Row {
                                if (viewModel.configModel.value.setPointControl && !viewModel.configModel.value.dualSetPointControl) {
                                    Row {

                                        Column {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight()
                                            ) {
                                                val satMin = viewModel.configModel.value.getPointByDomainName(viewModel.profileModelDefination,SAT_SP_MIN)
                                                val satMax = viewModel.configModel.value.getPointByDomainName(viewModel.profileModelDefination,SAT_SP_MAX)
                                                if (satMin != null ) {
                                                    SetPointConfig(
                                                        satMin.name,
                                                        (satMin.defaultValue ?: 0).toString(),
                                                        viewModel.itemsFromMinMax(satMin.valueConstraint.minValue.),
                                                        satMin.defaultUnit ?: EMPTY,
                                                    ) { selected ->
                                                        viewModel.heatingMinSp = selected
                                                    }
                                                }
                                                SetPointConfig(
                                                    satMax?.name ?: NOT_FOUND,
                                                    (satMax?.defaultValue ?: 0).toString(),
                                                    items, satMax?.defaultUnit ?: EMPTY,
                                                ) { selected -> viewModel.heatingMaxSp = selected }
                                            }
                                        }
                                    }
                                }
                            }
                            Row {
                                if (viewModel.configModel.value.dualSetPointControl) {
                                    Row {
                                        val items = viewModel.itemsFromMinMax()
                                        Column {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight()
                                            ) {
                                                val satHeatingMin = viewModel.configModel.value.getPointByDomainName(viewModel.profileModelDefination,SAT_HEATING_SP_MIN)
                                                val satHeatingMax = viewModel.configModel.value.getPointByDomainName(viewModel.profileModelDefination,SAT_HEATING_SP_MAX)
                                                val satCoolingMin = viewModel.configModel.value.getPointByDomainName(viewModel.profileModelDefination,SAT_COOLING_SP_MIN)
                                                val satCoolingMax = viewModel.configModel.value.getPointByDomainName(viewModel.profileModelDefination,SAT_COOLING_SP_MAX)
                                                SetPointConfig(
                                                    viewModel.configModel.value.controlName(
                                                        viewModel.profileModelDefination,
                                                        SAT_HEATING_SP_MIN
                                                    ),
                                                    "0",
                                                    items, "F",
                                                ) { selected -> viewModel.heatingMinSp = selected }

                                                SetPointConfig(
                                                    viewModel.configModel.value.controlName(
                                                        viewModel.profileModelDefination,
                                                        SAT_HEATING_SP_MAX
                                                    ),
                                                    "0",
                                                    items, "F",
                                                ) { selected -> viewModel.heatingMaxSp = selected }
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight()
                                            ) {

                                                SetPointConfig(
                                                    viewModel.configModel.value.controlName(
                                                        viewModel.profileModelDefination,
                                                        SAT_COOLING_SP_MIN
                                                    ),
                                                    "0",
                                                    items, "F",
                                                ) { selected -> viewModel.heatingMinSp = selected }

                                                SetPointConfig(
                                                    viewModel.configModel.value.controlName(
                                                        viewModel.profileModelDefination,
                                                        SAT_COOLING_SP_MAX
                                                    ),
                                                    "0",
                                                    items, "F",
                                                ) { selected -> viewModel.heatingMaxSp = selected }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            SetPointControlCompose(
                                viewModel.configModel.value.controlName(
                                    viewModel.profileModelDefination,
                                    FAN_SP_CONTROL
                                ),
                                state = viewModel.configModel.value.fanStaticSetPointControl
                            ) {
                                viewModel.configModel.value.fanStaticSetPointControl = it
                            }
                        }
                        item {
                            if (viewModel.configModel.value.fanStaticSetPointControl) {
                                Row {
                                    val items = viewModel.itemsFromMinMax()
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                        ) {
                                            SetPointConfig(
                                                viewModel.configModel.value.controlName(
                                                    viewModel.profileModelDefination,
                                                    FAN_SP_MIN
                                                ),
                                                "0",
                                                items, "F",
                                            ) { selected -> viewModel.heatingMinSp = selected }

                                            SetPointConfig(
                                                viewModel.configModel.value.controlName(
                                                    viewModel.profileModelDefination,
                                                    FAN_SP_MAX
                                                ),
                                                "0",
                                                items, "F",
                                            ) { selected -> viewModel.heatingMaxSp = selected }

                                        }
                                    }
                                }
                            }
                        }
                        item {
                            SetPointControlCompose(
                                viewModel.configModel.value.controlName(
                                    viewModel.profileModelDefination,
                                    DCV_CONTROL_LABEL
                                ), state = viewModel.configModel.value.dcvControl
                            ) {
                                viewModel.configModel.value.dcvControl = it
                            }
                        }
                        item {
                            if (viewModel.configModel.value.dcvControl) {
                                Row {
                                    val items = viewModel.itemsFromMinMax()
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                        ) {
                                            SetPointConfig(
                                                viewModel.configModel.value.controlName(
                                                    viewModel.profileModelDefination,
                                                    DCV_CONTROL_MIN
                                                ),
                                                "0",
                                                items, "F",
                                            ) { selected -> viewModel.heatingMinSp = selected }

                                            SetPointConfig(
                                                viewModel.configModel.value.controlName(
                                                    viewModel.profileModelDefination,
                                                    DCV_CONTROL_MAX
                                                ),
                                                "0",
                                                items, "F",
                                            ) { selected -> viewModel.heatingMaxSp = selected }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            SetPointControlCompose(
                                viewModel.configModel.value.controlName(
                                    viewModel.profileModelDefination,
                                    OCCUPANCY_CONTROL_LABEL
                                ),
                                state = viewModel.configModel.value.occupancyMode
                            ) {
                                viewModel.configModel.value.occupancyMode = it
                            }
                        }
                        item {
                            SetPointControlCompose(
                                viewModel.configModel.value.controlName(
                                    viewModel.profileModelDefination,
                                    HUMIDIFIER_CONTROL_LABEL
                                ),
                                state = viewModel.configModel.value.humidifierControl
                            ) {
                                viewModel.configModel.value.humidifierControl = it
                            }
                        }
                        item {
                            if (viewModel.configModel.value.humidifierControl) {
                                Row {
                                    val items = viewModel.itemsFromMinMax()
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                        ) {
                                            SetPointConfig(
                                                viewModel.configModel.value.controlName(
                                                    viewModel.profileModelDefination,
                                                    TARGET_HUMIDIFIER
                                                ),
                                                "0",
                                                items, "F",
                                            ) { selected -> viewModel.heatingMinSp = selected }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            SetPointControlCompose(
                                viewModel.configModel.value.controlName(
                                    viewModel.profileModelDefination,
                                    DEHUMIDIFIER_CONTROL_LABEL
                                ),
                                state = viewModel.configModel.value.dehumidifierControl
                            ) {
                                viewModel.configModel.value.dehumidifierControl = it
                            }
                        }
                        item {
                            if (viewModel.configModel.value.dehumidifierControl) {
                                Row {
                                    val items = viewModel.itemsFromMinMax()
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                        ) {
                                            SetPointConfig(
                                                viewModel.configModel.value.controlName(
                                                    viewModel.profileModelDefination,
                                                    TARGET_DEHUMIDIFIER
                                                ),
                                                "0",
                                                items, "F",
                                            ) { selected -> viewModel.heatingMinSp = selected }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                val radioOptions = listOf(BACNET, MODBUS)
                                RadioButtonCompose(
                                    radioOptions,
                                    viewModel.configType.value.ordinal
                                ) {
                                    when (it) {
                                        BACNET -> {
                                            viewModel.configType.value =
                                                AhuControlViewModel.ConfigType.BACNET
                                        }

                                        MODBUS -> {
                                            viewModel.configType.value =
                                                AhuControlViewModel.ConfigType.MODBUS
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            if (viewModel.configType.value == AhuControlViewModel.ConfigType.MODBUS) {
                                ModbusConfig()
                            } else {
                                BacnetConfig()
                            }
                        }
                        item {
                            SaveConfig()
                        }

                    }
                } else {
                    showErrorDialog(requireContext(), LOADING_ERROR)
                }
            }
        }
        return rootView
    }

    @Composable
    fun SaveConfig() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(top = 20.dp)),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(PaddingValues(bottom = 10.dp, end = 10.dp)),
                contentAlignment = Alignment.Center
            ) { SaveTextView(CANCEL) { } }
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(PaddingValues(bottom = 10.dp, end = 10.dp)),
                contentAlignment = Alignment.Center
            ) {
                SaveTextView(SET) { viewModel.saveConfiguration() }
            }
        }
    }

    @Composable
    fun BacnetConfig() {
        /**
         * Add Bacnet configuration here
         */
    }

    @Composable
    fun ModbusConfig() {
        viewModel.configModbusDetails()
        Row {
            Box(
                modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
            ) { HeaderTextView(EQUIP_TYPE) }
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row {
                if (viewModel.equipModel.value.isDevicePaired) {
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
                        text = viewModel.modelName, onClick = {
                            showDialogFragment(
                                ModelSelectionFragment.newInstance(
                                    viewModel.deviceList, viewModel.onItemSelect, SEARCH_MODEL
                                ), ModelSelectionFragment.ID
                            )
                        }, enableClick = true, isCompress = false
                    )
                    HeaderTextView(viewModel.equipModel.value.version.value)
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {

            Box(modifier = Modifier.weight(7f)) {
                HeaderLeftAlignedTextView(
                    if (viewModel.equipModel.value.equipDevice.value.name.isNullOrEmpty()) "" else getName(
                        viewModel.equipModel.value.equipDevice.value.name
                    )
                )
            }
            Box(modifier = Modifier.weight(1f)) { HeaderTextView(SLAVE_ID) }
            Box(modifier = Modifier.weight(1f)) {
                val onItemSelect = object : OnItemSelect {
                    override fun onItemSelected(index: Int, item: String) {
                        viewModel.equipModel.value.slaveId.value = item.toInt()
                    }
                }
                TextViewWithClickOption(
                    text = viewModel.equipModel.value.slaveId,
                    onClick = {
                        ProgressDialogUtils.showProgressDialog(context, LOADING)
                        showDialogFragment(
                            ModelSelectionFragment.newInstance(
                                viewModel.slaveIdList, onItemSelect, SEARCH_SLAVE_ID
                            ), ModelSelectionFragment.ID
                        )
                    },
                    enableClick = !viewModel.equipModel.value.isDevicePaired,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(bottom = 5.dp)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderTextView(SELECT_ALL)
            ToggleButton(defaultSelection = viewModel.equipModel.value.selectAllParameters.value) {
                viewModel.equipModel.value.selectAllParameters.value = it
                viewModel.onSelectAll(it)
            }
        }
        Row(modifier = Modifier.padding(start = 10.dp)) { ParameterLabel() }
        ParametersListView(data = viewModel.equipModel)
    }


    @Composable
    fun ParametersListView(data: MutableState<EquipModel>) {
        if (data.value.parameters.isNotEmpty()) {
            var index = 0
            while (index < data.value.parameters.size) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Row {
                        for (rowIndex in 0 until 2) {
                            if (index < data.value.parameters.size) {
                                val item = data.value.parameters[index]
                                LabelTextView(
                                    if (item.param.value.name.length > 30) item.param.value.name.substring(
                                        0,
                                        30
                                    )
                                    else item.param.value.name
                                )
                                ToggleButton(item.displayInUi.value) {
                                    item.displayInUi.value = it
                                    item.param.value.getParameterId()
                                    viewModel.updateSelectAll()
                                }
                                Box(modifier = Modifier.width(50.dp)) { }
                                index++
                            }
                        }
                    }
                }
            }
        }
    }

    fun getName(name: String): String {
        return if (name.length > 30) name.substring(0, 30) else name
    }

    fun showDialogFragment(dialogFragment: DialogFragment, id: String?) {
        val fragmentManager = requireActivity().supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        val prev = fragmentManager.findFragmentByTag(id)
        if (prev != null) {
            transaction.remove(prev)
        }
        transaction.addToBackStack(null)
        dialogFragment.show(transaction, id)
    }

}


