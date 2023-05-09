package a75f.io.logic.diag.otastatus

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Kind
import a75f.io.api.haystack.Point
import a75f.io.api.haystack.Tags
import a75f.io.logic.L
import android.util.Log

/**
 * Created by Manjunath K on 28-02-2023.
 */

class OtaStatusDiagPoint {
    companion object {
        private const val OTA_STATUS = "otaStatus"

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
            Log.i(L.TAG_CCU_OTA_PROCESS, "addOTAStatusPoint: $statusPointId")
        }

        /**
         * function creates an point and push to haystack
         */
        fun addOTAStatusPoint(equipDis: String,equipRef : String, siteRef: String, tz: String, ccuHsApi: CCUHsApi) {
            val otaStatusPoint = createOtaStatusDiagPoint(equipDis,equipRef,siteRef,tz)
            val statusPointId = ccuHsApi.addPoint(otaStatusPoint)
            Log.i(L.TAG_CCU_OTA_PROCESS, "addOTAStatusPoint CCU: $statusPointId")
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
            Log.i(L.TAG_CCU_OTA_PROCESS, "apk update updated $equipRef : $status ${status.ordinal.toDouble()}")
        }
        fun updateOtaStatusPoint(status: OtaStatus,nodes: ArrayList<Int>) {
            val hsApi = CCUHsApi.getInstance()
            nodes.forEach { i ->
                if (isCMDevice(i)) {
                    val systemEquip: HashMap<*, *> = CCUHsApi.getInstance().readEntity("system and equip")
                    hsApi.writeHisValByQuery("ota and status and equipRef ==\"${systemEquip[Tags.ID].toString()}\"",status.ordinal.toDouble())
                } else {
                    hsApi.writeHisValByQuery("ota and status and group ==\"$i\"",status.ordinal.toDouble())
                }
                Log.i(L.TAG_CCU_OTA_PROCESS, "multiple updates updated $i : $status ${status.ordinal.toDouble()}")
            }
        }

        fun updateOtaStatusPoint(status: OtaStatus,node: Int) {
            val hsApi = CCUHsApi.getInstance()
            if (isCMDevice(node)) {
                val systemEquip: HashMap<*, *> = CCUHsApi.getInstance().readEntity("system and equip")
                hsApi.writeHisValByQuery("ota and status and equipRef ==\"${systemEquip[Tags.ID].toString()}\"",status.ordinal.toDouble())
            } else {
                hsApi.writeHisValByQuery("ota and status and group ==\"$node\"",status.ordinal.toDouble())
            }
            Log.i(L.TAG_CCU_OTA_PROCESS, "updateOtaStatusPoint updated $node : $status ${status.ordinal.toDouble()}")
        }

        /**
         * Function to get the current ota status
         */
        fun getCurrentOtaStatus(node: Int): Double {
            Log.i(L.TAG_CCU_OTA_PROCESS, "Reading OTA status $node" );
            return if (isCMDevice(node)) {
                val systemEquip: HashMap<*, *> = CCUHsApi.getInstance().readEntity("system and equip")
                CCUHsApi.getInstance().readHisValByQuery("ota and status and equipRef ==\"${systemEquip[Tags.ID].toString()}\"")
            } else {
                CCUHsApi.getInstance().readHisValByQuery("ota and status and group ==\"$node\"")
            }

        }

        fun updateCcuToCmOtaProgress(totalPackets: Double, currentRunningPacket: Int, nodeAddress: Int) {
            val otaProgress = ((currentRunningPacket.toDouble() / totalPackets) * 100).toInt()
            val currentOtaStatus = getCurrentOtaStatus(nodeAddress).toInt()
            Log.i(L.TAG_CCU_OTA_PROCESS, "" +
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

        private fun updateIfChangeInValue(currentState: Int, newState: OtaStatus, nodeAddress: Int) {
            if (currentState != newState.ordinal)
                updateOtaStatusPoint(newState, nodeAddress)
        }

        fun updateCmToDeviceOtaProgress(totalPackets: Double, currentRunningPacket: Int, nodeAddress: Int) {

            val otaProgress = ((currentRunningPacket.toDouble() / totalPackets) * 100).toInt()
            val currentOtaStatus = getCurrentOtaStatus(nodeAddress).toInt()

            Log.i(L.TAG_CCU_OTA_PROCESS, "" +
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

        private fun isCMDevice(nodeAddress: Int) : Boolean {
            return nodeAddress == ( L.ccu().smartNodeAddressBand + 99 )
        }


        fun updateCCUOtaStatus(status: OtaStatus) {
            val hsApi = CCUHsApi.getInstance()
            val diag = hsApi.readEntity("diag and equip")
            updateOtaStatusPoint(
                hsApi, diag[Tags.ID].toString(),
                status
            )
        }

    }
}