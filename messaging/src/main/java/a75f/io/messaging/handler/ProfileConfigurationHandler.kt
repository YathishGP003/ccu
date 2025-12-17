package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.HSUtil
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.logger.CcuLog
import a75f.io.logic.bo.building.vrv.VrvControlMessageCache
import a75f.io.logic.bo.util.DesiredTempDisplayMode
import a75f.io.logic.interfaces.ProfileConfigurationHandlerInterface
import a75f.io.messaging.handler.ACBUpdatePointHandler.Companion.updateACBCondensateType
import a75f.io.messaging.handler.ACBUpdatePointHandler.Companion.updateACBRelay1Type
import a75f.io.messaging.handler.ACBUpdatePointHandler.Companion.updateACBValveType
import a75f.io.messaging.handler.HyperStatMonitoringConfigHandler.reconfigureMonitoring
import a75f.io.messaging.handler.MessageUtil.Companion.updateLocalPointWriteChanges
import android.util.Log
import com.google.gson.JsonObject

object ProfileConfigurationHandler: ProfileConfigurationHandlerInterface {

    private val TAG = "CCU_RECONFIG_HANDLER"

    override fun handleProfileConfigPointUpdate(
        hayStack: CCUHsApi,
        pointUid: String,
        msgObject: JsonObject,
        localPoint: Point
    ): Boolean {
        CcuLog.d(TAG, "ProfileConfigurationHandler: Handling config point update for $pointUid")
        if (HSUtil.isPIConfig(pointUid, hayStack)) {
            updateConfigPoint(msgObject, localPoint)
            UpdatePointHandler.updatePoints(localPoint)
            hayStack.scheduleSync()
            return true
        }


        //Handle DCWB specific system config here.
        if (HSUtil.isDcwbConfig(pointUid, hayStack)) {
            ConfigPointUpdateHandler.updateConfigPoint(msgObject, localPoint, hayStack)
            updateUI(localPoint)
            hayStack.scheduleSync()
            return true
        }

        if ((HSUtil.isHsCPUEquip(pointUid, hayStack)
                    || HSUtil.isHSPipe2Equip(pointUid, hayStack)
                    || HSUtil.isHSPipe4Equip(pointUid, hayStack)
                    || HSUtil.isHSHpuEquip(pointUid, hayStack))
            && !isReconfigurationPoint(localPoint)
        ) {
            reconfigureHyperstatEquips(msgObject, localPoint)
            UpdatePointHandler.updatePoints(localPoint)
            hayStack.scheduleSync()
            return true
        }

        if ((HSUtil.isMyStatCpuEquip(pointUid, hayStack)
                    || HSUtil.isMyStatHpuEquip(pointUid, hayStack)
                    || HSUtil.isMyStatPipe2Equip(pointUid, hayStack)
                    || HSUtil.isMyStatPipe4Equip(pointUid, hayStack))
            && !isReconfigurationPoint(localPoint)
        ) {
            reconfigureMyStat(msgObject, localPoint)
            UpdatePointHandler.updatePoints(localPoint)
            hayStack.scheduleSync()
            return true
        }

        if (HSUtil.isHyperStatSplitEquip(pointUid, hayStack) &&
            !isReconfigurationPoint(localPoint)
        ) {
            reconfigureHsSplitEquip(msgObject, localPoint)
            UpdatePointHandler.updatePoints(localPoint)
            hayStack.scheduleSync()
            return true
        }

        if (HSUtil.isSystemConfigOutputPoint(pointUid, hayStack)
            || HSUtil.isSystemConfigHumidifierType(pointUid, hayStack)
            || HSUtil.isSystemConfigIE(pointUid, hayStack)
            || (HSUtil.skipUserIntentOrTunerForV2(localPoint) && HSUtil.isAdvanceAhuV2(
                pointUid,
                hayStack
            ))
            && HSUtil.skipLoopOutputPointsForV2(localPoint)
        ) {
            ConfigPointUpdateHandler.updateConfigPoint(msgObject, localPoint, hayStack)
            UpdatePointHandler.updatePoints(localPoint)
            hayStack.scheduleSync()
            return true
        }

        if (HSUtil.isSSEConfig(pointUid, hayStack)) {
            SSEConfigHandler.updateConfigPoint(msgObject, localPoint, hayStack)
            UpdatePointHandler.updatePoints(localPoint)
            SSEConfigHandler.updateTemperatureMode(localPoint, hayStack)
            hayStack.scheduleSync()
            return true
        }

        if (HSUtil.isMonitoringConfig(pointUid, hayStack)) {
            reconfigureMonitoring(msgObject, localPoint)
            UpdatePointHandler.updatePoints(localPoint)
            hayStack.scheduleSync()
            return true
        }

        if (HSUtil.isHyperStatConfig(pointUid, hayStack)
            && !localPoint.markers.contains(Tags.DESIRED) && !localPoint.markers
                .contains(Tags.SCHEDULE_TYPE) && !localPoint.markers.contains(Tags.TUNER)
        ) {
            UpdatePointHandler.updatePoints(localPoint)
            if (localPoint.markers.contains(Tags.USERINTENT) && localPoint.markers
                    .contains(Tags.CONDITIONING)
            ) {
                DesiredTempDisplayMode.setModeTypeOnUserIntentChange(
                    localPoint.roomRef,
                    hayStack
                )
            }
            if (localPoint.markers.contains(Tags.VRV)) {
                VrvControlMessageCache.getInstance()
                    .setControlsPending(localPoint.group.toInt())
            }
            return true
        }

        /* Only the config changes require profile specific handling.
     * DesiredTemp or Schedule type updates are handled using generic implementation below.
     */
        if (HSUtil.isStandaloneConfig(pointUid, hayStack)
            && !localPoint.markers.contains(Tags.DESIRED) && !localPoint.markers
                .contains(Tags.SCHEDULE_TYPE) && !localPoint.markers.contains(Tags.TUNER)
        ) {
            StandaloneConfigHandler.updateConfigPoint(msgObject, localPoint, hayStack)
            updateUI(localPoint)
            hayStack.scheduleSync()
            return true
        }

        if (HSUtil.isDamperReheatTypeConfig(pointUid, hayStack)) {
            DamperReheatTypeHandler.updatePoint(msgObject, localPoint, hayStack)
            hayStack.scheduleSync()
            return true
        }

        if (HSUtil.isVAVTrueCFMConfig(pointUid, hayStack)) {
            TrueCFMVAVConfigHandler.updateVAVConfigPoint(msgObject, localPoint, hayStack)
            hayStack.scheduleSync()
            return true
        }

        if (HSUtil.isVAVZonePriorityConfig(pointUid, hayStack)) {
            VAVZonePriorityHandler.updateVAVZonePriority(msgObject, localPoint)
            updateLocalPointWriteChanges(hayStack, pointUid, msgObject, localPoint)
            return true
        }

        if (HSUtil.isDABTrueCFMConfig(pointUid, hayStack)) {
            TrueCFMDABConfigHandler.updateDABConfigPoint(msgObject, localPoint, hayStack)
            hayStack.scheduleSync()
            return true
        }

        if (HSUtil.isDamperSizeConfigPoint(pointUid, hayStack)) {
            TrueCFMDABConfigHandler.updatePointVal(localPoint, msgObject)
            UpdatePointHandler.updatePoints(localPoint)
            hayStack.scheduleSync()
            return true
        }

        if (HSUtil.isMaxCFMCoolingConfigPoint(pointUid, hayStack)) {
            TrueCFMVAVConfigHandler.updateMinCoolingConfigPoint(msgObject, localPoint, hayStack)
            TrueCFMVAVConfigHandler.updateAirflowCFMProportionalRange(
                msgObject,
                localPoint,
                hayStack
            )
            hayStack.scheduleSync()
            return true
        }

        if (HSUtil.isMaxCFMReheatingConfigPoint(pointUid, hayStack)) {
            TrueCFMVAVConfigHandler.updateMinReheatingConfigPoint(
                msgObject,
                localPoint,
                hayStack
            )
            hayStack.scheduleSync()
            return true
        }

        if (HSUtil.isACBRelay1TypeConfig(pointUid, hayStack)) {
            updateACBRelay1Type(msgObject, localPoint, hayStack)
            hayStack.scheduleSync()
            return true
        }

        if (HSUtil.isACBCondensateTypeConfig(pointUid, hayStack)) {
            updateACBCondensateType(msgObject, localPoint, hayStack)
            hayStack.scheduleSync()
            return true
        }

        if (HSUtil.isACBValveTypeConfig(pointUid, hayStack)) {
            updateACBValveType(msgObject, localPoint, hayStack)
            updateLocalPointWriteChanges(hayStack, pointUid, msgObject, localPoint)
            hayStack.scheduleSync()
            return true
        }

        if (HSUtil.isTIProfile(pointUid, hayStack)) {
            //TIConfigHandlerToBeDelete.Companion.updateTIConfig(msgObject,localPoint,hayStack);
            tiReconfiguration(msgObject, localPoint)
            UpdatePointHandler.updatePoints(localPoint)
            return true
        }

        if (localPoint.markers.contains(Tags.OAO)) {
            updateLocalPointWriteChanges(hayStack, pointUid, msgObject, localPoint)
            updateOaoDevicePoints(msgObject, localPoint)
            hayStack.scheduleSync()
            return true
        }
        return false
    }

    private fun updateUI(updatedPoint: Point) {
        if (UpdatePointHandler.zoneDataInterface != null) {
            Log.i("PubNub", "Zone Data Received Refresh")
            UpdatePointHandler.zoneDataInterface.refreshScreen(updatedPoint.id, true)
        }
    }

    private fun isReconfigurationPoint(localPoint: Point): Boolean {
        return (localPoint.markers.contains(Tags.DESIRED)
                || localPoint.markers.contains(Tags.SCHEDULE_TYPE)
                || localPoint.markers.contains(Tags.TUNER))
    }
}