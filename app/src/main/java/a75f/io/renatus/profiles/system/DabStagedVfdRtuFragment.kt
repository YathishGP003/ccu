package a75f.io.renatus.profiles.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

enum class CoolingStage(val index: Int) {
    STAGE_1(0),
    STAGE_2(1),
    STAGE_3(2),
    STAGE_4(3),
    STAGE_5(4)
}

enum class HeatingStage(val index: Int) {
    STAGE_1(5),
    STAGE_2(6),
    STAGE_3(7),
    STAGE_4(8),
    STAGE_5(9)
}

class DabStagedVfdRtuFragment : DStagedRtuFragment() {
    private val viewModel : DabStagedVfdRtuViewModel by viewModels()
    var viewState: MutableState<StagedRtuVfdViewState> = mutableStateOf(StagedRtuVfdViewState())

    fun hasUnsavedChanged(): Boolean{
        return viewModel.hasUnsavedChanges()
    }

    companion object {
        val ID: String = DabStagedVfdRtuFragment::class.java.simpleName
        fun newInstance() : DabStagedVfdRtuFragment {
            return DabStagedVfdRtuFragment()
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

    private fun coolingStageSelected(stage : CoolingStage): Boolean {
        return when(stage) {
            CoolingStage.STAGE_1 -> checkAssociation(CoolingStage.STAGE_1.index)
            CoolingStage.STAGE_2 -> checkAssociation(CoolingStage.STAGE_2.index)
            CoolingStage.STAGE_3 -> checkAssociation(CoolingStage.STAGE_3.index)
            CoolingStage.STAGE_4 -> checkAssociation(CoolingStage.STAGE_4.index)
            CoolingStage.STAGE_5 -> checkAssociation(CoolingStage.STAGE_5.index)
        }
    }

    private fun heatingStageSelected(stage : HeatingStage): Boolean {
        return when(stage) {
            HeatingStage.STAGE_1 -> checkAssociation(HeatingStage.STAGE_1.index)
            HeatingStage.STAGE_2 -> checkAssociation(HeatingStage.STAGE_2.index)
            HeatingStage.STAGE_3 -> checkAssociation(HeatingStage.STAGE_3.index)
            HeatingStage.STAGE_4 -> checkAssociation(HeatingStage.STAGE_4.index)
            HeatingStage.STAGE_5 -> checkAssociation(HeatingStage.STAGE_5.index)
        }
    }

    private fun checkAssociation(associationIndex : Int): Boolean {
        if((viewState.value.relay1Enabled && viewState.value.relay1Association == associationIndex)
            || (viewState.value.relay2Enabled  && viewState.value.relay2Association == associationIndex )
            || (viewState.value.relay3Enabled  && viewState.value.relay3Association == associationIndex )
            || (viewState.value.relay4Enabled  && viewState.value.relay4Association == associationIndex )
            || (viewState.value.relay5Enabled  && viewState.value.relay5Association == associationIndex )
            || (viewState.value.relay6Enabled  && viewState.value.relay6Association == associationIndex )
            || (viewState.value.relay7Enabled  && viewState.value.relay7Association == associationIndex)) {
            return true
        }
        else {
            return false
        }
    }

    @Preview
    @Composable
    fun RootView() {
        val modelLoaded by viewModel.modelLoaded.observeAsState(initial = false)
        if (!modelLoaded) {
            AddProgressGif()
            CcuLog.i(Domain.LOG_TAG, "Show Progress")
            return
        }

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
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && coolingStageSelected(CoolingStage.STAGE_1)),
                        previewWidth = 100,
                        expandedWidth = 100,
                        spacerLimit = 90)
                    Spacer(modifier = Modifier.width(97.dp))
                    DropDownWithLabel(label = "Analog-Out2\nDuring Cool Stage 2",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.analogOut2CoolStage2,
                        onSelected = {viewState.analogOut2CoolStage2 = it
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && coolingStageSelected(CoolingStage.STAGE_2)),
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
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && coolingStageSelected(CoolingStage.STAGE_3) ),
                        previewWidth = 100,
                        expandedWidth = 100,
                        spacerLimit = 90)
                    Spacer(modifier = Modifier.width(97.dp))
                    DropDownWithLabel(label = "Analog-Out2\nDuring Cool Stage 4",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.analogOut2CoolStage4,
                        onSelected = {viewState.analogOut2CoolStage5 = it
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && coolingStageSelected(CoolingStage.STAGE_4)),
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
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && coolingStageSelected(CoolingStage.STAGE_5)),
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
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && heatingStageSelected(HeatingStage.STAGE_1)),
                        previewWidth = 100,
                        expandedWidth = 100,
                        spacerLimit = 90)
                    Spacer(modifier = Modifier.width(97.dp))
                    DropDownWithLabel(label = "Analog-Out2\nDuring Heat Stage 2",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.analogOut2HeatStage2,
                        onSelected = {viewState.analogOut2HeatStage2 = it
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && heatingStageSelected(HeatingStage.STAGE_2)),
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
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && heatingStageSelected(HeatingStage.STAGE_3)),
                        previewWidth = 100,
                        expandedWidth = 100,
                        spacerLimit = 90)
                    Spacer(modifier = Modifier.width(97.dp))
                    DropDownWithLabel(label = "Analog-Out2\nDuring Heat Stage 4",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.analogOut2HeatStage4,
                        onSelected = {viewState.analogOut2HeatStage4 = it
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && heatingStageSelected(HeatingStage.STAGE_4)),
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
                            viewModel.setStateChanged()}, isEnabled = (viewState.analogOut2Enabled && heatingStageSelected(HeatingStage.STAGE_5)),
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