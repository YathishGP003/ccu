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
        viewModel = ViewModelProvider(this)[AhuControlViewModel::class.java]
        viewModel.configModelDefinition(
            NodeType.SMART_NODE, ProfileType.DAB_EXTERNAL_AHU, requireContext()
        )
        rootView.apply {
            setContent {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                ) {
                    item {
                        Row {
                            Row {

                            }
                            SetPointControlCompose(
                                SET_POINT_CONTROL, state = viewModel.setPointControl
                            ) {
                                viewModel.setPointControl = it
                                if (!viewModel.setPointControl) viewModel.dualSetPointControl =
                                    false
                            }
                            if (viewModel.setPointControl) {
                                SetPointControlCompose(
                                    DUAL_SET_POINT_CONTROL, state = viewModel.dualSetPointControl
                                ) {
                                    viewModel.dualSetPointControl = it
                                }
                            }
                        }
                        Row {
                            if (viewModel.setPointControl && !viewModel.dualSetPointControl) {
                                Row {
                                    val items = viewModel.getOptions()
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                        ) {
                                            SetPointConfig(
                                                SAT_SP_MIN,
                                                viewModel.getIndexFromVal(viewModel.heatingMinSp),
                                                items
                                            ) { selected -> viewModel.heatingMinSp = selected }

                                            SetPointConfig(
                                                SAT_SP_MAX,
                                                viewModel.getIndexFromVal(viewModel.heatingMaxSp),
                                                items
                                            ) { selected -> viewModel.heatingMaxSp = selected }
                                        }
                                    }
                                }
                            }
                        }
                        Row {
                            if (viewModel.dualSetPointControl) {
                                Row {
                                    val items = viewModel.getOptions()
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                        ) {

                                            SetPointConfig(
                                                SAT_HEATING_SP_MIN,
                                                viewModel.getIndexFromVal(viewModel.heatingMinSp),
                                                items
                                            ) { selected -> viewModel.heatingMinSp = selected }

                                            SetPointConfig(
                                                SAT_HEATING_SP_MAX,
                                                viewModel.getIndexFromVal(viewModel.heatingMaxSp),
                                                items
                                            ) { selected -> viewModel.heatingMaxSp = selected }
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                        ) {

                                            SetPointConfig(
                                                SAT_COOLING_SP_MIN,
                                                viewModel.getIndexFromVal(viewModel.heatingMinSp),
                                                items
                                            ) { selected -> viewModel.heatingMinSp = selected }

                                            SetPointConfig(
                                                SAT_COOLING_SP_MAX,
                                                viewModel.getIndexFromVal(viewModel.heatingMaxSp),
                                                items
                                            ) { selected -> viewModel.heatingMaxSp = selected }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        SetPointControlCompose(
                            FAN_SP_CONTROL, state = viewModel.fanStaticSetPointControl
                        ) {
                            viewModel.fanStaticSetPointControl = it
                        }
                    }
                    item {
                        if (viewModel.fanStaticSetPointControl) {
                            Row {
                                val items = viewModel.getOptions()
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                    ) {
                                        SetPointConfig(
                                            FAN_SP_MIN,
                                            viewModel.getIndexFromVal(viewModel.heatingMinSp),
                                            items
                                        ) { selected -> viewModel.heatingMinSp = selected }

                                        SetPointConfig(
                                            FAN_SP_MAX,
                                            viewModel.getIndexFromVal(viewModel.heatingMaxSp),
                                            items
                                        ) { selected -> viewModel.heatingMaxSp = selected }

                                    }
                                }
                            }
                        }
                    }
                    item {
                        SetPointControlCompose(
                            DCV_CONTROL_LABEL, state = viewModel.dcvControl
                        ) {
                            viewModel.dcvControl = it
                        }
                    }
                    item {
                        if (viewModel.dcvControl) {
                            Row {
                                val items = viewModel.getOptions()
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                    ) {
                                        SetPointConfig(
                                            DCV_CONTROL_MIN,
                                            viewModel.getIndexFromVal(viewModel.heatingMinSp),
                                            items
                                        ) { selected -> viewModel.heatingMinSp = selected }

                                        SetPointConfig(
                                            DCV_CONTROL_MAX,
                                            viewModel.getIndexFromVal(viewModel.heatingMaxSp),
                                            items
                                        ) { selected -> viewModel.heatingMaxSp = selected }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        SetPointControlCompose(
                            OCCUPANCY_CONTROL_LABEL, state = viewModel.occupancyMode
                        ) {
                            viewModel.occupancyMode = it
                        }
                    }
                    item {
                        SetPointControlCompose(
                            HUMIDIFIER_CONTROL_LABEL, state = viewModel.humidifierControl
                        ) {
                            viewModel.humidifierControl = it
                        }
                    }
                    item {
                        if (viewModel.humidifierControl) {
                            Row {
                                val items = viewModel.getOptions()
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                    ) {
                                        SetPointConfig(
                                            TARGET_HUMIDIFIER,
                                            viewModel.getIndexFromVal(viewModel.heatingMinSp),
                                            items
                                        ) { selected -> viewModel.heatingMinSp = selected }
                                    }
                                }
                            }
                        }
                    }
                    item {
                        SetPointControlCompose(
                            DEHUMIDIFIER_CONTROL_LABEL, state = viewModel.dehumidifierControl
                        ) {
                            viewModel.dehumidifierControl = it
                        }
                    }
                    item {
                        if (viewModel.dehumidifierControl) {
                            Row {
                                val items = viewModel.getOptions()
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                    ) {
                                        SetPointConfig(
                                            TARGET_DEHUMIDIFIER,
                                            viewModel.getIndexFromVal(viewModel.heatingMinSp),
                                            items
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
                            RadioButtonCompose(radioOptions) {
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
            }
        }
        return rootView
    }

    @Composable
    fun SaveConfig() {
        Row {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(bottom = 20.dp, end = 10.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                SaveTextView(SET) { viewModel.saveConfiguration() }
            }
        }
        Row {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(bottom = 10.dp, end = 10.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                SaveTextView(CANCEL) {  }
            }
        }
    }

    @Composable
    fun BacnetConfig() {

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


