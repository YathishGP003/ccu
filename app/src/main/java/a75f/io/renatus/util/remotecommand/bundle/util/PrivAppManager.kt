package a75f.io.renatus.util.remotecommand.bundle.util

import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L.TAG_CCU_DOWNLOAD
import a75f.io.renatus.util.CCUUiUtil
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import java.io.File

object PrivAppUpdater {
    private const val PRIV_APP_DIR = "/system/priv-app"

    private fun getApkVersionCode(context: Context, apkFile: File): Long? {
        val pm = context.packageManager
        val pkgInfo: PackageInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, 0) ?: return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pkgInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pkgInfo.versionCode.toLong()
        }
    }

    private fun listPrivAppApks(newApkName: String): List<File> {
        val dir = File(PRIV_APP_DIR)
        return dir.listFiles { f ->
            f.name.endsWith(".apk") && f.name.startsWith("CCU", ignoreCase = true)
                    && !f.name.equals(newApkName)
        }?.toList() ?: emptyList()
    }

    /**
     * Ensures priv-app only contains the new APK if a higher version already exists.
     * @param context Android context
     * @param newApkName Full path to the new APK file (on device, e.g. /sdcard/Download/CCU_4.3.1.apk)
     * @return true if APK was pushed, false if skipped
     */
    fun getVersionsToRemoveFromPrivApp(
        context: Context,
        newApkName: String
    ): List<Pair<File, Long>> {
        val newApk = File(context.getExternalFilesDir(null), newApkName)
        val newVer = getApkVersionCode(context, newApk)
        if (newVer == null) {
            CcuLog.e(TAG_CCU_DOWNLOAD, "Cannot read version from new APK: $newApkName")
            return emptyList()
        }

        val privApks = listPrivAppApks(newApkName)
        if (privApks.isEmpty()) {
            CcuLog.d(TAG_CCU_DOWNLOAD, "No APKs in priv-app. Skipping.")
            return emptyList()
        }

        // Check if priv-app has higher version
        val privVersions = privApks.mapNotNull { f ->
            getApkVersionCode(context, f)?.let { Pair(f, it) }
        }

        if (privVersions.isEmpty()) {
            CcuLog.d(TAG_CCU_DOWNLOAD, "No valid APKs in priv-app. Skipping.")
            return emptyList()
        }

        val highest = privVersions.maxByOrNull { it.second }!!
        val highestVer = highest.second
        val highestFile = highest.first

        return if (highestVer > newVer) {
            CcuLog.d(
                TAG_CCU_DOWNLOAD,
                "Priv-app has higher version (${highestFile.name} : $highestVer) > New: $newVer"
            )
            privVersions
        } else {
            CcuLog.d(TAG_CCU_DOWNLOAD, "No higher version in priv-app. Skipping push.")
            return emptyList()
        }
    }

    fun isDBDeleteRequired(ccuAppName: String): Boolean {
        val requiredSplitVersion = ccuAppName.split("_")

        val requiredCCUAppVersion = requiredSplitVersion[requiredSplitVersion.size - 1]

        // we need delete DB only if replacing CCU version should be less than 4.3.2
        return CCUUiUtil.isCurrentVersionHigherOrEqualToRequired("4.3.2", requiredCCUAppVersion)
    }

    fun deleteRoomDb() {
        Globals.getInstance().applicationContext.deleteDatabase("renatusDb")
        CcuLog.i(TAG_CCU_DOWNLOAD , "Deleted DB")
    }
}
