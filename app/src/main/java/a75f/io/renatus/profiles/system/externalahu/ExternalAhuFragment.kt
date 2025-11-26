package a75f.io.renatus.profiles.system.externalahu

import a75f.io.api.haystack.bacnet.parser.BacnetProperty
import a75f.io.api.haystack.bacnet.parser.BacnetSelectedValue
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
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.util.PreferenceUtil
import a75f.io.logic.util.bacnet.BacnetConfigConstants
import a75f.io.logic.util.bacnet.TAG_BACNET
import a75f.io.renatus.R
import a75f.io.renatus.bacnet.BacnetDeviceSelectionFragment
import a75f.io.renatus.bacnet.models.BacnetModel
import a75f.io.renatus.bacnet.models.BacnetPointState
import a75f.io.renatus.bacnet.util.*
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.compose.*
import a75f.io.renatus.modbus.ModelSelectionFragment
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.modbus.models.RegisterItemForSubEquip
import a75f.io.renatus.modbus.util.*
import a75f.io.renatus.modbus.util.LOADING
import a75f.io.renatus.modbus.util.MAC_ADDRESS
import a75f.io.renatus.modbus.util.MODBUS
import a75f.io.renatus.modbus.util.SAME_AS_PARENT
import a75f.io.renatus.modbus.util.SEARCH_MODEL
import a75f.io.renatus.modbus.util.SEARCH_SLAVE_ID
import a75f.io.renatus.modbus.util.SLAVE_ID
import a75f.io.renatus.util.ProgressDialogUtils
import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import io.seventyfivef.domainmodeler.common.point.NumericConstraint
import org.json.JSONException
import org.json.JSONObject
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp


/**
 * Created by Manjunath K on 06-06-2023.
 */

class ExternalAhuFragment(var profileType: ProfileType) : Fragment() {
    private lateinit var viewModel: ExternalAhuViewModel
    //private lateinit var bacnetConfigViewmodel : BacNetConfigViewModel
    private var isBacNetInitialized = false
    private var isBacnetMstpInitialized = false
    companion object {
        private const val RESET_VALUE = "0"
    }

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
                            if (it) {
                                viewModel.configModel.value.fanMinSp =
                                    viewModel.getDefaultValByDomain(systemStaticPressureMinimum)
                                viewModel.configModel.value.fanMaxSp =
                                    viewModel.getDefaultValByDomain(systemStaticPressureMaximum)
                            } else {
                                viewModel.configModel.value.fanMinSp = RESET_VALUE
                                viewModel.configModel.value.fanMaxSp = RESET_VALUE
                            }
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
                            if (it) {
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
                            } else {
                                viewModel.configModel.value.dcvMin = RESET_VALUE
                                viewModel.configModel.value.dcvMax = RESET_VALUE
                                viewModel.configModel.value.co2Threshold = RESET_VALUE
                                viewModel.configModel.value.co2Target = RESET_VALUE
                                viewModel.configModel.value.damperOpeningRate = RESET_VALUE
                            }
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
                            var disabledOptions: List<String>
                            if(!viewModel.isBacnetIpEnabled.value && !viewModel.isBacnetMstpEnabled.value){
                                disabledOptions =  listOf(BACNET)
                            }else{
                                disabledOptions = listOf()
                            }
                            RadioButtonComposeWithToast(
                                requireContext(),
                                radioOptions, viewModel.configTypeRadioOption.value.ordinal,
                                disabledOptions = disabledOptions
                            ) {
                                when (it) {
                                    BACNET -> {
                                        viewModel.configType.value =
                                            ExternalAhuViewModel.ConfigType.BACNET
                                        viewModel.configTypeRadioOption.value = ExternalAhuViewModel.ConfigType.BACNET
                                        setStateChanged()
                                        resetModelName()
                                    }

                                    MODBUS -> {
                                        viewModel.configType.value =
                                            ExternalAhuViewModel.ConfigType.MODBUS
                                        viewModel.configTypeRadioOption.value = ExternalAhuViewModel.ConfigType.MODBUS
                                        setStateChanged()
                                        resetModelName()
                                        viewModel.resetBacnetView()
                                    }
                                }
                            }
                        }
                    }
                    item {
                        if (viewModel.configTypeRadioOption.value == ExternalAhuViewModel.ConfigType.MODBUS) {
                            ModbusConfig()
                        } else {
                            BacnetConfig()
                        }
                    }
                    items(
                        viewModel.bacnetModel.value.points.filter { !it.equipTagNames.contains("heartbeat") }
                    ) { item ->
                        if (viewModel.configTypeRadioOption.value == ExternalAhuViewModel.ConfigType.BACNET) {
                            ParametersListItem(item)
                        }
                    }
                    item {
                        if (viewModel.configTypeRadioOption.value == ExternalAhuViewModel.ConfigType.BACNET) {
                            BacnetButtons()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.deviceIp = getDataFromSf(requireContext(), BacnetConfigConstants.IP_ADDRESS)
        CcuLog.d("TAG", "device ip--->${viewModel.deviceIp}")
        viewModel.devicePort = getDataFromSf(requireContext(), BacnetConfigConstants.PORT)
        isBacNetInitialized = isBacNetInitialized(requireContext())
    }

    private fun getDataFromSf(context: Context, key: String) :String {
        var ipAddress = ""
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val confString: String? = sharedPreferences.getString(BacnetConfigConstants.BACNET_CONFIGURATION, null)
        if (confString != null) {
            try {
                val config = JSONObject(confString)
                val networkObject = config.getJSONObject("network")
                ipAddress = networkObject.getString(BacnetConfigConstants.IP_ADDRESS)
                //port = networkObject.getInt(BacnetConfigConstants.PORT)
                //service = ServiceManager.CcuServiceFactory.makeCcuService(ipAddress)
                //val deviceObject = config.getJSONObject("device")
                //deviceId = deviceObject.getString(BacnetConfigConstants.IP_DEVICE_INSTANCE_NUMBER)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        return ipAddress
    }

    private fun isBacNetInitialized(context: Context): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getBoolean(BacnetConfigConstants.IS_BACNET_INITIALIZED, false)
    }
    private fun init() {
        isNewProfile()
        //ProgressDialogUtils.showProgressDialog(requireContext(), LOADING)
        viewModel.getExternalProfileSelected()
        viewModel.configModelDefinition(
            requireContext(), profileType
        )
        if (PreferenceUtil.getIsNewExternalAhu()) resetScreen()

        viewModel.isBacNetEnabled("init")
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
        viewModel.configBacnetDetails()

        if(viewModel.bacnetModel.value.isDevicePaired){
            //resetScreen()
        }

        if(viewModel.isErrorMsg.value){
            Toast.makeText(requireContext(), viewModel.errorMsg, Toast.LENGTH_SHORT).show()
            viewModel.isErrorMsg.value = false
        }

        if(viewModel.isConnectedDevicesSearchFinished.value){
            viewModel.isConnectedDevicesSearchFinished.value = false
            if(viewModel.connectedDevices.value.isEmpty()){
                ProgressDialogUtils.hideProgressDialog()
                //Toast.makeText(requireContext(), "No devices found", Toast.LENGTH_SHORT).show()
            }else{
                showDialogFragment(
                    BacnetDeviceSelectionFragment.newInstance(
                        viewModel.connectedDevices,
                        viewModel.onBacnetDeviceSelect, SEARCH_DEVICE,
                        viewModel.configurationType.value == MSTP_CONFIGURATION
                    ), BacnetDeviceSelectionFragment.ID
                )
                ProgressDialogUtils.hideProgressDialog()
            }
        }

        BacnetHeader()
        BacnetModelSelection()
        val expanded = remember { mutableStateOf(false) }
        val onConfigDropdownClickEvent = if (!viewModel.bacnetModel.value.isDevicePaired) { { expanded.value = true } } else { {} }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .wrapContentWidth()         // ✅ keep natural width
                    .widthIn(max = 400.dp),     // ✅ optional cap
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ExternalConfigDropdownSelector(
                    titleText = CONFIGURATION_TYPE,
                    isPaired = viewModel.bacnetModel.value.isDevicePaired,
                    selectedItemName = viewModel.configurationType,
                    modelVersion = "",
                    onClickEvent = onConfigDropdownClickEvent,
                    otherUiComposable = {
                        if (!viewModel.bacnetModel.value.isDevicePaired) {
                            Box(
                                modifier = Modifier.wrapContentWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                ShowDropdownList(expanded) // stays centered
                            }
                        }
                    },
                    isNested = true
                )
            }
        }
        when (viewModel.configurationType.value) {
            IP_CONFIGURATION -> AddressSelector()
            MSTP_CONFIGURATION-> {
                if (!viewModel.bacnetModel.value.isDevicePaired) {
                    DeviceSelector()
                } else {
                    ConfigurationDetailsReadOnly(viewModel.configurationType.value)
                }
            }
            else -> {}
        }

        BacnetModelName()
        Row(modifier = Modifier.padding(start = 10.dp)) {
            ParameterLabel()
        }
    }

    @Composable
    fun AddressSelector(){
        if(viewModel.bacnetModel.value.isDevicePaired){
            BacnetDeviceDetailsReadOnly()
            BacnetPortDetailsReadOnly()
            BacnetDeviceNetworkDetailsReadOnly()
        }else{
            BacnetDeviceSelectionModes()
            if(viewModel.deviceSelectionMode.value == 0){
                BacnetDeviceDetails()
                BacnetPortDetails()
                BacnetDeviceNetworkDetails()
            }else{
                // use below methods once device is connected
                BacnetDeviceDetailsReadOnly()
                BacnetPortDetailsReadOnly()
                BacnetDeviceNetworkDetailsReadOnly()
            }
        }
    }
    @Composable
    fun ConfigurationDetailsReadOnly(configType: String) {

        val configTableData : List<Pair<Pair<String, String>?, Pair<String, String>?>> =
            when(configType) {
                IP_CONFIGURATION -> listOf(
                    Pair(
                        Pair(DEVICE_ID, viewModel.deviceId.value),
                        Pair(DESTINATION_IP, viewModel.destinationIp.value),
                    ),
                    Pair(
                        Pair(DESTINATION_PORT, viewModel.destinationPort.value),
                        Pair(MAC_ADDRESS, viewModel.destinationMacAddress.value)
                    ),
                    Pair(
                        Pair(DEVICE_NETWORK, viewModel.dnet.value),
                        null
                    )
                )
                MSTP_CONFIGURATION -> listOf(
                    Pair(
                        Pair(MAC_ADDRESS, viewModel.destinationMacAddress.value),
                        null
                    )
                )

                else -> emptyList()
            }


        ReadOnlyConfigFields(configTableData)
    }

    @Composable
    private fun ReadOnlyConfigFields(
        configTableData: List<Pair<Pair<String, String>?, Pair<String, String>?>>
    ) {
        Column(modifier = Modifier
            .padding(top = 20.dp)
            .wrapContentHeight()
            .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            configTableData.forEach { rowPair ->
                val subRowPair1 = rowPair.first
                val subRowPair2 = rowPair.second

                Row(horizontalArrangement = Arrangement.spacedBy(50.dp)) {
                    Row(modifier = Modifier.weight(0.5f)) {
                        subRowPair1?.let {
                            Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.CenterStart) {
                                LabelBoldTextViewForTable(subRowPair1.first, fontSize = 22, fontColor = Color.Black)
                            }
                            Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.CenterStart) {
                                LabelTextView(subRowPair1.second, fontSize = 22)
                            }
                        }
                    }
                    Row(modifier = Modifier.weight(0.5f)) {
                        subRowPair2?.let {
                            Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.CenterStart) {
                                LabelBoldTextViewForTable(subRowPair2.first, fontSize = 22, fontColor = Color.Black)
                            }
                            Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.CenterStart) {
                                LabelTextView(subRowPair2.second, fontSize = 22)
                            }
                        }
                    }
                }
            }
        }
    }


    @Composable
    fun DeviceInfo(){
        val configType = MSTP_CONFIGURATION
        val defaultTextList =
                when(configType) {
                    MSTP_CONFIGURATION -> listOf(MAC_ADDRESS)
                    else -> emptyList()
                }

        val configEntryWithHint =
                when(configType) {
                    IP_CONFIGURATION -> listOf(DEVICE_ID, DESTINATION_PORT, DEVICE_NETWORK)
                    MSTP_CONFIGURATION -> listOf(MAC_ADDRESS)
                    else -> emptyList()
                }

        val numberTypeConfigEntries =
                when(configType) {
                    IP_CONFIGURATION -> listOf(DEVICE_ID, DESTINATION_PORT, DEVICE_NETWORK)
                    MSTP_CONFIGURATION -> listOf(MAC_ADDRESS)
                    else -> emptyList()
                }

        val editableEventMap =
                when(configType) {
                    IP_CONFIGURATION -> hashMapOf(
                            DEVICE_ID to { state: String ->
                                viewModel.deviceId.value = state
                            },
                            DESTINATION_PORT to { state: String ->
                                viewModel.destinationPort.value = state
                            },
                            DEVICE_NETWORK to { state: String ->
                                viewModel.dnet.value = state
                            },
                            DESTINATION_IP to { state: String ->
                                viewModel.destinationIp.value = state
                            },
                            MAC_ADDRESS to { state: String ->
                                viewModel.destinationMacAddress.value = state
                            },
                    )
                    MSTP_CONFIGURATION -> hashMapOf(
                            MAC_ADDRESS to { state: String ->
                                viewModel.destinationMacAddress.value = state
                            }
                    )
                    else -> hashMapOf()
                }

        val configTableData : List<Pair<Triple<String, String, String>?, Triple<String, String, String>?>> =
                when(configType) {
                    IP_CONFIGURATION -> listOf(
                            Pair(
                                    Triple(
                                            DEVICE_ID,
                                            viewModel.deviceId.value,
                                            getString(R.string.txt_ip_device_instance_number_hint)
                                    ),
                                    Triple(DESTINATION_IP, viewModel.destinationIp.value, ""),
                            ),
                            Pair(
                                    Triple(
                                            DESTINATION_PORT,
                                            viewModel.destinationPort.value,
                                            getString(R.string.txt_destination_port_value_hint)
                                    ),
                                    Triple(MAC_ADDRESS, viewModel.destinationMacAddress.value, "")
                            ),
                            Pair(
                                    Triple(
                                            DEVICE_NETWORK,
                                            viewModel.dnet.value,
                                            getString(R.string.txt_dnet_value_hint)
                                    ),
                                    null
                            )
                    )

                    MSTP_CONFIGURATION -> listOf(
                            Pair(
                                    Triple(
                                            MAC_ADDRESS,
                                            viewModel.destinationMacAddress.value,
                                            if (viewModel.deviceSelectionMode.value == 0) MAC_ADDRESS_INFO_SLAVE
                                            else MAC_ADDRESS_INFO_MASTER
                                    ),
                                    null
                            )
                    )

                    else -> emptyList()
                }

        if(viewModel.deviceSelectionMode.value == 0){
            // slave device config
            EditableConfigFields(configTableData, configEntryWithHint, numberTypeConfigEntries, editableEventMap,
                    defaultTextList)
        }else{
            // master device config
            EditableConfigFields(configTableData, configEntryWithHint, numberTypeConfigEntries, editableEventMap,
                    defaultTextList)
        }
    }

    @Composable
    private fun EditableConfigFields(
            configTableData: List<Pair<Triple<String, String, String>?, Triple<String, String, String>?>>,
            configEntryWithHint: List<String>,
            numberTypeConfigEntries: List<String>,
            editableEventMap: HashMap<String, (String) -> Unit>,
            defaultTextList: List<String>
    ) {
        Column(modifier = Modifier
                .padding(top = 20.dp)
                .wrapContentHeight()
                .fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            configTableData.forEach { rowPair ->
                val subRowPair1 = rowPair.first
                val subRowPair2 = rowPair.second

                Row(horizontalArrangement = Arrangement.spacedBy(50.dp)) {
                    Row(modifier = Modifier
                            .padding(top = 14.dp)
                            .weight(0.5f)
                            .align(Alignment.Top), verticalAlignment = Alignment.Top) {
                        subRowPair1?.let {
                            Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.TopStart) {
                                if(subRowPair1.first in configEntryWithHint) {
                                    TextViewWithHint(modifier = Modifier, text = annotatedStringBySpannableString(text = subRowPair1.first), hintText = subRowPair1.third, fontSize = 22)
                                } else {
                                    LabelTextViewForTable(
                                            modifier = Modifier.align(Alignment.CenterStart),
                                            text = subRowPair1.first,
                                            fontSize = 22
                                    )
                                }
                            }
                            Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.TopStart) {
                                editableEventMap[subRowPair1.first]?.let { it1 -> HintedEditableText(valueTypeIsNumber = numberTypeConfigEntries.contains(subRowPair1.first), hintText = "Enter ${subRowPair1.first}", defaultText = if(defaultTextList.contains(subRowPair1.first)) subRowPair1.second else "", onEditEvent = { it1 -> viewModel.updateMacAddress(it1) }) }
                            }
                        }
                    }
                    Row(modifier = Modifier
                            .padding(top = 14.dp)
                            .weight(0.5f)
                            .align(Alignment.Top), verticalAlignment = Alignment.Top) {
                        subRowPair2?.let {
                            Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.TopStart) {
                                if(subRowPair2.first in configEntryWithHint) {
                                    TextViewWithHint(modifier = Modifier, text = annotatedStringBySpannableString(text = subRowPair2.first), hintText = subRowPair2.third, fontSize = 22)
                                } else {
                                    LabelTextViewForTable(
                                            modifier = Modifier.align(Alignment.CenterStart),
                                            text = subRowPair2.first,
                                            fontSize = 22
                                    )
                                }
                            }
                            Box(modifier = Modifier.weight(0.25f), contentAlignment = Alignment.TopStart) {
                                editableEventMap[subRowPair2.first]?.let { it1 -> HintedEditableText(valueTypeIsNumber = numberTypeConfigEntries.contains(subRowPair2.first), hintText = "Enter ${subRowPair2.first}", defaultText = if(defaultTextList.contains(subRowPair2.first)) subRowPair2.second else "", onEditEvent = { it1 -> viewModel.updateMacAddress(it1) }) }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun DeviceSelector() {

        Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
        ) {

            Row {
                Box(modifier = Modifier
                        .weight(1f)
                ) {
                }
                Box(modifier = Modifier
                        .weight(1f)
                        /*.padding(top = 5.dp, bottom = 10.dp)*/) {
                    //RadioButtonComposeSelectModel()
                    val radioOptions = listOf("Slave", "Master")
                    RadioButtonComposeSelectModelCustom(
                            radioOptions, viewModel.deviceSelectionMode.value
                    ) {
                        when (it) {
                            "Slave" -> {
                                viewModel.resetBacnetNetworkConfig()
                                viewModel.deviceSelectionMode.value = 0
                            }

                            "Master" -> {
                                ProgressDialogUtils.showProgressDialog(context, CONST_AUTO_DISCOVERY)
                                viewModel.resetBacnetNetworkConfig()
                                viewModel.deviceSelectionMode.value = 1
                                viewModel.searchDevices()
                                CcuLog.d("TAG", "searching devices ${viewModel.isConnectedDevicesSearchFinished.value}")
                            }
                        }
                    }
                }
                Box(modifier = Modifier
                        .weight(1f)
                ) {
                }
            }
        }
        DeviceInfo()
    }

    @Composable
    fun RadioButtonSelector(
            headerText: String,
            radioOptions: List<String>,
            defaultValue: Int,
            onSelectEvent: (String) -> Unit
    ) {
        Column (/*modifier = Modifier.padding(top = 20.dp)*/){
            Box(
                    modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Gray)
                            .padding(PaddingValues(top = 10.dp, end = 25.dp)),
                    contentAlignment = Alignment.Center
            ) {
                HeaderLeftAlignedTextViewNewFixedWidth(
                        headerText,
                        fontSize = 18,
                        Modifier.padding(bottom = 0.dp)
                )
            }

            Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
            ) {
                Column(
//                        modifier = Modifier
//                                .wrapContentWidth()         // ✅ keep natural width
//                                .widthIn(max = 400.dp),     // ✅ optional cap
                        horizontalAlignment = Alignment.CenterHorizontally
                ) {
//                    Box(
//                            modifier = Modifier
//                    ) {
                    RadioButtonComposeSelectModelCustom(
                                radioOptions, defaultValue
                        ) {
                            onSelectEvent(it)
                        }
                    //}
                }
            }
        }
    }

    @Composable
    private fun ShowDropdownList(expanded: MutableState<Boolean>) {
        val configurationTypes = listOf(
                //MSTP_CONFIGURATION,
                IP_CONFIGURATION
        )

        var selectedIndex by remember { mutableStateOf(-1) }
        DropdownMenu(
                expanded = expanded.value,
                onDismissRequest = { expanded.value = false },
                modifier = Modifier
                        .width(280.dp)
                        .height(120.dp)
                        .background(Color.White)
                        .border(0.5.dp, Color.LightGray)
                        .shadow(1.dp, shape = RoundedCornerShape(2.dp))

        ) {
            LazyColumn(modifier = Modifier
                    .width(280.dp)
                    .height(120.dp)) {

                itemsIndexed(configurationTypes) { index, s ->
                    DropdownMenuItem(onClick = {
                        selectedIndex = index
                        expanded.value = false
                        viewModel.configurationType.value = s
                        viewModel.deviceSelectionMode.value = 0
                    }, text = { Text(text = s, style = TextStyle(fontSize = 22.sp)) },
                            modifier = Modifier.background(if (index == selectedIndex) ComposeUtil.secondaryColor else Color.White),
                            contentPadding = PaddingValues(10.dp),
                    )
                }
            }
        }
    }

    @Composable
    fun BacnetButtons(){
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(50.dp)
                .padding(PaddingValues(bottom = 10.dp, end = 10.dp)),
            contentAlignment = Alignment.CenterEnd
        ) {
        }

        Row(
            modifier = Modifier
                .fillMaxWidth(),
                //.padding(50.dp)
                //.padding(PaddingValues(bottom = 5.dp)),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var isEnabledFetch = false
            if(!viewModel.bacnetModel.value.isDevicePaired){
                if(viewModel.deviceId.value.isNotEmpty()
                    && viewModel.destinationIp.value.isNotEmpty()
                    && viewModel.destinationPort.value.isNotEmpty()
                    && viewModel.bacnetModel.value.points.isNotEmpty()
                    ) {
                    isEnabledFetch = true
                }
            }

            var fetchButtonText = FETCH
            if(viewModel.bacnetPropertiesFetched.value){
                fetchButtonText = RE_FETCH
            }
            if(viewModel.configurationType.value == IP_CONFIGURATION){
                SaveTextView(fetchButtonText, isEnabledFetch) {
                    if(viewModel.destinationIp.value.isNullOrEmpty() ){
                        Toast.makeText(requireContext(), getString(R.string.ipAddressValidation), Toast.LENGTH_SHORT).show()
                    }else if(viewModel.destinationPort.value.isNullOrEmpty()){
                        Toast.makeText(requireContext(), getString(R.string.portValidation), Toast.LENGTH_SHORT).show()
                    }else if(viewModel.deviceId.value.isNullOrEmpty()){
                        Toast.makeText(requireContext(), getString(R.string.deviceIdValidation), Toast.LENGTH_SHORT).show()
                    }else{
                        if(viewModel.isBacnetIpEnabled.value){
                            viewModel.fetchData()
                        }else{
                            Toast.makeText(requireContext(), getString(R.string.bacnetIpNotInitializedWarning), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun BacnetDeviceNetworkDetailsReadOnly() {

        Row(
            modifier = Modifier
                .padding(PaddingValues(bottom = 5.dp)),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaddingValues(bottom = 0.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        LabelTextView("Device Network", fontSize = 22)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        LabelTextView(viewModel.dnet.value, fontSize = 22)
                    }
                }
            }
        }
    }

    @Composable
    fun BacnetPortDetailsReadOnly() {

        Row(
            modifier = Modifier
                .padding(PaddingValues(bottom = 5.dp)),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingValues(bottom = 0.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        LabelTextView(annotatedString(DESTINATION_PORT), fontSize = 22)
                    }

                    Box(
                        modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                    ) {
                        LabelTextView(viewModel.destinationPort.value, fontSize = 22)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingValues(bottom = 0.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        LabelTextView("Mac Address", fontSize = 22)
                    }

                    Box(
                        modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                    ) {
                        LabelTextView(viewModel.destinationMacAddress.value, fontSize = 22)
                    }
                }
            }
        }
    }

    @Composable
    fun BacnetDeviceDetails() {

        Row(
            modifier = Modifier
                .padding(PaddingValues(bottom = 5.dp)),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingValues(bottom = 0.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        LabelTextView(annotatedString(DEVICE_ID), fontSize = 22)
                    }

                    Box(
                        modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                    ) {
                        UnderlinedInputNumberOnly(
                            onTextChanged = {
                                viewModel.deviceId.value = it
                                CcuLog.d("BacNetSelectModelView", "device id-->$it")
                            },
                            placeholder = "Enter Device ID",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingValues(bottom = 0.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        LabelTextView(annotatedString(DESTINATION_IP), fontSize = 22)
                    }

                    Box(
                        modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                    ) {
                        val isDestinationIpInvalid = viewModel.isDestinationIpValid

                        UnderlinedInput(
                            onTextChanged = {
                                CcuLog.d("BacNetSelectModelView", "destination ip-->$it")
                                viewModel.destinationIp.value = it
                            },
                            placeholder = "Enter IP Address",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun BacnetModelName(){
        Row(
            modifier = Modifier
                    .fillMaxSize()
                    .padding(PaddingValues(bottom = 20.dp, top = 20.dp)),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderLeftAlignedTextViewNew(
                if (viewModel.bacnetModel.value.equipDevice.value!!.name.isNullOrEmpty()) "" else getName(
                    viewModel.bacnetModel.value.equipDevice.value!!.name
                ), 22, Modifier.padding(start = 20.dp)
            )

        }
    }


    @Composable
    fun BacnetHeader(){
        Row {
            Box(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaddingValues(top = 10.dp, end = 25.dp)),
                contentAlignment = Alignment.Center
            ) { HeaderCenterLeftAlignedTextView(SELECT_MODEL) }
        }
    }
    @Composable
    fun BacnetModelSelection(){
        Box(
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(top = 10.dp, end = 25.dp)),
            contentAlignment = Alignment.Center
        ) {
            Row {
                if (viewModel.bacnetModel.value.isDevicePaired) {
                    viewModel.modelName.value =
                        getName(viewModel.bacnetModel.value.equipDevice.value.name)
                    TextViewWithClick(
                        text = viewModel.modelName,
                        onClick = { },
                        enableClick = false,
                        isCompress = false
                    )
                    VersionTextView(" V " + viewModel.bacnetModel.value.version.value)
                } else {
                    TextViewWithClick(
                        text = viewModel.modelName, onClick = {
                            if (!viewModel.bacnetModel.value.isDevicePaired) {
                                showDialogFragment(
                                    ModelSelectionFragment.newInstance(
                                        viewModel.bacnetDeviceList, viewModel.onBacnetItemSelect, SEARCH_MODEL
                                    ), ModelSelectionFragment.ID
                                )
                            }
                        }, enableClick = true, isCompress = false
                    )
                    if (viewModel.bacnetModel.value.version.value.isNotEmpty()) {
                        VersionTextView(" V ${viewModel.bacnetModel.value.version.value}")
                    }
                }
            }
        }
    }

    @Composable
    fun BacnetDeviceSelectionModes() {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {
            Row {
                Box(modifier = Modifier
                    .weight(1f)
                ) {
                }
                Box(modifier = Modifier
                        .weight(1f)
                        .padding(top = 0.dp, bottom = 0.dp, start = 15.dp, end = 0.dp)) {
                    HeaderTextViewCustom("Select address")
                }
                Box(modifier = Modifier
                    .weight(1f)
                ) {
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterStart
        ) {

            Row {
                Box(modifier = Modifier
                    .weight(1f)
                ) {
                }
                Box(modifier = Modifier
                    .weight(1f)
                    /*.padding(top = 5.dp, bottom = 10.dp)*/) {
                    //RadioButtonComposeSelectModel()
                    val radioOptions = listOf("Manual", "Auto")
                    RadioButtonComposeSelectModelCustom(
                        radioOptions, viewModel.deviceSelectionMode.value
                    ) {
                        when (it) {
                            "Manual" -> {
                                viewModel.resetBacnetNetworkConfig()
                                viewModel.deviceSelectionMode.value = 0
                            }

                            "Auto" -> {
                                ProgressDialogUtils.showProgressDialog(context, CONST_AUTO_DISCOVERY)
                                viewModel.resetBacnetNetworkConfig()
                                viewModel.deviceSelectionMode.value = 1
                                viewModel.searchDevices()
                                CcuLog.d("TAG", "searching devices ${viewModel.isConnectedDevicesSearchFinished.value}")
                            }
                        }
                    }
                }
                Box(modifier = Modifier
                    .weight(1f)
                ) {
                }
            }
        }
    }

    @Composable
    fun ParameterLabel() {
        Row(modifier = Modifier
                .padding(start = 10.dp)
                .fillMaxWidth()) {
            Box(modifier = Modifier.weight(4f)) { SubTitle("PARAMETER") }
            Box(modifier = Modifier.weight(3f)) {

                Row {
                    SubTitle("DISPLAY_IN_UI")
                    Spacer(modifier = Modifier.width(40.dp))
                    //CreateToggleButton("on", viewModel.displayInUi.value)
                    ToggleButton(viewModel.displayInUi.value) {
                        viewModel.displayInUi.value = it
                        viewModel.updateDisplayInUiModules(it)
                        setStateChanged()
                    }
                }
            }
            Box(modifier = Modifier.weight(3f)) { SubTitle("MODELLED VALUE") }
            Box(modifier = Modifier.weight(3f)) {
                Row {
                    SubTitle("DEVICE VALUE")
                    Spacer(modifier = Modifier.width(40.dp))
                    //CreateToggleButton("on", viewModel.deviceValue.value)
                    ToggleButton(viewModel.deviceValue.value) {
                        if (viewModel.bacnetPropertiesFetched.value) {
                            viewModel.deviceValue.value = it
                            viewModel.updateDeviceValueInUiModules(it)
                        }
                        setStateChanged()
                    }
                }
            }
        }
    }

    @Composable
    fun BacnetDeviceDetailsReadOnly() {

        Box(
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
        }

        Row(
            modifier = Modifier
                .padding(PaddingValues(bottom = 5.dp)),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingValues(bottom = 0.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        LabelTextView(annotatedString(DEVICE_ID), fontSize = 22)
                    }

                    Box(
                        modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                    ) {
                        LabelTextView(viewModel.deviceId.value, fontSize = 22)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingValues(bottom = 0.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        LabelTextView(annotatedString(DESTINATION_IP), fontSize = 22)
                    }

                    Box(
                        modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                    ) {
                        LabelTextView(viewModel.destinationIp.value, fontSize = 22)
                    }
                }
            }
        }
    }

    @Composable
    fun annotatedString(text: String): AnnotatedString {
        val annotatedString = with(AnnotatedString.Builder()) {
            append(text)
            pushStyle(MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = Color.Red))
            append(" *")
            toAnnotatedString()
        }
        return annotatedString
    }

    @Composable
    fun BacnetPortDetails() {

        Row(
            modifier = Modifier
                .padding(PaddingValues(bottom = 5.dp, top = 16.dp)),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingValues(bottom = 0.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        LabelTextView(annotatedString(DESTINATION_PORT), fontSize = 22)
                    }

                    Box(
                        modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                    ) {
                        UnderlinedInputNumberOnly(onTextChanged = {
                            CcuLog.d("BacNetSelectModelView", "port value-->$it")
                            viewModel.destinationPort.value = it
                        },
                            placeholder = "Enter Port",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingValues(bottom = 0.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        LabelTextView("Mac Address", fontSize = 22)
                    }

                    Box(
                        modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                    ) {
                        UnderlinedInput(
                            onTextChanged = {
                                CcuLog.d("BacNetSelectModelView", "destination ip-->$it")
                                viewModel.destinationMacAddress.value = it
                            },
                            placeholder = "Enter Mac Address"
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun BacnetDeviceNetworkDetails() {
        Row(
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(bottom = 5.dp, top = 16.dp)),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f) // Ensures the Box takes 50% of the Row's width
            ) {
                Row(
                    modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingValues(bottom = 0.dp)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        LabelTextView("Device Network", fontSize = 22)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        UnderlinedInputNumberOnly(
                            onTextChanged = {
                                CcuLog.d("BacNetSelectModelView", "device network val-->$it")
                                viewModel.dnet.value = it
                            },
                            placeholder = "Enter Device Network Number",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }
        }
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

            DropDownWithLabel(
                label = "Port:",
                list = viewModel.portList,
                previewWidth = 100,
                expandedWidth = 150,
                onSelected = { index ->
                    if (viewModel.equipModel.value.port.value != viewModel.portList[index]) {
                       setStateChanged()
                    }
                    viewModel.equipModel.value.port.value = viewModel.portList[index]
                },
                defaultSelection = if (viewModel.equipModel.value.port.value.isNotEmpty()) {
                    val savedIndex =
                        viewModel.portList.indexOf(viewModel.equipModel.value.port.value)
                    if (savedIndex >= 0) savedIndex else 0
                } else 0,
                spacerLimit = 20,
                heightValue = 272
            )
            Spacer(modifier = Modifier.width(32.dp))

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
        setStateChanged()
    }

    @Composable
    private fun ParametersListItem(item: BacnetPointState) {
        Row {
            Box(modifier = Modifier
                    .weight(3f)
                    .padding(top = 10.dp, bottom = 10.dp)) {
                Row {
                    val images = listOf(
                        R.drawable.ic_arrow_down,
                        R.drawable.ic_arrow_right
                    )
                    var clickedImageIndex by remember { mutableStateOf(0) }
                    ImageViewComposable(images, "") {
                        clickedImageIndex =
                            (clickedImageIndex + 1) % images.size
                        item.displayInEditor.value = !item.displayInEditor.value
                    }
                    Box(modifier = Modifier.width(20.dp)) { }
                    if (item.disName.isNotEmpty()) {
                        HeaderTextViewMultiLine(item.disName)
                    } else {
                        HeaderTextViewMultiLine(item.name)
                    }

                }
            }

            Box(modifier =
            Modifier
                    .weight(3f)
                    .padding(top = 10.dp, bottom = 10.dp)
            ) {
                ToggleButton(item.displayInUi.value) {
                    // need to fix below 2 lines
                    item.displayInUi.value = it
                    viewModel.updateSelectAll(it, item)
                    setStateChanged()
                }
            }
            val pointDisplayValue: String = if(item.defaultValue == "null" || item.defaultValue == null)
                "-" else{
                "${item.defaultValue} ${item.defaultUnit}"
            }

            Box(modifier =
            Modifier
                    .weight(1f)
                    .padding(top = 10.dp, bottom = 10.dp), contentAlignment = Alignment.Center
            ) { LabelTextView(pointDisplayValue, fontSize = 22) }
        }

        if (item.displayInEditor.value) {
            populateProperties(item)
        }

        Divider(color = Color.Gray, modifier = Modifier.padding(0.dp, 5.dp, 0.dp, 5.dp))
    }

    @Composable
    fun populateProperties(item: BacnetPointState) {
        item.bacnetProperties!!.forEach {bacnetProperty ->
            Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 100.dp)) {

                Box(modifier = Modifier.weight(3f)) { LabelTextView(bacnetProperty.displayName, fontSize = 22) }
                Box(modifier = Modifier.weight(3f)) {
                    Row {
                        if (!viewModel.bacnetPropertiesFetched.value) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                if (bacnetProperty.defaultValue == null) {
                                    Box(modifier = Modifier
                                            .weight(3f)
                                            .padding(top = 10.dp, bottom = 10.dp)) { LabelTextView("-", fontSize = 22) }
                                } else {
                                    Box(modifier = Modifier
                                            .weight(3f)
                                            .padding(top = 10.dp, bottom = 10.dp)) { LabelTextView("${bacnetProperty.defaultValue}", fontSize = 22) }
                                }
                                Box(modifier = Modifier
                                        .weight(3f)
                                        .padding(top = 10.dp, bottom = 10.dp)) { LabelTextView(
                                    BAC_PROP_NOT_FETCHED, fontSize = 22) }
                            }
                        } else {
                            val testProperty = remember {
                                mutableStateOf(bacnetProperty)
                            }
                            BacnetProperty(testProperty, item.id,
                                item.protocolData?.bacnet?.objectId
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun BacnetProperty(bacnetProperty: MutableState<BacnetProperty>, id: String, objectId: Int?) {

        //var selectedValue by remember { mutableStateOf(bacnetProperty.selectedValue) }
        var selectedValue by remember { mutableStateOf(bacnetProperty) }

        // Observe changes in selectedValue and trigger recomposition
        DisposableEffect(bacnetProperty.value) {
            //bacnetProperty.value = selectedValue.value
            onDispose { /* Cleanup logic if needed */ }
        }

        BacnetPropertyUI(selectedValue, id, objectId)
    }

    @Composable
    fun BacnetPropertyUI(
        bacnetProperty: MutableState<BacnetProperty>,
        pointId: String,
        objectId: Int?
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val radioTexts = listOf(bacnetProperty.value.defaultValue, bacnetProperty.value.fetchedValue)
            val radioButtonDefaultState = if(viewModel.isDeviceValueSelected.value) 1 else bacnetProperty.value.selectedValue
            val radioOptions = listOf(BacnetSelectedValue.DEVICE.ordinal, BacnetSelectedValue.FETCHED.ordinal)
            RadioButtonComposeBacnet(radioTexts, radioOptions, radioButtonDefaultState) {
                when (it) {
                    BacnetSelectedValue.DEVICE.ordinal -> {
                        viewModel.isDeviceValueSelected.value = false
                        val newState = bacnetProperty.value.copy(selectedValue = 0)
                        bacnetProperty.value = newState
                        viewModel.updatePropertyStatus(0, pointId, bacnetProperty, objectId)
                    }

                    BacnetSelectedValue.FETCHED.ordinal -> {
                        val newState = bacnetProperty.value.copy(selectedValue = 1)
                        bacnetProperty.value = newState
                        viewModel.updatePropertyStatus(1, pointId, bacnetProperty, objectId)
                    }
                }
            }
        }
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
                    var boxWeight = 1f
                    if (subEquip.value.childSlaveId.value == SAME_AS_PARENT
                        && !viewModel.equipModel.value.isDevicePaired) {
                        boxWeight = 2f
                    }
                    Box(modifier = Modifier.weight(boxWeight)) {
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
                        if((subEquip.value.childSlaveId.value == SAME_AS_PARENT)
                            && !viewModel.equipModel.value.isDevicePaired) {
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
                        } else {
                            TextViewWithClickOption(
                                text = subEquip.value.slaveId,
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

        viewModel.configModel.value.dcvControl = false

        viewModel.configModel.value.occupancyMode = false
        viewModel.configModel.value.humidifierControl = false
        viewModel.configModel.value.dehumidifierControl = false

        viewModel.configType.value = ExternalAhuViewModel.ConfigType.BACNET
    }

    private fun setStateChanged() {
        viewModel.configModel.value.isStateChanged = true
    }

    private fun resetModelName(){
        viewModel.modelName.value = "Select Model"
    }

    override fun onResume() {
        super.onResume()
        CcuLog.d(TAG_BACNET, "--externalAhuFragment--onResume")
    }

    fun onOpenFragment(){
        viewModel.isBacNetEnabled("dynamic")
        CcuLog.d(TAG_BACNET, "--externalAhuFragment--onOpenFragment --isBacnetEnabled-->${viewModel.isBacnetIpEnabled}")
    }

}


