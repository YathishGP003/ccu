package a75f.io.logic.bo.building.connectnode

import a75f.io.api.haystack.CCUHsApi
import a75f.io.constants.HttpConstants
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.cloud.RenatusServicesEnvironment
import a75f.io.logic.connectnode.SequenceMetaDataDTO
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

class SequenceMetaDownloader {
    fun downloadLowCode(sequenceMetaData: SequenceMetaDataDTO, lowCodeFileName: String) : Long {
        val seqId = sequenceMetaData.seqId

        if (seqId.isEmpty()) {
            CcuLog.e(L.TAG_CCU_DOWNLOAD, "Sequence ID is null or empty.")
            return -1L
        }

        val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val filePath = File(downloadDir, lowCodeFileName)

        if (filePath.exists()) {
            deleteDownloadedFile(Globals.getInstance().applicationContext, lowCodeFileName)
        }

        val fileUrl = "${RenatusServicesEnvironment.instance.urls.sequencerUrl}device-seq/fetch/compiled?seqId=$seqId"
        CcuLog.d(L.TAG_CCU_DOWNLOAD, "[DOWNLOAD] Starting download of file $fileUrl")

        val downloadRequest = DownloadManager.Request(Uri.parse(fileUrl))
            .setTitle("Downloading ConnectSequence.mpy")
            .addRequestHeader("Authorization", "Bearer ${CCUHsApi.getInstance().getJwt()}")
            .addRequestHeader(
                HttpConstants.APP_NAME_HEADER_NAME,
                HttpConstants.APP_NAME_HEADER_VALUE
            )
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            .setDestinationUri(Uri.fromFile(filePath))

        val context = Globals.getInstance().applicationContext
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(downloadRequest)
        return downloadId

    }

    fun deleteDownloadedFile(context: Context, fileName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore for Android 10+
            val contentResolver = context.contentResolver
            val downloadsUri = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val selection = "${MediaStore.Downloads.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)

            try {
                val deletedRows = contentResolver.delete(downloadsUri, selection, selectionArgs)
                CcuLog.d(L.TAG_CCU_DOWNLOAD, "Deleted $deletedRows rows for file $fileName")
            } catch (e: Exception) {
                CcuLog.e(L.TAG_CCU_DOWNLOAD, "Failed to delete $fileName via MediaStore", e)
            }
        } else {
            // Use File API for Android 9 and below
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadDir, fileName)
            if (file.exists()) {
                val deleted = file.delete()
                CcuLog.d(L.TAG_CCU_DOWNLOAD, "Deleted file using File API: $deleted")
            } else {
                CcuLog.w(L.TAG_CCU_DOWNLOAD, "File not found to delete using File API")
            }
        }
    }
}