package a75f.io.renatus.profiles.system.externalahu

import a75f.io.domain.api.DomainName.dcvDamperControlEnable
import a75f.io.domain.api.DomainName.dehumidifierOperationEnable
import a75f.io.domain.api.DomainName.dualSetpointControlEnable
import a75f.io.domain.api.DomainName.humidifierOperationEnable
import a75f.io.domain.api.DomainName.occupancyModeControl
import a75f.io.domain.api.DomainName.satSetpointControlEnable
import a75f.io.domain.api.DomainName.staticPressureSetpointControlEnable
import a75f.io.domain.api.DomainName.systemCO2DamperOpeningRate
import a75f.io.domain.api.DomainName.systemCO2Target
import a75f.io.domain.api.DomainName.systemCO2Threshold
import a75f.io.domain.api.DomainName.systemCoolingSATMaximum
import a75f.io.domain.api.DomainName.systemCoolingSATMinimum
import a75f.io.domain.api.DomainName.systemDCVDamperPosMaximum
import a75f.io.domain.api.DomainName.systemDCVDamperPosMinimum
import a75f.io.domain.api.DomainName.systemHeatingSATMaximum
import a75f.io.domain.api.DomainName.systemHeatingSATMinimum
import a75f.io.domain.api.DomainName.systemStaticPressureMaximum
import a75f.io.domain.api.DomainName.systemStaticPressureMinimum
import a75f.io.domain.api.DomainName.tagValueIncrement
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.util.PreferenceUtil
import a75f.io.renatus.compose.HeaderCenterLeftAlignedTextView
import a75f.io.renatus.compose.HeaderLeftAlignedTextView
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.LabelTextViewForModbus
import a75f.io.renatus.compose.ParameterLabel_ForOneColumn
import a75f.io.renatus.compose.RadioButtonCompose
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.SetPointConfig
import a75f.io.renatus.compose.SetPointControlCompose
import a75f.io.renatus.compose.TextViewCompose
import a75f.io.renatus.compose.TextViewWithClick
import a75f.io.renatus.compose.TextViewWithClickOption
import a75f.io.renatus.compose.ToggleButton
import a75f.io.renatus.compose.VersionTextView
import a75f.io.renatus.modbus.ModelSelectionFragment
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.modbus.models.RegisterItemForSubEquip
import a75f.io.renatus.modbus.util.CANCEL
import a75f.io.renatus.modbus.util.LOADING
import a75f.io.renatus.modbus.util.MODBUS
import a75f.io.renatus.modbus.util.OnItemSelect
import a75f.io.renatus.modbus.util.SAME_AS_PARENT
import a75f.io.renatus.modbus.util.SAVE
import a75f.io.renatus.modbus.util.SEARCH_MODEL
import a75f.io.renatus.modbus.util.SEARCH_SLAVE_ID
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import io.seventyfivef.domainmodeler.common.point.NumericConstraint


/**
 * Created by Manjunath K on 06-06-2023.
 */

class ExternalAhuFragment(var profileType: ProfileType) : Fragment() {
    private lateinit var viewModel: ExternalAhuViewModel

    fun hasUnsavedChanged(): Boolean{
        return viewModel.hasUnsavedChanges()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext())
        viewModel = ViewModelProvider(this)[ExternalAhuViewModel::class.java]
        rootView.apply {
            setContent {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                ) {

                    item {
                        Row {
                            SetPointControlCompose(
                                viewModel.configModel.value.controlName(
                                    viewModel.profileModelDefinition, satSetpointControlEnable
                                ), state = viewModel.configModel.value.setPointControl
                            ) {
                                viewModel.configModel.value.setPointControl = it
                                if (it && viewModel.configModel.value.dualSetPointControl)
                                    viewModel.configModel.value.dualSetPointControl = false

                                viewModel.configModel.value.heatingMinSp =
                                    viewModel.getDefaultValByDomain(systemHeatingSATMinimum)
                                viewModel.configModel.value.heatingMaxSp =
                                    viewModel.getDefaultValByDomain(systemHeatingSATMaximum)
                                viewModel.configModel.value.coolingMinSp =
                                    viewModel.getDefaultValByDomain(systemCoolingSATMinimum)
                                viewModel.configModel.value.coolingMaxSp =
                                    viewModel.getDefaultValByDomain(systemCoolingSATMaximum)
                                setStateChanged()
                            }
                            Row(modifier = Modifier.padding(start = 60.dp)) {
                                SetPointControlCompose(
                                    viewModel.configModel.value.controlName(
                                        viewModel.profileModelDefinition, dualSetpointControlEnable
                                    ), state = viewModel.configModel.value.dualSetPointControl
                                ) {
                                    if (it && viewModel.configModel.value.setPointControl)
                                        viewModel.configModel.value.setPointControl = false
                                    viewModel.configModel.value.dualSetPointControl = it
                                    setStateChanged()
                                }
                            }
                        }

                        Row {
                            if (viewModel.configModel.value.setPointControl || viewModel.configModel.value.dualSetPointControl) {
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
                                                    setStateChanged()
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
                                                    setStateChanged()
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
                                                    setStateChanged()
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
                                                    setStateChanged()
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
                            ), state = viewModel.configModel.value.fanStaticSetPointControl
                        ) {
                            viewModel.configModel.value.fanStaticSetPointControl = it
                            viewModel.configModel.value.fanMinSp =
                                viewModel.getDefaultValByDomain(systemStaticPressureMinimum)
                            viewModel.configModel.value.fanMaxSp =
                                viewModel.getDefaultValByDomain(systemStaticPressureMaximum)
                            setStateChanged()
                        }
                    }
                    item {
                        if (viewModel.configModel.value.fanStaticSetPointControl) {
                            Row {

                                val fanSpMin = viewModel.configModel.value.getPointByDomainName(
                                    viewModel.profileModelDefinition, systemStaticPressureMinimum
                                )
                                val fanSpMax = viewModel.configModel.value.getPointByDomainName(
                                    viewModel.profileModelDefinition, systemStaticPressureMaximum
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
                                                ).toString().toDouble())
                                            )
                                            SetPointConfig(
                                                fanSpMin.name,
                                                viewModel.configModel.value.fanMinSp,
                                                items, fanSpMin.defaultUnit ?: EMPTY,
                                            ) { selected ->
                                                viewModel.configModel.value.fanMinSp = selected
                                                setStateChanged()
                                            }
                                        }
                                        if (fanSpMax != null) {
                                            val items = viewModel.itemsFromMinMax(
                                                (fanSpMax.valueConstraint as NumericConstraint).minValue,
                                                (fanSpMax.valueConstraint as NumericConstraint).maxValue,
                                                (fanSpMax.presentationData?.get(
                                                    tagValueIncrement
                                                ).toString().toDouble())
                                            )
                                            SetPointConfig(
                                                fanSpMax.name,
                                                viewModel.configModel.value.fanMaxSp,
                                                items, fanSpMax.defaultUnit ?: EMPTY,
                                            ) { selected ->
                                                viewModel.configModel.value.fanMaxSp = selected
                                                setStateChanged()
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
                                viewModel.profileModelDefinition, dcvDamperControlEnable
                            ), state = viewModel.configModel.value.dcvControl
                        ) {
                            viewModel.configModel.value.dcvControl = it
                            viewModel.configModel.value.dcvMin =
                                viewModel.getDefaultValByDomain(systemDCVDamperPosMinimum)
                            viewModel.configModel.value.dcvMax =
                                viewModel.getDefaultValByDomain(systemDCVDamperPosMaximum)
                            viewModel.configModel.value.co2Threshold =
                                viewModel.getDefaultValByDomain(systemCO2Threshold)
                            viewModel.configModel.value.co2Target =
                                viewModel.getDefaultValByDomain(systemCO2Target)
                            viewModel.configModel.value.damperOpeningRate =
                                viewModel.getDefaultValByDomain(systemCO2DamperOpeningRate)
                            setStateChanged()
                        }
                    }
                    item {
                        if (viewModel.configModel.value.dcvControl) {

                            val dcvMin = viewModel.configModel.value.getPointByDomainName(
                                viewModel.profileModelDefinition, systemDCVDamperPosMinimum
                            )
                            val dcvMax = viewModel.configModel.value.getPointByDomainName(
                                viewModel.profileModelDefinition, systemDCVDamperPosMaximum
                            )
                            val co2Threshold = viewModel.configModel.value.getPointByDomainName(
                                viewModel.profileModelDefinition, systemCO2Threshold
                            )
                            val damperOpeningRate =
                                viewModel.configModel.value.getPointByDomainName(
                                    viewModel.profileModelDefinition, systemCO2DamperOpeningRate
                                )
                            val co2Target = viewModel.configModel.value.getPointByDomainName(
                                viewModel.profileModelDefinition, systemCO2Target
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
                                                setStateChanged()
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
                                                setStateChanged()
                                            }
                                        }
                                    }
                                }
                            }
                            Row {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                    ) {
                                        if (co2Threshold != null) {
                                            val items = viewModel.itemsFromMinMax(
                                                (co2Threshold.valueConstraint as NumericConstraint).minValue,
                                                (co2Threshold.valueConstraint as NumericConstraint).maxValue,
                                                (co2Threshold.presentationData?.get(
                                                    tagValueIncrement
                                                ) as Int).toDouble()
                                            )
                                            SetPointConfig(
                                                co2Threshold.name,
                                                viewModel.configModel.value.co2Threshold,
                                                items, co2Threshold.defaultUnit ?: EMPTY,
                                            ) { selected ->
                                                viewModel.configModel.value.co2Threshold = selected
                                                setStateChanged()
                                            }
                                        }
                                        if (co2Target != null) {
                                            val items = viewModel.itemsFromMinMax(
                                                (co2Target.valueConstraint as NumericConstraint).minValue,
                                                (co2Target.valueConstraint as NumericConstraint).maxValue,
                                                (co2Target.presentationData?.get(tagValueIncrement) as Int).toDouble()
                                            )
                                            SetPointConfig(
                                                co2Target.name,
                                                viewModel.configModel.value.co2Target,
                                                items, co2Target.defaultUnit ?: EMPTY,
                                            ) { selected ->
                                                viewModel.configModel.value.co2Target = selected
                                                setStateChanged()
                                            }
                                        }
                                    }
                                }
                            }

                            Row {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .wrapContentHeight()
                                    ) {

                                        if (damperOpeningRate != null) {
                                            val items = viewModel.itemsFromMinMax(
                                                (damperOpeningRate.valueConstraint as NumericConstraint).minValue,
                                                (damperOpeningRate.valueConstraint as NumericConstraint).maxValue,
                                                (damperOpeningRate.presentationData?.get(
                                                    tagValueIncrement
                                                ) as Int).toDouble()
                                            )
                                            SetPointConfig(
                                                damperOpeningRate.name,
                                                viewModel.configModel.value.damperOpeningRate,
                                                items, damperOpeningRate.defaultUnit ?: EMPTY,
                                            ) { selected ->
                                                viewModel.configModel.value.damperOpeningRate =
                                                    selected
                                                setStateChanged()
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
                                viewModel.profileModelDefinition, occupancyModeControl
                            ), state = viewModel.configModel.value.occupancyMode
                        ) {
                            viewModel.configModel.value.occupancyMode = it
                            setStateChanged()
                        }
                    }
                    item {
                        SetPointControlCompose(
                            viewModel.configModel.value.controlName(
                                viewModel.profileModelDefinition, humidifierOperationEnable
                            ), state = viewModel.configModel.value.humidifierControl
                        ) {
                            viewModel.configModel.value.humidifierControl = it
                            setStateChanged()
                        }
                    }
                    item {
                        SetPointControlCompose(
                            viewModel.configModel.value.controlName(
                                viewModel.profileModelDefinition, dehumidifierOperationEnable
                            ), state = viewModel.configModel.value.dehumidifierControl
                        ) {
                            viewModel.configModel.value.dehumidifierControl = it
                            setStateChanged()
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(PaddingValues(top = 10.dp, end = 30.dp))
                                .wrapContentHeight(), contentAlignment = Alignment.Center
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
                                radioOptions, viewModel.configType.value.ordinal
                            ) {
                                when (it) {
                                    BACNET -> {
                                        viewModel.configType.value =
                                            ExternalAhuViewModel.ConfigType.BACNET
                                        setStateChanged()
                                    }

                                    MODBUS -> {
                                        viewModel.configType.value =
                                            ExternalAhuViewModel.ConfigType.MODBUS
                                        setStateChanged()
                                    }
                                }
                            }
                        }
                    }
                    item {
                        if (viewModel.configType.value == ExternalAhuViewModel.ConfigType.MODBUS) {
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
        init()
        return rootView
    }

    private fun init() {
        isNewProfile()
        ProgressDialogUtils.showProgressDialog(requireContext(), LOADING)
        viewModel.configModelDefinition(
            requireContext(), profileType
        )
        if (PreferenceUtil.getIsNewExternalAhu()) resetScreen()
    }

    private fun isNewProfile() {
        val systemProfile = L.ccu().systemProfile.profileType
        val selectedProfileType = profileType
        if(((systemProfile == ProfileType.vavExternalAHUController) && (selectedProfileType == ProfileType.vavExternalAHUController))
            || (systemProfile == ProfileType.dabExternalAHUController) && (selectedProfileType == ProfileType.dabExternalAHUController)){
            PreferenceUtil.setIsNewExternalAhu(false)
        }
        else PreferenceUtil.setIsNewExternalAhu(true)
    }

    private fun reload() {
         isNewProfile()
         if (PreferenceUtil.getIsNewExternalAhu()) {
            viewModel.apply {
                equipModel = mutableStateOf(EquipModel())
                selectedModbusType = mutableStateOf(0)
                modelName = mutableStateOf("Select Model")
            }
         }
         init()
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
                    .padding(PaddingValues(bottom = 10.dp, end = 5.dp)),
                contentAlignment = Alignment.Center
            ) { SaveTextView(CANCEL, viewModel.configModel.value.isStateChanged) { reload() } }
            Divider(
                modifier = Modifier
                    .height(25.dp)
                    .width(2.dp)
                    .padding(bottom = 6.dp),
                color = Color.LightGray
            )
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(PaddingValues(bottom = 10.dp, end = 10.dp)),
                contentAlignment = Alignment.Center
            ) {
                SaveTextView(SAVE, viewModel.configModel.value.isStateChanged) { viewModel.saveConfiguration() }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(top = 10.dp, end = 25.dp)),
                contentAlignment = Alignment.Center
            ) { HeaderCenterLeftAlignedTextView(SELECT_MODEL) }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 430.dp),
            contentAlignment = Alignment.CenterStart
        ) {
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
                    VersionTextView(" V " + viewModel.equipModel.value.equipDevice.value.modbusEquipIdId)
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
                    if (viewModel.equipModel.value.version.value.isNotEmpty()) {
                        VersionTextView(" V ${viewModel.equipModel.value.version.value}")
                    }
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
            Box(modifier = Modifier.weight(1f)) { HeaderTextView(SLAVE_ID, fontSize = 20) }
            Box(modifier = Modifier
                .weight(1f)
                .wrapContentHeight()) {
                val onItemSelect = object : OnItemSelect {
                    override fun onItemSelected(index: Int, item: String) {
                        viewModel.equipModel.value.slaveId.value = item.toInt()
                    }
                }
                TextViewWithClickOption(
                    text = viewModel.equipModel.value.slaveId,
                    onClick = {
                        if (!viewModel.equipModel.value.isDevicePaired) {
                            ProgressDialogUtils.showProgressDialog(context, LOADING)
                            showDialogFragment(
                                ModelSelectionFragment.newInstance(
                                    viewModel.slaveIdList, onItemSelect, SEARCH_SLAVE_ID
                                ), ModelSelectionFragment.ID
                            )
                        }
                    },
                    enableClick = !viewModel.equipModel.value.isDevicePaired,
                )
            }
        }

        Row(modifier = Modifier.padding(start = 10.dp)) {
            ParameterLabel_ForOneColumn()
            ToggleButton(defaultSelection = viewModel.equipModel.value.selectAllParameters_Left.value) {
                viewModel.equipModel.value.selectAllParameters_Left.value = it
                viewModel.onSelectAllLeft(it)
                setStateChanged()
            }
            Spacer(modifier = Modifier.width(30.dp))
            ParameterLabel_ForOneColumn()
            ToggleButton(defaultSelection = viewModel.equipModel.value.selectAllParameters_Right.value) {
                viewModel.equipModel.value.selectAllParameters_Right.value = it
                viewModel.onSelectAllRight(it)
                setStateChanged()
            }
        }

        // This index -1 refers to parameters equip and other index 0 to n refers index of sub equips
        ParametersListView(data = viewModel.equipModel, indexForSelectAllRelay = -1)
        SubEquipments(viewModel.equipModel)
    }


    @Composable
    fun ParametersListView(data: MutableState<EquipModel>, indexForSelectAllRelay: Int) {
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
                                LabelTextViewForModbus(
                                    if (item.param.value.name.length > 30) item.param.value.name.substring(
                                        0, 30
                                    )
                                    else item.param.value.name
                                )
                                Box(modifier = Modifier.padding(end = 55.dp)) {
                                    ToggleButton(item.displayInUi.value) {
                                        item.displayInUi.value = it
                                        item.param.value.getParameterId()
                                        if (indexForSelectAllRelay == -1) //This is for finding main parameters
                                            viewModel.updateSelectAllBoth()
                                        else {
                                            viewModel.updateSelectAllSubEquipLeft(
                                                indexForSelectAllRelay
                                            )
                                            viewModel.updateSelectAllSubEquipRight(
                                                indexForSelectAllRelay
                                            )
                                        }
                                        setStateChanged()
                                    }
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
            data.value.subEquips.forEachIndexed { index, subEquip ->
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
                                            onItemSelect,
                                            SEARCH_SLAVE_ID
                                        ), ModelSelectionFragment.ID
                                    )
                                },
                                enableClick = !viewModel.equipModel.value.isDevicePaired,
                                isCompress = false
                            )
                        }
                    }
                }
                Row(modifier = Modifier.padding(start = 10.dp)) {
                    ParameterLabel_ForOneColumn()
                    if (data.value.selectAllParameters_Left_subEquip.size <= index) {
                        data.value.selectAllParameters_Left_subEquip.add(RegisterItemForSubEquip())
                    }
                    ToggleButton(defaultSelection = data.value.selectAllParameters_Left_subEquip[index].displayInUi.value) {
                        data.value.selectAllParameters_Left_subEquip[index].displayInUi.value = it
                        viewModel.onSelectAllLeftSubEquip(it, subEquip)
                    }
                    Spacer(modifier = Modifier.width(30.dp))
                    ParameterLabel_ForOneColumn()
                    if (data.value.selectAllParameters_Right_subEquip.size <= index) {
                        data.value.selectAllParameters_Right_subEquip.add(RegisterItemForSubEquip())
                    }
                    ToggleButton(defaultSelection = data.value.selectAllParameters_Right_subEquip[index].displayInUi.value) {
                        data.value.selectAllParameters_Right_subEquip[index].displayInUi.value = it
                        viewModel.onSelectAllRightSubEquip(it, subEquip)
                    }
                }
                ParametersListView(data = subEquip, indexForSelectAllRelay = index)
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
    private fun resetScreen() {
        viewModel.configModel.value.setPointControl = false
        viewModel.configModel.value.dualSetPointControl = false
        viewModel.configModel.value.heatingMinSp = viewModel.getDefaultValByDomain(systemHeatingSATMinimum)
        viewModel.configModel.value.heatingMaxSp = viewModel.getDefaultValByDomain(systemHeatingSATMaximum)
        viewModel.configModel.value.coolingMinSp = viewModel.getDefaultValByDomain(systemCoolingSATMinimum)
        viewModel.configModel.value.coolingMaxSp = viewModel.getDefaultValByDomain(systemCoolingSATMaximum)

        viewModel.configModel.value.fanStaticSetPointControl = false
        viewModel.configModel.value.fanMinSp = viewModel.getDefaultValByDomain(systemStaticPressureMinimum)
        viewModel.configModel.value.fanMaxSp = viewModel.getDefaultValByDomain(systemStaticPressureMaximum)

        viewModel.configModel.value.dcvControl = false
        viewModel.configModel.value.dcvMin = viewModel.getDefaultValByDomain(systemDCVDamperPosMinimum)
        viewModel.configModel.value.dcvMax = viewModel.getDefaultValByDomain(systemDCVDamperPosMaximum)
        viewModel.configModel.value.co2Threshold = viewModel.getDefaultValByDomain(systemCO2Threshold)
        viewModel.configModel.value.co2Target = viewModel.getDefaultValByDomain(systemCO2Target)
        viewModel.configModel.value.damperOpeningRate = viewModel.getDefaultValByDomain(systemCO2DamperOpeningRate)

        viewModel.configModel.value.occupancyMode = false
        viewModel.configModel.value.humidifierControl = false
        viewModel.configModel.value.dehumidifierControl = false

        viewModel.configType.value = ExternalAhuViewModel.ConfigType.BACNET
    }
    private fun setStateChanged() {
        viewModel.configModel.value.isStateChanged = true
    }

}


