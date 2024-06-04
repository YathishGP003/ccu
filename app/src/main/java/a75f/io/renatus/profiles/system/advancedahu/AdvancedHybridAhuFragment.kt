package a75f.io.renatus.profiles.system.advancedahu

import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.renatus.R
import a75f.io.renatus.composables.AOTHConfig
import a75f.io.renatus.composables.AnalogOutConfig
import a75f.io.renatus.composables.ConfigCompose
import a75f.io.renatus.composables.EnableCompose
import a75f.io.renatus.composables.HumidityCompose
import a75f.io.renatus.composables.MinMaxConfiguration
import a75f.io.renatus.composables.RelayConfiguration
import a75f.io.renatus.compose.BoldStyledTextView
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.SaveTextViewNew
import a75f.io.renatus.compose.SaveTextViewNewExtraBold
import a75f.io.renatus.compose.SearchSpinnerElement
import a75f.io.renatus.compose.SpinnerElementOption
import a75f.io.renatus.compose.StyledTextView
import a75f.io.renatus.compose.SubTitle
import a75f.io.renatus.profiles.system.ADDRESS_0
import a75f.io.renatus.profiles.system.ADDRESS_1
import a75f.io.renatus.profiles.system.ADDRESS_2
import a75f.io.renatus.profiles.system.ADDRESS_3
import a75f.io.renatus.profiles.system.ANALOG_IN1
import a75f.io.renatus.profiles.system.ANALOG_IN2
import a75f.io.renatus.profiles.system.CM
import a75f.io.renatus.profiles.system.CO2
import a75f.io.renatus.profiles.system.CO2_DAMPER_CONTROL
import a75f.io.renatus.profiles.system.CO2_DAMPER_CONTROL_ON
import a75f.io.renatus.profiles.system.CO2_TARGET
import a75f.io.renatus.profiles.system.CO2_THRESHOLD
import a75f.io.renatus.profiles.system.COMPOSITE_CONTROL
import a75f.io.renatus.profiles.system.CONNECT_MODULE
import a75f.io.renatus.profiles.system.COOLING_CONTROL
import a75f.io.renatus.profiles.system.ENABLE
import a75f.io.renatus.profiles.system.FAN_CONTROL
import a75f.io.renatus.profiles.system.HEATING_CONTROL
import a75f.io.renatus.profiles.system.HUMIDITY
import a75f.io.renatus.profiles.system.MAPPING
import a75f.io.renatus.profiles.system.OCCUPANCY
import a75f.io.renatus.profiles.system.OPENING_RATE
import a75f.io.renatus.profiles.system.PRESSURE
import a75f.io.renatus.profiles.system.PRESSURE_BASED_FC
import a75f.io.renatus.profiles.system.PRESSURE_BASED_FC_ON
import a75f.io.renatus.profiles.system.SAT_CONTROL
import a75f.io.renatus.profiles.system.SAT_CONTROL_ON
import a75f.io.renatus.profiles.system.SENSOR_BUS
import a75f.io.renatus.profiles.system.SP_MAX
import a75f.io.renatus.profiles.system.SP_MIN
import a75f.io.renatus.profiles.system.SYSTEM_COOLING_SAT_MAX
import a75f.io.renatus.profiles.system.SYSTEM_COOLING_SAT_MIN
import a75f.io.renatus.profiles.system.SYSTEM_HEATING_SAT_MAX
import a75f.io.renatus.profiles.system.SYSTEM_HEATING_SAT_MIN
import a75f.io.renatus.profiles.system.TEMPERATURE
import a75f.io.renatus.profiles.system.TEST_SIGNAL
import a75f.io.renatus.profiles.system.THERMISTOR_1
import a75f.io.renatus.profiles.system.THERMISTOR_2
import a75f.io.renatus.profiles.system.UNIVERSAL_IN1
import a75f.io.renatus.profiles.system.UNIVERSAL_IN2
import a75f.io.renatus.profiles.system.UNIVERSAL_IN3
import a75f.io.renatus.profiles.system.UNIVERSAL_IN4
import a75f.io.renatus.profiles.system.UNIVERSAL_IN5
import a75f.io.renatus.profiles.system.UNIVERSAL_IN6
import a75f.io.renatus.profiles.system.UNIVERSAL_IN7
import a75f.io.renatus.profiles.system.UNIVERSAL_IN8
import android.os.Bundle
import android.view.View
import android.view.View.OnAttachStateChangeListener
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment

/**
 * Created by Manjunath K on 14-03-2024.
 */

open class AdvancedHybridAhuFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
            }
            override fun onViewDetachedFromWindow(view: View) {
                if (Globals.getInstance().isTestMode) {
                    Globals.getInstance().isTestMode = false
                }
            }
        })
    }

    @Composable
    fun TitleLabel() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            Box(
                modifier = Modifier.weight(0.5f), contentAlignment = Alignment.Center
            ) { SubTitle(CM) }
            Box(modifier = Modifier.weight(3f), contentAlignment = Alignment.Center) {
                SubTitle(
                    ENABLE
                )
            }
            Box(modifier = Modifier.weight(1.5f), contentAlignment = Alignment.Center) {
                SubTitle(
                    MAPPING
                )
            }
            Box(modifier = Modifier.weight(1.5f), contentAlignment = Alignment.Center) {
                SubTitle(
                    TEST_SIGNAL
                )
            }
        }
    }

    @Composable
    fun ConnectModule1(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.viewState.value.isConnectEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Box(modifier = Modifier.padding(top = 7.dp)) {
                    HeaderTextView(text = CONNECT_MODULE)
                }
                SaveTextViewNewExtraBold(text = "x") { viewModel.viewState.value.pendingDeleteConnect = true }
                Spacer(Modifier.weight(1f))
                Box(modifier = Modifier.padding(top = 12.dp, end = 30.dp)) {
                    StyledTextView("Address: " + viewModel.viewState.value.connectAddress.toString(), fontSize = 20)
                }
            }
        }
    }

    /**
     * This function is used to display the Sensor Bus configurations
     */
    @Composable
    fun CMSensorConfig(viewModel: AdvancedHybridAhuViewModel) {
        val temperatureEnums = viewModel.getAllowedValues(DomainName.temperatureSensorBusAdd1, viewModel.cmModel)
        val humidityEnums = viewModel.getAllowedValues(DomainName.humiditySensorBusAdd1, viewModel.cmModel)
        val occupancyEnum = viewModel.getAllowedValues(DomainName.occupancySensorBusAdd0, viewModel.cmModel)
        val co2Enum = viewModel.getAllowedValues(DomainName.co2SensorBusAdd0, viewModel.cmModel)
        val pressureEnum = viewModel.getAllowedValues(DomainName.pressureSensorBusAdd0, viewModel.cmModel)

        SubTitle(SENSOR_BUS)
        Row {
            EnableCompose(ADDRESS_0, viewModel.viewState.value.sensorAddress0.enabled) {
                viewModel.viewState.value.sensorAddress0.enabled = it
            }
            Column {
                ConfigCompose(
                    TEMPERATURE,
                    temperatureEnums[viewModel.viewState.value.sensorAddress0.temperatureAssociation].value,
                    temperatureEnums,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress0.enabled
                ) {
                    viewModel.viewState.value.sensorAddress0.temperatureAssociation = it.index
                    viewModel.viewState.value.sensorAddress0.humidityAssociation = it.index
                }
                HumidityCompose(
                    HUMIDITY,
                    humidityEnums[viewModel.viewState.value.sensorAddress0.humidityAssociation].value
                )
                ConfigCompose(
                    OCCUPANCY,
                    occupancyEnum[viewModel.viewState.value.sensorAddress0.occupancyAssociation].value,
                    occupancyEnum,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress0.enabled
                ) {
                    viewModel.viewState.value.sensorAddress0.occupancyAssociation = it.index
                }
                ConfigCompose(
                    CO2,
                    co2Enum[viewModel.viewState.value.sensorAddress0.co2Association].value,
                    co2Enum,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress0.enabled
                ) {
                    viewModel.viewState.value.sensorAddress0.co2Association = it.index
                }
            }
        }
        Row {
            EnableCompose(
                "", viewModel.viewState.value.sensorBusPressureEnable, hide = true
            ) {
                viewModel.viewState.value.sensorBusPressureEnable = it
            }
            Column {
                ConfigCompose(
                    PRESSURE,
                    pressureEnum[viewModel.viewState.value.sensorAddress0.pressureAssociation].value,
                    pressureEnum,
                    "",
                    isEnabled = viewModel.viewState.value.sensorBusPressureEnable
                ) {
                    viewModel.viewState.value.sensorAddress0.pressureAssociation = it.index
                }
            }
        }

        Row {
            EnableCompose(ADDRESS_1, viewModel.viewState.value.sensorAddress1.enabled) {
                viewModel.viewState.value.sensorAddress1.enabled = it
            }
            Column {
                ConfigCompose(
                    TEMPERATURE,
                    temperatureEnums[viewModel.viewState.value.sensorAddress1.temperatureAssociation].value,
                    temperatureEnums,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress1.enabled
                ) {
                    viewModel.viewState.value.sensorAddress1.temperatureAssociation = it.index
                    viewModel.viewState.value.sensorAddress1.humidityAssociation = it.index
                }
                HumidityCompose(
                    HUMIDITY,
                    humidityEnums[viewModel.viewState.value.sensorAddress1.humidityAssociation].value
                )
                ConfigCompose(
                    OCCUPANCY,
                    occupancyEnum[viewModel.viewState.value.sensorAddress1.occupancyAssociation].value,
                    occupancyEnum,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress1.enabled
                ) {
                    viewModel.viewState.value.sensorAddress1.occupancyAssociation = it.index
                }
                ConfigCompose(
                    CO2,
                    co2Enum[viewModel.viewState.value.sensorAddress1.co2Association].value,
                    co2Enum,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress1.enabled
                ) {
                    viewModel.viewState.value.sensorAddress1.co2Association = it.index
                }
            }
        }
        Row {
            EnableCompose(ADDRESS_2, viewModel.viewState.value.sensorAddress2.enabled) {
                viewModel.viewState.value.sensorAddress2.enabled = it
            }
            Column {
                ConfigCompose(
                    TEMPERATURE,
                    temperatureEnums[viewModel.viewState.value.sensorAddress2.temperatureAssociation].value,
                    temperatureEnums,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress2.enabled
                ) {
                    viewModel.viewState.value.sensorAddress2.temperatureAssociation = it.index
                    viewModel.viewState.value.sensorAddress2.humidityAssociation = it.index
                }
                HumidityCompose(
                    HUMIDITY,
                    humidityEnums[viewModel.viewState.value.sensorAddress2.humidityAssociation].value
                )
                ConfigCompose(
                    OCCUPANCY,
                    occupancyEnum[viewModel.viewState.value.sensorAddress2.occupancyAssociation].value,
                    occupancyEnum,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress2.enabled
                ) {
                    viewModel.viewState.value.sensorAddress2.occupancyAssociation = it.index
                }
                ConfigCompose(
                    CO2,
                    co2Enum[viewModel.viewState.value.sensorAddress2.co2Association].value,
                    co2Enum,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress2.enabled
                ) {
                    viewModel.viewState.value.sensorAddress2.co2Association = it.index
                }
            }
        }
        Row {
            EnableCompose(ADDRESS_3, viewModel.viewState.value.sensorAddress3.enabled) {
                viewModel.viewState.value.sensorAddress3.enabled = it
            }
            Column {
                ConfigCompose(
                    TEMPERATURE,
                    temperatureEnums[viewModel.viewState.value.sensorAddress3.temperatureAssociation].value,
                    temperatureEnums,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress3.enabled
                ) {
                    viewModel.viewState.value.sensorAddress3.temperatureAssociation = it.index
                    viewModel.viewState.value.sensorAddress3.humidityAssociation = it.index
                }
                HumidityCompose(
                    HUMIDITY,
                    humidityEnums[viewModel.viewState.value.sensorAddress3.humidityAssociation].value

                )
                ConfigCompose(
                    OCCUPANCY,
                    occupancyEnum[viewModel.viewState.value.sensorAddress3.occupancyAssociation].value,
                    occupancyEnum,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress3.enabled
                ) {
                    viewModel.viewState.value.sensorAddress3.occupancyAssociation = it.index
                }
                ConfigCompose(
                    CO2,
                    co2Enum[viewModel.viewState.value.sensorAddress3.co2Association].value,
                    co2Enum,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress3.enabled
                ) {
                    viewModel.viewState.value.sensorAddress3.co2Association = it.index
                }
            }
        }
    }


    /**
     * This function is used to display the Analog Input and Thermistor configurations
     */
    @Composable
    fun CMAnalogInThIn(viewModel: AdvancedHybridAhuViewModel) {

        /**
         * By assuming the allowed values are same all the Analog Inputs are same, Similarly for Thermistor also
         */
        val analogEnum = viewModel.getAllowedValues(DomainName.analog1InputAssociation, viewModel.cmModel)
        val thEnum = viewModel.getAllowedValues(DomainName.thermistor1InputAssociation, viewModel.cmModel)

        Row(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.g1),
                contentDescription = "Analog in Thermistor",
                modifier = Modifier
                    .weight(1.5f)
                    .padding(top = 25.dp)
                    .height(240.dp)
            )
            Column(
                modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)
            ) {
                AOTHConfig(ANALOG_IN1,
                    viewModel.viewState.value.analogIn1Config.enabled,
                    { viewModel.viewState.value.analogIn1Config.enabled = it },
                    analogEnum[viewModel.viewState.value.analogIn1Config.association].value,
                    analogEnum,
                    "",
                    viewModel.viewState.value.analogIn1Config.enabled,
                    { viewModel.viewState.value.analogIn1Config.association = it.index })
                AOTHConfig(ANALOG_IN2,
                    viewModel.viewState.value.analogIn2Config.enabled,
                    { viewModel.viewState.value.analogIn2Config.enabled = it },
                    analogEnum[viewModel.viewState.value.analogIn2Config.association].value,
                    analogEnum,
                    "",
                    viewModel.viewState.value.analogIn2Config.enabled,
                    { viewModel.viewState.value.analogIn2Config.association = it.index })

                AOTHConfig(THERMISTOR_1,
                    viewModel.viewState.value.thermistor1Config.enabled,
                    { viewModel.viewState.value.thermistor1Config.enabled = it },
                    thEnum[viewModel.viewState.value.thermistor1Config.association].value,
                    thEnum,
                    "",
                    viewModel.viewState.value.thermistor1Config.enabled,
                    { viewModel.viewState.value.thermistor1Config.association = it.index })
                AOTHConfig(THERMISTOR_2,
                    viewModel.viewState.value.thermistor2Config.enabled,
                    { viewModel.viewState.value.thermistor2Config.enabled = it },
                    thEnum[viewModel.viewState.value.thermistor2Config.association].value,
                    thEnum,
                    "",
                    viewModel.viewState.value.thermistor2Config.enabled,
                    { viewModel.viewState.value.thermistor2Config.association = it.index })

            }
        }
    }

    /**
     * This function is used to display the Relay configurations
     */
    @Composable
    fun CMRelayConfig(viewModel: AdvancedHybridAhuViewModel) {
        val relayEnums = viewModel.getAllowedValues(DomainName.relay1OutputAssociation, viewModel.cmModel)

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Display relay image
            Image(
                painter = painterResource(id = R.drawable.relay),
                contentDescription = "Relays",
                modifier = Modifier
                    .weight(1.5f)
                    .padding(top = 35.dp)
                    .height(505.dp)
            )

            // Display relay configurations
            Column(
                modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)
            ) {
                repeat(8) { index ->
                    val relayConfig = when (index) {
                        0 -> viewModel.viewState.value.relay1Config
                        1 -> viewModel.viewState.value.relay2Config
                        2 -> viewModel.viewState.value.relay3Config
                        3 -> viewModel.viewState.value.relay4Config
                        4 -> viewModel.viewState.value.relay5Config
                        5 -> viewModel.viewState.value.relay6Config
                        6 -> viewModel.viewState.value.relay7Config
                        7 -> viewModel.viewState.value.relay8Config
                        else -> throw IllegalArgumentException("Invalid relay index: $index")
                    }

                    RelayConfiguration(relayName = "Relay ${index + 1}",
                        enabled = relayConfig.enabled,
                        onEnabledChanged = {
                            if (!it) {
                                relayConfig.association = viewModel.getModelDefaultValue(relayConfig)
                            }
                            relayConfig.enabled = it
                        },
                        association = relayEnums[relayConfig.association].value,
                        unit = "",
                        relayEnums = relayEnums,
                        onAssociationChanged = { associationIndex ->
                            relayConfig.association = associationIndex.index
                        },
                        isEnabled = relayConfig.enabled,
                        testState = viewModel.getPhysicalPointForRelayIndex(index, false)?.let { it.readHisVal() > 0 } ?: false,
                        onTestActivated = {viewModel.sendCMRelayTestCommand(index, it)})
                }
            }
        }
    }

    /**
     * This function is used to display the Analog Out configurations
     */
    @Composable
    fun CMAnalogOutConfig(viewModel: AdvancedHybridAhuViewModel) {
        val analogOutEnums = viewModel.getAllowedValues(DomainName.analog1OutputAssociation, viewModel.cmModel)

        Row(modifier = Modifier.fillMaxWidth()) {
            // Display analog out image
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .padding(top = 10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.analog_out),
                    contentDescription = "Analog Out",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .height(240.dp)
                )
            }

            // Display analog out configurations
            Column(
                modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)
            ) {
                repeat(4) { index ->
                    val analogOut = "Analog-out${index + 1}"
                    val enabled = when (index) {
                        0 -> viewModel.viewState.value.analogOut1Enabled
                        1 -> viewModel.viewState.value.analogOut2Enabled
                        2 -> viewModel.viewState.value.analogOut3Enabled
                        3 -> viewModel.viewState.value.analogOut4Enabled
                        else -> false
                    }
                    val associationIndex = when (index) {
                        0 -> viewModel.viewState.value.analogOut1Association
                        1 -> viewModel.viewState.value.analogOut2Association
                        2 -> viewModel.viewState.value.analogOut3Association
                        3 -> viewModel.viewState.value.analogOut4Association
                        else -> 0
                    }

                    AnalogOutConfig(analogOutName = analogOut,
                        enabled = enabled,
                        onEnabledChanged = { enabledAction ->
                            when (index) {
                                0 -> viewModel.viewState.value.analogOut1Enabled = enabledAction
                                1 -> viewModel.viewState.value.analogOut2Enabled = enabledAction
                                2 -> viewModel.viewState.value.analogOut3Enabled = enabledAction
                                3 -> viewModel.viewState.value.analogOut4Enabled = enabledAction
                            }
                        },
                        association = analogOutEnums[associationIndex].value,
                        analogOutEnums = analogOutEnums,
                        testSingles = viewModel.testVoltage,
                        isEnabled = enabled,
                        onAssociationChanged = { association ->
                            when (index) {
                                0 -> viewModel.viewState.value.analogOut1Association =
                                    association.index

                                1 -> viewModel.viewState.value.analogOut2Association =
                                    association.index

                                2 -> viewModel.viewState.value.analogOut3Association =
                                    association.index

                                3 -> viewModel.viewState.value.analogOut4Association =
                                    association.index
                            }
                        },
                        testVal = viewModel.getPhysicalPointForAnalogIndex(index)?.readHisVal()?.toInt() ?: 0,
                        onTestSignalSelected = {viewModel.sendCMAnalogTestCommand(index, it)})
                }
            }
        }
    }


    /**
     * This function is used to display the dynamic configuration for the Advanced AHU
     */
    @Composable
    fun CMAnalogOutDynamicConfig(viewModel: AdvancedHybridAhuViewModel) {
        PressureBasedFanControl(viewModel)
        SATBasedControl(viewModel)
        CO2BasedDamperControl(viewModel)
        LoadBasedCoolingControl(viewModel)
        LoadBasedFanControl(viewModel)
        HeatLoadBasedControl(viewModel)
        CompositeBasedControl(viewModel)
        viewModel.modelLoaded = true
    }

    @Composable
    fun PressureBasedFanControl(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isPressureEnabled()) {
            val pressureEnum = viewModel.getAllowedValues(DomainName.pressureBasedFanControlOn, viewModel.cmModel)
            val pressureMinMaxList = viewModel.getListByDomainName(DomainName.staticPressureMin, viewModel.cmModel)
            val unit = viewModel.getUnit(DomainName.staticPressureMin, viewModel.cmModel)

            Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
                BoldStyledTextView(PRESSURE_BASED_FC, fontSize = 20)
                Row(modifier = Modifier.padding(top = 10.dp, bottom = 15.dp)) {
                    Box(modifier = Modifier.weight(1f).padding(top = 10.dp)) {
                        StyledTextView(
                            PRESSURE_BASED_FC_ON, fontSize = 20, textAlignment = TextAlign.Left
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SearchSpinnerElement(
                            default = pressureEnum[viewModel.viewState.value.pressureConfig.pressureControlAssociation].value,
                            allItems = pressureEnum,
                            unit = "",
                            onSelect = {
                                viewModel.viewState.value.pressureConfig.pressureControlAssociation =
                                    it.index
                            },
                            width = 350
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) { }
                }
                MinMaxConfiguration(SP_MIN,
                    SP_MAX,
                    pressureMinMaxList,
                    unit,
                    minDefault = viewModel.viewState.value.pressureConfig.staticMinPressure.toString(),
                    maxDefault = viewModel.viewState.value.pressureConfig.staticMaxPressure.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.pressureConfig.staticMinPressure =
                            it.value.toDouble()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.pressureConfig.staticMaxPressure =
                            it.value.toDouble()
                    })
            }
            PressureBaseMinMaxConfig(viewModel)
        }
    }

    @Composable
    fun PressureBaseMinMaxConfig(viewModel: AdvancedHybridAhuViewModel) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.PRESSURE_BASED_FAN_CONTROL,
                    viewModel.viewState.value.analogOut1Enabled,
                    viewModel.viewState.value.analogOut1Association
                )
            ) {
                MinMaxConfiguration("Analog-out1 at Min \nStatic Pressure",
                    "Analog-out1 at Max \nStatic Pressure",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut1MinMax.staticPressureMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut1MinMax.staticPressureMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut1MinMax.staticPressureMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut1MinMax.staticPressureMaxVoltage =
                            it.value.toInt()
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.PRESSURE_BASED_FAN_CONTROL,
                    viewModel.viewState.value.analogOut2Enabled,
                    viewModel.viewState.value.analogOut2Association
                )
            ) {
                MinMaxConfiguration("Analog-out2 at Min \nStatic Pressure",
                    "Analog-out2 at Max \nStatic Pressure",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut2MinMax.staticPressureMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut2MinMax.staticPressureMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut2MinMax.staticPressureMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut2MinMax.staticPressureMaxVoltage =
                            it.value.toInt()
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.PRESSURE_BASED_FAN_CONTROL,
                    viewModel.viewState.value.analogOut3Enabled,
                    viewModel.viewState.value.analogOut3Association
                )
            ) {
                MinMaxConfiguration("Analog-out3 at Min \nStatic Pressure",
                    "Analog-out3 at Max \nStatic Pressure",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut3MinMax.staticPressureMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut3MinMax.staticPressureMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut3MinMax.staticPressureMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut3MinMax.staticPressureMaxVoltage =
                            it.value.toInt()
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.PRESSURE_BASED_FAN_CONTROL,
                    viewModel.viewState.value.analogOut4Enabled,
                    viewModel.viewState.value.analogOut4Association
                )
            ) {
                MinMaxConfiguration("Analog-out4 at Min \nStatic Pressure",
                    "Analog-out4 at Max \nStatic Pressure",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut4MinMax.staticPressureMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut4MinMax.staticPressureMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut4MinMax.staticPressureMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut4MinMax.staticPressureMaxVoltage =
                            it.value.toInt()
                    })
            }
        }
    }

    @Composable
    fun SATBasedControl(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isSATCoolingEnabled() || viewModel.isSATHeatingEnabled()) {
            val satEnum = viewModel.getAllowedValues(DomainName.supplyAirTempControlOn, viewModel.cmModel)
            val minMax = viewModel.getListByDomainName(DomainName.systemCoolingSatMin, viewModel.cmModel)
            val unit = viewModel.getUnit(DomainName.systemCoolingSatMin, viewModel.cmModel)

            Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
                BoldStyledTextView(SAT_CONTROL, fontSize = 20)
                Row(modifier = Modifier.padding(top = 10.dp, bottom = 15.dp)) {
                    Box(modifier = Modifier.weight(1f).padding(top = 10.dp)) {
                        StyledTextView(
                            SAT_CONTROL_ON, fontSize = 20, textAlignment = TextAlign.Left
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SearchSpinnerElement(
                            default = satEnum[viewModel.viewState.value.satConfig.satControlAssociation].value,
                            allItems = satEnum, unit = "", onSelect = {
                                viewModel.viewState.value.satConfig.satControlAssociation = it.index
                            }, width = 350
                        )
                    }
                    Box(modifier = Modifier.weight(1f))
                }
                if (viewModel.isSATCoolingEnabled()) {
                    MinMaxConfiguration(SYSTEM_COOLING_SAT_MIN,
                        SYSTEM_COOLING_SAT_MAX,
                        minMax,
                        unit,
                        minDefault = viewModel.viewState.value.satConfig.systemSatCoolingMin.toInt().toString(),
                        maxDefault = viewModel.viewState.value.satConfig.systemSatCoolingMax.toInt().toString(),
                        onMinSelected = {
                            viewModel.viewState.value.satConfig.systemSatCoolingMin =
                                it.value.toDouble()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.satConfig.systemSatCoolingMax =
                                it.value.toDouble()
                        })
                }
                if (viewModel.isSATHeatingEnabled()) {
                    MinMaxConfiguration(SYSTEM_HEATING_SAT_MIN,
                        SYSTEM_HEATING_SAT_MAX,
                        minMax,
                        unit,
                        minDefault = viewModel.viewState.value.satConfig.systemSatHeatingMin.toInt().toString(),
                        maxDefault = viewModel.viewState.value.satConfig.systemSatHeatingMax.toInt().toString(),
                        onMinSelected = {
                            viewModel.viewState.value.satConfig.systemSatHeatingMin =
                                it.value.toDouble()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.satConfig.systemSatHeatingMax =
                                it.value.toDouble()
                        })
                }
            }
            SATCoolingMinMaxConfig(viewModel)
            SATHeatingMinMaxConfig(viewModel)
        }
    }

    @Composable
    fun SATCoolingMinMaxConfig(viewModel: AdvancedHybridAhuViewModel) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.SAT_BASED_COOLING_CONTROL,
                    viewModel.viewState.value.analogOut1Enabled,
                    viewModel.viewState.value.analogOut1Association
                )
            ) {
                MinMaxConfiguration("Analog-out1 at Min \nSAT Cooling",
                    "Analog-out1 at Max \nSAT Cooling",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut1MinMax.satCoolingMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut1MinMax.satCoolingMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut1MinMax.satCoolingMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut1MinMax.satCoolingMaxVoltage =
                            it.value.toInt()
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.SAT_BASED_COOLING_CONTROL,
                    viewModel.viewState.value.analogOut2Enabled,
                    viewModel.viewState.value.analogOut2Association
                )
            ) {
                MinMaxConfiguration("Analog-out2 at Min \nSAT Cooling",
                    "Analog-out2 at Max \nSAT Cooling",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut2MinMax.satCoolingMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut2MinMax.satCoolingMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut2MinMax.satCoolingMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut2MinMax.satCoolingMaxVoltage =
                            it.value.toInt()
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.SAT_BASED_COOLING_CONTROL,
                    viewModel.viewState.value.analogOut3Enabled,
                    viewModel.viewState.value.analogOut3Association
                )
            ) {
                MinMaxConfiguration("Analog-out3 at Min \nSAT Cooling",
                    "Analog-out3 at Max \nSAT Cooling",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut3MinMax.satCoolingMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut3MinMax.satCoolingMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut3MinMax.satCoolingMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut3MinMax.satCoolingMaxVoltage =
                            it.value.toInt()
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.SAT_BASED_COOLING_CONTROL,
                    viewModel.viewState.value.analogOut4Enabled,
                    viewModel.viewState.value.analogOut4Association
                )
            ) {
                MinMaxConfiguration("Analog-out4 at Min \nSAT Cooling",
                    "Analog-out4 at Max \nSAT Cooling",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut4MinMax.satCoolingMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut4MinMax.satCoolingMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut4MinMax.satCoolingMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut4MinMax.satCoolingMaxVoltage =
                            it.value.toInt()
                    })
            }
        }
    }

    @Composable
    fun SATHeatingMinMaxConfig(viewModel: AdvancedHybridAhuViewModel) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.SAT_BASED_HEATING_CONTROL,
                    viewModel.viewState.value.analogOut1Enabled,
                    viewModel.viewState.value.analogOut1Association
                )
            ) {
                MinMaxConfiguration("Analog-out1 at Min \nSAT Heating",
                    "Analog-out1 at Max \nSAT Heating",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut1MinMax.satHeatingMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut1MinMax.satHeatingMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut1MinMax.satHeatingMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut1MinMax.satHeatingMaxVoltage =
                            it.value.toInt()
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.SAT_BASED_HEATING_CONTROL,
                    viewModel.viewState.value.analogOut2Enabled,
                    viewModel.viewState.value.analogOut2Association
                )
            ) {
                MinMaxConfiguration("Analog-out2 at Min \nSAT Heating",
                    "Analog-out2 at Max \nSAT Heating",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut2MinMax.satHeatingMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut2MinMax.satHeatingMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut2MinMax.satHeatingMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut2MinMax.satHeatingMaxVoltage =
                            it.value.toInt()
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.SAT_BASED_HEATING_CONTROL,
                    viewModel.viewState.value.analogOut3Enabled,
                    viewModel.viewState.value.analogOut3Association
                )
            ) {
                MinMaxConfiguration("Analog-out3 at Min \nSAT Heating",
                    "Analog-out3 at Max \nSAT Heating",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut3MinMax.satHeatingMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut3MinMax.satHeatingMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut3MinMax.satHeatingMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut3MinMax.satHeatingMaxVoltage =
                            it.value.toInt()
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.SAT_BASED_HEATING_CONTROL,
                    viewModel.viewState.value.analogOut4Enabled,
                    viewModel.viewState.value.analogOut4Association
                )
            ) {
                MinMaxConfiguration("Analog-out4 at Min \nSAT Heating",
                    "Analog-out4 at Max \nSAT Heating",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut4MinMax.satHeatingMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut4MinMax.satHeatingMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut4MinMax.satHeatingMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut4MinMax.satHeatingMaxVoltage =
                            it.value.toInt()
                    })
            }
        }
    }

    @Composable
    fun CO2BasedDamperControl(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isDampersEnabled()) {
            val damperEnum = viewModel.getAllowedValues(DomainName.co2BasedDamperControlOn, viewModel.cmModel)
            val minMaxCo2List = viewModel.getListByDomainName(DomainName.co2Target, viewModel.cmModel)
            val co2Unit = viewModel.getUnit(DomainName.co2Target, viewModel.cmModel)
            val damperOperate = viewModel.getListByDomainName(DomainName.co2DamperOpeningRate, viewModel.cmModel)
            val percentUnit = viewModel.getUnit(DomainName.co2DamperOpeningRate, viewModel.cmModel)

            Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
                BoldStyledTextView(CO2_DAMPER_CONTROL, fontSize = 20)
                Row(modifier = Modifier.padding(top = 10.dp, bottom = 15.dp)) {
                    Box(modifier = Modifier.weight(1f).padding(top = 10.dp)) {
                        StyledTextView(
                            CO2_DAMPER_CONTROL_ON, fontSize = 20, textAlignment = TextAlign.Left
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SearchSpinnerElement(
                            default = damperEnum[viewModel.viewState.value.damperConfig.damperControlAssociation].value,
                            allItems = damperEnum,
                            unit = "",
                            onSelect = {
                                viewModel.viewState.value.damperConfig.damperControlAssociation =
                                    it.index
                            },
                            350
                        )
                    }
                    Box(modifier = Modifier.weight(1f))
                }
                MinMaxConfiguration(CO2_THRESHOLD,
                    CO2_TARGET,
                    minMaxCo2List,
                    co2Unit,
                    minDefault = viewModel.viewState.value.damperConfig.co2Threshold.toString(),
                    maxDefault = viewModel.viewState.value.damperConfig.co2Target.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.damperConfig.co2Threshold = it.value.toDouble()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.damperConfig.co2Target = it.value.toDouble()
                    })

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp, top = 10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        StyledTextView(
                            OPENING_RATE, fontSize = 20, textAlignment = TextAlign.Left
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SpinnerElementOption(defaultSelection = viewModel.viewState.value.damperConfig.openingRate.toString(),
                            items = damperOperate,
                            unit = percentUnit,
                            itemSelected = {
                                viewModel.viewState.value.damperConfig.openingRate =
                                    it.value.toDouble()
                            })
                    }
                    Box(modifier = Modifier.weight(2f))
                }
            }
            Co2MinMaxConfig(viewModel)
        }
    }

    @Composable
    fun Co2MinMaxConfig(viewModel: AdvancedHybridAhuViewModel) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.CO2_BASED_DAMPER_CONTROL,
                    viewModel.viewState.value.analogOut1Enabled,
                    viewModel.viewState.value.analogOut1Association
                )
            ) {
                MinMaxConfiguration("Analog-out1 at Min \nDCV Damper Pos",
                    "Analog-out1 at Max \nDCV Damper Pos",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut1MinMax.damperPosMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut1MinMax.damperPosMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut1MinMax.damperPosMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut1MinMax.damperPosMaxVoltage =
                            it.value.toInt()
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.CO2_BASED_DAMPER_CONTROL,
                    viewModel.viewState.value.analogOut2Enabled,
                    viewModel.viewState.value.analogOut2Association
                )
            ) {
                MinMaxConfiguration("Analog-out2 at Min \nDCV Damper Pos",
                    "Analog-out2 at Max \nDCV Damper Pos",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut2MinMax.damperPosMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut2MinMax.damperPosMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut2MinMax.damperPosMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut2MinMax.damperPosMaxVoltage =
                            it.value.toInt()
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.CO2_BASED_DAMPER_CONTROL,
                    viewModel.viewState.value.analogOut3Enabled,
                    viewModel.viewState.value.analogOut3Association
                )
            ) {
                MinMaxConfiguration("Analog-out3 at Min \nDCV Damper Pos",
                    "Analog-out3 at Max \nDCV Damper Pos",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut3MinMax.damperPosMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut3MinMax.damperPosMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut3MinMax.damperPosMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut3MinMax.damperPosMaxVoltage =
                            it.value.toInt()
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.CO2_BASED_DAMPER_CONTROL,
                    viewModel.viewState.value.analogOut4Enabled,
                    viewModel.viewState.value.analogOut4Association
                )
            ) {
                MinMaxConfiguration("Analog-out4 at Min \nDCV Damper Pos",
                    "Analog-out4 at Max \nDCV Damper Pos",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut4MinMax.damperPosMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut4MinMax.damperPosMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut4MinMax.damperPosMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut4MinMax.damperPosMaxVoltage =
                            it.value.toInt()
                    })
            }
        }
    }

    @Composable
    fun LoadBasedCoolingControl(viewModel: AdvancedHybridAhuViewModel) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
            if (viewModel.isCoolingLoadEnabled()) {
                BoldStyledTextView(COOLING_CONTROL, fontSize = 20)
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_COOLING_CONTROL,
                        viewModel.viewState.value.analogOut1Enabled,
                        viewModel.viewState.value.analogOut1Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out1 at Min \nCooling",
                        "Analog-out1 at Max \nCooling",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut1MinMax.coolingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut1MinMax.coolingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut1MinMax.coolingMinVoltage = it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut1MinMax.coolingMaxVoltage = it.value.toInt()
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_COOLING_CONTROL,
                        viewModel.viewState.value.analogOut2Enabled,
                        viewModel.viewState.value.analogOut2Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out2 at Min \nCooling",
                        "Analog-out2 at Max \nCooling",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut2MinMax.coolingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut2MinMax.coolingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut2MinMax.coolingMinVoltage = it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut2MinMax.coolingMaxVoltage = it.value.toInt()
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_COOLING_CONTROL,
                        viewModel.viewState.value.analogOut3Enabled,
                        viewModel.viewState.value.analogOut3Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out3 at Min \nCooling",
                        "Analog-out3 at Max \nCooling",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut3MinMax.coolingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut3MinMax.coolingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut3MinMax.coolingMinVoltage = it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut3MinMax.coolingMaxVoltage = it.value.toInt()
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_COOLING_CONTROL,
                        viewModel.viewState.value.analogOut4Enabled,
                        viewModel.viewState.value.analogOut4Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out4 at Min \nCooling",
                        "Analog-out4 at Max \nCooling",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut4MinMax.coolingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut4MinMax.coolingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut4MinMax.coolingMinVoltage = it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut4MinMax.coolingMaxVoltage = it.value.toInt()
                        })
                }
            }
        }
    }

    @Composable
    fun LoadBasedFanControl(viewModel: AdvancedHybridAhuViewModel) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
            if (viewModel.isFanLoadEnabled()) {
                BoldStyledTextView(FAN_CONTROL, fontSize = 20)
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_FAN_CONTROL,
                        viewModel.viewState.value.analogOut1Enabled,
                        viewModel.viewState.value.analogOut1Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out1 at Min \nFan",
                        "Analog-out1 at Max \nFan",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut1MinMax.fanMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut1MinMax.fanMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut1MinMax.fanMinVoltage =
                                it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut1MinMax.fanMaxVoltage =
                                it.value.toInt()
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_FAN_CONTROL,
                        viewModel.viewState.value.analogOut2Enabled,
                        viewModel.viewState.value.analogOut2Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out2 at Min \nFan",
                        "Analog-out2 at Max \nFan",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut2MinMax.fanMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut2MinMax.fanMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut2MinMax.fanMinVoltage =
                                it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut2MinMax.fanMaxVoltage =
                                it.value.toInt()
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_FAN_CONTROL,
                        viewModel.viewState.value.analogOut3Enabled,
                        viewModel.viewState.value.analogOut3Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out3 at Min \nFan",
                        "Analog-out3 at Max \nFan",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut3MinMax.fanMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut3MinMax.fanMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut3MinMax.fanMinVoltage =
                                it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut3MinMax.fanMaxVoltage =
                                it.value.toInt()
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_FAN_CONTROL,
                        viewModel.viewState.value.analogOut4Enabled,
                        viewModel.viewState.value.analogOut4Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out4 at Min \nFan",
                        "Analog-out4 at Max \nFan",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut4MinMax.fanMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut4MinMax.fanMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut4MinMax.fanMinVoltage =
                                it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut4MinMax.fanMaxVoltage =
                                it.value.toInt()
                        })
                }
            }

        }
    }

    @Composable
    fun HeatLoadBasedControl(viewModel: AdvancedHybridAhuViewModel) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
            if (viewModel.isHeatLoadEnabled()) {
                BoldStyledTextView(HEATING_CONTROL, fontSize = 20)
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_HEATING_CONTROL,
                        viewModel.viewState.value.analogOut1Enabled,
                        viewModel.viewState.value.analogOut1Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out1 at Min \nHeating",
                        "Analog-out1 at Max \nHeating",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut1MinMax.heatingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut1MinMax.heatingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut1MinMax.heatingMinVoltage = it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut1MinMax.heatingMaxVoltage = it.value.toInt()
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_HEATING_CONTROL,
                        viewModel.viewState.value.analogOut2Enabled,
                        viewModel.viewState.value.analogOut2Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out2 at Min \nHeating",
                        "Analog-out2 at Max \nHeating",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut2MinMax.heatingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut2MinMax.heatingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut2MinMax.heatingMinVoltage = it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut2MinMax.heatingMaxVoltage = it.value.toInt()
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_HEATING_CONTROL,
                        viewModel.viewState.value.analogOut3Enabled,
                        viewModel.viewState.value.analogOut3Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out3 at Min \nHeating",
                        "Analog-out3 at Max \nHeating",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut3MinMax.heatingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut3MinMax.heatingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut3MinMax.heatingMinVoltage = it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut3MinMax.heatingMaxVoltage = it.value.toInt()
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_HEATING_CONTROL,
                        viewModel.viewState.value.analogOut4Enabled,
                        viewModel.viewState.value.analogOut4Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out4 at Min \nHeating",
                        "Analog-out4 at Max \nHeating",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut4MinMax.heatingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut4MinMax.heatingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut4MinMax.heatingMinVoltage = it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut4MinMax.heatingMaxVoltage = it.value.toInt()
                        })
                }
            }
        }
    }

    @Composable
    fun CompositeBasedControl(viewModel: AdvancedHybridAhuViewModel) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
            if (viewModel.isCompositeEnabled()) {
                BoldStyledTextView(COMPOSITE_CONTROL, fontSize = 20)
                EnableCompositeCoolingMinMax(viewModel)
                EnableCompositeHeatingMinMax(viewModel)
            }
        }
    }

    @Composable
    fun EnableCompositeCoolingMinMax(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isAnalogEnabledAndMapped(
                ControlType.COMPOSITE,
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nCooling",
                "Analog-out1 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.analogOut1MinMax.compositeCoolingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.analogOut1MinMax.compositeCoolingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.analogOut1MinMax.compositeCoolingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    viewModel.viewState.value.analogOut1MinMax.compositeCoolingMaxVoltage = it.value.toInt()
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ControlType.COMPOSITE,
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nCooling",
                "Analog-out2 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.analogOut2MinMax.compositeCoolingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.analogOut2MinMax.compositeCoolingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.analogOut2MinMax.compositeCoolingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    viewModel.viewState.value.analogOut2MinMax.compositeCoolingMaxVoltage = it.value.toInt()
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ControlType.COMPOSITE,
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nCooling",
                "Analog-out3 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.analogOut3MinMax.compositeCoolingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.analogOut3MinMax.compositeCoolingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.analogOut3MinMax.compositeCoolingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    viewModel.viewState.value.analogOut3MinMax.compositeCoolingMaxVoltage = it.value.toInt()
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ControlType.COMPOSITE,
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nCooling",
                "Analog-out4 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.analogOut4MinMax.compositeCoolingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.analogOut4MinMax.compositeCoolingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.analogOut4MinMax.compositeCoolingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    viewModel.viewState.value.analogOut4MinMax.compositeCoolingMaxVoltage = it.value.toInt()
                })
        }
    }

    @Composable
    fun EnableCompositeHeatingMinMax(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isAnalogEnabledAndMapped(
                ControlType.COMPOSITE,
                viewModel.viewState.value.analogOut1Enabled,
                viewModel.viewState.value.analogOut1Association
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nHeating",
                "Analog-out1 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.analogOut1MinMax.compositeHeatingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.analogOut1MinMax.compositeHeatingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.analogOut1MinMax.compositeHeatingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    viewModel.viewState.value.analogOut1MinMax.compositeHeatingMaxVoltage = it.value.toInt()
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ControlType.COMPOSITE,
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nHeating",
                "Analog-out2 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.analogOut2MinMax.compositeHeatingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.analogOut2MinMax.compositeHeatingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.analogOut2MinMax.compositeHeatingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    viewModel.viewState.value.analogOut2MinMax.compositeHeatingMaxVoltage = it.value.toInt()
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ControlType.COMPOSITE,
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nHeating",
                "Analog-out3 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.analogOut3MinMax.compositeHeatingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.analogOut3MinMax.compositeHeatingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.analogOut3MinMax.compositeHeatingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    viewModel.viewState.value.analogOut3MinMax.compositeHeatingMaxVoltage = it.value.toInt()
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ControlType.COMPOSITE,
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nHeating",
                "Analog-out4 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.analogOut4MinMax.compositeHeatingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.analogOut4MinMax.compositeHeatingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.analogOut4MinMax.compositeHeatingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    viewModel.viewState.value.analogOut4MinMax.compositeHeatingMaxVoltage = it.value.toInt()
                })
        }
    }

    @Composable
    fun AddConnectModule(viewModel: AdvancedHybridAhuViewModel) {
        SaveTextViewNew(text = "ADD CONNECT MODULE") {
            viewModel.viewState.value.isConnectEnabled = true
        }
    }

    @Composable
    fun ConnectSensorConfig(viewModel: AdvancedHybridAhuViewModel) {
        val temperatureEnums = viewModel.getAllowedValues(DomainName.temperatureSensorBusAdd1, viewModel.connectModel)
        val humidityEnums = viewModel.getAllowedValues(DomainName.humiditySensorBusAdd1, viewModel.connectModel)
        val occupancyEnum = viewModel.getAllowedValues(DomainName.occupancySensorBusAdd0, viewModel.connectModel)
        val co2Enum = viewModel.getAllowedValues(DomainName.co2SensorBusAdd0, viewModel.connectModel)
        val pressureEnum = viewModel.getAllowedValues(DomainName.pressureSensorBusAdd0, viewModel.connectModel)

        SubTitle(SENSOR_BUS)
        Row {
            EnableCompose(ADDRESS_0, viewModel.viewState.value.connectSensorAddress0.enabled) {
                viewModel.viewState.value.connectSensorAddress0.enabled = it
            }
            Column {
                ConfigCompose(
                    TEMPERATURE,
                    temperatureEnums[viewModel.viewState.value.connectSensorAddress0.temperatureAssociation].value,
                    temperatureEnums,
                    "",
                    viewModel.viewState.value.connectSensorAddress0.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress0.temperatureAssociation = it.index
                    viewModel.viewState.value.connectSensorAddress0.humidityAssociation = it.index
                }
                HumidityCompose(
                    HUMIDITY,
                    humidityEnums[viewModel.viewState.value.connectSensorAddress0.humidityAssociation].value
                )
                ConfigCompose(
                    OCCUPANCY,
                    occupancyEnum[viewModel.viewState.value.connectSensorAddress0.occupancyAssociation].value,
                    occupancyEnum,
                    "",
                    viewModel.viewState.value.connectSensorAddress0.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress0.occupancyAssociation = it.index
                }
                ConfigCompose(
                    CO2,
                    co2Enum[viewModel.viewState.value.connectSensorAddress0.co2Association].value,
                    co2Enum,
                    "",
                    viewModel.viewState.value.connectSensorAddress0.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress0.co2Association = it.index
                }
            }
        }
        Row {
            EnableCompose(
                "", viewModel.viewState.value.connectSensorBusPressureEnable, hide = true
            ) {
                viewModel.viewState.value.connectSensorBusPressureEnable = it
            }
            Column {
                ConfigCompose(
                    PRESSURE,
                    pressureEnum[viewModel.viewState.value.connectSensorAddress0.pressureAssociation].value,
                    pressureEnum,
                    "",
                    viewModel.viewState.value.connectSensorBusPressureEnable
                ) {
                    viewModel.viewState.value.connectSensorAddress0.pressureAssociation = it.index
                }
            }
        }

        Row {
            EnableCompose(ADDRESS_1, viewModel.viewState.value.connectSensorAddress1.enabled) {
                viewModel.viewState.value.connectSensorAddress1.enabled = it
            }
            Column {
                ConfigCompose(
                    TEMPERATURE,
                    temperatureEnums[viewModel.viewState.value.connectSensorAddress1.temperatureAssociation].value,
                    temperatureEnums,
                    "",
                    viewModel.viewState.value.connectSensorAddress1.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress1.temperatureAssociation = it.index
                    viewModel.viewState.value.connectSensorAddress1.humidityAssociation = it.index
                }
                HumidityCompose(
                    HUMIDITY,
                    humidityEnums[viewModel.viewState.value.connectSensorAddress1.humidityAssociation].value
                )
                ConfigCompose(
                    OCCUPANCY,
                    occupancyEnum[viewModel.viewState.value.connectSensorAddress1.occupancyAssociation].value,
                    occupancyEnum,
                    "",
                    viewModel.viewState.value.connectSensorAddress1.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress1.occupancyAssociation = it.index
                }
                ConfigCompose(
                    CO2,
                    co2Enum[viewModel.viewState.value.connectSensorAddress1.co2Association].value,
                    co2Enum,
                    "",
                    viewModel.viewState.value.connectSensorAddress1.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress1.co2Association = it.index
                }
            }
        }
        Row {
            EnableCompose(ADDRESS_2, viewModel.viewState.value.connectSensorAddress2.enabled) {
                viewModel.viewState.value.connectSensorAddress2.enabled = it
            }
            Column {
                ConfigCompose(
                    TEMPERATURE,
                    temperatureEnums[viewModel.viewState.value.connectSensorAddress2.temperatureAssociation].value,
                    temperatureEnums,
                    "",
                    viewModel.viewState.value.connectSensorAddress2.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress2.temperatureAssociation = it.index
                    viewModel.viewState.value.connectSensorAddress2.humidityAssociation = it.index
                }
                HumidityCompose(
                    HUMIDITY,
                    humidityEnums[viewModel.viewState.value.connectSensorAddress2.humidityAssociation].value
                )
                ConfigCompose(
                    OCCUPANCY,
                    occupancyEnum[viewModel.viewState.value.connectSensorAddress2.occupancyAssociation].value,
                    occupancyEnum,
                    "",
                    viewModel.viewState.value.connectSensorAddress2.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress2.occupancyAssociation = it.index
                }
                ConfigCompose(
                    CO2,
                    co2Enum[viewModel.viewState.value.connectSensorAddress2.co2Association].value,
                    co2Enum,
                    "",
                    viewModel.viewState.value.connectSensorAddress2.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress2.co2Association = it.index
                }
            }
        }
        Row {
            EnableCompose(ADDRESS_3, viewModel.viewState.value.connectSensorAddress3.enabled) {
                viewModel.viewState.value.connectSensorAddress3.enabled = it
            }
            Column {
                ConfigCompose(
                    TEMPERATURE,
                    temperatureEnums[viewModel.viewState.value.connectSensorAddress3.temperatureAssociation].value,
                    temperatureEnums,
                    "",
                    viewModel.viewState.value.connectSensorAddress3.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress3.temperatureAssociation = it.index
                    viewModel.viewState.value.connectSensorAddress3.humidityAssociation = it.index
                }
                HumidityCompose(
                    HUMIDITY,
                    humidityEnums[viewModel.viewState.value.connectSensorAddress3.humidityAssociation].value
                )
                ConfigCompose(
                    OCCUPANCY,
                    occupancyEnum[viewModel.viewState.value.connectSensorAddress3.occupancyAssociation].value,
                    occupancyEnum,
                    "",
                    viewModel.viewState.value.connectSensorAddress3.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress3.occupancyAssociation = it.index
                }
                ConfigCompose(
                    CO2,
                    co2Enum[viewModel.viewState.value.connectSensorAddress3.co2Association].value,
                    co2Enum,
                    "",
                    viewModel.viewState.value.connectSensorAddress3.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress3.co2Association = it.index
                }
            }
        }
    }

    @Composable
    fun ConnectUniInConfig(viewModel: AdvancedHybridAhuViewModel) {

        /**
         * By assuming the allowed values are same all the Analog Inputs are same, Similarly for Thermistor also
         */
        val universalEnum = viewModel.getAllowedValues(DomainName.universalIn1Association, viewModel.connectModel)

        Row(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.connect_ui),
                contentDescription = "Universal Inputs",
                modifier = Modifier
                    .weight(1.5f)
                    .padding(top = 25.dp)
                    .height(550.dp)
            )
            Column(
                modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)
            ) {
                AOTHConfig(UNIVERSAL_IN1,
                    viewModel.viewState.value.connectUniversalIn1Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn1Config.enabled = it },
                    universalEnum[viewModel.viewState.value.connectUniversalIn1Config.association].value,
                    universalEnum,
                    "",
                    viewModel.viewState.value.connectUniversalIn1Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn1Config.association = it.index })
                AOTHConfig(UNIVERSAL_IN2,
                    viewModel.viewState.value.connectUniversalIn2Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn2Config.enabled = it },
                    universalEnum[viewModel.viewState.value.connectUniversalIn2Config.association].value,
                    universalEnum,
                    "",
                    viewModel.viewState.value.connectUniversalIn2Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn2Config.association = it.index })
                AOTHConfig(UNIVERSAL_IN3,
                    viewModel.viewState.value.connectUniversalIn3Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn3Config.enabled = it },
                    universalEnum[viewModel.viewState.value.connectUniversalIn3Config.association].value,
                    universalEnum,
                    "",
                    viewModel.viewState.value.connectUniversalIn3Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn3Config.association = it.index })
                AOTHConfig(UNIVERSAL_IN4,
                    viewModel.viewState.value.connectUniversalIn4Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn4Config.enabled = it },
                    universalEnum[viewModel.viewState.value.connectUniversalIn4Config.association].value,
                    universalEnum,
                    "",
                    viewModel.viewState.value.connectUniversalIn4Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn4Config.association = it.index })
                AOTHConfig(UNIVERSAL_IN5,
                    viewModel.viewState.value.connectUniversalIn5Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn5Config.enabled = it },
                    universalEnum[viewModel.viewState.value.connectUniversalIn5Config.association].value,
                    universalEnum,
                    "",
                    viewModel.viewState.value.connectUniversalIn5Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn5Config.association = it.index })
                AOTHConfig(UNIVERSAL_IN6,
                    viewModel.viewState.value.connectUniversalIn6Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn6Config.enabled = it },
                    universalEnum[viewModel.viewState.value.connectUniversalIn6Config.association].value,
                    universalEnum,
                    "",
                    viewModel.viewState.value.connectUniversalIn6Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn6Config.association = it.index })
                AOTHConfig(UNIVERSAL_IN7,
                    viewModel.viewState.value.connectUniversalIn7Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn7Config.enabled = it },
                    universalEnum[viewModel.viewState.value.connectUniversalIn7Config.association].value,
                    universalEnum,
                    "",
                    viewModel.viewState.value.connectUniversalIn7Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn7Config.association = it.index })
                AOTHConfig(UNIVERSAL_IN8,
                    viewModel.viewState.value.connectUniversalIn8Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn8Config.enabled = it },
                    universalEnum[viewModel.viewState.value.connectUniversalIn8Config.association].value,
                    universalEnum,
                    "",
                    viewModel.viewState.value.connectUniversalIn8Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn8Config.association = it.index })
            }
        }
    }

    @Composable
    fun ConnectRelayConfig(viewModel: AdvancedHybridAhuViewModel) {
        val relayEnums = viewModel.getAllowedValues(DomainName.relay1OutputAssociation, viewModel.connectModel)

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Display relay image
            Image(
                painter = painterResource(id = R.drawable.connect_relays),
                contentDescription = "Relays",
                modifier = Modifier
                    .weight(1.5f)
                    .padding(top = 32.dp)
                    .height(508.dp)
            )

            // Display relay configurations
            Column(
                modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)
            ) {
                repeat(8) { index ->
                    val relayConfig = when (index) {
                        0 -> viewModel.viewState.value.connectRelay1Config
                        1 -> viewModel.viewState.value.connectRelay2Config
                        2 -> viewModel.viewState.value.connectRelay3Config
                        3 -> viewModel.viewState.value.connectRelay4Config
                        4 -> viewModel.viewState.value.connectRelay5Config
                        5 -> viewModel.viewState.value.connectRelay6Config
                        6 -> viewModel.viewState.value.connectRelay7Config
                        7 -> viewModel.viewState.value.connectRelay8Config
                        else -> throw IllegalArgumentException("Invalid relay index: $index")
                    }

                    RelayConfiguration(relayName = "Relay ${index + 1}",
                        enabled = relayConfig.enabled,
                        onEnabledChanged = { enabled -> relayConfig.enabled = enabled },
                        association = relayEnums[relayConfig.association].value,
                        unit = "",
                        relayEnums = relayEnums,
                        isEnabled = relayConfig.enabled,
                        onAssociationChanged = { associationIndex ->
                            relayConfig.association = associationIndex.index
                        },
                        testState = viewModel.getPhysicalPointForRelayIndex(index, true)?.let { it.readHisVal() > 0 } ?: false,
                        onTestActivated = {viewModel.sendConnectRelayTestCommand(index, it)})
                }
            }
        }
    }

    @Composable
    fun ConnectAnalogOutConfig(viewModel: AdvancedHybridAhuViewModel) {
        val analogOutEnums = viewModel.getAllowedValues(DomainName.analog1OutputAssociation, viewModel.connectModel)

        Row(modifier = Modifier.fillMaxWidth()) {
            // Display analog out image
            Column(
                modifier = Modifier
                    .weight(1.5f)
                    .padding(top = 10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.connect_ao),
                    contentDescription = "Analog Out",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(15.dp)
                        .height(280.dp)
                )
            }

            // Display analog out configurations
            Column(
                modifier = Modifier
                    .weight(4f)
                    .padding(top = 10.dp)
            ) {
                repeat(4) { index ->
                    val analogOut = "Analog-out${index + 1}"
                    val enabled = when (index) {
                        0 -> viewModel.viewState.value.connectAnalogOut1Enabled
                        1 -> viewModel.viewState.value.connectAnalogOut2Enabled
                        2 -> viewModel.viewState.value.connectAnalogOut3Enabled
                        3 -> viewModel.viewState.value.connectAnalogOut4Enabled
                        else -> false
                    }
                    val associationIndex = when (index) {
                        0 -> viewModel.viewState.value.connectAnalogOut1Association
                        1 -> viewModel.viewState.value.connectAnalogOut2Association
                        2 -> viewModel.viewState.value.connectAnalogOut3Association
                        3 -> viewModel.viewState.value.connectAnalogOut4Association
                        else -> 0
                    }

                    AnalogOutConfig(analogOutName = analogOut,
                        enabled = enabled,
                        onEnabledChanged = { enabledAction ->
                            when (index) {
                                0 -> viewModel.viewState.value.connectAnalogOut1Enabled = enabledAction
                                1 -> viewModel.viewState.value.connectAnalogOut2Enabled = enabledAction
                                2 -> viewModel.viewState.value.connectAnalogOut3Enabled = enabledAction
                                3 -> viewModel.viewState.value.connectAnalogOut4Enabled = enabledAction
                            }
                        },
                        association = analogOutEnums[associationIndex].value,
                        analogOutEnums = analogOutEnums,
                        testSingles = viewModel.testVoltage,
                        isEnabled = enabled,
                        onAssociationChanged = { association ->
                            when (index) {
                                0 -> viewModel.viewState.value.connectAnalogOut1Association =
                                    association.index

                                1 -> viewModel.viewState.value.connectAnalogOut2Association =
                                    association.index
                                
                                2 -> viewModel.viewState.value.connectAnalogOut3Association =
                                    association.index

                                3 -> viewModel.viewState.value.connectAnalogOut4Association =
                                    association.index
                            }
                        },
                        testVal = viewModel.getPhysicalPointForAnalogIndex(index)?.readHisVal()?.toInt() ?: 0,
                        onTestSignalSelected = {viewModel.sendConnectAnalogTestCommand(index, it)})
                }
            }
        }
    }

    @Composable
    fun ConnectAnalogOutDynamicConfig(viewModel: AdvancedHybridAhuViewModel) {
        ConnectCO2BasedDamperControl(viewModel)
        ConnectLoadBasedCoolingControl(viewModel)
        ConnectLoadBasedFanControl(viewModel)
        ConnectHeatLoadBasedControl(viewModel)
        ConnectCompositeBasedControl(viewModel)
    }

    @Composable
    fun ConnectLoadBasedCoolingControl(viewModel: AdvancedHybridAhuViewModel) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
            if (viewModel.isConnectCoolingLoadEnabled()) {
                BoldStyledTextView(COOLING_CONTROL, fontSize = 20)
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_COOLING_CONTROL,
                        viewModel.viewState.value.connectAnalogOut1Enabled,
                        viewModel.viewState.value.connectAnalogOut1Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out1 at Min \nCooling",
                        "Analog-out1 at Max \nCooling",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut1MinMax.coolingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut1MinMax.coolingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut1MinMax.coolingMinVoltage = it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut1MinMax.coolingMaxVoltage = it.value.toInt()
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_COOLING_CONTROL,
                        viewModel.viewState.value.connectAnalogOut2Enabled,
                        viewModel.viewState.value.connectAnalogOut2Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out2 at Min \nCooling",
                        "Analog-out2 at Max \nCooling",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut2MinMax.coolingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut2MinMax.coolingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut2MinMax.coolingMinVoltage = it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut2MinMax.coolingMaxVoltage = it.value.toInt()
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_COOLING_CONTROL,
                        viewModel.viewState.value.connectAnalogOut3Enabled,
                        viewModel.viewState.value.connectAnalogOut3Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out3 at Min \nCooling",
                        "Analog-out3 at Max \nCooling",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut3MinMax.coolingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut3MinMax.coolingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut3MinMax.coolingMinVoltage = it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut3MinMax.coolingMaxVoltage = it.value.toInt()
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_COOLING_CONTROL,
                        viewModel.viewState.value.connectAnalogOut4Enabled,
                        viewModel.viewState.value.connectAnalogOut4Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out4 at Min \nCooling",
                        "Analog-out4 at Max \nCooling",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut4MinMax.coolingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut4MinMax.coolingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut4MinMax.coolingMinVoltage = it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut4MinMax.coolingMaxVoltage = it.value.toInt()
                        })
                }
            }
        }
    }

    @Composable
    fun ConnectLoadBasedFanControl(viewModel: AdvancedHybridAhuViewModel) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
            if (viewModel.isConnectFanLoadEnabled()) {
                BoldStyledTextView(FAN_CONTROL, fontSize = 20)
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_FAN_CONTROL,
                        viewModel.viewState.value.connectAnalogOut1Enabled,
                        viewModel.viewState.value.connectAnalogOut1Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out1 at Min \nFan",
                        "Analog-out1 at Max \nFan",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut1MinMax.fanMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut1MinMax.fanMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut1MinMax.fanMinVoltage =
                                it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut1MinMax.fanMaxVoltage =
                                it.value.toInt()
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_FAN_CONTROL,
                        viewModel.viewState.value.connectAnalogOut2Enabled,
                        viewModel.viewState.value.connectAnalogOut2Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out2 at Min \nFan",
                        "Analog-out2 at Max \nFan",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut2MinMax.fanMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut2MinMax.fanMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut2MinMax.fanMinVoltage =
                                it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut2MinMax.fanMaxVoltage =
                                it.value.toInt()
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_FAN_CONTROL,
                        viewModel.viewState.value.connectAnalogOut3Enabled,
                        viewModel.viewState.value.connectAnalogOut3Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out3 at Min \nFan",
                        "Analog-out3 at Max \nFan",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut3MinMax.fanMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut3MinMax.fanMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut3MinMax.fanMinVoltage =
                                it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut3MinMax.fanMaxVoltage =
                                it.value.toInt()
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_FAN_CONTROL,
                        viewModel.viewState.value.connectAnalogOut4Enabled,
                        viewModel.viewState.value.connectAnalogOut4Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out4 at Min \nFan",
                        "Analog-out4 at Max \nFan",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut4MinMax.fanMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut4MinMax.fanMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut4MinMax.fanMinVoltage =
                                it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut4MinMax.fanMaxVoltage =
                                it.value.toInt()
                        })
                }
            }

        }
    }

    @Composable
    fun ConnectHeatLoadBasedControl(viewModel: AdvancedHybridAhuViewModel) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
            if (viewModel.isConnectHeatLoadEnabled()) {
                BoldStyledTextView(HEATING_CONTROL, fontSize = 20)
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_HEATING_CONTROL,
                        viewModel.viewState.value.connectAnalogOut1Enabled,
                        viewModel.viewState.value.connectAnalogOut1Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out1 at Min \nHeating",
                        "Analog-out1 at Max \nHeating",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut1MinMax.heatingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut1MinMax.heatingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut1MinMax.heatingMinVoltage = it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut1MinMax.heatingMaxVoltage = it.value.toInt()
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_HEATING_CONTROL,
                        viewModel.viewState.value.connectAnalogOut2Enabled,
                        viewModel.viewState.value.connectAnalogOut2Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out2 at Min \nHeating",
                        "Analog-out2 at Max \nHeating",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut2MinMax.heatingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut2MinMax.heatingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut2MinMax.heatingMinVoltage = it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut2MinMax.heatingMaxVoltage = it.value.toInt()
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_HEATING_CONTROL,
                        viewModel.viewState.value.connectAnalogOut3Enabled,
                        viewModel.viewState.value.connectAnalogOut3Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out3 at Min \nHeating",
                        "Analog-out3 at Max \nHeating",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut3MinMax.heatingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut3MinMax.heatingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut3MinMax.heatingMinVoltage = it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut3MinMax.heatingMaxVoltage = it.value.toInt()
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_HEATING_CONTROL,
                        viewModel.viewState.value.connectAnalogOut4Enabled,
                        viewModel.viewState.value.connectAnalogOut4Association
                    )
                ) {
                    MinMaxConfiguration("Analog-out4 at Min \nHeating",
                        "Analog-out4 at Max \nHeating",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut4MinMax.heatingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut4MinMax.heatingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut4MinMax.heatingMinVoltage = it.value.toInt()
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut4MinMax.heatingMaxVoltage = it.value.toInt()
                        })
                }
            }
        }
    }

    @Composable
    fun ConnectCompositeBasedControl(viewModel: AdvancedHybridAhuViewModel) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
            if (viewModel.isConnectCompositeEnabled()) {
                BoldStyledTextView(COMPOSITE_CONTROL, fontSize = 20)
                EnableConnectCompositeCoolingMinMax(viewModel)
                EnableConnectCompositeHeatingMinMax(viewModel)
            }
        }
    }
    @Composable
    fun ConnectCO2BasedDamperControl(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isConnectDampersEnabled()) {
            val damperEnum = viewModel.getAllowedValues(DomainName.co2BasedDamperControlOn, viewModel.connectModel)
            val minMaxCo2List = viewModel.getListByDomainName(DomainName.co2Target, viewModel.connectModel)
            val co2Unit = viewModel.getUnit(DomainName.co2Target, viewModel.connectModel)
            val damperOperate = viewModel.getListByDomainName(DomainName.co2DamperOpeningRate, viewModel.connectModel)
            val percentUnit = viewModel.getUnit(DomainName.co2DamperOpeningRate, viewModel.connectModel)

            Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
                BoldStyledTextView(CO2_DAMPER_CONTROL, fontSize = 20)
                Row(modifier = Modifier.padding(top = 10.dp, bottom = 15.dp)) {
                    Box(modifier = Modifier.weight(1f).padding(top = 10.dp)) {
                        StyledTextView(
                            CO2_DAMPER_CONTROL_ON, fontSize = 20, textAlignment = TextAlign.Left
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SearchSpinnerElement(
                            default = damperEnum[viewModel.viewState.value.connectDamperConfig.damperControlAssociation].value,
                            allItems = damperEnum,
                            unit = "",
                            onSelect = {
                                viewModel.viewState.value.connectDamperConfig.damperControlAssociation =
                                    it.index
                            },
                            350
                        )
                    }
                    Box(modifier = Modifier.weight(1f))
                }
                MinMaxConfiguration(CO2_THRESHOLD,
                    CO2_TARGET,
                    minMaxCo2List,
                    co2Unit,
                    minDefault = viewModel.viewState.value.connectDamperConfig.co2Threshold.toString(),
                    maxDefault = viewModel.viewState.value.connectDamperConfig.co2Target.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.connectDamperConfig.co2Threshold = it.value.toDouble()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.connectDamperConfig.co2Target = it.value.toDouble()
                    })

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp, top = 10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        StyledTextView(
                            OPENING_RATE, fontSize = 20, textAlignment = TextAlign.Left
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SpinnerElementOption(defaultSelection = viewModel.viewState.value.connectDamperConfig.openingRate.toString(),
                            items = damperOperate,
                            unit = percentUnit,
                            itemSelected = {
                                viewModel.viewState.value.connectDamperConfig.openingRate =
                                    it.value.toDouble()
                            })
                    }
                    Box(modifier = Modifier.weight(2f))
                }
            }
            ConnectCo2MinMaxConfig(viewModel)
        }
    }

    @Composable
    fun ConnectCo2MinMaxConfig(viewModel: AdvancedHybridAhuViewModel) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)) {
            if (viewModel.isAnalogEnabledAndMapped(
                    ConnectControlType.CO2_BASED_DAMPER_CONTROL,
                    viewModel.viewState.value.connectAnalogOut1Enabled,
                    viewModel.viewState.value.connectAnalogOut1Association
                )
            ) {
                MinMaxConfiguration("Analog-out1 at Min \nDCV Damper Pos",
                    "Analog-out1 at Max \nDCV Damper Pos",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.connectAnalogOut1MinMax.damperPosMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.connectAnalogOut1MinMax.damperPosMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.connectAnalogOut1MinMax.damperPosMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.connectAnalogOut1MinMax.damperPosMaxVoltage =
                            it.value.toInt()
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ConnectControlType.CO2_BASED_DAMPER_CONTROL,
                    viewModel.viewState.value.connectAnalogOut2Enabled,
                    viewModel.viewState.value.connectAnalogOut2Association
                )
            ) {
                MinMaxConfiguration("Analog-out2 at Min \nDCV Damper Pos",
                    "Analog-out2 at Max \nDCV Damper Pos",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.connectAnalogOut2MinMax.damperPosMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.connectAnalogOut2MinMax.damperPosMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.connectAnalogOut2MinMax.damperPosMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.connectAnalogOut2MinMax.damperPosMaxVoltage =
                            it.value.toInt()
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ConnectControlType.CO2_BASED_DAMPER_CONTROL,
                    viewModel.viewState.value.connectAnalogOut3Enabled,
                    viewModel.viewState.value.connectAnalogOut3Association
                )
            ) {
                MinMaxConfiguration("Analog-out3 at Min \nDCV Damper Pos",
                    "Analog-out3 at Max \nDCV Damper Pos",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.connectAnalogOut3MinMax.damperPosMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.connectAnalogOut3MinMax.damperPosMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.connectAnalogOut3MinMax.damperPosMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.connectAnalogOut3MinMax.damperPosMaxVoltage =
                            it.value.toInt()
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ConnectControlType.CO2_BASED_DAMPER_CONTROL,
                    viewModel.viewState.value.connectAnalogOut4Enabled,
                    viewModel.viewState.value.connectAnalogOut4Association
                )
            ) {
                MinMaxConfiguration("Analog-out4 at Min \nDCV Damper Pos",
                    "Analog-out4 at Max \nDCV Damper Pos",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.connectAnalogOut4MinMax.damperPosMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.connectAnalogOut4MinMax.damperPosMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.connectAnalogOut4MinMax.damperPosMinVoltage =
                            it.value.toInt()
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.connectAnalogOut4MinMax.damperPosMaxVoltage =
                            it.value.toInt()
                    })
            }
        }
    }

    @Composable
    fun EnableConnectCompositeCoolingMinMax(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isAnalogEnabledAndMapped(
                ConnectControlType.COMPOSITE,
                viewModel.viewState.value.connectAnalogOut1Enabled,
                viewModel.viewState.value.connectAnalogOut1Association
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nCooling",
                "Analog-out1 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.connectAnalogOut1MinMax.compositeCoolingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.connectAnalogOut1MinMax.compositeCoolingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.connectAnalogOut1MinMax.compositeCoolingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    viewModel.viewState.value.connectAnalogOut1MinMax.compositeCoolingMaxVoltage = it.value.toInt()
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ConnectControlType.COMPOSITE,
                viewModel.viewState.value.connectAnalogOut2Enabled,
                viewModel.viewState.value.connectAnalogOut2Association
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nCooling",
                "Analog-out2 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.connectAnalogOut2MinMax.compositeCoolingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.connectAnalogOut2MinMax.compositeCoolingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.connectAnalogOut2MinMax.compositeCoolingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    viewModel.viewState.value.connectAnalogOut2MinMax.compositeCoolingMaxVoltage = it.value.toInt()
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ConnectControlType.COMPOSITE,
                viewModel.viewState.value.connectAnalogOut3Enabled,
                viewModel.viewState.value.connectAnalogOut3Association
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nCooling",
                "Analog-out3 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.connectAnalogOut3MinMax.compositeCoolingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.connectAnalogOut3MinMax.compositeCoolingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.connectAnalogOut3MinMax.compositeCoolingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    viewModel.viewState.value.connectAnalogOut3MinMax.compositeCoolingMaxVoltage = it.value.toInt()
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ConnectControlType.COMPOSITE,
                viewModel.viewState.value.connectAnalogOut4Enabled,
                viewModel.viewState.value.connectAnalogOut4Association
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nCooling",
                "Analog-out4 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.connectAnalogOut4MinMax.compositeCoolingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.connectAnalogOut4MinMax.compositeCoolingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.connectAnalogOut4MinMax.compositeCoolingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    viewModel.viewState.value.connectAnalogOut4MinMax.compositeCoolingMaxVoltage = it.value.toInt()
                })
        }
    }

    @Composable
    fun EnableConnectCompositeHeatingMinMax(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isAnalogEnabledAndMapped(
                ConnectControlType.COMPOSITE,
                viewModel.viewState.value.connectAnalogOut1Enabled,
                viewModel.viewState.value.connectAnalogOut1Association
            )
        ) {
            MinMaxConfiguration("Analog-out1 at Min \nHeating",
                "Analog-out1 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.connectAnalogOut1MinMax.compositeHeatingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.connectAnalogOut1MinMax.compositeHeatingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.connectAnalogOut1MinMax.compositeHeatingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    viewModel.viewState.value.connectAnalogOut1MinMax.compositeHeatingMaxVoltage = it.value.toInt()
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ConnectControlType.COMPOSITE,
                viewModel.viewState.value.connectAnalogOut2Enabled,
                viewModel.viewState.value.connectAnalogOut2Association
            )
        ) {
            MinMaxConfiguration("Analog-out2 at Min \nHeating",
                "Analog-out2 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.connectAnalogOut2MinMax.compositeHeatingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.connectAnalogOut2MinMax.compositeHeatingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.connectAnalogOut2MinMax.compositeHeatingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    viewModel.viewState.value.connectAnalogOut2MinMax.compositeHeatingMaxVoltage = it.value.toInt()
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ConnectControlType.COMPOSITE,
                viewModel.viewState.value.connectAnalogOut3Enabled,
                viewModel.viewState.value.connectAnalogOut3Association
            )
        ) {
            MinMaxConfiguration("Analog-out3 at Min \nHeating",
                "Analog-out3 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.connectAnalogOut3MinMax.compositeHeatingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.connectAnalogOut3MinMax.compositeHeatingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.connectAnalogOut3MinMax.compositeHeatingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    viewModel.viewState.value.connectAnalogOut3MinMax.compositeHeatingMaxVoltage = it.value.toInt()
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ConnectControlType.COMPOSITE,
                viewModel.viewState.value.connectAnalogOut4Enabled,
                viewModel.viewState.value.connectAnalogOut4Association
            )
        ) {
            MinMaxConfiguration("Analog-out4 at Min \nHeating",
                "Analog-out4 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.connectAnalogOut4MinMax.compositeHeatingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.connectAnalogOut4MinMax.compositeHeatingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.connectAnalogOut4MinMax.compositeHeatingMinVoltage = it.value.toInt()
                },
                onMaxSelected = {
                    viewModel.viewState.value.connectAnalogOut4MinMax.compositeHeatingMaxVoltage = it.value.toInt()
                })
        }
    }
}