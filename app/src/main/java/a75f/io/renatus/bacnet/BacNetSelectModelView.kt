package a75f.io.renatus.bacnet

import a75f.io.api.haystack.bacnet.parser.BacnetProperty
import a75f.io.api.haystack.bacnet.parser.BacnetSelectedValue
import a75f.io.device.bacnet.BacnetConfigConstants
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.bacnet.models.BacnetModel
import a75f.io.renatus.bacnet.models.BacnetPointState
import a75f.io.renatus.bacnet.util.BACNET
import a75f.io.renatus.bacnet.util.CONST_AUTO_DISCOVERY
import a75f.io.renatus.bacnet.util.LOADING
import a75f.io.renatus.bacnet.util.SELECTED_MODEL
import a75f.io.renatus.bacnet.util.SELECT_MODEL
import a75f.io.renatus.compose.EditableTextFieldWhiteBgUnderline
import a75f.io.renatus.compose.EditableTextFieldWhiteBgUnderlineOnlyNumbers
import a75f.io.renatus.compose.HeaderLeftAlignedTextViewNew
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.HeaderTextViewCustom
import a75f.io.renatus.compose.ImageViewComposable
import a75f.io.renatus.compose.LabelTextView
import a75f.io.renatus.compose.RadioButtonComposeBacnet
import a75f.io.renatus.compose.RadioButtonComposeSelectModelCustom
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.compose.SubTitle
import a75f.io.renatus.compose.TextViewWithClickCustom
import a75f.io.renatus.compose.TitleTextViewCustom
import a75f.io.renatus.compose.ToggleButton
import a75f.io.renatus.modbus.ModelSelectionFragment
import a75f.io.renatus.modbus.util.BAC_PROP_NOT_FETCHED
import a75f.io.renatus.modbus.util.CANCEL
import a75f.io.renatus.modbus.util.DESTINATION_IP
import a75f.io.renatus.modbus.util.DESTINATION_PORT
import a75f.io.renatus.modbus.util.DEVICE_ID
import a75f.io.renatus.modbus.util.FETCH
import a75f.io.renatus.modbus.util.ModbusLevel
import a75f.io.renatus.modbus.util.RE_FETCH
import a75f.io.renatus.modbus.util.SAVE
import a75f.io.renatus.modbus.util.SEARCH_DEVICE
import a75f.io.renatus.modbus.util.SEARCH_MODEL
import a75f.io.renatus.util.ProgressDialogUtils
import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import org.json.JSONException
import org.json.JSONObject


class BacNetSelectModelView : BaseDialogFragment() {
    private lateinit var viewModel: BacNetConfigViewModel

    private val TAG = "BacNetSelectModelView"
    private var isBacNetInitialized = false

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
        viewModel.deviceIp = getDataFromSf(requireContext(), BacnetConfigConstants.IP_ADDRESS)
        CcuLog.d(TAG, "device ip--->${viewModel.deviceIp}")
        viewModel.devicePort = getDataFromSf(requireContext(), BacnetConfigConstants.PORT)
        isBacNetInitialized = isBacNetInitialized(requireContext())
        if (!isBacNetInitialized) {
            Toast.makeText(requireContext(), "CCU BacNet Server is not initialized", Toast.LENGTH_SHORT).show()
        }
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

        if(viewModel.bacnetPropertiesFetched.value){
            Toast.makeText(requireContext(), "Fetched Properties from Bacnet", Toast.LENGTH_LONG).show()
        }

        if(viewModel.isConnectedDevicesSearchFinished.value){
            viewModel.isConnectedDevicesSearchFinished.value = false
            if(viewModel.connectedDevices.value.isEmpty()){
                ProgressDialogUtils.hideProgressDialog()
                Toast.makeText(requireContext(), "No devices found", Toast.LENGTH_SHORT).show()
            }else{
                showDialogFragment(
                    BacnetDeviceSelectionFragment.newInstance(
                        viewModel.connectedDevices,
                        viewModel.onBacnetDeviceSelect, SEARCH_DEVICE
                    ), BacnetDeviceSelectionFragment.ID
                )
                ProgressDialogUtils.hideProgressDialog()
            }
        }

        val state by remember { viewModel.isStateChanged }

        DisposableEffect(state) {
            onDispose {}
        }


        Log.d(TAG, "RootView-->")
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(50.dp),
        ) {
            viewModel.configModelDefinition(requireContext())
            if(viewModel.isUpdateMode.value){
                // show update UI only
                item{
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) { TitleTextViewCustom(BACNET, Color.Black) }

                    ModelSelected()
                    DeviceDetailsReadOnly()
                    PortDetailsReadOnlyUpdate()

                }
                item {
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
                            ), 22
                        )

                    }
                    Row(modifier = Modifier.padding(start = 10.dp)) { ParameterLabelUpdateConfig() }

                    ParametersListViewUpdate(viewModel.bacnetModel)
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(PaddingValues(bottom = 5.dp)),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SaveTextView(CANCEL) {
                            dismiss()
                        }
                        SaveTextView(SAVE) { viewModel.updateData()
                            Toast.makeText(requireContext(), "Configuration saved successfully", Toast.LENGTH_LONG).show()
                            //dismiss()
                        }
                    }

                }
            }else{
                // show create UI only
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) { TitleTextViewCustom(BACNET, Color.Black) }
                    ModelSelection()
                    BacnetDeviceSelectionModes()

                    if(viewModel.deviceSelectionMode.value == 0){
                        // manual mode
                        DeviceDetails()
                        PortDetails()
                    }else{
                        DeviceDetailsReadOnly()
                        PortDetailsReadOnly()
                    }

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
                            ), 22
                        )

                    }
                    Row(modifier = Modifier.padding(start = 10.dp)) { ParameterLabel() }
                }
                item { ParametersListView(data = viewModel.bacnetModel) }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(PaddingValues(bottom = 10.dp, end = 10.dp)),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                    }
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(PaddingValues(bottom = 5.dp)),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var isEnabledFetch = false
                        if(viewModel.deviceId.value.isNotEmpty()
                            && viewModel.destinationIp.value.isNotEmpty()
                            && viewModel.destinationPort.value.isNotEmpty()) {
                            isEnabledFetch = true
                        }

                        SaveTextView(CANCEL) { dismiss() }

                        Divider(modifier = Modifier
                            .height(48.dp)
                            .width(1.dp))

                        var fetchButtonText = FETCH
                        if(viewModel.bacnetPropertiesFetched.value){
                            fetchButtonText = RE_FETCH
                        }

                        SaveTextView(fetchButtonText, isEnabledFetch) {
                            if(viewModel.destinationIp.value.isNullOrEmpty() ){
                                Toast.makeText(requireContext(), "Please input ip address", Toast.LENGTH_SHORT).show()
                            }else if(viewModel.destinationPort.value.isNullOrEmpty()){
                                Toast.makeText(requireContext(), "Please input port number", Toast.LENGTH_SHORT).show()
                            }else if(viewModel.deviceId.value.isNullOrEmpty()){
                                Toast.makeText(requireContext(), "Please input deviceId number", Toast.LENGTH_SHORT).show()
                            }else{
                                if(isBacNetInitialized){
                                    viewModel.fetchData()
                                }else{
                                    Toast.makeText(requireContext(), "BacNet is not initialized", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        if(viewModel.bacnetPropertiesFetched.value && viewModel.isDeviceIdValid.value){
                            Divider(modifier = Modifier
                                .height(40.dp)
                                .width(1.dp))
                            SaveTextView(SAVE) {
                                if(viewModel.destinationIp.value.isNullOrEmpty() ){
                                    Toast.makeText(requireContext(), "Please input ip address", Toast.LENGTH_SHORT).show()
                                }else if(viewModel.destinationPort.value.isNullOrEmpty()){
                                    Toast.makeText(requireContext(), "Please input port number", Toast.LENGTH_SHORT).show()
                                }else if(viewModel.deviceId.value.isNullOrEmpty()){
                                    Toast.makeText(requireContext(), "Please input deviceId number", Toast.LENGTH_SHORT).show()
                                }else{
                                    viewModel.save()
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    @Composable
    fun ParametersListView(data: MutableState<BacnetModel>) {
        if (data.value.points.isNotEmpty()) {
            var index = 0
            while (index < data.value.points.size) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    for (rowIndex in 0 until 1) {
                        if (index < data.value.points.size) {
                            var item = data.value.points[index]
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
                                        HeaderTextView(
                                            if (item.name.length > 30)
                                                item.name.substring(0, 30)
                                            else
                                                item.name
                                        )
                                    }
                                }

                                Box(modifier = Modifier.weight(3f)) {
                                    ToggleButton(item.displayInUi.value) {
                                        // need to fix below 2 lines
                                        item.displayInUi.value = it
                                        viewModel.updateSelectAll(it, item)
                                    }
                                }
                                val pointDisplayValue: String = if(item.defaultValue == "null" || item.defaultValue == null)
                                    "-" else{
                                    "${item.defaultValue} ${item.defaultUnit}"
                                }

                                Box(modifier = Modifier.weight(1f)) { LabelTextView(pointDisplayValue, fontSize = 22) }
                            }

                            if (item.displayInEditor.value) {
                                populateProperties(item)
                            }
                            index++
                        }
                    }
                    Divider(color = Color.Gray, modifier = Modifier.padding(0.dp, 5.dp, 0.dp, 5.dp))
                }
            }
        }
    }

    @Composable
    fun ParametersListViewUpdate(data: MutableState<BacnetModel>) {
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
                            if(item.disName.toLowerCase() != "heartbeat"){
                                Row {
                                    Box(modifier = Modifier
                                        .weight(5f)
                                        .padding(top = 10.dp, bottom = 10.dp)) {
                                        Row {
                                            val images = listOf(
                                                //R.drawable.ic_arrow_down,
                                                R.drawable.ic_arrow_right
                                            )
                                            var clickedImageIndex by remember { mutableStateOf(0) }
                                            ImageViewComposable(images, "") {

                                            }
                                            HeaderTextView(
                                                if (item.disName.length> 30)
                                                    item.disName.substring(0, 30)
                                                else
                                                    item.disName
                                            )
                                        }
                                    }

                                    Box(modifier = Modifier.weight(5f).padding(start=100.dp)) {
                                        ToggleButton(item.displayInUi.value) {
                                            item.displayInUi.value = it
                                            viewModel.updateSelectAll(it, item)
                                        }
                                    }
                                    //Box(modifier = Modifier.width(50.dp)) { }
                                    //val pointDisplayValue = "${item.defaultValue} ${item.defaultUnit}"
                                    //Box(modifier = Modifier.weight(1f)) { LabelTextView(pointDisplayValue, fontSize = 22) }
                                }

                                if (item.displayInEditor.value) {
                                    //populateProperties(item)
                                }
                                Divider(color = Color.Gray, modifier = Modifier.padding(0.dp, 5.dp, 0.dp, 5.dp))
                            }
                            index++
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ModelSelected() {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row {
                Box(
                    modifier = Modifier
                        .padding(top = 20.dp, bottom = 0.dp, start = 15.dp, end = 10.dp)
                ) {
                    HeaderTextViewCustom(SELECTED_MODEL)
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row {
                Box(
                    modifier = Modifier
                        .padding(top = 0.dp, bottom = 10.dp)
                ) {

                    TextViewWithClickCustom(
                        text = viewModel.modelName,
                        onClick = { },
                        enableClick = false,
                        isCompress = false
                    )

                    /*LabelTextView(
                        text = viewModel.modelName.value, 600, fontSize = 22, TextAlign.Center
                    )*/
                }
            }
        }
    }

    @Composable
    fun ModelSelection() {
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
                    .padding(top = 10.dp, bottom = 0.dp, start = 15.dp, end = 10.dp)) {
                    HeaderTextViewCustom(SELECT_MODEL)
                }
                Box(modifier = Modifier
                    .weight(1f)
                   ) {
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            Row {
                Box(modifier = Modifier
                    .weight(1f)
                ) {
                }
                Box(modifier = Modifier
                    .weight(1f)
                    .padding(top = 0.dp, bottom = 10.dp)) {
                    if (viewModel.bacnetModel.value.isDevicePaired) {
                        TextViewWithClickCustom(
                            text = viewModel.modelName,
                            onClick = { },
                            enableClick = false,
                            isCompress = false
                        )
                    } else {
                        TextViewWithClickCustom(
                            text = viewModel.modelName,
                            onClick = {
                                ProgressDialogUtils.showProgressDialog(context, LOADING)
                                showDialogFragment(
                                    ModelSelectionFragment.newInstance(
                                        viewModel.deviceList,
                                        viewModel.onItemSelect, SEARCH_MODEL
                                    ), ModelSelectionFragment.ID
                                )
                            },
                            enableClick = true, isCompress = false
                        )
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
                                viewModel.deviceSelectionMode.value = 0
                            }

                            "Auto" -> {
                                viewModel.deviceSelectionMode.value = 1
                                viewModel.searchDevices()

                                ProgressDialogUtils.showProgressDialog(context, CONST_AUTO_DISCOVERY)
                                CcuLog.d(TAG, "searching devices ${viewModel.isConnectedDevicesSearchFinished.value}")
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
    fun DeviceDetails() {

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
                        EditableTextFieldWhiteBgUnderlineOnlyNumbers(
                            "Enter Device ID",
                            "Invalid Device ID",
                            !viewModel.isDeviceIdValid.value,
                            onTextChanged = {
                                viewModel.deviceId.value = it
                                CcuLog.d("BacNetSelectModelView", "device id-->$it")
                            })
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
                        EditableTextFieldWhiteBgUnderline(
                            "Enter IP Address",
                            "Invalid IP Address",
                            showError = isDestinationIpInvalid.value.not(),
                            onTextChanged = {
                                //viewModel.bacnetModel.value.bacNetDevice.value!!.name = it
                                CcuLog.d("BacNetSelectModelView", "destination ip-->$it")
                                viewModel.destinationIp.value = it
                            })
                    }
                }
            }
        }
    }

    @Composable
    fun DeviceDetailsReadOnly() {

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
    private fun annotatedString(text: String): AnnotatedString {
        val annotatedString = with(AnnotatedString.Builder()) {
            append(text)
            pushStyle(MaterialTheme.typography.bodyLarge.toSpanStyle().copy(color = Color.Red))
            append(" *")
            toAnnotatedString()
        }
        return annotatedString
    }

    @Composable
    fun PortDetails() {

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
                        EditableTextFieldWhiteBgUnderlineOnlyNumbers("Enter Port","Invalid Port", false, onTextChanged = {
                            //viewModel.bacnetModel.value.bacNetDevice.value!!.name = it
                            CcuLog.d("BacNetSelectModelView", "port value-->$it")
                            viewModel.destinationPort.value = it
                        })
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
                        EditableTextFieldWhiteBgUnderline(
                            "Enter Mac Address",
                            "Invalid Mac Address",
                            false,
                            onTextChanged = {
                                //viewModel.bacnetModel.value.bacNetDevice.value!!.name = it
                                CcuLog.d("BacNetSelectModelView", "destination ip-->$it")
                                viewModel.destinationMacAddress.value = it
                            })
                    }
                }
            }
        }
    }

    @Composable
    fun PortDetailsReadOnly() {

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
    fun PortDetailsReadOnlyUpdate() {

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
                                    .padding(top = 10.dp, bottom = 10.dp)) { LabelTextView(BAC_PROP_NOT_FETCHED, fontSize = 22) }
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
    fun ParameterLabel() {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(4f)) { SubTitle("PARAMETER") }
            Box(modifier = Modifier.weight(3f)) {

                Row {
                    SubTitle("DISPLAY_IN_UI")
                    Spacer(modifier = Modifier.width(40.dp))
                    //CreateToggleButton("on", viewModel.displayInUi.value)
                    ToggleButton(viewModel.displayInUi.value) {
                        viewModel.displayInUi.value = it
                        viewModel.updateDisplayInUiModules(it)
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
                        if(viewModel.bacnetPropertiesFetched.value){
                            viewModel.deviceValue.value = it
                            viewModel.updateDeviceValueInUiModules(it)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun ParameterLabelUpdateConfig() {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(5f)) { SubTitle("PARAMETER") }
            Box(modifier = Modifier.weight(5f)) {

                Row {
                    SubTitle("DISPLAY_IN_UI")
                    Spacer(modifier = Modifier.width(20.dp))
                    //CreateToggleButton("on", viewModel.displayInUi.value)
                    ToggleButton(viewModel.displayInUi.value) {
                        viewModel.displayInUi.value = it
                        viewModel.updateDisplayInUiModules(it)
                    }
                }
            }
        }
    }

   /* private fun setStateChanged() {
        viewModel.isStateChanged.value = true
        viewModel.update()
    }*/

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

}

/*class MyPropertyDelegate<T>(private var value: T) : ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }

    private fun getIpAddress() {
        var deviceIpAddress = ""
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                // filters out 127.0.0.1 and inactive interfaces
                if (iface.isLoopback || !iface.isUp) {
                    continue
                }
                if (iface.name.startsWith("wlan") || iface.name.startsWith("eth")) {
                    val addresses = iface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (!addr.isLoopbackAddress && addr.hostAddress.indexOf(':') == -1) {
                            Log.d(
                                Tags.BACNET,
                                "device interface and ip" + iface.displayName + "-" + addr.hostAddress
                            )
                            deviceIpAddress = addr.hostAddress
                            if (iface.name.startsWith("eth")) {
                                break
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        //ipAddress.setText(deviceIpAddress)
    }


}*/

