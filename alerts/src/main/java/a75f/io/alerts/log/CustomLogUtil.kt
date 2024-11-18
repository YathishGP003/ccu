package a75f.io.alerts.log

import a75f.io.logger.CcuLog
import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.IOException

class CustomLogUtil {
    companion object {
        const val LOGS_DIR_SEQUENCER = "ccu/sequencer"
        const val LOGS_DIR_ALERTS = "ccu/alerts"

        fun dumpLogs(context: Context, fileName: String?, jsonString: String, logDirectory: File, tag : String) {

            if (!logDirectory.exists()) {
                logDirectory.mkdirs()
            }

            val file = File(logDirectory, fileName)
            var fileWriter: FileWriter? = null
            try {
                // Write the JSON string to the file
                fileWriter = FileWriter(file)
                fileWriter.write(jsonString)
                fileWriter.flush()
                CcuLog.d(tag, "File written successfully: " + file.absolutePath)
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

        fun deleteJsonFile(context: Context, fileName: String?, logDirectory: File): Boolean {
            if (logDirectory.exists()) {
                // Get the file from internal storage
                val file = File(logDirectory, fileName)
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