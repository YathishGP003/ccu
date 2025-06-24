package a75f.io.messaging.handler

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.devices.ConnectNodeDevice
import a75f.io.domain.service.DomainService
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil
import a75f.io.logic.bo.building.connectnode.ConnectNodeUtil.Companion.getAddressById
import a75f.io.logic.bo.building.connectnode.SequenceMetaDownloader
import a75f.io.logic.connectnode.ConnectNodeEntitiesBuilder
import a75f.io.logic.connectnode.SequenceMetaDataDTO
import a75f.io.logic.preconfig.LowCodeDownloadException
import a75f.io.logic.preconfig.ModbusEquipCreationException
import a75f.io.logic.util.SequenceApplyPrefHandler
import a75f.io.messaging.client.SequenceService
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.google.gson.JsonObject
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference


class SQMetaDataHandler {
    fun handleMetaData(appliedDetails: JsonObject, seqId: String, messageId : String, context: Context) {

        val originalMetaData = SequenceService().retrieveMetaData(seqId)
        CcuLog.d(L.TAG_CCU_SEQUENCE_APPLY, "Original Sequence metadata response: $originalMetaData $seqId")

        if(originalMetaData == null) {
            CcuLog.e(L.TAG_CCU_SEQUENCE_APPLY, "Failed to retrieve sequence metadata for seqId: $seqId")
            return
        }

        val sequenceMetaData = originalMetaData.copy(metadata = ConnectNodeUtil.normaliseSeqMetaData(originalMetaData))
        CcuLog.d(L.TAG_CCU_SEQUENCE_APPLY, "Normalised Sequence metadata response: $sequenceMetaData")

        val hayStack = CCUHsApi.getInstance()
        val domainService = DomainService()

        val seqVersion = sequenceMetaData.version
        val seqName = sequenceMetaData.seqName.trim()

        val signature = sequenceMetaData.signature
        val lowcodeLength = sequenceMetaData.sizeInBytes

        val addedDeviceList = appliedDetails["entitiesAdded"].asJsonArray.map { it.asString }
        val finalDeviceList = appliedDetails["finalEntities"].asJsonArray.map { it.asString }
        val removedDeviceList = appliedDetails["entitiesRemoved"].asJsonArray.map { it.asString }

        /*
        * Get device list where sequence version is greater than local version
        * */
        val reconfiguredDeviceList = (finalDeviceList.toSet() - addedDeviceList.toSet()).toList().filter { deviceId ->
//            val localSeqVersion = ConnectNodeDevice(deviceId.prependIndent("@")).sequenceMetadataIdentity.readDefaultVal()
//            seqVersion > localSeqVersion
            true
        }

        val deviceListToSendLowCode = (addedDeviceList + reconfiguredDeviceList).toSet().toList()


        val errors = ConnectNodeEntitiesBuilder().createConnectNodeSequence(
            sequenceMetaData,
            addedDeviceList,
            hayStack,
            domainService,
        )

        if (errors.isNotEmpty()) {
            ConnectNodeEntitiesBuilder().deleteConnectNodeSequence(
                addedDeviceList,
                hayStack
            )
            SequenceApplyPrefHandler.removeMessageFromHandling(context, messageId)
            CcuLog.e(L.TAG_CCU_SEQUENCE_APPLY, "Failed to create sequence for devices: $addedDeviceList. Errors: $errors")
            throw ModbusEquipCreationException(
                "One or more devices failed during connect node sequence. Errors:\n" +
                        errors.joinToString("\n") { it.message ?: "Unknown error" }
            )
        }

        // This will not create or delete any sequence, it will just create or delete equips in sequence.
        ConnectNodeEntitiesBuilder().updateExistingSequence(
            sequenceMetaData,
            reconfiguredDeviceList,
            hayStack,
            domainService
        )

        ConnectNodeEntitiesBuilder().deleteConnectNodeSequence(
            removedDeviceList,
            hayStack
        )

        CcuLog.i(
            L.TAG_CCU_SEQUENCE_APPLY, "Reconfiguration of Sequence completed for. " +
                    "\nsequence ID: $seqId, " +
                    "\nSequence name: $seqName, " +
                    "\nSequence version: $seqVersion, " +
                    "\nAdded devices: $addedDeviceList, " +
                    "\nReconfigured devices: $reconfiguredDeviceList, " +
                    "\nRemoved devices: $removedDeviceList, " +
                    "\nLow code will be sent devices: $deviceListToSendLowCode"
        )

        val lowCodeFileName = seqName+"_v$seqVersion.mpy"

        val downloadId = SequenceMetaDownloader().downloadLowCode(sequenceMetaData, lowCodeFileName)

        try {
            handleLowCodeDownload(
                context,
                downloadId,
                sequenceMetaData,
                seqVersion,
                deviceListToSendLowCode,
                removedDeviceList,
                lowCodeFileName,
                signature,
                lowcodeLength,
                messageId
            )
        } catch (e: LowCodeDownloadException) {
            CcuLog.e(L.TAG_CCU_DOWNLOAD, "Handled LowCode download exception: ${e.message}")

            ConnectNodeEntitiesBuilder().deleteConnectNodeSequence(addedDeviceList, hayStack)
            SequenceApplyPrefHandler.removeMessageFromHandling(context, messageId)

            CcuLog.e(L.TAG_CCU_SEQUENCE_APPLY, "Failed to download low code for sequence")
            throw LowCodeDownloadException(
                "Failed to download low code for sequence $seqId. Error: ${e.message}"
            )
        }

    }



    private fun handleLowCodeDownload(
        context: Context,
        downloadId: Long,
        sequenceMetaData: SequenceMetaDataDTO,
        seqVersion: Int,
        deviceListToSendLowCode: List<String>,
        removedDeviceList: List<String>,
        lowCodeFileName: String,
        signature: String,
        lowcodeLength: Int,
        messageId: String
    ) {
        val latch = CountDownLatch(1)
        val exceptionRef = AtomicReference<Exception?>(null)

        val onDownloadComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (completedId == downloadId) {
                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val query = DownloadManager.Query().setFilterById(completedId)
                    val cursor = downloadManager.query(query)

                    if (cursor != null && cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                CcuLog.d(L.TAG_CCU_DOWNLOAD, "Download completed for ID: $completedId")

                                val metaFileName = "${sequenceMetaData.seqName.trim()}_v$seqVersion.meta"
                                ConnectNodeUtil.createMetaFileForCN(
                                    metaFileName,
                                    signature,
                                    seqVersion, 15, lowcodeLength
                                )
                                val emptyMetaName = "emptyMeta_v0.meta"
                                val emptySequenceFileName = "emptySequence_v0.mpy"
                                // Create meta file for empty sequence with a predefined signature(for 32 bytes of 0xFF) and version
                                ConnectNodeUtil.createMetaFileForCN(
                                    emptyMetaName,
                                    "7b07415e7380c4576e2781ab51af4cb8e43b5f11ed6391d780ac8059b6cf517c",
                                    0,15,32
                                )
                                ConnectNodeUtil.createDummyMpyFile(emptySequenceFileName)

                                // Extract address for every device in the deviceListToSendLowCode
//                                var addr = "0"
                                val eventIntent = Intent(Globals.IntentActions.SEQUENCE_UPDATE_START).apply {
                                    putExtra("deviceList", ArrayList(deviceListToSendLowCode)) // Send only one device in the list
                                    putExtra("removedDeviceList", ArrayList(removedDeviceList))
                                    putExtra("seqVersion", seqVersion.toString())
                                    putExtra("seqName", sequenceMetaData.seqName.trim())
                                    putExtra("cmdLevel", "module") // Dummy
                                    putExtra("remoteCmdType", "sequence");
                                    putExtra("metaFileName", metaFileName)
                                    putExtra("firmwareName", lowCodeFileName)
                                    putExtra("emptyMetaFileName", emptyMetaName)
                                    putExtra("emptyFirmwareName", emptySequenceFileName)
                                }
                                context.sendBroadcast(eventIntent)
                                SequenceApplyPrefHandler.removeMessageFromHandling(context, messageId)
                            }

                            DownloadManager.STATUS_FAILED -> {
                                val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                                CcuLog.e(L.TAG_CCU_DOWNLOAD, "Download failed for ID: $completedId, reason: $reason")
                                SequenceApplyPrefHandler.removeMessageFromHandling(context, messageId)

                                exceptionRef.set(
                                    LowCodeDownloadException(
                                        "Download failed for ID: $completedId, reason: $reason"
                                    )
                                )
                            }

                            else -> {
                                CcuLog.w(L.TAG_CCU_DOWNLOAD, "Download ID $completedId status: $status")
                            }
                        }
                    }

                    cursor?.close()
                    context.unregisterReceiver(this)
                    latch.countDown()
                }
            }
        }

        context.registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        latch.await()

        exceptionRef.get()?.let { throw it }
    }

}