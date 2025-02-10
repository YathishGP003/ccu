package a75f.io.renatus.profiles.system

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.Domain
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.util.onLoadingCompleteListener
import a75f.io.renatus.R
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.LabelTextView
import a75f.io.renatus.compose.SaveTextView
import a75f.io.renatus.modbus.util.CANCEL
import a75f.io.renatus.modbus.util.SAVE
import a75f.io.renatus.profiles.profileUtils.UnusedPortsFragment
import a75f.io.renatus.profiles.profileUtils.UnusedPortsFragment.Companion.DividerRow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class VavModulatingRtuFragment(loadingListener: onLoadingCompleteListener) : ModulatingRtuFragment() {

    private val vavModulatingViewModel: VavModulatingRtuViewModel by viewModels()
    private val listener : onLoadingCompleteListener = loadingListener

    fun hasUnsavedChanged(): Boolean{
        return vavModulatingViewModel.hasUnsavedChanges()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        viewLifecycleOwner.lifecycleScope.launch (highPriorityDispatcher) {
            vavModulatingViewModel.init(
                    requireContext(),
                    CCUHsApi.getInstance()
                )

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

    @Composable
    fun RootView() {
        val modelLoaded by vavModulatingViewModel.modelLoaded.observeAsState(initial = false)
        if (!modelLoaded) {
            AddProgressGif()
            CcuLog.i(Domain.LOG_TAG, "Show Progress")
            return

        }
        listener.onLoadingComplete()
        CcuLog.i(Domain.LOG_TAG, "Hide Progress")
        val viewState = vavModulatingViewModel.viewState
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.input_vavanalog),
                        contentDescription = "Relays",
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(top = 42.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 5.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Text(text = "ENABLE", fontSize = 20.sp, color = ComposeUtil.greyColor)
                            Spacer(modifier = Modifier.width(304.dp))
                            Text(text = "MAPPING", fontSize = 20.sp, color = ComposeUtil.greyColor)
                            Spacer(modifier = Modifier.width(189.dp))
                            Text(text = "TEST SIGNAL", fontSize = 20.sp, color = ComposeUtil.greyColor)
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                        AnalogOutAndRelayComposable(viewModel = vavModulatingViewModel)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 40.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    DropDownWithLabel(label = "Analog-Out1 at\nMin Cooling",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.value.analogOut1CoolingMin,
                        onSelected = {
                            viewState.value.analogOut1CoolingMin = it
                            vavModulatingViewModel.setStateChanged()
                        },
                        isEnabled = viewState.value.isAnalog1OutputEnabled,
                        spacerLimit = 102,
                        previewWidth = 100,
                        expandedWidth = 120)
                    Spacer(modifier = Modifier.width(130.dp))
                    DropDownWithLabel(label = "Analog-Out1 at\nMax Cooling",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.value.analogOut1CoolingMax,
                        onSelected = {
                            viewState.value.analogOut1CoolingMax = it
                            vavModulatingViewModel.setStateChanged()
                        },
                        isEnabled = viewState.value.isAnalog1OutputEnabled,
                        spacerLimit = 147,
                        previewWidth = 100,
                        expandedWidth = 120)
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 40.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    DropDownWithLabel(label = "Analog-Out2 at\nMin Static",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.value.analogOut2StaticPressureMin,
                        onSelected = {
                            viewState.value.analogOut2StaticPressureMin = it
                            vavModulatingViewModel.setStateChanged()
                        },
                        isEnabled = viewState.value.isAnalog2OutputEnabled,
                        spacerLimit = 102,
                        previewWidth = 100,
                        expandedWidth = 120)
                    Spacer(modifier = Modifier.width(130.dp))
                    DropDownWithLabel(label = "Analog-Out2 at\nMax Static",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.value.analogOut2StaticPressureMax,
                        onSelected = {
                            viewState.value.analogOut2StaticPressureMax = it
                            vavModulatingViewModel.setStateChanged()
                        },
                        isEnabled = viewState.value.isAnalog2OutputEnabled,
                        spacerLimit = 147,
                        previewWidth = 100,
                        expandedWidth = 120)
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 40.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    DropDownWithLabel(label = "Analog-Out3 at\nMin Heating",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.value.analogOut3HeatingMin,
                        onSelected = {
                            viewState.value.analogOut3HeatingMin = it
                            vavModulatingViewModel.setStateChanged()
                        },
                        isEnabled = viewState.value.isAnalog3OutputEnabled,
                        spacerLimit = 102,
                        previewWidth = 100,
                        expandedWidth = 120)
                    Spacer(modifier = Modifier.width(130.dp))
                    DropDownWithLabel(label = "Analog-Out3 at\nMax Heating",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.value.analogOut3HeatingMax,
                        onSelected = {
                            viewState.value.analogOut3HeatingMax = it
                            vavModulatingViewModel.setStateChanged()
                        },
                        isEnabled = viewState.value.isAnalog3OutputEnabled,
                        spacerLimit = 147,
                        previewWidth = 100,
                        expandedWidth = 120)
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 40.dp),
                    horizontalArrangement = Arrangement.Start
                ) {

                    DropDownWithLabel(label = "Analog-Out4 at\nMin Fresh Air",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = viewState.value.analogOut4FreshAirMin,
                        onSelected = {
                            viewState.value.analogOut4FreshAirMin = it
                            vavModulatingViewModel.setStateChanged()
                        },
                        isEnabled = viewState.value.isAnalog4OutputEnabled,
                        spacerLimit = 102,
                        previewWidth = 100,
                        expandedWidth = 120)
                    Spacer(modifier = Modifier.width(130.dp))
                    DropDownWithLabel(label = "Analog-Out4 at\nMax Fresh Air",
                        list = (0..10).map { it.toString() }, isHeader = false,
                        defaultSelection = vavModulatingViewModel.viewState.value.analogOut4FreshAirMax,
                        onSelected = {
                            viewState.value.analogOut4FreshAirMax = it
                            vavModulatingViewModel.setStateChanged()
                        },
                        isEnabled = viewState.value.isAnalog4OutputEnabled,
                        spacerLimit = 147,
                        previewWidth = 100,
                        expandedWidth = 120)
                }
            }
            if(vavModulatingViewModel.viewState.value.unusedPortState.isNotEmpty()) {
                item {
                    DividerRow()
                }
                item {
                    LabelTextView(text = "Unused ports for External mapping", widthValue = 400)
                }
                item {
                    UnusedPortsFragment.UnUsedPortsListView(vavModulatingViewModel)
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
                SaveTextView(CANCEL, vavModulatingViewModel.viewState.value.isStateChanged) {
                    vavModulatingViewModel.reset()
                    vavModulatingViewModel.viewState.value.isSaveRequired = false
                    vavModulatingViewModel.viewState.value.isStateChanged = false
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
                SaveTextView(SAVE, vavModulatingViewModel.viewState.value.isSaveRequired) {
                    vavModulatingViewModel.saveConfiguration()
                    vavModulatingViewModel.viewState.value.isSaveRequired = false
                    vavModulatingViewModel.viewState.value.isStateChanged = false
                }
            }
        }
    }
}