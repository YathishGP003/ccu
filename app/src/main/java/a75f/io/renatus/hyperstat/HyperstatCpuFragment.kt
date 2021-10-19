package a75f.io.renatus.hyperstat

import a75f.io.device.HyperStat
import a75f.io.device.mesh.LSerial
import a75f.io.device.mesh.hyperstat.HyperStatMessageSender
import a75f.io.device.serial.MessageType
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.bo.building.NodeType
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.hyperstat.cpu.CpuAnalogOutAssociation
import a75f.io.renatus.BASE.BaseDialogFragment
import a75f.io.renatus.BASE.FragmentCommonBundleArgs
import a75f.io.renatus.FloorPlanFragment
import a75f.io.renatus.R
import a75f.io.renatus.util.ProgressDialogUtils
import a75f.io.renatus.util.RxjavaUtil
import a75f.io.renatus.util.extension.showErrorDialog
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


/**
 * @author tcase@75f.io
 * Created on 6/14/21.
 */
class HyperStatCpuFragment : BaseDialogFragment() {
    private val disposables =
        CompositeDisposable()           // All of our fragment Rx subscriptions, for easy management.
    private val configurationDisposable = CompositeDisposable()
    private val viewModel: HyperStatCpuViewModel by viewModels()

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

    //   The UI widgets we're holding references to.
    //   Butterknife and kotlin synthetics are deprecated, so we're binding the old fashioned way

    lateinit var tempOffsetSelector: NumberPicker
    lateinit var forceOccupiedSwitch: ToggleButton
    lateinit var autoAwaySwitch: ToggleButton

    // 6 rows, 1 for each relay
    lateinit var relayUIs: List<RelayWidgets>

    // 3 rows, 1 for each analog out, plus damper voltage selectors
    lateinit var analogOutUIs: List<AnalogOutWidgets>

    lateinit var airflowSensorSwitch: ToggleButton
    lateinit var doorWindowSensorSwitch: ToggleButton

    // 2 rows, 1 for each analog in.
    lateinit var analogInUIs: List<AnalogInWidgets>

    lateinit var setButton: Button
    lateinit var zoneCO2Layout: View
    lateinit var zoneCO2DamperOpeningRate: Spinner
    lateinit var zoneCO2Threshold: Spinner
    lateinit var zoneCO2Target: Spinner


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
        const val ID = "HyperStatCpuFragment"

        @JvmStatic
        fun newInstance(
            meshAddress: Short, roomName: String, floorName: String, nodeType: NodeType,
            profileType: ProfileType
        ): HyperStatCpuFragment {
            val args = Bundle()

            args.putShort(FragmentCommonBundleArgs.ARG_PAIRING_ADDR, meshAddress)
            args.putString(FragmentCommonBundleArgs.ARG_NAME, roomName)
            args.putString(FragmentCommonBundleArgs.FLOOR_NAME, floorName)
            args.putString(FragmentCommonBundleArgs.NODE_TYPE, nodeType.toString())
            args.putInt(FragmentCommonBundleArgs.PROFILE_TYPE, profileType.ordinal)

            val fragment = HyperStatCpuFragment()
            fragment.arguments = args
            return fragment
        }

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_hyperstat_cpu_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews()
        setUpSpinners()
        setUpViewListeners()


        // register with view model
        viewModel.initData(meshAddress, roomName, floorName, nodeType, profileType)

        disposables.add(
            viewModel.viewState
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ viewState -> render(viewState) },
                    { error -> handleError(error) })
        )
        CcuLog.i("CCU_HYPERSTAT", "View created for Hyperstat Cpu Fragment")
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
            doorWindowSensorSwitch = findViewById(R.id.doorWindowEnableToggle)

            analogInUIs = listOf(
                AnalogInWidgets(findViewById(R.id.toggle_analog_in1), findViewById(R.id.analog1_in_spinner)),
                AnalogInWidgets(findViewById(R.id.toggle_analog_in2), findViewById(R.id.analog2_in_spinner))
            )

            zoneCO2Layout = findViewById(R.id.dcvCo2Config)
            zoneCO2DamperOpeningRate = findViewById(R.id.zoneCO2DamperOpeningRateSpinner)
            zoneCO2Threshold = findViewById(R.id.zoneCO2ThresholdSpinner)
            zoneCO2Target = findViewById(R.id.zoneCO2TargetSpinner)

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

            analogOut1Test.setOnItemSelected { sendControl() }
            analogOut2Test.setOnItemSelected { sendControl() }
            analogOut3Test.setOnItemSelected { sendControl() }

        }
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
        //temperatureOffset.setTextSize()

        // default values  10 , 800 , 1000
        val co2Values = co2DCVDamperValue()
        val co2OpeningValues = co2DCVOpeningDamperValue()

        val co2Adapter: ArrayAdapter<*> = ArrayAdapter<String?>(
            requireContext(), R.layout.larger_spinner_item,
            co2Values
        )
        val co2OpeningAdapter: ArrayAdapter<*> = ArrayAdapter<String?>(
            requireContext(), R.layout.larger_spinner_item,
            co2OpeningValues
        )
        zoneCO2DamperOpeningRate.adapter = co2OpeningAdapter
        zoneCO2Threshold.adapter = co2Adapter
        zoneCO2Target.adapter = co2Adapter


        analogOutUIs.forEach {

            val vValues = analogVoltageAtSpinnerValues()
            // Create the instance of ArrayAdapter
            // having the list of courses
            val adapterVAtMin: ArrayAdapter<*> = ArrayAdapter<String?>(
                requireContext(),
                R.layout.larger_spinner_item,
                vValues
            )

            it.vAtMinDamperSelector.adapter = adapterVAtMin
            val adapterVAtMax: ArrayAdapter<*> = ArrayAdapter<String?>(
                requireContext(),
                R.layout.larger_spinner_item,
                vValues
            )
            it.vAtMaxDamperSelector.adapter = adapterVAtMax

            val percentValues = analogFanLevelSpeedValue()

            val lowAdapter: ArrayAdapter<*> = ArrayAdapter<String?>(
                requireContext(), R.layout.larger_spinner_item,
                percentValues
            )
            val mediumAdapter: ArrayAdapter<*> = ArrayAdapter<String?>(
                requireContext(), R.layout.larger_spinner_item,
                percentValues
            )
            val highAdapter: ArrayAdapter<*> = ArrayAdapter<String?>(
                requireContext(), R.layout.larger_spinner_item,
                percentValues
            )

            it.analogOutAtFanLow.adapter = lowAdapter
            it.analogOutAtFanMedium.adapter = mediumAdapter
            it.analogOutAtFanHigh.adapter = highAdapter

        }
    }

    private fun setUpViewListeners() {
        tempOffsetSelector.setOnValueChangedListener { _, _, newVal ->
            viewModel.tempOffsetSelected(newVal)
        }

        forceOccupiedSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.enableForceOccupiedSwitchChanged(isChecked)
        }
        autoAwaySwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.enableAutoAwaySwitchChanged(isChecked)
        }

        relayUIs.forEachIndexed { index, widgets ->
            widgets.switch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.relaySwitchChanged(index, isChecked)
            }
            widgets.selector.setOnItemSelected { position -> viewModel.relayMappingSelected(index, position) }
        }
        analogOutUIs.forEachIndexed { index, widgets ->
            widgets.switch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.analogOutSwitchChanged(index, isChecked)
            }
            widgets.selector.setOnItemSelected { position -> viewModel.analogOutMappingSelected(index, position) }

            val minDamper = true
            val maxDamper = false
            widgets.vAtMinDamperSelector.setOnItemSelected { position ->
                viewModel.voltageAtDamperSelected(
                    minDamper, index, position
                )
            }
            widgets.vAtMaxDamperSelector.setOnItemSelected { position ->
                viewModel.voltageAtDamperSelected(
                    maxDamper, index, position
                )
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
        }
        doorWindowSensorSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.doorWindowSensorSwitchChanged(isChecked)
        }

        analogInUIs.forEachIndexed { index, widgets ->
            widgets.switch.setOnCheckedChangeListener { _, isChecked ->
                viewModel.analogInSwitchChanged(index, isChecked)
            }
            widgets.selector.setOnItemSelected { position -> viewModel.analogInMappingSelected(index, position) }
        }

        zoneCO2DamperOpeningRate.setOnItemSelected { position -> viewModel.zoneCO2DamperOpeningRateSelect(position) }
        zoneCO2Threshold.setOnItemSelected { position -> viewModel.zoneCO2ThresholdSelect(position) }
        zoneCO2Target.setOnItemSelected { position -> viewModel.zoneCO2TargetSelect(position) }


        // On Click save the CPU configuration
        setButton.setOnClickListener {
            setButton.isEnabled = false
            configurationDisposable.add(RxjavaUtil.executeBackgroundTaskWithDisposable(
                {
                    ProgressDialogUtils.showProgressDialog(activity, "Saving Hyperstat CPU Configuration")
                }, {
                    viewModel.setConfigSelected()

                    LSerial.getInstance().sendHyperStatSeedMessage(
                        this.meshAddress, roomName, floorName, "hyperstatcpu"
                    )
                }, {
                    ProgressDialogUtils.hideProgressDialog()
                    closeAllBaseDialogFragments()
                    activity?.sendBroadcast(Intent(FloorPlanFragment.ACTION_BLE_PAIRING_COMPLETED))
                }

            ))

        }
    }

    private fun handleError(error: Throwable) {
        showErrorDialog(error::class.java.simpleName + " : " + error.localizedMessage)
    }

    /**
     * This rendor method will be called in UI change
     */
    private fun render(viewState: CpuViewState) {

        tempOffsetSelector.value = viewState.tempOffsetPosition

        forceOccupiedSwitch.isChecked = viewState.forceOccupiedEnabled
        autoAwaySwitch.isChecked = viewState.autoAwayEnabled

        viewState.relays.forEachIndexed { index, relayState ->
            with(relayUIs[index]) {
                switch.isChecked = relayState.enabled
                selector.isEnabled = relayState.enabled
                selector.setSelection(relayState.association.ordinal)
                /*testButton.isEnabled = relayState.enabled
                if (!relayState.enabled) {
                    testButton.isChecked = false
                }*/
            }
        }
        var isDampSelected = false
        viewState.analogOutUis.forEachIndexed { index, analogOutState ->
            with(analogOutUIs[index]) {
                switch.isChecked = analogOutState.enabled
                selector.isEnabled = analogOutState.enabled
                selector.setSelection(analogOutState.association.ordinal)
                // testSelector.isEnabled = analogOutState.enabled
                vAtMinDamperLabel.text = String.format(
                    "%s%d at \nMin %s",
                    getString(R.string.hyperstat_analog_out),
                    index + 1,
                    getString(analogOutState.association.displayName)
                )
                vAtMinDamperLabel.isEnabled = analogOutState.enabled
                vAtMinDamperSelector.isEnabled = analogOutState.enabled
                vAtMinDamperSelector.setSelection(analogOutState.vAtMinDamperPosition)
                vAtMaxDamperLabel.text = String.format(
                    "%s%d at \nMax %s",
                    getString(R.string.hyperstat_analog_out),
                    index + 1,
                    getString(analogOutState.association.displayName)
                )
                vAtMaxDamperLabel.isEnabled = analogOutState.enabled
                vAtMaxDamperSelector.isEnabled = analogOutState.enabled
                vAtMaxDamperSelector.setSelection(analogOutState.vAtMaxDamperPosition)


                analogOutFanConfig.visibility =
                    if (analogOutState.enabled && analogOutState.association.ordinal == 1) View.VISIBLE else View.GONE
                analogOutAtFanLow.setSelection(analogOutState.perAtFanLowPosition)
                analogOutAtFanMedium.setSelection(analogOutState.perAtFanMediumPosition)
                analogOutAtFanHigh.setSelection(analogOutState.perAtFanHighPosition)

                if (analogOutState.association.ordinal == CpuAnalogOutAssociation.DCV_DAMPER.ordinal) {
                    isDampSelected = true
                }
            }
        }

        airflowSensorSwitch.isChecked = viewState.airflowTempSensorEnabled
        doorWindowSensorSwitch.isChecked = viewState.doorWindowSensor1Enabled

        viewState.analogIns.forEachIndexed { index, analogInState ->
            with(analogInUIs[index]) {
                switch.isChecked = analogInState.enabled
                selector.isEnabled = analogInState.enabled
                selector.setSelection(analogInState.association.ordinal)
            }
        }
        zoneCO2Layout.visibility = if (isDampSelected) View.VISIBLE else View.GONE
        zoneCO2DamperOpeningRate.setSelection(viewState.zoneCO2DamperOpeningRatePos)
        zoneCO2Threshold.setSelection(viewState.zoneCO2ThresholdPos)
        zoneCO2Target.setSelection(viewState.zoneCO2TargetPos)
    }


    // Just dispose the
    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
        configurationDisposable.dispose()
    }


    private fun sendControl() {
        HyperStatMessageSender.writeControlMessage(
            getControlMessage(), meshAddress.toInt(), MessageType.HYPERSTAT_CONTROLS_MESSAGE,
            false
        )
        if (relay1Test.isChecked || relay2Test.isChecked || relay3Test.isChecked
            || relay4Test.isChecked || relay5Test.isChecked || relay5Test.isChecked
        ) {
            if (!Globals.getInstance().isTestMode) {
                Globals.getInstance().isTestMode = true
            }
        } else {
            if (Globals.getInstance().isTestMode) {
                Globals.getInstance().isTestMode = false
            }
        }
    }


    private fun getControlMessage(): HyperStat.HyperStatControlsMessage_t? {
        return HyperStat.HyperStatControlsMessage_t.newBuilder()
            .setAnalogOut1(HyperStat.HyperStatAnalogOutputControl_t.newBuilder().setPercent(80))
            .setRelay1(relay1Test.isChecked)
            .setRelay2(relay2Test.isChecked)
            .setRelay3(relay3Test.isChecked)
            .setRelay4(relay4Test.isChecked)
            .setRelay5(relay5Test.isChecked)
            .setRelay6(relay6Test.isChecked)

            .setAnalogOut1(
                HyperStat.HyperStatAnalogOutputControl_t
                    .newBuilder().setPercent(
                        Integer.parseInt(
                            analogOut1Test.selectedItem.toString()
                        )
                    ).build()
            )
            .setAnalogOut2(
                HyperStat.HyperStatAnalogOutputControl_t
                    .newBuilder().setPercent(
                        Integer.parseInt(
                            analogOut2Test.selectedItem.toString()
                        )
                    ).build()
            )
            .setAnalogOut3(
                HyperStat.HyperStatAnalogOutputControl_t
                    .newBuilder().setPercent(
                        Integer.parseInt(
                            analogOut3Test.selectedItem.toString()
                        )
                    ).build()
            )
            .setSetTempCooling(10 * 74)
            .setSetTempHeating(10 * 69)
            .build()
    }


}

// Extenstion method to kotlinize and pretty up our code above
private fun Spinner.setOnItemSelected(onItemSelected: (position: Int) -> Unit) {
    onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            onItemSelected.invoke(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {} // no implementation
    }
}

class RelayWidgets(
    val switch: ToggleButton,
    val selector: Spinner,
)

class AnalogOutWidgets(
    val switch: ToggleButton,
    val selector: Spinner,
    val vAtMinDamperLabel: TextView,
    val vAtMinDamperSelector: Spinner,
    val vAtMaxDamperLabel: TextView,
    val vAtMaxDamperSelector: Spinner,
    val analogOutFanConfig: View,
    val analogOutAtFanLow: Spinner,
    val analogOutAtFanMedium: Spinner,
    val analogOutAtFanHigh: Spinner
)

class AnalogInWidgets(
    val switch: ToggleButton,
    val selector: Spinner
)
