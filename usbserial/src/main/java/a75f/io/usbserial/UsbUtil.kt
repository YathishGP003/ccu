package a75f.io.usbserial

import a75f.io.api.haystack.CCUTagsDb
import a75f.io.logger.CcuLog
import android.os.Environment
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UsbUtil {
    companion object {
        private const val LOGS_DIR = "RenatusLogs"
        private const val MAX_FILE_SIZE = 10 * 1024 // 10 KB
        private const val LINES_TO_KEEP = 100

        private val logDir: File by lazy {
            File(Environment.getExternalStorageDirectory().absolutePath + File.separator + LOGS_DIR)
        }

        @JvmStatic
        fun writeUsbEvent(message: String) {
            val logFile = File(logDir, "Renatus_USB_Events_Log.txt")
            if (logFile.exists() && logFile.length() > MAX_FILE_SIZE) {
                removeOlderUsbEvents(logFile)
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logMessage = "$timestamp: $message\n"
            CcuLog.d(CCUTagsDb.TAG_CCU_USB_EVENT, logMessage)
            try {
                logFile.appendText(logMessage)
            } catch (e: Exception) {
                e.printStackTrace() // Handle the error appropriately
            }
        }

        private fun removeOlderUsbEvents(logFile: File) {
            val tempFile = File(logFile.parent, "${logFile.name}.tmp")
            val linesToKeep = mutableListOf<String>()

            try {
                logFile.bufferedReader().useLines { lines ->
                    linesToKeep.addAll(lines.drop(LINES_TO_KEEP))
                }

                tempFile.bufferedWriter().use { writer ->
                    linesToKeep.forEach { writer.write("$it\n") }
                }

                if (logFile.delete()) {
                    tempFile.renameTo(logFile)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}