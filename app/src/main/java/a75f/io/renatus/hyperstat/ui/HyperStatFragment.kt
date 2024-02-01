package a75f.io.renatus.hyperstat.ui
import a75f.io.api.haystack.CCUHsApi
import a75f.io.device.HyperStat
import a75f.io.device.bacnet.addBacnetTags
import a75f.io.device.mesh.LSerial
import a75f.io.device.mesh.hyperstat.HyperStatMessageSender
import a75f.io.device.serial.MessageType
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.CpuAnalogOutAssociation
import a75f.io.logic.bo.building.hyperstat.profiles.cpu.CpuRelayAssociation
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.R
import a75f.io.renatus.hyperstat.AnalogInWidgets
import a75f.io.renatus.hyperstat.AnalogOutWidgets
import a75f.io.renatus.hyperstat.RelayWidgets
import a75f.io.renatus.hyperstat.StagedFanWidgets
import a75f.io.renatus.hyperstat.viewModels.*
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.RxjavaUtil
import a75f.io.renatus.util.extension.showErrorDialog
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.viewModels
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import a75f.io.renatus.views.CustomCCUSwitch;
/**
 * Created by Manjunath K on 15-07-2022.
 */

class HyperStatFragment : BaseDialogFragment() {
    private var adapterAnalogOutMapping: AnalogOutAdapter? = null
    private val disposables = CompositeDisposable()
    private val configurationDisposable = CompositeDisposable()

    private lateinit var  viewModel: HyperStatModel
    private val meshAddress: Short
        get() = requireArguments().getShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR)
    private val roomName: String
        get() = requireArguments().getString(FragmentCommonBundleArgs.ARG_NAME)!!
    private val floorName: String
        get() = requireArguments().getString(FragmentCommonBundleArgs.FLOOR_NAME)!!
    private val nodeType: NodeType
        get() = NodeType.valueOf(requireArguments().getString(FragmentCommonBundleArgs.NODE_TYPE)!!)
    private val profileType: ProfileType
        get() = ProfileType.values()[requireArguments().getInt(FragmentCommonBundleArgs.PROFILE_TYPE)]

    private lateinit var profileName: TextView
    lateinit var th2Label: TextView

    lateinit var tempOffsetSelector: NumberPicker
    lateinit var forceOccupiedSwitch: CustomCCUSwitch
    lateinit var autoAwaySwitch: CustomCCUSwitch

    // 6 rows, 1 for each relay
    private lateinit var relayUIs: List<RelayWidgets>

    // 3 rows, 1 for each analog out, plus damper voltage selectors
    private lateinit var analogOutUIs: List<AnalogOutWidgets>

    lateinit var airflowSensorSwitch: CustomCCUSwitch
    lateinit var th2Switch: CustomCCUSwitch

    // 2 rows, 1 for each analog in.
    lateinit var analogInUIs: List<AnalogInWidgets>

    private lateinit var stagedFanUIs: List<StagedFanWidgets>

    lateinit var setButton: Button
    lateinit var zoneCO2Layout: View
    private lateinit var tvZoneCO2Threshold: TextView
    private lateinit var llCO2Threshold:LinearLayout
    private lateinit var zoneCO2DamperOpeningRate: Spinner
    private lateinit var zoneCO2Threshold: Spinner
    private lateinit var zoneCO2Target: Spinner
    private lateinit var tvZoneCO2DamperOpeningRate: TextView

    lateinit var zoneVOCThreshold: Spinner
    lateinit var zoneVOCTarget: Spinner
    lateinit var zonePMThreshold: Spinner
    lateinit var zonePMTarget: Spinner

    lateinit var displayHumidity: CustomCCUSwitch
    lateinit var displayVOC: CustomCCUSwitch
    lateinit var displayPp2p5: CustomCCUSwitch
    lateinit var displayCo2: CustomCCUSwitch


    /**
     * Test Signal Buttons
     */
    lateinit var relay1Test: ToggleButton
    lateinit var relay2Test: ToggleButton
    lateinit var relay3Test: ToggleButton
    lateinit var relay4Test: ToggleButton
    lateinit var relay5Test: ToggleButton
    lateinit var relay6Test: ToggleButton
    lateinit var analogOut1Test: Spinner
    lateinit var analogOut2Test: Spinner
    lateinit var analogOut3Test: Spinner
    companion object {
        const val ID = "HyperStatFragment"

        @JvmStatic
        fun newInstance(
            meshAddress: Short, roomName: String, floorName: String, nodeType: NodeType,
            profileType: ProfileType
        ): HyperStatFragment {
            val args = Bundle()

            args.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            args.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            args.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            args.putString(FragmentCommonBundleArgs.NODE_TYPE, nodeType.toString())
            args.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal)

            val fragment = HyperStatFragment()
            fragment.arguments = args
            return fragment
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_hyperstat_cpu_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        when(profileType){
            ProfileType.HYPERSTAT_CONVENTIONAL_PACKAGE_UNIT-> {
                val cpuViewModel: CpuViewModel by viewModels()
                viewModel = cpuViewModel
                viewModel.initData(meshAddress, roomName, floorName, nodeType, profileType)

            }
            ProfileType.HYPERSTAT_TWO_PIPE_FCU->{
                val pipe2ViewModel: Pipe2ViewModel by viewModels()
                viewModel = pipe2ViewModel
                viewModel.initData(meshAddress, roomName, floorName, nodeType, profileType)

            }ProfileType.HYPERSTAT_HEAT_PUMP_UNIT->{
                val hpuViewModel: HpuViewModel by viewModels()
                viewModel = hpuViewModel
                viewModel.initData(meshAddress, roomName, floorName, nodeType, profileType)

            }
            else -> {}
        }

        bindViews()
        setUpSpinners()
        setUpViewListeners()

        disposables.add(
            viewModel.getState()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ viewState -> render(viewState) },
                    { error -> handleError(error) })
        )
        if (!viewModel.isProfileConfigured()) {
            disableTestUI()
        }


    }

    override fun onStart() {
        super.onStart()
        // todo: Found this code in other profile Fragments.
        val dialog = dialog
        if (dialog != null) {
            val width = 1165 //ViewGroup.LayoutParams.WRAP_CONTENT;
            val height = 720 //ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.window?.setLayout(width, height)
        }
    }

    // Used by BaseDialogFragment for navigation
    override fun getIdString(): String {
        return ID
    }

    /**
     * Just initialising the view
     */
    private fun bindViews() {
        with(requireView()) {

            tempOffsetSelector = findViewById(R.id.temperatureOffset)
            forceOccupiedSwitch = findViewById(R.id.toggleForceOccupied)
            autoAwaySwitch = findViewById(R.id.toggleAutoAway)

            relayUIs = listOf(
                RelayWidgets(findViewById(R.id.toggleRelay1), findViewById(R.id.relay1Spinner)),
                RelayWidgets(findViewById(R.id.toggleRelay2), findViewById(R.id.relay2Spinner)),
                RelayWidgets(findViewById(R.id.toggleRelay3), findViewById(R.id.relay3Spinner)),
                RelayWidgets(findViewById(R.id.toggleRelay4), findViewById(R.id.relay4Spinner)),
                RelayWidgets(findViewById(R.id.toggleRelay5), findViewById(R.id.relay5Spinner)),
                RelayWidgets(findViewById(R.id.toggleRelay6), findViewById(R.id.relay6Spinner))
            )

            analogOutUIs = listOf(
                AnalogOutWidgets(
                    findViewById(R.id.toggleAnalog1),
                    findViewById(R.id.analog1Spinner),
                    findViewById(R.id.ao1MinDamperLabel),
                    findViewById(R.id.ao1MinDamperSpinner),
                    findViewById(R.id.ao1MaxDamperLabel),
                    findViewById(R.id.ao1MaxDamperSpinner),
                    findViewById(R.id.ao1FanConfig),
                    findViewById(R.id.ao1AtFanLowSpinner),
                    findViewById(R.id.ao1AtFanMediumSpinner),
                    findViewById(R.id.ao1AtFanHighSpinner)
                ),
                AnalogOutWidgets(
                    findViewById(R.id.toggleAnalog2),
                    findViewById(R.id.analog2Spinner),
                    findViewById(R.id.ao2MinDamperLabel),
                    findViewById(R.id.ao2MinDamperSpinner),
                    findViewById(R.id.ao2MaxDamperLabel),
                    findViewById(R.id.ao2MaxDamperSpinner),
                    findViewById(R.id.ao2FanConfig),
                    findViewById(R.id.ao2AtFanLowSpinner),
                    findViewById(R.id.ao2AtFanMediumSpinner),
                    findViewById(R.id.ao2AtFanHighSpinner)
                ),
                AnalogOutWidgets(
                    findViewById(R.id.toggleAnalog3),
                    findViewById(R.id.analog3Spinner),
                    findViewById(R.id.ao3MinDamperLabel),
                    findViewById(R.id.ao3MinDamperSpinner),
                    findViewById(R.id.ao3MaxDamperLabel),
                    findViewById(R.id.ao3MaxDamperSpinner),
                    findViewById(R.id.ao3FanConfig),
                    findViewById(R.id.ao3AtFanLowSpinner),
                    findViewById(R.id.ao3AtFanMediumSpinner),
                    findViewById(R.id.ao3AtFanHighSpinner)
                )
            )

            airflowSensorSwitch = findViewById(R.id.airflowTempToggle)
            th2Switch = findViewById(R.id.doorWindowEnableToggle)

            displayHumidity = findViewById(R.id.humidity)
            displayCo2 = findViewById(R.id.co2)
            displayVOC = findViewById(R.id.voc)
            displayPp2p5 = findViewById(R.id.p2pm)


            configUIOnProfile(this)

            analogInUIs = listOf(
                AnalogInWidgets(findViewById(R.id.toggle_analog_in1), findViewById(R.id.analog1_in_spinner)),
                AnalogInWidgets(findViewById(R.id.toggle_analog_in2), findViewById(R.id.analog2_in_spinner))
            )

            stagedFanUIs = listOf(
                StagedFanWidgets(findViewById(R.id.fanOutCoolingStage1Label), findViewById(R.id.fanOutCoolingStage1Spinner)),
                StagedFanWidgets(findViewById(R.id.fanOutCoolingStage2Label), findViewById(R.id.fanOutCoolingStage2Spinner)),
                StagedFanWidgets(findViewById(R.id.fanOutCoolingStage3Label), findViewById(R.id.fanOutCoolingStage3Spinner)),
                StagedFanWidgets(findViewById(R.id.fanOutHeatingStage1Label), findViewById(R.id.fanOutHeatingStage1Spinner)),
                StagedFanWidgets(findViewById(R.id.fanOutHeatingStage2Label), findViewById(R.id.fanOutHeatingStage2Spinner)),
                StagedFanWidgets(findViewById(R.id.fanOutHeatingStage3Label), findViewById(R.id.fanOutHeatingStage3Spinner)),

            )

            zoneCO2Layout = findViewById(R.id.dcvCo2Config)
            zoneCO2DamperOpeningRate = findViewById(R.id.zoneCO2DamperOpeningRateSpinner)
            tvZoneCO2Threshold=findViewById(R.id.zoneCO2Threshold)
            llCO2Threshold=findViewById(R.id.lLC02Threshold)
            zoneCO2Threshold = findViewById(R.id.zoneCO2ThresholdSpinner)
            zoneCO2Target = findViewById(R.id.zoneCO2TargetSpinner)
            tvZoneCO2DamperOpeningRate = findViewById(R.id.zoneCO2DamperOpeningRate)
            zoneVOCThreshold = findViewById(R.id.zoneVocThresholdSpinner)
            zoneVOCTarget = findViewById(R.id.zoneVocTargetSpinner)
            zonePMThreshold = findViewById(R.id.zonepmThresholdSpinner)
            zonePMTarget = findViewById(R.id.zonepmTargetSpinner)

            setButton = findViewById(R.id.setButton)

            /**
             * Initialise Test view
             */
            relay1Test = findViewById(R.id.relay1Test)
            relay2Test = findViewById(R.id.relay2Test)
            relay3Test = findViewById(R.id.relay3Test)
            relay4Test = findViewById(R.id.relay4Test)
            relay5Test = findViewById(R.id.relay5Test)
            relay6Test = findViewById(R.id.relay6Test)

            analogOut1Test = findViewById(R.id.analog1TestSpinner)
            analogOut2Test = findViewById(R.id.analog2TestSpinner)
            analogOut3Test = findViewById(R.id.analog3TestSpinner)


            val adapterTestSignal: ArrayAdapter<*> = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                0.rangeTo(100).toList()
            )

            relay1Test.setOnCheckedChangeListener { _, _ -> sendControl() }
            relay2Test.setOnCheckedChangeListener { _, _ -> sendControl() }
            relay3Test.setOnCheckedChangeListener { _, _ -> sendControl() }
            relay4Test.setOnCheckedChangeListener { _, _ -> sendControl() }
            relay5Test.setOnCheckedChangeListener { _, _ -> sendControl() }
            relay6Test.setOnCheckedChangeListener { _, _ -> sendControl() }

            analogOut1Test.adapter = adapterTestSignal
            analogOut2Test.adapter = adapterTestSignal
            analogOut3Test.adapter = adapterTestSignal

            analogOut1Test.setSelection(0,false)
            analogOut2Test.setSelection(0,false)
            analogOut3Test.setSelection(0,false)

             val spinnerSelectionListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    sendControl()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
            analogOut1Test.onItemSelectedListener = spinnerSelectionListener
            analogOut2Test.onItemSelectedListener = spinnerSelectionListener
            analogOut3Test.onItemSelectedListener = spinnerSelectionListener

        }
    }

    private fun disableTestUI() {
        relay1Test.isEnabled = false
        relay2Test.isEnabled = false
        relay3Test.isEnabled = false
        relay4Test.isEnabled = false
        relay5Test.isEnabled = false
        relay6Test.isEnabled = false
        analogOut1Test.isEnabled = false
        analogOut2Test.isEnabled = false
        analogOut3Test.isEnabled = false
    }

    /**
     * Setting all the spinner values
     */
    private fun setUpSpinners() {

        tempOffsetSelector.descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
        val tempOffsetValues = tempOffsetSpinnerValues()
        tempOffsetSelector.displayedValues = tempOffsetValues
        tempOffsetSelector.minValue = 0
        tempOffsetSelector.maxValue = tempOffsetValues.size - 1
        tempOffsetSelector.wrapSelectorWheel = false
        // We want to set text size, but cannot do with our NumberPicker until API 29
        //val pickerTextSize = resources.getDimensionPixelSize(R.dimen.text_numberpicker_hyperstat)


        val co2Adapter = getAdapterValue(co2DCVDamperValue())
        val co2OpeningAdapter = getAdapterValue(co2DCVOpeningDamperValue())
        val vocAdapter = getAdapterValue(vocValues())
        val pmAdapter = getAdapterValue(pmValues())

        zoneCO2DamperOpeningRate.adapter = co2OpeningAdapter
        zoneCO2Threshold.adapter = co2Adapter
        zoneCO2Target.adapter = co2Adapter

        zoneVOCThreshold.adapter = vocAdapter
        zoneVOCTarget.adapter = vocAdapter

        zonePMThreshold.adapter = pmAdapter
        zonePMTarget.adapter = pmAdapter


        // set default values
        zoneVOCThreshold.setSelection(zoneVOCThreshold.adapter.count -1)
        zoneVOCTarget.setSelection(zoneVOCTarget.adapter.count -1)

        zonePMThreshold.setSelection(zonePMThreshold.adapter.count -1)
        zonePMTarget.setSelection(zonePMTarget.adapter.count -1)


        zoneCO2Threshold.setSelection(zoneCO2Threshold.adapter.count -1)
        zoneCO2Target.setSelection(zoneCO2Target.adapter.count -1)
        analogOutUIs.forEach {
            val minMaxAdapter = getAdapterValue(analogVoltageAtSpinnerValues())
            it.vAtMinDamperSelector.adapter = minMaxAdapter
            it.vAtMaxDamperSelector.adapter = minMaxAdapter
            val adapter = getAdapterValue(analogFanLevelSpeedValue())
            it.analogOutAtFanLow.adapter = adapter
            it.analogOutAtFanMedium.adapter = adapter
            it.analogOutAtFanHigh.adapter = adapter
        }

        if (isViewModelCPUViewModel(viewModel)) {
            stagedFanUIs.forEachIndexed { index, stagedFanState ->
                stagedFanState.selector.adapter = getAdapterValue(analogVoltageAtSpinnerValues())
                val spinner = stagedFanState.selector
                if (index == CpuRelayAssociation.COOLING_STAGE_1.ordinal || index == CpuRelayAssociation.HEATING_STAGE_1.ordinal) {
                    spinner.setSelection(analogVoltageAtSpinnerValues().indexOf("7V"))
                } else {
                    spinner.setSelection(analogVoltageAtSpinnerValues().indexOf("10V"))
                }
            }
        }
    }

    private var pendingTempOffsetChange : Boolean = true
    private var pendingForceOccupiedChange : Boolean = true
    private var pendingAutoAwayChange : Boolean = true
    private var pendingRelayChange : Boolean = true
    private var pendingAnalogOutChange : Boolean = true
    private var pendingSwitchChange : Boolean = true
    private var pendingCo2ConfigChange : Boolean = true
    private var pendingVocConfigChange : Boolean = true
    private var pendingPmConfigChange : Boolean = true
    private var pendingDisplayChange : Boolean = true

    @SuppressLint("SuspiciousIndentation")
    private fun setUpViewListeners() {
        tempOffsetSelector.setOnValueChangedListener { _, _, newVal ->
            viewModel.tempOffsetSelected(newVal)
            pendingTempOffsetChange = true
        }

        forceOccupiedSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.enableForceOccupiedSwitchChanged(isChecked)
            pendingForceOccupiedChange = true
        }
        autoAwaySwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.enableAutoAwaySwitchChanged(isChecked)
            pendingAutoAwayChange = true
        }

        relayUIs.forEachIndexed { index, widgets ->
            widgets.switch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.relaySwitchChanged(index, isChecked)
                pendingRelayChange = true
            }
            widgets.selector.setOnItemSelected {
                    position -> viewModel.relayMappingSelected(index, position)
                pendingRelayChange = true
            }
        }
        analogOutUIs.forEachIndexed { index, widgets ->
            widgets.switch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.analogOutSwitchChanged(index, isChecked)
                pendingAnalogOutChange = true
            }
            widgets.selector.setOnItemSelected { position ->
                viewModel.analogOutMappingSelected(
                    index,
                    position
                )
                pendingAnalogOutChange = true
            }

            widgets.vAtMinDamperSelector.setOnItemSelected { position ->
                viewModel.voltageAtDamperSelected(
                    true, index, position
                )
                pendingAnalogOutChange = true
            }
            widgets.vAtMaxDamperSelector.setOnItemSelected { position ->
                viewModel.voltageAtDamperSelected(
                    false, index, position
                )
                pendingAnalogOutChange = true
            }
            widgets.analogOutAtFanLow.setOnItemSelected { position ->
                viewModel.updateFanConfigSelected(1, index, position)
            }

            widgets.analogOutAtFanMedium.setOnItemSelected { position ->
                viewModel.updateFanConfigSelected(2, index, position)
            }

            widgets.analogOutAtFanHigh.setOnItemSelected { position ->
                viewModel.updateFanConfigSelected(3, index, position)
            }


        }
        airflowSensorSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.airflowTempSensorSwitchChanged(isChecked)
            pendingSwitchChange = true
        }
        th2Switch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.th2SwitchChanged(isChecked)
            pendingSwitchChange = true
        }
        analogInUIs.forEachIndexed { index, widgets ->
            widgets.switch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.analogInSwitchChanged(index, isChecked)
                pendingSwitchChange = true
            }
            widgets.selector.setOnItemSelected { position -> viewModel.analogInMappingSelected(index, position) }
        }

        zoneCO2DamperOpeningRate.setOnItemSelected {
                position -> viewModel.zoneCO2DamperOpeningRateSelect(position)
                pendingCo2ConfigChange = true
        }
        zoneCO2Threshold.setOnItemSelected {
                position -> viewModel.zoneCO2ThresholdSelect(position)
                pendingCo2ConfigChange = true
        }
        zoneCO2Target.setOnItemSelected {
                position -> viewModel.zoneCO2TargetSelect(position)
                pendingCo2ConfigChange = true
        }

        zoneVOCThreshold.setOnItemSelected {
                position ->viewModel.zoneVOCThresholdSelect(position)
                pendingVocConfigChange = true
        }
        zoneVOCTarget.setOnItemSelected {
                position ->viewModel.zoneVOCTargetSelect(position)
                pendingVocConfigChange = true
        }
        zonePMThreshold.setOnItemSelected {
                position ->viewModel.zonePmThresholdSelect(position)
                pendingPmConfigChange = true
        }
        zonePMTarget.setOnItemSelected {
                position ->viewModel.zonePmTargetSelect(position)
                pendingPmConfigChange = true
        }

        stagedFanUIs.forEachIndexed { index, stagedFanWidgets ->
            stagedFanWidgets.selector.setOnItemSelected {
                position -> viewModel.voltageAtStagedFanSelected(index, position)
            }
        }

        displayHumidity.setOnCheckedChangeListener { _, isChecked ->
            if(enableDisplay(displayHumidity))
                viewModel.onDisplayHumiditySelected(isChecked)
                pendingDisplayChange = true
        }
        displayCo2.setOnCheckedChangeListener { _, isChecked ->
            if(enableDisplay(displayCo2))
                viewModel.onDisplayCo2Selected(isChecked)
                pendingDisplayChange = true

        }
        displayVOC.setOnCheckedChangeListener { _, isChecked ->
            if(enableDisplay(displayVOC))
                viewModel.onDisplayVocSelected(isChecked)
                pendingDisplayChange = true
        }
        displayPp2p5.setOnCheckedChangeListener { _, isChecked ->
            if(enableDisplay(displayPp2p5))
                viewModel.onDisplayP2pmSelected(isChecked)
                pendingDisplayChange = true
        }

        // On Click save the CPU configuration
        setButton.setOnClickListener {
            if (!viewModel.validateProfileConfig()) {
                Toast.makeText(context, viewModel.getValidationMessage() , Toast.LENGTH_LONG).show()
            } else {
                setButton.isEnabled = false
                configurationDisposable.add(RxjavaUtil.executeBackgroundTaskWithDisposable(
                    {
                        ProgressDialogUtils.showProgressDialog(
                            activity,
                            "Saving HyperStat Configuration"
                        )
                    }, {
                        CCUHsApi.getInstance().resetCcuReady()
                        viewModel.setConfigSelected()
                        CCUHsApi.getInstance().setCcuReady()
                        LSerial.getInstance().sendHyperStatSeedMessage(
                            this.meshAddress, roomName, floorName, false
                        )
                    }, {
                        addBacnetTags(requireContext(), floorName, roomName)
                        ProgressDialogUtils.hideProgressDialog()
                        closeAllBaseDialogFragments()
                        activity?.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                    }

                ))
            }
        }
    }

    private fun handleError(error: Throwable) {
        showErrorDialog(error::class.java.simpleName + " : " + error.localizedMessage)
    }
    private fun renderTempOffset(viewState: ViewState) {
        tempOffsetSelector.value = viewState.tempOffsetPosition
        pendingTempOffsetChange = false
    }
    private fun renderForceOccupiedSwitch(viewState: ViewState) {
        forceOccupiedSwitch.isChecked = viewState.forceOccupiedEnabled
        pendingForceOccupiedChange = false
    }
    private fun renderAutoAwaySwitch(viewState: ViewState) {
        autoAwaySwitch.isChecked = viewState.autoAwayEnabled
        pendingAutoAwayChange = false
    }
    private fun renderCo2Config(viewState: ViewState) {
        zoneCO2DamperOpeningRate.setSelection(viewState.zoneCO2DamperOpeningRatePos)
        zoneCO2Threshold.setSelection(viewState.zoneCO2ThresholdPos)
        zoneCO2Target.setSelection(viewState.zoneCO2TargetPos)

        pendingCo2ConfigChange = false
    }
    private fun renderVocConfig(viewState: ViewState) {
        zoneVOCThreshold.setSelection(viewState.zoneVocThresholdPos)
        zoneVOCTarget.setSelection(viewState.zoneVocTargetPos)

        pendingVocConfigChange = false
    }
    private fun renderPmConfig(viewState: ViewState) {
        zonePMThreshold.setSelection(viewState.zonePm2p5ThresholdPos)
        zonePMTarget.setSelection(viewState.zonePm2p5TargetPos)

        pendingPmConfigChange = false
    }
    private fun renderDisplayEnabled(viewState: ViewState) {
        displayHumidity.isChecked = viewState.isDisplayHumidityEnabled
        displayCo2.isChecked = viewState.isDisplayCo2Enabled
        displayVOC.isChecked = viewState.isDisplayVOCEnabled
        displayPp2p5.isChecked = viewState.isDisplayPp2p5Enabled

        pendingDisplayChange = false
    }
    @SuppressLint("ResourceType")
    private fun renderRelayAndAnalogOutEnabled(viewState: ViewState) {
        var isCoolingStage1Enabled = false
        var isCoolingStage2Enabled = false
        var isCoolingStage3Enabled = false
        var isHeatingStage1Enabled = false
        var isHeatingStage2Enabled = false
        var isHeatingStage3Enabled = false
        var isStagedFanEnabled = false
        var isDampSelected = false

        viewState.analogOutUis.forEachIndexed { index, analogOutState ->
            with(analogOutUIs[index]) {
                switch.isChecked = analogOutState.enabled
                selector.isEnabled = analogOutState.enabled
                selector.setSelection(analogOutState.association)
                vAtMinDamperLabel.text = String.format(
                    "%s%d at \nMin %s",
                    getString(R.string.hyperstat_analog_out),
                    index + 1,
                    getString(getAnalogOutDisplayName(profileType,analogOutState.association))
                )

                vAtMaxDamperLabel.text = String.format(
                    "%s%d at \nMax %s",
                    getString(R.string.hyperstat_analog_out),
                    index + 1,
                    getString(getAnalogOutDisplayName(profileType,analogOutState.association))
                )

                if (isViewModelCPUViewModel(viewModel)) {
                    if (analogOutState.enabled && analogOutState.association == CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal) {
                        vAtMinDamperLabel.visibility = View.GONE
                        vAtMinDamperSelector.visibility = View.GONE
                        vAtMinDamperSelector.setSelection(analogVoltageIndexFromValue(analogOutState.voltageAtMin))

                        vAtMaxDamperLabel.visibility = View.GONE
                        vAtMaxDamperSelector.visibility = View.GONE
                        vAtMaxDamperSelector.setSelection(analogVoltageIndexFromValue(analogOutState.voltageAtMax))
                        isStagedFanEnabled = true
                    } else {
                        vAtMinDamperLabel.visibility = if(analogOutState.enabled) View.VISIBLE else View.GONE
                        vAtMinDamperSelector.visibility = if(analogOutState.enabled) View.VISIBLE else View.GONE
                        vAtMinDamperSelector.setSelection(analogVoltageIndexFromValue(analogOutState.voltageAtMin))

                        vAtMaxDamperLabel.visibility = if(analogOutState.enabled) View.VISIBLE else View.GONE
                        vAtMaxDamperSelector.visibility = if(analogOutState.enabled) View.VISIBLE else View.GONE
                        vAtMaxDamperSelector.setSelection(analogVoltageIndexFromValue(analogOutState.voltageAtMax))
                    }
                } else {

                    vAtMinDamperLabel.isEnabled = analogOutState.enabled
                    vAtMinDamperSelector.isEnabled = analogOutState.enabled
                    vAtMinDamperSelector.setSelection(analogVoltageIndexFromValue(analogOutState.voltageAtMin))

                    vAtMaxDamperLabel.isEnabled = analogOutState.enabled
                    vAtMaxDamperSelector.isEnabled = analogOutState.enabled
                    vAtMaxDamperSelector.setSelection(analogVoltageIndexFromValue(analogOutState.voltageAtMax))
                }

                analogOutFanConfig.visibility =
                    if (analogOutState.enabled && (analogOutState.association == CpuAnalogOutAssociation.MODULATING_FAN_SPEED.ordinal ||
                                analogOutState.association == CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal)) View.VISIBLE else View.GONE

                analogOutAtFanLow.setSelection(analogFanSpeedIndexFromValue(analogOutState.perAtFanLow))
                analogOutAtFanMedium.setSelection(analogFanSpeedIndexFromValue(analogOutState.perAtFanMedium))
                analogOutAtFanHigh.setSelection(analogFanSpeedIndexFromValue(analogOutState.perAtFanHigh))

                if (!isDampSelected && analogOutState.enabled)
                    isDampSelected = viewModel.isDamperSelected(analogOutState.association)

            }
            if (isViewModelCPUViewModel(viewModel)) {
                stagedFanUIs.forEachIndexed { index, stagedFanWidgets ->
                    with(stagedFanWidgets) {
                        selector.setSelection(viewState.stagedFanUis[index])
                    }
                }

                if (isNoFanStageEnabled()) {
                    (adapterAnalogOutMapping as AnalogOutAdapter).setItemEnabled(4,false)
                    analogOutUIs.forEach {
                        if (it.selector.selectedItemPosition == CpuAnalogOutAssociation.PREDEFINED_FAN_SPEED.ordinal) it.selector.setSelection(1)
                    }
                } else {
                    (adapterAnalogOutMapping as AnalogOutAdapter).setItemEnabled(4,true)
                }
            }
            zoneCO2DamperOpeningRate.visibility = if (isDampSelected) View.VISIBLE else View.GONE
            tvZoneCO2DamperOpeningRate.visibility = if (isDampSelected) View.VISIBLE else View.GONE
            //Linear layout of C02Threshold
            val paramsLlCO2Threshold = llCO2Threshold.layoutParams as LinearLayout.LayoutParams
            //TextView of C02Threshold
            val paramsTvCO2Threshold = tvZoneCO2Threshold.layoutParams as LinearLayout.LayoutParams

            if (isDampSelected) {
                // If isDampSelected is true, set the margin to start: 10dp, end: 10dp
                paramsLlCO2Threshold.setMargins(resources.getDimensionPixelSize(R.dimen.start_margin), 0, resources.getDimensionPixelSize(R.dimen.end_margin), 0)
                // If isDampSelected is true, set the margin end: -48dp
                paramsTvCO2Threshold.setMargins(0,0,-48,0)
            } else {
                // Otherwise, set the margin to end: 250dp
                paramsLlCO2Threshold.setMargins(0, 0, resources.getDimensionPixelSize(R.dimen.large_end_margin), 0)
                // If isDampSelected is true, set the margin end: 25dp
                paramsTvCO2Threshold.setMargins(0,0,25,0)
            }
            //set the margins
            llCO2Threshold.layoutParams = paramsLlCO2Threshold
            tvZoneCO2Threshold.layoutParams=paramsTvCO2Threshold
        }


        viewState.relays.forEachIndexed { index, relayState ->
            with(relayUIs[index]) {
                switch.isChecked = relayState.enabled
                selector.isEnabled = relayState.enabled
                selector.setSelection(relayState.association)

                if (relayState.enabled) {
                    when (relayState.association) {
                        CpuRelayAssociation.COOLING_STAGE_1.ordinal -> isCoolingStage1Enabled = true
                        CpuRelayAssociation.COOLING_STAGE_2.ordinal -> isCoolingStage2Enabled = true
                        CpuRelayAssociation.COOLING_STAGE_3.ordinal -> isCoolingStage3Enabled = true
                        CpuRelayAssociation.HEATING_STAGE_1.ordinal -> isHeatingStage1Enabled = true
                        CpuRelayAssociation.HEATING_STAGE_2.ordinal -> isHeatingStage2Enabled = true
                        CpuRelayAssociation.HEATING_STAGE_3.ordinal -> isHeatingStage3Enabled = true
                    }
                }
                if (isViewModelCPUViewModel(viewModel)) {
                    makeStagedFanVisible(
                        isCoolingStage1Enabled,
                        isCoolingStage2Enabled,
                        isCoolingStage3Enabled,
                        isHeatingStage1Enabled,
                        isHeatingStage2Enabled,
                        isHeatingStage3Enabled,
                        isStagedFanEnabled
                    )
                } else {
                    makeStagedFanVisible(
                        isCoolingStage1Enabled = false,
                        isCoolingStage2Enabled = false,
                        isCoolingStage3Enabled = false,
                        isHeatingStage1Enabled = false,
                        isHeatingStage2Enabled = false,
                        isHeatingStage3Enabled = false,
                        false
                    )
                }
            }
        }
        pendingAnalogOutChange = false
        pendingRelayChange = false
    }
    private fun renderSwitch(viewState: ViewState) {
        airflowSensorSwitch.isChecked = viewState.airflowTempSensorEnabled
        if(viewModel is Pipe2ViewModel) {
            th2Switch.isChecked = true
            th2Switch.isEnabled = false
        }else{
            th2Switch.isChecked = viewState.th2Enabled
        }
        viewState.analogIns.forEachIndexed { index, analogInState ->
            with(analogInUIs[index]) {
                switch.isChecked = analogInState.enabled
                selector.isEnabled = analogInState.enabled
                selector.setSelection(analogInState.association)
            }
        }
        pendingSwitchChange = false
    }

    /**
     * This rendor method will be called in UI change
     */
    private fun render(viewState: ViewState) {

        if (pendingTempOffsetChange) {
            renderTempOffset(viewState)
        }
        if (pendingForceOccupiedChange) {
            renderForceOccupiedSwitch(viewState)
        }
        if (pendingAutoAwayChange) {
            renderAutoAwaySwitch(viewState)
        }
        if (pendingRelayChange || pendingAnalogOutChange) {
            renderRelayAndAnalogOutEnabled(viewState)
        }
        if (pendingSwitchChange) {
            renderSwitch(viewState)
        }
        if (pendingCo2ConfigChange) {
            renderCo2Config(viewState)
        }
        if (pendingVocConfigChange) {
            renderVocConfig(viewState)
        }
        if (pendingPmConfigChange) {
            renderPmConfig(viewState)
        }
        if (pendingDisplayChange) {
            renderDisplayEnabled(viewState)
        }
    }

    private fun isNoFanStageEnabled(): Boolean {
        relayUIs.forEach {
            if (it.switch.isChecked && (it.selector.selectedItemPosition == CpuRelayAssociation.COOLING_STAGE_1.ordinal ||
                        it.selector.selectedItemPosition == CpuRelayAssociation.COOLING_STAGE_2.ordinal ||
                        it.selector.selectedItemPosition == CpuRelayAssociation.COOLING_STAGE_3.ordinal ||
                        it.selector.selectedItemPosition == CpuRelayAssociation.HEATING_STAGE_1.ordinal ||
                        it.selector.selectedItemPosition == CpuRelayAssociation.HEATING_STAGE_2.ordinal ||
                        it.selector.selectedItemPosition == CpuRelayAssociation.HEATING_STAGE_3.ordinal )) {
                return false
            }
        }
        return true
    }

    private fun isViewModelCPUViewModel(viewModel: HyperStatModel): Boolean {
        return viewModel is CpuViewModel
    }

    private fun makeStagedFanVisible(
        isCoolingStage1Enabled: Boolean,
        isCoolingStage2Enabled: Boolean,
        isCoolingStage3Enabled: Boolean,
        isHeatingStage1Enabled: Boolean,
        isHeatingStage2Enabled: Boolean,
        isHeatingStage3Enabled: Boolean,
        isStagedFanEnabled: Boolean
    ) {
        if (isStagedFanEnabled) {
            stagedFanUIs.forEachIndexed { index, stagedFanState ->
                val isVisible: Boolean = when (index) {
                    0 -> isCoolingStage1Enabled
                    1 -> isCoolingStage2Enabled
                    2 -> isCoolingStage3Enabled
                    3 -> isHeatingStage1Enabled
                    4 -> isHeatingStage2Enabled
                    5 -> isHeatingStage3Enabled
                    else -> false
                }
                stagedFanState.stagedFanLabel.visibility =
                    if (isVisible) View.VISIBLE else View.GONE
                stagedFanState.selector.visibility =
                    if (isVisible) View.VISIBLE else View.GONE
            }
        } else {
            stagedFanUIs.forEach { stagedFanState ->
                stagedFanState.stagedFanLabel.visibility = View.GONE
                stagedFanState.selector.visibility = View.GONE
            }
        }
    }


    private fun getDisplayDeviceCount(): Int{
        var count = 0
        if(displayHumidity.isChecked) count++
        if(displayCo2.isChecked) count++
        if(displayVOC.isChecked) count++
        if(displayPp2p5.isChecked) count++
        return count
    }

    private fun enableDisplay(toggle: CustomCCUSwitch): Boolean{
        val count = getDisplayDeviceCount()
        if(count > 2) {
            Toast.makeText(requireContext(),"Only two items can be displayed in home screen", Toast.LENGTH_SHORT).show()
            toggle.isChecked = false
            return false
        }
        return true
    }

    // Just dispose the
    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
        configurationDisposable.dispose()
        if (Globals.getInstance().isTestMode) {
            Globals.getInstance().isTestMode = false
        }
    }


    @SuppressLint("LogNotTimber")
    private fun sendControl() {
        if (!viewModel.isProfileConfigured()) {
            Log.i(L.TAG_CCU_HSCPU,
                "--------------HyperStat CPU test signal sendControl: Not Ready")
            return
        }
        val testSignalControlMessage: HyperStat.HyperStatControlsMessage_t  = getControlMessage()
        Log.i(L.TAG_CCU_HSCPU,
            "--------------Hyperstat CPU test signal sendControl: ------------------\n" +
                    "Node address  ${meshAddress.toInt()}\n" +
                    "setTemp Heating  ${testSignalControlMessage.setTempHeating}\n" +
                    "setTemp Cooling  ${testSignalControlMessage.setTempCooling}\n" +
                    "conditioningMode  ${testSignalControlMessage.conditioningMode}\n" +
                    "Fan Mode  ${testSignalControlMessage.fanSpeed}\n" +
                    "Relay1 ${testSignalControlMessage.relay1}\n" +
                    "Relay2 ${testSignalControlMessage.relay2}\n" +
                    "Relay3 ${testSignalControlMessage.relay3}\n" +
                    "Relay4 ${testSignalControlMessage.relay4}\n" +
                    "Relay5 ${testSignalControlMessage.relay5}\n" +
                    "Relay6 ${testSignalControlMessage.relay6}\n" +
                    "Analog Out1 ${testSignalControlMessage.analogOut1.percent}\n" +
                    "Analog Out2 ${testSignalControlMessage.analogOut2.percent}\n" +
                    "Analog Out3 ${testSignalControlMessage.analogOut3.percent}\n" +
                    "-------------------------------------------------------------")
        HyperStatMessageSender.writeControlMessage(
            testSignalControlMessage, meshAddress.toInt(), MessageType.HYPERSTAT_CONTROLS_MESSAGE,
            false
        )
        if (relay1Test.isChecked || relay2Test.isChecked || relay3Test.isChecked
            || relay4Test.isChecked || relay5Test.isChecked || relay5Test.isChecked ) {
            if (!Globals.getInstance().isTestMode) {
                Globals.getInstance().isTestMode = true
            }
        } else {
            if (Globals.getInstance().isTestMode) {
                Globals.getInstance().isTestMode = false
            }
        }
    }


    private fun getControlMessage(): HyperStat.HyperStatControlsMessage_t {
        if (meshAddress != null) {
            val ao1Min = analogOutUIs[0].vAtMinDamperSelector.selectedItem.toString().replace("V", "").toDouble()
            val ao1Max = analogOutUIs[0].vAtMaxDamperSelector.selectedItem.toString().replace("V", "").toDouble()

            val ao2Min = analogOutUIs[1].vAtMinDamperSelector.selectedItem.toString().replace("V", "").toDouble()
            val ao2Max = analogOutUIs[1].vAtMaxDamperSelector.selectedItem.toString().replace("V", "").toDouble()

            val ao3Min = analogOutUIs[2].vAtMinDamperSelector.selectedItem.toString().replace("V", "").toDouble()
            val ao3Max = analogOutUIs[2].vAtMaxDamperSelector.selectedItem.toString().replace("V", "").toDouble()

            return HyperStat.HyperStatControlsMessage_t.newBuilder()
                .setRelay1(relay1Test.isChecked)
                .setRelay2(relay2Test.isChecked)
                .setRelay3(relay3Test.isChecked)
                .setRelay4(relay4Test.isChecked)
                .setRelay5(relay5Test.isChecked)
                .setRelay6(relay6Test.isChecked)
                .setAnalogOut1(
                    HyperStat.HyperStatAnalogOutputControl_t
                        .newBuilder().setPercent(
                            getAnalogVal(ao1Min, ao1Max, analogOut1Test.selectedItem.toString().toDouble())
                        ).build()
                )
                .setAnalogOut2(
                    HyperStat.HyperStatAnalogOutputControl_t
                        .newBuilder().setPercent(
                            getAnalogVal(ao2Min, ao2Max, analogOut2Test.selectedItem.toString().toDouble())
                        ).build()
                )
                .setAnalogOut3(
                    HyperStat.HyperStatAnalogOutputControl_t
                        .newBuilder().setPercent(
                            getAnalogVal(ao3Min, ao3Max, analogOut3Test.selectedItem.toString().toDouble())
                        ).build()
                )
                .setSetTempCooling(getDesiredTempCooling(meshAddress).toInt() * 2)
                .setSetTempHeating(getDesiredTempHeating(meshAddress).toInt() * 2)
                .setFanSpeed(HyperStat.HyperStatFanSpeed_e.HYPERSTAT_FAN_SPEED_AUTO)
                .setConditioningMode(HyperStat.HyperStatConditioningMode_e.HYPERSTAT_CONDITIONING_MODE_AUTO)
                .build()
        } else {
            return HyperStat.HyperStatControlsMessage_t.newBuilder().build()
        }
    }
    private fun getDesiredTempCooling(node: Short): Double {
        return CCUHsApi.getInstance()
            .readPointPriorityValByQuery("desired and temp and cooling and group == \"$node\"")
    }

    private fun getDesiredTempHeating(node: Short): Double {
        return CCUHsApi.getInstance().readPointPriorityValByQuery(
            "desired and temp and heating and group == \"$node\"")
    }

    private fun getAnalogVal(min: Double, max: Double, voltage : Double): Int {
        return if (max > min) (10 * (min + (max - min) * voltage / 100)).toInt() else (10 * (min - (min - max) * voltage /
                100)).toInt()
    }

    private fun configUIOnProfile(view: View){

        profileName = view.findViewById(R.id.profile_name)
        th2Label = view.findViewById(R.id.th2Label)

        profileName.text = viewModel.getProfileName()
        th2Label.text = viewModel.getTh2SensorLabel()

        val adapterRelayMapping = viewModel.getRelayMappingAdapter(requireContext(), viewModel.getRelayMapping())
        adapterAnalogOutMapping = context?.let { AnalogOutAdapter(it, R.layout.spinner_dropdown_item, viewModel.getAnalogOutMapping()) }
        var relayPos = 0
        relayUIs.forEach {
                it.selector.adapter = adapterRelayMapping
                it.selector.tag = relayPos++
        }
        analogOutUIs.forEach {
                analogOutWidgets -> analogOutWidgets.selector.adapter = adapterAnalogOutMapping
        }
    }

    private fun getAdapterValue(values: Array<String?>): ArrayAdapter<*> {
        return ArrayAdapter( requireContext(), R.layout.spinner_dropdown_item, values)
    }

}

