package a75f.io.renatus.profiles.hss.unitventilator.ui

import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe2UvAnalogOutControls
import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UvAnalogOutControls
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.compose.StyledTextView
import a75f.io.renatus.profiles.hss.HyperStatSplitFragment
import a75f.io.renatus.profiles.hss.unitventilator.viewmodels.Pipe2UvViewModel
import a75f.io.renatus.profiles.hss.unitventilator.viewmodels.Pipe4UvViewModel
import a75f.io.renatus.profiles.hss.unitventilator.viewmodels.UnitVentilatorViewModel
import a75f.io.renatus.profiles.hss.unitventilator.viewstate.Pipe2UvViewState
import a75f.io.renatus.profiles.hss.unitventilator.viewstate.Pipe4UvViewState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels

open class UnitVentilatorFragment : HyperStatSplitFragment() {

    open val viewModel : UnitVentilatorViewModel by viewModels()


    private fun isPipe4UnitVentilator(): Boolean {
        return viewModel is Pipe4UvViewModel
    }

    @Composable
    fun AnalogOutDynamicConfig(viewModel: UnitVentilatorViewModel, modifier: Modifier = Modifier) {
        FanControl(viewModel,modifier)
        FaceAndBypassDamper(viewModel,modifier)
        OAODamperControl(viewModel,modifier)
        DcvModulationControl(viewModel,modifier)
        FanSpeed(viewModel)
    }

    @Composable
    fun GenericView(viewModel: UnitVentilatorViewModel){
        Title(viewModel)
        ControlVia(viewModel)
        TempOffset(viewModel)
        AutoAwayConfig(viewModel)
        TitleLabel()
    }

    @Composable
    fun ConfigurationsView(viewModel: UnitVentilatorViewModel){
        SensorConfig(viewModel)
        RelayConfig(viewModel)
        AnalogOutConfig(viewModel)
        UniversalInConfig(viewModel)
    }

    @Composable
    fun ControlVia(viewModel: UnitVentilatorViewModel) {

        Column(
            modifier = Modifier.padding(start = 25.dp, top = 55.dp, bottom = 25.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = AbsoluteAlignment.Left
        )
        {

            fun UnitVentilatorViewModel.getControlVia(): Int {
                return when (profileType) {
                    ProfileType.HYPERSTATSPLIT_4PIPE_UV -> (viewState.value as Pipe4UvViewState).controlVia
                    ProfileType.HYPERSTATSPLIT_2PIPE_UV -> (viewState.value as Pipe2UvViewState).controlVia
                    else -> (viewState.value as Pipe4UvViewState).controlVia // fallback
                }
            }

            fun UnitVentilatorViewModel.setControlVia(value: Int) {
                when (profileType) {
                    ProfileType.HYPERSTATSPLIT_4PIPE_UV -> (viewState.value as Pipe4UvViewState).controlVia = value
                    ProfileType.HYPERSTATSPLIT_2PIPE_UV -> (viewState.value as Pipe2UvViewState).controlVia = value
                    else -> (viewState.value as Pipe4UvViewState).controlVia = value
                }
            }

            DropDownWithLabel(
                "Control Via :",
                viewModel.controlViaList,
                onSelected = {
                    viewModel.setControlVia(it)
                },
                defaultSelection = viewModel.getControlVia(),
                isHeader = true,
                spacerLimit = 10,
                paddingLimit = 0,
                previewWidth = 250,
                expandedWidth = 270,
                labelWidth = 150,
            )
        }

    }


    @Composable
    fun FanControl(viewModel: UnitVentilatorViewModel, modifier: Modifier = Modifier) {

        val key = when(viewModel){
            is Pipe4UvViewModel -> Pipe4UvAnalogOutControls.FAN_SPEED.ordinal
            is Pipe2UvViewModel -> Pipe2UvAnalogOutControls.FAN_SPEED.ordinal
            else -> { Pipe4UvAnalogOutControls.FAN_SPEED.ordinal}
        }
        Column(modifier =  modifier) {
            if (viewModel.isLinearFanAOEnabled(key)) {
                EnableFanVoltage(viewModel,key)
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun FanSpeed(viewModel: UnitVentilatorViewModel) {
        val state = viewModel.viewState.value
        val isPipe4 = isPipe4UnitVentilator()
        var viewCount :Int
        val key = when (viewModel) {
            is Pipe4UvViewModel -> Pipe4UvAnalogOutControls.FAN_SPEED.ordinal
            is Pipe2UvViewModel -> Pipe2UvAnalogOutControls.FAN_SPEED.ordinal
            else -> Pipe4UvAnalogOutControls.FAN_SPEED.ordinal
        }

        fun getFanSpeed(index: Int): Triple<String, String, String> {
            return if (isPipe4) {
                val s = state as Pipe4UvViewState
                val minMax = when (index) {
                    1 -> s.analogOut1MinMax
                    2 -> s.analogOut2MinMax
                    3 -> s.analogOut3MinMax
                    4 -> s.analogOut4MinMax
                    else -> throw IllegalArgumentException("Invalid index")
                }
                Triple(
                    minMax.fanAtLow.toString(),
                    minMax.fanAtMedium.toString(),
                    minMax.fanAtHigh.toString()
                )
            } else {
                val s = state as Pipe2UvViewState
                val minMax = when (index) {
                    1 -> s.analogOut1MinMax
                    2 -> s.analogOut2MinMax
                    3 -> s.analogOut3MinMax
                    4 -> s.analogOut4MinMax
                    else -> throw IllegalArgumentException("Invalid index")
                }
                Triple(
                    minMax.fanAtLow.toString(),
                    minMax.fanAtMedium.toString(),
                    minMax.fanAtHigh.toString()
                )
            }
        }

        fun setFanSpeed(index: Int, level: String, value: Int) {
            if (isPipe4) {
                val s = state as Pipe4UvViewState
                when (index) {
                    1 -> when (level) {
                        "Low" -> s.analogOut1MinMax.fanAtLow = value
                        "Medium" -> s.analogOut1MinMax.fanAtMedium = value
                        "High" -> s.analogOut1MinMax.fanAtHigh = value
                    }

                    2 -> when (level) {
                        "Low" -> s.analogOut2MinMax.fanAtLow = value
                        "Medium" -> s.analogOut2MinMax.fanAtMedium = value
                        "High" -> s.analogOut2MinMax.fanAtHigh = value
                    }

                    3 -> when (level) {
                        "Low" -> s.analogOut3MinMax.fanAtLow = value
                        "Medium" -> s.analogOut3MinMax.fanAtMedium = value
                        "High" -> s.analogOut3MinMax.fanAtHigh = value
                    }

                    4 -> when (level) {
                        "Low" -> s.analogOut4MinMax.fanAtLow = value
                        "Medium" -> s.analogOut4MinMax.fanAtMedium = value
                        "High" -> s.analogOut4MinMax.fanAtHigh = value
                    }
                }
            } else {
                val s = state as Pipe2UvViewState
                when (index) {
                    1 -> when (level) {
                        "Low" -> s.analogOut1MinMax.fanAtLow = value
                        "Medium" -> s.analogOut1MinMax.fanAtMedium = value
                        "High" -> s.analogOut1MinMax.fanAtHigh = value
                    }

                    2 -> when (level) {
                        "Low" -> s.analogOut2MinMax.fanAtLow = value
                        "Medium" -> s.analogOut2MinMax.fanAtMedium = value
                        "High" -> s.analogOut2MinMax.fanAtHigh = value
                    }

                    3 -> when (level) {
                        "Low" -> s.analogOut3MinMax.fanAtLow = value
                        "Medium" -> s.analogOut3MinMax.fanAtMedium = value
                        "High" -> s.analogOut3MinMax.fanAtHigh = value
                    }

                    4 -> when (level) {
                        "Low" -> s.analogOut4MinMax.fanAtLow = value
                        "Medium" -> s.analogOut4MinMax.fanAtMedium = value
                        "High" -> s.analogOut4MinMax.fanAtHigh = value
                    }
                }
            }
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 25.dp, bottom = 10.dp),
            maxItemsInEachRow = 4
        ) {
            (1..4).forEach { index ->
                viewCount = 0
                val isEnabled = when (index) {
                    1 -> viewModel.isAnalogEnabledAndMapped(
                        state.analogOut1Enabled,
                        state.analogOut1Association,
                        key
                    )

                    2 -> viewModel.isAnalogEnabledAndMapped(
                        state.analogOut2Enabled,
                        state.analogOut2Association,
                        key
                    )

                    3 -> viewModel.isAnalogEnabledAndMapped(
                        state.analogOut3Enabled,
                        state.analogOut3Association,
                        key
                    )

                    4 -> viewModel.isAnalogEnabledAndMapped(
                        state.analogOut4Enabled,
                        state.analogOut4Association,
                        key
                    )

                    else -> false
                }

                if (isEnabled) {

                    val (low, medium, high) = getFanSpeed(index)

                    viewCount = 0
                    listOf(
                        "Low" to low,
                        "Medium" to medium,
                        "High" to high
                    ).forEach { (label, default) ->
                        viewCount++

                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .padding(
                                    start = if (viewCount % 2 == 0) 10.dp else 0.dp,
                                    top = 10.dp,
                                    bottom = 10.dp
                                )
                        ) {
                            StyledTextView(
                                "Analog-out$index at Fan $label\n",
                                fontSize = 20,
                                textAlignment = TextAlign.Left
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SpinnerElementOption(
                                defaultSelection = default,
                                items = viewModel.testVoltage,
                                unit = "%",
                                itemSelected = { setFanSpeed(index, label, it.value.toInt()) },
                                previewWidth = 70
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(2f))
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }

        @OptIn(ExperimentalLayoutApi::class)
        @Composable
        fun OAODamperConfig(viewModel: UnitVentilatorViewModel, modifier: Modifier = Modifier) {
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 25.dp, bottom = 10.dp),
                maxItemsInEachRow = 4
            ){
                var nEntries = 0
                if (viewModel.viewState.value.enableOutsideAirOptimization) {

                        nEntries += 6
                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .padding(top = 10.dp, bottom = 20.dp)
                        ) {
                            StyledTextView(
                                "Outside Damper Min Open\nDuring Recirc",
                                fontSize = 20,
                                textAlignment = TextAlign.Left
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SpinnerElementString(
                                defaultSelection = viewModel.viewState.value.outsideDamperMinOpenDuringRecirc.toInt()
                                    .toString(),
                                items = viewModel.outsideDamperMinOpenList, unit = "%",
                                itemSelected = {
                                    viewModel.viewState.value.outsideDamperMinOpenDuringRecirc =
                                        it.toDouble()
                                }, previewWidth = 70)
                        }

                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .padding(top = 10.dp, bottom = 10.dp, start = 10.dp)
                        ) {
                            StyledTextView(
                                "Outside Damper Min Open\nDuring Conditioning",
                                fontSize = 20,
                                textAlignment = TextAlign.Left
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SpinnerElementString(
                                defaultSelection = viewModel.viewState.value.outsideDamperMinOpenDuringConditioning.toInt()
                                    .toString(),
                                items = viewModel.outsideDamperMinOpenList, unit = "%",
                                itemSelected = {
                                    viewModel.viewState.value.outsideDamperMinOpenDuringConditioning =
                                        it.toDouble()
                                },previewWidth = 70)
                        }

                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .padding(top = 10.dp, bottom = 20.dp)
                        ) {
                            StyledTextView(
                                "Outside Damper Min Open\nDuring Fan Low",
                                fontSize = 20,
                                textAlignment = TextAlign.Left
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SpinnerElementString(
                                defaultSelection = viewModel.viewState.value.outsideDamperMinOpenDuringFanLow.toInt()
                                    .toString(),
                                items = viewModel.outsideDamperMinOpenList, unit = "%",
                                itemSelected = {
                                    viewModel.viewState.value.outsideDamperMinOpenDuringFanLow =
                                        it.toDouble()
                                },previewWidth = 70)
                        }

                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .padding(top = 10.dp, bottom = 20.dp, start = 10.dp)
                        ) {
                            StyledTextView(
                                "Outside Damper Min Open\nDuring Fan Medium",
                                fontSize = 20,
                                textAlignment = TextAlign.Left
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SpinnerElementString(
                                defaultSelection = viewModel.viewState.value.outsideDamperMinOpenDuringFanMedium.toInt()
                                    .toString(),
                                items = viewModel.outsideDamperMinOpenList, unit = "%",
                                itemSelected = {
                                    viewModel.viewState.value.outsideDamperMinOpenDuringFanMedium =
                                        it.toDouble()
                                },previewWidth = 70)
                        }

                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .padding(top = 10.dp, bottom = 20.dp)
                        ) {
                            StyledTextView(
                                "Outside Damper Min Open\nDuring Fan High",
                                fontSize = 20,
                                textAlignment = TextAlign.Left
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SpinnerElementString(
                                defaultSelection = viewModel.viewState.value.outsideDamperMinOpenDuringFanHigh.toInt()
                                    .toString(),
                                items = viewModel.outsideDamperMinOpenList, unit = "%",
                                itemSelected = {
                                    viewModel.viewState.value.outsideDamperMinOpenDuringFanHigh =
                                        it.toDouble()
                                },previewWidth = 70)
                        }
                        Box(
                            modifier = Modifier
                                .weight(2f)
                                .padding(top = 10.dp, bottom = 20.dp, start = 10.dp)
                        ) {
                            StyledTextView(
                                "CO2 Damper Opening Rate\n",
                                fontSize = 20,
                                textAlignment = TextAlign.Left
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            SpinnerElementString(
                                defaultSelection = viewModel.viewState.value.zoneCO2DamperOpeningRate.toInt()
                                    .toString(),
                                items = viewModel.zoneCO2DamperOpeningRateList, unit = "%",
                                itemSelected = {
                                    viewModel.viewState.value.zoneCO2DamperOpeningRate =
                                        it.toDouble()
                                },previewWidth = 70)
                        }

                        if (viewModel.isPrePurgeEnabled()) {
                            nEntries += 1
                            Box(
                                modifier = Modifier
                                    .weight(2f)
                                    .padding(top = 10.dp, bottom = 20.dp)
                            ) {
                                StyledTextView(
                                    "Pre Purge Min\nDamper Position",
                                    fontSize = 20,
                                    textAlignment = TextAlign.Left
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                SpinnerElementString(
                                    defaultSelection = viewModel.viewState.value.prePurgeOutsideDamperOpen.toInt()
                                        .toString(),
                                    items = viewModel.prePurgeOutsideDamperOpenList, unit = "%",
                                    itemSelected = {
                                        viewModel.viewState.value.prePurgeOutsideDamperOpen =
                                            it.toDouble()
                                    },previewWidth = 70)
                            }
                        }
                    }

                    nEntries += 3
                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .padding(
                                top = 10.dp,
                                bottom = 20.dp,
                                start = if (nEntries % 2 == 0) 10.dp else 0.dp
                            )
                    ) {
                        StyledTextView(
                            "Zone CO2 Threshold\n", fontSize = 20, textAlignment = TextAlign.Left
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 25.dp)
                    ) {
                        SpinnerElementString(
                            defaultSelection = viewModel.viewState.value.zoneCO2Threshold.toInt()
                                .toString(),
                            items = viewModel.zoneCO2ThresholdList, unit = "ppm",
                            itemSelected = {
                                viewModel.viewState.value.zoneCO2Threshold = it.toDouble()
                            }, previewWidth = 125,textWidth = 120)
                    }

                    Box(
                        modifier = Modifier
                            .weight(2f)
                            .padding(
                                top = 10.dp,
                                bottom = 20.dp,
                                start = if (nEntries % 2 == 1) 10.dp else 0.dp
                            )
                    ) {
                        StyledTextView(
                            "Zone CO2 Target\n", fontSize = 20, textAlignment = TextAlign.Left
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 25.dp)
                    ) {
                        SpinnerElementString(
                            defaultSelection = viewModel.viewState.value.zoneCO2Target.toInt()
                                .toString(),
                            items = viewModel.zoneCO2TargetList, unit = "ppm",
                            itemSelected = {
                                viewModel.viewState.value.zoneCO2Target = it.toDouble()
                            }, previewWidth = 125,textWidth = 120)
                    }
                    Box(
                        modifier = modifier
                            .weight(2f)
                            .padding(
                                top = 10.dp,
                                bottom = 20.dp,
                                start = if (nEntries % 2 == 1) 0.dp else 10.dp
                            )
                    ) {
                        StyledTextView(
                            "Zone PM2.5 Target\n", fontSize = 20, textAlignment = TextAlign.Left
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 25.dp)
                    ) {
                        SpinnerElementString(
                            defaultSelection = viewModel.viewState.value.zonePM2p5Target.toInt()
                                .toString(),
                            items = viewModel.zonePM2p5TargetList, unit = "ug/m3",
                            itemSelected = {
                                viewModel.viewState.value.zonePM2p5Target = it.toDouble()
                            }, previewWidth = 125,textWidth = 120)
                    }

                    if (nEntries % 2 == 1) {
                        Box(modifier = Modifier.weight(2f))
                        Box(modifier = Modifier.weight(1f))
                    }

                }

        }

    @Composable
    fun EnableFanVoltage(viewModel: UnitVentilatorViewModel, enumValue: Int) {
        val state = viewModel.viewState.value
        val isPipe4 = isPipe4UnitVentilator()

        fun getFanMinMax(index: Int): Pair<String, String> {
            return if (isPipe4) {
                val pipe4 = state as Pipe4UvViewState
                when (index) {
                    1 -> pipe4.analogOut1MinMax
                    2 -> pipe4.analogOut2MinMax
                    3 -> pipe4.analogOut3MinMax
                    4 -> pipe4.analogOut4MinMax
                    else -> throw IllegalArgumentException("Invalid analog index")
                }.let {
                    it.fanMin.toString() to it.fanMax.toString()
                }
            } else {
                val pipe2 = state as Pipe2UvViewState
                when (index) {
                    1 -> pipe2.analogOut1MinMax
                    2 -> pipe2.analogOut2MinMax
                    3 -> pipe2.analogOut3MinMax
                    4 -> pipe2.analogOut4MinMax
                    else -> throw IllegalArgumentException("Invalid analog index")
                }.let {
                    it.fanMin.toString() to it.fanMax.toString()

                }
            }
        }

        fun setFanMin(index: Int, value: Int) {
            if (isPipe4) {
                val pipe4 = state as Pipe4UvViewState
                when (index) {
                    1 -> pipe4.analogOut1MinMax.fanMin = value
                    2 -> pipe4.analogOut2MinMax.fanMin = value
                    3 -> pipe4.analogOut3MinMax.fanMin = value
                    4 -> pipe4.analogOut4MinMax.fanMin = value
                }
            } else {
                val pipe2 = state as Pipe2UvViewState
                when (index) {
                    1 -> pipe2.analogOut1MinMax.fanMin = value
                    2 -> pipe2.analogOut2MinMax.fanMin = value
                    3 -> pipe2.analogOut3MinMax.fanMin = value
                    4 -> pipe2.analogOut4MinMax.fanMin = value
                }
            }
        }

        fun setFanMax(index: Int, value: Int) {
            if (isPipe4) {
                val pipe4 = state as Pipe4UvViewState
                when (index) {
                    1 -> pipe4.analogOut1MinMax.fanMax = value
                    2 -> pipe4.analogOut2MinMax.fanMax = value
                    3 -> pipe4.analogOut3MinMax.fanMax = value
                    4 -> pipe4.analogOut4MinMax.fanMax = value
                }
            } else {
                val pipe2 = state as Pipe2UvViewState
                when (index) {
                    1 -> pipe2.analogOut1MinMax.fanMax = value
                    2 -> pipe2.analogOut2MinMax.fanMax = value
                    3 -> pipe2.analogOut3MinMax.fanMax = value
                    4 -> pipe2.analogOut4MinMax.fanMax = value
                }
            }
        }

        (1..4).forEach { index ->
            val isEnabled = when (index) {
                1 -> viewModel.isAnalogEnabledAndMapped(state.analogOut1Enabled, state.analogOut1Association, enumValue)
                2 -> viewModel.isAnalogEnabledAndMapped(state.analogOut2Enabled, state.analogOut2Association, enumValue)
                3 -> viewModel.isAnalogEnabledAndMapped(state.analogOut3Enabled, state.analogOut3Association, enumValue)
                4 -> viewModel.isAnalogEnabledAndMapped(state.analogOut4Enabled, state.analogOut4Association, enumValue)
                else -> false
            }

            if (isEnabled) {
                val (minVal, maxVal) = getFanMinMax(index)
                MinMaxConfiguration(
                    "Analog-out$index at Min \nFan Speed",
                    "Analog-out$index at Max \nFan Speed",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = minVal,
                    maxDefault = maxVal,
                    onMinSelected = { setFanMin(index, it.value.toInt()) },
                    onMaxSelected = { setFanMax(index, it.value.toInt()) }
                )
            }
        }
    }

    @Composable
    fun FaceAndBypassDamper(viewModel: UnitVentilatorViewModel,modifier: Modifier = Modifier) {
        val key = when(viewModel){
            is Pipe4UvViewModel -> Pipe4UvAnalogOutControls.FACE_DAMPER_VALVE.ordinal
            is Pipe2UvViewModel -> Pipe2UvAnalogOutControls.FACE_DAMPER_VALVE.ordinal
            else -> { Pipe4UvAnalogOutControls.FACE_DAMPER_VALVE.ordinal}
        }
        Column(modifier = modifier) {
            EnableFaceAndBypassDamper(viewModel,key)
        }
    }

    @Composable
    fun EnableFaceAndBypassDamper(viewModel: UnitVentilatorViewModel, type: Int) {
        val state = viewModel.viewState.value
        val isPipe4 = isPipe4UnitVentilator()

        fun getAnalogMinMax(index: Int): Pair<String, String> {
            return if (isPipe4) {
                val pipe4 = state as Pipe4UvViewState
                when (index) {
                    1 -> pipe4.analogOut1MinMax
                    2 -> pipe4.analogOut2MinMax
                    3 -> pipe4.analogOut3MinMax
                    4 -> pipe4.analogOut4MinMax
                    else -> throw IllegalArgumentException("Invalid analog index")
                }.let {
                    it.faceAndBypassDamperMin.toString() to it.faceAndBypassDamperMax.toString()
                }
            } else {
                val pipe2 = state as Pipe2UvViewState
                when (index) {
                    1 -> pipe2.analogOut1MinMax
                    2 -> pipe2.analogOut2MinMax
                    3 -> pipe2.analogOut3MinMax
                    4 -> pipe2.analogOut4MinMax
                    else -> throw IllegalArgumentException("Invalid analog index")
                }.let {
                    it.faceAndBypassDamperMin.toString() to it.faceAndBypassDamperMax.toString()
                }
            }
        }

        fun setAnalogMin(index: Int, value: Int) {
            if (isPipe4) {
                val pipe4 = state as Pipe4UvViewState
                when (index) {
                    1 -> pipe4.analogOut1MinMax.faceAndBypassDamperMin = value
                    2 -> pipe4.analogOut2MinMax.faceAndBypassDamperMin = value
                    3 -> pipe4.analogOut3MinMax.faceAndBypassDamperMin = value
                    4 -> pipe4.analogOut4MinMax.faceAndBypassDamperMin = value
                }
            } else {
                val pipe2 = state as Pipe2UvViewState
                when (index) {
                    1 -> pipe2.analogOut1MinMax.faceAndBypassDamperMin = value
                    2 -> pipe2.analogOut2MinMax.faceAndBypassDamperMin = value
                    3 -> pipe2.analogOut3MinMax.faceAndBypassDamperMin = value
                    4 -> pipe2.analogOut4MinMax.faceAndBypassDamperMin = value
                }
            }
        }

        fun setAnalogMax(index: Int, value: Int) {
            if (isPipe4) {
                val pipe4 = state as Pipe4UvViewState
                when (index) {
                    1 -> pipe4.analogOut1MinMax.faceAndBypassDamperMax = value
                    2 -> pipe4.analogOut2MinMax.faceAndBypassDamperMax = value
                    3 -> pipe4.analogOut3MinMax.faceAndBypassDamperMax = value
                    4 -> pipe4.analogOut4MinMax.faceAndBypassDamperMax = value
                }
            } else {
                val pipe2 = state as Pipe2UvViewState
                when (index) {
                    1 -> pipe2.analogOut1MinMax.faceAndBypassDamperMax = value
                    2 -> pipe2.analogOut2MinMax.faceAndBypassDamperMax = value
                    3 -> pipe2.analogOut3MinMax.faceAndBypassDamperMax = value
                    4 -> pipe2.analogOut4MinMax.faceAndBypassDamperMax = value
                }
            }
        }

        (1..4).forEach { index ->
            val isEnabled = when (index) {
                1 -> viewModel.isAnalogEnabledAndMapped(state.analogOut1Enabled, state.analogOut1Association, type)
                2 -> viewModel.isAnalogEnabledAndMapped(state.analogOut2Enabled, state.analogOut2Association, type)
                3 -> viewModel.isAnalogEnabledAndMapped(state.analogOut3Enabled, state.analogOut3Association, type)
                4 -> viewModel.isAnalogEnabledAndMapped(state.analogOut4Enabled, state.analogOut4Association, type)
                else -> false
            }

            if (isEnabled) {
                val (minVal, maxVal) = getAnalogMinMax(index)
                MinMaxConfiguration(
                    "Analog-out$index at Min \nFace & Bypass Damper",
                    "Analog-out$index at Max \nFace & Bypass Damper",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = minVal,
                    maxDefault = maxVal,
                    onMinSelected = { setAnalogMin(index, it.value.toInt()) },
                    onMaxSelected = { setAnalogMax(index, it.value.toInt()) }
                )
            }
        }
    }


    @Composable
    fun DcvModulationControl(viewModel: UnitVentilatorViewModel,modifier: Modifier = Modifier) {
        val key = when(viewModel){
            is Pipe4UvViewModel -> Pipe4UvAnalogOutControls.DCV_MODULATING_DAMPER.ordinal
            is Pipe2UvViewModel -> Pipe2UvAnalogOutControls.DCV_MODULATING_DAMPER.ordinal
            else -> { Pipe4UvAnalogOutControls.DCV_MODULATING_DAMPER.ordinal}
        }
        Column(modifier = modifier) {
            if (viewModel.isDamperModulationAOEnabled(key)) {
                EnableDcvModulationVoltage(viewModel,key)
            }
        }
    }

    @Composable
    fun EnableDcvModulationVoltage(viewModel: UnitVentilatorViewModel, type: Int) {
        val state = viewModel.viewState.value
        val isPipe4 = isPipe4UnitVentilator()

        fun getDcvModMinMax(index: Int): Pair<String, String> {
            return if (isPipe4) {
                val pipe4 = state as Pipe4UvViewState
                when (index) {
                    1 -> pipe4.analogOut1MinMax
                    2 -> pipe4.analogOut2MinMax
                    3 -> pipe4.analogOut3MinMax
                    4 -> pipe4.analogOut4MinMax
                    else -> throw IllegalArgumentException("Invalid analog index")
                }.let {
                    it.dcvModulationMinVoltage.toString() to it.dcvModulationMaxVoltage.toString()
                }
            } else {
                val pipe2 = state as Pipe2UvViewState
                when (index) {
                    1 -> pipe2.analogOut1MinMax
                    2 -> pipe2.analogOut2MinMax
                    3 -> pipe2.analogOut3MinMax
                    4 -> pipe2.analogOut4MinMax
                    else -> throw IllegalArgumentException("Invalid analog index")
                }.let {
                    it.dcvModulationMinVoltage.toString() to it.dcvModulationMaxVoltage.toString()
                }
            }
        }

        fun setDcvModMin(index: Int, value: Int) {
            if (isPipe4) {
                val pipe4 = state as Pipe4UvViewState
                when (index) {
                    1 -> pipe4.analogOut1MinMax.dcvModulationMinVoltage = value
                    2 -> pipe4.analogOut2MinMax.dcvModulationMinVoltage = value
                    3 -> pipe4.analogOut3MinMax.dcvModulationMinVoltage = value
                    4 -> pipe4.analogOut4MinMax.dcvModulationMinVoltage = value
                }
            } else {
                val pipe2 = state as Pipe2UvViewState
                when (index) {
                    1 -> pipe2.analogOut1MinMax.dcvModulationMinVoltage = value
                    2 -> pipe2.analogOut2MinMax.dcvModulationMinVoltage = value
                    3 -> pipe2.analogOut3MinMax.dcvModulationMinVoltage = value
                    4 -> pipe2.analogOut4MinMax.dcvModulationMinVoltage = value
                }
            }
        }

        fun setDcvModMax(index: Int, value: Int) {
            if (isPipe4) {
                val pipe4 = state as Pipe4UvViewState
                when (index) {
                    1 -> pipe4.analogOut1MinMax.dcvModulationMaxVoltage = value
                    2 -> pipe4.analogOut2MinMax.dcvModulationMaxVoltage = value
                    3 -> pipe4.analogOut3MinMax.dcvModulationMaxVoltage = value
                    4 -> pipe4.analogOut4MinMax.dcvModulationMaxVoltage = value
                }
            } else {
                val pipe2 = state as Pipe2UvViewState
                when (index) {
                    1 -> pipe2.analogOut1MinMax.dcvModulationMaxVoltage = value
                    2 -> pipe2.analogOut2MinMax.dcvModulationMaxVoltage = value
                    3 -> pipe2.analogOut3MinMax.dcvModulationMaxVoltage = value
                    4 -> pipe2.analogOut4MinMax.dcvModulationMaxVoltage = value
                }
            }
        }

        (1..4).forEach { index ->
            val isEnabled = when (index) {
                1 -> viewModel.isAnalogEnabledAndMapped(state.analogOut1Enabled, state.analogOut1Association, type)
                2 -> viewModel.isAnalogEnabledAndMapped(state.analogOut2Enabled, state.analogOut2Association, type)
                3 -> viewModel.isAnalogEnabledAndMapped(state.analogOut3Enabled, state.analogOut3Association, type)
                4 -> viewModel.isAnalogEnabledAndMapped(state.analogOut4Enabled, state.analogOut4Association, type)
                else -> false
            }

            if (isEnabled) {
                val (minVal, maxVal) = getDcvModMinMax(index)
                MinMaxConfiguration(
                    "Analog-out$index at Min \nDCV Modulation Damper",
                    "Analog-out$index at Max \nDCV Modulation Damper",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = minVal,
                    maxDefault = maxVal,
                    onMinSelected = { setDcvModMin(index, it.value.toInt()) },
                    onMaxSelected = { setDcvModMax(index, it.value.toInt()) }
                )
            }
        }
    }

    @Composable
    fun OAODamperControl(viewModel: UnitVentilatorViewModel,modifier: Modifier = Modifier) {
        val key = when(viewModel){
            is Pipe4UvViewModel -> Pipe4UvAnalogOutControls.OAO_DAMPER.ordinal
            is Pipe2UvViewModel -> Pipe2UvAnalogOutControls.OAO_DAMPER.ordinal
            else -> { Pipe4UvAnalogOutControls.OAO_DAMPER.ordinal}
        }
        Column(modifier = modifier) {
            if (viewModel.isOAODamperAOEnabled(key)) {
                EnableOAODamperVoltage(viewModel, key)
            }
        }
    }

    @Composable
    fun EnableOAODamperVoltage(viewModel: UnitVentilatorViewModel, type: Int) {
        val state = viewModel.viewState.value
        val isPipe4 = isPipe4UnitVentilator()

        fun getOaoMinMax(index: Int): Pair<String, String> {
            return if (isPipe4) {
                val pipe4 = state as Pipe4UvViewState
                when (index) {
                    1 -> pipe4.analogOut1MinMax
                    2 -> pipe4.analogOut2MinMax
                    3 -> pipe4.analogOut3MinMax
                    4 -> pipe4.analogOut4MinMax
                    else -> throw IllegalArgumentException("Invalid analog index")
                }.let {
                    it.oaoDamperMinVoltage.toString() to it.oaoDamperMaxVoltage.toString()
                }
            } else {
                val pipe2 = state as Pipe2UvViewState
                when (index) {
                    1 -> pipe2.analogOut1MinMax
                    2 -> pipe2.analogOut2MinMax
                    3 -> pipe2.analogOut3MinMax
                    4 -> pipe2.analogOut4MinMax
                    else -> throw IllegalArgumentException("Invalid analog index")
                }.let {
                    it.oaoDamperMinVoltage.toString() to it.oaoDamperMaxVoltage.toString()
                }
            }
        }

        fun setOaoMin(index: Int, value: Int) {
            if (isPipe4) {
                val pipe4 = state as Pipe4UvViewState
                when (index) {
                    1 -> pipe4.analogOut1MinMax.oaoDamperMinVoltage = value
                    2 -> pipe4.analogOut2MinMax.oaoDamperMinVoltage = value
                    3 -> pipe4.analogOut3MinMax.oaoDamperMinVoltage = value
                    4 -> pipe4.analogOut4MinMax.oaoDamperMinVoltage = value
                }
            } else {
                val pipe2 = state as Pipe2UvViewState
                when (index) {
                    1 -> pipe2.analogOut1MinMax.oaoDamperMinVoltage = value
                    2 -> pipe2.analogOut2MinMax.oaoDamperMinVoltage = value
                    3 -> pipe2.analogOut3MinMax.oaoDamperMinVoltage = value
                    4 -> pipe2.analogOut4MinMax.oaoDamperMinVoltage = value
                }
            }
        }

        fun setOaoMax(index: Int, value: Int) {
            if (isPipe4) {
                val pipe4 = state as Pipe4UvViewState
                when (index) {
                    1 -> pipe4.analogOut1MinMax.oaoDamperMaxVoltage = value
                    2 -> pipe4.analogOut2MinMax.oaoDamperMaxVoltage = value
                    3 -> pipe4.analogOut3MinMax.oaoDamperMaxVoltage = value
                    4 -> pipe4.analogOut4MinMax.oaoDamperMaxVoltage = value
                }
            } else {
                val pipe2 = state as Pipe2UvViewState
                when (index) {
                    1 -> pipe2.analogOut1MinMax.oaoDamperMaxVoltage = value
                    2 -> pipe2.analogOut2MinMax.oaoDamperMaxVoltage = value
                    3 -> pipe2.analogOut3MinMax.oaoDamperMaxVoltage = value
                    4 -> pipe2.analogOut4MinMax.oaoDamperMaxVoltage = value
                }
            }
        }

        (1..4).forEach { index ->
            val isEnabled = when (index) {
                1 -> viewModel.isAnalogEnabledAndMapped(state.analogOut1Enabled, state.analogOut1Association, type)
                2 -> viewModel.isAnalogEnabledAndMapped(state.analogOut2Enabled, state.analogOut2Association, type)
                3 -> viewModel.isAnalogEnabledAndMapped(state.analogOut3Enabled, state.analogOut3Association, type)
                4 -> viewModel.isAnalogEnabledAndMapped(state.analogOut4Enabled, state.analogOut4Association, type)
                else -> false
            }

            if (isEnabled) {
                val (minVal, maxVal) = getOaoMinMax(index)
                MinMaxConfiguration(
                    minLabel = "Analog-out$index at Min \nOAO Damper",
                    maxLabel = "Analog-out$index at Max \nOAO Damper",
                    itemList = viewModel.minMaxVoltage,
                    unit = "V",
                    minDefault = minVal,
                    maxDefault = maxVal,
                    onMinSelected = { setOaoMin(index, it.value.toInt()) },
                    onMaxSelected = { setOaoMax(index, it.value.toInt()) }
                )
            }
        }
    }
}
