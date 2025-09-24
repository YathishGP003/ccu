package a75f.io.renatus.util.remotecommand.bundle

import a75f.io.logger.CcuLog
import a75f.io.renatus.RenatusApp
import android.content.pm.ApplicationInfo
import java.io.DataOutputStream
import java.io.File
import java.io.IOException

open class BaseBundleApp(val fileName: String) {
    class Instruction(val command: String, val fatal: Boolean=true, val successStatus: Int = 0)
    var instructions: List<Instruction> = listOf()
    val TAG = "CCU_BUNDLE"
    fun getInstallInstructionCommands(stripAnnotations: Boolean = false): List<String> {
        return instructions.map { it.command }
    }

    fun getInstallInstructions(stripAnnotations: Boolean = false): List<Instruction> {
        return instructions
    }

    val fullDownloadPath: String
        get() = File(RenatusApp.getAppContext().getExternalFilesDir(null), fileName).absolutePath


    private fun executeAsRoot(
        instructions: List<Instruction>
    ) {
        try {
            // Do the magic
            val appInfo = RenatusApp.getAppContext().applicationInfo
            CcuLog.d(TAG, "ExecuteAsRoot===>rooted=" + RenatusApp.isRooted() + ", system flag=" + (appInfo.flags and ApplicationInfo.FLAG_SYSTEM))

            if (RenatusApp.isRooted()) {
                for (instruction in instructions) {
                    // Run instructions 1 at a time so that we can handle fatal errors
                    val p = Runtime.getRuntime().exec("su")
                    val stdout = p.inputStream
                    val es = p.errorStream
                    val os =
                        DataOutputStream(p.outputStream)
                    CcuLog.d(TAG, "Executing command: '${instruction.command}'")

                    os.writeBytes(instruction.command + "\n")
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

                    CcuLog.d(TAG, "     command status: ${p.exitValue()} ${if (p.exitValue() == 0) "Success" else "*** FAILURE ***"}")

                    if (stdOutput.isNotEmpty()) {
                        CcuLog.d(TAG, "     stdout: $stdOutput")
                    }
                    if (errorOutput.isNotEmpty()) {
                        CcuLog.d(TAG, "     stderr: $errorOutput")
                    }

                    if (p.exitValue() != instruction.successStatus) {
                        if (instruction.fatal) {
                            val msg = "     command failed with status ${p.exitValue()} (FATAL ERROR)"
                            CcuLog.e(TAG, msg)
                            CcuLog.e(TAG, "     *** FATAL ERROR detected, aborting further commands ***")
                            throw Exception(msg)
                        } else {
                            val msg = "     command failed with status ${p.exitValue()} (Ignoring NON-FATAL ERROR)"
                            CcuLog.e(TAG, msg)
                        }
                    }
                }
                CcuLog.d(TAG, "RenatusAPP ExecuteAsRoot END")
            } else {
                // Two semicolons in case one of the commands is actually multiple commands separated by a semicolon
                CcuLog.e(TAG, "Tablet is NOT rooted, unable to execute remote commands:" +
                         java.lang.String.join(";; ", instructions.map { it.command })
                )
            }
        } catch (e: IOException) {
            CcuLog.e(TAG, e.toString())
        } catch (e: InterruptedException) {
            CcuLog.e(TAG, e.toString())
        }
    }

    fun installApp() {
        executeAsRoot(instructions)
    }
}