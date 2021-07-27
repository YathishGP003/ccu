package a75f.io.device.daikin

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.device.mesh.RootCommandExecuter
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.vav.VavIERtu
import io.reactivex.rxjava3.schedulers.Schedulers

class IEDeviceHandler {

    private var currentCondMode = NetApplicMode.UnInit
    private var staticPressureSp : Double = -1.0
    private var fanSpeedSp : Double = -1.0
    private var humCtrl : HumidityCtrl = HumidityCtrl.UnInit
    private var fiveMinCounter = 0

    private lateinit var ieService : IEService
    private lateinit var serviceBaseUrl : String

    companion object {
        @JvmStatic
        val instance: IEDeviceHandler by lazy {
            IEDeviceHandler()
        }
    }

    fun sendControl(systemProfile: VavIERtu, hayStack: CCUHsApi) {

        CcuLog.e(L.TAG_CCU_DEVICE, "IEDeviceHandler : sendControl")
        val ieEquipUrl : String? = getIEUrl(hayStack)
        if (ieEquipUrl.isNullOrEmpty()) {
            CcuLog.e(L.TAG_CCU_DEVICE, "Invalid IE equip URL $ieEquipUrl")
            return
        }
        //Recreate the retrofit service when IE url has been updated.
        if (!::ieService.isInitialized ||
            !::serviceBaseUrl.isInitialized ||
            serviceBaseUrl != ieEquipUrl) {
            serviceBaseUrl = ieEquipUrl
            ieService = IEServiceGenerator.instance.createService("http://$serviceBaseUrl:8080")
        }

        ieService?.let {
            //Ethernet interface seems to go down randomly, just making sure here that route & addr are configured.
            RootCommandExecuter.runRootCommand("ip route add 172.16.0.0/24 " +
                                                    "dev eth0 proto static scope link table wlan0")
            RootCommandExecuter.runRootCommand("ip addr add 172.16.0.10/24 broadcast " +
                                                    "172.16.0.255 dev eth0");
            updateOccMode(it, systemProfile)

            //Sleep is experimental to give a breather to IE. Could be removed in future
            //IE responds consistently without this.
            Thread.sleep(100)
            if (systemProfile.getConfigEnabled(Tags.FAN) > 0) {
                updateFanControl(it, hayStack, systemProfile)
            }
            Thread.sleep(100)
            updateConditioningMode(it, hayStack)
            Thread.sleep(100)
            updateDatClgSetpoint(it, systemProfile)
            Thread.sleep(100)
            updateFanControl(it, hayStack, systemProfile)
            Thread.sleep(100)
            updateHumidityControl(it, systemProfile)
            Thread.sleep(100)
            fetchAlarms(it, hayStack)
            Thread.sleep(100)
            //OccStatus to be fetched every 5 minutes
            if (fiveMinCounter == 0 || fiveMinCounter >= 5) {
                fetchOccStatus(it, hayStack)
                Thread.sleep(100)
                fetchSystemClock(it, hayStack)
                fiveMinCounter = 0;
            }
            fiveMinCounter++
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
            IE_POINT_TYPE_AV,
            IE_POINT_NAME_DAT_SETPOINT,
            IE_MSG_BODY.format(fahrenheitToCelsius(systemProfile.getCmdSignal("dat and setpoint")))
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
                fanSpeedSp = -1.0
            }
        } else {
            val fanSpeed = getFanSpeedTarget(systemProfile)
            if (fanSpeedSp != fanSpeed) {
                writeToIEDevice(
                    service,
                    IE_POINT_TYPE_AV,
                    IE_POINT_NAME_FAN_SPEED_CONTROL,
                    IE_MSG_BODY.format(fanSpeed)
                )
                fanSpeedSp = fanSpeed
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
                IE_MSG_BODY.format(getHumidityTarget())
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
                { response -> response?.result?.let {
                                    hayStack.writeHisValByQuery(
                                        "system and point and ie and alarm and warning",
                                        it.toDouble()
                                    )
                                }
                },
                { error -> CcuLog.e(L.TAG_CCU_DEVICE, "Error fetching alarm warnings", error) }
            )

        ieService.readPoint(IE_POINT_TYPE_AV, IE_POINT_NAME_ALARM_PROB)
            .subscribeOn(Schedulers.io())
            .subscribe(
                { response -> response?.result?.let{
                                    hayStack.writeHisValByQuery(
                                        "system and point and ie and alarm and problem",
                                        it.toDouble())
                                }
                },
                { error -> CcuLog.e(L.TAG_CCU_DEVICE, "Error fetching alarm problems", error) }
            )

        ieService.readPoint(IE_POINT_TYPE_AV, IE_POINT_NAME_ALARM_FAULT)
            .subscribeOn(Schedulers.io())
            .subscribe(
                { response -> response?.result?.let {
                                    hayStack.writeHisValByQuery(
                                        "system and point and ie and alarm and fault",
                                    it.toDouble())
                                }
                },
                { error -> CcuLog.e(L.TAG_CCU_DEVICE, "Error fetching alarm fault", error) }
            )

    }

    private fun fetchSystemClock(ieService: IEService, hayStack: CCUHsApi) {
        ieService.readPoint(IE_POINT_TYPE_SY, IE_POINT_NAME_SYSTEM_CLOCK)
            .subscribeOn(Schedulers.io())
            .subscribe(
                { response -> response?.result?.let {
                                    hayStack.writeHisValByQuery(
                                        "system and point and ie and clock",
                                    it.toDouble())
                                }
                },
                { error -> CcuLog.e(L.TAG_CCU_DEVICE, "Error fetching system clock", error) }
            )
    }

    private fun fetchOccStatus(ieService: IEService, hayStack: CCUHsApi) {
        ieService.readPoint(IE_POINT_TYPE_MV, IE_POINT_NAME_OCCUPANCY_STATUS)
            .subscribeOn(Schedulers.io())
            .subscribe(
                { response -> response?.result?.let {
                                    hayStack.writeHisValByQuery(
                                        "system and point and ie and occStatus",
                                        it.toDouble())
                                }
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