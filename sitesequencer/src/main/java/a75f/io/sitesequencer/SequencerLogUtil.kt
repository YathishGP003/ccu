package a75f.io.sitesequencer

import a75f.io.alerts.log.SequenceLogs
import a75f.io.logger.CcuLog
import android.content.Context
import android.os.Environment
import com.google.gson.Gson
import java.io.File
import java.io.FileWriter
import java.io.IOException

class SequencerLogUtil {
    companion object {
        private val TAG = "CCU_SITE_SEQUENCER"

        private const val LOGS_DIR = "ccu/sequencer"
        private val logDir =
            File(Environment.getExternalStorageDirectory().absolutePath + File.separator + LOGS_DIR)

        fun dumpLogs(context: Context, fileName: String?, sequenceLogs: SequenceLogs) {
            val gson = Gson()
            val jsonString = gson.toJson(sequenceLogs)
            //val file = File(context.filesDir, fileName)

            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            val file = File(logDir, fileName)
            var fileWriter: FileWriter? = null
            try {
                // Write the JSON string to the file
                fileWriter = FileWriter(file)
                fileWriter.write(jsonString)
                fileWriter.flush()
                CcuLog.d(TAG, "File written successfully: " + file.absolutePath)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                if (fileWriter != null) {
                    try {
                        fileWriter.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        fun deleteJsonFile(context: Context, fileName: String?): Boolean {
            if (logDir.exists()) {
                // Get the file from internal storage
                val file = File(logDir, fileName)
                //val file = File(context.filesDir, fileName)

                // Check if the file exists and then delete it
                return if (file.exists()) {
                    file.delete() // This will return true if deletion is successful
                } else {
                    false // File doesn't exist, return false
                }
            }
            return false
        }
    }
}