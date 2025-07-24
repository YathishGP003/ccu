package a75f.io.renatus.profiles.system.advancedahu

import a75f.io.domain.api.DomainName
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.AdvancedAhuRelayMappings
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu
import a75f.io.renatus.R
import a75f.io.renatus.composables.AOTHConfig
import a75f.io.renatus.composables.AnalogOutConfig
import a75f.io.renatus.composables.ConfigCompose
import a75f.io.renatus.composables.DropDownWithLabel
import a75f.io.renatus.composables.EnableCompose
import a75f.io.renatus.composables.HumidityCompose
import a75f.io.renatus.composables.MinMaxConfiguration
import a75f.io.renatus.composables.RelayConfiguration
import a75f.io.renatus.compose.BoldStyledTextView
import a75f.io.renatus.compose.ComposeUtil
import a75f.io.renatus.compose.ComposeUtil.Companion.myFontFamily
import a75f.io.renatus.compose.HeaderTextView
import a75f.io.renatus.compose.LabelTextView
import a75f.io.renatus.compose.SaveTextViewNew
import a75f.io.renatus.compose.SaveTextViewNewExtraBold
import a75f.io.renatus.compose.SearchSpinnerElement
import a75f.io.renatus.compose.SpinnerElementOption
import a75f.io.renatus.compose.StyledTextView
import a75f.io.renatus.compose.SubTitle
import a75f.io.renatus.compose.ToggleButtonStateful
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
import a75f.io.renatus.profiles.system.COMPRESSOR_SPEED
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
import a75f.io.renatus.util.TestSignalManager
import android.os.Bundle
import android.view.View
import android.view.View.OnAttachStateChangeListener
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels

/**
 * Created by Manjunath K on 14-03-2024.
 */

open class AdvancedHybridAhuFragment : Fragment() {
    open val viewModel: AdvancedHybridAhuViewModel by viewModels()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
            }
            override fun onViewDetachedFromWindow(view: View) {
                if (Globals.getInstance().isTestMode) {
                    Globals.getInstance().isTestMode = false
                    viewModel.updateTestCacheConfig(0, true)
                    TestSignalManager.restoreAllPoints()
                }
            }
        })
    }

    @Composable
    fun TitleLabel() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp, bottom = 10.dp)
        ) {
            Box(
                modifier = Modifier.weight(0.5f), contentAlignment = Alignment.Center
            ) { SubTitle(CM) }
            Box(modifier = Modifier.weight(3.5f), contentAlignment = Alignment.Center) {
                SubTitle(
                    ENABLE
                )
            }
            Box(modifier = Modifier.weight(2f), contentAlignment = Alignment.Center) {
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
                    .padding(top = 10.dp, bottom = 10.dp)
            ) {
                Box(modifier = Modifier.padding(top = 10.dp)) {
                    LabelTextView(text = CONNECT_MODULE, widthValue = 250)
                }
                SaveTextViewNewExtraBold {
                    viewModel.viewState.value.pendingDeleteConnect = true
                    setStateChanged(viewModel)
                }
                Spacer(Modifier.weight(1f))
                Box(modifier = Modifier.padding(end = 30.dp)) {
                    StyledTextView("Address: " + (L.ccu().addressBand + 98), fontSize = 20)
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

        SubTitle(SENSOR_BUS, fontSizeCustom = 16.0, topPaddingValue = 3, startPaddingValue = 15)
        Row {
            EnableCompose(ADDRESS_0, viewModel.viewState.value.sensorAddress0.enabled) {
                viewModel.viewState.value.sensorAddress0.enabled = it
                setStateChanged(viewModel)
            }
            Column {
                ConfigCompose(
                    TEMPERATURE,
                    temperatureEnums[viewModel.viewState.value.sensorAddress0.temperatureAssociation],
                    temperatureEnums,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress0.enabled
                ) {
                    viewModel.viewState.value.sensorAddress0.temperatureAssociation = it.index
                    viewModel.viewState.value.sensorAddress0.humidityAssociation = it.index
                    setStateChanged(viewModel)
                }
                HumidityCompose(
                    HUMIDITY,
                    humidityEnums[viewModel.viewState.value.sensorAddress0.humidityAssociation].value
                )
                ConfigCompose(
                    OCCUPANCY,
                    occupancyEnum[viewModel.viewState.value.sensorAddress0.occupancyAssociation],
                    occupancyEnum,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress0.enabled
                ) {
                    viewModel.viewState.value.sensorAddress0.occupancyAssociation = it.index
                    setStateChanged(viewModel)
                }
                ConfigCompose(
                    CO2,
                    co2Enum[viewModel.viewState.value.sensorAddress0.co2Association],
                    co2Enum,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress0.enabled
                ) {
                    viewModel.viewState.value.sensorAddress0.co2Association = it.index
                    setStateChanged(viewModel)
                }
            }
        }
        Row {
            EnableCompose(
                "", viewModel.viewState.value.sensorBusPressureEnable, hide = true
            ) {
                viewModel.viewState.value.sensorBusPressureEnable = it
                setStateChanged(viewModel)
            }
            Column {
                ConfigCompose(
                    PRESSURE,
                    pressureEnum[viewModel.viewState.value.sensorAddress0.pressureAssociation],
                    pressureEnum,
                    "",
                    isEnabled = viewModel.viewState.value.sensorBusPressureEnable
                ) {
                    viewModel.viewState.value.sensorAddress0.pressureAssociation = it.index
                    setStateChanged(viewModel)
                }
            }
        }

        Row {
            EnableCompose(ADDRESS_1, viewModel.viewState.value.sensorAddress1.enabled) {
                viewModel.viewState.value.sensorAddress1.enabled = it
                setStateChanged(viewModel)
            }
            Column {
                ConfigCompose(
                    TEMPERATURE,
                    temperatureEnums[viewModel.viewState.value.sensorAddress1.temperatureAssociation],
                    temperatureEnums,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress1.enabled
                ) {
                    viewModel.viewState.value.sensorAddress1.temperatureAssociation = it.index
                    viewModel.viewState.value.sensorAddress1.humidityAssociation = it.index
                    setStateChanged(viewModel)
                }
                HumidityCompose(
                    HUMIDITY,
                    humidityEnums[viewModel.viewState.value.sensorAddress1.humidityAssociation].value
                )
                ConfigCompose(
                    OCCUPANCY,
                    occupancyEnum[viewModel.viewState.value.sensorAddress1.occupancyAssociation],
                    occupancyEnum,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress1.enabled
                ) {
                    viewModel.viewState.value.sensorAddress1.occupancyAssociation = it.index
                    setStateChanged(viewModel)
                }
                ConfigCompose(
                    CO2,
                    co2Enum[viewModel.viewState.value.sensorAddress1.co2Association],
                    co2Enum,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress1.enabled
                ) {
                    viewModel.viewState.value.sensorAddress1.co2Association = it.index
                    setStateChanged(viewModel)
                }
            }
        }
        Row {
            EnableCompose(ADDRESS_2, viewModel.viewState.value.sensorAddress2.enabled) {
                viewModel.viewState.value.sensorAddress2.enabled = it
                setStateChanged(viewModel)
            }
            Column {
                ConfigCompose(
                    TEMPERATURE,
                    temperatureEnums[viewModel.viewState.value.sensorAddress2.temperatureAssociation],
                    temperatureEnums,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress2.enabled
                ) {
                    viewModel.viewState.value.sensorAddress2.temperatureAssociation = it.index
                    viewModel.viewState.value.sensorAddress2.humidityAssociation = it.index
                    setStateChanged(viewModel)
                }
                HumidityCompose(
                    HUMIDITY,
                    humidityEnums[viewModel.viewState.value.sensorAddress2.humidityAssociation].value
                )
                ConfigCompose(
                    OCCUPANCY,
                    occupancyEnum[viewModel.viewState.value.sensorAddress2.occupancyAssociation],
                    occupancyEnum,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress2.enabled
                ) {
                    viewModel.viewState.value.sensorAddress2.occupancyAssociation = it.index
                    setStateChanged(viewModel)
                }
                ConfigCompose(
                    CO2,
                    co2Enum[viewModel.viewState.value.sensorAddress2.co2Association],
                    co2Enum,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress2.enabled
                ) {
                    viewModel.viewState.value.sensorAddress2.co2Association = it.index
                    setStateChanged(viewModel)
                }
            }
        }
        Row {
            EnableCompose(ADDRESS_3, viewModel.viewState.value.sensorAddress3.enabled) {
                viewModel.viewState.value.sensorAddress3.enabled = it
                setStateChanged(viewModel)
            }
            Column {
                ConfigCompose(
                    TEMPERATURE,
                    temperatureEnums[viewModel.viewState.value.sensorAddress3.temperatureAssociation],
                    temperatureEnums,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress3.enabled
                ) {
                    viewModel.viewState.value.sensorAddress3.temperatureAssociation = it.index
                    viewModel.viewState.value.sensorAddress3.humidityAssociation = it.index
                    setStateChanged(viewModel)
                }
                HumidityCompose(
                    HUMIDITY,
                    humidityEnums[viewModel.viewState.value.sensorAddress3.humidityAssociation].value

                )
                ConfigCompose(
                    OCCUPANCY,
                    occupancyEnum[viewModel.viewState.value.sensorAddress3.occupancyAssociation],
                    occupancyEnum,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress3.enabled
                ) {
                    viewModel.viewState.value.sensorAddress3.occupancyAssociation = it.index
                    setStateChanged(viewModel)
                }
                ConfigCompose(
                    CO2,
                    co2Enum[viewModel.viewState.value.sensorAddress3.co2Association],
                    co2Enum,
                    "",
                    isEnabled = viewModel.viewState.value.sensorAddress3.enabled
                ) {
                    viewModel.viewState.value.sensorAddress3.co2Association = it.index
                    setStateChanged(viewModel)
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
                    { viewModel.viewState.value.analogIn1Config.enabled = it
                        setStateChanged(viewModel)},
                    analogEnum[viewModel.viewState.value.analogIn1Config.association],
                    analogEnum,
                    "",
                    viewModel.viewState.value.analogIn1Config.enabled,
                    { viewModel.viewState.value.analogIn1Config.association = it.index
                        setStateChanged(viewModel)})
                AOTHConfig(ANALOG_IN2,
                    viewModel.viewState.value.analogIn2Config.enabled,
                    { viewModel.viewState.value.analogIn2Config.enabled = it
                        setStateChanged(viewModel)},
                    analogEnum[viewModel.viewState.value.analogIn2Config.association],
                    analogEnum,
                    "",
                    viewModel.viewState.value.analogIn2Config.enabled,
                    { viewModel.viewState.value.analogIn2Config.association = it.index
                        setStateChanged(viewModel)})

                AOTHConfig(THERMISTOR_1,
                    viewModel.viewState.value.thermistor1Config.enabled,
                    { viewModel.viewState.value.thermistor1Config.enabled = it
                        setStateChanged(viewModel)},
                    thEnum[viewModel.viewState.value.thermistor1Config.association],
                    thEnum,
                    "",
                    viewModel.viewState.value.thermistor1Config.enabled,
                    { viewModel.viewState.value.thermistor1Config.association = it.index
                        setStateChanged(viewModel)})
                AOTHConfig(THERMISTOR_2,
                    viewModel.viewState.value.thermistor2Config.enabled,
                    { viewModel.viewState.value.thermistor2Config.enabled = it
                        setStateChanged(viewModel)},
                    thEnum[viewModel.viewState.value.thermistor2Config.association],
                    thEnum,
                    "",
                    viewModel.viewState.value.thermistor2Config.enabled,
                    { viewModel.viewState.value.thermistor2Config.association = it.index
                        setStateChanged(viewModel)})

            }
        }
    }

    /**
     * This function is used to display the Relay configurations
     */
    @Composable
    fun CMRelayConfig(viewModel: AdvancedHybridAhuViewModel) {
        val relayEnums = viewModel.getAllowedValues(DomainName.relay1OutputAssociation, viewModel.cmModel)


        fun getDisabledIndices(config: ConfigState): List<Int> {
            return if (viewModel.viewState.value.isAnyRelayMapped(
                    AdvancedAhuRelayMappings.CHANGE_OVER_B_HEATING.ordinal,
                    config)) {
                listOf(AdvancedAhuRelayMappings.CHANGE_OVER_O_COOLING.ordinal)
            } else if (viewModel.viewState.value.isAnyRelayMapped(
                    AdvancedAhuRelayMappings.CHANGE_OVER_O_COOLING.ordinal,
                    config)) {
                listOf(AdvancedAhuRelayMappings.CHANGE_OVER_B_HEATING.ordinal)
            } else { emptyList() }
        }

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
                            setStateChanged(viewModel)
                        },
                        association = relayEnums[relayConfig.association],
                        unit = "",
                        relayEnums = relayEnums,
                        onAssociationChanged = { associationIndex ->
                            relayConfig.association = associationIndex.index
                            setStateChanged(viewModel)
                        },
                        isEnabled = relayConfig.enabled,
                        testState = getRelayStatus(index + 1),
                        onTestActivated = { viewModel.sendCMRelayTestCommand(index, it)},
                        disabledIndices= getDisabledIndices(relayConfig)
                    )

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
                    val analogOut = "Analog-Out${index + 1}"
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
                            setStateChanged(viewModel)
                        },
                        association = analogOutEnums[associationIndex],
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
                            setStateChanged(viewModel)
                        },
                        testVal = getAnalogStatus(index),
                        onTestSignalSelected = {viewModel.sendCMAnalogTestCommand(index, (it * 10))})
                }
            }
        }
    }


    /**
     * This function is used to display the dynamic configuration for the Advanced AHU
     */
    @Composable
    fun CMAnalogOutDynamicConfig(viewModel: AdvancedHybridAhuViewModel) {
        viewModel.viewState.value.noOfAnalogOutDynamic = 0
        PressureBasedFanControl(viewModel)
        SATBasedControl(viewModel)
        CO2BasedDamperControl(viewModel)
        LoadBasedCoolingControl(viewModel)
        LoadBasedFanControl(viewModel)
        HeatLoadBasedControl(viewModel)
        CompositeBasedControl(viewModel)
        CompressorSpeedBasedControl(viewModel)
        viewModel.modelLoadedState.value = true
    }

    @Composable
    fun PressureBasedFanControl(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isPressureEnabled()) {
            val pressureEnum = viewModel.getAllowedValues(DomainName.pressureBasedFanControlOn, viewModel.cmModel)
            val pressureMinMaxList = viewModel.getListByDomainName(DomainName.staticPressureMin, viewModel.cmModel)
            val unit = viewModel.getUnit(DomainName.staticPressureMin, viewModel.cmModel)

            if (viewModel.viewState.value.noOfAnalogOutDynamic > 0) {
                ComposeUtil.DashDivider()
            }
            viewModel.viewState.value.noOfAnalogOutDynamic++

            Column(modifier = Modifier.padding(top = 15.dp, bottom = 10.dp, start = 17.dp)) {
                BoldStyledTextView(PRESSURE_BASED_FC, fontSize = 20)
                Row(modifier = Modifier.padding(top = 10.dp, bottom = 15.dp)) {
                    Box(modifier = Modifier
                        .weight(1f)
                        .padding(top = 10.dp)) {
                        StyledTextView(
                            PRESSURE_BASED_FC_ON, fontSize = 20, textAlignment = TextAlign.Left
                        )
                    }
                    Box(modifier = Modifier
                        .weight(1f)
                        .padding(top = 5.dp)) {
                        SearchSpinnerElement(
                            default = pressureEnum[viewModel.viewState.value.pressureConfig.pressureControlAssociation],
                            allItems = pressureEnum,
                            unit = "",
                            onSelect = {
                                viewModel.viewState.value.pressureConfig.pressureControlAssociation =
                                    it.index
                                setStateChanged(viewModel)
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
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.pressureConfig.staticMaxPressure =
                            it.value.toDouble()
                        setStateChanged(viewModel)
                    })
            }
            PressureBaseMinMaxConfig(viewModel)
        }
    }

    @Composable
    fun PressureBaseMinMaxConfig(viewModel: AdvancedHybridAhuViewModel) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 15.dp)) {
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.PRESSURE_BASED_FAN_CONTROL,
                    viewModel.viewState.value.analogOut1Enabled,
                    viewModel.viewState.value.analogOut1Association
                )
            ) {
                MinMaxConfiguration("Analog-Out1 at Min \nStatic Pressure",
                    "Analog-Out1 at Max \nStatic Pressure",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut1MinMax.staticPressureMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut1MinMax.staticPressureMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut1MinMax.staticPressureMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut1MinMax.staticPressureMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.PRESSURE_BASED_FAN_CONTROL,
                    viewModel.viewState.value.analogOut2Enabled,
                    viewModel.viewState.value.analogOut2Association
                )
            ) {
                MinMaxConfiguration("Analog-Out2 at Min \nStatic Pressure",
                    "Analog-Out2 at Max \nStatic Pressure",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut2MinMax.staticPressureMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut2MinMax.staticPressureMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut2MinMax.staticPressureMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut2MinMax.staticPressureMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.PRESSURE_BASED_FAN_CONTROL,
                    viewModel.viewState.value.analogOut3Enabled,
                    viewModel.viewState.value.analogOut3Association
                )
            ) {
                MinMaxConfiguration("Analog-Out3 at Min \nStatic Pressure",
                    "Analog-Out3 at Max \nStatic Pressure",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut3MinMax.staticPressureMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut3MinMax.staticPressureMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut3MinMax.staticPressureMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut3MinMax.staticPressureMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.PRESSURE_BASED_FAN_CONTROL,
                    viewModel.viewState.value.analogOut4Enabled,
                    viewModel.viewState.value.analogOut4Association
                )
            ) {
                MinMaxConfiguration("Analog-Out4 at Min \nStatic Pressure",
                    "Analog-Out4 at Max \nStatic Pressure",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut4MinMax.staticPressureMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut4MinMax.staticPressureMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut4MinMax.staticPressureMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut4MinMax.staticPressureMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
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

            if (viewModel.viewState.value.noOfAnalogOutDynamic > 0) {
                ComposeUtil.DashDivider()
            }
            viewModel.viewState.value.noOfAnalogOutDynamic++

            Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 17.dp)) {
                BoldStyledTextView(SAT_CONTROL, fontSize = 20)
                Row(modifier = Modifier.padding(top = 10.dp, bottom = 15.dp)) {
                    Box(modifier = Modifier
                        .weight(1f)
                        .padding(top = 10.dp)) {
                        StyledTextView(
                            SAT_CONTROL_ON, fontSize = 20, textAlignment = TextAlign.Left
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SearchSpinnerElement(
                            default = satEnum[viewModel.viewState.value.satConfig.satControlAssociation],
                            allItems = satEnum, unit = "", onSelect = {
                                viewModel.viewState.value.satConfig.satControlAssociation = it.index
                            setStateChanged(viewModel)
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
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.satConfig.systemSatCoolingMax =
                                it.value.toDouble()
                            setStateChanged(viewModel)
                        }, viewModel = viewModel)
                }
                if (viewModel.isSATHeatingEnabled()) {
                    Spacer(modifier = Modifier.padding(top = 5.dp))
                    MinMaxConfiguration(SYSTEM_HEATING_SAT_MIN,
                        SYSTEM_HEATING_SAT_MAX,
                        minMax,
                        unit,
                        minDefault = viewModel.viewState.value.satConfig.systemSatHeatingMin.toInt().toString(),
                        maxDefault = viewModel.viewState.value.satConfig.systemSatHeatingMax.toInt().toString(),
                        onMinSelected = {
                            viewModel.viewState.value.satConfig.systemSatHeatingMin =
                                it.value.toDouble()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.satConfig.systemSatHeatingMax =
                                it.value.toDouble()
                            setStateChanged(viewModel)
                        },viewModel = viewModel)
                }
            }
            SATCoolingMinMaxConfig(viewModel)
            SATHeatingMinMaxConfig(viewModel)
        }
    }

    @Composable
    fun SATCoolingMinMaxConfig(viewModel: AdvancedHybridAhuViewModel) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 15.dp)) {
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.SAT_BASED_COOLING_CONTROL,
                    viewModel.viewState.value.analogOut1Enabled,
                    viewModel.viewState.value.analogOut1Association
                )
            ) {
                MinMaxConfiguration("Analog-Out1 at Min \nSAT Cooling",
                    "Analog-Out1 at Max \nSAT Cooling",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut1MinMax.satCoolingMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut1MinMax.satCoolingMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut1MinMax.satCoolingMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut1MinMax.satCoolingMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.SAT_BASED_COOLING_CONTROL,
                    viewModel.viewState.value.analogOut2Enabled,
                    viewModel.viewState.value.analogOut2Association
                )
            ) {
                MinMaxConfiguration("Analog-Out2 at Min \nSAT Cooling",
                    "Analog-Out2 at Max \nSAT Cooling",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut2MinMax.satCoolingMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut2MinMax.satCoolingMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut2MinMax.satCoolingMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut2MinMax.satCoolingMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.SAT_BASED_COOLING_CONTROL,
                    viewModel.viewState.value.analogOut3Enabled,
                    viewModel.viewState.value.analogOut3Association
                )
            ) {
                MinMaxConfiguration("Analog-Out3 at Min \nSAT Cooling",
                    "Analog-Out3 at Max \nSAT Cooling",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut3MinMax.satCoolingMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut3MinMax.satCoolingMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut3MinMax.satCoolingMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut3MinMax.satCoolingMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.SAT_BASED_COOLING_CONTROL,
                    viewModel.viewState.value.analogOut4Enabled,
                    viewModel.viewState.value.analogOut4Association
                )
            ) {
                MinMaxConfiguration("Analog-Out4 at Min \nSAT Cooling",
                    "Analog-Out4 at Max \nSAT Cooling",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut4MinMax.satCoolingMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut4MinMax.satCoolingMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut4MinMax.satCoolingMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut4MinMax.satCoolingMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
        }
    }

    @Composable
    fun SATHeatingMinMaxConfig(viewModel: AdvancedHybridAhuViewModel) {
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 15.dp)) {
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.SAT_BASED_HEATING_CONTROL,
                    viewModel.viewState.value.analogOut1Enabled,
                    viewModel.viewState.value.analogOut1Association
                )
            ) {
                MinMaxConfiguration("Analog-Out1 at Min \nSAT Heating",
                    "Analog-Out1 at Max \nSAT Heating",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut1MinMax.satHeatingMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut1MinMax.satHeatingMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut1MinMax.satHeatingMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut1MinMax.satHeatingMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.SAT_BASED_HEATING_CONTROL,
                    viewModel.viewState.value.analogOut2Enabled,
                    viewModel.viewState.value.analogOut2Association
                )
            ) {
                MinMaxConfiguration("Analog-Out2 at Min \nSAT Heating",
                    "Analog-Out2 at Max \nSAT Heating",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut2MinMax.satHeatingMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut2MinMax.satHeatingMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut2MinMax.satHeatingMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut2MinMax.satHeatingMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.SAT_BASED_HEATING_CONTROL,
                    viewModel.viewState.value.analogOut3Enabled,
                    viewModel.viewState.value.analogOut3Association
                )
            ) {
                MinMaxConfiguration("Analog-Out3 at Min \nSAT Heating",
                    "Analog-Out3 at Max \nSAT Heating",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut3MinMax.satHeatingMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut3MinMax.satHeatingMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut3MinMax.satHeatingMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut3MinMax.satHeatingMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.SAT_BASED_HEATING_CONTROL,
                    viewModel.viewState.value.analogOut4Enabled,
                    viewModel.viewState.value.analogOut4Association
                )
            ) {
                MinMaxConfiguration("Analog-Out4 at Min \nSAT Heating",
                    "Analog-Out4 at Max \nSAT Heating",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut4MinMax.satHeatingMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut4MinMax.satHeatingMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut4MinMax.satHeatingMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut4MinMax.satHeatingMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
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

            if (viewModel.viewState.value.noOfAnalogOutDynamic > 0) {
                ComposeUtil.DashDivider()
            }
            viewModel.viewState.value.noOfAnalogOutDynamic++

            Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 17.dp)) {
                BoldStyledTextView(CO2_DAMPER_CONTROL, fontSize = 20)
                Row(modifier = Modifier.padding(top = 10.dp, bottom = 15.dp)) {
                    Box(modifier = Modifier
                        .weight(1f)
                        .padding(top = 10.dp)) {
                        StyledTextView(
                            CO2_DAMPER_CONTROL_ON, fontSize = 20, textAlignment = TextAlign.Left
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SearchSpinnerElement(
                            default = damperEnum[viewModel.viewState.value.damperConfig.damperControlAssociation],
                            allItems = damperEnum,
                            unit = "",
                            onSelect = {
                                viewModel.viewState.value.damperConfig.damperControlAssociation =
                                    it.index
                                setStateChanged(viewModel)
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
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.damperConfig.co2Target = it.value.toDouble()
                        setStateChanged(viewModel)
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
                                setStateChanged(viewModel)
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
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 15.dp)) {
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.CO2_BASED_DAMPER_CONTROL,
                    viewModel.viewState.value.analogOut1Enabled,
                    viewModel.viewState.value.analogOut1Association
                )
            ) {
                MinMaxConfiguration("Analog-Out1 at Min \nDCV Damper Pos",
                    "Analog-Out1 at Max \nDCV Damper Pos",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut1MinMax.damperPosMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut1MinMax.damperPosMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut1MinMax.damperPosMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut1MinMax.damperPosMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.CO2_BASED_DAMPER_CONTROL,
                    viewModel.viewState.value.analogOut2Enabled,
                    viewModel.viewState.value.analogOut2Association
                )
            ) {
                MinMaxConfiguration("Analog-Out2 at Min \nDCV Damper Pos",
                    "Analog-Out2 at Max \nDCV Damper Pos",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut2MinMax.damperPosMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut2MinMax.damperPosMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut2MinMax.damperPosMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut2MinMax.damperPosMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.CO2_BASED_DAMPER_CONTROL,
                    viewModel.viewState.value.analogOut3Enabled,
                    viewModel.viewState.value.analogOut3Association
                )
            ) {
                MinMaxConfiguration("Analog-Out3 at Min \nDCV Damper Pos",
                    "Analog-Out3 at Max \nDCV Damper Pos",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut3MinMax.damperPosMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut3MinMax.damperPosMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut3MinMax.damperPosMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut3MinMax.damperPosMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ControlType.CO2_BASED_DAMPER_CONTROL,
                    viewModel.viewState.value.analogOut4Enabled,
                    viewModel.viewState.value.analogOut4Association
                )
            ) {
                MinMaxConfiguration("Analog-Out4 at Min \nDCV Damper Pos",
                    "Analog-Out4 at Max \nDCV Damper Pos",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.analogOut4MinMax.damperPosMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.analogOut4MinMax.damperPosMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.analogOut4MinMax.damperPosMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.analogOut4MinMax.damperPosMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
        }
    }

    @Composable
    fun LoadBasedCoolingControl(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isCoolingLoadEnabled()) {

            if (viewModel.viewState.value.noOfAnalogOutDynamic > 0) {
                ComposeUtil.DashDivider()
            }
            viewModel.viewState.value.noOfAnalogOutDynamic++

        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 17.dp)) {

                BoldStyledTextView(COOLING_CONTROL, fontSize = 20)
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_COOLING_CONTROL,
                        viewModel.viewState.value.analogOut1Enabled,
                        viewModel.viewState.value.analogOut1Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out1 at Min \nCooling",
                        "Analog-Out1 at Max \nCooling",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut1MinMax.coolingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut1MinMax.coolingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut1MinMax.coolingMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut1MinMax.coolingMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_COOLING_CONTROL,
                        viewModel.viewState.value.analogOut2Enabled,
                        viewModel.viewState.value.analogOut2Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out2 at Min \nCooling",
                        "Analog-Out2 at Max \nCooling",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut2MinMax.coolingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut2MinMax.coolingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut2MinMax.coolingMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut2MinMax.coolingMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_COOLING_CONTROL,
                        viewModel.viewState.value.analogOut3Enabled,
                        viewModel.viewState.value.analogOut3Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out3 at Min \nCooling",
                        "Analog-Out3 at Max \nCooling",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut3MinMax.coolingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut3MinMax.coolingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut3MinMax.coolingMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut3MinMax.coolingMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_COOLING_CONTROL,
                        viewModel.viewState.value.analogOut4Enabled,
                        viewModel.viewState.value.analogOut4Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out4 at Min \nCooling",
                        "Analog-Out4 at Max \nCooling",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut4MinMax.coolingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut4MinMax.coolingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut4MinMax.coolingMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut4MinMax.coolingMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
            }
        }
    }

    @Composable
    fun LoadBasedFanControl(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isFanLoadEnabled()) {

            if (viewModel.viewState.value.noOfAnalogOutDynamic > 0) {
                ComposeUtil.DashDivider()
            }
            viewModel.viewState.value.noOfAnalogOutDynamic++

        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 17.dp)) {

                BoldStyledTextView(FAN_CONTROL, fontSize = 20)
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_FAN_CONTROL,
                        viewModel.viewState.value.analogOut1Enabled,
                        viewModel.viewState.value.analogOut1Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out1 at Min \nFan",
                        "Analog-Out1 at Max \nFan",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut1MinMax.fanMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut1MinMax.fanMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut1MinMax.fanMinVoltage =
                                it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut1MinMax.fanMaxVoltage =
                                it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_FAN_CONTROL,
                        viewModel.viewState.value.analogOut2Enabled,
                        viewModel.viewState.value.analogOut2Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out2 at Min \nFan",
                        "Analog-Out2 at Max \nFan",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut2MinMax.fanMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut2MinMax.fanMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut2MinMax.fanMinVoltage =
                                it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut2MinMax.fanMaxVoltage =
                                it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_FAN_CONTROL,
                        viewModel.viewState.value.analogOut3Enabled,
                        viewModel.viewState.value.analogOut3Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out3 at Min \nFan",
                        "Analog-Out3 at Max \nFan",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut3MinMax.fanMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut3MinMax.fanMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut3MinMax.fanMinVoltage =
                                it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut3MinMax.fanMaxVoltage =
                                it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_FAN_CONTROL,
                        viewModel.viewState.value.analogOut4Enabled,
                        viewModel.viewState.value.analogOut4Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out4 at Min \nFan",
                        "Analog-Out4 at Max \nFan",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut4MinMax.fanMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut4MinMax.fanMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut4MinMax.fanMinVoltage =
                                it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut4MinMax.fanMaxVoltage =
                                it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
            }

        }
    }

    @Composable
    fun HeatLoadBasedControl(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isHeatLoadEnabled()) {

            if (viewModel.viewState.value.noOfAnalogOutDynamic > 0) {
                ComposeUtil.DashDivider()
            }
            viewModel.viewState.value.noOfAnalogOutDynamic++

        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 17.dp)) {

                BoldStyledTextView(HEATING_CONTROL, fontSize = 20)
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_HEATING_CONTROL,
                        viewModel.viewState.value.analogOut1Enabled,
                        viewModel.viewState.value.analogOut1Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out1 at Min \nHeating",
                        "Analog-Out1 at Max \nHeating",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut1MinMax.heatingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut1MinMax.heatingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut1MinMax.heatingMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut1MinMax.heatingMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_HEATING_CONTROL,
                        viewModel.viewState.value.analogOut2Enabled,
                        viewModel.viewState.value.analogOut2Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out2 at Min \nHeating",
                        "Analog-Out2 at Max \nHeating",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut2MinMax.heatingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut2MinMax.heatingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut2MinMax.heatingMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut2MinMax.heatingMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_HEATING_CONTROL,
                        viewModel.viewState.value.analogOut3Enabled,
                        viewModel.viewState.value.analogOut3Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out3 at Min \nHeating",
                        "Analog-Out3 at Max \nHeating",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut3MinMax.heatingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut3MinMax.heatingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut3MinMax.heatingMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut3MinMax.heatingMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.LOAD_BASED_HEATING_CONTROL,
                        viewModel.viewState.value.analogOut4Enabled,
                        viewModel.viewState.value.analogOut4Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out4 at Min \nHeating",
                        "Analog-Out4 at Max \nHeating",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut4MinMax.heatingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut4MinMax.heatingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut4MinMax.heatingMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut4MinMax.heatingMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
            }
        }
    }

    @Composable
    fun CompressorSpeedBasedControl(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isCompressorSpeedEnabled()) {

            if (viewModel.viewState.value.noOfAnalogOutDynamic > 0) {
                ComposeUtil.DashDivider()
            }
            viewModel.viewState.value.noOfAnalogOutDynamic++

            Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 17.dp)) {

                BoldStyledTextView(COMPRESSOR_SPEED, fontSize = 20)
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.COMPRESSOR_SPEED,
                        viewModel.viewState.value.analogOut1Enabled,
                        viewModel.viewState.value.analogOut1Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out1 at Min \nCompressor Speed",
                        "Analog-Out1 at Max \nCompressor Speed",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut1MinMax.compressorMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut1MinMax.compressorMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut1MinMax.compressorMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut1MinMax.compressorMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.COMPRESSOR_SPEED,
                        viewModel.viewState.value.analogOut2Enabled,
                        viewModel.viewState.value.analogOut2Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out2 at Min \nCompressor Speed",
                        "Analog-Out2 at Max \nCompressor Speed",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut2MinMax.compressorMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut2MinMax.compressorMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut2MinMax.compressorMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut2MinMax.compressorMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.COMPRESSOR_SPEED,
                        viewModel.viewState.value.analogOut3Enabled,
                        viewModel.viewState.value.analogOut3Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out3 at Min \nCompressor Speed",
                        "Analog-Out3 at Max \nCompressor Speed",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut3MinMax.compressorMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut3MinMax.compressorMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut3MinMax.compressorMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut3MinMax.compressorMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ControlType.COMPRESSOR_SPEED,
                        viewModel.viewState.value.analogOut4Enabled,
                        viewModel.viewState.value.analogOut4Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out4 at Min \nCompressor Speed",
                        "Analog-Out4 at Max \nCompressor Speed",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.analogOut4MinMax.compressorMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.analogOut4MinMax.compressorMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.analogOut4MinMax.compressorMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.analogOut4MinMax.compressorMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
            }
        }
    }


    @Composable
    fun CompositeBasedControl(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isCompositeEnabled()) {

            if (viewModel.viewState.value.noOfAnalogOutDynamic > 0) {
                ComposeUtil.DashDivider()
            }
            viewModel.viewState.value.noOfAnalogOutDynamic++

            Column(modifier = Modifier.padding(bottom = 10.dp, start = 17.dp)) {

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
            MinMaxConfiguration("Analog-Out1 at Min \nCooling",
                "Analog-Out1 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.analogOut1MinMax.compositeCoolingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.analogOut1MinMax.compositeCoolingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.analogOut1MinMax.compositeCoolingMinVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                },
                onMaxSelected = {
                    viewModel.viewState.value.analogOut1MinMax.compositeCoolingMaxVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ControlType.COMPOSITE,
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association
            )
        ) {
            MinMaxConfiguration("Analog-Out2 at Min \nCooling",
                "Analog-Out2 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.analogOut2MinMax.compositeCoolingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.analogOut2MinMax.compositeCoolingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.analogOut2MinMax.compositeCoolingMinVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                },
                onMaxSelected = {
                    viewModel.viewState.value.analogOut2MinMax.compositeCoolingMaxVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ControlType.COMPOSITE,
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association
            )
        ) {
            MinMaxConfiguration("Analog-Out3 at Min \nCooling",
                "Analog-Out3 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.analogOut3MinMax.compositeCoolingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.analogOut3MinMax.compositeCoolingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.analogOut3MinMax.compositeCoolingMinVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                },
                onMaxSelected = {
                    viewModel.viewState.value.analogOut3MinMax.compositeCoolingMaxVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ControlType.COMPOSITE,
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association
            )
        ) {
            MinMaxConfiguration("Analog-Out4 at Min \nCooling",
                "Analog-Out4 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.analogOut4MinMax.compositeCoolingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.analogOut4MinMax.compositeCoolingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.analogOut4MinMax.compositeCoolingMinVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                },
                onMaxSelected = {
                    viewModel.viewState.value.analogOut4MinMax.compositeCoolingMaxVoltage = it.value.toInt()
                    setStateChanged(viewModel)
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
            MinMaxConfiguration("Analog-Out1 at Min \nHeating",
                "Analog-Out1 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.analogOut1MinMax.compositeHeatingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.analogOut1MinMax.compositeHeatingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.analogOut1MinMax.compositeHeatingMinVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                },
                onMaxSelected = {
                    viewModel.viewState.value.analogOut1MinMax.compositeHeatingMaxVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ControlType.COMPOSITE,
                viewModel.viewState.value.analogOut2Enabled,
                viewModel.viewState.value.analogOut2Association
            )
        ) {
            MinMaxConfiguration("Analog-Out2 at Min \nHeating",
                "Analog-Out2 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.analogOut2MinMax.compositeHeatingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.analogOut2MinMax.compositeHeatingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.analogOut2MinMax.compositeHeatingMinVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                },
                onMaxSelected = {
                    viewModel.viewState.value.analogOut2MinMax.compositeHeatingMaxVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ControlType.COMPOSITE,
                viewModel.viewState.value.analogOut3Enabled,
                viewModel.viewState.value.analogOut3Association
            )
        ) {
            MinMaxConfiguration("Analog-Out3 at Min \nHeating",
                "Analog-Out3 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.analogOut3MinMax.compositeHeatingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.analogOut3MinMax.compositeHeatingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.analogOut3MinMax.compositeHeatingMinVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                },
                onMaxSelected = {
                    viewModel.viewState.value.analogOut3MinMax.compositeHeatingMaxVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ControlType.COMPOSITE,
                viewModel.viewState.value.analogOut4Enabled,
                viewModel.viewState.value.analogOut4Association
            )
        ) {
            MinMaxConfiguration("Analog-Out4 at Min \nHeating",
                "Analog-Out4 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.analogOut4MinMax.compositeHeatingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.analogOut4MinMax.compositeHeatingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.analogOut4MinMax.compositeHeatingMinVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                },
                onMaxSelected = {
                    viewModel.viewState.value.analogOut4MinMax.compositeHeatingMaxVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                })
        }
    }

    @Composable
    fun AddConnectModule(viewModel: AdvancedHybridAhuViewModel) {
        Box(modifier = Modifier.padding(top = 10.dp)) {
            SaveTextViewNew(text = "ADD CONNECT MODULE") {
                viewModel.viewState.value.isConnectEnabled = true
                setStateChanged(viewModel)
            }
        }
    }

    @Composable
    fun ConnectSensorConfig(viewModel: AdvancedHybridAhuViewModel) {
        val temperatureEnums = viewModel.getAllowedValues(DomainName.temperatureSensorBusAdd1, viewModel.connectModel)
        val humidityEnums = viewModel.getAllowedValues(DomainName.humiditySensorBusAdd1, viewModel.connectModel)
        val occupancyEnum = viewModel.getAllowedValues(DomainName.occupancySensorBusAdd0, viewModel.connectModel)
        val co2Enum = viewModel.getAllowedValues(DomainName.co2SensorBusAdd0, viewModel.connectModel)
        val pressureEnum = viewModel.getAllowedValues(DomainName.pressureSensorBusAdd0, viewModel.connectModel)
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(10.dp)) {
            Text(
                modifier = Modifier
                    .height(50.dp)
                    .padding(start = 5.dp, top = 10.dp),
                style = TextStyle(
                    textAlign = TextAlign.Start,
                    fontWeight = FontWeight.Normal,
                    fontFamily = myFontFamily,
                    fontSize =  19.5.sp,
                    color = Color.Black
                ),
                text = "Outside Air Optimization Damper"
            )
            Spacer(modifier = Modifier.width(15.dp))
            ToggleButtonStateful(
                defaultSelection = viewModel.viewState.value.enableOutsideAirOptimization,
                onEnabled = {
                    viewModel.viewState.value.enableOutsideAirOptimization = it
                    setStateChanged(viewModel)
                }
            )
        }

        Spacer(modifier = Modifier.height(15.dp))
        SubTitle(SENSOR_BUS, fontSizeCustom = 16.0, startPaddingValue = 18, topPaddingValue = 0)
        Row {
            EnableCompose(ADDRESS_0, viewModel.viewState.value.connectSensorAddress0.enabled) {
                viewModel.viewState.value.connectSensorAddress0.enabled = it
                setStateChanged(viewModel)
            }
            Column {
                ConfigCompose(
                    TEMPERATURE,
                    temperatureEnums[viewModel.viewState.value.connectSensorAddress0.temperatureAssociation],
                    temperatureEnums,
                    "",
                    viewModel.viewState.value.connectSensorAddress0.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress0.temperatureAssociation = it.index
                    viewModel.viewState.value.connectSensorAddress0.humidityAssociation = it.index
                    setStateChanged(viewModel)
                }
                HumidityCompose(
                    HUMIDITY,
                    humidityEnums[viewModel.viewState.value.connectSensorAddress0.humidityAssociation].value
                )
                ConfigCompose(
                    OCCUPANCY,
                    occupancyEnum[viewModel.viewState.value.connectSensorAddress0.occupancyAssociation],
                    occupancyEnum,
                    "",
                    viewModel.viewState.value.connectSensorAddress0.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress0.occupancyAssociation = it.index
                    setStateChanged(viewModel)
                }
                ConfigCompose(
                    CO2,
                    co2Enum[viewModel.viewState.value.connectSensorAddress0.co2Association],
                    co2Enum,
                    "",
                    viewModel.viewState.value.connectSensorAddress0.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress0.co2Association = it.index
                    setStateChanged(viewModel)
                }
            }
        }
        Row {
            EnableCompose(
                "", viewModel.viewState.value.connectSensorBusPressureEnable, hide = true
            ) {
                viewModel.viewState.value.connectSensorBusPressureEnable = it
                setStateChanged(viewModel)
            }
            Column {
                ConfigCompose(
                    PRESSURE,
                    pressureEnum[viewModel.viewState.value.connectSensorAddress0.pressureAssociation],
                    pressureEnum,
                    "",
                    viewModel.viewState.value.connectSensorBusPressureEnable
                ) {
                    viewModel.viewState.value.connectSensorAddress0.pressureAssociation = it.index
                    setStateChanged(viewModel)
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
                    temperatureEnums[viewModel.viewState.value.connectSensorAddress1.temperatureAssociation],
                    temperatureEnums,
                    "",
                    viewModel.viewState.value.connectSensorAddress1.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress1.temperatureAssociation = it.index
                    viewModel.viewState.value.connectSensorAddress1.humidityAssociation = it.index
                    setStateChanged(viewModel)
                }
                HumidityCompose(
                    HUMIDITY,
                    humidityEnums[viewModel.viewState.value.connectSensorAddress1.humidityAssociation].value
                )
                ConfigCompose(
                    OCCUPANCY,
                    occupancyEnum[viewModel.viewState.value.connectSensorAddress1.occupancyAssociation],
                    occupancyEnum,
                    "",
                    viewModel.viewState.value.connectSensorAddress1.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress1.occupancyAssociation = it.index
                    setStateChanged(viewModel)
                }
                ConfigCompose(
                    CO2,
                    co2Enum[viewModel.viewState.value.connectSensorAddress1.co2Association],
                    co2Enum,
                    "",
                    viewModel.viewState.value.connectSensorAddress1.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress1.co2Association = it.index
                    setStateChanged(viewModel)
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
                    temperatureEnums[viewModel.viewState.value.connectSensorAddress2.temperatureAssociation],
                    temperatureEnums,
                    "",
                    viewModel.viewState.value.connectSensorAddress2.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress2.temperatureAssociation = it.index
                    viewModel.viewState.value.connectSensorAddress2.humidityAssociation = it.index
                    setStateChanged(viewModel)
                }
                HumidityCompose(
                    HUMIDITY,
                    humidityEnums[viewModel.viewState.value.connectSensorAddress2.humidityAssociation].value
                )
                ConfigCompose(
                    OCCUPANCY,
                    occupancyEnum[viewModel.viewState.value.connectSensorAddress2.occupancyAssociation],
                    occupancyEnum,
                    "",
                    viewModel.viewState.value.connectSensorAddress2.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress2.occupancyAssociation = it.index
                    setStateChanged(viewModel)
                }
                ConfigCompose(
                    CO2,
                    co2Enum[viewModel.viewState.value.connectSensorAddress2.co2Association],
                    co2Enum,
                    "",
                    viewModel.viewState.value.connectSensorAddress2.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress2.co2Association = it.index
                    setStateChanged(viewModel)
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
                    temperatureEnums[viewModel.viewState.value.connectSensorAddress3.temperatureAssociation],
                    temperatureEnums,
                    "",
                    viewModel.viewState.value.connectSensorAddress3.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress3.temperatureAssociation = it.index
                    viewModel.viewState.value.connectSensorAddress3.humidityAssociation = it.index
                    setStateChanged(viewModel)
                }
                HumidityCompose(
                    HUMIDITY,
                    humidityEnums[viewModel.viewState.value.connectSensorAddress3.humidityAssociation].value
                )
                ConfigCompose(
                    OCCUPANCY,
                    occupancyEnum[viewModel.viewState.value.connectSensorAddress3.occupancyAssociation],
                    occupancyEnum,
                    "",
                    viewModel.viewState.value.connectSensorAddress3.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress3.occupancyAssociation = it.index
                    setStateChanged(viewModel)
                }
                ConfigCompose(
                    CO2,
                    co2Enum[viewModel.viewState.value.connectSensorAddress3.co2Association],
                    co2Enum,
                    "",
                    viewModel.viewState.value.connectSensorAddress3.enabled
                ) {
                    viewModel.viewState.value.connectSensorAddress3.co2Association = it.index
                    setStateChanged(viewModel)
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

        Row(modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)) {
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
                    { viewModel.viewState.value.connectUniversalIn1Config.enabled = it
                        setStateChanged(viewModel)},
                    universalEnum[viewModel.viewState.value.connectUniversalIn1Config.association],
                    universalEnum,
                    "",
                    viewModel.viewState.value.connectUniversalIn1Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn1Config.association = it.index
                        setStateChanged(viewModel)})
                AOTHConfig(UNIVERSAL_IN2,
                    viewModel.viewState.value.connectUniversalIn2Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn2Config.enabled = it
                        setStateChanged(viewModel)},
                    universalEnum[viewModel.viewState.value.connectUniversalIn2Config.association],
                    universalEnum,
                    "",
                    viewModel.viewState.value.connectUniversalIn2Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn2Config.association = it.index
                        setStateChanged(viewModel)})
                AOTHConfig(UNIVERSAL_IN3,
                    viewModel.viewState.value.connectUniversalIn3Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn3Config.enabled = it
                        setStateChanged(viewModel)},
                    universalEnum[viewModel.viewState.value.connectUniversalIn3Config.association],
                    universalEnum,
                    "",
                    viewModel.viewState.value.connectUniversalIn3Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn3Config.association = it.index
                        setStateChanged(viewModel)})
                AOTHConfig(UNIVERSAL_IN4,
                    viewModel.viewState.value.connectUniversalIn4Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn4Config.enabled = it
                        setStateChanged(viewModel)},
                    universalEnum[viewModel.viewState.value.connectUniversalIn4Config.association],
                    universalEnum,
                    "",
                    viewModel.viewState.value.connectUniversalIn4Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn4Config.association = it.index
                        setStateChanged(viewModel)})
                AOTHConfig(UNIVERSAL_IN5,
                    viewModel.viewState.value.connectUniversalIn5Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn5Config.enabled = it
                        setStateChanged(viewModel)},
                    universalEnum[viewModel.viewState.value.connectUniversalIn5Config.association],
                    universalEnum,
                    "",
                    viewModel.viewState.value.connectUniversalIn5Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn5Config.association = it.index
                        setStateChanged(viewModel)})
                AOTHConfig(UNIVERSAL_IN6,
                    viewModel.viewState.value.connectUniversalIn6Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn6Config.enabled = it
                        setStateChanged(viewModel)},
                    universalEnum[viewModel.viewState.value.connectUniversalIn6Config.association],
                    universalEnum,
                    "",
                    viewModel.viewState.value.connectUniversalIn6Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn6Config.association = it.index
                        setStateChanged(viewModel)})
                AOTHConfig(UNIVERSAL_IN7,
                    viewModel.viewState.value.connectUniversalIn7Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn7Config.enabled = it
                        setStateChanged(viewModel)},
                    universalEnum[viewModel.viewState.value.connectUniversalIn7Config.association],
                    universalEnum,
                    "",
                    viewModel.viewState.value.connectUniversalIn7Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn7Config.association = it.index
                        setStateChanged(viewModel)})
                AOTHConfig(UNIVERSAL_IN8,
                    viewModel.viewState.value.connectUniversalIn8Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn8Config.enabled = it
                        setStateChanged(viewModel)},
                    universalEnum[viewModel.viewState.value.connectUniversalIn8Config.association],
                    universalEnum,
                    "",
                    viewModel.viewState.value.connectUniversalIn8Config.enabled,
                    { viewModel.viewState.value.connectUniversalIn8Config.association = it.index
                        setStateChanged(viewModel)})
            }
        }
    }

    @Composable
    fun ConnectRelayConfig(viewModel: AdvancedHybridAhuViewModel) {
        val relayEnums = viewModel.getAllowedValues(DomainName.relay1OutputAssociation, viewModel.connectModel)

        fun getDisabledIndices(config: ConfigState): List<Int> {
            return if (viewModel.viewState.value.isAnyConnectRelayMapped(28,config)) { // 28 is changeOverHeating
                listOf(27)
            } else if (viewModel.viewState.value.isAnyConnectRelayMapped(27, config)) { // 27 is changeOverCooling
                listOf(28)
            } else {
                emptyList()
            }
        }
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
                        onEnabledChanged = { enabled -> relayConfig.enabled = enabled
                            setStateChanged(viewModel)},
                        association = relayEnums[relayConfig.association],
                        unit = "",
                        relayEnums = relayEnums,
                        isEnabled = relayConfig.enabled,
                        onAssociationChanged = { associationIndex ->
                            relayConfig.association = associationIndex.index
                            setStateChanged(viewModel)
                        },
                        testState = if(viewModel.isConnectModulePaired) viewModel.getConnectPhysicalPointForRelayIndex(index)?.let { it.readHisVal() > 0 } ?: false else false,
                        onTestActivated = {viewModel.sendConnectRelayTestCommand(index, it)},
                        disabledIndices= getDisabledIndices(relayConfig)
                    )
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
                    val analogOut = "Analog-Out${index + 1}"
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
                            setStateChanged(viewModel)
                        },
                        association = analogOutEnums[associationIndex],
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
                            setStateChanged(viewModel)
                        },
                        testVal =  viewModel.getConnectPhysicalPointForAnalogIndex(index)?.readHisVal()?.div(10) ?: 0.0,
                        onTestSignalSelected = {viewModel.sendConnectAnalogTestCommand(index, it * 10)},
                        disabledIndices = if (viewModel.isOaoPairedInSystemLevel || !viewModel.viewState.value.enableOutsideAirOptimization) {
                            //if OAO is already paired in system level disabling the option in the drop down
                            // for OAO Damper and return damper
                          listOf(ConnectControlType.OAO_DAMPER.ordinal, ConnectControlType.RETURN_DAMPER.ordinal)
                        } else {
                            listOf()
                        }
                    )
                }
            }
        }
    }

    private fun getRelayStatus(relay: Int): Boolean {
        return when(L.ccu().systemProfile) {
            is VavAdvancedAhu -> (L.ccu().systemProfile as VavAdvancedAhu).cmRelayStatus.get(relay)
            is DabAdvancedAhu -> (L.ccu().systemProfile as DabAdvancedAhu).cmRelayStatus.get(relay)
            else -> { false }
        }
    }
    private fun getAnalogStatus(analog: Int): Double {
        return when(L.ccu().systemProfile) {
            is VavAdvancedAhu -> (L.ccu().systemProfile as VavAdvancedAhu).analogStatus[analog]
            is DabAdvancedAhu -> (L.ccu().systemProfile as DabAdvancedAhu).analogStatus[analog]
            else -> { 0.0 }
        }
    }

    @Composable
    fun ConnectAnalogOutDynamicConfig(viewModel: AdvancedHybridAhuViewModel) {
        viewModel.viewState.value.noOfAnalogOutDynamic = 0
        ConnectCO2BasedDamperControl(viewModel)
        ConnectLoadBasedCoolingControl(viewModel)
        ConnectLoadBasedFanControl(viewModel)
        ConnectHeatLoadBasedControl(viewModel)
        ConnectCompositeBasedControl(viewModel)
        ConnectCompressorSpeedBasedControl(viewModel)
    }

    @Composable
    fun ConnectLoadBasedCoolingControl(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isConnectCoolingLoadEnabled()) {
            if (viewModel.viewState.value.noOfAnalogOutDynamic > 0) {
                ComposeUtil.DashDivider()
            }
            viewModel.viewState.value.noOfAnalogOutDynamic++

         Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 10.dp)) {

                BoldStyledTextView(COOLING_CONTROL, fontSize = 20)
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_COOLING_CONTROL,
                        viewModel.viewState.value.connectAnalogOut1Enabled,
                        viewModel.viewState.value.connectAnalogOut1Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out1 at Min \nCooling",
                        "Analog-Out1 at Max \nCooling",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut1MinMax.coolingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut1MinMax.coolingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut1MinMax.coolingMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut1MinMax.coolingMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_COOLING_CONTROL,
                        viewModel.viewState.value.connectAnalogOut2Enabled,
                        viewModel.viewState.value.connectAnalogOut2Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out2 at Min \nCooling",
                        "Analog-Out2 at Max \nCooling",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut2MinMax.coolingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut2MinMax.coolingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut2MinMax.coolingMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut2MinMax.coolingMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_COOLING_CONTROL,
                        viewModel.viewState.value.connectAnalogOut3Enabled,
                        viewModel.viewState.value.connectAnalogOut3Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out3 at Min \nCooling",
                        "Analog-Out3 at Max \nCooling",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut3MinMax.coolingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut3MinMax.coolingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut3MinMax.coolingMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut3MinMax.coolingMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_COOLING_CONTROL,
                        viewModel.viewState.value.connectAnalogOut4Enabled,
                        viewModel.viewState.value.connectAnalogOut4Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out4 at Min \nCooling",
                        "Analog-Out4 at Max \nCooling",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut4MinMax.coolingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut4MinMax.coolingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut4MinMax.coolingMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut4MinMax.coolingMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
            }
        }
    }

    @Composable
    fun ConnectLoadBasedFanControl(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isConnectFanLoadEnabled()) {
            if (viewModel.viewState.value.noOfAnalogOutDynamic > 0) {
                ComposeUtil.DashDivider()
            }
            viewModel.viewState.value.noOfAnalogOutDynamic++
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 17.dp)) {

                BoldStyledTextView(FAN_CONTROL, fontSize = 20)
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_FAN_CONTROL,
                        viewModel.viewState.value.connectAnalogOut1Enabled,
                        viewModel.viewState.value.connectAnalogOut1Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out1 at Min \nFan",
                        "Analog-Out1 at Max \nFan",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut1MinMax.fanMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut1MinMax.fanMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut1MinMax.fanMinVoltage =
                                it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut1MinMax.fanMaxVoltage =
                                it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_FAN_CONTROL,
                        viewModel.viewState.value.connectAnalogOut2Enabled,
                        viewModel.viewState.value.connectAnalogOut2Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out2 at Min \nFan",
                        "Analog-Out2 at Max \nFan",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut2MinMax.fanMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut2MinMax.fanMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut2MinMax.fanMinVoltage =
                                it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut2MinMax.fanMaxVoltage =
                                it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_FAN_CONTROL,
                        viewModel.viewState.value.connectAnalogOut3Enabled,
                        viewModel.viewState.value.connectAnalogOut3Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out3 at Min \nFan",
                        "Analog-Out3 at Max \nFan",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut3MinMax.fanMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut3MinMax.fanMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut3MinMax.fanMinVoltage =
                                it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut3MinMax.fanMaxVoltage =
                                it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_FAN_CONTROL,
                        viewModel.viewState.value.connectAnalogOut4Enabled,
                        viewModel.viewState.value.connectAnalogOut4Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out4 at Min \nFan",
                        "Analog-Out4 at Max \nFan",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut4MinMax.fanMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut4MinMax.fanMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut4MinMax.fanMinVoltage =
                                it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut4MinMax.fanMaxVoltage =
                                it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
            }

        }
    }

    @Composable
    fun ConnectHeatLoadBasedControl(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isConnectHeatLoadEnabled()) {

            if (viewModel.viewState.value.noOfAnalogOutDynamic > 0) {
                ComposeUtil.DashDivider()
            }
            viewModel.viewState.value.noOfAnalogOutDynamic++
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 17.dp)) {

                BoldStyledTextView(HEATING_CONTROL, fontSize = 20)
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_HEATING_CONTROL,
                        viewModel.viewState.value.connectAnalogOut1Enabled,
                        viewModel.viewState.value.connectAnalogOut1Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out1 at Min \nHeating",
                        "Analog-Out1 at Max \nHeating",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut1MinMax.heatingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut1MinMax.heatingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut1MinMax.heatingMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut1MinMax.heatingMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_HEATING_CONTROL,
                        viewModel.viewState.value.connectAnalogOut2Enabled,
                        viewModel.viewState.value.connectAnalogOut2Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out2 at Min \nHeating",
                        "Analog-Out2 at Max \nHeating",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut2MinMax.heatingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut2MinMax.heatingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut2MinMax.heatingMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut2MinMax.heatingMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_HEATING_CONTROL,
                        viewModel.viewState.value.connectAnalogOut3Enabled,
                        viewModel.viewState.value.connectAnalogOut3Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out3 at Min \nHeating",
                        "Analog-Out3 at Max \nHeating",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut3MinMax.heatingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut3MinMax.heatingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut3MinMax.heatingMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut3MinMax.heatingMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.LOAD_BASED_HEATING_CONTROL,
                        viewModel.viewState.value.connectAnalogOut4Enabled,
                        viewModel.viewState.value.connectAnalogOut4Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out4 at Min \nHeating",
                        "Analog-Out4 at Max \nHeating",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut4MinMax.heatingMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut4MinMax.heatingMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut4MinMax.heatingMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut4MinMax.heatingMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
            }
        }
    }
    @Composable
    fun ConnectCompressorSpeedBasedControl(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isConnectCompressorSpeedEnabled()) {

            if (viewModel.viewState.value.noOfAnalogOutDynamic > 0) {
                ComposeUtil.DashDivider()
            }
            viewModel.viewState.value.noOfAnalogOutDynamic++
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 17.dp)) {

                BoldStyledTextView(COMPRESSOR_SPEED, fontSize = 20)
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.COMPRESSOR_SPEED,
                        viewModel.viewState.value.connectAnalogOut1Enabled,
                        viewModel.viewState.value.connectAnalogOut1Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out1 at Min \nCompressor Speed",
                        "Analog-Out1 at Max \nCompressor Speed",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut1MinMax.compressorMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut1MinMax.compressorMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut1MinMax.compressorMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut1MinMax.compressorMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.COMPRESSOR_SPEED,
                        viewModel.viewState.value.connectAnalogOut2Enabled,
                        viewModel.viewState.value.connectAnalogOut2Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out2 at Min \nCompressor Speed",
                        "Analog-Out2 at Max \nCompressor Speed",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut2MinMax.compressorMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut2MinMax.compressorMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut2MinMax.compressorMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut2MinMax.compressorMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.COMPRESSOR_SPEED,
                        viewModel.viewState.value.connectAnalogOut3Enabled,
                        viewModel.viewState.value.connectAnalogOut3Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out3 at Min \nCompressor Speed",
                        "Analog-Out3 at Max \nCompressor Speed",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut3MinMax.compressorMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut3MinMax.compressorMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut3MinMax.compressorMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut3MinMax.compressorMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
                if (viewModel.isAnalogEnabledAndMapped(
                        ConnectControlType.COMPRESSOR_SPEED,
                        viewModel.viewState.value.connectAnalogOut4Enabled,
                        viewModel.viewState.value.connectAnalogOut4Association
                    )
                ) {
                    MinMaxConfiguration("Analog-Out4 at Min \nCompressor Speed",
                        "Analog-Out4 at Max \nCompressor Speed",
                        viewModel.minMaxVoltage,
                        "V",
                        minDefault = viewModel.viewState.value.connectAnalogOut4MinMax.compressorMinVoltage.toString(),
                        maxDefault = viewModel.viewState.value.connectAnalogOut4MinMax.compressorMaxVoltage.toString(),
                        onMinSelected = {
                            viewModel.viewState.value.connectAnalogOut4MinMax.compressorMinVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        },
                        onMaxSelected = {
                            viewModel.viewState.value.connectAnalogOut4MinMax.compressorMaxVoltage = it.value.toInt()
                            setStateChanged(viewModel)
                        })
                }
            }
        }
    }


    @Composable
    fun ConnectCompositeBasedControl(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isConnectCompositeEnabled()) {

            if (viewModel.viewState.value.noOfAnalogOutDynamic > 0) {
                ComposeUtil.DashDivider()
            }
            viewModel.viewState.value.noOfAnalogOutDynamic++
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 17.dp)) {

                BoldStyledTextView(COMPOSITE_CONTROL, fontSize = 20)
                EnableConnectCompositeCoolingMinMax(viewModel)
                EnableConnectCompositeHeatingMinMax(viewModel)
            }
        }
    }
    @Composable
    fun ConnectCO2BasedDamperControl(viewModel: AdvancedHybridAhuViewModel) {
        if (viewModel.isConnectDampersEnabled()) {

            if (viewModel.viewState.value.noOfAnalogOutDynamic > 0) {
                ComposeUtil.DashDivider()
            }
            viewModel.viewState.value.noOfAnalogOutDynamic++

            val damperEnum = viewModel.getAllowedValues(DomainName.co2BasedDamperControlOn, viewModel.connectModel)
            val minMaxCo2List = viewModel.getListByDomainName(DomainName.co2Target, viewModel.connectModel)
            val co2Unit = viewModel.getUnit(DomainName.co2Target, viewModel.connectModel)
            val damperOperate = viewModel.getListByDomainName(DomainName.co2DamperOpeningRate, viewModel.connectModel)
            val percentUnit = viewModel.getUnit(DomainName.co2DamperOpeningRate, viewModel.connectModel)

            Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 17.dp)) {
                BoldStyledTextView(CO2_DAMPER_CONTROL, fontSize = 20)
                Row(modifier = Modifier.padding(top = 10.dp, bottom = 15.dp)) {
                    Box(modifier = Modifier
                        .weight(1f)
                        .padding(top = 10.dp)) {
                        StyledTextView(
                            CO2_DAMPER_CONTROL_ON, fontSize = 20, textAlignment = TextAlign.Left
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SearchSpinnerElement(
                            default = damperEnum[viewModel.viewState.value.connectDamperConfig.damperControlAssociation],
                            allItems = damperEnum,
                            unit = "",
                            onSelect = {
                                viewModel.viewState.value.connectDamperConfig.damperControlAssociation =
                                    it.index
                                setStateChanged(viewModel)
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
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.connectDamperConfig.co2Target = it.value.toDouble()
                        setStateChanged(viewModel)
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
                                setStateChanged(viewModel)
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
        Column(modifier = Modifier.padding(top = 10.dp, bottom = 10.dp, start = 15.dp)) {
            if (viewModel.isAnalogEnabledAndMapped(
                    ConnectControlType.CO2_BASED_DAMPER_CONTROL,
                    viewModel.viewState.value.connectAnalogOut1Enabled,
                    viewModel.viewState.value.connectAnalogOut1Association
                )
            ) {
                MinMaxConfiguration("Analog-Out1 at Min \nDCV Damper Pos",
                    "Analog-Out1 at Max \nDCV Damper Pos",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.connectAnalogOut1MinMax.damperPosMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.connectAnalogOut1MinMax.damperPosMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.connectAnalogOut1MinMax.damperPosMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.connectAnalogOut1MinMax.damperPosMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ConnectControlType.CO2_BASED_DAMPER_CONTROL,
                    viewModel.viewState.value.connectAnalogOut2Enabled,
                    viewModel.viewState.value.connectAnalogOut2Association
                )
            ) {
                MinMaxConfiguration("Analog-Out2 at Min \nDCV Damper Pos",
                    "Analog-Out2 at Max \nDCV Damper Pos",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.connectAnalogOut2MinMax.damperPosMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.connectAnalogOut2MinMax.damperPosMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.connectAnalogOut2MinMax.damperPosMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.connectAnalogOut2MinMax.damperPosMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ConnectControlType.CO2_BASED_DAMPER_CONTROL,
                    viewModel.viewState.value.connectAnalogOut3Enabled,
                    viewModel.viewState.value.connectAnalogOut3Association
                )
            ) {
                MinMaxConfiguration("Analog-Out3 at Min \nDCV Damper Pos",
                    "Analog-Out3 at Max \nDCV Damper Pos",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.connectAnalogOut3MinMax.damperPosMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.connectAnalogOut3MinMax.damperPosMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.connectAnalogOut3MinMax.damperPosMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.connectAnalogOut3MinMax.damperPosMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    })
            }
            if (viewModel.isAnalogEnabledAndMapped(
                    ConnectControlType.CO2_BASED_DAMPER_CONTROL,
                    viewModel.viewState.value.connectAnalogOut4Enabled,
                    viewModel.viewState.value.connectAnalogOut4Association
                )
            ) {
                MinMaxConfiguration("Analog-Out4 at Min \nDCV Damper Pos",
                    "Analog-Out4 at Max \nDCV Damper Pos",
                    viewModel.minMaxVoltage,
                    "V",
                    minDefault = viewModel.viewState.value.connectAnalogOut4MinMax.damperPosMinVoltage.toString(),
                    maxDefault = viewModel.viewState.value.connectAnalogOut4MinMax.damperPosMaxVoltage.toString(),
                    onMinSelected = {
                        viewModel.viewState.value.connectAnalogOut4MinMax.damperPosMinVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
                    },
                    onMaxSelected = {
                        viewModel.viewState.value.connectAnalogOut4MinMax.damperPosMaxVoltage =
                            it.value.toInt()
                        setStateChanged(viewModel)
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
            MinMaxConfiguration("Analog-Out1 at Min \nCooling",
                "Analog-Out1 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.connectAnalogOut1MinMax.compositeCoolingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.connectAnalogOut1MinMax.compositeCoolingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.connectAnalogOut1MinMax.compositeCoolingMinVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                },
                onMaxSelected = {
                    viewModel.viewState.value.connectAnalogOut1MinMax.compositeCoolingMaxVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ConnectControlType.COMPOSITE,
                viewModel.viewState.value.connectAnalogOut2Enabled,
                viewModel.viewState.value.connectAnalogOut2Association
            )
        ) {
            MinMaxConfiguration("Analog-Out2 at Min \nCooling",
                "Analog-Out2 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.connectAnalogOut2MinMax.compositeCoolingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.connectAnalogOut2MinMax.compositeCoolingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.connectAnalogOut2MinMax.compositeCoolingMinVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                },
                onMaxSelected = {
                    viewModel.viewState.value.connectAnalogOut2MinMax.compositeCoolingMaxVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ConnectControlType.COMPOSITE,
                viewModel.viewState.value.connectAnalogOut3Enabled,
                viewModel.viewState.value.connectAnalogOut3Association
            )
        ) {
            MinMaxConfiguration("Analog-Out3 at Min \nCooling",
                "Analog-Out3 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.connectAnalogOut3MinMax.compositeCoolingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.connectAnalogOut3MinMax.compositeCoolingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.connectAnalogOut3MinMax.compositeCoolingMinVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                },
                onMaxSelected = {
                    viewModel.viewState.value.connectAnalogOut3MinMax.compositeCoolingMaxVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ConnectControlType.COMPOSITE,
                viewModel.viewState.value.connectAnalogOut4Enabled,
                viewModel.viewState.value.connectAnalogOut4Association
            )
        ) {
            MinMaxConfiguration("Analog-Out4 at Min \nCooling",
                "Analog-Out4 at Max \nCooling",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.connectAnalogOut4MinMax.compositeCoolingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.connectAnalogOut4MinMax.compositeCoolingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.connectAnalogOut4MinMax.compositeCoolingMinVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                },
                onMaxSelected = {
                    viewModel.viewState.value.connectAnalogOut4MinMax.compositeCoolingMaxVoltage = it.value.toInt()
                    setStateChanged(viewModel)
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
            MinMaxConfiguration("Analog-Out1 at Min \nHeating",
                "Analog-Out1 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.connectAnalogOut1MinMax.compositeHeatingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.connectAnalogOut1MinMax.compositeHeatingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.connectAnalogOut1MinMax.compositeHeatingMinVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                },
                onMaxSelected = {
                    viewModel.viewState.value.connectAnalogOut1MinMax.compositeHeatingMaxVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ConnectControlType.COMPOSITE,
                viewModel.viewState.value.connectAnalogOut2Enabled,
                viewModel.viewState.value.connectAnalogOut2Association
            )
        ) {
            MinMaxConfiguration("Analog-Out2 at Min \nHeating",
                "Analog-Out2 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.connectAnalogOut2MinMax.compositeHeatingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.connectAnalogOut2MinMax.compositeHeatingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.connectAnalogOut2MinMax.compositeHeatingMinVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                },
                onMaxSelected = {
                    viewModel.viewState.value.connectAnalogOut2MinMax.compositeHeatingMaxVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ConnectControlType.COMPOSITE,
                viewModel.viewState.value.connectAnalogOut3Enabled,
                viewModel.viewState.value.connectAnalogOut3Association
            )
        ) {
            MinMaxConfiguration("Analog-Out3 at Min \nHeating",
                "Analog-Out3 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.connectAnalogOut3MinMax.compositeHeatingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.connectAnalogOut3MinMax.compositeHeatingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.connectAnalogOut3MinMax.compositeHeatingMinVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                },
                onMaxSelected = {
                    viewModel.viewState.value.connectAnalogOut3MinMax.compositeHeatingMaxVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                })
        }
        if (viewModel.isAnalogEnabledAndMapped(
                ConnectControlType.COMPOSITE,
                viewModel.viewState.value.connectAnalogOut4Enabled,
                viewModel.viewState.value.connectAnalogOut4Association
            )
        ) {
            MinMaxConfiguration("Analog-Out4 at Min \nHeating",
                "Analog-Out4 at Max \nHeating",
                viewModel.minMaxVoltage,
                "V",
                minDefault = viewModel.viewState.value.connectAnalogOut4MinMax.compositeHeatingMinVoltage.toString(),
                maxDefault = viewModel.viewState.value.connectAnalogOut4MinMax.compositeHeatingMaxVoltage.toString(),
                onMinSelected = {
                    viewModel.viewState.value.connectAnalogOut4MinMax.compositeHeatingMinVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                },
                onMaxSelected = {
                    viewModel.viewState.value.connectAnalogOut4MinMax.compositeHeatingMaxVoltage = it.value.toInt()
                    setStateChanged(viewModel)
                })
        }
    }
    fun setStateChanged(viewModel: AdvancedHybridAhuViewModel) {
        viewModel.viewState.value.isStateChanged = true
        viewModel.viewState.value.isSaveRequired = true
    }
    @Composable
     fun OaoConfigScreen(viewModel: AdvancedHybridAhuViewModel) {

        Spacer(modifier = Modifier.height(15.dp))

        //Oao damper
        if (viewModel.viewState.value.connectAnalogOut1Enabled && viewModel.viewState.value.connectAnalogOut1Association == ConnectControlType.OAO_DAMPER.ordinal) {
            AnalogOutOaoDamper(0)
        }
        if (viewModel.viewState.value.connectAnalogOut2Enabled && viewModel.viewState.value.connectAnalogOut2Association == ConnectControlType.OAO_DAMPER.ordinal) {
            AnalogOutOaoDamper(1)
        }
        if (viewModel.viewState.value.connectAnalogOut3Enabled && viewModel.viewState.value.connectAnalogOut3Association ==ConnectControlType.OAO_DAMPER.ordinal) {
            AnalogOutOaoDamper(2)
        }
        if (viewModel.viewState.value.connectAnalogOut4Enabled && viewModel.viewState.value.connectAnalogOut4Association == ConnectControlType.OAO_DAMPER.ordinal) {
            AnalogOutOaoDamper(3)
        }

        //return damper
        if (viewModel.viewState.value.connectAnalogOut1Enabled && viewModel.viewState.value.connectAnalogOut1Association == ConnectControlType.RETURN_DAMPER.ordinal) {
            AnalogOutReturnDamper(0)
        }
        if (viewModel.viewState.value.connectAnalogOut2Enabled && viewModel.viewState.value.connectAnalogOut2Association == ConnectControlType.RETURN_DAMPER.ordinal) {
            AnalogOutReturnDamper(1)
        }
        if (viewModel.viewState.value.connectAnalogOut3Enabled && viewModel.viewState.value.connectAnalogOut3Association == ConnectControlType.RETURN_DAMPER.ordinal) {
            AnalogOutReturnDamper(2)
        }
        if (viewModel.viewState.value.connectAnalogOut4Enabled && viewModel.viewState.value.connectAnalogOut4Association == ConnectControlType.RETURN_DAMPER.ordinal) {
            AnalogOutReturnDamper(3)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, start = 20.dp, end = 10.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            DropDownWithLabel(
                label = "Outside Damper Min Open during\n" +
                        "Recirculation (%)",
                list = viewModel.outsideDamperMinOpenDuringRecirculationList,
                previewWidth = 60,
                expandedWidth = 60,
                isHeader = false,
                labelWidth = 350,
                onSelected = { selectedIndex ->
                    viewModel.viewState.value.outsideDamperMinOpenDuringRecirculationPos =
                        selectedIndex.toDouble()
                    setStateChanged(viewModel)
                },
                defaultSelection = viewModel.outsideDamperMinOpenDuringRecirculationList
                    .indexOf(
                        viewModel.viewState.value.outsideDamperMinOpenDuringRecirculationPos.toInt()
                            .toString()
                    ),
                spacerLimit = 55,
                heightValue = 270
            )
            Spacer(modifier = Modifier.width(64.dp))
            DropDownWithLabel(
                label = "Outside Damper Min Open during\n" +
                        "Conditioning (%)",
                list = viewModel.outsideDamperMinOpenDuringConditioningList,
                previewWidth = 60,
                expandedWidth = 60,
                isHeader = false,
                labelWidth = 350,
                onSelected = { selectedIndex ->
                    viewModel.viewState.value.outsideDamperMinOpenDuringConditioningPos =
                        selectedIndex.toDouble()
                    setStateChanged(viewModel)
                },
                defaultSelection = viewModel.outsideDamperMinOpenDuringConditioningList
                    .indexOf(
                        viewModel.viewState.value.outsideDamperMinOpenDuringConditioningPos.toInt()
                            .toString()
                    ),
                spacerLimit = 55,
                heightValue = 270
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 10.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                DropDownWithLabel(
                    label = "Outside Damper Min Open during\n" +
                            "Fan Low (%)",
                    list = viewModel.outsideDamperMinOpenDuringFanLowList,
                    previewWidth = 60,
                    expandedWidth = 60,
                    isHeader = false,
                    labelWidth = 350,
                    onSelected = { selectedIndex ->
                        viewModel.viewState.value.outsideDamperMinOpenDuringFanLowPos =
                            selectedIndex.toDouble()
                        setStateChanged(viewModel)
                    },
                    defaultSelection = viewModel.outsideDamperMinOpenDuringFanLowList
                        .indexOf(
                            viewModel.viewState.value.outsideDamperMinOpenDuringFanLowPos.toInt()
                                .toString()
                        ),
                    spacerLimit = 55,
                    heightValue = 270
                )
                Spacer(modifier = Modifier.width(64.dp))
                DropDownWithLabel(
                    label = "Outside Damper Min Open during\n" +
                            "Fan Medium (%)",
                    list = viewModel.outsideDamperMinOpenDuringFanMediumList,
                    previewWidth = 60,
                    expandedWidth = 60,
                    isHeader = false,
                    labelWidth = 350,
                    onSelected = { selectedIndex ->
                        viewModel.viewState.value.outsideDamperMinOpenDuringFanMediumPos =
                            selectedIndex.toDouble()
                        setStateChanged(viewModel)
                    },
                    defaultSelection = viewModel.outsideDamperMinOpenDuringFanMediumList
                        .indexOf(
                            viewModel.viewState.value.outsideDamperMinOpenDuringFanMediumPos.toInt()
                                .toString()
                        ),
                    spacerLimit = 55,
                    heightValue = 270
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 10.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                DropDownWithLabel(
                    label = "Outside Damper Min Open during\n" +
                            "Fan High (%)",
                    list = viewModel.outsideDamperMinOpenDuringFanHighList,
                    previewWidth = 60,
                    expandedWidth = 60,
                    isHeader = false,
                    labelWidth = 350,
                    onSelected = { selectedIndex ->
                        viewModel.viewState.value.outsideDamperMinOpenDuringFanHighPos =
                            selectedIndex.toDouble()
                        setStateChanged(viewModel)
                    },
                    defaultSelection = viewModel.outsideDamperMinOpenDuringFanHighList
                        .indexOf(
                            viewModel.viewState.value.outsideDamperMinOpenDuringFanHighPos.toInt()
                                .toString()
                        ),
                    spacerLimit = 55,
                    heightValue = 270
                )
                Spacer(modifier = Modifier.width(64.dp))
                DropDownWithLabel(
                    label = "Return Damper Min Open (%)",
                    list = viewModel.returnDamperMinOpenPosList,
                    previewWidth = 60,
                    expandedWidth = 60,
                    isHeader = false,
                    labelWidth = 350,
                    onSelected = { selectedIndex ->
                        viewModel.viewState.value.returnDamperMinOpenPos =
                            selectedIndex.toDouble()
                        setStateChanged(viewModel)
                    },
                    defaultSelection = viewModel.returnDamperMinOpenPosList
                        .indexOf(
                            viewModel.viewState.value.returnDamperMinOpenPos.toInt().toString()
                        ),
                    spacerLimit = 55,
                    heightValue = 270
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 10.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                DropDownWithLabel(
                    label = "Exhaust Fan Stage 1 Threshold (%)",
                    list = viewModel.exhaustFanStage1ThresholdList,
                    previewWidth = 60,
                    expandedWidth = 60,
                    isHeader = false,
                    labelWidth = 350,
                    onSelected = { selectedIndex ->
                        viewModel.viewState.value.exhaustFanStage1ThresholdPos =
                            selectedIndex.toDouble()
                        setStateChanged(viewModel)
                    },
                    defaultSelection = viewModel.exhaustFanStage1ThresholdList
                        .indexOf(
                            viewModel.viewState.value.exhaustFanStage1ThresholdPos.toInt()
                                .toString()
                        ),
                    spacerLimit = 55,
                    heightValue = 270
                )
                Spacer(modifier = Modifier.width(63.dp))
                DropDownWithLabel(
                    label = "Exhaust Fan Stage 2 Threshold (%)",
                    list = viewModel.exhaustFanStage2ThresholdList,
                    previewWidth = 60,
                    expandedWidth = 60,
                    isHeader = false,
                    labelWidth = 350,
                    onSelected = { selectedIndex ->
                        viewModel.viewState.value.exhaustFanStage2ThresholdPos =
                            selectedIndex.toDouble()
                        setStateChanged(viewModel)
                    },
                    defaultSelection = viewModel.exhaustFanStage2ThresholdList
                        .indexOf(
                            viewModel.viewState.value.exhaustFanStage2ThresholdPos.toInt()
                                .toString()
                        ),
                    spacerLimit = 55,
                    heightValue = 270
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 10.dp),
                horizontalArrangement = Arrangement.Start
            ) {
               DropDownWithLabel(
                    label = "Current Transformer Type",
                    list = viewModel.currentTransformerTypeList,
                    previewWidth = 120,
                    expandedWidth = 120,
                   isHeader = false,
                   labelWidth = 275,
                    onSelected = { selectedIndex ->
                        viewModel.viewState.value.currentTransformerTypePos =
                            selectedIndex.toDouble()
                        setStateChanged(viewModel)
                    },
                    defaultSelection = viewModel.viewState.value.currentTransformerTypePos.toInt(),
                    spacerLimit = 69,
                    heightValue = 270
                )
                Spacer(modifier = Modifier.width(64.dp))
                DropDownWithLabel(
                    label = "CO2 Threshold (ppm)",
                    list = viewModel.co2ThresholdList,
                    previewWidth = 80,
                    expandedWidth = 80,
                    isHeader = false,
                    labelWidth = 330,

                    onSelected = { selectedIndex ->
                        viewModel.viewState.value.oaoCo2ThresholdVal =
                            viewModel.co2ThresholdList[selectedIndex].toDouble()
                        setStateChanged(viewModel)
                    },
                    defaultSelection = viewModel.co2ThresholdList
                        .indexOf(viewModel.viewState.value.oaoCo2ThresholdVal.toInt().toString()),
                    spacerLimit = 55,
                    heightValue = 270
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 10.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                DropDownWithLabel(
                    label = "Exhaust Fan Hysteresis (%)",
                    list = viewModel.exhaustFanHysteresisList,
                    previewWidth = 60,
                    expandedWidth = 60,
                    isHeader = false,
                    labelWidth = 320,
                    onSelected =
                    { selectedIndex ->
                        viewModel.viewState.value.exhaustFanHysteresisPos =
                            selectedIndex.toDouble()
                        setStateChanged(viewModel)
                    },
                    defaultSelection = viewModel.exhaustFanHysteresisList.indexOf(
                        viewModel.viewState.value.exhaustFanHysteresisPos.toInt().toString()
                    ),
                    spacerLimit = 85,
                    heightValue = 270
                )
                Spacer(modifier = Modifier.width(75.dp))
                HeaderTextView(text = "Use Per Room CO2 Sensing", fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(144.dp))
                ToggleButtonStateful(

                    defaultSelection = viewModel.viewState.value.usePerRoomCO2SensingState,
                    onEnabled = { viewModel.viewState.value.usePerRoomCO2SensingState = it
                        setStateChanged(viewModel)}
                )
            }

            Spacer(modifier = Modifier.height(24.dp))


            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 10.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                DropDownWithLabel(
                    label = "Smart Purge Outside Damper Min \nOpen",
                    list = viewModel.systemPurgeOutsideDamperMinPosList,
                    previewWidth = 60,
                    isHeader = false,
                    labelWidth = 350,
                    expandedWidth = 60,
                    onSelected = { selectedIndex ->
                        viewModel.viewState.value.systemPurgeOutsideDamperMinPos =
                            selectedIndex.toDouble()
                        setStateChanged(viewModel)
                    },
                    defaultSelection = viewModel.systemPurgeOutsideDamperMinPosList
                        .indexOf(
                            viewModel.viewState.value.systemPurgeOutsideDamperMinPos.toInt()
                                .toString()
                        ),
                    spacerLimit = 48,
                    heightValue = 270
                )

                Spacer(modifier = Modifier.width(67.dp))
                DropDownWithLabel(
                    label = "Enhanced Ventilation Outside \nDamper Min Open",
                    list = viewModel.enhancedVentilationOutsideDamperMinOpenList,
                    previewWidth = 60,
                    isHeader = false,
                    labelWidth = 350,
                    expandedWidth = 60,
                    onSelected = { selectedIndex ->
                        viewModel.viewState.value.enhancedVentilationOutsideDamperMinOpenPos =
                            selectedIndex.toDouble()
                        setStateChanged(viewModel)
                    },
                    defaultSelection = viewModel.enhancedVentilationOutsideDamperMinOpenList
                        .indexOf(
                            viewModel.viewState.value.enhancedVentilationOutsideDamperMinOpenPos.toInt()
                                .toString()
                        ),
                    spacerLimit = 58,
                    heightValue = 270
                )
            }

        }
    @Composable
   fun AnalogOutOaoDamper(index:Int){
        val analogOutMin = "Analog-Out${index + 1} at Min OAO Damper Pos(V)"
        val analogOutMax = "Analog-Out${index+ 1} at Max OAO Damper Pos(V)"
        val minOaoDamperList: List<String> = when (index) {
            0 -> viewModel.analog1MinOaoDamperList
            1 -> viewModel.analog2MinOaoDamperList
            2 -> viewModel.analog3MinOaoDamperList
            3 -> viewModel.analog4MinOaoDamperList
            else -> {
                emptyList()
            }
        }
        val maxOaoDamperList: List<String> = when (index) {
            0 -> viewModel.analog1MaxOaoDamperList
            1 -> viewModel.analog2MaxOaoDamperList
            2 -> viewModel.analog3MaxOaoDamperList
            3 -> viewModel.analog4MaxOaoDamperList
            else -> {
                emptyList()
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 10.dp),
            horizontalArrangement = Arrangement.Start
        ) {

            DropDownWithLabel(
                label = analogOutMin,
                list = minOaoDamperList,
                previewWidth = 60,
                expandedWidth = 60,
                isHeader = false,
                labelWidth = 340,
                onSelected = { selectedIndex ->
                    when (index) {
                        0 -> viewModel.viewState.value.analog1MinOaoDamper =
                            selectedIndex.toDouble()

                        1 -> viewModel.viewState.value.analog2MinOaoDamper =
                            selectedIndex.toDouble()

                        2 -> viewModel.viewState.value.analog3MinOaoDamper =
                            selectedIndex.toDouble()

                        3 -> viewModel.viewState.value.analog4MinOaoDamper =
                            selectedIndex.toDouble()

                    }
                    setStateChanged(viewModel)
                },
                defaultSelection = when (index) {
                    0 -> viewModel.analog1MinOaoDamperList.indexOf(
                        viewModel.viewState.value.analog1MinOaoDamper.toInt().toString()
                    )

                    1 -> viewModel.analog2MinOaoDamperList.indexOf(
                        viewModel.viewState.value.analog2MinOaoDamper.toInt().toString()
                    )

                    2 -> viewModel.analog3MinOaoDamperList.indexOf(
                        viewModel.viewState.value
                            .analog3MinOaoDamper.toInt().toString()
                    )
                    3-> viewModel.analog4MinOaoDamperList.indexOf(
                        viewModel.viewState.value.analog4MinOaoDamper.toInt().toString()
                    )
                    else->{1}
                },
                spacerLimit = 64,
                heightValue = 270
            )
            Spacer(modifier = Modifier.width(64.dp))
            DropDownWithLabel(
                label = analogOutMax,
                list = maxOaoDamperList,
                previewWidth = 60,
                expandedWidth = 60,
                isHeader = false,
                labelWidth = 340,
                onSelected = { selectedIndex ->
                    when (index) {
                        0 -> viewModel.viewState.value.analog1MaxOaoDamper =
                            selectedIndex.toDouble()

                        1 -> viewModel.viewState.value.analog2MaxOaoDamper =
                            selectedIndex.toDouble()

                        2 -> viewModel.viewState.value.analog3MaxOaoDamper =
                            selectedIndex.toDouble()

                        3 -> viewModel.viewState.value.analog4MaxOaoDamper =
                            selectedIndex.toDouble()
                    }
                    setStateChanged(viewModel)
                },

                defaultSelection = when (index) {
                    0 -> viewModel.analog1MaxOaoDamperList.indexOf(
                        viewModel.viewState.value.analog1MaxOaoDamper.toInt().toString()
                    )

                    1 -> viewModel.analog2MaxOaoDamperList.indexOf(
                        viewModel.viewState.value.analog2MaxOaoDamper.toInt().toString()
                    )

                    2 -> viewModel.analog3MaxOaoDamperList.indexOf(
                        viewModel.viewState.value.analog3MaxOaoDamper.toInt().toString()
                    )


                    3-> viewModel.analog4MaxOaoDamperList.indexOf(
                        viewModel.viewState.value.analog4MaxOaoDamper.toInt().toString()
                    )
                    else->{1}
                },
                spacerLimit = 64,
                heightValue = 270
            )

        }
        Spacer(modifier = Modifier.height(24.dp))

    }

    @Composable
    fun AnalogOutReturnDamper(index: Int){
            val analogOutMin = "Analog-Out${index + 1} at Min Return Damper Pos(V)"
            val analogOutMax = "Analog-Out${index + 1} at Max Return Damper Pos(V)"
            val minReturnDamperList: List<String> = when (index) {
                0 -> viewModel.analog1MinReturnDamperList
                1 -> viewModel.analog2MinReturnDamperList
                2 -> viewModel.analog3MinReturnDamperList
                3 -> viewModel.analog4MinReturnDamperList
                else -> {
                    emptyList()
                }
            }
            val maxReturnDamperList: List<String> = when (index) {
                0 -> viewModel.analog1MaxReturnDamperList
                1 -> viewModel.analog2MaxReturnDamperList
                2 -> viewModel.analog3MaxReturnDamperList
                3 -> viewModel.analog4MaxReturnDamperList
                else -> {
                    emptyList()
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 10.dp),
                horizontalArrangement = Arrangement.Start
            ) {
                DropDownWithLabel(
                    label = analogOutMin,
                    list = minReturnDamperList,
                    previewWidth = 60,
                    expandedWidth = 60,
                    isHeader = false,
                    labelWidth = 330,
                    onSelected = { selectedIndex ->
                        when (index) {
                            0 -> viewModel.viewState.value.analog1MinReturnDamper =
                                selectedIndex.toDouble()

                            1 -> viewModel.viewState.value.analog2MinReturnDamper =
                                selectedIndex.toDouble()

                            2 -> viewModel.viewState.value.analog3MinReturnDamper =
                                selectedIndex.toDouble()

                            3 -> viewModel.viewState.value.analog4MinReturnDamper =
                                selectedIndex.toDouble()
                        }
                        setStateChanged(viewModel)
                    },
                    defaultSelection = when (index) {
                        0 -> viewModel.analog1MinReturnDamperList.indexOf(
                            viewModel.viewState.value.analog1MinReturnDamper.toInt().toString()
                        )

                        1 -> viewModel.analog2MinReturnDamperList.indexOf(
                            viewModel.viewState.value.analog2MinReturnDamper.toInt().toString()
                        )

                        2 -> viewModel.analog3MinReturnDamperList.indexOf(
                            viewModel.viewState.value.analog3MinReturnDamper.toInt().toString()
                        )


                        3-> viewModel.analog4MinReturnDamperList.indexOf(
                            viewModel.viewState.value.analog4MinReturnDamper.toInt().toString()
                        )
                        else->{1}
                    },
                    spacerLimit = 76,
                    heightValue = 270
                )
                Spacer(modifier = Modifier.width(65.dp))
                DropDownWithLabel(
                    label = analogOutMax,
                    list = maxReturnDamperList,
                    previewWidth = 60,
                    isHeader = false,
                    labelWidth = 330,
                    expandedWidth = 60,
                    onSelected =  { selectedIndex ->
                        when (index) {
                            0 -> viewModel.viewState.value.analog1MaxReturnDamper =
                                selectedIndex.toDouble()

                            1 -> viewModel.viewState.value.analog2MaxReturnDamper =
                                selectedIndex.toDouble()

                            2 -> viewModel.viewState.value.analog3MaxReturnDamper =
                                selectedIndex.toDouble()

                            3 -> viewModel.viewState.value.analog4MaxReturnDamper =
                                selectedIndex.toDouble()
                        }
                        setStateChanged(viewModel)
                    },
                    defaultSelection = when (index) {
                        0 -> viewModel.analog1MaxReturnDamperList.indexOf(
                            viewModel.viewState.value.analog1MaxReturnDamper.toInt().toString()
                        )

                        1 -> viewModel.analog2MaxReturnDamperList.indexOf(
                            viewModel.viewState.value.analog2MaxReturnDamper.toInt().toString()
                        )

                        2 -> viewModel.analog3MaxReturnDamperList.indexOf(
                            viewModel.viewState.value.analog3MaxReturnDamper.toInt().toString()
                        )

                        else -> viewModel.analog4MaxReturnDamperList.indexOf(
                            viewModel.viewState.value.analog4MaxReturnDamper.toInt().toString()
                        )

                    },
                    spacerLimit = 74,
                    heightValue = 270
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
    }
}
