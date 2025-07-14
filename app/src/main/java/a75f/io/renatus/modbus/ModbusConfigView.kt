package a75f.io.renatus.modbus

import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.bacnet.util.SELECT_MODEL
import a75f.io.renatus.compose.ButtonListRow
import a75f.io.renatus.compose.ExternalConfigDropdownSelector
import a75f.io.renatus.compose.FormattedTableWithoutHeader
import a75f.io.renatus.compose.HeaderLeftAlignedTextView
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.LabelTextView
import a75f.io.renatus.compose.ParameterLabel
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.TableHeaderRow
import a75f.io.renatus.compose.TextViewWithClick
import a75f.io.renatus.compose.TextViewWithClickNoLeadingSpace
import a75f.io.renatus.compose.TextViewWithClickOption
import a75f.io.renatus.compose.TextViewWithClickOptionNoLeadingSpace
import a75f.io.renatus.compose.TitleTextView
import a75f.io.renatus.compose.TitleTextViewCustomNoModifier
import a75f.io.renatus.compose.ToggleButton
import a75f.io.renatus.modbus.models.EquipModel
import a75f.io.renatus.modbus.util.CANCEL
import a75f.io.renatus.modbus.util.DISPLAY_UI_CAPITALIZED
import a75f.io.renatus.modbus.util.EQUIP_TYPE
import a75f.io.renatus.modbus.util.LOADING
import a75f.io.renatus.modbus.util.MODBUS
import a75f.io.renatus.modbus.util.MODBUS_CAPITALIZED
import a75f.io.renatus.modbus.util.ModbusLevel
import a75f.io.renatus.modbus.util.OnItemSelect
import a75f.io.renatus.modbus.util.PARAMETER_CAPITALIZED
import a75f.io.renatus.modbus.util.SAME_AS_PARENT
import a75f.io.renatus.modbus.util.SAVE
import a75f.io.renatus.modbus.util.SCHEDULABLE_CAPITALIZED
import a75f.io.renatus.modbus.util.SEARCH_MODEL
import a75f.io.renatus.modbus.util.SEARCH_SLAVE_ID
import a75f.io.renatus.modbus.util.SELECT_ALL
import a75f.io.renatus.modbus.util.SET
import a75f.io.renatus.modbus.util.SLAVE_ID
import a75f.io.renatus.profiles.profileUtils.PasteBannerFragment
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider

/**
 * Created by Manjunath K on 13-07-2023.
 */

class ModbusConfigView : BaseDialogFragment() {
    private lateinit var viewModel: ModbusConfigViewModel

    companion object {
        val ID: String = ModelSelectionFragment::class.java.simpleName

        fun newInstance(
            meshAddress: Short, roomName: String, floorName: String, profileType: ProfileType,
            level: ModbusLevel, filer: String
        ): ModbusConfigView {
            val fragment = ModbusConfigView()
            val bundle = Bundle()
            bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            bundle.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal)
            bundle.putInt(FragmentCommonBundleArgs.MODBUS_LEVEL, level.ordinal)
            bundle.putString(FragmentCommonBundleArgs.MODBUS_FILTER, filer)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext())
        viewModel = ViewModelProvider(this)[ModbusConfigViewModel::class.java]
        viewModel.holdBundleValues(requireArguments())
        rootView.apply {
            setContent {
                RootView(
                    filter = requireArguments().getString(
                        FragmentCommonBundleArgs.MODBUS_FILTER,
                        ""
                    )
                )
            }
            return rootView
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.isDialogOpen.observe(viewLifecycleOwner) { isDialogOpen ->
            if (!isDialogOpen) {
                this@ModbusConfigView.closeAllBaseDialogFragments()
            }
        }
    }

    @Composable
    fun RootView(filter : String ="") {
        if (filter == "btu" || filter == "emr") {
            Column {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                        viewModel.configModelDefinition(requireContext())
                    item {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) { TitleTextView(MODBUS) }
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) { HeaderTextView(EQUIP_TYPE) }
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
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
                                        if (viewModel.equipModel.value.version.value.isNotEmpty()) {
                                            HeaderTextView("V${viewModel.equipModel.value.version.value}")
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
                                                    viewModel.slaveIdList,
                                                    onItemSelect, SEARCH_SLAVE_ID
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
                            Row(modifier = Modifier.padding(start = 10.dp)) {
                                ParameterLabel(
                                    filterValue = filter
                                )
                            }
                        }
                    }
                    item { SystemEquipParametersListView(data = viewModel.equipModel) }
                    item { SystemSubEquipments(viewModel.equipModel) }
                    item {

                        Row(
                            modifier = Modifier
                                .padding(start = 5.dp)
                                .fillMaxSize()
                                .padding(PaddingValues(bottom = 5.dp)),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (viewModel.isExistingProfile(filter) && viewModel.floorRef == "SYSTEM" && viewModel.zoneRef == "SYSTEM"
                            ) {
                                SaveTextView("UNPAIR") { viewModel.unpair() }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            SaveTextView(SET) { viewModel.saveConfiguration() }
                        }

                    }
                }
            }
        } else {
            Column {
                LazyColumn(modifier = Modifier.fillMaxSize()) {

                    // Configuring the View Model for display
                    viewModel.configModelDefinition(requireContext())
                    // Displaying the Mandatory Modbus Configuration Components
                    item {
                        Box {
                            val isDisabled by viewModel.isDisabled.observeAsState(false)
                            if (isDisabled) {
                                PasteBannerFragment.PasteCopiedConfiguration(
                                    onPaste = { viewModel.applyCopiedConfiguration() },
                                    onClose = { viewModel.disablePasteConfiguration() }
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.padding(
                                start = 40.dp,
                                top = 40.dp,
                                end = 40.dp
                            )
                        ) {

                            // Modbus Title
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                TitleTextViewCustomNoModifier(MODBUS_CAPITALIZED, Color.Black)
                            }

                            // Data collected from View Model for displaying Modbus equip version
                            val modelEquipVersion: String
                            if (viewModel.equipModel.value.isDevicePaired) {
                                viewModel.modelName.value =
                                    getName(viewModel.equipModel.value.equipDevice.value.name)
                                modelEquipVersion =
                                    viewModel.equipModel.value.equipDevice.value.modbusEquipIdId
                            } else {
                                modelEquipVersion = viewModel.equipModel.value.version.value
                            }
                            val onClickEvent = {
                                showDialogFragment(
                                    ModelSelectionFragment.newInstance(
                                        viewModel.deviceList,
                                        viewModel.onItemSelect, SEARCH_MODEL
                                    ), ModelSelectionFragment.ID
                                )
                            }

                            // Select Modbus Model Dropdown with Version Info
                            ExternalConfigDropdownSelector(
                                EQUIP_TYPE,
                                viewModel.equipModel.value.isDevicePaired,
                                viewModel.modelName,
                                modelEquipVersion,
                                onClickEvent
                            )

                            /**
                             * Displays the information regarding the parent and subEquips (if present)
                             * If equip is not paired and no model is selected, a message is displayed
                             * to select a model to proceed with configuration.
                             * Till then the equip configuration is not displayed.
                             */
                            Column(modifier = Modifier.defaultMinSize(minHeight = 372.dp)) {
                                if(!viewModel.equipModel.value.isDevicePaired && viewModel.modelName.value == SELECT_MODEL) {
                                    Box(modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 96.dp), contentAlignment = Alignment.Center) {
                                        SaveTextView(getString(R.string.select_model_to_proceed), isChanged = false, onClick = {})
                                    }
                                } else {
                                        ZoneParentEquip()
                                        ZoneSubEquipments(viewModel.equipModel)
                                }
                            }
                        }

                        // Displaying the Save and Cancel buttons
                        ButtonListRow(
                            textActionPairMap = mapOf(
                                CANCEL to Pair(true) { closeAllBaseDialogFragments() },
                                SAVE to Pair(true) { viewModel.saveConfiguration() },
                            )
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun SystemEquipParametersListView(data: MutableState<EquipModel>) {
        if (data.value.parameters.isNotEmpty()) {
            var index = 0
            while (index < data.value.parameters.size) {
                Column(
                    modifier = Modifier
                        .padding(start = 5.dp)
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Row {
                        for (rowIndex in 0 until 2) {
                            if (index < data.value.parameters.size) {
                                val item = data.value.parameters[index]
                                LabelTextView(
                                    if (item.param.value.name.length > 30)
                                        item.param.value.name.substring(0, 30)
                                    else
                                        item.param.value.name
                                    , widthValue = 440
                                )
                                ToggleButton(item.displayInUi.value) {
                                    item.displayInUi.value = it
                                    item.param.value.getParameterId()
                                    viewModel.updateSelectAll()
                                }
                                Box(modifier = Modifier.width(90.dp)) { }
                                index++
                            }
                        }
                    }
                }
            }
            viewModel.updateSelectAll()
        }
    }

    @Composable
    fun SystemSubEquipments(data: MutableState<EquipModel>) {

        Column(modifier = Modifier.padding(8.dp)) {
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
                                            onItemSelect, SEARCH_SLAVE_ID
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
                Row(modifier = Modifier.padding(start = 10.dp)) { ParameterLabel() }
                SystemEquipParametersListView(data = subEquip)
            }
        }
    }

    fun getName(name: String): String = if (name.length > 30) name.substring(0, 30) else name
    override fun getIdString(): String = ModbusConfigView::class.java.simpleName

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window!!.setLayout(width, height)
        }
    }

    @Composable
    fun ZoneParentEquip() {
        Column(modifier = Modifier.padding(top = 40.dp)) {
            ZoneEquipNameAndSlaveId()
            ZoneEquipmentPointsConfigTable(equipmentModel = viewModel.equipModel, isRootEquip = true)
        }
    }

    @Composable
    fun ZoneSubEquipments(data: MutableState<EquipModel>) {
        data.value.subEquips.forEach { subEquip ->
            Column(modifier = Modifier.padding(top = 40.dp)) {
                ZoneSubEquipNameAndId(subEquip)
                ZoneEquipmentPointsConfigTable(equipmentModel = subEquip, isRootEquip = false)
            }
        }
    }

    @Composable
    fun ZoneEquipNameAndSlaveId() {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {

            // Displays the name of the Equip
            Box(modifier = Modifier.weight(7f)) {
                HeaderLeftAlignedTextView(
                    if (viewModel.equipModel.value.equipDevice.value.name.isNullOrEmpty()) {""}
                    else {
                        viewModel.equipModel.value.equipDevice.value.name
                    },
                    modifier = Modifier
                )
            }

            // Displays the Slave ID
            Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.CenterEnd) {
                Row(modifier = Modifier.offset(x = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    HeaderLeftAlignedTextView(
                        SLAVE_ID,
                        modifier = Modifier.padding(top = 5.dp, start = 5.dp, bottom = 5.dp, end = 8.dp))

                    val onItemSelect = object : OnItemSelect {
                        override fun onItemSelected(index: Int, item: String) {
                            viewModel.equipModel.value.slaveId.value = item.toInt()
                        }
                    }
                    TextViewWithClickOptionNoLeadingSpace(
                        text = viewModel.equipModel.value.slaveId,
                        onClick = {
                            ProgressDialogUtils.showProgressDialog(context, LOADING)
                            showDialogFragment(
                                ModelSelectionFragment.newInstance(
                                    viewModel.slaveIdList,
                                    onItemSelect, SEARCH_SLAVE_ID
                                ), ModelSelectionFragment.ID
                            )
                        },
                        enableClick = !viewModel.equipModel.value.isDevicePaired,
                    )
                }
            }
        }
    }

    @Composable
    fun ZoneSubEquipNameAndId(subEquip: MutableState<EquipModel>) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {

            // Displays the name of the Sub Equip
            Box(modifier = Modifier.weight(7f)) {
                HeaderLeftAlignedTextView(
                    text = subEquip.value.equipDevice.value.name,
                    modifier = Modifier
                )
            }

            // Displays the Slave ID
            Box(modifier = Modifier.wrapContentWidth(align = Alignment.End), contentAlignment = Alignment.CenterEnd) {
                Row(modifier = Modifier.offset(x = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    HeaderLeftAlignedTextView(SLAVE_ID
                        , modifier = Modifier.padding(top = 5.dp, start = 5.dp, bottom = 5.dp, end = 8.dp))
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
                        TextViewWithClickNoLeadingSpace(
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
                    } else {
                        TextViewWithClickOptionNoLeadingSpace(
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
        }
    }

    @Composable
    fun ZoneEquipmentPointsConfigTable(equipmentModel: MutableState<EquipModel>, isRootEquip: Boolean) {

        // Stores the width of each column for the table.
        val tableColumnsWidth = remember {
            mutableStateListOf<Float>().apply {
                repeat(3) {
                    add(0.0f)
                }
            }
        }

        // List of Column Names to be displayed in the table header.
        val columnList =
            listOf(PARAMETER_CAPITALIZED, DISPLAY_UI_CAPITALIZED, SCHEDULABLE_CAPITALIZED)

        // Map of toggle callbacks for the table header.
        val toggleCallbackMap = mapOf(
            DISPLAY_UI_CAPITALIZED to Pair(equipmentModel.value.selectAllParameters.value) { state: Boolean ->
                equipmentModel.value.selectAllParameters.value = state
                viewModel.onSelectAllTerminal(equipmentModel = equipmentModel, isRootEquip = isRootEquip, state)
            }
        )

        // Displays the Table Header and sets the width of each column.
        TableHeaderRow(
            columnList = columnList, toggleCallbackMap
        ) { columnWidth ->
            tableColumnsWidth[columnWidth.first] = columnWidth.second
        }

        // Displays the list of point along with the config options.
        PointListPopulatedConfig(data = equipmentModel, true, tableColumnsWidth)
    }

    @Composable
    fun PointListPopulatedConfig(
        data: MutableState<EquipModel>,
        isRootEquip: Boolean,
        tableColumnsWidth: SnapshotStateList<Float>? = null
    ) {
        data.value.parameters.forEachIndexed { index, registerItem ->

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {

                val rowDataList = mutableListOf<Pair<String, Any>>()

                // For Point Name Column
                rowDataList.add(
                    Pair(
                        "text", Pair(
                            registerItem.param.value.name,
                            Alignment.CenterStart
                        )
                    )
                )

                // For Display in UI Column
                rowDataList.add(Pair("toggle",
                    Pair(registerItem.displayInUi.value) { state: Boolean ->
                        registerItem.displayInUi.value = state
                        registerItem.param.value.getParameterId()
                        viewModel.updateSelectAllTerminal(
                            equipmentModel = data,
                            isRootEquip = isRootEquip,
                        )
                    }
                ))

                // For Schedulable Column
                if (registerItem.param.value.userIntentPointTags.isNullOrEmpty()) {
                    rowDataList.add(Pair("text", Pair("NA", Alignment.Center)))
                } else {
                    rowDataList.add(
                        Pair(
                            "toggle",
                            Pair(registerItem.schedulable.value) { state: Boolean ->
                                registerItem.schedulable.value = state
                                registerItem.param.value.getParameterId()
                            })
                    )
                }

                FormattedTableWithoutHeader(
                    rowNo = index,
                    columnWidthList = tableColumnsWidth!!,
                    rowDataList = rowDataList,
                )
            }
        }
        viewModel.updateSelectAllTerminal(equipmentModel = data, isRootEquip = isRootEquip)
    }
}
