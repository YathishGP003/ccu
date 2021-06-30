package a75f.io.device.daikin

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.vav.VavIERtu
import io.reactivex.rxjava3.schedulers.Schedulers

class IEDeviceHandler {

    internal enum class OccMode {
        Occ, Unocc, TntOvrd, Auto, UnInit
    }

    internal enum class NetApplicMode {
        Null, Off, HeatOnly, CoolOnly, FanOnly, Auto, Invalid, UnInit
    }

    internal enum class HumidityCtrl {
        None, RelHum, DewPt, Always, UnInit
    }

    private var currentCondMode = NetApplicMode.UnInit

    private var staticPressureSp : Double = -1.0;
    private var fanLoopSp : Double = -1.0;

    private var humCtrl : HumidityCtrl = HumidityCtrl.UnInit

    private var serviceBaseUrl : String? = null
    private var ieService : IEService? = null

    private var occFetchCounter = 0

    companion object {
        @JvmStatic
        val instance: IEDeviceHandler by lazy {
            IEDeviceHandler()
        }
    }

    fun sendControl(hayStack: CCUHsApi) {

        CcuLog.e(L.TAG_CCU_DEVICE, "IEDeviceHandler : sendControl")
        val ieEquipUrl : String? = getIEUrl(hayStack)
        if (ieEquipUrl.isNullOrEmpty()) {
            CcuLog.e(L.TAG_CCU_DEVICE, "Invalid IE equip URL $ieEquipUrl")
            return
        }
        //Recreate the retrofit service when IE url has been updated.
        if (ieService == null || serviceBaseUrl != ieEquipUrl) {
            ieService = IEServiceGenerator.instance.createService("http://$ieEquipUrl:8080")
        }

        ieService?.let {
            val systemProfile = L.ccu().systemProfile as VavIERtu
            updateOccMode(it, systemProfile
            )
            if (systemProfile.getConfigEnabled(Tags.FAN) > 0) {
                updateFanControl(it, hayStack, systemProfile)
            }

            updateConditioningMode(it, hayStack)
            updateDatClgSetpoint(it, systemProfile)
            updateFanControl(it, hayStack, systemProfile)
            updateHumidityControl(it, systemProfile)
            fetchAlarms(it, hayStack)
            fetchSystemClock(it, hayStack)

            //OccStatus to be fetched every 5 minutes
            if (occFetchCounter++ % 5 == 0) {
                fetchOccStatus(it, hayStack)
                occFetchCounter = 0
            }

        }
    }

    private fun updateOccMode(service : IEService, systemProfile: VavIERtu) {

        if (isSystemOccupied(systemProfile)) {
            writeToIEDevice(
                service,
                IE_POINT_TYPE_MV,
                IE_POINT_NAME_OCCUPANCY,
                IE_MSG_BODY.format(OccMode.Occ.ordinal.toFloat())
            )
        } else {
            writeToIEDevice(
                service,
                IE_POINT_TYPE_MV,
                IE_POINT_NAME_OCCUPANCY,
                IE_MSG_BODY.format(OccMode.Unocc.ordinal.toFloat())
            )
        }
    }

    private fun updateConditioningMode(service : IEService, hayStack: CCUHsApi) {
        if (isConditioningRequired(hayStack)) {
            if (currentCondMode != NetApplicMode.Auto) {
                writeToIEDevice(
                    service,
                    IE_POINT_TYPE_MI,
                    IE_POINT_NAME_CONDITIONING_MODE,
                    IE_MSG_BODY.format(NetApplicMode.Auto.ordinal.toFloat())
                )
                currentCondMode = NetApplicMode.Auto
            }
        } else {
            if (currentCondMode != NetApplicMode.FanOnly) {
                writeToIEDevice(
                    service,
                    IE_POINT_TYPE_MI,
                    IE_POINT_NAME_CONDITIONING_MODE,
                    IE_MSG_BODY.format(NetApplicMode.FanOnly.ordinal.toFloat())
                )
                currentCondMode = NetApplicMode.FanOnly
            }
        }

    }

    private fun updateDatClgSetpoint(service : IEService, systemProfile: VavIERtu) {
        writeToIEDevice(
            service,
            IE_POINT_TYPE_MI,
            IE_POINT_NAME_DAT_SETPOINT,
            IE_MSG_BODY.format(fahrenheitToCelsius(systemProfile.getCmd("dat and setpoint")))
        )
    }

    /**
     * Update fanLoop only when multizone configuration or dspSp or fanLoopOp changes.
     */
    private fun updateFanControl(service : IEService, hayStack: CCUHsApi, systemProfile: VavIERtu) {
        if (isMultiZoneEnabled(hayStack)) {
            val updatedStaticPressureSp = inchToPascal(getDuctStaticPressureTarget(systemProfile))
            if (updatedStaticPressureSp != staticPressureSp) {
                writeToIEDevice(
                    service,
                    IE_POINT_TYPE_AV,
                    IE_POINT_NAME_DSP_SETPOINT,
                    IE_MSG_BODY.format(updatedStaticPressureSp)
                )
                staticPressureSp = updatedStaticPressureSp
                fanLoopSp = -1.0
            }
        } else {
            if (fanLoopSp != systemProfile.systemFanLoopOp) {
                writeToIEDevice(
                    service,
                    IE_POINT_TYPE_AV,
                    IE_POINT_NAME_FAN_SPEED_CONTROL,
                    IE_MSG_BODY.format(systemProfile.systemFanLoopOp)
                )
                fanLoopSp = systemProfile.systemFanLoopOp
                staticPressureSp = -1.0
            }
        }
    }

    private fun updateHumidityControl(service : IEService, systemProfile: VavIERtu) {
        if (systemProfile.getConfigEnabled(Tags.HUMIDIFICATION) > 0) {
            if (humCtrl != HumidityCtrl.RelHum) {
                writeToIEDevice(
                    service,
                    IE_POINT_TYPE_MI,
                    IE_POINT_NAME_HUMIDITY_CONTROL,
                    IE_MSG_BODY.format(HumidityCtrl.RelHum.ordinal.toFloat())
                )
                humCtrl = HumidityCtrl.RelHum
            }

            writeToIEDevice(
                service,
                IE_POINT_TYPE_AV,
                IE_POINT_NAME_RELATIVE_HUMIDITY,
                IE_MSG_BODY.format(systemProfile.systemController.averageSystemHumidity)
            )

            writeToIEDevice(
                service,
                IE_POINT_TYPE_AV,
                IE_POINT_NAME_HUMIDITY_SETPOINT,
                IE_MSG_BODY.format(getMeanHumidityTarget())
            )

        } else {
            if (humCtrl != HumidityCtrl.None) {
                writeToIEDevice(
                    service,
                    IE_POINT_TYPE_MI,
                    IE_POINT_NAME_HUMIDITY_CONTROL,
                    IE_MSG_BODY.format(HumidityCtrl.None.ordinal.toFloat())
                )
                humCtrl = HumidityCtrl.None
            }
        }
    }

    private fun fetchAlarms(ieService: IEService, hayStack: CCUHsApi) {

        ieService.readPoint(IE_POINT_TYPE_AV, IE_POINT_NAME_ALARM_WARN)
            .subscribeOn(Schedulers.io())
            .subscribe(
                { response ->
                    hayStack.writeHisValByQuery("system and point and ie and alarm and warning",
                                            response.responseVal.toDouble())
                },
                { error -> CcuLog.e(L.TAG_CCU_DEVICE, "Error fetching alarm warnings", error) }
            )

        ieService.readPoint(IE_POINT_TYPE_AV, IE_POINT_NAME_ALARM_PROB)
            .subscribeOn(Schedulers.io())
            .subscribe(
                { response ->
                    hayStack.writeHisValByQuery("system and point and ie and alarm and problem",
                        response.responseVal.toDouble())
                },
                { error -> CcuLog.e(L.TAG_CCU_DEVICE, "Error fetching alarm problems", error) }
            )

        ieService.readPoint(IE_POINT_TYPE_AV, IE_POINT_NAME_ALARM_FAULT)
            .subscribeOn(Schedulers.io())
            .subscribe(
                { response ->
                    hayStack.writeHisValByQuery("system and point and ie and alarm and fault",
                        response.responseVal.toDouble())
                },
                { error -> CcuLog.e(L.TAG_CCU_DEVICE, "Error fetching alarm fault", error) }
            )

    }

    private fun fetchSystemClock(ieService: IEService, hayStack: CCUHsApi) {
        ieService.readPoint(IE_POINT_TYPE_AV, IE_POINT_NAME_SYSTEM_CLOCK)
            .subscribeOn(Schedulers.io())
            .subscribe(
                { response ->
                    hayStack.writeHisValByQuery("system and point and ie and clock",
                        response.responseVal.toDouble())
                },
                { error -> CcuLog.e(L.TAG_CCU_DEVICE, "Error fetching system clock", error) }
            )
    }

    private fun fetchOccStatus(ieService: IEService, hayStack: CCUHsApi) {
        ieService.readPoint(IE_POINT_TYPE_MV, IE_POINT_NAME_OCCUPANCY_STATUS)
            .subscribeOn(Schedulers.io())
            .subscribe(
                { response ->
                    hayStack.writeHisValByQuery("system and point and ie and occStatus",
                        response.responseVal.toDouble())
                },
                { error -> CcuLog.e(L.TAG_CCU_DEVICE, "Error fetching occStatus", error) }
            )
    }

    private fun writeToIEDevice(service: IEService, pointType : String, pointName : String, msg : String)  {
        service.writePoint(
            pointType,
            pointName,
            msg
        ).subscribeOn(Schedulers.io())
         .subscribe(
             { CcuLog.e(L.TAG_CCU_DEVICE, "IE Write Completed")},
             { error -> CcuLog.e(L.TAG_CCU_DEVICE, "IE Write failed for $msg : $error.message" ) }
         )
    }
}