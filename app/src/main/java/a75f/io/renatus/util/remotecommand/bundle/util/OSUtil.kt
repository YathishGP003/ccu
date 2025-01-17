package a75f.io.renatus.util.remotecommand.bundle.util

import a75f.io.logger.CcuLog
import a75f.io.renatus.RenatusApp
import android.content.pm.ApplicationInfo
import java.io.DataOutputStream
import java.io.IOException

class OSUtil {
    companion object {
        private val TAG = "CCU_BUNDLE_OSUTIL"
        fun executeAsRoot(
            commands: Array<String>
        ) {
            val thread = Thread {
                try {
                    // Do the magic
                    val appInfo = RenatusApp.getAppContext().applicationInfo
                    CcuLog.d(
                        TAG,
                        "ExecuteAsRoot===>rooted=" + RenatusApp.isRooted() + ", system flag=" + (appInfo.flags and ApplicationInfo.FLAG_SYSTEM)
                    )
                    if (RenatusApp.isRooted()) {
                        for (command in commands) {
                            val p = Runtime.getRuntime().exec("su")
                            val stdout = p.inputStream
                            val es = p.errorStream
                            val os =
                                DataOutputStream(p.outputStream)
                            CcuLog.d(
                                TAG,
                                "Executing command: '$command'"
                            )
                            os.writeBytes(command + "\n")
                            os.writeBytes("exit $?\n")
                            os.flush()
                            os.close()
                            var read: Int
                            val buffer = ByteArray(4096)
                            var errorOutput = ""

                            // Read stderr
                            while (es.read(buffer).also { read = it } > 0) {
                                errorOutput += String(buffer, 0, read)
                            }
                            errorOutput = errorOutput.trim { it <= ' ' }

                            // Read stdout
                            var stdOutput = ""
                            while (stdout.read(buffer).also { read = it } > 0) {
                                stdOutput += String(buffer, 0, read)
                            }
                            stdOutput = stdOutput.trim { it <= ' ' }
                            p.waitFor()
                            CcuLog.d(
                                TAG, String.format(
                                    "ExecuteAsRoot command status: %d %s",
                                    p.exitValue(),
                                    if (p.exitValue() == 0) "Success" else "*** FAILURE ***"
                                )
                            )
                            if (!stdOutput.isEmpty()) {
                                CcuLog.d(TAG, "ExecuteAsRoot stdout: $stdOutput")
                            }
                            if (!errorOutput.isEmpty()) {
                                CcuLog.d(TAG, "ExecuteAsRoot stderr: $errorOutput")
                            }
                        }
                        val appInfo2 = RenatusApp.getAppContext().applicationInfo
                        CcuLog.d(
                            TAG,
                            "RenatusAPP ExecuteAsRoot END===>" + (appInfo2.flags and ApplicationInfo.FLAG_SYSTEM)
                        )
                    } else {
                        // Two semicolons in case one of the commands is actually multiple commands separated by a semicolon
                        CcuLog.e(
                            TAG,
                            "Tablet is NOT rooted, unable to execute remote commands:" + java.lang.String.join(
                                ";; ",
                                *commands
                            )
                        )
                    }
                } catch (e: IOException) {
                    CcuLog.e(TAG, e.message)
                } catch (e: InterruptedException) {
                    CcuLog.e(TAG, e.message)
                }
            }
            thread.start()
        }
    }
}