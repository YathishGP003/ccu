package a75f.io.logic.diag.otastatus

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.api.haystack.util.hayStack
import a75f.io.domain.api.Domain
import a75f.io.domain.api.DomainName
import a75f.io.domain.util.CommonQueries
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil.Companion.connectNodeEquip

/**
 * Created by Manjunath K on 28-02-2023.
 */

class OtaStatusDiagPoint {
    companion object {
        private const val OTA_STATUS = "otaStatus"
        private var connectmoduleUpdateLevel = false


        fun setConnectModuleUpdateInZoneOrModuleLevel(value: Boolean) {
            connectmoduleUpdateLevel = value
        }
        /**
         * Function to create an ota status point for module
         * returns point
         */
        private fun createOtaStatusDiagPoint(
            equipDis: String,
            equipRef: String,
            siteRef: String,
            room: String,
            floor: String,
            nodeAddr: Int,
            tz: String
        ): Point {
            return Point.Builder()
                .setDisplayName("$equipDis-$OTA_STATUS")
                .setEquipRef(equipRef).setSiteRef(siteRef)
                .setRoomRef(room).setFloorRef(floor)

                .addMarker(Tags.CUR).addMarker(Tags.DIAG)
                .addMarker(Tags.ZONE).addMarker(Tags.HIS)
                .addMarker(Tags.OTA).addMarker(Tags.SP)
                .addMarker(Tags.STATUS)

                .setKind(Kind.NUMBER).setHisInterpolate(Tags.LINEAR)
                .setTz(tz).setEnums(otaStatusEnums)
                .setGroup(nodeAddr.toString())
                .build()
        }

        /**
         * function to create an ota status point for cm and ccu
         * returns point
         */
        fun createOtaStatusDiagPoint(
            equipDis: String,
            equipRef: String,
            siteRef: String,
            tz: String
        ): Point {
            return Point.Builder()
                .setDisplayName("$equipDis-$OTA_STATUS")
                .setEquipRef(equipRef).setSiteRef(siteRef)

                .addMarker(Tags.CUR).addMarker(Tags.DIAG)
                .addMarker(Tags.ZONE).addMarker(Tags.HIS)
                .addMarker(Tags.OTA).addMarker(Tags.SP)
                .addMarker(Tags.STATUS)

                .setKind(Kind.NUMBER).setHisInterpolate(Tags.COV)
                .setTz(tz).setEnums(otaStatusEnums)
                .build()

        }

        /**
         * function creates an point and push to haystack
         */
        fun addOTAStatusPoint(
            equipDis: String, equipRef: String, siteRef: String,
            room: String, floor: String, nodeAddr: Int, tz: String , ccuHsApi: CCUHsApi) {
            val siteMap = ccuHsApi.read(Tags.SITE)
            val otaStatusPoint = createOtaStatusDiagPoint(
                siteMap[Tags.DIS].toString()+"-"+equipDis,
                equipRef, siteRef, room, floor, nodeAddr, tz
            )
            val statusPointId = ccuHsApi.addPoint(otaStatusPoint)
            CcuLog.d(L.TAG_CCU_OTA_PROCESS, "addOTAStatusPoint: $statusPointId")
        }

        /**
         * function creates an point and push to haystack
         */
        fun addOTAStatusPoint(equipDis: String,equipRef : String, siteRef: String, tz: String, ccuHsApi: CCUHsApi) {
            val otaStatusPoint = createOtaStatusDiagPoint(equipDis,equipRef,siteRef,tz)
            val statusPointId = ccuHsApi.addPoint(otaStatusPoint)
            CcuLog.d(L.TAG_CCU_OTA_PROCESS, "addOTAStatusPoint CCU: $statusPointId")
        }

        /**
         * All possible enum list for ota status
         */
        private const val otaStatusEnums =
            "OTA_REQUEST_RECEIVED,"+
            "OTA_UPDATE_STARTED,"+
            "OTA_REQUEST_IN_PROGRESS,"+
            "OTA_FIRMWARE_DOWNLOAD_FAILED,"+
            "OTA_CCU_TO_CM_FAILED,"+
            "OTA_CCU_TO_CM_UPDATE_STARTED,"+
            "OTA_CCU_TO_CM_PERCENT_COMPLETED_10,"+
            "OTA_CCU_TO_CM_PERCENT_COMPLETED_50,"+
            "OTA_CCU_TO_CM_PERCENT_COMPLETED_100,"+
            "OTA_CCU_TO_CM_FIRMWARE_RECEIVED,"+
            "OTA_CM_TO_DEVICE_PACKET_STARTED,"+
            "OTA_CM_TO_DEVICE_PERCENT_COMPLETED_10,"+
            "OTA_CM_TO_DEVICE_PERCENT_COMPLETED_20,"+
            "OTA_CM_TO_DEVICE_PERCENT_COMPLETED_30,"+
            "OTA_CM_TO_DEVICE_PERCENT_COMPLETED_40,"+
            "OTA_CM_TO_DEVICE_PERCENT_COMPLETED_50,"+
            "OTA_CM_TO_DEVICE_PERCENT_COMPLETED_60,"+
            "OTA_CM_TO_DEVICE_PERCENT_COMPLETED_70,"+
            "OTA_CM_TO_DEVICE_PERCENT_COMPLETED_80,"+
            "OTA_CM_TO_DEVICE_PERCENT_COMPLETED_90,"+
            "OTA_CM_TO_DEVICE_PERCENT_COMPLETED_100,"+
            "OTA_SUCCEEDED,"+
            "OTA_TIMEOUT,"+
            "OTA_CM_TO_DEVICE_FAILED,"+
            "NODE_STATUS_VALUE_FW_OTA_SUCCESSFUL,"+
            "NODE_STATUS_VALUE_FW_OTA_FAIL_REBOOT_INTERRUPTION,"+
            "NODE_STATUS_VALUE_FW_OTA_FAIL_NOT_FOR_ME_DEV_TYPE,"+
            "NODE_STATUS_VALUE_FW_OTA_FAIL_NOT_FOR_ME_FW_VERSION,"+
            "NODE_STATUS_VALUE_FW_OTA_FAIL_IMAGE_SIZE,"+
            "NODE_STATUS_VALUE_FW_OTA_FAIL_EXT_FLASH_ERROR,"+
            "NODE_STATUS_VALUE_FW_OTA_FAIL_IMAGE_VERIFICATION,"+
            "NODE_STATUS_VALUE_FW_OTA_FAIL_INACTIVITY_TIMEOUT,"+
            "NO_INFO"

        fun updateOtaStatusPoint(ccuHsApi: CCUHsApi, equipRef: String, status: OtaStatus) {
            ccuHsApi.writeHisValByQuery("ota and status and equipRef == \"$equipRef\"",status.ordinal.toDouble())
            CcuLog.d(L.TAG_CCU_OTA_PROCESS, "apk update updated $equipRef : $status ${status.ordinal.toDouble()}")
        }

        fun updateOtaStatusPoint(status: OtaStatus,nodes: ArrayList<Int>) {
            val hsApi = CCUHsApi.getInstance()
            nodes.forEach { i ->
                if (isCMDevice(i)) {
                    val systemEquip: HashMap<*, *> = hsApi.readEntity(CommonQueries.SYSTEM_PROFILE)
                    hsApi.writeHisValByQuery("ota and status and equipRef ==\"${systemEquip[Tags.ID].toString()}\"",status.ordinal.toDouble())
                } else{
                    //hyperstatsplit we have two OTA point -->  connect module and Hyperstatsplit
                    if(connectmoduleUpdateLevel) {
                        hsApi.writeHisValByQuery(" ota and status and domainName ==\"otaStatusConnectModule\" and  group ==\"$i\"",status.ordinal.toDouble())
                    }
                    else {
                        hsApi.writeHisValByQuery("ota and status and not connectModule and group ==\"$i\"",status.ordinal.toDouble())
                    }
                }
                CcuLog.d(L.TAG_CCU_OTA_PROCESS, "multiple updates updated $i : $status ${status.ordinal.toDouble()}")
            }
        }

        fun updateOtaStatusPoint(status: OtaStatus,node: Int) {
            val hsApi = CCUHsApi.getInstance()
            if (isCMDevice(node)) {
                Domain.diagEquip.otaStatusCM.writeHisVal(status.ordinal.toDouble())
            } else {
                //hyperstatsplit we have two OTA point -->  connect module and Hyperstatsplit
                if(connectmoduleUpdateLevel) {
                    hsApi.writeHisValByQuery(" ota and status and domainName ==\"otaStatusConnectModule\" and  group ==\"$node\"",status.ordinal.toDouble())
                }
                else {
                    hsApi.writeHisValByQuery("ota and status and not connectModule and group ==\"$node\"", status.ordinal.toDouble())
                }
            }
            CcuLog.d(L.TAG_CCU_OTA_PROCESS, "updateOtaStatusPoint updated $node : $status ${status.ordinal.toDouble()}")
        }

        /**
         * Function to get the current ota status
         */
        fun getCurrentOtaStatus(node: Int): Double {
            CcuLog.d(L.TAG_CCU_OTA_PROCESS, "Reading OTA status $node" )
            return if (isCMDevice(node)) {
                val systemEquip: HashMap<*, *> = CCUHsApi.getInstance().readEntity(CommonQueries.SYSTEM_PROFILE)
                CCUHsApi.getInstance().readHisValByQuery("ota and status and equipRef ==\"${systemEquip[Tags.ID].toString()}\"")
            } else {
                if(connectmoduleUpdateLevel) {
                  CCUHsApi.getInstance().readHisValByQuery("ota and status and domainName ==\"otaStatusConnectModule\" and  group ==\"$node\"")
                }
                else {
                    CCUHsApi.getInstance().readHisValByQuery("ota and status and not connectModule and group ==\"$node\"")
                }
            }

        }

        fun updateCcuToCmOtaProgress(totalPackets: Double, currentRunningPacket: Int, nodeAddress: Int) {
            val otaProgress = ((currentRunningPacket.toDouble() / totalPackets) * 100).toInt()
            val currentOtaStatus = getCurrentOtaStatus(nodeAddress).toInt()
            CcuLog.d(L.TAG_CCU_OTA_PROCESS, "" +
                    "\n totalPackets : $totalPackets"+ "  currentRunningPacket : $currentRunningPacket"+
                    "\n otaProgress : $otaProgress"+ "  currentOtaStatus : ${OtaStatus.values()[currentOtaStatus]}"
            )
            when {
                (otaProgress in (10..49)) -> {
                    updateIfChangeInValue (currentOtaStatus,OtaStatus.OTA_CCU_TO_CM_PERCENT_COMPLETED_10,nodeAddress)
                    return
                }
                (otaProgress in (50..98)) -> {
                    updateIfChangeInValue (currentOtaStatus,OtaStatus.OTA_CCU_TO_CM_PERCENT_COMPLETED_50,nodeAddress)
                    return
                }
                (otaProgress >= 99)  -> {
                    updateIfChangeInValue (currentOtaStatus,OtaStatus.OTA_CCU_TO_CM_PERCENT_COMPLETED_100,nodeAddress)
                    return
                }
            }
        }

        fun updateCcuToCmSeqProgress(totalPackets: Double, currentRunningPacket: Int, nodeAddress: Int) {
            val seqProgress = ((currentRunningPacket.toDouble() / totalPackets) * 100).toInt()
            val currentSeqStatus = connectNodeEquip(nodeAddress).otaStatus.readHisVal().toInt()
            CcuLog.d(L.TAG_CCU_OTA_PROCESS, "" +
                    "\n totalPackets : $totalPackets"+ "  currentRunningPacket : $currentRunningPacket"+
                    "\n otaProgress : $seqProgress"+ "  currentOtaStatus : ${SequenceOtaStatus.values()[currentSeqStatus]}"
            )
            when {
                (seqProgress in (10..49)) -> {
                    updateIfChangeInSeqValue (currentSeqStatus,SequenceOtaStatus.SEQ_CCU_TO_CM_PERCENT_COMPLETED_10,nodeAddress)
                    return
                }
                (seqProgress in (50..98)) -> {
                    updateIfChangeInSeqValue (currentSeqStatus,SequenceOtaStatus.SEQ_CCU_TO_CM_PERCENT_COMPLETED_50,nodeAddress)
                    return
                }
                (seqProgress >= 99)  -> {
                    updateIfChangeInSeqValue (currentSeqStatus,SequenceOtaStatus.SEQ_CCU_TO_CM_PERCENT_COMPLETED_100,nodeAddress)
                    return
                }
            }
        }

        private fun updateIfChangeInValue(currentState: Int, newState: OtaStatus, nodeAddress: Int) {
            if (currentState != newState.ordinal)
                updateOtaStatusPoint(newState, nodeAddress)
        }

        private fun updateIfChangeInSeqValue(currentState: Int, newState: SequenceOtaStatus, nodeAddress: Int) {
            if (currentState != newState.ordinal)
                ConnectNodeUtil.updateOtaSequenceStatus(newState, nodeAddress)
        }

        fun updateCmToDeviceOtaProgress(totalPackets: Double, currentRunningPacket: Int, nodeAddress: Int) {

            val otaProgress = ((currentRunningPacket.toDouble() / totalPackets) * 100).toInt()
            val currentOtaStatus = getCurrentOtaStatus(nodeAddress).toInt()

            CcuLog.d(L.TAG_CCU_OTA_PROCESS, "" +
                    "\n totalPackets : $totalPackets"+ " currentRunningPacket : $currentRunningPacket"+
                    "\n otaProgress : $otaProgress"+ " currentOtaStatus : ${OtaStatus.values()[getCurrentOtaStatus(nodeAddress).toInt()]}")
            when {
                (otaProgress in (10..19)) -> {
                    updateIfChangeInValue (currentOtaStatus,OtaStatus.OTA_CM_TO_DEVICE_PERCENT_COMPLETED_10,nodeAddress)
                    return
                }
                (otaProgress in (20..29)) -> {
                    updateIfChangeInValue (currentOtaStatus,OtaStatus.OTA_CM_TO_DEVICE_PERCENT_COMPLETED_20,nodeAddress)
                    return
                }
                (otaProgress in (30..39)) -> {
                    updateIfChangeInValue (currentOtaStatus,OtaStatus.OTA_CM_TO_DEVICE_PERCENT_COMPLETED_30,nodeAddress)
                    return
                }
                (otaProgress in (40..49)) -> {
                    updateIfChangeInValue (currentOtaStatus,OtaStatus.OTA_CM_TO_DEVICE_PERCENT_COMPLETED_40,nodeAddress)
                    return
                }
                (otaProgress in (50..59)) -> {
                    updateIfChangeInValue (currentOtaStatus,OtaStatus.OTA_CM_TO_DEVICE_PERCENT_COMPLETED_50,nodeAddress)
                    return
                }
                (otaProgress in (60..69)) -> {
                    updateIfChangeInValue (currentOtaStatus,OtaStatus.OTA_CM_TO_DEVICE_PERCENT_COMPLETED_60,nodeAddress)
                    return
                }
                (otaProgress in (70..79)) -> {
                    updateIfChangeInValue (currentOtaStatus,OtaStatus.OTA_CM_TO_DEVICE_PERCENT_COMPLETED_70,nodeAddress)
                    return
                }
                (otaProgress in (80..89)) -> {
                    updateIfChangeInValue (currentOtaStatus,OtaStatus.OTA_CM_TO_DEVICE_PERCENT_COMPLETED_80,nodeAddress)
                    return
                }
                (otaProgress in (90..98)) -> {
                    updateIfChangeInValue (currentOtaStatus,OtaStatus.OTA_CM_TO_DEVICE_PERCENT_COMPLETED_90,nodeAddress)
                    return
                }
                (otaProgress >= 99) -> {
                    updateIfChangeInValue (currentOtaStatus,OtaStatus.OTA_CM_TO_DEVICE_PERCENT_COMPLETED_100,nodeAddress)
                    return
                }
            }
        }

        /**
         * Updates and logs the progress of an OTA (Over-The-Air) sequence transfer from CM to device.
         *
         * @param totalPackets The total number of packets expected in the transfer
         * @param currentRunningPacket The current packet number being processed
         * @param nodeAddress The network address of the target device
         *
         * This function:
         * 1. Calculates the current progress percentage
         * 2. Reads the current OTA status from the device
         * 3. Logs detailed progress information
         * 4. Updates the sequence status when progress reaches each 10% milestone
         *
         * Progress Thresholds:
         * - Updates status at every 10% interval (10%, 20%, ..., 100%)
         * - Uses [SequenceOtaStatus] to track completion milestones
         * - Only updates status when crossing a new threshold (via [updateIfChangeInSeqValue])
         *
         */
        fun updateCmToDeviceSeqProgress(totalPackets: Double, currentRunningPacket: Int, nodeAddress: Int) {

            val otaProgress = ((currentRunningPacket.toDouble() / totalPackets) * 100).toInt()
            val currentSeqStatus = connectNodeEquip(nodeAddress).otaStatus.readHisVal().toInt()

            CcuLog.d(L.TAG_CCU_OTA_PROCESS, "" +
                    "\n totalPackets : $totalPackets"+ " currentRunningPacket : $currentRunningPacket"+
                    "\n otaProgress : $otaProgress"+ " currentOtaStatus : ${OtaStatus.values()[getCurrentOtaStatus(nodeAddress).toInt()]}")
            // Determine the appropriate progress milestone
            val progressStatus = when (otaProgress) {
                in 10..19 -> SequenceOtaStatus.SEQ_CM_TO_DEVICE_PERCENT_COMPLETED_10
                in 20..29 -> SequenceOtaStatus.SEQ_CM_TO_DEVICE_PERCENT_COMPLETED_20
                in 30..39 -> SequenceOtaStatus.SEQ_CM_TO_DEVICE_PERCENT_COMPLETED_30
                in 40..49 -> SequenceOtaStatus.SEQ_CM_TO_DEVICE_PERCENT_COMPLETED_40
                in 50..59 -> SequenceOtaStatus.SEQ_CM_TO_DEVICE_PERCENT_COMPLETED_50
                in 60..69 -> SequenceOtaStatus.SEQ_CM_TO_DEVICE_PERCENT_COMPLETED_60
                in 70..79 -> SequenceOtaStatus.SEQ_CM_TO_DEVICE_PERCENT_COMPLETED_70
                in 80..89 -> SequenceOtaStatus.SEQ_CM_TO_DEVICE_PERCENT_COMPLETED_80
                in 90..98 -> SequenceOtaStatus.SEQ_CM_TO_DEVICE_PERCENT_COMPLETED_90
                99, 100 -> SequenceOtaStatus.SEQ_CM_TO_DEVICE_PERCENT_COMPLETED_100
                else -> return  // No status update for progress < 10%
            }
            currentSeqStatus.takeIf { it != progressStatus.ordinal }?.let {
                updateIfChangeInSeqValue(currentSeqStatus, progressStatus, nodeAddress)
            } ?: CcuLog.d(L.TAG_CCU_OTA_PROCESS, "No change in sequence status for node $nodeAddress")
        }

        private fun isCMDevice(nodeAddress: Int) : Boolean {
            return nodeAddress == ( L.ccu().addressBand + 99 )
        }


        fun updateCCUOtaStatus(status: OtaStatus) {
            val daigEquip = CCUHsApi.getInstance()
                .readEntity("equip and diag or domainName == \"${DomainName.diagEquip}\"")
            if(daigEquip.isEmpty()) return

            if (daigEquip["domainName"] != null && daigEquip["domainName"].toString() == DomainName.diagEquip) {
                Domain.isDiagEquipInitialised();
                CcuLog.e(L.TAG_CCU_DOWNLOAD, "updateCCUOtaStatus: DM DiagEquip")
                Domain.diagEquip.otaStatusCCU.writeHisVal(status.ordinal.toDouble())
            } else {
                CcuLog.e(L.TAG_CCU_DOWNLOAD, "updateCCUOtaStatus: Non DM DiagEquip")
                hayStack.writeHisValByQuery(
                    "ota and status and equipRef ==\"${daigEquip.get("id")}\"",
                    status.ordinal.toDouble()
                )
            }
        }

        /**
         * Function to update the ota status of a bundle
         */
        fun updateBundleOtaStatus(status: BundleOtaStatus) {
            if(Domain.isDiagEquipInitialised()) {
                val point = Domain.diagEquip.otaStatusBundle
                point.writeHisVal(status.ordinal.toDouble())
            } else {
                CcuLog.e(L.TAG_CCU_BUNDLE, "updateBundleOtaStatus: DiagEquip not initialised")
            }
        }

        /**
         * Function to update the bundle version point
         */
        fun updateBundleVersion(version: String) {
            if(Domain.isDiagEquipInitialised()) {
                val point = Domain.diagEquip.bundleVersion
                point.writeDefaultVal(version)
            } else {
                CcuLog.e(L.TAG_CCU_BUNDLE, "updateBundleVersion: DiagEquip not initialised")
            }
        }
    }
}