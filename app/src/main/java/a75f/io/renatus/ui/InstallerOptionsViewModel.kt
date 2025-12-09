package a75f.io.renatus.ui

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.observer.PointSubscriber
import a75f.io.api.haystack.observer.PointWriteObservable
import a75f.io.domain.api.Domain
import a75f.io.domain.api.Domain.getDomainCCUEquip
import a75f.io.domain.api.DomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.L.ccu
import a75f.io.logic.bo.building.definitions.ProfileType
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu
import a75f.io.logic.bo.util.DemandResponseMode
import a75f.io.logic.bo.util.UnitUtils.fahrenheitToCelsius
import a75f.io.logic.bo.util.UnitUtils.isCelsiusTunerAvailableStatus
import a75f.io.logic.tuners.TunerConstants
import a75f.io.logic.tuners.TunerUtil.getTuner
import a75f.io.renatus.profiles.system.advancedahu.dab.DabAdvancedHybridAhuFragment
import a75f.io.renatus.profiles.system.advancedahu.vav.VavAdvancedHybridAhuFragment
import a75f.io.renatus.tuners.TunerFragment
import a75f.io.renatus.ui.model.DetailedViewItem
import a75f.io.renatus.ui.model.ToggleViewItem
import a75f.io.renatus.util.BackFillViewModel
import a75f.io.renatus.util.TemperatureModeUtil
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class InstallerOptionsViewModel: ViewModel() , PointSubscriber {

    var coolingLockoutEnable = mutableStateOf(ToggleViewItem())
    var coolingLockoutDropdown = mutableStateOf(DetailedViewItem())
    var heatingLockoutEnable = mutableStateOf(ToggleViewItem())
    var heatingLockoutDropdown = mutableStateOf(DetailedViewItem())
    var offlineMode = mutableStateOf(ToggleViewItem())
    var drEnrollment = mutableStateOf(ToggleViewItem())
    var useCelsius = mutableStateOf(ToggleViewItem())
    var backFillTime = mutableStateOf(DetailedViewItem())
    var temperatureMode = mutableStateOf(DetailedViewItem())

    fun loadViews(
        installerOptionsTopView: ComposeView,
        installerOptionsBottomView: ComposeView
    ) {
        loadTempLockoutCooling(true)
        loadTempLockoutHeating(true)
        loadOfflineMode(true)
        loadBackFillTime(true)
        loadDRMode(true)
        loadCelsiusToggle(true)
        loadTemperatureMode(true)
        showHeaderView(
            installerOptionsTopView,
            installerOptionsBottomView,
            this
        )
    }

    private fun loadCelsiusToggle(subScribe: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            val useCelsiusEntity = CCUHsApi.getInstance().readEntity("displayUnit")
            if(useCelsiusEntity.isEmpty()) {
                CcuLog.d("InstallerOptionsViewModel", "celsius not found")
                return@launch
            }

            val id = useCelsiusEntity["id"].toString()
            val state  = isCelsiusTunerAvailableStatus()
            useCelsius.value = ToggleViewItem(
                id = id,
                state = state
            )
            intimateAdvanceAhu()
            withContext(context = coroutineContext){
                if (TunerFragment.newInstance().tunerExpandableLayoutHelper != null) {
                    TunerFragment.newInstance().tunerExpandableLayoutHelper.notifyDataSetChanged()
                }
                if (subScribe) {
                    PointWriteObservable.subscribe(id, this@InstallerOptionsViewModel)
                }
            }
        }

    }


    private fun loadDRMode(subScribe: Boolean = false) {
        val state = DemandResponseMode.isDREnrollmentSelected()

        val ccuEquip = getDomainCCUEquip()
        ccuEquip?.demandResponseEnrollment?.pointExists()
        val id = ccuEquip?.demandResponseEnrollment?.id

        drEnrollment.value = ToggleViewItem(
            id = id,
            state = state
        )

        if(subScribe) PointWriteObservable.subscribe(id!!, this)
    }

    private fun loadBackFillTime(subScribe: Boolean) {
        val backfillQuery = "backfill and duration"
        val backfillIndex: Int
        val ccuHsApi = CCUHsApi.getInstance()
        if (ccuHsApi.readEntity(backfillQuery).isNotEmpty()) {
            val backfillValue = Domain.ccuEquip.backFillDuration.readDefaultVal()
            backfillIndex = BackFillViewModel.BackFillDuration.getIndex(
                BackFillViewModel.BackFillDuration.toIntArray(),
                backfillValue.toInt(),
                BackFillViewModel.BACKFIELD_DEFAULT_DURATION
            )
        } else {
            backfillIndex = BackFillViewModel.BACKFIELD_DEFAULT_DURATION_INDEX
        }
        val backfillOptions = getBackFillOptions()
        backFillTime.value = DetailedViewItem(
            id = Domain.ccuEquip.backFillDuration.id,
            usesDropdown = true,
            dropdownOptions = backfillOptions,
            selectedIndex = backfillIndex
        )
        if (subScribe) PointWriteObservable.subscribe(Domain.ccuEquip.backFillDuration.id, this)
    }


    private fun loadOfflineMode(subscribe: Boolean = false) {
        val state = CCUHsApi.getInstance().readDefaultVal("offline and mode") > 0

        val offlineModeEntity = CCUHsApi.getInstance()
            .readEntity("offline and mode")

        val id = offlineModeEntity["id"].toString()

        offlineMode.value = ToggleViewItem(
            id = id,
            state = state
        )
        if (subscribe)
            PointWriteObservable.subscribe(id, this)

    }

    private fun loadTempLockoutCooling(subscribe: Boolean = false) {

        if (ccu().systemProfile == null ||
            ccu().systemProfile.profileType == ProfileType.SYSTEM_DEFAULT
        ) {
            return
        }

        val state = isOutsideTempCoolingLockoutEnabled(CCUHsApi.getInstance())
        val coolingLockOutEnable = CCUHsApi.getInstance()
            .readEntity("system and config and cooling and lockout")
        val id = coolingLockOutEnable["id"].toString()
        coolingLockoutEnable.value = ToggleViewItem(
            id = id,
            state = state
        )

        if (subscribe) {
            PointWriteObservable.subscribe(coolingLockOutEnable["id"].toString(), this)
        }


        val coolingLockoutEntity = getCoolingLockoutEntity()
        val tunerValue = getTuner(coolingLockoutEntity["id"].toString())
        var convertedVal = tunerValue
        val spinnerOptions = mutableListOf<String>()
        if (isCelsiusTunerAvailableStatus()) {
            val start = Math.round(fahrenheitToCelsius(0.0)).toInt()
            val end = Math.round(fahrenheitToCelsius(70.0)).toInt()

            for (i in start..end) {
                spinnerOptions.add(i.toDouble().toString())
            }

            convertedVal = Math.round(fahrenheitToCelsius(tunerValue)).toDouble()
        } else {
            for (i in 0..70) {
                spinnerOptions.add(i.toDouble().toString())
            }
        }

        CcuLog.d("kumar_debug", "viewmodel cooling spinnerOptions size: ${spinnerOptions.size} " +
                ""+spinnerOptions)
        CcuLog.d("kumar_debug", "viewmodel cooling index " +
                ""+spinnerOptions.indexOf(convertedVal.toString()))

        coolingLockoutDropdown.value = DetailedViewItem(
            id = coolingLockoutEntity["id"].toString(),
            usesDropdown = true,
            dropdownOptions = spinnerOptions,
            selectedIndex = spinnerOptions.indexOf(convertedVal.toString())
        )

        if (subscribe) {
            PointWriteObservable.subscribe(coolingLockoutEntity["id"].toString(), this)
            PointWriteObservable.subscribe(coolingLockOutEnable["id"].toString(), this)
        }
    }

    private fun loadTempLockoutHeating(subscribe: Boolean = false) {
        if (ccu().systemProfile == null ||
            ccu().systemProfile.profileType == ProfileType.SYSTEM_DEFAULT
        ) {
            return
        }

        val state = isOutsideTempHeatingLockoutEnabled(CCUHsApi.getInstance())
        val heatingLockOutEnableEntity = CCUHsApi.getInstance()
            .readEntity("system and config and heating and lockout")
        val id = heatingLockOutEnableEntity["id"].toString()

        heatingLockoutEnable.value = ToggleViewItem(
            id = id,
            state = state
        )

        if (subscribe) {
            PointWriteObservable.subscribe(id, this)
        }


        val heatingLockoutDropDownEntity = getHeatingLockoutEntity()
        val tunerValue = getTuner(heatingLockoutDropDownEntity["id"].toString())


        var convertedVal = tunerValue
        val heatingLockoutList = mutableListOf<String>()
        if (isCelsiusTunerAvailableStatus()) {
            val start = Math.round(fahrenheitToCelsius(50.0)).toInt()
            val end = Math.round(fahrenheitToCelsius(100.0)).toInt()
            for (i in start..end) {
                heatingLockoutList.add(i.toDouble().toString())
            }

            convertedVal = Math.round(fahrenheitToCelsius(tunerValue)).toDouble()
        } else {
            for (i in 50..100) {
                heatingLockoutList.add(i.toDouble().toString())
            }
        }

        heatingLockoutDropdown.value = DetailedViewItem(
            id = heatingLockoutDropDownEntity["id"].toString(),
            usesDropdown = true,
            dropdownOptions = heatingLockoutList,
            selectedIndex = heatingLockoutList.indexOf(convertedVal.toString())
        )

        if (subscribe) {
            PointWriteObservable.subscribe(heatingLockoutDropDownEntity["id"].toString(), this)
        }

    }

    private fun showHeaderView(
        installerOptionsTopView: ComposeView,
        installerOptionsBottomView: ComposeView,
        systemViewModel: InstallerOptionsViewModel
    ) {
        installerOptionsTopView.showTopViews(systemViewModel) { _, _ ->
            // handle change here
        }

        installerOptionsBottomView.showBottomViews(systemViewModel) { _, _ ->
            // handle change here
        }
    }

    private fun loadTemperatureMode(subScribe: Boolean = false) {
        val point: HashMap<Any, Any> = Domain.hayStack.readEntity(
            "point and" +
                    " domainName == \"${DomainName.temperatureMode}\""
        )
        if (point.isNotEmpty()) {
            val id = point["id"].toString()
            if (subScribe) PointWriteObservable.subscribe(id, this)
            val tempMode = CCUHsApi.getInstance().readDefaultValByLevel(
                id,
                TunerConstants.SYSTEM_BUILDING_VAL_LEVEL
            )
            val options = TemperatureModeUtil().getTemperatureModeArray()
            temperatureMode.value = DetailedViewItem(
                id = point["id"].toString(),
                usesDropdown = true,
                dropdownOptions = options,
                selectedIndex = tempMode.toInt()
            )
        } else {
            CcuLog.i(Domain.LOG_TAG, "Temperature Mode point does not exist")
            /*By default point value should be 1*/
            temperatureMode.value = DetailedViewItem(
                id = "",
                usesDropdown = true,
                dropdownOptions = listOf(""),
                selectedIndex = 0
            )
        }
    }

    fun intimateAdvanceAhu() {
        if (ccu().systemProfile is VavAdvancedAhu) {
            VavAdvancedHybridAhuFragment.instance.viewModel.toggleChecked()
        }
        if (ccu().systemProfile is DabAdvancedAhu) {
            DabAdvancedHybridAhuFragment.instance.viewModel.toggleChecked()
        }
    }

    private fun isOutsideTempCoolingLockoutEnabled(hayStack: CCUHsApi): Boolean {
        return hayStack.readDefaultVal(
            "system and config and cooling and lockout"
        ) > 0
    }

    private fun isOutsideTempHeatingLockoutEnabled(hayStack: CCUHsApi): Boolean {
        return hayStack.readDefaultVal(
            "system and config and " +
                    "heating and lockout"
        ) > 0
    }

    fun setOutsideTempCoolingLockoutEnabled(hayStack: CCUHsApi, enabled: Boolean) {
        hayStack.writeDefaultVal(
            "system and config and cooling and lockout",
            if (enabled) 1.0 else 0.0
        )

        hayStack.writeHisValByQuery(
            "system and config and cooling and lockout",
            if (enabled) 1.0 else 0.0
        )
    }

    fun setOutsideTempHeatingLockoutEnabled(hayStack: CCUHsApi, enabled: Boolean) {
        hayStack.writeDefaultVal(
            "system and config and heating and lockout",
            if (enabled) 1.0 else 0.0
        )
        hayStack.writeHisValByQuery(
            "system and config and heating and lockout",
            if (enabled) 1.0 else 0.0
        )
    }

    override fun onHisPointChanged(pointId: String, value: Double) {
        //
    }

    override fun onWritablePointChanged(pointId: String, value: Any) {
        when (pointId) {
            coolingLockoutEnable.value.id -> loadTempLockoutCooling(false)
            heatingLockoutEnable.value.id -> loadTempLockoutHeating(false)
            coolingLockoutDropdown.value.id -> loadTempLockoutCooling(false)
            heatingLockoutDropdown.value.id -> loadTempLockoutHeating(false)
            offlineMode.value.id -> loadOfflineMode(false)
            drEnrollment.value.id -> loadDRMode(false)
            useCelsius.value.id -> {
                loadCelsiusToggle(false)
                loadTempLockoutCooling(false)
                loadTempLockoutHeating(false)
            }backFillTime.value.id      -> loadBackFillTime(false)
            temperatureMode.value.id   -> loadTemperatureMode(false)
        }
    }

    private fun getCoolingLockoutEntity(): HashMap<Any, Any> {
        if (ccu().systemProfile.isVavSystemProfile) {
            return CCUHsApi.getInstance().readEntity(
                "((domainName == \"vavOutsideTempCoolingLockout\") or" +
                        " (outsideTemp and cooling and lockout)) " +
                        "and equipRef == \"" + ccu().systemProfile.getSystemEquipRef() + "\""
            )

        }

        return CCUHsApi.getInstance().readEntity(
            "((domainName == \"dabOutsideTempCoolingLockout\") or" +
                    " (outsideTemp and cooling and lockout)) " +
                    "and equipRef == \"" + ccu().systemProfile.getSystemEquipRef() + "\""
        )
    }

    private fun getHeatingLockoutEntity(): HashMap<Any, Any> {
        if (ccu().systemProfile.isVavSystemProfile) {
            return CCUHsApi.getInstance().readEntity(
                "((domainName == \"vavOutsideTempHeatingLockout\") or" +
                        " (outsideTemp and heating and lockout)) " +
                        "and equipRef == \"" + ccu().systemProfile.getSystemEquipRef() + "\""
            )
        }

        return CCUHsApi.getInstance().readEntity(
            "((domainName == \"dabOutsideTempHeatingLockout\") or" +
                    " (outsideTemp and heating and lockout)) " +
                    "and equipRef == \"" + ccu().systemProfile.getSystemEquipRef() + "\""
        )
    }
}