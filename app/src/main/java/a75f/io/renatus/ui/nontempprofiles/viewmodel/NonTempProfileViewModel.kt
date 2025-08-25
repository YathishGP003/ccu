package a75f.io.renatus.ui.nontempprofiles.viewmodel

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Equip
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.observer.HisWriteObservable
import a75f.io.api.haystack.observer.PointSubscriber
import a75f.io.api.haystack.observer.PointWriteObservable
import a75f.io.device.mesh.hyperstat.getHyperStatDevice
import a75f.io.device.mesh.hyperstat.getHyperStatDomainDevice
import a75f.io.domain.api.Domain.getDomainEquip
import a75f.io.domain.equips.PlcEquip
import a75f.io.domain.equips.hyperstat.MonitoringEquip
import a75f.io.domain.util.ModelLoader.getModelForDomainName
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.Thermistor
import a75f.io.logic.bo.building.plc.PlcProfile
import a75f.io.logic.bo.building.sensors.SensorManager
import a75f.io.logic.bo.util.UnitUtils
import a75f.io.renatus.ui.model.HeaderViewItem
import a75f.io.renatus.ui.nontempprofiles.model.ExternalPointItem
import a75f.io.renatus.ui.nontempprofiles.utilities.getIndexOf
import a75f.io.renatus.ui.nontempprofiles.utilities.getLastUpdatedViewItem
import a75f.io.renatus.ui.nontempprofiles.utilities.heartBeatStatus
import a75f.io.renatus.util.HeartBeatUtil
import a75f.io.renatus.util.HeartBeatUtil.isModuleAlive
import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfileDirective
import io.seventyfivef.domainmodeler.client.type.SeventyFiveFProfilePointDef
import io.seventyfivef.domainmodeler.common.point.MultiStateConstraint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.StringUtils
import org.projecthaystack.HStr
import java.util.Objects
import java.util.function.Function


class NonTempProfileViewModel : ViewModel(), PointSubscriber {
    var profile = ""
    var equipId = ""
    var equipName = ""
    var externalEquipHeartBeat = false
    var lastUpdated = mutableStateOf(HeaderViewItem())

    var equipStatusPoint = mutableStateOf(HeaderViewItem())

    // below list used for equips like modbus, connectModule, bacnet, etc.
    var detailedViewPoints = mutableStateListOf<ExternalPointItem>()

    // below list used for PLC
    var headerViewPoints = mutableStateListOf<HeaderViewItem>()

    // handler for periodic equip health updates
    var handler = Handler(Looper.getMainLooper())
    var runnable: Runnable? = null

    fun setEquipStatusPoint(item: HeaderViewItem) {
        equipStatusPoint.value = item
        PointWriteObservable.subscribe(equipStatusPoint.value.id.toString(), this)
    }

    private fun setLastUpdatedPoint(item: HeaderViewItem) {
        lastUpdated.value = item
    }

    fun initializeDetailedViewPoints(detailedViewItems: List<ExternalPointItem>) {
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

    fun initializeHeaderViewPoints(headerViewItems: List<HeaderViewItem>) {
        viewModelScope.launch(Dispatchers.Default) {
            val oldPoints = headerViewPoints.toList()
            unSubscribeHeaderViewHisPoints(oldPoints)
            subscribeHeaderViewPoints(headerViewItems)
            withContext(Dispatchers.Main) {
                headerViewPoints.clear()
                headerViewPoints.addAll(headerViewItems)
            }
        }
    }


    private fun subscribeHeaderViewPoints(hisPoints: List<HeaderViewItem>) {
        hisPoints.forEach { hisPoint ->
            hisPoint.id?.let { id ->
                CcuLog.d(
                    Tags.CCU_ZONE_SCREEN,
                    " subscribing - pointDis: ${hisPoint.disName}, pointId: $id" )
                HisWriteObservable.subscribe(id, this)
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

    private fun unSubscribeDetailedViewHisPoints(hisPoints: List<ExternalPointItem>) {
        hisPoints.forEach { hisPoint ->
            hisPoint.id?.let { id ->
                HisWriteObservable.unsubscribe(id, this)
            }
        }
    }

    private fun unSubscribeHeaderViewHisPoints(hisPoints: List<HeaderViewItem>) {
        hisPoints.forEach { hisPoint ->
            hisPoint.id?.let { id ->
                HisWriteObservable.unsubscribe(id, this)
                CcuLog.d(
                    Tags.CCU_ZONE_SCREEN,
                    " unsubscribing - pointDis: ${hisPoint.disName}, pointId: $id"
                )
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        unSubscribeDetailedViewHisPoints(detailedViewPoints)
        unSubscribeHeaderViewHisPoints(headerViewPoints)
    }

    fun cleanUp() {
        unSubscribeDetailedViewHisPoints(detailedViewPoints)
        unSubscribeHeaderViewHisPoints(headerViewPoints)
        stopObservingEquipHealth()
    }

    override fun onWritablePointChanged(pointId: String, value: Any) {
        if (value is HStr) {
            if (equipStatusPoint.value.id == pointId) {
                changeInEquipStatusPoint(value.toString())
            } else if (profile == "PLC") {
                loadPLCPoint(pointId)
            } else {
                CcuLog.d(Tags.CCU_ZONE_SCREEN, "pointWrite: no matching ids: $pointId")
            }
        } else {
            CcuLog.d(
                Tags.CCU_ZONE_SCREEN,
                "pointWrite: non-string value changed, pointId: $pointId value is: $value"
            )
        }
    }

    override fun onHisPointChanged(pointId: String, value: Double) {
        detailedViewPoints.find { it.id == pointId }?.let {
            changeInDetailedView(pointId, value)
            return
        }
        when (profile) {
            "PLC" -> loadPLCPoint(pointId)
            "EMR" -> loadEmrPoints(equipId)
            "MONITORING" -> loadHyperStatMonitoringEquipPoints(equipId)
        }
    }


    fun initializeEquipHealth(
        eName: String,
        showStatus: Boolean, deviceId: String
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            equipName = eName
            if (showStatus) {
                withContext(Dispatchers.IO) {
                    setupEquipHealth(deviceId)
                }
            }
        }
    }

    fun observeEquipHealthByGroupId(
        groupId: String
    ) {
        runnable = object : Runnable {
            override fun run() {
                val lastUpdatedTime = HeartBeatUtil.getLastUpdatedTime(groupId)
                setLastUpdatedPoint(
                    HeaderViewItem(
                        id = groupId,
                        disName = "Last Updated: ",
                        currentValue = lastUpdatedTime,
                        usesDropdown = false
                    )
                )
                externalEquipHeartBeat = isModuleAlive(groupId)
                handler.postDelayed(this, 60_000L)
            }
        }
        handler.post(runnable!!)
    }

    fun observeExternalEquipHealth(
        deviceId: String
    ) {
        runnable = object : Runnable {
            override fun run() {
                setupEquipHealth(deviceId)
                handler.postDelayed(this, 60000)
            }
        }
        handler.post(runnable!!)
    }

    fun setupEquipHealth(deviceId: String) {
        this.setLastUpdatedPoint(getLastUpdatedViewItem(deviceId, this))
        this.externalEquipHeartBeat = heartBeatStatus(deviceId)
    }

    fun stopObservingEquipHealth() {
        runnable?.let {
            handler.removeCallbacks(it)
        }
        runnable = null
    }

    @SuppressLint("DefaultLocale")
    fun getPlcUiItems(
        equipID: String
    ): List<HeaderViewItem> {
        CcuLog.d("kumar_debug", "calling plc header points")
        val headerViewItems = mutableListOf<HeaderViewItem>()
        val plcEquip = getDomainEquip(equipID) as PlcEquip?
        val plcPoints = HashMap<String, Any>()
        val equip = CCUHsApi.getInstance().readMapById(equipID)


        if (plcEquip == null) {
            CcuLog.d(L.TAG_CCU_UI, "getPiEquipPoints: plcEquip is null")
            return headerViewItems
        }

        val inputValue: java.util.ArrayList<*>? = CCUHsApi.getInstance()
            .readAllEntities("point and process and logical and variable and equipRef == \"$equipID\"")
        val piSensorValue = plcEquip.analog1InputType.readDefaultVal()
        val piA2SensorValue = plcEquip.analog2InputType.readDefaultVal()
        val analog2Config = plcEquip.useAnalogIn2ForSetpoint.readDefaultVal()
        val th1InputSensor = plcEquip.thermistor1InputType.readDefaultVal().toInt()
        var targetValue = if (analog2Config > 0) 0.0 else plcEquip.pidTargetValue.readDefaultVal()
        val offsetValue = plcEquip.setpointSensorOffset.readDefaultVal()
        val loopOutput = plcEquip.controlVariable.readHisVal()

        // equip status
        val status = plcEquip.equipStatusMessage.readDefaultStrVal()
        if (status.isNotEmpty()) {
            val headerStatusView = HeaderViewItem(
                id = plcEquip.equipStatusMessage.id,
                disName = "Status: ",
                currentValue = status,
                usesDropdown = false
            )
            headerViewItems.add(headerStatusView)
            setEquipStatusPoint(headerStatusView)
        } else {
            plcPoints["Status"] = "OFF"
            val headerStatusView = HeaderViewItem(
                id = plcEquip.equipStatusMessage.id,
                disName = "Status: ",
                currentValue = "OFF",
                usesDropdown = false
            )
            headerViewItems.add(headerStatusView)
            setEquipStatusPoint(headerStatusView)
        }

        // loopOutput
        plcPoints["LoopOutput"] = loopOutput
        val headerLoopOutputView = HeaderViewItem(
            id = plcEquip.controlVariable.id,
            disName = "Loop Output: ",
            currentValue = "$loopOutput %",
            usesDropdown = false
        )
        headerViewItems.add(headerLoopOutputView)


        // input value
        var inputValueId: String? = null
        if (inputValue != null && inputValue.size > 0) {
            inputValueId = (inputValue[0] as HashMap<*, *>)["id"].toString()
            val inputVal = CCUHsApi.getInstance().readHisValById(inputValueId)
            plcPoints["Input Value"] = inputVal
        }


        val profile = L.getProfile(equip["group"].toString().toShort()) as PlcProfile
        val processVariable = profile.processVariableDomainName
        val dynamicTargetValue = profile.dynamicTargetDomainName
        val inputDetails = CCUHsApi.getInstance().readEntity(
            "point and domainName == \"$processVariable\" and equipRef == \"$equipID\""
        )

        plcPoints["Unit Type"] =
            StringUtils.substringAfterLast(inputDetails["dis"].toString(), "-")
        val inputUnit = if (inputDetails["unit"] == null) "" else inputDetails["unit"].toString()
        plcPoints["Unit"] = inputUnit

        val processValue = plcPoints["Input Value"] as Double
        var inputVal: String?
        inputVal = if (plcPoints["Unit Type"] == "Generic Alarm NO"
            || plcPoints["Unit Type"] == "Generic Alarm NC"
        ) {
            if (processValue == 0.0) {
                "Normal" + plcPoints["Unit"].toString()
            } else {
                "Alarm" + plcPoints["Unit"].toString()
            }
        } else {
            String.format("%.2f",
                processValue
            ) + " " + plcPoints["Unit"].toString()
        }
        if (plcPoints["Unit"] == "\u00B0F") {
            if (UnitUtils.isCelsiusTunerAvailableStatus()) {
                inputVal =    String.format(
                    "%.2f",
                    UnitUtils.fahrenheitToCelsius(processValue)
                ) + " " + " \u00B0C"
            }
        }

        val headerInputView = HeaderViewItem(
            id = inputValueId,
            disName = "Input Value ("+plcPoints["Unit Type"]+"): ",
            currentValue = inputVal.toString(),
            usesDropdown = false
        )
        headerViewItems.add(headerInputView)

        // target value
        val targetDetails: Map<Any, Any?>
        if (analog2Config == 1.0) {
            plcPoints["Dynamic Setpoint"] = true
            targetDetails = CCUHsApi.getInstance().readEntity(
                "point and domainName == \"$dynamicTargetValue\" and equipRef == \"$equipID\""
            )
            targetValue = plcEquip.dynamicTargetValue.readDefaultVal()
        } else {
            targetDetails = plcEquip.pidTargetValue.getPoint()
            if (analog2Config == 0.0) plcPoints["Dynamic Setpoint"] = false
        }

        plcPoints["Target Value"] = targetValue

        CcuLog.d(L.TAG_CCU_UI, "inputDetails = $inputDetails targetDetails = $targetDetails")

        plcPoints["Unit"] = inputUnit
        plcPoints["Dynamic Unit Type"] =
            StringUtils.substringAfterLast(targetDetails["dis"].toString(), "-")

        val targetUnit = if (analog2Config == 1.0)
            if (targetDetails["unit"] == null) "" else targetDetails["unit"].toString()
        else
            inputUnit

        plcPoints["Dynamic Unit"] = targetUnit

        if (th1InputSensor == 1 || th1InputSensor == 2) {
            plcPoints["Unit Type"] = "Temperature"
            plcPoints["Unit"] = "\u00B0F"
        }

        val nativeInputSensor = plcEquip.nativeSensorType.readDefaultVal().toInt()
        if (nativeInputSensor > 0) {
            val selectedSensor = SensorManager.getInstance().nativeSensorList[nativeInputSensor - 1]
            plcPoints["Unit Type"] = selectedSensor.sensorName
            plcPoints["Unit"] = selectedSensor.engineeringUnit
        }

        plcPoints["Offset Value"] = offsetValue

        if (piSensorValue > 0) {
            plcPoints["Pi Sensor Value"] = piSensorValue
        }

        var sensorId: String? = null
        if (analog2Config > 0) {
            val model =
                getModelForDomainName(equip["domainName"].toString()) as SeventyFiveFProfileDirective

            val domainName = model.points.stream()
                .filter { point: SeventyFiveFProfilePointDef -> point.domainName == "analog2InputType" }
                .map { point: SeventyFiveFProfilePointDef -> point.valueConstraint as MultiStateConstraint }
                .findFirst()
                .map(Function { constraint: MultiStateConstraint -> constraint.allowedValues[piA2SensorValue.toInt()].value })
                .orElse(null)

            val sensorPoint = CCUHsApi.getInstance().readEntity(
                "domainName == \"$domainName\""
            )
            sensorId = sensorPoint["id"].toString()
            val a2SensorValue = CCUHsApi.getInstance().readHisValById(sensorId)
            val unit = Objects.requireNonNull(sensorPoint["unit"]).toString()

            plcPoints["ai2Sensor"] = a2SensorValue
            plcPoints["ai2SensorUnit"] = unit

        }


        try {
            if (plcPoints["Dynamic Setpoint"] as Boolean) {
                val a2SensorVal = getUnitString(
                    plcPoints["ai2Sensor"] as Double,
                    plcPoints["ai2SensorUnit"].toString()
                )

                val a2Sensor = HeaderViewItem(
                    id = sensorId,
                    disName = "Target (" + plcPoints["Dynamic Unit Type"].toString() + ") : ",
                    currentValue = a2SensorVal
                )
                headerViewItems.add(a2Sensor)


                val targetVal = getUnitString(
                    plcPoints["Target Value"] as Double,
                    plcPoints["Dynamic Unit"].toString()
                )

                val dynamicTarget = HeaderViewItem(
                    id = plcEquip.dynamicTargetValue.id,
                    disName = "Dynamic Target (" + plcPoints["Dynamic Unit Type"].toString() + ") : ",
                    currentValue = targetVal
                )

                headerViewItems.add(dynamicTarget)

                val offset = HeaderViewItem(
                    id = plcEquip.setpointSensorOffset.id,
                    disName = "Offset (" + plcPoints["Dynamic Unit Type"].toString() + ") : ",
                    currentValue = plcPoints["Offset Value"].toString() + " " + plcPoints["Dynamic Unit"].toString(),
                    usesDropdown = false
                )

                headerViewItems.add(offset)

            } else {
                val targetVal = getUnitString(
                    plcPoints["Target Value"] as Double,
                    plcPoints["Dynamic Unit"].toString()
                )

                val target = HeaderViewItem(
                    id = plcEquip.pidTargetValue.id,
                    disName = plcPoints["Dynamic Unit Type"].toString() +" :",
                    currentValue = targetVal,
                    usesDropdown = false
                )

                headerViewItems.add(target)

            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return headerViewItems
    }

    private fun getUnitString(pointValue: Double, unit: String): String {
        return if (UnitUtils.isCelsiusTunerAvailableStatus() && unit == "\u00B0F") {
            "%.2f \u00B0C".format(UnitUtils.fahrenheitToCelsius(pointValue))
        } else {
            "%.2f%s".format(pointValue, unit)
        }
    }

    private fun changeInDetailedView(pointId: String, data: Double) {
        viewModelScope.launch(Dispatchers.Main) {
            val index = detailedViewPoints.indexOfFirst { it.id == pointId }
            if (index == -1) return@launch  // no matches

            val point = detailedViewPoints[index]
            val value = data.toString()

            val newIndex = getIndexOf(value, point.dropdownOptions)

            if (point.currentValue != value || point.selectedIndex != newIndex) {
                detailedViewPoints[index] = point.copy(
                    currentValue = value,
                    selectedIndex = newIndex
                )
                CcuLog.d(
                    Tags.CCU_ZONE_SCREEN,
                    "onPointChanged - pointId: $pointId," +
                            " updatedValue: $value," +
                            " newIndex: $newIndex"
                )
            } else {
                CcuLog.d(
                    Tags.CCU_ZONE_SCREEN,
                    "point value not changed - pointId: $pointId," +
                            " currentValue: $value," +
                            " selectedIndex: $newIndex"
                )
            }
        }
    }

    private fun loadPLCPoint(pointId: String) {
        viewModelScope.launch(Dispatchers.Main) {
            if (headerViewPoints.toList().find { it.id == pointId } != null) {
                val list = getPlcUiItems(equipId)
                initializeHeaderViewPoints(list)
            }
        }
    }

    private fun changeInEquipStatusPoint(value: String) {
        viewModelScope.launch(Dispatchers.Main) {
            equipStatusPoint.value = equipStatusPoint.value.copy(
                currentValue = value.trimEnd()
            )
        }
    }


    fun loadEmrPoints(equipID: String) {
        val headerViewItems = mutableListOf<HeaderViewItem>()

        //val equipStatusPoint = CCUHsApi.getInstance()
        //    .readAllEntities("point and status and message and equipRef == \"$equipID\"")
        val currentRate =
            CCUHsApi.getInstance()
                .readAllEntities("point and emr and rate and equipRef == \"$equipID\"")


        val energyReadingPoint = CCUHsApi.getInstance()
            .readEntity("point and emr and sensor and sp and equipRef == \"$equipID\"")

        val energyReading = CCUHsApi.getInstance()
            .readHisValById(energyReadingPoint["id"].toString())

        var currentRateVal = 0.0
        if (currentRate != null && currentRate.size > 0) {
            val id = (currentRate[0] as HashMap)["id"].toString()
            val currentRateHis = CCUHsApi.getInstance().curRead(id)
            if (currentRateHis != null) {
                currentRateVal = currentRateHis.getVal()
            }
        }


        val statusHeaderView = HeaderViewItem(
            id = "@tempId",
            disName = "Status: ",
            currentValue = "Total Energy Consumed %.2f kWh Current Rate: %.2f kW"
                .format(energyReading, currentRateVal),
            usesDropdown = false
        )

        headerViewItems.add(statusHeaderView)
        initializeHeaderViewPoints(headerViewItems)
        HisWriteObservable.unsubscribe(currentRate[0]["id"].toString(), this)
        HisWriteObservable.unsubscribe(energyReadingPoint["id"].toString(), this)
        HisWriteObservable.subscribe(currentRate[0]["id"].toString(), this)
        HisWriteObservable.subscribe(energyReadingPoint["id"].toString(), this)
    }


    fun monitorTemp(
        equip: Equip
    ): Double {
        val monitoringEquip = getDomainEquip(equip.id) as MonitoringEquip
        return monitoringEquip.currentTemp.readHisVal()
    }


    fun loadHyperStatMonitoringEquipPoints(
        equipId: String
    ){
        val equip = Equip.Builder()
            .setHashMap(CCUHsApi.getInstance().readMapById(equipId)).build()
        val hayStack = CCUHsApi.getInstance()
        val monitoringEquip = getDomainEquip(equip.id) as MonitoringEquip
        val hyperStatDeviceMap = getHyperStatDevice(equip.group.toInt())
        val hyperStatDevice =  getHyperStatDomainDevice(hyperStatDeviceMap!![Tags.ID].toString(), equip.id)
        val headerViewItems = mutableListOf<HeaderViewItem>()

        val monitoringPoints: HashMap<Any, Any> = HashMap()
        monitoringPoints["Profile"] = "MONITORING"

        val currentTemp = monitoringEquip.currentTemp.readHisVal()
        val tempOffset = monitoringEquip.tempOffset.readHisVal()

        val analogIn1Association = monitoringEquip.analogIn1Association.readDefaultVal()
        val analogIn2Association = monitoringEquip.analogIn2Association.readDefaultVal()
        val thermistor1Association = monitoringEquip.thermistor1Association.readDefaultVal()
        val thermistor2Association = monitoringEquip.thermistor2Association.readDefaultVal()

        val isAnalog1Enable = monitoringEquip.analogIn1Enabled.readDefaultVal() > 0
        val isAnalog2Enable = monitoringEquip.analogIn2Enabled.readDefaultVal() > 0
        val isTh1Enable = monitoringEquip.thermistor1Enabled.readDefaultVal() > 0
        val isTh2Enable = monitoringEquip.thermistor2Enabled.readDefaultVal() > 0

        fun getHistoricalValue(pointRef: String?) = pointRef?.let { hayStack.readHisValById(it) } ?: 0.0

        val an1Val = getHistoricalValue(hyperStatDevice.analog1In.readPoint().pointRef)
        val an2Val = getHistoricalValue(hyperStatDevice.analog2In.readPoint().pointRef)
        val th1Val = getHistoricalValue(hyperStatDevice.th1In.readPoint().pointRef)
        val th2Val = getHistoricalValue(hyperStatDevice.th2In.readPoint().pointRef)

        var size = 0

        monitoringPoints["curtempwithoffset"] = currentTemp

        if (tempOffset != 0.0) {
            monitoringPoints["TemperatureOffset"] = tempOffset
        } else {
            monitoringPoints["TemperatureOffset"] = 0
        }

        if (isAnalog1Enable) {
            size++
            monitoringPoints["iAn1Enable"] = "true"
        } else monitoringPoints["iAn1Enable"] = "false"

        if (isAnalog2Enable) {
            size++
            monitoringPoints["iAn2Enable"] = "true"
        } else monitoringPoints["iAn2Enable"] = "false"

        if (isTh1Enable) {
            size++
            monitoringPoints["isTh1Enable"] = "true"
        } else monitoringPoints["isTh1Enable"] = "false"

        if (isTh2Enable) {
            size++
            monitoringPoints["isTh2Enable"] = "true"
        } else monitoringPoints["isTh2Enable"] = "false"

        monitoringPoints["size"] = size
        if (analogIn1Association >= 0) {
            val selectedSensor = SensorManager.getInstance().externalSensorList[analogIn1Association.toInt()]
            monitoringPoints["Analog1"] = selectedSensor.sensorName
            monitoringPoints["Unit1"] = selectedSensor.engineeringUnit ?: ""
            monitoringPoints["An1Val"] = an1Val
        }

        if (analogIn2Association >= 0) {
            val selectedSensor = SensorManager.getInstance().externalSensorList[analogIn2Association.toInt()]
            monitoringPoints["Analog2"] = selectedSensor.sensorName
            monitoringPoints["Unit2"] = selectedSensor.engineeringUnit ?: ""
            monitoringPoints["An2Val"] = an2Val
        }

        if (thermistor1Association >= 0) {
            val selectedSensor = Thermistor.getThermistorList()[thermistor1Association.toInt()]
            monitoringPoints["Thermistor1"] = selectedSensor.sensorName
            monitoringPoints["Unit3"] = selectedSensor.engineeringUnit ?: ""
            monitoringPoints["Th1Val"] = th1Val
        }
        if (thermistor2Association >= 0) {
            val selectedSensor = Thermistor.getThermistorList()[thermistor2Association.toInt()]
            monitoringPoints["Thermistor2"] = selectedSensor.sensorName
            monitoringPoints["Unit4"] = selectedSensor.engineeringUnit ?: ""
            monitoringPoints["Th2Val"] = th2Val
        }


        if (monitoringPoints["isTh1Enable"] == "true") {
            if (UnitUtils.isCelsiusTunerAvailableStatus() &&
                (monitoringPoints["Thermistor1"].toString() != "Generic Fault (NC)")
                && (monitoringPoints["Thermistor1"].toString() != "Generic Fault (NO)")) {
                val label = monitoringPoints["Thermistor1"].toString() +
                        " ( Th1 ) " + " : "
                val value = (UnitUtils.fahrenheitToCelsiusTwoDecimal(
                    monitoringPoints["Th1Val"].toString().toDouble()
                ).toString()) + " " + (" \u00B0C")
                headerViewItems.add(
                    HeaderViewItem(
                        id = hyperStatDevice.th1In.readPoint().pointRef,
                        disName = label,
                        currentValue = value,
                        usesDropdown = false
                    )
                )

            } else if (monitoringPoints["Thermistor1"].toString() == "Generic Fault (NC)"
                || monitoringPoints["Thermistor1"].toString() == "Generic Fault (NO)") {
                val label = monitoringPoints["Thermistor1"].toString() +
                        " ( Th1 ) " + " : "
                val value =
                    (if (monitoringPoints["Th1Val"] as Double > 0) "Fault" else "Normal")

                headerViewItems.add(
                    HeaderViewItem(
                        id = hyperStatDevice.th1In.readPoint().pointRef,
                        disName = label,
                        currentValue = value,
                        usesDropdown = false
                    )
                )
            } else {
                val label = monitoringPoints["Thermistor1"].toString() +
                        " ( Th1 ) " + " : "
                val unit3 =
                    if (monitoringPoints["Unit3"] != null) monitoringPoints["Unit3"].toString() else ""
                val value = (monitoringPoints["Th1Val"].toString()) + " " + unit3

                headerViewItems.add(
                    HeaderViewItem(
                        id = hyperStatDevice.th1In.readPoint().pointRef,
                        disName = label,
                        currentValue = value,
                        usesDropdown = false
                    )
                )
            }
        }

        if (monitoringPoints["isTh2Enable"] == "true") {

            if (UnitUtils.isCelsiusTunerAvailableStatus()
                && (monitoringPoints["Thermistor2"].toString() != "Generic Fault (NC)")
                && (monitoringPoints["Thermistor2"].toString() != "Generic Fault (NO)")) {
                val label = monitoringPoints["Thermistor2"].toString() +
                        " ( Th2 ) " + " : "
                val value = (UnitUtils.fahrenheitToCelsiusTwoDecimal(
                    monitoringPoints["Th2Val"].toString().toDouble()
                ).toString()) + " " + (" \u00B0C")

                headerViewItems.add(
                    HeaderViewItem(
                        id = hyperStatDevice.th2In.readPoint().pointRef,
                        disName = label,
                        currentValue = value,
                        usesDropdown = false
                    )
                )

            } else if (monitoringPoints["Thermistor2"].toString() == "Generic Fault (NC)"
                || monitoringPoints["Thermistor2"].toString() == "Generic Fault (NO)") {
                val label = monitoringPoints["Thermistor2"].toString() +
                        " ( Th2 ) " + " : "
                val value =
                    (if (monitoringPoints["Th2Val"] as Double > 0) "Fault" else "Normal")

                headerViewItems.add(
                    HeaderViewItem(
                        id = hyperStatDevice.th2In.readPoint().pointRef,
                        disName = label,
                        currentValue = value,
                        usesDropdown = false
                    )
                )
            } else {
                val label = monitoringPoints["Thermistor2"].toString() +
                        " ( Th2 ) " + " : "
                val value =
                    (monitoringPoints["Th2Val"].toString()) + " " + (monitoringPoints["Unit4"].toString())

                headerViewItems.add(
                    HeaderViewItem(
                        id = hyperStatDevice.th2In.readPoint().pointRef,
                        disName = label,
                        currentValue = value,
                        usesDropdown = false
                    )
                )
            }
        }

        if (monitoringPoints["iAn1Enable"] == "true") {

            val label = monitoringPoints["Analog1"].toString() +
                    (monitoringPoints["Unit1"].toString()) +
                    " ( A-in1 )" + " : "
            val value =
                (monitoringPoints["An1Val"].toString()) + " " + (monitoringPoints["Unit1"].toString())

            headerViewItems.add(
                HeaderViewItem(
                    id = hyperStatDevice.analog1In.readPoint().pointRef,
                    disName = label,
                    currentValue = value,
                    usesDropdown = false
                )
            )
        }


        if (monitoringPoints["iAn2Enable"] == "true") {


            val label = monitoringPoints["Analog2"].toString() +
                    (monitoringPoints["Unit2"].toString()) +
                    " ( A-in2 ) " + " : "
            val value =
                (monitoringPoints["An2Val"].toString()) + " " + (monitoringPoints["Unit2"].toString())

            headerViewItems.add(
                HeaderViewItem(
                    id = hyperStatDevice.analog2In.readPoint().pointRef,
                    disName = label,
                    currentValue = value,
                    usesDropdown = false
                )
            )
        }

        initializeHeaderViewPoints(headerViewItems)
    }

}