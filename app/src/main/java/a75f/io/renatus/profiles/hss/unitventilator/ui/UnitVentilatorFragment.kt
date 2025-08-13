package a75f.io.renatus.profiles.hss.unitventilator.ui

import a75f.io.logic.bo.building.statprofiles.hyperstatsplit.profiles.unitventilator.Pipe4UvAnalogOutControls
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.compose.StyledTextView
import a75f.io.renatus.profiles.hss.HyperStatSplitFragment
import a75f.io.renatus.profiles.hss.unitventilator.viewmodels.Pipe4ViewModel
import a75f.io.renatus.profiles.hss.unitventilator.viewmodels.UnitVentilatorViewModel
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

    @Composable
    fun AnalogOutDynamicConfig(viewModel: UnitVentilatorViewModel, modifier: Modifier = Modifier) {
        FanControl(viewModel,modifier)
        FaceAndBypassDamper(viewModel,modifier)
        OAODamperControl(viewModel,modifier)
        DcvModulationControl(viewModel,modifier)
        CoolingControl(viewModel,modifier)
        HeatingControl(viewModel,modifier)
        FanSpeed(viewModel)
    }

    @Composable
    fun GenericView(viewModel: UnitVentilatorViewModel){
        Title(viewModel)
        ControlVia()
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
        AnalogOutDynamicConfig(viewModel)
        OAODamperConfig(viewModel)
        DividerRow()
    }

    @Composable
    fun ControlVia() {
        Column(
            modifier = Modifier.padding(start = 25.dp, top = 55.dp, bottom = 25.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = AbsoluteAlignment.Left
        )
        {
            DropDownWithLabel(
                "Control Via :",
                viewModel.controlViaList,
                onSelected = { (viewModel.viewState.value as Pipe4UvViewState).controlVia = it },
                defaultSelection = (viewModel.viewState.value as Pipe4UvViewState).controlVia,
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
            is Pipe4ViewModel -> Pipe4UvAnalogOutControls.FAN_SPEED.ordinal
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

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 25.dp, bottom = 10.dp),
            maxItemsInEachRow = 4
        ) {
            var nEntries = 0

            if (viewModel.isAnalogEnabledAndMapped(
                    viewModel.viewState.value.analogOut1Enabled,
                    viewModel.viewState.value.analogOut1Association,
                    Pipe4UvAnalogOutControls.FAN_SPEED.ordinal
                )
            ) {
                nEntries += 3
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out1 at Fan Low\n",
                        fontSize = 20,
                        textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.fanAtLow.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.fanAtLow =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp, start = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out1 at Fan Medium\n",
                        fontSize = 20,
                        textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.fanAtMedium.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.fanAtMedium =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out1 at Fan High\n",
                        fontSize = 20,
                        textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.fanAtHigh.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.fanAtHigh =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                    Box(modifier = Modifier.weight(2f))
                    Box(modifier = Modifier.weight(1f))


            }

            if ((viewModel.isAnalogEnabledAndMapped(
                    viewModel.viewState.value.analogOut2Enabled,
                    viewModel.viewState.value.analogOut2Association,
                    Pipe4UvAnalogOutControls.FAN_SPEED.ordinal
                ))
            ) {
                nEntries += 3
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out2 at Fan Low\n",
                        fontSize = 20,
                        textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.fanAtLow.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.fanAtLow =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp, start = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out2 at Fan Medium\n",
                        fontSize = 20,
                        textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.fanAtMedium.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.fanAtMedium =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out2 at Fan High\n",
                        fontSize = 20,
                        textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.fanAtHigh.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.fanAtHigh =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                    Box(modifier = Modifier.weight(2f))
                    Box(modifier = Modifier.weight(1f))

            }

            if ((viewModel.isAnalogEnabledAndMapped(
                    viewModel.viewState.value.analogOut3Enabled,
                    viewModel.viewState.value.analogOut3Association,
                    Pipe4UvAnalogOutControls.FAN_SPEED.ordinal
                ))
            ) {
                nEntries += 3
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out3 at Fan Low\n",
                        fontSize = 20,
                        textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.fanAtLow.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.fanAtLow =
                                it.value.toInt()
                        }, previewWidth = 70

                    )
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp, start = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out3 at Fan Medium\n",
                        fontSize = 20,
                        textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.fanAtMedium.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.fanAtMedium =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out3 at Fan High\n",
                        fontSize = 20,
                        textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.fanAtHigh.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.fanAtHigh =
                                it.value.toInt()
                        }, previewWidth = 70

                    )
                }

                    Box(modifier = Modifier.weight(2f))
                   Box(modifier = Modifier.weight(1f))

            }

            if ((viewModel.isAnalogEnabledAndMapped(
                    viewModel.viewState.value.analogOut4Enabled,
                    viewModel.viewState.value.analogOut4Association,
                    Pipe4UvAnalogOutControls.FAN_SPEED.ordinal
                ))
            ) {
                nEntries += 3
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out4 at Fan Low\n",
                        fontSize = 20,
                        textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.fanAtLow.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.fanAtLow =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp, start = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out4 at Fan Medium\n",
                        fontSize = 20,
                        textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.fanAtMedium.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.fanAtMedium =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(2f)
                        .padding(top = 10.dp, bottom = 10.dp)
                ) {
                    StyledTextView(
                        "Analog-out4 at Fan High\n",
                        fontSize = 20,
                        textAlignment = TextAlign.Left
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SpinnerElementOption(
                        defaultSelection = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.fanAtHigh.toString(),
                        items = viewModel.testVoltage,
                        unit = "%",
                        itemSelected = {
                            (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.fanAtHigh =
                                it.value.toInt()
                        },
                        previewWidth = 70
                    )
                }


                    Box(modifier = Modifier.weight(2f))
                    Box(modifier = Modifier.weight(1f))

              
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
                                .padding(top = 10.dp, bottom = 10.dp,start = 10.dp)
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
                                .padding(top = 10.dp, bottom = 20.dp,start = 10.dp)
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
                                .padding(top = 10.dp, bottom = 20.dp,start = 10.dp)
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
                            .padding(top = 10.dp, bottom = 20.dp,start = if(nEntries % 2 == 0) 10.dp else 0.dp)
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
                            .padding(top = 10.dp, bottom = 20.dp,start = if(nEntries % 2 == 1) 10.dp else 0.dp)
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
                            .padding(top = 10.dp, bottom = 20.dp , start = if(nEntries % 2 == 1) 0.dp else 10.dp)
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

        if (viewModel.isAnalogEnabledAndMapped(viewModel.viewState.value.analogOut1Enabled,viewModel.viewState.value.analogOut1Association,enumValue)
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nFan Speed",
                "Analog-out1 at Max \n Fan Speed",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.fanMin.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.fanMax.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.fanMin = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.fanMax = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association,
                 enumValue
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nFan Speed",
                "Analog-out2 at Max \nFan Speed",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.fanMin.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.fanMax.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.fanMin = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.fanMax = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association,
                enumValue
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nFan Speed",
                "Analog-out3 at Max \nFan Speed",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.fanMin.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.fanMax.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.fanMin = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.fanMax = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association,
                enumValue
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nFan Speed",
                "Analog-out4 at Max \nFan Speed",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.fanMin.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.fanMax.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.fanMin = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.fanMax = it.value.toInt()
                }
            )
        }
    }

    @Composable
    fun CoolingControl(viewModel: UnitVentilatorViewModel, modifier: Modifier = Modifier) {
        val key = when(viewModel){
            is Pipe4ViewModel -> Pipe4UvAnalogOutControls.COOLING_WATER_MODULATING_VALVE.ordinal
            else -> { Pipe4UvAnalogOutControls.COOLING_WATER_MODULATING_VALVE.ordinal}
        }
        Column(modifier = modifier) {
            if (viewModel.isCoolingAOEnabled(key)) {
                EnableCoolingVoltage(viewModel, key)
            }
        }
    }



    @Composable
    fun EnableCoolingVoltage(viewModel: UnitVentilatorViewModel, type: Int) {
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nCooling Modulating Valve",
                "Analog-out1 at Max \nCooling Modulating Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.coolingWaterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.coolingWaterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.coolingWaterValveMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState ).analogOut1MinMax.coolingWaterValveMaxVoltage = it.value.toInt()
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nCooling Modulating Valve",
                "Analog-out2 at Max \nCooling Modulating Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState ).analogOut2MinMax.coolingWaterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState ).analogOut2MinMax.coolingWaterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.coolingWaterValveMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.coolingWaterValveMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nCooling Modulating Valve",
                "Analog-out3 at Max \nCooling Modulating Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState ).analogOut3MinMax.coolingWaterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.coolingWaterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.coolingWaterValveMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.coolingWaterValveMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nCooling Modulating Valve",
                "Analog-out4 at Max \nCooling Modulating Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.coolingWaterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.coolingWaterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.coolingWaterValveMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.coolingWaterValveMaxVoltage = it.value.toInt()
                }
            )
        }
    }

    @Composable
    fun HeatingControl(viewModel: UnitVentilatorViewModel, modifier: Modifier = Modifier) {

        val key = when(viewModel){
            is Pipe4ViewModel -> Pipe4UvAnalogOutControls.HEATING_WATER_MODULATING_VALVE.ordinal
            else -> { Pipe4UvAnalogOutControls.HEATING_WATER_MODULATING_VALVE.ordinal}
        }
        Column(modifier = modifier) {
            if (viewModel.isHeatingAOEnabled(key)) {
                EnableHeatingVoltage(viewModel, key)
            }
        }
    }

    @Composable
    fun EnableHeatingVoltage(viewModel: UnitVentilatorViewModel, type: Int) {
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nHeating Modulating Valve",
                "Analog-out1 at Max \nHeating Modulating Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value  as Pipe4UvViewState).analogOut1MinMax.hotWaterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.hotWaterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.hotWaterValveMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.hotWaterValveMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nHeating Modulating Valve",
                "Analog-out2 at Max \nHeating Modulating Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.hotWaterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.hotWaterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.hotWaterValveMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.hotWaterValveMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nHeating Modulating Valve",
                "Analog-out3 at Max \nHeating Modulating Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.hotWaterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.hotWaterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState ).analogOut3MinMax.hotWaterValveMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.hotWaterValveMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nHeating Modulating Valve",
                "Analog-out4 at Max \nHeating Modulating Valve",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.hotWaterValveMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.hotWaterValveMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.hotWaterValveMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.hotWaterValveMaxVoltage = it.value.toInt()
                }
            )
        }
    }

    @Composable
    fun FaceAndBypassDamper(viewModel: UnitVentilatorViewModel,modifier: Modifier = Modifier) {
        val key = when(viewModel){
            is Pipe4ViewModel -> Pipe4UvAnalogOutControls.FACE_DAMPER_VALVE.ordinal
            else -> { Pipe4UvAnalogOutControls.FACE_DAMPER_VALVE.ordinal}
        }
        Column(modifier = modifier) {
            EnableFaceAndBypassDamper(viewModel,key)
        }
    }

    @Composable
    fun EnableFaceAndBypassDamper(viewModel: UnitVentilatorViewModel, type: Int) {
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nFace & Bypass Damper",
                "Analog-out1 at Max \nFace & Bypass Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.faceAndBypassDamperMin.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.faceAndBypassDamperMax.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.faceAndBypassDamperMin = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.faceAndBypassDamperMax = it.value.toInt()
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nFace & Bypass Damper",
                "Analog-out2 at Max \nFace & Bypass Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.faceAndBypassDamperMin.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.faceAndBypassDamperMax.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.faceAndBypassDamperMin = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.faceAndBypassDamperMax = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nFace & Bypass Damper",
                "Analog-out3 at Max \nFace & Bypass Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.faceAndBypassDamperMin.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.faceAndBypassDamperMax.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.faceAndBypassDamperMin = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.faceAndBypassDamperMax = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nFace & Bypass Damper",
                "Analog-out4 at Max \nFace & Bypass Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.faceAndBypassDamperMin.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.faceAndBypassDamperMax.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.faceAndBypassDamperMin = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.faceAndBypassDamperMax = it.value.toInt()
                }
            )
        }
    }

    @Composable
    fun DcvModulationControl(viewModel: UnitVentilatorViewModel,modifier: Modifier = Modifier) {
        val key = when(viewModel){
            is Pipe4ViewModel -> Pipe4UvAnalogOutControls.DCV_MODULATING_DAMPER.ordinal
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
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nDCV Modulation Damper",
                "Analog-out1 at Max \nDCV Modulation Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.dcvModulationMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.dcvModulationMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.dcvModulationMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.dcvModulationMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nDCV Modulation Damper",
                "Analog-out2 at Max \nDCV Modulation Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.dcvModulationMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.dcvModulationMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.dcvModulationMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.dcvModulationMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nDCV Modulation Damper",
                "Analog-out3 at Max \nDCV Modulation Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.dcvModulationMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.dcvModulationMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.dcvModulationMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.dcvModulationMaxVoltage = it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(

                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nDCV Modulation Damper",
                "Analog-out4 at Max \nDCV Modulation Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.dcvModulationMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.dcvModulationMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.dcvModulationMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.dcvModulationMaxVoltage = it.value.toInt()
                }
            )
        }
    }
    @Composable
    fun OAODamperControl(viewModel: UnitVentilatorViewModel,modifier: Modifier = Modifier) {
        val key = when(viewModel){
            is Pipe4ViewModel -> Pipe4UvAnalogOutControls.OAO_DAMPER.ordinal
            else -> { Pipe4UvAnalogOutControls.DCV_MODULATING_DAMPER.ordinal}
        }
        Column(modifier = modifier) {
            if (viewModel.isOAODamperAOEnabled(key)) {
                EnableOAODamperVoltage(viewModel, key)
            }
        }
    }

    @Composable
    fun EnableOAODamperVoltage(viewModel:UnitVentilatorViewModel, type: Int) {
        if (viewModel.isAnalogEnabledAndMapped(
                
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nOAO Damper",
                "Analog-out1 at Max \nOAO Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.oaoDamperMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.oaoDamperMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.oaoDamperMinVoltage =
                        it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut1MinMax.oaoDamperMaxVoltage =
                        it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association,
                type
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nOAO Damper",
                "Analog-out2 at Max \nOAO Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.oaoDamperMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.oaoDamperMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.oaoDamperMinVoltage =
                        it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut2MinMax.oaoDamperMaxVoltage =
                        it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association,
                type,
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nOAO Damper",
                "Analog-out3 at Max \nOAO Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.oaoDamperMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.oaoDamperMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.oaoDamperMinVoltage =
                        it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut3MinMax.oaoDamperMaxVoltage =
                        it.value.toInt()
                }
            )
        }
        if (viewModel.isAnalogEnabledAndMapped(
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association,type

            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nOAO Damper",
                "Analog-out4 at Max \nOAO Damper",
                viewModel.minMaxVoltage,
                "V",
                minDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.oaoDamperMinVoltage.toString(),
                maxDefault = (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.oaoDamperMaxVoltage.toString(),
                onMinSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.oaoDamperMinVoltage =
                        it.value.toInt()
                },
                onMaxSelected = {
                    (viewModel.viewState.value as Pipe4UvViewState).analogOut4MinMax.oaoDamperMaxVoltage =
                        it.value.toInt()
                }
            )
        }
    }
}
