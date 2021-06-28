package a75f.io.device.daikin

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Tags
import a75f.io.device.daikin.DaikinIE.HumidityCtrl
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.system.vav.VavIERtu

class IEDeviceHandler {

    internal enum class OccMode {
        Occ, Unocc, TntOvrd, Auto, UnInit
    }
    private var currentOccMode = OccMode.UnInit

    fun sendControl() {
        val hayStack : CCUHsApi = CCUHsApi.getInstance()
        val ieEquipUrl : String? = getIEUrl(hayStack)
        if (ieEquipUrl.isNullOrEmpty()) {
            CcuLog.e(L.TAG_CCU_DEVICE, "Invalid IE equip URL $ieEquipUrl")
            return
        }
        //Needs to recreate the retrofit service since IP address might have been updated.
        //Check if address url can be updated
        val ieService : IEService = IEServiceGenerator.instance.createService("$ieEquipUrl:8080")

        val systemProfile = L.ccu().systemProfile as VavIERtu

        if (systemProfile.getConfigEnabled(Tags.FAN) > 0) {
            updateFanControl(
                ieService,
                hayStack,
                systemProfile
            )
        }

        updateOccMode(
            ieService,
            systemProfile
        )

        updateConditioningMode(
            ieService,
            hayStack
        )
        updateDatClgSetpoint(
            ieService,
            systemProfile
        )

        updateFanControl(
            ieService,
            hayStack,
            systemProfile
        )

        updateHumidityControl(
            ieService,
            systemProfile
        )
    }

    private fun updateOccMode(service : IEService, systemProfile: VavIERtu) {

        if (isSystemOccupied(systemProfile)) {
            if (currentOccMode != OccMode.Occ) {
                service.writePoint(
                    IE_POINT_TYPE_MV,
                    IE_POINT_NAME_OCCUPANCY,
                    IE_MSG_BODY.format(OccMode.Occ.ordinal)
                )
                currentOccMode = OccMode.Occ
            }
        } else {
            if (currentOccMode != OccMode.Unocc) {
                service.writePoint(
                    IE_POINT_TYPE_MV,
                    IE_POINT_NAME_OCCUPANCY,
                    IE_MSG_BODY.format(OccMode.Unocc.ordinal)
                )
                currentOccMode = OccMode.Unocc
            }
        }
    }

    private fun updateConditioningMode(service : IEService, hayStack: CCUHsApi) {
        if (isConditioningRequired(hayStack)) {
            service.writePoint(
                IE_POINT_TYPE_MI,
                IE_POINT_NAME_CONDITIONING_MODE,
                IE_MSG_BODY.format(DaikinIE.NetApplicMode.Auto.ordinal)
            )
        } else {
            service.writePoint(
                IE_POINT_TYPE_MI,
                IE_POINT_NAME_CONDITIONING_MODE,
                IE_MSG_BODY.format(DaikinIE.NetApplicMode.FanOnly.ordinal)
            )
        }

    }

    private fun updateDatClgSetpoint(service : IEService, systemProfile: VavIERtu) {
        service.writePoint(
            IE_POINT_TYPE_MI,
            IE_POINT_NAME_DAT_SETPOINT,
            IE_MSG_BODY.format(fahrenheitToCelsius(systemProfile.getCmd("dat and setpoint")))
        )
    }

    private fun updateFanControl(service : IEService, hayStack: CCUHsApi, systemProfile: VavIERtu) {
        if (isMultiZoneEnabled(hayStack)) {
            service.writePoint(
                IE_POINT_TYPE_AV,
                IE_POINT_NAME_DSP_SETPOINT,
                IE_MSG_BODY.format(inchToPascal(getDuctStaticPressureTarget(systemProfile)))
            )
        } else {
            service.writePoint(
                IE_POINT_TYPE_AV,
                IE_POINT_NAME_FAN_SPEED_CONTROL,
                IE_MSG_BODY.format(systemProfile.systemFanLoopOp)
            )
        }
    }

    private fun updateHumidityControl(service : IEService, systemProfile: VavIERtu) {
        if (systemProfile.getConfigEnabled(Tags.HUMIDIFICATION) > 0) {
            if (DaikinIE.humCtrl != HumidityCtrl.RelHum) {
                service.writePoint(
                    IE_POINT_TYPE_MI,
                    IE_POINT_NAME_HUMIDITY_CONTROL,
                    IE_MSG_BODY.format(HumidityCtrl.RelHum.ordinal)
                )
                DaikinIE.humCtrl = HumidityCtrl.RelHum
            }

            service.writePoint(
                IE_POINT_TYPE_AV,
                IE_POINT_NAME_RELATIVE_HUMIDITY,
                IE_MSG_BODY.format(systemProfile.systemController.averageSystemHumidity)
            )

            service.writePoint(
                IE_POINT_TYPE_AV,
                IE_POINT_NAME_HUMIDITY_SETPOINT,
                IE_MSG_BODY.format(getMeanHumidityTarget())
            )

        } else {
            if (DaikinIE.humCtrl != HumidityCtrl.None) {
                service.writePoint(
                    IE_POINT_TYPE_MI,
                    IE_POINT_NAME_HUMIDITY_CONTROL,
                    IE_MSG_BODY.format(HumidityCtrl.None.ordinal)
                )
                DaikinIE.humCtrl = HumidityCtrl.None
            }
        }
    }
}