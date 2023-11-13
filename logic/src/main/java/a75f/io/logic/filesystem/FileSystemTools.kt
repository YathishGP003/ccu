package a75f.io.logic.filesystem

import a75f.io.data.RenatusDatabaseBuilder
import a75f.io.data.entities.EntityDatabaseHelper
import a75f.io.data.message.MessageDatabaseHelper
import a75f.io.data.writablearray.WritableArrayDatabaseHelper
import a75f.io.logic.Globals
import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import android.util.Log
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.Throws

private const val LOGS_DIR = "RenatusLogs"

/**
 * Utilities for interacting with the file system on the device.
 * Depends on application context.
 *
 * @author Tony Case
 * Created on 1/5/21.
 */
class FileSystemTools(private val appContext: Context) {
   private val TAG_BAC_APP_LOGS = "BacAppLogs"

   private val logDir =
      File(Environment.getExternalStorageDirectory().absolutePath + File.separator + LOGS_DIR)

   /** Our common Date time format for file names */
   fun timeStamp(): String = SimpleDateFormat("yyyy_MM_dd'T'HH_mm_ss", Locale.getDefault())
      .format(Date())

   //  This code exists in DevSettings.java and RenatusApp.java.  However, those are both in the
   //  app module and other modules cannot/should not call into app.
   /**
    * Write a log file to file system (external storage) with the given name.
    * NOTE:  this method will block for IO.  Expect it to be slow and don't call on main.
    * The caller should handle FileNotFoundException and IOException in the event the file cannot
    * be written.
    *
    * @throws IOException
    * @throws SecurityException
    */
   @Throws(IOException::class, SecurityException::class)
   fun writeLogCat(fileName: String): File {

      val process = Runtime.getRuntime().exec("logcat -v threadtime -b main -m 20000")
      val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))

      val log = StringBuilder()
      var line: String?
      while (bufferedReader.readLine().also { line = it } != null) {
         log.append(line)
         log.append("\n")
      }

      return writeStringToFileInLogsDir(log.toString(), fileName)
   }

   fun zipFiles(files: List<File>, fileId: String): File {
      if (files.isEmpty()) throw IllegalArgumentException("list of files empty in zipFiles()")

      val destZipPath = files[0].parentFile.absolutePath + File.separator + fileId
      ZipUtility().zip(files, destZipPath)
      return File(destZipPath)
   }

   /** Collect all SharedPreferences and write them to the given file name */
   fun writePreferences(fileName: String): File {

      val log = StringBuilder(" *** Shared Preferences ***\n")

      findAllSharedPreferences().forEach {
         val name = it.key
         val prefs = it.value
         log.append(String.format("\n\n%28s \n\n", "- - - $name"))
         prefs.all.forEach { pref ->
            log.append(String.format("%27s: %s \n", pref.key, pref.value))
         }
      }

      return writeStringToFileInLogsDir(log.toString(), fileName)
   }

   private fun findAllSharedPreferences(): Map<String, SharedPreferences> {
      val prefsDir = File(appContext.applicationInfo.dataDir, "shared_prefs")

      return if (prefsDir.exists() && prefsDir.isDirectory) {
         prefsDir.list()                                    // all the filenames in dir
            .filter { it.endsWith(".xml") }                 // take just .xml filesnames
            .filterNot { it.startsWith("ccu_tags") }        // donot add ccutags file
            .map { it.substring(0, it.length-4) }           // shave off suffix to get pref key
            .map { Pair(it, appContext.getSharedPreferences(it, Context.MODE_PRIVATE)) }   // get SharedPrefs for each name and make a Pair
            .toMap()                                        // convert List<Pair> to Map<Key, Value>
       } else {
         // empty map.  We could also choose to throw an exception here, but throwing exception is not a
         // preferred pattern in Kotlin, so we're going to leave this as is for now.
         mapOf()
      }
   }

   fun writeMessages(fileName: String): File {

      val log = StringBuilder("\n\n *** Message Status ***\n")

      val messageDatabaseHelper = MessageDatabaseHelper(RenatusDatabaseBuilder
                                    .getInstance(Globals.getInstance().applicationContext))

      val messageList = messageDatabaseHelper.getAllMessagesList()
      if (messageList.size < 5000) {
         messageList.forEach {
            message -> log.append("$message \n")
         }
      } else {
         log.append(" There has been ${messageList.size} messages. Which is unusually high for 24 hour period.")
      }

      return writeStringToFileInLogsDir(log.toString(), fileName)
   }

   private fun writeStringToFileInLogsDir(str: String, fileName: String): File {

      //Create txt file in SD Card
      if (!logDir.exists()) {
         logDir.mkdirs()
      }
      val file = File(logDir, fileName)

      //To write logcat in text file
      val writer = OutputStreamWriter(FileOutputStream(file))

      //Writing the string to file
      writer.write(str)
      writer.flush()
      writer.close()
      return file
   }

   fun getBacAppLogs(directoryName: String, logFileName: String): File? {
      val logsDir =
         Environment.getExternalStorageDirectory().absolutePath + File.separator + directoryName
      removeZipFiles(logsDir)
      val logFile =
         File(logsDir + File.separator + logFileName)

      if (!logFile.exists()) {
         Log.d(TAG_BAC_APP_LOGS, "----bac app log file not present");
         return null
      }
      return logFile
   }

   private fun removeZipFiles(folderPath: String) {
      val folder = File(folderPath)

      if (!folder.exists() || !folder.isDirectory) {
         Log.d(TAG_BAC_APP_LOGS, "The specified path is not a valid directory.")
         return
      }

      val zipFiles = folder.listFiles { _, name -> name.toLowerCase().endsWith(".zip") }
      zipFiles?.forEach { file ->
         try {
            if (file.delete()) {
               Log.d(TAG_BAC_APP_LOGS, "Deleted file: ${file.name}")
            } else {
               Log.d(TAG_BAC_APP_LOGS, "Failed to delete file: ${file.name}")
            }
         } catch (e: Exception) {
            Log.d(TAG_BAC_APP_LOGS, "Failed to delete file: ${file.name}, Error: ${e.message}")
         }
      }
   }

   fun writeHayStackEntities(fileName: String): File {

      val log = StringBuilder("\n\n *** Entities ***\n")

      val entityDatabaseHelper = EntityDatabaseHelper(RenatusDatabaseBuilder
         .getInstance(Globals.getInstance().applicationContext))

      val entityList = entityDatabaseHelper.getAllEntities()
      if (entityList.size < 15000) {
         entityList.forEach {
               entity -> log.append("$entity \n")
         }
      }

      log.append("\n\n *** Writable Arrays ***\n")

      val writableDatabaseHelper = WritableArrayDatabaseHelper(RenatusDatabaseBuilder
         .getInstance(Globals.getInstance().applicationContext))

      val writableArrayList = writableDatabaseHelper.getAllwritableArrays()
      if (writableArrayList.size < 15000) {
         writableArrayList.forEach {
               entity -> log.append("$entity \n")
         }
      }

      return writeStringToFileInLogsDir(log.toString(), fileName)
   }


}