package a75f.io.renatus.profiles.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.util.onLoadingCompleteListener
import a75f.io.renatus.R
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.composables.SystemAnalogOutMappingViewVavStagedVfdRtu
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.modbus.util.CANCEL
import a75f.io.renatus.modbus.util.SAVE
import a75f.io.renatus.profiles.profileUtils.UnusedPortsFragment
import a75f.io.renatus.profiles.profileUtils.UnusedPortsFragment.Companion.LabelUnusedPorts
import a75f.io.renatus.profiles.profileUtils.UnusedPortsModel
import a75f.io.renatus.util.AddProgressGif
import a75f.io.renatus.util.TestSignalManager
import a75f.io.renatus.util.highPriorityDispatcher
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class VavStagedVfdRtuFragment(loadingListener: onLoadingCompleteListener) : StagedRtuFragment() {

    private val viewModel : VavStagedVfdRtuViewModel by viewModels()
    var viewState: MutableState<StagedRtuVfdViewState> = mutableStateOf(StagedRtuVfdViewState())
    private val listener : onLoadingCompleteListener = loadingListener

    fun hasUnsavedChanged(): Boolean{
        return viewModel.hasUnsavedChanges()
    }

    companion object {
        val ID: String = VavStagedVfdRtuFragment::class.java.simpleName
        fun newInstance() : VavStagedVfdRtuFragment {
            return VavStagedVfdRtuFragment({this})
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        viewLifecycleOwner.lifecycleScope.launch(highPriorityDispatcher){
                viewModel.init(requireContext(), CCUHsApi.getInstance())
                viewState.value = viewModel.viewState.value as StagedRtuVfdViewState

        }
        val rootView = ComposeView(requireContext())
        rootView.apply {
            setContent { RootView() }
            return rootView
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
            }

            override fun onViewDetachedFromWindow(v: View) {
                if (Globals.getInstance().isTestMode) {
                    Globals.getInstance().isTestMode = false
                    TestSignalManager.restoreAllPoints()
                }
            }
        })
    }

    private fun coolingStageSelected(stage : Int): Boolean {
        when(stage){
            1 ->{
                return checkAssociation(0)
            }
            2 ->{
                return checkAssociation(1)
            }
            3 ->{
                return checkAssociation(2)
            }
            4 ->{
                return checkAssociation(3)
            }
            5 ->{
                return checkAssociation(4)
            }
            else -> {
                return false
            }
        }
    }


    private fun heatingStageSelected(stage : Int): Boolean {
        when (stage) {
            1 -> {
                return checkAssociation(5)
            }

            2 -> {
                return checkAssociation(6)
            }

            3 -> {
                return checkAssociation(7)
            }

            4 -> {
                return checkAssociation(8)
            }

            5 -> {
                return checkAssociation(9)
            }

            else -> {
                return false
            }
        }
    }
    private fun checkAssociation(associationIndex : Int): Boolean {
        if((viewState.value.relay1Enabled == true && viewState.value.relay1Association == associationIndex)
            || (viewState.value.relay2Enabled == true && viewState.value.relay2Association == associationIndex )
            || (viewState.value.relay3Enabled == true && viewState.value.relay3Association == associationIndex )
            || (viewState.value.relay4Enabled == true && viewState.value.relay4Association == associationIndex )
            || (viewState.value.relay5Enabled == true && viewState.value.relay5Association == associationIndex )
            || (viewState.value.relay6Enabled == true && viewState.value.relay6Association == associationIndex )
            || (viewState.value.relay7Enabled == true && viewState.value.relay7Association == associationIndex)) {
            return true
        }
        else {
            return false
        }
    }

    @Composable
    fun RootView() {
        val modelLoaded by viewModel.modelLoaded.observeAsState(initial = false)
        if (!modelLoaded) {
            AddProgressGif()
            CcuLog.i(Domain.LOG_TAG, "Show Progress")
            return
        }
        listener.onLoadingComplete()
        val viewState = viewModel.viewState.value as StagedRtuVfdViewState
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(10.dp))
        {
            item {
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(10.dp)
            )
            {
                Image(
                    painter = painterResource(id = R.drawable.input_vavvfd),
                    contentDescription = "Relays",
                    modifier = Modifier
                        .wrapContentSize()
                        .padding(top = 80.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 20.dp),
                        ) {
                            Text(text = "ENABLE", fontSize = 20.sp, color = ComposeUtil.greyColor)
                            Spacer(modifier = Modifier.width(282.dp))
                            Text(text = "MAPPING", fontSize = 20.sp, color = ComposeUtil.greyColor)
                            Spacer(modifier = Modifier.width(172.dp))
                            Text(text = "TEST SIGNAL", fontSize = 20.sp, color = ComposeUtil.greyColor)
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        StagedRtuRelayMappingView(viewModel = viewModel)
                        Spacer(modifier = Modifier.height(15.dp))
                    SystemAnalogOutMappingViewVavStagedVfdRtu(
                        analogName = "Analog-Out 2",
                        analogOutState = viewState.analogOut2Enabled,
                        onAnalogOutEnabled = {
                            viewState.analogOut2Enabled = it
                            viewModel.setStateChanged()
                            viewModel.viewState.value.unusedPortState = UnusedPortsModel.setPortState(
                                "Analog 2 Output",
                                it,
                                viewModel.profileConfiguration
                            )
                        },
                        mappingText = "Fan Speed",
                        analogOutValList = (0..10).map { it.toString() },
                        analogOutVal = (0..10).map { it }
                            .indexOf((viewState.analogOut2FanSpeedTestSignal.toInt()) / 10),
                        onAnalogOutChanged = {
                            viewState.analogOut2FanSpeedTestSignal = it.toDouble()
                            viewModel.sendAnalogTestSignal(it.toDouble())
                        }
                    )
                }
            }
            }
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 30.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    DropDownWithLabel(label = "Analog-Out2\nDuring Economizer",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.analogOut2Economizer,
                        onSelected = {viewState.analogOut2Economizer = it
                            viewModel.setStateChanged()},
                        previewWidth = 100,
                        expandedWidth = 100,
                        spacerLimit = 90 )
                    Spacer(modifier = Modifier.width(97.dp))
                    DropDownWithLabel(label = "Analog-Out2\nDuring Recirculate",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.analogOut2Recirculate,
                        onSelected = {viewState.analogOut2Recirculate = it
                            viewModel.setStateChanged()},
                        previewWidth = 100,
                        expandedWidth = 100,
                        spacerLimit = 179)
                }
            }
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 30.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    DropDownWithLabel(label = "Analog-Out2\nDuring Cool Stage 1",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.analogOut2CoolStage1,
                        onSelected = {viewState.analogOut2CoolStage1 = it
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && coolingStageSelected(1)),
                        previewWidth = 100,
                        expandedWidth = 100,
                        spacerLimit = 90)
                    Spacer(modifier = Modifier.width(97.dp))
                    DropDownWithLabel(label = "Analog-Out2\nDuring Cool Stage 2",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.analogOut2CoolStage2,
                        onSelected = {viewState.analogOut2CoolStage2 = it
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && coolingStageSelected(2)),
                        previewWidth = 100,
                        expandedWidth = 100,
                        spacerLimit = 179)
                }
            }
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 30.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    DropDownWithLabel(label = "Analog-Out2\nDuring Cool Stage 3",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.analogOut2CoolStage3,
                        onSelected = {viewState.analogOut2CoolStage3 = it
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && coolingStageSelected(3) ),
                        previewWidth = 100,
                        expandedWidth = 100,
                        spacerLimit = 90)
                    Spacer(modifier = Modifier.width(97.dp))
                    DropDownWithLabel(label = "Analog-Out2\nDuring Cool Stage 4",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.analogOut2CoolStage4,
                        onSelected = {viewState.analogOut2CoolStage5 = it
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && coolingStageSelected(4)),
                        previewWidth = 100,
                        expandedWidth = 100,
                        spacerLimit = 179)
                }
            }
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 30.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DropDownWithLabel(label = "Analog-Out2\nDuring Cool Stage 5",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.analogOut2CoolStage5,
                        onSelected = {viewState.analogOut2CoolStage5 = it
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && coolingStageSelected(5)),
                        previewWidth = 100,
                        expandedWidth = 100,
                        spacerLimit = 90)
                }
            }
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 30.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    DropDownWithLabel(label = "Analog-Out2\nDuring Heat Stage 1",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.analogOut2HeatStage1,
                        onSelected = {viewState.analogOut2HeatStage1 = it
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && heatingStageSelected(1)),
                        previewWidth = 100,
                        expandedWidth = 100,
                        spacerLimit = 90)
                    Spacer(modifier = Modifier.width(97.dp))
                    DropDownWithLabel(label = "Analog-Out2\nDuring Heat Stage 2",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.analogOut2HeatStage2,
                        onSelected = {viewState.analogOut2HeatStage2 = it
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && heatingStageSelected(2)),
                        previewWidth = 100,
                        expandedWidth = 100,
                        spacerLimit = 179)
                }
            }
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 30.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    DropDownWithLabel(label = "Analog-Out2\nDuring Heat Stage 3",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.analogOut2HeatStage3,
                        onSelected = {viewState.analogOut2HeatStage3 = it
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && heatingStageSelected(3)),
                        previewWidth = 100,
                        expandedWidth = 100,
                        spacerLimit = 90)
                    Spacer(modifier = Modifier.width(97.dp))
                    DropDownWithLabel(label = "Analog-Out2\nDuring Heat Stage 4",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.analogOut2HeatStage4,
                        onSelected = {viewState.analogOut2HeatStage4 = it
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && heatingStageSelected(4)),
                        previewWidth = 100,
                        expandedWidth = 100,
                        spacerLimit = 179)
                }
            }
            item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 30.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DropDownWithLabel(label = "Analog-Out2\nDuring Heat Stage 5",
                            list = (0..10).map { it.toString() }, isHeader = false,
                            defaultSelection = viewState.analogOut2HeatStage5,
                            onSelected = {viewState.analogOut2HeatStage5 = it
                                viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && heatingStageSelected(5)),
                            previewWidth = 100,
                            expandedWidth = 100,
                            spacerLimit = 90)
                    }

                }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 30.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DropDownWithLabel(label = "Analog-Out2 Default",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.analogOut2Default,
                        onSelected = {viewState.analogOut2Default = it
                            viewModel.setStateChanged()},
                        previewWidth = 100,
                        expandedWidth = 100, spacerLimit = 90)
                }

            }

            if(viewModel.viewState.value.unusedPortState.isNotEmpty()) {
                item {
                    UnusedPortsFragment.DividerRow()
                }
                item {
                    LabelUnusedPorts()
                }
                item {
                    UnusedPortsFragment.UnUsedPortsListView(viewModel)
                }
            }
            item {
                SaveConfig()
            }
        }

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
            ) {
                SaveTextView(CANCEL, viewModel.viewState.value.isStateChanged) {
                    viewModel.reset()
                    viewModel.viewState.value.isSaveRequired = false
                    viewModel.viewState.value.isStateChanged = false
                }
            }
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
                SaveTextView(SAVE, viewModel.viewState.value.isSaveRequired) {
                    viewModel.saveConfiguration()
                    viewModel.viewState.value.isSaveRequired = false
                    viewModel.viewState.value.isStateChanged = false
                }
            }
        }
    }
}