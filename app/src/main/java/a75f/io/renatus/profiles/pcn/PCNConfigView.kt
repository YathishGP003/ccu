package a75f.io.renatus.profiles.pcn

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.pcn.ConnectModule
import a75f.io.logic.bo.building.pcn.ExternalEquip
import a75f.io.logic.bo.building.pcn.PCNUtil
import a75f.io.logic.bo.building.pcn.PCNValidation
import a75f.io.logic.bo.building.pcn.PCNViewState
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.compose.ComposeUtil.Companion.DashDivider
import a75f.io.renatus.compose.ComposeUtil.Companion.greyDropDownColor
import a75f.io.renatus.compose.ComposeUtil.Companion.primaryColor
import a75f.io.renatus.compose.HeaderLeftAlignedTextView
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.LabelBoldTextViewForTable
import a75f.io.renatus.compose.SaveTextViewNew
import a75f.io.renatus.modbus.ModbusConfigView
import a75f.io.renatus.modbus.ModelSelectionFragment
import a75f.io.renatus.modbus.util.OnItemSelect
import a75f.io.renatus.modbus.util.SEARCH_FOR_MODEL
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.connectnode.ConnectNodeFragment
import a75f.io.renatus.profiles.pcn.PCNUIUtil.Companion.ModelConfigurationView
import a75f.io.renatus.profiles.pcn.PCNUIUtil.Companion.ModuleHeader
import a75f.io.renatus.profiles.pcn.PCNUIUtil.Companion.ShowPairedEquip
import a75f.io.renatus.profiles.pcn.PCNUIUtil.Companion.SpinnerView
import a75f.io.renatus.profiles.profileUtils.PasteBannerFragment
import a75f.io.renatus.util.highPriorityDispatcher
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PCNConfigView : BaseDialogFragment(), OnPairingCompleteListener {
    val viewModel: PCNConfigViewModel by viewModels()

    companion object {

        fun newInstance(
            meshAddress: Short,
            roomName: String,
            floorName: String,
            nodeType: NodeType,
            profileType: ProfileType,
        ): PCNConfigView {
            val fragment = PCNConfigView()
            val bundle = Bundle()
            bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            bundle.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal)
            bundle.putInt(FragmentCommonBundleArgs.NODE_TYPE, nodeType.ordinal)
            fragment.arguments = bundle
            getIdString()
            return fragment
        }

        fun getIdString(): String {
            return ConnectNodeFragment::class.java.simpleName
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        val rootView = ComposeView(requireContext())
        rootView.setContent {
            ShowProgressBar()
            CcuLog.i(Domain.LOG_TAG, "Show Progress")
        }
        viewModel.isReloadRequired.observe(viewLifecycleOwner) { isDialogOpen ->
            if (isDialogOpen) {
                viewLifecycleOwner.lifecycleScope.launch(highPriorityDispatcher) {
                    withContext(Dispatchers.Main) {
                        rootView.setContent {
                            viewModel.isFreshDraw = true
                            RootView()
                        }
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch(highPriorityDispatcher) {
            viewModel.init(requireArguments(), requireContext(), CCUHsApi.getInstance())
            viewModel.setOnPairingCompleteListener(this@PCNConfigView)
            withContext(Dispatchers.Main) {
                rootView.setContent {
                    viewModel.isFreshDraw = true
                    RootView()
                }
            }
        }
        return rootView
    }

    @Composable
    fun RootView() {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 40.dp, vertical = 40.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ShowCopyPasteSection()

            ShowPCNSection()

            ShowRS485Section()

            ShowConnectModules()

            ShowExternalEquip()

            ShowNewlyPairedModules()

            ShowActionChips()

            FooterButtons()
        }
    }

    @Composable
    private fun ShowNewlyPairedModules() {
        val mergedList: List<PCNViewState.Equip> =
            (viewModel.getNewConnectModules() + viewModel.getNewExternalEquips())
                .sortedBy { it.serverId }

        mergedList.forEachIndexed { index, equip ->
            key(equip.serverId) {
                if (index != 0) {
                    DashDivider()
                }
                when (equip) {
                    is ConnectModule -> {

                        ModelConfigurationView(
                            isConnect = true,
                            equip.serverId.toString(),
                            equip,
                            viewModel = viewModel,
                            fragmentManager = requireFragmentManager()
                        )

                    }

                    is ExternalEquip -> {
                        ModuleHeader(
                            moduleTitle = PCNUIUtil.EXTERNAL_EQUIP,
                            serverId = equip.serverId.toString(),
                            isEnabled = equip.newConfiguration,
                            viewModel = viewModel,
                            requireFragmentManager()
                        )
                        ShowPairedEquip(
                            equip.name,
                            null,
                            listOf(equip.equipModel),
                            newConfiguration = equip.newConfiguration,
                            allowDelete = false,
                            viewModel,
                            fragmentManager = requireFragmentManager()
                        )
                    }

                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    @Composable
    private fun ShowCopyPasteSection() {
        val isDisabled by viewModel.isDisabled.observeAsState(false)
        if (isDisabled) {
            PasteBannerFragment.PasteCopiedConfiguration(
                onPaste = { viewModel.applyCopiedConfiguration() },
                onClose = { viewModel.disablePasteConfiguration() }
            )
        }
    }

    @Composable
    private fun ShowPCNSection() {
        HeaderTextView (text = "Protocol Converter Node", fontSize = 34)

        Spacer (modifier = Modifier.height(40.dp))

        Row (verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            HeaderLeftAlignedTextView(text = "Smart Node - Custom Code")
            Divider(
                color = Color.LightGray,
                thickness = 1.dp,
                modifier = Modifier.weight(1f)
            )
        }

        if (viewModel.viewState.value.pcnEquips.isEmpty()) {
            ModelConfigurationView(isConnect = false, serverId = null, null, viewModel,
                fragmentManager = requireFragmentManager())
        } else {
            viewModel.viewState.value.pcnEquips.sortedBy { it.name }.forEach { pcnEquip ->
                ShowPairedEquip(
                    pcnEquip.name,
                    null,
                    listOf(pcnEquip.equipModelList),
                    pcnEquip.newConfiguration,
                    false,
                    viewModel,
                    fragmentManager = requireFragmentManager()
                )
            }

        }
        CcuLog.i(L.TAG_PCN, "PCN Equipments completed")
    }

    @Composable
    private fun ShowRS485Section() {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            HeaderLeftAlignedTextView("RS485 Bridging")

            Divider(
                color = Color.LightGray,
                thickness = 1.dp,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f)
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF2F2F2)
            ),
            shape = RectangleShape
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                RS485ConfigScreen()
            }
        }

        CcuLog.i(L.TAG_PCN, "RS485 Config completed")
    }

    @Composable
    fun RS485ConfigScreen() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            HeaderLeftAlignedTextView("RS485 Config")

            Column(modifier = Modifier.padding(top = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SpinnerView(
                        label = PCNUtil.BAUD_RATE,
                        options = viewModel.getAllowedValues(
                            DomainName.modbusBaudRate,
                            viewModel.pcnDeviceModel
                        ),
                        defaultIndex = viewModel.viewState.value.baudRate.doubleValue.toInt(),
                        viewModel = viewModel
                    ) { selected, _ ->
                        viewModel.viewState.value.baudRate.doubleValue = selected.index.toDouble()
                    }

                    SpinnerView(
                        label = PCNUtil.PARITY,
                        options = viewModel.getAllowedValues(
                            DomainName.modbusParity,
                            viewModel.pcnDeviceModel
                        ),
                        defaultIndex = viewModel.viewState.value.parity.doubleValue.toInt(),
                        viewModel = viewModel
                    ) { selected, _ ->
                        viewModel.viewState.value.parity.doubleValue = selected.index.toDouble()
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SpinnerView(
                        label = PCNUtil.DATA_BITS,
                        options = viewModel.getAllowedValues(
                            DomainName.modbusDataBits,
                            viewModel.pcnDeviceModel
                        ),
                        defaultIndex = viewModel.viewState.value.dataBits.doubleValue.toInt(),
                        viewModel = viewModel
                    ) { selected, _ ->
                        viewModel.viewState.value.dataBits.doubleValue = selected.index.toDouble()
                    }

                    SpinnerView(
                        label = PCNUtil.STOP_BITS,
                        options = viewModel.getAllowedValues(
                            DomainName.modbusStopBits,
                            viewModel.pcnDeviceModel
                        ),
                        defaultIndex = viewModel.viewState.value.stopBits.doubleValue.toInt(),
                        viewModel = viewModel
                    ) { selected, _ ->
                        viewModel.viewState.value.stopBits.doubleValue = selected.index.toDouble()
                    }

                }
            }
        }
    }

    @Composable
    private fun ShowConnectModules() {

        Spacer(modifier = Modifier.height(8.dp))

        viewModel.getPairedConnectModules().forEachIndexed {index, connectModule ->
            if (index != 0) {
                DashDivider()
            }
            key(connectModule.serverId) {
                ModelConfigurationView(
                    isConnect = true,
                    serverId = connectModule.serverId.toString(),
                    connectModule = connectModule,
                    viewModel = viewModel,
                    fragmentManager = requireFragmentManager()
                )
            }
        }

        CcuLog.i(L.TAG_PCN, "Connect Modules completed")
    }

    @Composable
    private fun ShowExternalEquip() {

        viewModel.getPairedExternalEquips().forEachIndexed { index, externalEquip ->
            key(externalEquip.serverId) {
                if (index != 0) {
                    DashDivider()
                }
                ModuleHeader(
                    moduleTitle = PCNUIUtil.EXTERNAL_EQUIP,
                    serverId = externalEquip.serverId.toString(),
                    isEnabled = externalEquip.newConfiguration,
                    viewModel = viewModel,
                    requireFragmentManager()
                )
                ShowPairedEquip(
                    externalEquip.name,
                    null,
                    listOf(externalEquip.equipModel),
                    newConfiguration = externalEquip.newConfiguration,
                    allowDelete = false,
                    viewModel,
                    fragmentManager = requireFragmentManager()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        viewModel.isFreshDraw = false
        CcuLog.i(L.TAG_PCN, "External Equipments completed")
    }

    @Composable
    private fun ShowActionChips() {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LabelBoldTextViewForTable(
                text = "ADD CONNECT MODULE",
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        if (PCNValidation.isEligibleToAdd(viewModel.viewState.value)) {
                            viewModel.addConnectModule()
                        }
                    }
                    .padding(8.dp),
                fontSize = 22,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Start,
                fontColor = if (PCNValidation.isEligibleToAdd(viewModel.viewState.value)) primaryColor else greyDropDownColor
            )

            Spacer(modifier = Modifier.width(20.dp))

            Divider(
                color = Color.Gray,
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
            )

            Spacer(modifier = Modifier.width(20.dp))

            LabelBoldTextViewForTable(
                text = "ADD EXTERNAL EQUIP",
                modifier = Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        if (PCNValidation.isEligibleToAdd(viewModel.viewState.value)) {
                            addExternalEquip()
                        }
                    }
                    .padding(8.dp),
                fontSize = 22,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Start,
                fontColor = if (PCNValidation.isEligibleToAdd(viewModel.viewState.value)) primaryColor else greyDropDownColor
            )
        }
    }

    @Composable
    private fun FooterButtons() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (PCNValidation.isEligibleToAdd(viewModel.viewState.value)) {
                LabelBoldTextViewForTable(
                    text = "Note: A maximum of 50 integer registers can be allocated for the protocol converter.",
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    fontSize = 20,
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.Normal,
                    fontColor = colorResource(id = R.color.resp_header_title_bg)
                )
            } else {
                    Image(
                        painter = painterResource(id = R.drawable.warning_toast),
                        contentDescription = "Trailing Icon",
                        modifier = Modifier.size(22.dp)
                    )
                    LabelBoldTextViewForTable(
                        text = "50 integer registers limit reached. Remove equips to add other",
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        fontSize = 20,
                        textAlign = TextAlign.Start,
                        fontWeight = FontWeight.Normal,
                        fontColor = colorResource(id = R.color.warning_toast_red)
                    )
            }

            SaveTextViewNew("CANCEL", onClick = { closeAllBaseDialogFragments() })
            Spacer(modifier = Modifier.width(20.dp))

            Divider(
                color = Color.Gray,
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
            )

            Spacer(modifier = Modifier.width(20.dp))

            SaveTextViewNew("SAVE", onClick = {
                val serverId = PCNValidation.isServerIdsValid(viewModel.viewState.value)
                if (serverId == null) {
                    viewModel.saveConfiguration()
                } else {
                    Toast.makeText(requireContext(),"Slave Id " + serverId + " already exists, choose " +
                            "another slave id to proceed", Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun addExternalEquip() {
        viewModel.addExternalEquip(requireContext()) { isSuccess ->
            if (isSuccess){
                showModbusConfig()
            }
        }
    }

    private fun showModbusConfig() {
        val onItemSelect = object : OnItemSelect {
            override fun onItemSelected(index: Int, item: String) {
                CcuLog.i(L.TAG_PCN, "Model details: ${viewModel.equipModel.value.version}")
                viewModel.fetchModelDetails(item)
                CcuLog.i(L.TAG_PCN, "Selected item: $item at index: $index")
            }
        }

        showDialogFragment(
            ModelSelectionFragment.showUIForPCN(
                viewModel.deviceList,
                onItemSelect, SEARCH_FOR_MODEL,
                currentRegistersUsed = PCNValidation.getPairedRegisterCount(viewModel.viewState.value),
                maxRegisterCount = 50,
            ), ModelSelectionFragment.ID
        )
    }

    @Composable
    fun ShowProgressBar() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = primaryColor)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Loading Profile Configuration")
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(MATCH_PARENT, MATCH_PARENT)
    }

    override fun getIdString(): String = ModbusConfigView::class.java.simpleName

    override fun onPairingComplete() {
        this@PCNConfigView.closeAllBaseDialogFragments()
    }
}