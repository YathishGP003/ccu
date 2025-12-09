package a75f.io.renatus.ui.systemscreen.viewmodel

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.bacnet.parser.BacnetModelDetailResponse
import a75f.io.api.haystack.bacnet.parser.BacnetPoint
import a75f.io.api.haystack.bacnet.parser.BacnetZoneViewItem
import a75f.io.api.haystack.observer.HisWriteObservable
import a75f.io.api.haystack.observer.PointSubscriber
import a75f.io.api.haystack.observer.PointWriteObservable
import a75f.io.domain.OAOEquip
import a75f.io.domain.api.DomainName
import a75f.io.domain.api.DomainName.demandResponseActivation
import a75f.io.domain.api.DomainName.demandResponseEnrollment
import a75f.io.domain.api.DomainName.systemEnhancedVentilationEnable
import a75f.io.domain.api.DomainName.systemPostPurgeEnable
import a75f.io.domain.api.DomainName.systemPrePurgeEnable
import a75f.io.domain.util.ModelNames
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.modbus.buildModbusModelByEquipRef
import a75f.io.logic.bo.building.schedules.ScheduleManager
import a75f.io.logic.bo.building.system.DISCHARGE_AIR_TEMP
import a75f.io.logic.bo.building.system.DUCT_STATIC_PRESSURE_SENSOR
import a75f.io.logic.bo.building.system.DefaultSystem
import a75f.io.logic.bo.building.system.client.RemotePointUpdateInterface
import a75f.io.logic.bo.building.system.dab.DabAdvancedAhu
import a75f.io.logic.bo.building.system.dab.DabExternalAhu
import a75f.io.logic.bo.building.system.getConfigValue
import a75f.io.logic.bo.building.system.util.getAdvancedAhuSystemEquip
import a75f.io.logic.bo.building.system.util.getConnectEquip
import a75f.io.logic.bo.building.system.util.isConnectModuleExist
import a75f.io.logic.bo.building.system.vav.VavAdvancedAhu
import a75f.io.logic.bo.building.system.vav.VavExternalAhu
import a75f.io.logic.bo.building.system.vav.VavIERtu
import a75f.io.logic.bo.util.DemandResponseMode
import a75f.io.logic.bo.util.TemperatureMode
import a75f.io.logic.bo.util.UnitUtils
import a75f.io.logic.bo.util.UnitUtils.StatusCelsiusVal
import a75f.io.logic.bo.util.UnitUtils.isCelsiusTunerAvailableStatus
import a75f.io.logic.tuners.TunerUtil
import a75f.io.logic.util.bacnet.buildBacnetModelSystem
import a75f.io.messaging.handler.isAdvanceAhuV2Profile
import a75f.io.renatus.modbus.util.isOaoPairedInConnectModule
import a75f.io.renatus.ui.systemscreen.helper.getMaxInsideHumidityView
import a75f.io.renatus.ui.systemscreen.helper.getMinInsideHumidityView
import a75f.io.renatus.ui.systemscreen.helper.getModbusPointValueByQuery
import a75f.io.renatus.ui.systemscreen.helper.getSetPointByDomainName
import a75f.io.renatus.ui.systemscreen.helper.getSystemOperatingMode
import a75f.io.renatus.ui.systemscreen.view.showEpidemicModeView
import a75f.io.renatus.ui.systemscreen.view.showHeaderViews
import a75f.io.renatus.ui.systemscreen.view.showProfileComposeView
import a75f.io.renatus.ui.zonescreen.model.DetailedViewItem
import a75f.io.renatus.ui.zonescreen.model.HeaderViewItem
import a75f.io.renatus.ui.zonescreen.model.ToggleViewItem
import a75f.io.renatus.ui.zonescreen.nontempprofiles.helper.fetchZoneDataForBacnet
import a75f.io.renatus.ui.zonescreen.nontempprofiles.helper.getBacnetDetailedViewPoints
import a75f.io.renatus.ui.zonescreen.nontempprofiles.helper.getModbusDetailedViewPoints
import a75f.io.renatus.ui.zonescreen.nontempprofiles.model.ExternalPointItem
import a75f.io.renatus.ui.zonescreen.nontempprofiles.utilities.getDropDownPosition
import a75f.io.renatus.ui.zonescreen.nontempprofiles.utilities.getIndexOf
import a75f.io.renatus.ui.zonescreen.nontempprofiles.utilities.handleBacnetPoint
import a75f.io.renatus.ui.zonescreen.nontempprofiles.utilities.handleModbusOrConnectModulePoint
import a75f.io.renatus.ui.zonescreen.nontempprofiles.utilities.heartBeatStatus
import a75f.io.renatus.util.HeartBeatUtil
import a75f.io.renatus.util.HeartBeatUtil.isModuleAlive
import a75f.io.renatus.views.OaoArc
import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.StringUtils
import java.text.DecimalFormat
import java.util.Locale
import java.util.Objects
import java.util.concurrent.Executors

@SuppressLint("StaticFieldLeak")
class SystemViewModel : ViewModel(), PointSubscriber {

    private var isDefaultSystemProfile = false
    var oaoEquip: HashMap<Any, Any> = hashMapOf()
    private var oaoDomainEquip: OAOEquip = OAOEquip("")
    var oaoArc: OaoArc? = null
    var handler = Handler(Looper.getMainLooper())
    var runnable: Runnable? = null
    var occupancyStatus = mutableStateOf("")
    var equipStatus = mutableStateOf("")
    var cnEquipStatus = mutableStateOf("")
    var lastUpdated = mutableStateOf("")

    var minInsideHumidity = mutableStateOf(DetailedViewItem())
    var maxInsideHumidity = mutableStateOf(DetailedViewItem())

    var isDemandResponseModeActivated = mutableStateOf(ToggleViewItem())
    var isDemandResponseEnrolled = mutableStateOf(ToggleViewItem())


    var isVavIERtu = false
    var occupancyStatusOnIE = mutableStateOf("")
    private var guidDetails = mutableStateOf("")

    var isExternalAhuPaired = false
    var isModbusExists = false
    var isBacnetExists = false
    var isAdvancedAHUPaired = false
    var isConnectAndAdvanceAHUPaired = false
    var isOAOProfilePaired = false

    var oaoLastUpdated = mutableStateOf("")
    var smartPrePurgeState = mutableStateOf(ToggleViewItem())
    var smartPostPurgeState = mutableStateOf(ToggleViewItem())
    var enhancedVentilation = mutableStateOf(ToggleViewItem())

    var isPressureControlAvailable = mutableStateOf(false)
    private var isCo2DamperControlAvailable = mutableStateOf(false)

    var ductStaticPressureSensor = mutableStateOf(HeaderViewItem())
    var ductStaticPressureSetpoint = mutableStateOf(HeaderViewItem())

    var dischargeAirTempSensor = mutableStateOf(HeaderViewItem())
    var supplyAirflowTemperatureSetpoint = mutableStateOf(HeaderViewItem())

    var operatingMode = mutableStateOf(HeaderViewItem())
    var satCoolingSetPoint = mutableStateOf(HeaderViewItem())
    var satHeatingSetPoint = mutableStateOf(HeaderViewItem())

    var isDualSetPointControlEnabled = mutableStateOf(false)
    var isSATCoolingAvailable = mutableStateOf(false)
    var isSATHeatingAvailable = mutableStateOf(false)
    var isDCVEnabled = mutableStateOf(false)
    var dcvDamperPos = mutableStateOf(HeaderViewItem())

    var detailedViewPoints = mutableStateListOf<ExternalPointItem>()
    private var externalEquipNodeAddress : String  = "0"
    private var externalEquipId : String = ""
    var externalEquipName = mutableStateOf(HeaderViewItem())
    var externalEquipHeartBeat = mutableStateOf(false)
    var externalEquipLastUpdated =  mutableStateOf(HeaderViewItem())


    var btuMeterPoints = mutableStateListOf<ExternalPointItem>()
    var isBTUPaired = mutableStateOf(false)
    private var btuNodeAddress : String  = "0"
    private var btuEquipId : String = ""
    var btuEquipName = mutableStateOf(HeaderViewItem())
    var btuHeartBeat = mutableStateOf(false)
    var btuLastUpdated =  mutableStateOf(HeaderViewItem())


    var emrPoints = mutableStateListOf<ExternalPointItem>()
    var isEMRPaired = mutableStateOf(false)
    private var emrNodeAddress : String  = "0"
    private var emrEquipId : String = ""
    var emrEquipName = mutableStateOf(HeaderViewItem())
    var emrHeartBeat = mutableStateOf(false)
    var emrLastUpdated =  mutableStateOf(HeaderViewItem())
    private var remotePointUpdateInterface: RemotePointUpdateInterface? = null

    fun initializeProfileStates() {
        isDefaultSystemProfile = L.ccu().systemProfile is DefaultSystem
        isVavIERtu = L.ccu().systemProfile is VavIERtu
        isOAOProfilePaired = L.ccu().oaoProfile != null || isOaoPairedInConnectModule()
        isAdvancedAHUPaired = isAdvanceAhuV2Profile()
        isExternalAhuPaired = L.ccu().systemProfile is DabExternalAhu || L.ccu().systemProfile is VavExternalAhu
        PointWriteObservable.subscribe("systemSchedule", this)
    }

    fun loadViews(
        headerComposeView: ComposeView,
        profilePointComposeView: ComposeView,
        epidemicModeComposeView: ComposeView,
        remotePointUpdateInterface: RemotePointUpdateInterface
    ) {
        showHeaderView(headerComposeView, this)
        showProfilePointsView(profilePointComposeView, this, remotePointUpdateInterface)
        showEpidemicModeView(epidemicModeComposeView, this)
        loadOccupancyStatusAndEquipStatus()
        loadHumidifierAndDehumidifierViews()
        demandResponseView()
        loadOaoViews()
        observeEquipHealth()
        loadExternalAHUViews()
        loadAdvancedAHUViews()
        loadBTUorEnergyMeter()
        this.remotePointUpdateInterface = remotePointUpdateInterface
    }

    private fun loadOaoViews() {
        if (isOAOProfilePaired) {
            loadOaoEquip()
            updateOaoHealth()
            updatePrePurgeState(true)
            updatePostPurgeState(true)
            updateEnhancedVentilationState(true)
            oaoDomainEquip = OAOEquip(oaoEquip["id"].toString())
            initializeReturnAirCo2()
            if(isOaoPairedInConnectModule()){
                oaoArc?.disableHeartBeat()
            }
        } else {
            oaoDomainEquip = OAOEquip("")
        }

        if (isOaoPairedInConnectModule()) {
            HisWriteObservable.subscribe(getConnectEquip()?.getId().toString(), this)
        }

    }

    private fun initializeReturnAirCo2() {
        oaoDomainEquip.returnAirCo2.pointExists()
        HisWriteObservable.subscribe(oaoDomainEquip.returnAirCo2.id, this)
        onHisPointChanged(oaoDomainEquip.returnAirCo2.id, oaoDomainEquip.returnAirCo2.readHisVal())
    }

    private fun loadOaoEquip() {
        oaoEquip = CCUHsApi.getInstance().readEntity("equip and oao and not hyperstatsplit")
    }

    private fun updateOaoHealth() {
        if(!isOaoPairedInConnectModule()) {
            oaoLastUpdated.value = HeartBeatUtil.getLastUpdatedTime(oaoEquip["group"].toString())
        }
    }

    private fun updatePrePurgeState(subscribe: Boolean = false) {
        val state = TunerUtil.readSystemUserIntentVal("domainName == \"$systemPrePurgeEnable\"") > 0

        if (subscribe) {
            val prePurgePoint = CCUHsApi.getInstance()
                .readEntity("domainName == \"$systemPrePurgeEnable\"")

            val id = prePurgePoint["id"].toString()

            smartPrePurgeState.value = ToggleViewItem(
                id = id,
                state = state
            )

            PointWriteObservable.subscribe(id, this)
        } else {
            smartPrePurgeState.value = smartPrePurgeState.value.copy(state = state)
        }
    }


    private fun updatePostPurgeState(subscribe: Boolean = false) {
        val state = TunerUtil.readSystemUserIntentVal("domainName == \"$systemPostPurgeEnable\"") > 0

        if (subscribe) {
            val prePurgePoint = CCUHsApi.getInstance()
                .readEntity("domainName == \"$systemPostPurgeEnable\"")

            val id = prePurgePoint["id"].toString()

            smartPostPurgeState.value = ToggleViewItem(
                id = id,
                state = state
            )

            PointWriteObservable.subscribe(id, this)
        } else {
            smartPostPurgeState.value = smartPostPurgeState.value.copy(state = state)
        }
    }


    private fun updateEnhancedVentilationState(subscribe: Boolean = false) {
        val state = TunerUtil.readSystemUserIntentVal("domainName == \"$systemEnhancedVentilationEnable\"") > 0

        if (subscribe) {
            val enhancedVentilationToggle = CCUHsApi.getInstance()
                .readEntity("domainName == \"$systemEnhancedVentilationEnable\"")

            val id = enhancedVentilationToggle["id"].toString()

            enhancedVentilation.value = ToggleViewItem(
                id = id,
                state = state
            )

            PointWriteObservable.subscribe(id, this)
        } else {
            enhancedVentilation.value = enhancedVentilation.value.copy(state = state)
        }
    }


    private fun loadOccupancyStatusAndEquipStatus() {
        occupancyStatusView()
        equipStatusView()
        lastUpdatedView()
    }

    private fun loadHumidifierAndDehumidifierViews() {
        minInsideHumidity.value = getMinInsideHumidityView(isDefaultSystemProfile)
        PointWriteObservable.subscribe(minInsideHumidity.value.id.toString(), this)
        maxInsideHumidity.value = getMaxInsideHumidityView(isDefaultSystemProfile)
        PointWriteObservable.subscribe(maxInsideHumidity.value.id.toString(), this)
    }


    private fun showHeaderView(
        composeView: ComposeView,
        systemViewModel: SystemViewModel
    ) {
        composeView.showHeaderViews(systemViewModel)
    }

    private val backgroundExecutor = Executors.newSingleThreadExecutor()
    private fun showProfilePointsView(
        composeView: ComposeView,
        systemViewModel: SystemViewModel,
        remotePointUpdateInterface: RemotePointUpdateInterface
    ) {
        composeView.showProfileComposeView(systemViewModel) {
            selectedIndex: Int, point: Any ->
            backgroundExecutor.execute {
                val externalPointItem = point as ExternalPointItem
                val profileType = externalPointItem.profileType
                val isConnectNode = "connectModule".equals(profileType, ignoreCase = true)
                if ("modbus".equals(profileType, ignoreCase = true) || isConnectNode) {
                    handleModbusOrConnectModulePoint(
                        externalPointItem,
                        externalEquipId,
                        selectedIndex,
                        isConnectNode,
                        false
                    )
                } else {
                    handleBacnetPoint(externalPointItem, selectedIndex, remotePointUpdateInterface)
                }
            }
        }
    }

    private fun showEpidemicModeView(
        composeView: ComposeView,
        systemViewModel: SystemViewModel
    ) {
        composeView.showEpidemicModeView(systemViewModel) { selectedIndex: Int, point: Any ->
            backgroundExecutor.execute {
                val externalPointItem = point as ExternalPointItem
                val profileType = externalPointItem.profileType
                if ("modbus".equals(profileType, ignoreCase = true)) {
                    handleModbusOrConnectModulePoint(
                        externalPointItem,
                        externalEquipId,
                        selectedIndex,
                        isConnectNode = false,
                        isPCN = false
                    )
                }
            }
        }
    }

    private fun occupancyStatusView() {
        if (isDefaultSystemProfile) {
            occupancyStatus.value = ScheduleManager.getInstance().getSystemStatusString()
        } else {
            if (isCelsiusTunerAvailableStatus()) {
                occupancyStatus.value = StatusCelsiusVal(
                    ScheduleManager.getInstance()
                        .getSystemStatusString(), TemperatureMode.DUAL.ordinal
                )
            } else {
                occupancyStatus.value = ScheduleManager.getInstance().getSystemStatusString()
            }
        }

        if (isVavIERtu) {
            guidDetails.value = CCUHsApi.getInstance().siteIdRef.toString()
            occupancyStatusOnIE.value = getOccStatus()
        }
    }

    private fun equipStatusView() {
        isConnectAndAdvanceAHUPaired = isAdvanceAhuV2Profile() && isConnectModuleExist()
        if (isConnectAndAdvanceAHUPaired) {
            equipStatus.value =
                getAdvancedAhuSystemEquip().equipStatusMessage.readDefaultStrVal()
            cnEquipStatus.value = getConnectEquip()!!.equipStatusMessage.readDefaultStrVal()
        } else {
            equipStatus.value = CCUHsApi.getInstance()
                .readDefaultStrVal("system  and domainName == \"" + DomainName.equipStatusMessage + "\"")
        }

        //If the system status is not updated yet (within a minute of registering the device), generate a
        //default message.
        if (StringUtils.isEmpty(equipStatus.value)) {
            equipStatus.value = L.ccu().systemProfile.statusMessage
        }

        equipStatus.value = if (isDefaultSystemProfile) {
            occupancyStatus.value = ScheduleManager.getInstance().systemStatusString
            "System is in gateway mode"
        } else {
            equipStatus.value
        }
    }


    private fun lastUpdatedView() {
        lastUpdated.value = HeartBeatUtil.getLastUpdatedTime(Tags.CLOUD)
    }

    private fun demandResponseView() {
        val demandResponseEnrollment = CCUHsApi.getInstance()
            .readEntity("domainName == \"$demandResponseEnrollment\"")
        if (demandResponseEnrollment.isEmpty()) return
        isDemandResponseEnrolled.value.id = demandResponseEnrollment["id"].toString()
        isDemandResponseEnrolled.value.state = DemandResponseMode.isDREnrollmentSelected()
        PointWriteObservable.subscribe(isDemandResponseEnrolled.value.id.toString(), this)
        if (isDemandResponseEnrolled.value.state) {
            val demandResponseActivation = CCUHsApi.getInstance()
                .readEntity("domainName == \"$demandResponseActivation\"")
            isDemandResponseModeActivated.value.id = demandResponseActivation["id"].toString()
            isDemandResponseModeActivated.value.state = DemandResponseMode.isDRModeActivated()
            PointWriteObservable.subscribe(isDemandResponseModeActivated.value.id.toString(), this)
        }
    }

    private fun getOccStatus(): String {
        val point = CCUHsApi.getInstance().readEntity(
            "point and " +
                    "system and ie and occStatus"
        )
        if (point.isNotEmpty()) {
            val occStatus = CCUHsApi.getInstance().readHisValById(point["id"].toString())
            return when (occStatus) {
                0.0 -> {
                    "Occupied"
                }
                1.0 -> {
                    "Unoccupied"
                }
                else -> {
                    "Tenant Override"
                }
            }
        }
        return "Unoccupied"
    }

    override fun onHisPointChanged(pointId: String, value: Double) {
        if (isOAOProfilePaired) {
            if (pointId.equals(oaoDomainEquip.returnAirCo2.id, ignoreCase = true)) {
                val co2Threshold = oaoDomainEquip.co2Threshold.readHisVal()
                var angel = co2Threshold / 20
                if (angel < 0) {
                    angel = 0.0
                } else if (angel > 2000) {
                    angel = 2000.0
                }

                val returnAirCO2 = oaoDomainEquip.returnAirCo2.readHisVal()
                var progress = returnAirCO2 / 20
                if (progress < 0) {
                    progress = 0.0
                } else if (progress > 2000) {
                    progress = 2000.0
                }

                oaoArc?.progress = progress.toInt()
                CcuLog.d(  Tags.CCU_SYSTEM_SCREEN,"updating co2 value $returnAirCO2")
                oaoArc?.post {
                    oaoArc?.setData(angel.toInt(), returnAirCO2.toInt())
                    oaoArc?.contentDescription = returnAirCO2.toString()
                    oaoArc?.invalidate()
                    oaoArc?.requestLayout()
                }
                return
            }

            if (pointId.equals(getConnectEquip()?.returnAirCo2?.id, ignoreCase = true)) {
                val co2Threshold = getConnectEquip()?.co2Threshold?.readHisVal()
                var angel = co2Threshold?.div(20)
                if (angel != null) {
                    if (angel < 0) {
                        angel = 0.0
                    } else if (angel > 2000) {
                        angel = 2000.0
                    }
                }

                val returnAirCO2 = getConnectEquip()?.returnAirCo2?.readHisVal()
                var progress = returnAirCO2?.div(20)
                if (progress != null) {
                    if (progress < 0) {
                        progress = 0.0
                    } else if (progress > 2000) {
                        progress = 2000.0
                    }
                }

                if (progress != null) {
                    oaoArc?.progress = progress.toInt()
                }
                if (angel != null) {
                    if (returnAirCO2 != null) {
                        oaoArc?.post {
                            oaoArc?.setData(angel.toInt(), returnAirCO2.toInt())
                        }
                    }
                }
                oaoArc?.contentDescription = returnAirCO2.toString()
                oaoArc?.invalidate()
                return
            }
        }

        if (isExternalAhuPaired || isAdvancedAHUPaired) {
            val matchingIds = listOf(
                dischargeAirTempSensor.value.id,
                ductStaticPressureSensor.value.id,
                ductStaticPressureSetpoint.value.id,
                supplyAirflowTemperatureSetpoint.value.id,
                operatingMode.value.id,
                satCoolingSetPoint.value.id,
                satHeatingSetPoint.value.id,
                dcvDamperPos.value.id
            )

            if (pointId in matchingIds && isExternalAhuPaired) {
                loadExternalAHUViews()
            } else if (pointId in matchingIds && isAdvancedAHUPaired) {
                loadAdvancedAHUViews()
            } else {
                changeInExternalEquipPoints(pointId, value)
            }
        }
    }

    override fun onWritablePointChanged(pointId: String, value: Any) {
        when (pointId) {
            "systemSchedule" -> {
                loadOccupancyStatusAndEquipStatus()
                return
            }

            minInsideHumidity.value.id -> {
                minInsideHumidity.value = getMinInsideHumidityView(isDefaultSystemProfile)
                return
            }

            maxInsideHumidity.value.id -> {
                maxInsideHumidity.value = getMaxInsideHumidityView(isDefaultSystemProfile)
                return
            }

            smartPrePurgeState.value.id.toString() -> {
                updatePrePurgeState()
                return
            }

            smartPostPurgeState.value.id.toString() -> {
                updatePostPurgeState()
                return
            }

            enhancedVentilation.value.id.toString() -> {
                updateEnhancedVentilationState()
                return
            }

            isDemandResponseModeActivated.value.id,
            isDemandResponseEnrolled.value.id -> {
                isDemandResponseModeActivated.value =
                    isDemandResponseModeActivated.value.copy(
                        state = DemandResponseMode.isDRModeActivated()
                    )
            }
        }
    }

    private fun loadExternalAHUViews() {
        if (!isExternalAhuPaired) return

        if (L.ccu().systemProfile is VavExternalAhu) {
            showExternalAhuConfigPoints(ModelNames.VAV_EXTERNAL_AHU_CONTROLLER)
        } else {
            showExternalAhuConfigPoints(ModelNames.DAB_EXTERNAL_AHU_CONTROLLER)
        }

        loadExternalModbusViews()
        loadBacnetViews()
    }

    private fun showExternalAhuConfigPoints(externalControllerVariant: String) {
       isDualSetPointControlEnabled.value =
           getConfigValue("dualSetpointControlEnable", externalControllerVariant)
       isDCVEnabled.value =
           getConfigValue("dcvDamperControlEnable", externalControllerVariant)

        //DSP current
        val ductStaticPressureSensorData = getModbusPointValueByQuery(DUCT_STATIC_PRESSURE_SENSOR)
        ductStaticPressureSensor.value = HeaderViewItem(
            id = ductStaticPressureSensorData.first,
            currentValue = ductStaticPressureSensorData.second
        )
        HisWriteObservable.subscribe(ductStaticPressureSensor.value.id.toString(), this)


        //DSP setpoint
        val ductStaticPressureSetpointData =
            getSetPointByDomainName("ductStaticPressureSetpoint")
        ductStaticPressureSetpoint.value = HeaderViewItem(
            id = ductStaticPressureSetpointData.first,
            currentValue = ductStaticPressureSetpointData.second
        )
        HisWriteObservable.subscribe(ductStaticPressureSetpoint.value.id.toString(), this)


        if (isDualSetPointControlEnabled.value) {
            //SAT current
            val dischargeAirTempSensorData = getModbusPointValueByQuery(DISCHARGE_AIR_TEMP)
            dischargeAirTempSensor.value = HeaderViewItem(
                id = dischargeAirTempSensorData.first,
                currentValue = dischargeAirTempSensorData.second
            )
            HisWriteObservable.subscribe(dischargeAirTempSensor.value.id.toString(), this)

            //SAT operatingMode
            val operatingModeData = getSystemOperatingMode(externalControllerVariant)
            operatingMode.value = HeaderViewItem(
                id = operatingModeData.first,
                currentValue = operatingModeData.second
            )
            HisWriteObservable.subscribe(operatingMode.value.id.toString(), this)

            //SAT airTempHeatingSp
            val satCoolingSetPointData = getSetPointByDomainName("airTempCoolingSp")
            satCoolingSetPoint.value = HeaderViewItem(
                id = satCoolingSetPointData.first,
                currentValue = satCoolingSetPointData.second
            )
            HisWriteObservable.subscribe(satCoolingSetPoint.value.id.toString(), this)

            //SAT airTempCoolingSp
            val satHeatingSetPointData = getSetPointByDomainName("airTempHeatingSp")
            satHeatingSetPoint.value = HeaderViewItem(
                id = satHeatingSetPointData.first,
                currentValue = satHeatingSetPointData.second
            )
            HisWriteObservable.subscribe(satHeatingSetPoint.value.id.toString(), this)

        } else {
            //SAT current
            val dischargeAirTempSensorData = getModbusPointValueByQuery(DISCHARGE_AIR_TEMP)
            dischargeAirTempSensor.value = HeaderViewItem(
                id = dischargeAirTempSensorData.first,
                currentValue = dischargeAirTempSensorData.second
            )
            HisWriteObservable.subscribe(dischargeAirTempSensor.value.id.toString(), this)


            //SAT setPoint
            val supplyAirflowTemperatureSetPointData =
                getSetPointByDomainName("supplyAirflowTemperatureSetpoint")
            supplyAirflowTemperatureSetpoint.value = HeaderViewItem(
                id = supplyAirflowTemperatureSetPointData.first,
                currentValue = supplyAirflowTemperatureSetPointData.second
            )
            HisWriteObservable.subscribe(supplyAirflowTemperatureSetpoint.value.id.toString(), this)
        }

        isDCVEnabled.value = getConfigValue("dcvDamperControlEnable", externalControllerVariant)
        if (isDCVEnabled.value) {
            val dcvDamperPosPointData = getSetPointByDomainName("dcvDamperCalculatedSetpoint")
            dcvDamperPos.value = HeaderViewItem(
                id = dcvDamperPosPointData.first,
                currentValue = dcvDamperPosPointData.second
            )
            HisWriteObservable.subscribe(dcvDamperPos.value.id.toString(), this)
        }
    }

    private fun loadAdvancedAHUViews() {
        if (!isAdvancedAHUPaired) return

        if(L.ccu().systemProfile is VavAdvancedAhu){
            showAdvancedAhuVavConfigPoints(L.ccu().systemProfile as VavAdvancedAhu)
        } else {
            showAdvancedAhuDabConfigPoints(L.ccu().systemProfile as DabAdvancedAhu)
        }

    }

    private fun observeEquipHealth() {
        runnable = object : Runnable {
            override fun run() {
                lastUpdatedView()
                if(isOAOProfilePaired) updateOaoHealth()
                updateExternalEquipHealth()
                updateBTUEquipHealth()
                updateEMREquipHealth()
                if (isOAOProfilePaired && !isOaoPairedInConnectModule()) {
                    oaoArc?.updateStatus(isModuleAlive(oaoEquip["group"].toString()))
                    oaoArc?.invalidate()
                }
                handler.postDelayed(this, 60_000L)
            }
        }
        handler.post(runnable!!)
    }


    fun stopObservingEquipHealth() {
        runnable?.let {
            handler.removeCallbacks(it)
        }
        runnable = null
    }

    override fun onCleared() {
        stopObservingEquipHealth()
    }

    private fun loadExternalModbusViews() {
        if (isExternalAhuPaired) {
            val modbusEquip = CCUHsApi.getInstance()
                .readEntity("system and equip and modbus and not emr and not btu")
            isModbusExists = modbusEquip.isNotEmpty()
            if (modbusEquip.isNotEmpty()) {
                val externalModbusEquip = buildModbusModelByEquipRef(
                    Objects.requireNonNull(
                        modbusEquip["id"]
                    ).toString()
                )
                val nodeAddress: String = externalModbusEquip.slaveId.toString()
                val displayIndex: Int = externalModbusEquip.name.lastIndexOf('-') + 1
                val displayName: String = externalModbusEquip.name.substring(displayIndex)
                if (!externalModbusEquip.equipType.contains(displayName)) {
                    externalEquipName.value =
                        HeaderViewItem(currentValue = "$displayName($nodeAddress)")
                } else {
                    externalEquipName.value = HeaderViewItem(
                        currentValue = externalModbusEquip.name + "(" + externalModbusEquip.equipType.uppercase(
                            Locale.getDefault()
                        ) + nodeAddress + ")"
                    )
                }
                val modbusPoints = getModbusDetailedViewPoints(
                    externalModbusEquip,
                    "modbus",
                    modbusEquip["id"].toString()
                )
                initializeDetailedViewPoints(modbusPoints)
                externalEquipNodeAddress = nodeAddress
                externalEquipId = modbusEquip["id"].toString()
                updateExternalEquipHealth()
            }
        }
    }

    fun updateExternalEquipHealth() {
        externalEquipHeartBeat.value = heartBeatStatus(externalEquipNodeAddress)
        externalEquipLastUpdated.value = HeaderViewItem(
            disName = "Last Updated: ",
            currentValue = HeartBeatUtil.getLastUpdatedTime(externalEquipNodeAddress).toString()
        )
    }

    private fun updateBTUEquipHealth() {
        btuHeartBeat.value = heartBeatStatus(btuNodeAddress)
        btuLastUpdated.value = HeaderViewItem(
            disName = "Last Updated: ",
            currentValue = HeartBeatUtil.getLastUpdatedTime(btuNodeAddress).toString()
        )
    }

    private fun updateEMREquipHealth() {
        emrHeartBeat.value = heartBeatStatus(emrNodeAddress)
        emrLastUpdated.value = HeaderViewItem(
            disName = "Last Updated: ",
            currentValue = HeartBeatUtil.getLastUpdatedTime(emrNodeAddress).toString()
        )
    }


    private fun loadBacnetViews() {

        if (isExternalAhuPaired) {
            val bacnetEquip =
                CCUHsApi.getInstance()
                    .readEntity("system and equip and bacnet and not emr and not btu")

            isBacnetExists = bacnetEquip.isNotEmpty()
            if (bacnetEquip.isNotEmpty() && bacnetEquip != null) {
                var bacNetPointsList: List<BacnetZoneViewItem> = ArrayList()
                externalEquipName.value =
                    HeaderViewItem(currentValue = bacnetEquip["dis"].toString())

                val list: List<BacnetModelDetailResponse> = buildBacnetModelSystem(bacnetEquip)

                for (item: BacnetModelDetailResponse in list) {
                    val bacnetPoints: List<BacnetPoint> = item.points
                    bacNetPointsList = fetchZoneDataForBacnet(
                        bacnetPoints,
                        bacnetEquip["bacnetConfig"].toString()
                    )
                }


                val bacnetPoints: List<BacnetZoneViewItem> = bacNetPointsList
                val points = getBacnetDetailedViewPoints(
                    Globals.getInstance().applicationContext,
                    bacnetPoints,
                    "bacnet"
                )
                initializeDetailedViewPoints(points)

                externalEquipNodeAddress = bacnetEquip["group"].toString()
                externalEquipId = bacnetEquip["id"].toString()
                updateExternalEquipHealth()
            }
        }
    }

    private fun initializeDetailedViewPoints(detailedViewItems: List<ExternalPointItem>) {
        viewModelScope.launch(Dispatchers.Default) {
            val oldPoints = detailedViewPoints.toList()
            unSubscribeDetailedViewHisPoints(oldPoints)
            subscribeDetailedViewPoints(detailedViewItems)
            withContext(Dispatchers.Main) {
                detailedViewPoints.clear()
                detailedViewPoints.addAll(detailedViewItems)
            }
        }
    }

    private fun initializeBTUViewPoints(points: List<ExternalPointItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldPoints = btuMeterPoints.toList()
            unSubscribeDetailedViewHisPoints(oldPoints)
            subscribeDetailedViewPoints(points)
            withContext(Dispatchers.Main) {
                btuMeterPoints.clear()
                btuMeterPoints.addAll(points)
            }
        }
    }

    private fun initializeEMRViewPoints(points: List<ExternalPointItem>) {
        viewModelScope.launch(Dispatchers.Default) {
            val oldPoints = emrPoints.toList()
            unSubscribeDetailedViewHisPoints(oldPoints)
            subscribeDetailedViewPoints(points)
            withContext(Dispatchers.Main) {
                emrPoints.clear()
                emrPoints.addAll(points)
            }
        }
    }

    private fun unSubscribeDetailedViewHisPoints(hisPoints: List<ExternalPointItem>) {
        hisPoints.forEach { hisPoint ->
            hisPoint.id?.let { id ->
                HisWriteObservable.unsubscribe(id, this)
            }
        }
    }

    private fun subscribeDetailedViewPoints(hisPoints: List<ExternalPointItem>) {
        hisPoints.forEach { hisPoint ->
            hisPoint.id?.let { id ->
                HisWriteObservable.subscribe(id, this)
            }
        }
    }

    private fun changeInExternalEquipPoints(pointId: String, data: Double) {
        viewModelScope.launch(Dispatchers.Main) {
            if (isBTUPaired.value) {
                val index = btuMeterPoints.indexOfFirst { it.id == pointId }
                if (index == -1) return@launch

                val point = btuMeterPoints[index]
                val value = data.toString()

                var newIndex = getIndexOf(value, point.dropdownOptions)
                newIndex = getDropDownPosition(value, newIndex, point)
                if (point.currentValue != value || point.selectedIndex != newIndex) {
                    btuMeterPoints[index] = point.copy(
                        currentValue = value,
                        selectedIndex = newIndex
                    )
                    CcuLog.d(
                        Tags.CCU_SYSTEM_SCREEN,
                        "onPointChanged - pointId: $pointId," +
                                " updatedValue: $value," +
                                " newIndex: $newIndex"
                    )
                } else {
                    CcuLog.d(
                        Tags.CCU_SYSTEM_SCREEN,
                        "point value not changed - pointId: $pointId," +
                                " currentValue: $value," +
                                " selectedIndex: $newIndex"
                    )
                }
            } else if (isEMRPaired.value) {
                val index = emrPoints.indexOfFirst { it.id == pointId }
                if (index == -1) return@launch

                val point = emrPoints[index]
                val value = data.toString()

                var newIndex = getIndexOf(value, point.dropdownOptions)
                newIndex = getDropDownPosition(value, newIndex, point)
                if (point.currentValue != value || point.selectedIndex != newIndex) {
                    emrPoints[index] = point.copy(
                        currentValue = value,
                        selectedIndex = newIndex
                    )
                    CcuLog.d(
                        Tags.CCU_SYSTEM_SCREEN,
                        "onPointChanged - pointId: $pointId," +
                                " updatedValue: $value," +
                                " newIndex: $newIndex"
                    )
                } else {
                    CcuLog.d(
                        Tags.CCU_SYSTEM_SCREEN,
                        "point value not changed - pointId: $pointId," +
                                " currentValue: $value," +
                                " selectedIndex: $newIndex"
                    )
                }
            } else {
                val index = detailedViewPoints.indexOfFirst { it.id == pointId }
                if (index == -1) return@launch

                val point = detailedViewPoints[index]
                val value = data.toString()

                var newIndex = getIndexOf(value, point.dropdownOptions)
                newIndex = getDropDownPosition(value, newIndex, point)
                if (point.currentValue != value || point.selectedIndex != newIndex) {
                    detailedViewPoints[index] = point.copy(
                        currentValue = value,
                        selectedIndex = newIndex
                    )
                    CcuLog.d(
                        Tags.CCU_SYSTEM_SCREEN,
                        "onPointChanged - pointId: $pointId," +
                                " updatedValue: $value," +
                                " newIndex: $newIndex"
                    )
                } else {
                    CcuLog.d(
                        Tags.CCU_SYSTEM_SCREEN,
                        "point value not changed - pointId: $pointId," +
                                " currentValue: $value," +
                                " selectedIndex: $newIndex"
                    )
                }
            }
        }
    }

    private fun loadBTUorEnergyMeter() {
        val btuEquip: HashMap<Any, Any> = CCUHsApi.getInstance().readEntity(
            "equip and modbus and not equipRef and btu and system"
        )
        val emrEquip: HashMap<Any, Any> = CCUHsApi.getInstance().readEntity(
            "equip and modbus and not equipRef and emr and system"
        )
        loadBTUEquip(btuEquip)
        loadEMREquip(emrEquip)
    }

    private fun loadBTUEquip(btuEquip: HashMap<Any, Any>) {
        isBTUPaired.value = btuEquip.isNotEmpty()
        if (btuEquip.isNotEmpty()) {
            val externalModbusEquip = buildModbusModelByEquipRef(
                btuEquip["id"].toString()
            )
            val nodeAddress: String = externalModbusEquip.slaveId.toString()
            val displayIndex: Int = externalModbusEquip.name.lastIndexOf('-') + 1
            val displayName: String = externalModbusEquip.name.substring(displayIndex)
            if (!externalModbusEquip.equipType.contains(displayName)) {
                btuEquipName.value =
                    HeaderViewItem(currentValue = "$displayName($nodeAddress)")
            } else {
                externalEquipName.value = HeaderViewItem(
                    currentValue = externalModbusEquip.name + "(" + externalModbusEquip.equipType.uppercase(
                        Locale.getDefault()
                    ) + nodeAddress + ")"
                )
            }
            val btuMeterPoints = getModbusDetailedViewPoints(
                externalModbusEquip,
                "modbus",
                btuEquip["id"].toString()
            )
            initializeBTUViewPoints(btuMeterPoints)
            btuNodeAddress = nodeAddress
            btuEquipId = btuEquip["id"].toString()
            updateBTUEquipHealth()
            btuHeartBeat.value = heartBeatStatus(btuNodeAddress)
            btuLastUpdated.value = HeaderViewItem(
                disName = "Last Updated: ",
                currentValue = HeartBeatUtil.getLastUpdatedTime(btuNodeAddress).toString()
            )
        }
    }

    private fun loadEMREquip(emrEquip: HashMap<Any, Any>) {
        isEMRPaired.value = emrEquip.isNotEmpty()
        if (emrEquip.isNotEmpty()) {
            val externalModbusEquip = buildModbusModelByEquipRef(
                emrEquip["id"].toString()
            )
            val nodeAddress: String = externalModbusEquip.slaveId.toString()
            val displayIndex: Int = externalModbusEquip.name.lastIndexOf('-') + 1
            val displayName: String = externalModbusEquip.name.substring(displayIndex)
            if (!externalModbusEquip.equipType.contains(displayName)) {
                emrEquipName.value =
                    HeaderViewItem(currentValue = "$displayName($nodeAddress)")
            } else {
                emrEquipName.value = HeaderViewItem(
                    currentValue = externalModbusEquip.name + "(" + externalModbusEquip.equipType.uppercase(
                        Locale.getDefault()
                    ) + nodeAddress + ")"
                )
            }
            val btuMeterPoints = getModbusDetailedViewPoints(
                externalModbusEquip,
                "modbus",
                emrEquip["id"].toString()
            )
            initializeEMRViewPoints(btuMeterPoints)
            emrNodeAddress = nodeAddress
            emrEquipId = emrEquip["id"].toString()
            updateEMREquipHealth()
            emrHeartBeat.value = heartBeatStatus(emrNodeAddress)
            emrLastUpdated.value = HeaderViewItem(
                disName = "Last Updated: ",
                currentValue = HeartBeatUtil.getLastUpdatedTime(emrNodeAddress).toString()
            )
        }
    }

    private fun showAdvancedAhuVavConfigPoints(profile: VavAdvancedAhu) {
        val userIntentConfig = profile.getUserIntentConfig()
        val isCelsiusEnabled = isCelsiusTunerAvailableStatus()
        isDualSetPointControlEnabled.value =
            (userIntentConfig.isSatHeatingAvailable || userIntentConfig.isSatCoolingAvailable)

        isSATHeatingAvailable.value = userIntentConfig.isSatHeatingAvailable
        if (isSATHeatingAvailable.value) { // heating is available
            if (isCelsiusEnabled) {
                //Heating Setpoint
                val heatingSp = UnitUtils.fahrenheitToCelsiusTwoDecimal(
                    profile.systemEquip
                        .cmEquip.airTempHeatingSp.readHisVal()
                ).toString() + " °C"
                val heatingSpPoint = profile.systemEquip.cmEquip.airTempHeatingSp

                satHeatingSetPoint.value = HeaderViewItem(
                    id = heatingSpPoint.id,
                    currentValue = heatingSp
                )
                HisWriteObservable.subscribe(satHeatingSetPoint.value.id.toString(), this)

                // SAT Current
                val supplyAirTempControlONPoint =
                    profile.systemEquip.cmEquip.supplyAirTempControlOn
                val value =
                    UnitUtils.fahrenheitToCelsiusTwoDecimal(profile.getSatControlPoint())
                        .toString() + " °C"

                dischargeAirTempSensor.value = HeaderViewItem(
                    id = supplyAirTempControlONPoint.id,
                    currentValue = value
                )
                HisWriteObservable.subscribe(dischargeAirTempSensor.value.id.toString(), this)

            } else {
                //Heating Setpoint
                val heatingSp = profile.systemEquip.cmEquip.airTempHeatingSp.readHisVal()
                    .toString() + " " + profile.getSatUnit()
                val heatingSpPoint = profile.systemEquip.cmEquip.airTempHeatingSp

                satHeatingSetPoint.value = HeaderViewItem(
                    id = heatingSpPoint.id,
                    currentValue = heatingSp
                )
                HisWriteObservable.subscribe(satHeatingSetPoint.value.id.toString(), this)

                // SAT Current
                val supplyAirTempControlONPoint =
                    profile.systemEquip.cmEquip.supplyAirTempControlOn
                val value = profile.getSatControlPoint().toString() + " " + profile.getSatUnit()

                dischargeAirTempSensor.value = HeaderViewItem(
                    id = supplyAirTempControlONPoint.id,
                    currentValue = value
                )
                HisWriteObservable.subscribe(dischargeAirTempSensor.value.id.toString(), this)
            }
        }
        isSATCoolingAvailable.value = userIntentConfig.isSatCoolingAvailable
        if (isSATCoolingAvailable.value) { // Cooling is available
            if (isCelsiusEnabled) {

                // cooling sp
                val coolingSpValue = UnitUtils.fahrenheitToCelsiusTwoDecimal(
                    profile.systemEquip
                        .cmEquip.airTempCoolingSp.readHisVal()
                ).toString() + " °C"
                val coolingSpPoint = profile.systemEquip.cmEquip.airTempCoolingSp

                satHeatingSetPoint.value = HeaderViewItem(
                    id = coolingSpPoint.id,
                    currentValue = coolingSpValue
                )
                HisWriteObservable.subscribe(satHeatingSetPoint.value.id.toString(), this)

                // SAT Current
                val supplyAirTempControlONPoint = profile.systemEquip.cmEquip.supplyAirTempControlOn
                val value =
                    UnitUtils.fahrenheitToCelsiusTwoDecimal(profile.getSatControlPoint())
                        .toString() + " °C"

                dischargeAirTempSensor.value = HeaderViewItem(
                    id = supplyAirTempControlONPoint.id,
                    currentValue = value
                )
                HisWriteObservable.subscribe(dischargeAirTempSensor.value.id.toString(), this)

            } else {
                //Heating Setpoint
                val coolingSpValue = profile.systemEquip.cmEquip.airTempCoolingSp.readHisVal()
                    .toString() + " " + profile.getSatUnit()
                val coolingSpPoint = profile.systemEquip.cmEquip.airTempCoolingSp

                satCoolingSetPoint.value = HeaderViewItem(
                    id = coolingSpPoint.id,
                    currentValue = coolingSpValue
                )
                HisWriteObservable.subscribe(satCoolingSetPoint.value.id.toString(), this)

                // SAT Current
                val supplyAirTempControlONPoint =
                    profile.systemEquip.cmEquip.supplyAirTempControlOn
                val value = profile.getSatControlPoint().toString() + " " + profile.getSatUnit()

                dischargeAirTempSensor.value = HeaderViewItem(
                    id = supplyAirTempControlONPoint.id,
                    currentValue = value
                )
                HisWriteObservable.subscribe(dischargeAirTempSensor.value.id.toString(), this)
            }
        }

        isDualSetPointControlEnabled.value =
            (userIntentConfig.isSatHeatingAvailable || userIntentConfig.isSatCoolingAvailable)
        if (isDualSetPointControlEnabled.value) {
            val operatingModePoint = profile.systemEquip.operatingMode
            val operatingModeVal = profile.getOperatingMode()
            operatingMode.value = HeaderViewItem(
                id = operatingModePoint.id,
                currentValue = operatingModeVal
            )
            HisWriteObservable.subscribe(operatingMode.value.id.toString(), this)
        }

        isPressureControlAvailable.value = userIntentConfig.isPressureControlAvailable
        isCo2DamperControlAvailable.value = userIntentConfig.isCo2DamperControlAvailable
        isDCVEnabled.value = isCo2DamperControlAvailable.value

        if (isPressureControlAvailable.value) {
            // Static Pressure control is available
            val df = DecimalFormat("0.00")
            val dspSetPointVal =
                profile.systemEquip.cmEquip.ductStaticPressureSetpoint.readHisVal()
            val dspSetPoint = profile.systemEquip.cmEquip.ductStaticPressureSetpoint
            val dspUnit = profile.getUnit("ductStaticPressureSetpoint")

            //DSP setpoint
            ductStaticPressureSetpoint.value = HeaderViewItem(
                id = dspSetPoint.id,
                currentValue = df.format(dspSetPointVal) + " " + dspUnit
            )
            HisWriteObservable.subscribe(ductStaticPressureSetpoint.value.id.toString(), this)


            val point = profile.systemEquip.cmEquip.pressureBasedFanControlOn
            val staticPressureControlValue = profile.getStaticPressureControlPoint()

            ductStaticPressureSensor.value = HeaderViewItem(
                id = point.id,
                currentValue = "%.2f %s".format(staticPressureControlValue, dspUnit)
            )
            HisWriteObservable.subscribe(ductStaticPressureSensor.value.id.toString(), this)
        }

        if (isCo2DamperControlAvailable.value) {
            val co2damperControlValue =
                profile.systemEquip.cmEquip.co2BasedDamperControl.readHisVal()
            val co2damperControl = profile.systemEquip.cmEquip.co2BasedDamperControl
            dcvDamperPos.value = HeaderViewItem(
                id = co2damperControl.id,
                currentValue = "$co2damperControlValue %"
            )
            HisWriteObservable.subscribe(dcvDamperPos.value.id.toString(), this)
        }
    }

    private fun showAdvancedAhuDabConfigPoints(profile: DabAdvancedAhu) {
        val userIntentConfig = profile.getUserIntentConfig()
        val isCelsiusEnabled = isCelsiusTunerAvailableStatus()
        isDualSetPointControlEnabled.value =
            (userIntentConfig.isSatHeatingAvailable || userIntentConfig.isSatCoolingAvailable)

        isSATHeatingAvailable.value = userIntentConfig.isSatHeatingAvailable
        if (isSATHeatingAvailable.value) { // heating is available
            if (isCelsiusEnabled) {
                //Heating Setpoint
                val heatingSp = UnitUtils.fahrenheitToCelsiusTwoDecimal(
                    profile.systemEquip
                        .cmEquip.airTempHeatingSp.readHisVal()
                ).toString() + " °C"
                val heatingSpPoint = profile.systemEquip.cmEquip.airTempHeatingSp

                satHeatingSetPoint.value = HeaderViewItem(
                    id = heatingSpPoint.id,
                    currentValue = heatingSp
                )
                HisWriteObservable.subscribe(satHeatingSetPoint.value.id.toString(), this)

                // SAT Current
                val supplyAirTempControlONPoint =
                    profile.systemEquip.cmEquip.supplyAirTempControlOn
                val value =
                    UnitUtils.fahrenheitToCelsiusTwoDecimal(profile.getSatControlPoint())
                        .toString() + " °C"

                dischargeAirTempSensor.value = HeaderViewItem(
                    id = supplyAirTempControlONPoint.id,
                    currentValue = value
                )
                HisWriteObservable.subscribe(dischargeAirTempSensor.value.id.toString(), this)

            } else {
                //Heating Setpoint
                val heatingSp = profile.systemEquip.cmEquip.airTempHeatingSp.readHisVal()
                    .toString() + " " + profile.getSatUnit()
                val heatingSpPoint = profile.systemEquip.cmEquip.airTempHeatingSp

                satHeatingSetPoint.value = HeaderViewItem(
                    id = heatingSpPoint.id,
                    currentValue = heatingSp
                )
                HisWriteObservable.subscribe(satHeatingSetPoint.value.id.toString(), this)

                // SAT Current
                val supplyAirTempControlONPoint =
                    profile.systemEquip.cmEquip.supplyAirTempControlOn
                val value = profile.getSatControlPoint().toString() + " " + profile.getSatUnit()

                dischargeAirTempSensor.value = HeaderViewItem(
                    id = supplyAirTempControlONPoint.id,
                    currentValue = value
                )
                HisWriteObservable.subscribe(dischargeAirTempSensor.value.id.toString(), this)
            }
        }
        isSATCoolingAvailable.value = userIntentConfig.isSatCoolingAvailable
        if (isSATCoolingAvailable.value) { // Cooling is available
            if (isCelsiusEnabled) {

                // cooling sp
                val coolingSpValue = UnitUtils.fahrenheitToCelsiusTwoDecimal(
                    profile.systemEquip
                        .cmEquip.airTempCoolingSp.readHisVal()
                ).toString() + " °C"
                val coolingSpPoint = profile.systemEquip.cmEquip.airTempCoolingSp

                satHeatingSetPoint.value = HeaderViewItem(
                    id = coolingSpPoint.id,
                    currentValue = coolingSpValue
                )
                HisWriteObservable.subscribe(satHeatingSetPoint.value.id.toString(), this)

                // SAT Current
                val supplyAirTempControlONPoint = profile.systemEquip.cmEquip.supplyAirTempControlOn
                val value =
                    UnitUtils.fahrenheitToCelsiusTwoDecimal(profile.getSatControlPoint())
                        .toString() + " °C"

                dischargeAirTempSensor.value = HeaderViewItem(
                    id = supplyAirTempControlONPoint.id,
                    currentValue = value
                )
                HisWriteObservable.subscribe(dischargeAirTempSensor.value.id.toString(), this)

            } else {
                //Heating Setpoint
                val coolingSpValue = profile.systemEquip.cmEquip.airTempCoolingSp.readHisVal()
                    .toString() + " " + profile.getSatUnit()
                val coolingSpPoint = profile.systemEquip.cmEquip.airTempCoolingSp

                satCoolingSetPoint.value = HeaderViewItem(
                    id = coolingSpPoint.id,
                    currentValue = coolingSpValue
                )
                HisWriteObservable.subscribe(satCoolingSetPoint.value.id.toString(), this)

                // SAT Current
                val supplyAirTempControlONPoint =
                    profile.systemEquip.cmEquip.supplyAirTempControlOn
                val value = profile.getSatControlPoint().toString() + " " + profile.getSatUnit()

                dischargeAirTempSensor.value = HeaderViewItem(
                    id = supplyAirTempControlONPoint.id,
                    currentValue = value
                )
                HisWriteObservable.subscribe(dischargeAirTempSensor.value.id.toString(), this)
            }
        }

        isDualSetPointControlEnabled.value =
            (userIntentConfig.isSatHeatingAvailable || userIntentConfig.isSatCoolingAvailable)
        if (isDualSetPointControlEnabled.value) {
            val operatingModePoint = profile.systemEquip.operatingMode
            val operatingModeVal = profile.getOperatingMode()
            operatingMode.value = HeaderViewItem(
                id = operatingModePoint.id,
                currentValue = operatingModeVal
            )
            HisWriteObservable.subscribe(operatingMode.value.id.toString(), this)
        }

        isPressureControlAvailable.value = userIntentConfig.isPressureControlAvailable
        isCo2DamperControlAvailable.value = userIntentConfig.isCo2DamperControlAvailable
        isDCVEnabled.value = isCo2DamperControlAvailable.value

        if (isPressureControlAvailable.value) {
            // Static Pressure control is available
            val df = DecimalFormat("0.00")
            val dspSetPointVal =
                profile.systemEquip.cmEquip.ductStaticPressureSetpoint.readHisVal()
            val dspSetPoint = profile.systemEquip.cmEquip.ductStaticPressureSetpoint
            val dspUnit = profile.getUnit("ductStaticPressureSetpoint")

            //DSP setpoint
            ductStaticPressureSetpoint.value = HeaderViewItem(
                id = dspSetPoint.id,
                currentValue = df.format(dspSetPointVal) + " " + dspUnit
            )
            HisWriteObservable.subscribe(ductStaticPressureSetpoint.value.id.toString(), this)


            val point = profile.systemEquip.cmEquip.pressureBasedFanControlOn
            val staticPressureControlValue = profile.getStaticPressureControlPoint()

            ductStaticPressureSensor.value = HeaderViewItem(
                id = point.id,
                currentValue = "%.2f %s".format(staticPressureControlValue, dspUnit)
            )
            HisWriteObservable.subscribe(ductStaticPressureSensor.value.id.toString(), this)
        }

        if (isCo2DamperControlAvailable.value) {
            val co2damperControlValue =
                profile.systemEquip.cmEquip.co2BasedDamperControl.readHisVal()
            val co2damperControl = profile.systemEquip.cmEquip.co2BasedDamperControl
            dcvDamperPos.value = HeaderViewItem(
                id = co2damperControl.id,
                currentValue = "$co2damperControlValue %"
            )
            HisWriteObservable.subscribe(dcvDamperPos.value.id.toString(), this)
        }
    }
}
