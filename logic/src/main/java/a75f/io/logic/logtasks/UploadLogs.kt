package a75f.io.logic.logtasks

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.Globals
import a75f.io.logic.cloud.RemoteFileStorageManager
import a75f.io.logic.cloudservice.ServiceGenerator
import a75f.io.logic.filesystem.FileSystemTools
import a75f.io.logic.reportNull

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

   // companion object can be thought of as the static side of this class.
   companion object {

      // a convenience creator until we add Dependency injection
      @JvmStatic  // (for easy use from Java)
      fun instanceOf(): UploadLogs {
         val appContext = Globals.getInstance().applicationContext
         val haystackApi = CCUHsApi.getInstance()

         val fileSystemTools = FileSystemTools(appContext)
         val fileStorageService = ServiceGenerator(haystackApi).provideFileStorageService()
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
      val ccuGuid = haystackApi.globalCcuId ?: "ccu-id-missing"
      val ccuGuidTrimmed = if (ccuGuid.startsWith("@")) ccuGuid.drop(1) else ccuGuid

      val logFile = fileSystemTools.writeLogCat("Renatus_Logs_$dateStr.txt")
      val prefsFile = fileSystemTools.writePreferences("Renatus_Prefs_$dateStr.txt")

      // This specific file id format is a requirement (Earth(CCU)-4726)
      val fileId = ccuGuidTrimmed + "_" + dateStr + ".zip"
      val zipFile = fileSystemTools.zipFiles(listOf(logFile, prefsFile), fileId)

      val siteRef = haystackApi.globalSiteId

      if (siteRef == null) {
         // not the exact way I intended to use reportNull, but it works to report a bug
         siteRef.reportNull("null", "siteId missing while trying to save log")
         return
      }

      // drop the "@"
      val siteId = if (siteRef.startsWith("@")) siteRef.drop(1) else siteRef

      fileStorageManager.uploadFile(
         zipFile,
         mimeType =  "application/zip",
         container = "cculogs",
         siteId = siteId,
         fileId = fileId)
   }
}
