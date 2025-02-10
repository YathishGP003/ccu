package a75f.io.logic.logtasks

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.cloud.RemoteFileStorageManager
import a75f.io.logic.cloudservice.ServiceGenerator
import a75f.io.logic.filesystem.FileSystemTools
import a75f.io.logic.reportNull
import android.preference.PreferenceManager
import java.io.File

/**
 * A repository-level class, providing an interface to the filesystem layer and network layer, implementing
 * the task of uploading the log files.
 *
 * @author Tony Case
 * Created on 1/13/21.
 */
class UploadLogs(
   private val fileSystemTools: FileSystemTools,
   private val fileStorageManager: RemoteFileStorageManager,
   private val haystackApi: CCUHsApi
) {
   val TAG_CCU_SITE_SEQUENCER = "CCU_SITE_SEQUENCER"

   // companion object can be thought of as the static side of this class.
   companion object {

      // a convenience creator until we add Dependency injection
      @JvmStatic  // (for easy use from Java)
      fun instanceOf(): UploadLogs {
         val appContext = Globals.getInstance().applicationContext
         val haystackApi = CCUHsApi.getInstance()

         val fileSystemTools = FileSystemTools(appContext)
         val fileStorageService = ServiceGenerator().provideFileStorageService()
         val fileStorageManager = RemoteFileStorageManager(fileStorageService)

         return UploadLogs(fileSystemTools, fileStorageManager, haystackApi)
      }
   }

   /**
    * Save logcat log and preferences, and post combined zip to server
    * NOTE:  Currently, this function performs lots of file I/O and should be performed asynchronously.
    */
   fun saveCcuLogs() {
      val dateStr = fileSystemTools.timeStamp()
      val ccuGuid = haystackApi.ccuId ?: "ccu-id-missing"
      val ccuGuidTrimmed = if (ccuGuid.startsWith("@")) ccuGuid.drop(1) else ccuGuid

      val logFile = fileSystemTools.writeLogCat("Renatus_Logs_$dateStr.txt")
      val prefsFile = fileSystemTools.writePreferences("Renatus_Prefs_$dateStr.txt")

      val messageFile = fileSystemTools.writeMessages("Renatus_Messages_$dateStr.txt")

      val entityFile = fileSystemTools.writeHayStackEntities("Renatus_Entity_Writable_$dateStr.txt")

      val bacAppLogFile = fileSystemTools.getBacAppLogs( "ccu/bacnet", "logs.txt")

      val ccuInfo = fileSystemTools.writeCcuInfo("CCU_Info_$dateStr.txt")

      val usbEventsLogFile = fileSystemTools.getFile("Renatus_USB_Events_Log.txt")

      val listOfFiles: List<File> = mutableListOf<File>().apply {
         add(logFile)
         add(prefsFile)
         add(messageFile)
         add(entityFile)
         add(ccuInfo)
         bacAppLogFile?.let { add(it) }
         usbEventsLogFile?.let { add(it) }
      }

      // This specific file id format is a requirement (Earth(CCU)-4726)
      val fileId = ccuGuidTrimmed + "_" + dateStr + ".zip"
      val zipFile = fileSystemTools.zipFiles(listOfFiles, fileId)

      val siteRef = haystackApi.siteIdRef.toString()

      if (siteRef == null) {
         // not the exact way I intended to use reportNull, but it works to report a bug
         siteRef.reportNull("null", "siteId missing while trying to save log")
         return
      }

      // drop the "@"
      val siteId = if (siteRef.startsWith("@")) siteRef.drop(1) else siteRef

      updateFileData(fileSystemTools.getFilePath(listOfFiles, fileId), fileId, L.TAG_CCU_LOGS)

      fileStorageManager.uploadFile(
         zipFile,
         mimeType =  "application/zip",
         container = "cculogs",
         siteId = siteId,
         fileId = fileId,
         logType = L.TAG_CCU_LOGS)

      CcuLog.i(L.TAG_CCU, "file uploaded")
   }

   fun saveCcuSequencerLogs(id: String) {
      CcuLog.i(TAG_CCU_SITE_SEQUENCER, "saveCcuSequencerLogs id -> $id")
      if(id.isEmpty() || id.isBlank() || !id.contains("_")) {
         CcuLog.i(TAG_CCU_SITE_SEQUENCER, "saveCcuSequencerLogs id is empty -> $id")
         return
      }
      val fileName = "seq_@$id.json"
      val dateStr = fileSystemTools.timeStamp()
      val ccuGuid = haystackApi.ccuId ?: "ccu-id-missing"
      val ccuGuidTrimmed = if (id.startsWith("@")) id.drop(1) else id
      val logDir = "ccu/sequencer"
      val seqFile = fileSystemTools.getBacAppLogs( logDir, fileName)
      val listOfFiles: List<File> = mutableListOf<File>().apply {
         seqFile?.let { add(it) }
      }
      if(listOfFiles.isEmpty()){
            CcuLog.i(TAG_CCU_SITE_SEQUENCER, "No sequencer logs found")
            return
      }
      // This specific file id format is a requirement (Earth(CCU)-4726)
      val fileId = ccuGuidTrimmed + "_" + dateStr + ".zip"
      val zipFile = fileSystemTools.zipFiles(listOfFiles, fileId)
      val siteRef = haystackApi.siteIdRef.toString()

      if (siteRef == null) {
         // not the exact way I intended to use reportNull, but it works to report a bug
         siteRef.reportNull("null", "siteId missing while trying to save log")
         CcuLog.i(TAG_CCU_SITE_SEQUENCER, "saveCcuSequencerLogs site ref is null")
         return
      }

      // drop the "@"
      val siteId = if (siteRef.startsWith("@")) siteRef.drop(1) else siteRef

      updateFileData(fileSystemTools.getFilePath(listOfFiles, fileId), fileId, L.TAG_SEQUENCER_LOGS)

      fileStorageManager.uploadFile(
         zipFile!!,
         mimeType =  "application/zip",
         container = "seqlogs",
         siteId = siteId,
         fileId = fileId,
         logType = L.TAG_SEQUENCER_LOGS)

      CcuLog.i(TAG_CCU_SITE_SEQUENCER, "sequencer log files uploaded")
   }

   fun saveCcuAlertsLogs(id: String) {
      CcuLog.i("CCU_ALERTS", "saveCcuAlertsLogs id -> $id")
      val fileName = "blocklyAlert_@$id.json"
      val dateStr = fileSystemTools.timeStamp()
      val ccuGuid = haystackApi.ccuId ?: "ccu-id-missing"
      val ccuGuidTrimmed = if (id.startsWith("@")) id.drop(1) else id
      val logDir = "ccu/alerts"
      val seqFile = fileSystemTools.getBacAppLogs( logDir, fileName)
      val listOfFiles: List<File> = mutableListOf<File>().apply {
         seqFile?.let { add(it) }
      }
      if(listOfFiles.isEmpty()){
         CcuLog.i("CCU_ALERTS", "No alerts logs found")
         return
      }
      // This specific file id format is a requirement (Earth(CCU)-4726)
      val fileId = ccuGuidTrimmed + "_" + dateStr + ".zip"
      val zipFile = fileSystemTools.zipFiles(listOfFiles, fileId)
      val siteRef = haystackApi.siteIdRef.toString()

      if (siteRef == null) {
         // not the exact way I intended to use reportNull, but it works to report a bug
         siteRef.reportNull("null", "siteId missing while trying to save log")
         CcuLog.i("CCU_ALERTS", "saveCcuAlertsLogs site ref is null")
         return
      }

      // drop the "@"
      val siteId = if (siteRef.startsWith("@")) siteRef.drop(1) else siteRef

      updateFileData(fileSystemTools.getFilePath(listOfFiles, fileId), fileId, L.TAG_ALERT_LOGS)

      fileStorageManager.uploadFile(
         zipFile!!,
         mimeType =  "application/zip",
         container = "alertlogs",
         siteId = siteId,
         fileId = fileId,
         logType = L.TAG_ALERT_LOGS)

      CcuLog.i("CCU_ALERTS", "alerts log files uploaded")
   }

   fun syncUnSyncedLogFiles() {
      val defaultSharedPref =
         PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().applicationContext)
      val ccuLogFilePath = defaultSharedPref.getString(L.TAG_CCU_LOGS_FILE_PATH, null)
      val ccuLogFileId = defaultSharedPref.getString(L.TAG_CCU_LOGS_FILE_ID, null)
      val sequencerLogFilePath = defaultSharedPref.getString(L.TAG_SEQUENCER_LOGS_FILE_PATH, null)
      val sequencerLogFileId = defaultSharedPref.getString(L.TAG_SEQUENCER_LOGS_FILE_ID, null)
      val alertLogFilePath = defaultSharedPref.getString(L.TAG_ALERT_LOGS_FILE_PATH, null)
      val alertLogFileId = defaultSharedPref.getString(L.TAG_ALERT_LOGS_FILE_ID, null)

      try {
         if (ccuLogFilePath != null && ccuLogFileId != null) {
            CcuLog.i(
               L.TAG_CCU,
               "syncUnSyncedLogFiles ccuLogFilePath -> $ccuLogFilePath, ccuLogFileId -> $ccuLogFileId"
            )
            fileStorageManager.uploadFile(
               File(ccuLogFilePath),
               mimeType = "application/zip",
               container = "cculogs",
               siteId = haystackApi.siteIdRef.toString().replace("@", ""),
               fileId = ccuLogFileId,
               logType = L.TAG_CCU_LOGS
            )
         }

         if (sequencerLogFilePath != null && sequencerLogFileId != null) {
            CcuLog.i(
               L.TAG_CCU,
               "syncUnSyncedLogFiles sequencerLogFilePath -> $sequencerLogFilePath, sequencerLogFileId -> $sequencerLogFileId"
            )
            fileStorageManager.uploadFile(
               File(sequencerLogFilePath),
               mimeType = "application/zip",
               container = "seqlogs",
               siteId = haystackApi.siteIdRef.toString().replace("@", ""),
               fileId = sequencerLogFileId,
               logType = L.TAG_SEQUENCER_LOGS
            )
         }

         if (alertLogFilePath != null && alertLogFileId != null) {
            CcuLog.i(
               L.TAG_CCU,
               "syncUnSyncedLogFiles alertLogFilePath -> $alertLogFilePath, alertLogFileId -> $alertLogFileId"
            )
            fileStorageManager.uploadFile(
               File(alertLogFilePath),
               mimeType = "application/zip",
               container = "alertlogs",
               siteId = haystackApi.siteIdRef.toString().replace("@", ""),
               fileId = alertLogFileId,
               logType = L.TAG_ALERT_LOGS
            )
         }
      } catch (exception: Exception) {
         CcuLog.e(L.TAG_CCU, "syncUnSyncedLogFiles Exception -> $exception")
      }
   }

   fun updateFileData(filePath: String?, fileId: String?, logType: String) {
      CcuLog.i(L.TAG_CCU, "updateFIleData filePath -> $filePath, fileId -> $fileId, logType -> $logType")
      val defaultPrefs =
         PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().applicationContext)
      when (logType) {
         L.TAG_CCU_LOGS -> {
            defaultPrefs.edit().putString(L.TAG_CCU_LOGS_FILE_PATH, filePath).apply()
            defaultPrefs.edit().putString(L.TAG_CCU_LOGS_FILE_ID, fileId).apply()
         }

         L.TAG_SEQUENCER_LOGS -> {
            defaultPrefs.edit().putString(L.TAG_SEQUENCER_LOGS_FILE_PATH, filePath).apply()
            defaultPrefs.edit().putString(L.TAG_SEQUENCER_LOGS_FILE_ID, fileId).apply()
         }

         L.TAG_ALERT_LOGS -> {
            defaultPrefs.edit().putString(L.TAG_ALERT_LOGS_FILE_PATH, filePath).apply()
            defaultPrefs.edit().putString(L.TAG_ALERT_LOGS_FILE_ID, fileId).apply()
         }
      }
   }
}
