package a75f.io.renatus.profiles.hyperstatv2.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.api.DomainName
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.R
import a75f.io.renatus.composables.Picker
import a75f.io.renatus.composables.RelayConfiguration
import a75f.io.renatus.composables.rememberPickerState
import a75f.io.renatus.compose.*
import a75f.io.renatus.modbus.util.SET
import a75f.io.renatus.profiles.OnPairingCompleteListener
import a75f.io.renatus.profiles.hyperstatv2.util.ConfigState
import a75f.io.renatus.profiles.hyperstatv2.viewmodels.MonitoringModel
import a75f.io.renatus.profiles.system.advancedahu.Option
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HyperStatMonitoringFragment : BaseDialogFragment(), OnPairingCompleteListener {

    private val viewModel : MonitoringModel by viewModels()
    companion object {
        val ID: String = HyperStatMonitoringFragment::class.java.simpleName

        fun newInstance(
            meshAddress: Short, roomName: String, floorName: String, nodeType : NodeType, profileType: ProfileType
        ): HyperStatMonitoringFragment {
            val fragment = HyperStatMonitoringFragment()
            val bundle = Bundle()
            bundle.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            bundle.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            bundle.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            bundle.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal)
            bundle.putInt(FragmentCommonBundleArgs.NODE_TYPE, nodeType.ordinal)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val rootView = ComposeView(requireContext())
        rootView.setContent {
            ShowProgressBar()
        }
        viewLifecycleOwner.lifecycleScope.launch(highPriorityDispatcher) {
            viewModel.init(requireArguments(), requireContext(), CCUHsApi.getInstance())
            viewModel.setOnPairingCompleteListener(this@HyperStatMonitoringFragment)
            withContext(Dispatchers.Main) {
                rootView.setContent {
                    RootView()
                }
            }
        }
        return rootView
    }

    @Composable
    fun ShowProgressBar() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(color = ComposeUtil.primaryColor)
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "Loading Profile Configuration")
        }
    }
    //@Preview
    @Composable
    fun RootView() {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            item {

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TitleTextView("MONITORING")
                }
                Spacer(modifier = Modifier.height(20.dp))
                val valuesPickerState = rememberPickerState()

                Picker(
                    header = "Temperature Offset",
                    state = valuesPickerState,
                    items = viewModel.temperatureOffset,
                    onChanged = { it: String ->
                        viewModel.viewState.value.temperatureOffset = it.toDouble()
                    },
                    startIndex = viewModel.temperatureOffset.indexOf(viewModel.viewState.value.temperatureOffset.toString()),
                    visibleItemsCount = 3,
                    textModifier = Modifier.padding(8.dp),
                    textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Image(painter = painterResource(id = R.drawable.hyperstatsenseinput),
                        contentDescription = "Relays", modifier = Modifier
                            .padding(start = 50.dp, end = 20.dp)
                            .height(475.dp))

                    Column(modifier = Modifier.fillMaxSize().padding(top = 80.dp)) {

                        repeat(4) { index ->
                            val relayConfig = when (index) {
                                0 -> viewModel.viewState.value.thermistor1Config
                                1 -> viewModel.viewState.value.thermistor2Config
                                2 -> viewModel.viewState.value.analogIn1Config
                                3 -> viewModel.viewState.value.analogIn2Config
                                else -> throw IllegalArgumentException("Invalid relay index: $index")
                            }
                            val relayEnums = if(index < 2){
                                viewModel.getAllowedValues(DomainName.thermistor1InputAssociation, viewModel.equipModel)
                            } else {
                                viewModel.getAllowedValues(DomainName.analog1InputAssociation, viewModel.equipModel)
                            }

                            if(index == 2){
                                Spacer(modifier = Modifier.height(30.dp))
                            }

                            val disName = when (index) {
                                0 -> "Thermistor 1"
                                1 -> "Thermistor 2"
                                2 -> "Analog In 1"
                                3 -> "Analog In 2"
                                else -> {""}
                            }
                            DrawRelayConfig(relayConfig, relayEnums, disName)
                        }
                    }
                }
                Row(modifier = Modifier
                    .padding(start = 50.dp, end = 20.dp)) {
                    val co2ThresholdOptions = viewModel.getOptionByDomainName(DomainName.co2Target, viewModel.equipModel, true)
                    val co2Unit = viewModel.getUnit(DomainName.co2Target, viewModel.equipModel)
                    val pm25Unit = viewModel.getUnit(DomainName.pm25Target, viewModel.equipModel)
                    val pm25ThresholdOptions = viewModel.getOptionByDomainName(DomainName.pm25Target, viewModel.equipModel, true)

                    Box(modifier = Modifier
                        .weight(1f)
                        .padding(top = 10.dp)) {
                        StyledTextView("CO2 Target", fontSize = 20, textAlignment = TextAlign.Left)
                    }
                    Box(modifier = Modifier
                        .weight(.9f)
                        .padding(top = 5.dp)) {
                        SpinnerElementOption(viewModel.viewState.value.co2Config.target.toInt().toString(), co2ThresholdOptions, co2Unit,
                            itemSelected = { viewModel.viewState.value.co2Config.target = it.value.toDouble() }, viewModel = null)
                    }

                    Box(modifier = Modifier
                        .weight(1.1f)
                        .padding(top = 10.dp)) {
                        StyledTextView("PM 2.5 Target", fontSize = 20, textAlignment = TextAlign.Left)
                    }
                    Box(modifier = Modifier
                        .weight(1f)
                        .padding(top = 5.dp)) {
                        SpinnerElementOption(viewModel.viewState.value.pm2p5Config.target.toInt().toString(), pm25ThresholdOptions, pm25Unit,
                            itemSelected = { viewModel.viewState.value.pm2p5Config.target = it.value.toDouble() }, viewModel = null)
                    }
                }
                Row(modifier = Modifier.width(620.dp)
                    .padding(top = 10.dp,start = 50.dp, end = 20.dp)) {
                    val pm10TargetOptions = viewModel.getOptionByDomainName(
                        DomainName.pm10Target,
                        viewModel.equipModel,
                        true
                    )
                    val pm10Unit = viewModel.getUnit(DomainName.pm10Target, viewModel.equipModel)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 10.dp)
                    ) {
                        StyledTextView("Pm 10 Target", fontSize = 20, textAlignment = TextAlign.Left)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 5.dp)
                    ) {
                        SpinnerElementOption(viewModel.viewState.value.pm10Config.target.toInt()
                            .toString(),
                            pm10TargetOptions,
                            pm10Unit,
                            itemSelected = {
                                viewModel.viewState.value.pm10Config.target = it.value.toDouble()
                            },
                            viewModel = null
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(PaddingValues(top = 10.dp, bottom = 10.dp, end = 10.dp)),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    SaveTextView(SET) {
                        viewModel.saveConfiguration()
                    }
                }
            }
        }
    }

    @Composable
    private fun DrawRelayConfig(relayConfig: ConfigState, relayEnums: List<Option>, disName : String) {
        RelayConfiguration(
            relayName = disName,
            enabled = relayConfig.enabled,
            onEnabledChanged = {
                relayConfig.enabled = it

            },
            association = relayEnums[relayConfig.association],
            relayEnums = relayEnums,
            unit = "",
            isEnabled = relayConfig.enabled,
            onAssociationChanged = { associationIndex ->
                relayConfig.association = associationIndex.index
            },
            testState = false,
            onTestActivated = { // No test signals for monitoring //
            },
            isTestSignalVisible = false
        )
    }

    override fun getIdString(): String {
        return ID
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = 1265
            val height = 672
            dialog.window!!.setLayout(width, height)
        }
    }

    override fun onPairingComplete() {
        this@HyperStatMonitoringFragment.closeAllBaseDialogFragments()
    }
}