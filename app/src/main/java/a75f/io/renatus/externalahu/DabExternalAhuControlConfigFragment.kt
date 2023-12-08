package a75f.io.renatus.externalahu

import a75f.io.domain.api.DomainName.dcvDamperControlEnable
import a75f.io.domain.api.DomainName.dehumidifierOperationEnable
import a75f.io.domain.api.DomainName.dualSetpointControlEnable
import a75f.io.domain.api.DomainName.humidifierOperationEnable
import a75f.io.domain.api.DomainName.occupancyModeControl
import a75f.io.domain.api.DomainName.satSetpointControlEnable
import a75f.io.domain.api.DomainName.staticPressureSetpointControlEnable
import a75f.io.domain.api.DomainName.systemCoolingSATMaximum
import a75f.io.domain.api.DomainName.systemCoolingSATMinimum
import a75f.io.domain.api.DomainName.systemDCVDamperPosMaximum
import a75f.io.domain.api.DomainName.systemDCVDamperPosMinimum
import a75f.io.domain.api.DomainName.systemHeatingSATMaximum
import a75f.io.domain.api.DomainName.systemHeatingSATMinimum
import a75f.io.domain.api.DomainName.systemSATMaximum
import a75f.io.domain.api.DomainName.systemSATMinimum
import a75f.io.domain.api.DomainName.systemStaticPressureMaximum
import a75f.io.domain.api.DomainName.systemStaticPressureMinimum
import a75f.io.domain.api.DomainName.tagValueIncrement
import a75f.io.renatus.R
import a75f.io.renatus.compose.HeaderCenterLeftAlignedTextView
import a75f.io.renatus.compose.HeaderLeftAlignedTextView
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.LabelTextView
import a75f.io.renatus.compose.ParameterLabel
import a75f.io.renatus.compose.RadioButtonCompose
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.SetPointConfig
import a75f.io.renatus.compose.SetPointControlCompose
import a75f.io.renatus.compose.TextViewCompose
import a75f.io.renatus.compose.TextViewWithClick
import a75f.io.renatus.compose.TextViewWithClickOption
import a75f.io.renatus.compose.ToggleButton
import a75f.io.renatus.modbus.ModelSelectionFragment
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.modbus.util.CANCEL
import a75f.io.renatus.modbus.util.LOADING
import a75f.io.renatus.modbus.util.MODBUS
import a75f.io.renatus.modbus.util.OnItemSelect
import a75f.io.renatus.modbus.util.SAME_AS_PARENT
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
import io.seventyfivef.domainmodeler.common.point.NumericConstraint


/**
 * Created by Manjunath K on 06-06-2023.
 */

class DabExternalAhuControlConfigFragment : Fragment() {
    private lateinit var viewModel: ExternalAhuControlViewModel


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext())
        viewModel = ViewModelProvider(this)[ExternalAhuControlViewModel::class.java]
        rootView.apply {
            setContent {
                if (viewModel.profileModelDefinition != null) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                    ) {

                        item {
                            Row {
                                SetPointControlCompose(
                                    viewModel.configModel.value.controlName(
                                        viewModel.profileModelDefinition,
                                        satSetpointControlEnable
                                    ), state = viewModel.configModel.value.setPointControl
                                ) {
                                    viewModel.configModel.value.setPointControl = it
                                    if (!viewModel.configModel.value.setPointControl) viewModel.configModel.value.dualSetPointControl =
                                        false
                                }
                                if (viewModel.configModel.value.setPointControl) {
                                    SetPointControlCompose(
                                        viewModel.configModel.value.controlName(
                                            viewModel.profileModelDefinition,
                                            dualSetpointControlEnable
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
                                                val satMin =
                                                    viewModel.configModel.value.getPointByDomainName(
                                                        viewModel.profileModelDefinition,
                                                        systemSATMinimum
                                                    )
                                                val satMax =
                                                    viewModel.configModel.value.getPointByDomainName(
                                                        viewModel.profileModelDefinition,
                                                        systemSATMaximum
                                                    )
                                                if (satMin != null) {
                                                    val items = viewModel.itemsFromMinMax(
                                                        (satMin.valueConstraint as NumericConstraint).minValue,
                                                        (satMin.valueConstraint as NumericConstraint).maxValue,
                                                        (satMin.presentationData?.get(
                                                            tagValueIncrement
                                                        ) as Int).toDouble()
                                                    )
                                                    SetPointConfig(
                                                        satMin.name,
                                                        viewModel.configModel.value.satMin,
                                                        items, satMin.defaultUnit ?: EMPTY,
                                                    ) { selected ->
                                                        viewModel.configModel.value.satMin =
                                                            selected
                                                    }
                                                }
                                                if (satMax != null) {
                                                    val items = viewModel.itemsFromMinMax(
                                                        (satMax.valueConstraint as NumericConstraint).minValue,
                                                        (satMax.valueConstraint as NumericConstraint).maxValue,
                                                        (satMax.presentationData?.get(
                                                            tagValueIncrement
                                                        ) as Int).toDouble()
                                                    )
                                                    SetPointConfig(
                                                        satMax.name,
                                                        viewModel.configModel.value.satMax,
                                                        items, satMax.defaultUnit ?: EMPTY,
                                                    ) { selected ->
                                                        viewModel.configModel.value.satMax =
                                                            selected
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Row {
                                if (viewModel.configModel.value.dualSetPointControl) {
                                    Row {
                                        Column {
                                            val satHeatingMin =
                                                viewModel.configModel.value.getPointByDomainName(
                                                    viewModel.profileModelDefinition,
                                                    systemHeatingSATMinimum
                                                )
                                            val satHeatingMax =
                                                viewModel.configModel.value.getPointByDomainName(
                                                    viewModel.profileModelDefinition,
                                                    systemHeatingSATMaximum
                                                )
                                            val satCoolingMin =
                                                viewModel.configModel.value.getPointByDomainName(
                                                    viewModel.profileModelDefinition,
                                                    systemCoolingSATMinimum
                                                )
                                            val satCoolingMax =
                                                viewModel.configModel.value.getPointByDomainName(
                                                    viewModel.profileModelDefinition,
                                                    systemCoolingSATMaximum
                                                )

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight()
                                            ) {

                                                if (satHeatingMin != null) {
                                                    val items = viewModel.itemsFromMinMax(
                                                        (satHeatingMin.valueConstraint as NumericConstraint).minValue,
                                                        (satHeatingMin.valueConstraint as NumericConstraint).maxValue,
                                                        (satHeatingMin.presentationData?.get(
                                                            tagValueIncrement
                                                        ) as Int).toDouble()
                                                    )
                                                    SetPointConfig(
                                                        satHeatingMin.name,
                                                        viewModel.configModel.value.heatingMinSp,
                                                        items, satHeatingMin.defaultUnit ?: EMPTY,
                                                    ) { selected ->
                                                        viewModel.configModel.value.heatingMinSp =
                                                            selected
                                                    }
                                                }
                                                if (satHeatingMax != null) {
                                                    val items = viewModel.itemsFromMinMax(
                                                        (satHeatingMax.valueConstraint as NumericConstraint).minValue,
                                                        (satHeatingMax.valueConstraint as NumericConstraint).maxValue,
                                                        (satHeatingMax.presentationData?.get(
                                                            tagValueIncrement
                                                        ) as Int).toDouble()
                                                    )
                                                    SetPointConfig(
                                                        satHeatingMax.name,
                                                        viewModel.configModel.value.heatingMaxSp,
                                                        items, satHeatingMax.defaultUnit ?: EMPTY,
                                                    ) { selected ->
                                                        viewModel.configModel.value.heatingMaxSp =
                                                            selected
                                                    }
                                                }
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight()
                                            ) {
                                                if (satCoolingMin != null) {
                                                    val items = viewModel.itemsFromMinMax(
                                                        (satCoolingMin.valueConstraint as NumericConstraint).minValue,
                                                        (satCoolingMin.valueConstraint as NumericConstraint).maxValue,
                                                        (satCoolingMin.presentationData?.get(
                                                            tagValueIncrement
                                                        ) as Int).toDouble()
                                                    )
                                                    SetPointConfig(
                                                        satCoolingMin.name,
                                                        viewModel.configModel.value.coolingMinSp,
                                                        items, satCoolingMin.defaultUnit ?: EMPTY,
                                                    ) { selected ->
                                                        viewModel.configModel.value.coolingMinSp =
                                                            selected
                                                    }
                                                }
                                                if (satCoolingMax != null) {
                                                    val items = viewModel.itemsFromMinMax(
                                                        (satCoolingMax.valueConstraint as NumericConstraint).minValue,
                                                        (satCoolingMax.valueConstraint as NumericConstraint).maxValue,
                                                        (satCoolingMax.presentationData?.get(
                                                            tagValueIncrement
                                                        ) as Int).toDouble()
                                                    )
                                                    SetPointConfig(
                                                        satCoolingMax.name,
                                                        viewModel.configModel.value.coolingMaxSp,
                                                        items, satCoolingMax.defaultUnit ?: EMPTY,
                                                    ) { selected ->
                                                        viewModel.configModel.value.coolingMaxSp =
                                                            selected
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            SetPointControlCompose(
                                viewModel.configModel.value.controlName(
                                    viewModel.profileModelDefinition,
                                    staticPressureSetpointControlEnable
                                ),
                                state = viewModel.configModel.value.fanStaticSetPointControl
                            ) {
                                viewModel.configModel.value.fanStaticSetPointControl = it
                            }
                        }
                        item {
                            if (viewModel.configModel.value.fanStaticSetPointControl) {
                                Row {

                                    val fanSpMin = viewModel.configModel.value.getPointByDomainName(
                                        viewModel.profileModelDefinition,
                                        systemStaticPressureMinimum
                                    )
                                    val fanSpMax = viewModel.configModel.value.getPointByDomainName(
                                        viewModel.profileModelDefinition,
                                        systemStaticPressureMaximum
                                    )

                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                        ) {
                                            if (fanSpMin != null) {
                                                val items = viewModel.itemsFromMinMax(
                                                    (fanSpMin.valueConstraint as NumericConstraint).minValue,
                                                    (fanSpMin.valueConstraint as NumericConstraint).maxValue,
                                                    (fanSpMin.presentationData?.get(
                                                        tagValueIncrement
                                                    )
                                                        .toString().toDouble())
                                                )
                                                SetPointConfig(
                                                    fanSpMin.name,
                                                    viewModel.configModel.value.fanMinSp,
                                                    items, fanSpMin.defaultUnit ?: EMPTY,
                                                ) { selected ->
                                                    viewModel.configModel.value.fanMinSp = selected
                                                }
                                            }
                                            if (fanSpMax != null) {
                                                val items = viewModel.itemsFromMinMax(
                                                    (fanSpMax.valueConstraint as NumericConstraint).minValue,
                                                    (fanSpMax.valueConstraint as NumericConstraint).maxValue,
                                                    (fanSpMax.presentationData?.get(
                                                        tagValueIncrement
                                                    )
                                                        .toString().toDouble())
                                                )
                                                SetPointConfig(
                                                    fanSpMax.name,
                                                    viewModel.configModel.value.fanMaxSp,
                                                    items, fanSpMax.defaultUnit ?: EMPTY,
                                                ) { selected ->
                                                    viewModel.configModel.value.fanMaxSp = selected
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            SetPointControlCompose(
                                viewModel.configModel.value.controlName(
                                    viewModel.profileModelDefinition,
                                    dcvDamperControlEnable
                                ), state = viewModel.configModel.value.dcvControl
                            ) {
                                viewModel.configModel.value.dcvControl = it
                            }
                        }
                        item {
                            if (viewModel.configModel.value.dcvControl) {

                                val dcvMin = viewModel.configModel.value.getPointByDomainName(
                                    viewModel.profileModelDefinition,
                                    systemDCVDamperPosMinimum
                                )
                                val dcvMax = viewModel.configModel.value.getPointByDomainName(
                                    viewModel.profileModelDefinition,
                                    systemDCVDamperPosMaximum
                                )

                                Row {
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .wrapContentHeight()
                                        ) {
                                            if (dcvMin != null) {
                                                val items = viewModel.itemsFromMinMax(
                                                    (dcvMin.valueConstraint as NumericConstraint).minValue,
                                                    (dcvMin.valueConstraint as NumericConstraint).maxValue,
                                                    (dcvMin.presentationData?.get(tagValueIncrement) as Int).toDouble()
                                                )
                                                SetPointConfig(
                                                    dcvMin.name,
                                                    viewModel.configModel.value.dcvMin,
                                                    items, dcvMin.defaultUnit ?: EMPTY,
                                                ) { selected ->
                                                    viewModel.configModel.value.dcvMin = selected
                                                }
                                            }
                                            if (dcvMax != null) {
                                                val items = viewModel.itemsFromMinMax(
                                                    (dcvMax.valueConstraint as NumericConstraint).minValue,
                                                    (dcvMax.valueConstraint as NumericConstraint).maxValue,
                                                    (dcvMax.presentationData?.get(tagValueIncrement) as Int).toDouble()
                                                )
                                                SetPointConfig(
                                                    dcvMax.name,
                                                    viewModel.configModel.value.dcvMax,
                                                    items, dcvMax.defaultUnit ?: EMPTY,
                                                ) { selected ->
                                                    viewModel.configModel.value.dcvMax = selected
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            SetPointControlCompose(
                                viewModel.configModel.value.controlName(
                                    viewModel.profileModelDefinition,
                                    occupancyModeControl
                                ),
                                state = viewModel.configModel.value.occupancyMode
                            ) {
                                viewModel.configModel.value.occupancyMode = it
                            }
                        }
                        item {
                            SetPointControlCompose(
                                viewModel.configModel.value.controlName(
                                    viewModel.profileModelDefinition,
                                    humidifierOperationEnable
                                ),
                                state = viewModel.configModel.value.humidifierControl
                            ) {
                                viewModel.configModel.value.humidifierControl = it
                            }
                        }
                        item {
                            SetPointControlCompose(
                                viewModel.configModel.value.controlName(
                                    viewModel.profileModelDefinition,
                                    dehumidifierOperationEnable
                                ),
                                state = viewModel.configModel.value.dehumidifierControl
                            ) {
                                viewModel.configModel.value.dehumidifierControl = it
                            }
                        }

                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(PaddingValues(top = 10.dp))
                                    .wrapContentHeight(),
                                contentAlignment = Alignment.Center
                            ) { HeaderCenterLeftAlignedTextView(text = SELECT_PROTOCOL) }
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
                                                ExternalAhuControlViewModel.ConfigType.BACNET
                                        }

                                        MODBUS -> {
                                            viewModel.configType.value =
                                                ExternalAhuControlViewModel.ConfigType.MODBUS
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            if (viewModel.configType.value == ExternalAhuControlViewModel.ConfigType.MODBUS) {
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
        init()
        return rootView
    }

    private fun init() {
        ProgressDialogUtils.showProgressDialog(requireContext(), LOADING)
        viewModel.configModelDefinition(
            requireContext()
        )
    }
    private fun reload() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.profileContainer, DabExternalAhuControlConfigFragment()).commit()
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
            ) { SaveTextView(CANCEL) { reload() } }
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
            ) { HeaderCenterLeftAlignedTextView(SELECT_MODEL) }
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
                            if (!viewModel.equipModel.value.isDevicePaired) {
                                showDialogFragment(
                                    ModelSelectionFragment.newInstance(
                                        viewModel.deviceList, viewModel.onItemSelect, SEARCH_MODEL
                                    ), ModelSelectionFragment.ID
                                )
                            }
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
                    if (viewModel.equipModel.value.equipDevice.value.name.isNullOrEmpty()) "" else viewModel.equipModel.value.equipDevice.value.name
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
        SubEquipments(viewModel.equipModel)
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

    @Composable
    fun SubEquipments(data: MutableState<EquipModel>) {

        Column {
            data.value.subEquips.forEach { subEquip ->
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Box(modifier = Modifier.weight(7f)) {
                        HeaderLeftAlignedTextView(
                            text = getName(
                                subEquip.value.equipDevice.value.name
                            )
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) { HeaderTextView(SLAVE_ID) }
                    Box(modifier = Modifier.weight(2f)) {
                        if (viewModel.equipModel.value.isDevicePaired) {
                            TextViewCompose(subEquip.value.slaveId.value.toString())
                        } else {
                            val onItemSelect = object : OnItemSelect {
                                override fun onItemSelected(index: Int, item: String) {
                                    if (index == 0) {
                                        subEquip.value.slaveId.value = 0
                                        subEquip.value.childSlaveId.value = SAME_AS_PARENT
                                    } else {
                                        subEquip.value.slaveId.value = item.toInt()
                                        subEquip.value.childSlaveId.value = item
                                    }
                                }
                            }
                            TextViewWithClick(
                                text = subEquip.value.childSlaveId,
                                onClick = {
                                    ProgressDialogUtils.showProgressDialog(context, LOADING)
                                    showDialogFragment(
                                        ModelSelectionFragment.newInstance(
                                            viewModel.childSlaveIdList,
                                            onItemSelect, SEARCH_SLAVE_ID
                                        ), ModelSelectionFragment.ID
                                    )
                                },
                                enableClick = !viewModel.equipModel.value.isDevicePaired,
                                isCompress = false
                            )
                        }
                    }
                }
                Row(modifier = Modifier.padding(start = 10.dp)) { ParameterLabel() }
                ParametersListView(data = subEquip)
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


