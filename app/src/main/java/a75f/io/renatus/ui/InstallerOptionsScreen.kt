package a75f.io.renatus.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.updateBackfillDuration
import a75f.io.logic.bo.util.DemandResponseMode
import a75f.io.logic.bo.util.UnitUtils
import a75f.io.logic.tuners.TunerConstants
import a75f.io.logic.tuners.TunerUtil
import a75f.io.renatus.R
import a75f.io.renatus.UtilityApplication.context
import a75f.io.renatus.composables.DropDownWithoutLabel
import a75f.io.renatus.composables.HeaderView
import a75f.io.renatus.compose.ToggleButton
import a75f.io.renatus.modbus.util.formattedToastMessage
import a75f.io.renatus.ui.zonescreen.model.HeaderViewItem
import a75f.io.renatus.util.BackFillViewModel
import a75f.io.renatus.util.TemperatureModeUtil
import android.util.TypedValue
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.min


fun ComposeView.showTopViews(
    viewModel: InstallerOptionsViewModel,
    onValueChange: (selectedIndex: Int, point: HeaderViewItem) -> Unit
) {
    setContent {
        ShowTopOptions(viewModel, onValueChange)
    }
}

@Composable
fun ShowTopOptions(
    systemViewModel: InstallerOptionsViewModel,
    onValueChange: (selectedIndex: Int, point: HeaderViewItem) -> Unit
) {
    Column(
        modifier = Modifier
            .wrapContentSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        InstallerOptionsTopView(systemViewModel, onValueChange)
    }
}

@Composable
fun InstallerOptionsTopView(
    installerOptionsViewModel: InstallerOptionsViewModel,
    onValueChange: (selectedIndex: Int, point: HeaderViewItem) -> Unit
) {
    Column(
        modifier = Modifier
            .wrapContentSize()
            .padding(top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        if (L.ccu().systemProfile != null &&
            L.ccu().systemProfile.profileType != ProfileType.SYSTEM_DEFAULT
        ) {
            ShowTempLockoutOptions(installerOptionsViewModel)
        }
        OfflineModeView(installerOptionsViewModel)
        BackfillTime(installerOptionsViewModel)
        DRModeView(installerOptionsViewModel)

    }
    Spacer(modifier = Modifier.height(12.dp))

}


fun ComposeView.showBottomViews(
    viewModel: InstallerOptionsViewModel,
    onValueChange: (selectedIndex: Int, point: HeaderViewItem) -> Unit
) {
    setContent {
        ShowBottomOptions(viewModel, onValueChange)
    }
}


@Composable
fun ShowBottomOptions(
    systemViewModel: InstallerOptionsViewModel,
    onValueChange: (selectedIndex: Int, point: HeaderViewItem) -> Unit
) {
    Column(
        modifier = Modifier
            .wrapContentSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        InstallerOptionsBottomView(systemViewModel, onValueChange)
    }
}

@Composable
fun InstallerOptionsBottomView(
    installerOptionsViewModel: InstallerOptionsViewModel,
    onValueChange: (selectedIndex: Int, point: HeaderViewItem) -> Unit
) {
    Column(
        modifier = Modifier
            .wrapContentSize()
            .padding(top = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        UseCelsiusView(installerOptionsViewModel)
        TemperatureModeView(installerOptionsViewModel)
    }
    Spacer(modifier = Modifier.height(12.dp))

}


@Composable
fun UseCelsiusView(installerOptionsViewModel: InstallerOptionsViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            HeaderView(
                label = stringResource(R.string.usecelsius),
                paddingTop = 8.dp
            ) { }
        }
        val useCelsius = installerOptionsViewModel.useCelsius.value.state
        ToggleButton(useCelsius, modifier = Modifier.padding(end = 0.dp)) { newValue ->
            installerOptionsViewModel.useCelsius.value =
                installerOptionsViewModel.useCelsius.value.copy(state = newValue)
            CoroutineScope(Dispatchers.IO).launch {
                updateUiWithCelsius(installerOptionsViewModel.useCelsius.value.state)
            }
        }
    }
}

fun updateUiWithCelsius(isChecked: Boolean) {
    val useCelsiusEntity = CCUHsApi.getInstance().readEntity("displayUnit")
    if(useCelsiusEntity.isEmpty()) {
        CcuLog.d("InstallerOptionsViewModel", "celsius not found")
        return
    }
    val id = useCelsiusEntity["id"].toString()

    if (isChecked) {
        CCUHsApi.getInstance().writePoint(id, TunerConstants.TUNER_BUILDING_VAL_LEVEL,
            CCUHsApi.getInstance().getCCUUserName(), 1.0, 0);
    } else {
        CCUHsApi.getInstance().writePoint(id, TunerConstants.TUNER_BUILDING_VAL_LEVEL,
            CCUHsApi.getInstance().getCCUUserName(), 0.0, 0);
    }
}

@Composable
fun BackfillTime(installerOptionsViewModel: InstallerOptionsViewModel) {

    var showActions by remember { mutableStateOf(false) }
    var tempIndex by remember { mutableIntStateOf(0) }

    Column {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column {
                HeaderView(
                    label = stringResource(R.string.backFill_duration),
                    paddingTop = 8.dp
                ) {}

                HeaderView(
                    label = stringResource(R.string.backFill_duration_description),
                    paddingTop = 8.dp,
                    fontSize = 19.5
                ) {}
            }

            DropDownWithoutLabel(
                list = installerOptionsViewModel.backFillTime.value.dropdownOptions,
                maxLengthString = "1",
                maxContainerWidth = 452.dp,
                onSelected = { newIndex ->
                    tempIndex = newIndex
                    showActions = true      // SHOW APPLY | CANCEL
                },
                defaultSelection = installerOptionsViewModel.backFillTime.value.selectedIndex,
                extraWidth = 60
            )
        }

        if (showActions) {
            ApplyCancelRow(
                onCancel = { showActions = false },
                onApply = {
                    val durations = BackFillViewModel.BackFillDuration.toIntArray()
                    val index = if (tempIndex > 0) min(
                        tempIndex,
                        durations.size - 1
                    ) else 0
                    val backFillDurationSelected = durations[index]
                    updateBackfillDuration(backFillDurationSelected.toDouble())
                    formattedToastMessage(context.getString(R.string.backfill_time_saved_successfully), context)
                    showActions = false
                }
            )
        }
    }
}

@Composable
fun TemperatureModeView(installerOptionsViewModel: InstallerOptionsViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            HeaderView(
                label = stringResource(R.string.temperature_mode_description),
                paddingTop = 8.dp
            ) { }
        }

        DropDownWithoutLabel(
            list = installerOptionsViewModel.temperatureMode.value.dropdownOptions,
            maxLengthString = "1",
            maxContainerWidth = 452.dp,
            onSelected = { position ->
                TemperatureModeUtil().setTemperatureMode(position);
            },
            defaultSelection = installerOptionsViewModel.temperatureMode.value.selectedIndex,
            extraWidth = 260
        )
    }
}

fun getBackFillOptions(): List<String> {
    val strings = BackFillViewModel.BackFillDuration.getDisplayNames()
    return  ArrayList(listOf(*strings))
}

@Composable
fun OfflineModeView(installerOptionsViewModel: InstallerOptionsViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            HeaderView(
                label = "Offline Mode",
                paddingTop = 8.dp
            ) { }

            HeaderView(
                label = stringResource(R.string.offline_desc),
                paddingTop = 8.dp,
                fontSize = 19.5
            ) { }
        }
        val offlineMode = installerOptionsViewModel.offlineMode.value.state
        ToggleButton(offlineMode, modifier = Modifier.padding(end = 0.dp)) { newValue ->
            installerOptionsViewModel.offlineMode.value =
                installerOptionsViewModel.offlineMode.value.copy(state = newValue)
                CoroutineScope(Dispatchers.IO).launch{
                    val offlineValue = if (newValue) 1.0 else 0.0
                    CCUHsApi.getInstance().writeDefaultVal("offline and mode", offlineValue)
                    CCUHsApi.getInstance().writeHisValByQuery("offline and mode ", offlineValue)
                    TunerUtil.updateDefault(offlineValue)
                }
        }
    }
}


@Composable
fun DRModeView(installerOptionsViewModel: InstallerOptionsViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            HeaderView(
                label = stringResource(R.string.dr_enrollment),
                paddingTop = 8.dp
            ) { }

            HeaderView(
                label = stringResource(R.string.dr_enrollment_description),
                paddingTop = 8.dp,
                fontSize = 19.5
            ) { }
        }
        val drEnrollment = installerOptionsViewModel.drEnrollment.value.state
        ToggleButton(drEnrollment, modifier = Modifier.padding(end = 0.dp)) { newValue ->
            installerOptionsViewModel.drEnrollment.value =
                installerOptionsViewModel.drEnrollment.value.copy(state = newValue)
            DemandResponseMode.handleDRActivationConfiguration(newValue,
                CCUHsApi.getInstance())
        }
    }
}

@Composable
fun ShowTempLockoutOptions(installerOptionsViewModel: InstallerOptionsViewModel) {

    val isEnabledCooling = installerOptionsViewModel.coolingLockoutEnable.value.state

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column {
            HeaderView(
                label = stringResource(R.string.use_outside_temp_lockout_cooling),
                paddingTop = 8.dp
            ) { }

            HeaderView(
                label = stringResource(R.string.cooling_lockout_desc),
                paddingTop = 8.dp,
                fontSize = 19.5
            ) { }
        }
        Column {
            ToggleButton(isEnabledCooling, modifier = Modifier.padding(end = 0.dp)) { newValue ->
                installerOptionsViewModel.coolingLockoutEnable.value =
                    installerOptionsViewModel.coolingLockoutEnable.value.copy(state = newValue)
                CoroutineScope(Dispatchers.IO).launch {
                    installerOptionsViewModel.setOutsideTempCoolingLockoutEnabled(
                        CCUHsApi.getInstance(),
                        newValue
                    )
                }

            }
        }
    }

    if (isEnabledCooling) {
        ShowDropDown(installerOptionsViewModel,true)
    }

    val isEnabledHeating = installerOptionsViewModel.heatingLockoutEnable.value.state
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Column {
            HeaderView(
                label = stringResource(R.string.use_outside_temp_lockout_heating),
                paddingTop = 8.dp
            ) { }

            HeaderView(
                label = stringResource(R.string.heating_lockout_desc),
                paddingTop = 8.dp,
                fontSize = 19.5
            ) { }
        }

        Column {
            ToggleButton(isEnabledHeating, modifier = Modifier.padding(end = 0.dp)) { newValue ->
                installerOptionsViewModel.heatingLockoutEnable.value =
                    installerOptionsViewModel.heatingLockoutEnable.value.copy(state = newValue)
                CoroutineScope(Dispatchers.IO).launch{
                    installerOptionsViewModel.setOutsideTempHeatingLockoutEnabled(
                        CCUHsApi.getInstance(),
                        newValue
                    )
                }
            }
        }
    }
    if (isEnabledHeating) {
        ShowDropDown(installerOptionsViewModel,false)
    }
}

@Composable
fun ShowDropDown(
    installerOptionsViewModel: InstallerOptionsViewModel,
    isCoolingDropdown: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCoolingDropdown) {
            HeaderView(
                label = stringResource(R.string.no_mechanical_cooling_below)
            ) {
                //
            }
            DropDownWithoutLabel(
                list = installerOptionsViewModel.coolingLockoutDropdown.value.dropdownOptions,
                maxLengthString = "1",
                maxContainerWidth = 452.dp,
                onSelected = { position ->
                    if (UnitUtils.isCelsiusTunerAvailableStatus()) {
                        L.ccu().systemProfile.setCoolingLockoutVal(
                            CCUHsApi.getInstance(),
                            Math.round(
                                UnitUtils.celsiusToFahrenheit(
                                    installerOptionsViewModel.coolingLockoutDropdown.value.dropdownOptions[position].toDouble()
                                )
                            ).toDouble()
                        )
                    } else {
                        L.ccu().systemProfile.setCoolingLockoutVal(
                            CCUHsApi.getInstance(),
                            installerOptionsViewModel.coolingLockoutDropdown.value.dropdownOptions[position].toDouble()
                        )
                    }
                },
                defaultSelection = installerOptionsViewModel.coolingLockoutDropdown.value.selectedIndex,
                extraWidth = 30
            )
        } else {
            HeaderView(
                label = stringResource(R.string.no_mechanical_heating_above)
            ) {
                //
            }
            DropDownWithoutLabel(
                list = installerOptionsViewModel.heatingLockoutDropdown.value.dropdownOptions,
                maxLengthString = "1",
                maxContainerWidth = 452.dp,
                onSelected = { position ->
                    if (UnitUtils.isCelsiusTunerAvailableStatus()) {
                        L.ccu().systemProfile.setHeatingLockoutVal(
                            CCUHsApi.getInstance(),
                            Math.round(
                                UnitUtils.celsiusToFahrenheit(
                                    installerOptionsViewModel.heatingLockoutDropdown.value.dropdownOptions[position].toDouble()
                                )
                            ).toDouble()
                        )
                    } else {
                        L.ccu().systemProfile.setHeatingLockoutVal(
                            CCUHsApi.getInstance(),
                            installerOptionsViewModel.heatingLockoutDropdown.value.dropdownOptions[position].toDouble()
                        )
                    }
                },
                defaultSelection = installerOptionsViewModel.heatingLockoutDropdown.value.selectedIndex,
                extraWidth = 30
            )
        }
    }
}

@Composable
fun ApplyCancelRow(
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    val context = LocalContext.current
    val typedValue = remember { TypedValue() }
    context.theme.resolveAttribute(R.attr.orange_75f, typedValue, true)
    val orange = Color(typedValue.data)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = "CANCEL",
            color = orange,
            fontSize = 19.5.sp,
            modifier = Modifier
                .padding(end = 12.dp)
                .clickable { onCancel() }
        )
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(20.dp)
                .background(orange)
        )

        Text(
            text = "APPLY",
            color = orange,
            fontSize = 19.5.sp,
            modifier = Modifier
                .padding(start = 16.dp)
                .clickable { onApply() }
        )
    }
}
