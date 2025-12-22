package a75f.io.renatus.bacnet

import a75f.io.api.haystack.bacnet.parser.BacnetSelectedValue
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.util.bacnet.BacnetConfigConstants
import a75f.io.logic.util.bacnet.BacnetConfigConstants.MSTP_MASTER_HIGH_LIMIT
import a75f.io.logic.util.bacnet.BacnetConfigConstants.MSTP_MASTER_LOW_LIMIT
import a75f.io.logic.util.bacnet.BacnetConfigConstants.MSTP_SLAVE_HIGH_LIMIT
import a75f.io.logic.util.bacnet.BacnetConfigConstants.MSTP_SLAVE_LOW_LIMIT
import a75f.io.logic.util.bacnet.validateInputdata
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.UtilityApplication
import a75f.io.renatus.bacnet.models.BacnetModel
import a75f.io.renatus.bacnet.models.BacnetPointState
import a75f.io.renatus.bacnet.util.BACNET
import a75f.io.renatus.bacnet.util.CONFIGURATION_TYPE
import a75f.io.renatus.bacnet.util.CONST_AUTO_DISCOVERY
import a75f.io.renatus.bacnet.util.IP_CONFIGURATION
import a75f.io.renatus.bacnet.util.LOADING
import a75f.io.renatus.bacnet.util.MAC_ADDRESS_INFO_MASTER
import a75f.io.renatus.bacnet.util.MAC_ADDRESS_INFO_SLAVE
import a75f.io.renatus.bacnet.util.MODEL
import a75f.io.renatus.bacnet.util.MSTP_CONFIGURATION
import a75f.io.renatus.bacnet.util.SELECT_ADDRESS
import a75f.io.renatus.bacnet.util.SELECT_MODEL
import a75f.io.renatus.compose.ButtonListRow
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.ComposeUtil.Companion.secondaryColor
import a75f.io.renatus.compose.ExternalConfigDropdownSelector
import a75f.io.renatus.compose.FormattedTableWithoutHeader
import a75f.io.renatus.compose.HeaderLeftAlignedTextViewNew
import a75f.io.renatus.compose.HintedEditableText
import a75f.io.renatus.compose.LabelBoldTextViewForTable
import a75f.io.renatus.compose.LabelTextView
import a75f.io.renatus.compose.LabelTextViewForTable
import a75f.io.renatus.compose.RadioButtonComposeSelectModelCustom
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.TableHeaderRow
import a75f.io.renatus.compose.TextViewWithHint
import a75f.io.renatus.compose.TitleTextViewCustom
import a75f.io.renatus.compose.annotatedStringBySpannableString
import a75f.io.renatus.modbus.ModelSelectionFragment
import a75f.io.renatus.modbus.util.BAC_PROP_NOT_FETCHED
import a75f.io.renatus.modbus.util.CANCEL
import a75f.io.renatus.modbus.util.DESTINATION_IP
import a75f.io.renatus.modbus.util.DESTINATION_PORT
import a75f.io.renatus.modbus.util.DEVICE_ID
import a75f.io.renatus.modbus.util.DEVICE_NETWORK
import a75f.io.renatus.modbus.util.DEVICE_VALUE_CAPITALIZED
import a75f.io.renatus.modbus.util.DISPLAY_UI_CAPITALIZED
import a75f.io.renatus.modbus.util.FETCH
import a75f.io.renatus.modbus.util.MAC_ADDRESS
import a75f.io.renatus.modbus.util.MODELLED_VALUE_CAPITALIZED
import a75f.io.renatus.modbus.util.ModbusLevel
import a75f.io.renatus.modbus.util.PARAMETER_CAPITALIZED
import a75f.io.renatus.modbus.util.RE_FETCH
import a75f.io.renatus.modbus.util.SAVE
import a75f.io.renatus.modbus.util.SCHEDULABLE_CAPITALIZED
import a75f.io.renatus.modbus.util.SEARCH_DEVICE
import a75f.io.renatus.modbus.util.SEARCH_MODEL
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.profileUtils.PasteBannerFragment
import a75f.io.renatus.util.ErrorToastMessage
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import io.seventyfivef.ph.core.Tags
import org.json.JSONException
import org.json.JSONObject


class BacNetSelectModelView : BaseDialogFragment() , OnPairingCompleteListener {
    private lateinit var viewModel: BacNetConfigViewModel

    private val TAG = "BacNetSelectModelView"
    private var isBacNetInitialized = false
    private var isBacnetMstpInitialized = false
    companion object {
        val ID: String = ModelSelectionFragment::class.java.simpleName

        fun newInstance(
            meshAddress: String, roomName: String, floorName: String, profileType: ProfileType,
            level: ModbusLevel, filer: String
        ): BacNetSelectModelView {
            val fragment = BacNetSelectModelView()
            val bundle = Bundle()
            bundle.putString(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
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
        viewModel = ViewModelProvider(this)[BacNetConfigViewModel::class.java]
        viewModel.holdBundleValues(requireArguments())
        rootView.apply {
            setContent { RootView() }
            return rootView
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.isDialogOpen.observe(viewLifecycleOwner) { isDialogOpen ->
            if (!isDialogOpen) {
                this@BacNetSelectModelView.closeAllBaseDialogFragments()
            }
        }
        viewModel.setOnPairingCompleteListener(this)
        viewModel.deviceIp = getDataFromSf(requireContext(), BacnetConfigConstants.IP_ADDRESS)
        CcuLog.d(TAG, "device ip--->${viewModel.deviceIp}")
        viewModel.devicePort = getDataFromSf(requireContext(), BacnetConfigConstants.PORT)
        isBacNetInitialized = isBacNetInitialized(requireContext())
        isBacnetMstpInitialized = isBacnetMstpInitialized(requireContext())
    }

    @Preview(showSystemUi = true)
    @Composable
    fun RootView() {

        if(viewModel.isErrorMsg.value){
            viewModel.isErrorMsg.value = false
            Toast.makeText(requireContext(), viewModel.errorMsg, Toast.LENGTH_LONG).show()
        }

        if(!viewModel.isDeviceIdValid.value){
            Toast.makeText(requireContext(), "DeviceId is already in Use, please change it", Toast.LENGTH_LONG).show()
        }

        if(viewModel.bacnetRequestFailed.value){
            Toast.makeText(requireContext(), "Failed to fetch the required model or model not found", Toast.LENGTH_LONG).show()
            viewModel.bacnetRequestFailed.value = false
        }

        if(viewModel.isConnectedDevicesSearchFinished.value){
            viewModel.isConnectedDevicesSearchFinished.value = false
            if(viewModel.connectedDevices.value.isNotEmpty()) {
                showDialogFragment(
                    BacnetDeviceSelectionFragment.newInstance(
                        viewModel.connectedDevices,
                        viewModel.onBacnetDeviceSelect, SEARCH_DEVICE,
                        viewModel.configurationType.value == MSTP_CONFIGURATION
                    ), BacnetDeviceSelectionFragment.ID
                )
            }
            ProgressDialogUtils.hideProgressDialog()
        }

        val state by remember { viewModel.isStateChanged }

        DisposableEffect(state) {
            onDispose {}
        }


        CcuLog.d(TAG, "RootView-->")
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {

            // Configure the view model for display
            viewModel.configModelDefinition(requireContext())

            /** If the device is already paired, then this display will eligible only for update
             *  Only DisplayInUi and Schedulable toggles can be changed.
             */
            if(viewModel.isUpdateMode.value){
                // show update UI only

                // show the banner to paste the copied configuration
                item{
                    val isDisabled by viewModel.isDisabled.observeAsState(false)
                    if (isDisabled) {
                        PasteBannerFragment.PasteCopiedConfiguration(
                            onPaste = { viewModel.applyCopiedConfiguration() },
                            onClose = { viewModel.disablePasteConfiguration() }
                        )
                    }
                }

                item {

                    // Show the BACnet Title along with selected model and read only configuration details
                    Column(modifier = Modifier.padding(start = 40.dp, end = 40.dp, top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        BacnetTitleView()
                        ModelAndConfigModeSelector(isEditable = false)
                        ConfigurationDetailsReadOnly(viewModel.configurationType.value)
                        BacnetEquipTitle()
                    }
                }
                item {
                    // Show the Bacnet Point Config Update Only
                    Column(modifier = Modifier.padding(start = 40.dp, end = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        BacnetPointConfigUpdateOnly()
                    }
                }
                item {
                    ButtonListRow(
                        textActionPairMap= mapOf(
                            CANCEL to Pair(true) { closeAllBaseDialogFragments() },
                            SAVE to Pair(true) {
                                viewModel.updateData()
                                Toast.makeText(
                                    requireContext(),
                                    "Configuration saved successfully",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        )
                    )
                }
            } else{
                // show create UI only
                item {
                    val isDisabled by viewModel.isDisabled.observeAsState(false)
                    if (isDisabled) {
                        PasteBannerFragment.PasteCopiedConfiguration(
                            onPaste = { viewModel.applyCopiedConfiguration() },
                            onClose = { viewModel.disablePasteConfiguration() }
                        )
                    }
                }
                item {
                    Column(modifier = Modifier.padding(start = 40.dp, end = 40.dp)) {
                        BacnetTitleView()
                    }
                }
                item {
                    Column(modifier = Modifier.padding(start = 40.dp, end = 40.dp)) {
                        ModelAndConfigModeSelector(isEditable = true)
                    }
                }

                if (viewModel.modelName.value == SELECT_MODEL) {
                    item {
                        Column(modifier = Modifier.padding(start = 40.dp, end = 40.dp)) {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 96.dp), contentAlignment = Alignment.Center) {
                                SaveTextView(getString(R.string.select_model_to_proceed), isChanged = false, onClick = {})
                            }
                        }
                    }
                } else {
                    item {
                        Column(modifier = Modifier.padding(start = 40.dp, end = 40.dp)) {
                            if (viewModel.deviceSelectionMode.value == 0 || viewModel.configurationType.value == MSTP_CONFIGURATION) {
                                // manual mode
                                ConfigurationDetailsEditable(viewModel.configurationType.value)
                            } else {
                                ConfigurationDetailsReadOnly(viewModel.configurationType.value)
                            }
                        }
                    }
                    item {
                        Column(modifier = Modifier.padding(start = 40.dp, end = 40.dp)) {
                            // Actual toast
                            if (viewModel.showToast.value) {
                                ErrorToastMessage(
                                    message = "BACnet - MSTP Configuration is not initialized.",
                                    onDismiss = { viewModel.showToast.value = false }
                                )
                            }
                        }
                    }
                    item {
                        Column(modifier = Modifier.padding(start = 40.dp, end = 40.dp)) {
                            BacnetEquipTitle()
                        }
                    }
                    item {
                        Column(modifier = Modifier.padding(start = 40.dp, end = 40.dp)) {
                            BacnetPointConfigCreateOnly()
                        }
                    }
                }

                item {
                    CreateOnlyConfigControlButtonList()
                }
            }
        }
    }

    @Composable
    fun ParametersListView(
        data: MutableState<BacnetModel>,
        columnsWidthList: SnapshotStateList<Float>
    ) {
        data.value.points.forEachIndexed { index, item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {

                val rowDataList = mutableListOf<Pair<String, Any>>()

                // column data for Parameter Name
                val pointName = item.name

                val dropdownImageList = listOf(
                    R.drawable.arrow_down,
                    R.drawable.arrow_right_disabled
                )
                var clickedImageIndex by remember { mutableStateOf(0) }
                val imageClickEvent = {
                    clickedImageIndex =
                        (clickedImageIndex + 1) % dropdownImageList.size
                    item.displayInEditor.value = !item.displayInEditor.value
                }
                rowDataList.add(
                    Pair(
                        "text_with_dropdown",
                        Triple(pointName, dropdownImageList, imageClickEvent)
                    )
                )

                // column data for Display In UI
                rowDataList.add(
                    Pair("toggle",
                        Pair(item.displayInUi.value) { state: Boolean ->
                            item.displayInUi.value = state
                            viewModel.updateSelectAll(state, item)
                        }
                    )
                )

                // column data for Modelled Value
                rowDataList.add(Pair("none") {})

                // column data for Device Value
                val pointDisplayValue: String =
                    if (item.defaultValue == "null" || item.defaultValue == null)
                        "-" else {
                        "${item.defaultValue} ${item.defaultUnit}"
                    }
                rowDataList.add(Pair("text", Pair(pointDisplayValue, Alignment.CenterStart)))

                // column data for Schedulable Toggles
                if (item.equipTagNames.contains(Tags.WRITABLE)) {
                    rowDataList.add(
                        Pair(
                            "toggle",
                            Pair(item.isSchedulable.value) { state: Boolean ->
                                item.isSchedulable.value = state
                                viewModel.updateSchedulableEnableState(state, item)
                            })
                    )
                } else {
                    rowDataList.add(Pair("text", Pair("NA", Alignment.Center)))
                }

                FormattedTableWithoutHeader(
                    rowNo = index,
                    columnWidthList = columnsWidthList,
                    rowDataList = rowDataList
                )

                if (item.displayInEditor.value) {
                    PopulateProperties(item, columnsWidthList)
                }
            }
        }
    }

    @Composable
    fun ParametersListViewUpdate(data: MutableState<BacnetModel>, columnsWidthList: SnapshotStateList<Float>) {
        if (data.value.points.isNotEmpty()) {
            var index = 0
            while (index < data.value.points.size) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    //Row {
                    for (rowIndex in 0 until 1) {
                        if (index < data.value.points.size) {
                            val item = data.value.points[index]
                            if(!item.disName.equals("heartbeat", ignoreCase = true) && !item.disName.contains("heartbeat", ignoreCase = true)){
                                val rowDataList = mutableListOf<Pair<String, Any>>()

                                // column data for Parameter Name
                                rowDataList.add(Pair("text", Pair(item.disName, Alignment.CenterStart)))

                                // column data for Display In UI
                                rowDataList.add(Pair("toggle", Pair(item.displayInUi.value) { state : Boolean ->
                                    item.displayInUi.value = state
                                    viewModel.updateSelectAll(state, item)
                                }))

                                // column data for Schedulable Toggles
                                if(item.equipTagNames.contains(Tags.WRITABLE)) {
                                    rowDataList.add(Pair("toggle", Pair(item.isSchedulable.value) { state : Boolean ->
                                        item.isSchedulable.value = state
                                        viewModel.updateSchedulableEnableState(state, item)
                                    }))
                                } else {
                                    rowDataList.add(Pair("text", Pair("NA", Alignment.Center)))
                                }


                                FormattedTableWithoutHeader(
                                    rowNo = index,
                                    columnWidthList = columnsWidthList,
                                    rowDataList = rowDataList
                                )
                            }
                            index++
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ModelAndConfigModeSelector(isEditable: Boolean) {
        val onModelDropdownClickEvent = if(isEditable) {
            {
                ProgressDialogUtils.showProgressDialog(context, LOADING)
                showDialogFragment(
                    ModelSelectionFragment.newInstance(
                        viewModel.deviceList,
                        viewModel.onItemSelect, SEARCH_MODEL
                    ), ModelSelectionFragment.ID
                )
            }
        } else { {} }

        val expanded = remember { mutableStateOf(false) }
        val onConfigDropdownClickEvent =
            if(isEditable) {
                {
                    expanded.value = true
                    viewModel.clearConfigFieldData()
                }
            } else { {} }

            ExternalConfigDropdownSelector(
                titleText = MODEL,
                isPaired = viewModel.bacnetModel.value.isDevicePaired,
                selectedItemName = viewModel.modelName,
                modelVersion = viewModel.bacnetModel.value.version.value,
                onClickEvent = onModelDropdownClickEvent,
                otherUiComposable = {
                    if(viewModel.modelName.value != SELECT_MODEL) {
                        ExternalConfigDropdownSelector(
                            titleText = CONFIGURATION_TYPE,
                            isPaired = viewModel.bacnetModel.value.isDevicePaired,
                            selectedItemName = viewModel.configurationType,
                            modelVersion = "",
                            onClickEvent = onConfigDropdownClickEvent,
                            otherUiComposable = {
                                if(isEditable) {
                                    ShowDropdownList(expanded)
                                    when(viewModel.configurationType.value) {
                                        IP_CONFIGURATION -> AddressSelector()
                                        MSTP_CONFIGURATION -> DeviceSelector()
                                        else -> {}
                                    }
                                }
                            },
                            isNested = true
                        )
                    }
                }
            )

    }

    @Composable
    fun PopulateProperties(item: BacnetPointState, columnsWidthList: SnapshotStateList<Float>) {
        var propertyCount = 0
        item.bacnetProperties.forEach { bacnetProperty ->

            val rowDataList = mutableListOf<Pair<String, Any>>()

            // column data for nested property Name
            rowDataList.add(Pair("nestedText", Triple(bacnetProperty.displayName, Alignment.CenterStart, 43.dp)))

            // column data for Display In UI
            rowDataList.add(Pair("none") {})

            // column data for Device Value
            val propertyValue: String
            if (!viewModel.bacnetPropertiesFetched.value) {
                propertyValue = if (bacnetProperty.defaultValue == null) {
                    "-"
                } else {
                    "${bacnetProperty.defaultValue}"
                }
                // column data for Modelled Value
                rowDataList.add(Pair("text", Pair(propertyValue, Alignment.CenterStart)))
                // column Data for Device Data
                rowDataList.add(Pair("text", Pair(BAC_PROP_NOT_FETCHED, Alignment.CenterStart)))
            } else {
                val testProperty = remember {
                    mutableStateOf(bacnetProperty)
                }
                DisposableEffect(testProperty.value) {
                    //bacnetProperty.value = selectedValue.value
                    onDispose { /* Cleanup logic if needed */ }
                }
                val radioTexts = listOf(testProperty.value.defaultValue?.toString(), testProperty.value.fetchedValue)
                val radioButtonDefaultState = testProperty.value.selectedValue
                val radioOptions = listOf(BacnetSelectedValue.DEVICE.ordinal, BacnetSelectedValue.FETCHED.ordinal)
                val selectedItem = remember { mutableStateOf(radioOptions[radioButtonDefaultState]) }
                val pointId = item.id
                val objectId = item.protocolData?.bacnet?.objectId
                val onSelectEventHandler = { selectedItem: Int ->
                    when (selectedItem) {
                        BacnetSelectedValue.DEVICE.ordinal -> {
                            viewModel.isDeviceValueSelected.value = false
                            val newState = testProperty.value.copy(selectedValue = 0)
                            testProperty.value = newState
                            viewModel.updatePropertyStatus(0, pointId, testProperty, objectId)
                        }

                        BacnetSelectedValue.FETCHED.ordinal -> {
                            val newState = testProperty.value.copy(selectedValue = 1)
                            testProperty.value = newState
                            viewModel.updatePropertyStatus(1, pointId, testProperty, objectId)
                        }
                    }
                }
                rowDataList.add(
                    Pair("grouped_columns_with_radio_button", Triple(Triple(radioTexts, radioButtonDefaultState, radioOptions), Pair(BacnetSelectedValue.DEVICE.ordinal, selectedItem), onSelectEventHandler))
                )
                rowDataList.add(
                    Pair("grouped_columns_with_radio_button", Triple(Triple(radioTexts, radioButtonDefaultState, radioOptions), Pair(BacnetSelectedValue.FETCHED.ordinal, selectedItem), onSelectEventHandler))
                )
            }
            // column data for Schedulable Toggles
            rowDataList.add(Pair("none") {})

            FormattedTableWithoutHeader(rowNo = propertyCount, columnWidthList = columnsWidthList, rowDataList = rowDataList)
            propertyCount++
        }
    }

    fun getName(name: String): String {
        return if (name.length > 30) name.substring(0, 30) else name
    }

    override fun getIdString(): String {
        return BacNetSelectModelView::class.java.simpleName
    }

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
    fun BacnetReconfigParameterLabel(tableColumnsWidth: SnapshotStateList<Float>) {
        val visibleHeaderColumns = listOf(
            PARAMETER_CAPITALIZED, DISPLAY_UI_CAPITALIZED, SCHEDULABLE_CAPITALIZED)

        val toggleCallbackMap = mapOf(
            DISPLAY_UI_CAPITALIZED to Pair(viewModel.displayInUi.value) { state: Boolean ->
                viewModel.displayInUi.value = state
                viewModel.updateDisplayInUiModules(state)
            }
        )

        TableHeaderRow(columnList = visibleHeaderColumns, toggleCallbackMap = toggleCallbackMap) {
            columnWidth ->
                tableColumnsWidth[columnWidth.first] = columnWidth.second
        }
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


    @Composable
    fun BacnetPairingParameterLabel(tableColumnsWidth: SnapshotStateList<Float>) {

            val visibleHeaderColumnList = listOf(PARAMETER_CAPITALIZED, DISPLAY_UI_CAPITALIZED, MODELLED_VALUE_CAPITALIZED,
                DEVICE_VALUE_CAPITALIZED, SCHEDULABLE_CAPITALIZED)

            val toggleCallbackMap = mapOf(
                DISPLAY_UI_CAPITALIZED to Pair(viewModel.displayInUi.value) { state: Boolean ->
                    viewModel.displayInUi.value = state
                    viewModel.updateDisplayInUiModules(state)
                },
                DEVICE_VALUE_CAPITALIZED to Pair(viewModel.deviceValue.value) { state: Boolean ->
                    if (viewModel.bacnetPropertiesFetched.value) {
                        viewModel.deviceValue.value = state
                        viewModel.updateDeviceValueInUiModules(state)
                    }
                }
            )
            TableHeaderRow(columnList = visibleHeaderColumnList, toggleCallbackMap = toggleCallbackMap) {
                    columnWidth ->
                tableColumnsWidth[columnWidth.first] = columnWidth.second
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
    fun ConfigurationDetailsEditable(configType: String) {

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

        val defaultTextList =
            when(configType) {
                MSTP_CONFIGURATION -> listOf(MAC_ADDRESS)
                else -> emptyList()
            }

        EditableConfigFields(configTableData, configEntryWithHint, numberTypeConfigEntries, editableEventMap,
            defaultTextList)

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
                                editableEventMap[subRowPair1.first]?.let { it1 -> HintedEditableText(valueTypeIsNumber = numberTypeConfigEntries.contains(subRowPair1.first), hintText = "Enter ${subRowPair1.first}", defaultText = if(defaultTextList.contains(subRowPair1.first)) subRowPair1.second else "", onEditEvent = it1) }
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
                                editableEventMap[subRowPair2.first]?.let { it1 -> HintedEditableText(valueTypeIsNumber = numberTypeConfigEntries.contains(subRowPair2.first), hintText = "Enter ${subRowPair2.first}", defaultText = if(defaultTextList.contains(subRowPair2.first)) subRowPair2.second else "", onEditEvent = it1) }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun AddressSelector() {
        RadioButtonSelector(SELECT_ADDRESS, listOf("Manual", "Auto"), viewModel.deviceSelectionMode.value) {
            viewModel.clearConfigFieldData()
            when (it) {
                "Manual" -> {
                    viewModel.deviceSelectionMode.value = 0
                }

                "Auto" -> {
                    if (!viewModel.isAutoFetchSelected.value) {
                        viewModel.isAutoFetchSelected.value = true
                        viewModel.deviceSelectionMode.value = 1
                        viewModel.searchDevices()

                        ProgressDialogUtils.showProgressDialog(
                            context,
                            CONST_AUTO_DISCOVERY
                        )
                        CcuLog.d(
                            TAG,
                            "searching devices ${viewModel.isConnectedDevicesSearchFinished.value}"
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun DeviceSelector() {
        RadioButtonSelector(SEARCH_DEVICE, listOf("Slave", "Master"), viewModel.deviceSelectionMode.value) {
            viewModel.clearConfigFieldData()
            when (it) {
                "Slave" -> {
                    viewModel.deviceSelectionMode.value = 0
                }

                "Master" -> {
                    if (!viewModel.isAutoFetchSelected.value) {
                        viewModel.isAutoFetchSelected.value = true

                        viewModel.deviceSelectionMode.value = 1
                        if (!isBacnetMstpInitialized) {
                            viewModel.showToast.value = true
                        } else {
                            viewModel.showToast.value = false
                            viewModel.searchDevices()

                            ProgressDialogUtils.showProgressDialog(context, CONST_AUTO_DISCOVERY)
                            CcuLog.d(
                                TAG,
                                "searching devices ${viewModel.isConnectedDevicesSearchFinished.value}"
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun RadioButtonSelector(
        headerText: String,
        radioOptions: List<String>,
        defaultValue: Int,
        onSelectEvent: (String) -> Unit
    ) {
        Column (modifier = Modifier.padding(top = 20.dp)){

            HeaderLeftAlignedTextViewNew(
                headerText,
                fontSize = 18,
                Modifier.padding(bottom = 0.dp)
            )

            Row {
                Box(
                    modifier = Modifier
                ) {
                    RadioButtonComposeSelectModelCustom(
                        radioOptions, defaultValue
                    ) {
                        onSelectEvent(it)
                    }
                }
            }
        }
    }

    @Composable
    fun BacnetTitleView() {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) { TitleTextViewCustom(BACNET, Color.Black) }
    }

    @Composable
    fun BacnetEquipTitle() {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(top = 40.dp)),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderLeftAlignedTextViewNew(
                if (viewModel.bacnetModel.value.equipDevice.value.name.isEmpty()) "" else getName(
                    viewModel.bacnetModel.value.equipDevice.value.name
                ), 22,
            )

        }
    }

    @Composable
    fun BacnetPointConfigUpdateOnly() {
        val tableColumnWidth = remember { mutableStateListOf<Float>().apply {
            repeat(3) {
                add(0.0f)
            }
        } }
        Row(modifier = Modifier.padding(top = 10.dp)) { BacnetReconfigParameterLabel(tableColumnWidth) }

        ParametersListViewUpdate(viewModel.bacnetModel, tableColumnWidth)
    }

    @Composable
    fun BacnetPointConfigCreateOnly() {
        val tableColumnsWidth = remember { mutableStateListOf<Float>().apply {
            repeat(5) {
                add(0.0f)
            }
        } }
        Row(modifier = Modifier.padding(top = 10.dp))
        { BacnetPairingParameterLabel(tableColumnsWidth) }

        ParametersListView(data = viewModel.bacnetModel, columnsWidthList = tableColumnsWidth)
    }

    @Composable
    fun CreateOnlyConfigControlButtonList() {
        var showToast by remember { mutableStateOf(false) }
        var warningToastMessage by remember { mutableStateOf("") }
        val buttonInfoMap = mutableMapOf (
            CANCEL to Pair(true) { closeAllBaseDialogFragments() },
        )

        buttonInfoMap[SAVE] =
            Pair(true) {
                val (isValidWarning, warningMessage) = getMinValidationWarningToastMessage()
                if(isValidWarning) {
                    Toast.makeText(requireContext(), warningMessage, Toast.LENGTH_SHORT).show()
                }else{
                    viewModel.save()
                }
            }
        if (showToast) {
            ErrorToastMessage(
                message = warningToastMessage,
                onDismiss = { showToast = false }
            )
        }
        ButtonListRow(textActionPairMap = buttonInfoMap)
    }

    private fun getMinValidationWarningToastMessage() : Pair<Boolean, String> {
        var warningMessage = ""

        if(viewModel.configurationType.value == IP_CONFIGURATION) {
            warningMessage = if(viewModel.destinationIp.value.isEmpty() ){
                getString(R.string.ipAddressValidation)
            }else if(viewModel.destinationPort.value.isEmpty()){
                getString(R.string.portValidation)
            }else if(viewModel.deviceId.value.isEmpty()){
                getString(R.string.deviceIdValidation)
            } else {
                return Pair(false, "")
            }
        } else if (viewModel.configurationType.value == MSTP_CONFIGURATION) {
            if (viewModel.destinationMacAddress.value.isEmpty()) {
                warningMessage = getString(R.string.macAddressValidation)
            } else {
                warningMessage = if (L.isBacnetMstpMacAddressExists(viewModel.destinationMacAddress.value.toInt())) {
                    "${getString(R.string.macAddress)} ${viewModel.destinationMacAddress.value} ${getString(R.string.already_exists_validation)}"
                } else {
                    if (viewModel.deviceSelectionMode.value == 1 &&
                        !validateInputdata(MSTP_MASTER_LOW_LIMIT,MSTP_MASTER_HIGH_LIMIT, viewModel.destinationMacAddress.value.toInt()) ) {
                        getString(R.string.bacnetMstpMasterMacValidation)
                    } else if (viewModel.deviceSelectionMode.value == 0 &&
                        !validateInputdata(MSTP_SLAVE_LOW_LIMIT, MSTP_SLAVE_HIGH_LIMIT, viewModel.destinationMacAddress.value.toInt())) {
                        getString(R.string.bacnetMstpSlaveMacValidation)
                    } else {
                        return Pair(false, "")
                    }
                }
            }
        }

        return Pair(true, warningMessage)
    }


    override fun onPairingComplete() {
        this.closeAllBaseDialogFragments()
    }

    private fun isBacnetMstpInitialized(context: Context): Boolean {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return sharedPreferences.getBoolean(BacnetConfigConstants.IS_BACNET_MSTP_INITIALIZED, false)
    }

    @Composable
    fun ConfigItemLabel(title: String, disabled: Boolean): AnnotatedString {
        return buildAnnotatedString {
            append(title)

            if (disabled) {
                append("  ")  // spacing before italic text
                withStyle(style = SpanStyle(fontStyle = FontStyle.Italic, color = Color.Gray)) {
                    append("   Not Initialized")
                }
            }
        }
    }


    @Composable
    private fun ShowDropdownList(expanded: MutableState<Boolean>) {

        val mstpConfigLabelForDropdown =
            if(!UtilityApplication.isBacnetMstpInitialized()) "MSTP Configuration Not Initialized" else MSTP_CONFIGURATION
        val ipConfigLabelForDropdown =
            if(!UtilityApplication.isBACnetIntialized()) "IP Configuration Not Initialized" else IP_CONFIGURATION

        val configurationTypes = listOf(
            mstpConfigLabelForDropdown,
            ipConfigLabelForDropdown
        )

        var selectedIndex by remember { mutableStateOf(-1) }
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
            modifier = Modifier
                .width(320.dp)
                .height(120.dp)
                .background(Color.White)
                .border(0.5.dp, Color.LightGray)
                .shadow(1.dp, shape = RoundedCornerShape(2.dp))

        ) {
            LazyColumn(modifier = Modifier
                .width(320.dp)
                .height(120.dp)) {

                itemsIndexed(configurationTypes) { index, s ->

                    val isMstp = s.contains("MSTP")
                    val isIp = s.contains("IP")

                    val isDisabled =
                        (isMstp && !UtilityApplication.isBacnetMstpInitialized()) ||
                                (isIp && !UtilityApplication.isBACnetIntialized())

                    val label = when {
                        isMstp -> ConfigItemLabel("MSTP Configuration", !UtilityApplication.isBacnetMstpInitialized())
                        isIp   -> ConfigItemLabel("IP Configuration", !UtilityApplication.isBACnetIntialized())
                        else   -> AnnotatedString(s)
                    }

                    DropdownMenuItem(
                        onClick = {
                            if (!isDisabled) {
                                selectedIndex = index
                                expanded.value = false
                                viewModel.configurationType.value =
                                    if (s.contains("MSTP")) MSTP_CONFIGURATION else IP_CONFIGURATION
                                viewModel.deviceSelectionMode.value = 0
                            }
                        },
                        enabled = !isDisabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (index == selectedIndex) ComposeUtil.secondaryColor
                                else Color.White
                            ),
                        text = {
                            Text(
                                text = label,
                                fontSize = 18.sp,
                                color = if (isDisabled) Color.Gray else Color.Black
                            )
                        }
                    )
                }

            }
        }
    }
}
