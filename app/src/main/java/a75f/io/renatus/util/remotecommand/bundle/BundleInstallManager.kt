package a75f.io.renatus.util.remotecommand.bundle

import a75f.io.alerts.model.AlertCauses.Companion.BUNDLE_UPDATE_DEVICE_REBOOT
import a75f.io.alerts.model.AlertCauses.Companion.CCU_UPDATE
import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.diag.otastatus.BundleOtaStatus
import a75f.io.logic.diag.otastatus.OtaStatusDiagPoint.Companion.updateBundleOtaStatus
import a75f.io.logic.diag.otastatus.OtaStatusDiagPoint.Companion.updateBundleVersion
import a75f.io.logic.util.PreferenceUtil
import a75f.io.renatus.BuildConfig
import a75f.io.renatus.ENGG.AppInstaller.DOWNLOAD_BASE_URL
import a75f.io.renatus.RenatusApp
import a75f.io.renatus.registration.UpdateCCUFragment
import a75f.io.renatus.util.CCUUiUtil.UpdateAppRestartCause
import a75f.io.renatus.util.remotecommand.bundle.exception.DownloadFailedException
import a75f.io.renatus.util.remotecommand.bundle.models.APKVersion
import a75f.io.renatus.util.remotecommand.bundle.models.ArtifactDTO
import a75f.io.renatus.util.remotecommand.bundle.models.BundleDTO
import a75f.io.renatus.util.remotecommand.bundle.models.UpgradeBundle
import a75f.io.renatus.util.remotecommand.bundle.service.VersionManagementService
import a75f.io.renatus.util.remotecommand.bundle.util.PrivAppUpdater
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import com.google.gson.Gson
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import java.io.File
import java.lang.Thread.sleep
import java.util.Timer
import java.util.TimerTask
import kotlin.time.Duration.Companion.hours

interface BundleInstallListener {
    fun onBundleInstallMessage(installState: BundleInstallManager.BundleInstallState,
                               percentComplete: Int,
                               message: String)
}

/*** BundleInstallManager
 * The BundleInstallManager is responsible for managing the installation of OTA bundles
 * as well as manual bundle installations.  It supports callback messages in order to
 * manage UI updates during the installation process.
 *
 * It is a singleton class that is accessed via the getInstance() method
 */
class BundleInstallManager: BundleInstallListener {
    enum class ComponentCompatibility {
        NOT_IN_BUNDLE,
        MANUAL_UPDATE_REQUIRED,
        UPDATABLE,
        CURRENT
    }

    enum class BundleInstallState {
        IDLE,
        VALIDATING,
        DOWNLOADING,
        INSTALLING,
        COMPLETED,
        CANCELLED,
        DOWNLOAD_PAUSED,
        DOWNLOAD_FAILED,
        FAILED,
    }

    // Constants for the various components
    private val CCU = "CCU"
    private val REMOTE = "REMOTE"
    private val HOME = "HOME"
    private val BAC = "BAC"

    private var installSemaphore = Semaphore(1)
    var state: BundleInstallState = BundleInstallState.IDLE
        private set

    private val bundleInstallListeners: MutableList<BundleInstallListener> = mutableListOf()
    private val vmAPI = VersionManagementService(CCUHsApi.getInstance().getJwt())
    private var cancelPending = false

    // Singleton management
    companion object {
        const val TAG = "CCU_BUNDLE"
        val connectionTimeOut = 1.hours.inWholeSeconds
        var downloadPausedTime = 0L

        @Volatile
        private var instance: BundleInstallManager? = null
        private const val PREF_BUNDLE_OTA_STATUS = "bundle_ota_status"

        data class BundleInstallInfo(
            var bundleId: String,
            var bundleName: String,
            var rebootRequired: Boolean,
            var downloadedFiles: List<String>)

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: BundleInstallManager().also { instance = it }
            }

        private fun getSharedPreferences() = Globals.getInstance().applicationContext.getSharedPreferences("ccu_bundle_ota", Context.MODE_PRIVATE)

        /***
         * Called to persist the bundle installation state.   This is needed because the app restarts
         * in the middle of the install before the CCU ota point is updated, any downloaded files are cleaned up,
         * and before any required tablet reboots occur.  The basic flow will be as follows:
         * 1.  Persist the bundle installation state
         * 2.  Install the APK (which restarts the CCU app and interrupts the installation)
         * 3.  On restart, check for the persisted bundle installation state
         * 4.  If found, update the CCU ota point, clean up any downloaded files, and reboot the tablet
         * @param bundle - The UpgradeBundle object containing the bundle to upgrade to
         * @param rebootRequired - Flag indicating if a reboot is required after installation
         */
        private fun persistBundleInstallState(upgradeBundle: UpgradeBundle, rebootRequired: Boolean) {
            val files = upgradeBundle.componentsToUpgrade.map { it.fileName }.toCollection(mutableListOf())
            val gson = Gson()
            val otaStatus = gson.toJson(BundleInstallInfo(upgradeBundle.bundle.bundleId, upgradeBundle.bundle.bundleName, rebootRequired, files))
            getSharedPreferences()
                .edit().putString(PREF_BUNDLE_OTA_STATUS, otaStatus).commit()
        }

        private fun clearBundleInstallState() {
            getSharedPreferences()
                .edit().remove(PREF_BUNDLE_OTA_STATUS).commit()
        }

        private fun getBundleInstallState(): BundleInstallInfo? {
            val json = getSharedPreferences().getString(PREF_BUNDLE_OTA_STATUS, null)
            if (json != null) {
                val gson = Gson()
                return gson.fromJson(json, BundleInstallInfo::class.java)
            }
            return null
        }

        fun completeBundleInstallIfNecessary() {
            // Attempt to retrieve any stored bundle install state

            val bundleInstallState = getBundleInstallState()
            if (bundleInstallState != null) {
                // Update the CCU OTA status
                CcuLog.d(TAG, "completeBundleInstallIfNecessary: Writing bundle ota point")
                updateBundleOtaStatus(BundleOtaStatus.OTA_UPDATE_SUCCEEDED)
                updateBundleVersion(bundleInstallState.bundleName)
                try {
                    val syncResult = Runtime.getRuntime().exec("sync").waitFor()
                    if (syncResult != 0) {
                        CcuLog.e(TAG, "sync failed")
                    } else {
                        CcuLog.d(TAG, "sync success")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    CcuLog.e(TAG, "sync failed: " + e.message)
                }
                CcuLog.i(
                    L.TAG_CCU_BUNDLE,
                    "waiting for 5 minutes before checking for bundle install completion"
                )
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        CcuLog.i(
                            TAG,
                            "Five minutes have passed, checking for bundle install completion"
                        )
                        // Reset our bundle state so that we can't get into a loop
                        clearBundleInstallState()

                        // Finally, reboot if the bundle requires it (Home app was installed)
                        if (bundleInstallState.rebootRequired) {
                            // Reboot the tablet
                            CcuLog.d(TAG, "completeBundleInstallIfNecessary: Restarting tablet")
                            UpdateAppRestartCause(BUNDLE_UPDATE_DEVICE_REBOOT)
                            RenatusApp.rebootTablet()
                        }
                    }
                }, 300000)
            } else {
                CcuLog.d(TAG, "No bundle install state found, Bundle install might have been completed")
            }
        }
        fun initUpdatingSideAppsToRecommended() {
            CcuLog.i(L.TAG_CCU_BUNDLE, "Scheduled migrating side apps to recommended bundle after 1 minute")
            try {
                Timer().schedule(object : TimerTask() {
                    override fun run() {
                        val bundleInstallManager = getInstance()
                        val upgradeBundle = bundleInstallManager.getRecommendedUpgradeBundle(false)

                        if (upgradeBundle == null) {
                            CcuLog.i(L.TAG_CCU_BUNDLE, "Expected a recommended bundle, but none were returned")
                            return
                        }
                        PreferenceUtil.setSideAppsUpdateFinished()
                        // Remove CCU from the upgrade bundle
                        upgradeBundle.componentsToUpgrade.removeIf { it.fileName.contains("CCU") }
                        CcuLog.i(L.TAG_CCU_BUNDLE, "CCU removed from upgrade bundle")

                        bundleInstallManager.initiateBundleUpgrade(upgradeBundle, null)
                    }
                }, 60000)
            } catch (e: Exception) {
                CcuLog.e(L.TAG_CCU_BUNDLE, "Error while initiating bundle upgrade: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /***
     * Local listener for OTA bundle installation messages, logs each message received
     * @param installState - The current state of the bundle installation
     * @param percentComplete - The percentage of the installation that is complete
     * @param message - The message to send to the listeners
     */
    override fun onBundleInstallMessage(installState: BundleInstallState,
                                        percentComplete: Int,
                                        message: String) {
        CcuLog.d(TAG, "BundleInstallMessage: State: $installState, Percent complete: $percentComplete, Message: \"$message\"")
    }

    /***
     * Called to get the installed APK version for the given package
     * @param packageName - The name of the package to check
     * @return - The APKVersion object containing the installed version
     */
    private fun getPackageVersion(packageName: String): APKVersion? {
        try {
            val pm: PackageManager = RenatusApp.getAppContext().packageManager
            val pi: PackageInfo?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pi = pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pi = pm.getPackageInfo(packageName, 0)
            }

            if (pi == null) {
                return null
            }

            val version = APKVersion.extractVersion(pi.versionName)
            CcuLog.d(TAG, "getPackageVersion: $packageName versionName ${pi.versionName} resolved to $version")
            return APKVersion(version)
        } catch (e: Exception) {
            CcuLog.e(TAG, "Error getting package version for $packageName: ${e.message}")
            return null
        }
    }

    /***
     * Called to get the installed APK versions
     * @return - A map of the installed APK versions for each component
     */
    private fun getInstalledApkVersions() : Map<String, APKVersion?> {
        val ccuAppVersion = getPackageVersion(CCUApp.packageName)
        val bacAppVersion = getPackageVersion(BACApp.packageName) ?: getPackageVersion(BACApp.obseletePackageName)
        val remoteAppVersion = getPackageVersion(RemoteApp.packageName)
        val homeAppVersion = getPackageVersion(HomeApp.packageName) ?: getPackageVersion(HomeApp.oldPackageName)

        return mapOf(
            CCUApp.packageName to ccuAppVersion,
            BACApp.packageName to bacAppVersion,
            RemoteApp.packageName to remoteAppVersion,
            HomeApp.packageName to homeAppVersion,
        )
    }

    /***
     * Called to check the compatibility for the given app/artifact
     * @param appName - The name of the app to check
     * @param upgradeArtifact - The ArtifactDTO object containing the upgrade information
     * @param installedApkVersion - The APKVersion object containing the installed version
     * @return - A pair containing the ComponentCompatibility and a message
     */
    private fun checkBundleCompatibility(
        appName: String,
        upgradeArtifact: ArtifactDTO?,
        installedApkVersion: APKVersion?,
        isFreshCCU: Boolean,
        isReplace: Boolean
    ): Pair<ComponentCompatibility, String> {
        // If the bundle does not include something that is currently installed
        if (upgradeArtifact == null) {
            return Pair(ComponentCompatibility.NOT_IN_BUNDLE, "Bundle does not include an upgrade for the $appName App.")
        }

        // If the bundle includes something not currently installed
        if (installedApkVersion == null) {
            // Late requirement change, if component is not installed, we can install it
            return Pair(ComponentCompatibility.UPDATABLE, "$appName App is not currently installed and will be installed.")
        }

        // If the installed apk does not meet the minimum version requirement
        // But for Replace and fresh we dont need to check for minRequired.
        if (!(isFreshCCU || isReplace) && installedApkVersion < APKVersion(upgradeArtifact.minVersion)) {
            return Pair(ComponentCompatibility.MANUAL_UPDATE_REQUIRED, "The $appName App must be manually upgraded to minimum required version ${upgradeArtifact.minVersion} to install the bundle.")
        }

        // If the installed apk is already at the version (or greater) in the bundle
        // But for Replace we need to check for exact version.
        if ((!isReplace && installedApkVersion >= APKVersion(upgradeArtifact.version)) ||
            (isReplace && installedApkVersion == APKVersion(upgradeArtifact.version)))  {
            return Pair(
                ComponentCompatibility.CURRENT,
                "$appName App is already at version ${upgradeArtifact.version} and does not require upgrade."
            )
        }

        // Apk can be upgraded
        return Pair(ComponentCompatibility.UPDATABLE, "$appName App will be upgraded to version ${upgradeArtifact.version}.")
    }

    /***
     * Called to evaluate validity of the bundle for upgrade
     * @param bundleDTO - The BundleDTO object containing the bundle to upgrade to
     * @param caller - The caller/context of the function
     * @return - The UpgradeBundle object containing the bundle to upgrade to
     */
    private fun evaluateUpgradeBundle(bundleDTO: BundleDTO, caller: String,
                                      isReplace: Boolean = false, isFreshCCU: Boolean = false): UpgradeBundle {
        val upgradeBundle = UpgradeBundle(bundleDTO)

        val apkMap = getInstalledApkVersions()

        // Validate installed CCU app against bundle version
        val upgradeMessages = mutableListOf<String>()
        val (ccuCheck, ccuUpgradeInfo) = checkBundleCompatibility(
            CCU,
            bundleDTO.CCUArtifact,
            apkMap[CCUApp.packageName],
            isFreshCCU, isReplace
        )
        if (ccuCheck == ComponentCompatibility.UPDATABLE) {
            upgradeBundle.componentsToUpgrade.add(bundleDTO.CCUArtifact!!)
        } else if (ccuCheck == ComponentCompatibility.MANUAL_UPDATE_REQUIRED) {
            upgradeBundle.addErrorMessage(ccuUpgradeInfo)
        } else if (ccuUpgradeInfo.isNotEmpty()) {
            upgradeMessages.add(ccuUpgradeInfo)
        }

        // Validate installed BAC app against bundle version
        val (bacCheckOkay, bacUpgradeInfo) = checkBundleCompatibility(
            BAC,
            bundleDTO.BACArtifact,
            apkMap[BACApp.packageName],
            isFreshCCU,
            isReplace
        )
        if (bacCheckOkay == ComponentCompatibility.UPDATABLE) {
            upgradeBundle.componentsToUpgrade.add(bundleDTO.BACArtifact!!)
        } else if (bacCheckOkay == ComponentCompatibility.MANUAL_UPDATE_REQUIRED) {
            upgradeBundle.addErrorMessage(bacUpgradeInfo)
        } else if (bacUpgradeInfo.isNotEmpty()) {
            upgradeMessages.add(bacUpgradeInfo)
        }

        // Validate installed Remote app against bundle version
        val (remoteCheckOkay, remoteUpgradeInfo) = checkBundleCompatibility(
            REMOTE,
            bundleDTO.RemoteArtifact,
            apkMap[RemoteApp.packageName],
            isFreshCCU,
            isReplace
        )
        if (remoteCheckOkay == ComponentCompatibility.UPDATABLE) {
            upgradeBundle.componentsToUpgrade.add(bundleDTO.RemoteArtifact!!)
        } else if (remoteCheckOkay == ComponentCompatibility.MANUAL_UPDATE_REQUIRED) {
            upgradeBundle.addErrorMessage(remoteUpgradeInfo)
        } else if (remoteUpgradeInfo.isNotEmpty()) {
            upgradeMessages.add(remoteUpgradeInfo)
        }

        // Validate installed Home app against bundle version
        val (homeCheckOkay, homeUpgradeInfo) = checkBundleCompatibility(
            HOME,
            bundleDTO.HomeArtifact,
            apkMap[HomeApp.packageName],
            isFreshCCU,
            isReplace
        )
        if (homeCheckOkay == ComponentCompatibility.UPDATABLE) {
            upgradeBundle.componentsToUpgrade.add(bundleDTO.HomeArtifact!!)
        } else if (homeCheckOkay == ComponentCompatibility.MANUAL_UPDATE_REQUIRED) {
            upgradeBundle.addErrorMessage(homeUpgradeInfo)
        } else if (homeUpgradeInfo.isNotEmpty()) {
            upgradeMessages.add(homeUpgradeInfo)
        }

        CcuLog.i(TAG, "evaluateUpgradeBundle($caller): Bundle '${upgradeBundle.bundle.bundleName}' upgrade evaluation complete: okayToUpgrade: ${upgradeBundle.upgradeOkay}")
        upgradeBundle.errorMessages.forEach { CcuLog.i(TAG, "      - Error: $it") }
        upgradeMessages.forEach { CcuLog.i(TAG, "      - $it") }

        return upgradeBundle
    }

    /***
     * Called to retrieve the upgrade bundle by bundleId
     * @param bundleId - The bundleId to retrieve
     * @return - The UpgradeBundle object containing the bundle to upgrade to
     */
    fun getUpgradeBundleById(bundleId: String): UpgradeBundle? {
        /**
         * Calls the version-management API to find the specified bundle
         * Returns:
         *   - null - If the bundle is not found
         *   - UpgradeBundle object with upgradeOkay=true (rock and roll, proceed with the upgrade)
         *   - UpgradeBundle object with upgradeOkay=false - Cannot apply upgrade, reasons embedded in object.
         */

        // For local builds, no bundled upgrades
        if (BuildConfig.BUILD_TYPE == "local") {
            return null
        }

        try {
            // Retrieve the recommended bundle version from the cloud
            val recommendedBundleDTO = vmAPI.retrieveBundleByIdRetro(bundleId) ?: return null

            return evaluateUpgradeBundle(recommendedBundleDTO, "id: $bundleId")
        } catch (e: Exception) {
            CcuLog.e(TAG, "Error retrieving upgrade bundle by id: ${e.message}")
            return null
        }
    }

    /***
     * Called to retrieve the upgrade bundle by bundleName
     * @param bundleName - The bundleName to retrieve
     * @return - The UpgradeBundle object containing the bundle to upgrade to
     */
    fun getUpgradeBundleByName(bundleName: String?, isReplace : Boolean = false): UpgradeBundle? {
        // For local builds, no bundled upgrades
        if (BuildConfig.BUILD_TYPE == "local" || bundleName.isNullOrEmpty()) {
            return null
        }

        try {
            // Retrieve the recommended bundle version from the cloud
            val recommendedBundleDTO = vmAPI.retrieveBundleByName(bundleName) ?: return null

            return evaluateUpgradeBundle(recommendedBundleDTO, "name: $bundleName", isReplace = isReplace)
        } catch (e: Exception) {
            CcuLog.e(TAG, "Error retrieving upgrade bundle by name: ${e.message}")
            throw e
        }
    }

    /***
     * Called to retrieve the recommended upgrade bundle
     * @return - The recommended upgrade bundle, or null if no upgrade is available
     */
    fun getRecommendedUpgradeBundle(isFreshCCU: Boolean = false): UpgradeBundle? {
        /**
         * Calls the version-management API to find RECOMMENDED bundle versions
         * Returns:
         *   - null - If no upgrade is available OR required (components already meet or exceed bundle versions)
         *   - UpgradeBundle object with upgradeOkay=true (rock and roll, proceed with the upgrade)
         *   - UpgradeBundle object with upgradeOkay=false - Cannot apply upgrade, reasons embedded in object.
         */

        // For local builds, no bundled upgrades
        if (BuildConfig.BUILD_TYPE == "local") {
            return null
        }

        try {
            // Retrieve the recommended bundle version from the cloud
            val recommendedBundleDTO = vmAPI.retrieveRecommendedBundleRetro() ?: return null

            return evaluateUpgradeBundle(recommendedBundleDTO, "recommended", isFreshCCU = isFreshCCU)
        } catch (e: Exception) {
            CcuLog.e(TAG, "Error retrieving recommended upgrade bundle: ${e.message}")
            return null
        }
    }

    /***
     * Called to clear all bundle installation listeners
     */
    private fun clearBundleInstallListeners() {
        bundleInstallListeners.clear()
    }

    /***
     * Called to add a listener to receive bundle installation messages
     * @param listener - The listener to add
     */
    fun addBundleInstallListener(listener: BundleInstallListener) {
        bundleInstallListeners.add(listener)
    }

    /*** Called to send a message to all listeners
     * @param installState - The current state of the bundle installation
     * @param percentComplete - The percentage of the installation that is complete
     * @param message - The message to send to the listeners
     */
    private fun sendBundleInstallMessage(installState: BundleInstallState,
                                         percentComplete: Int,
                                         message: String) {
        bundleInstallListeners.forEach { it.onBundleInstallMessage(installState, percentComplete, message) }
    }

    /***
     * Called to cancel the bundle installation
     * Note: Only valid prior to actual installation (i.e. during validation or downloading)
     */
    fun cancelBundleInstallation() {
        when (state) {
            BundleInstallState.INSTALLING -> {
                throw Exception("Cancellation not allowed after downloads have completed")
            }
            BundleInstallState.VALIDATING, BundleInstallState.DOWNLOADING -> {
                cancelPending = true
            }
            else -> {
                // Nothing to do here, bundle installation not active
            }
        }
    }

    /***
     * Called to download the artifacts in the upgrade bundle
     * @param appName - The name of the app being downloaded
     * @param appNbr - The number of the app being downloaded
     * @param totalApps - The total number of apps to download
     * @param apkFile - The name of the APK file to download
     */
    @Synchronized
    private fun downloadFile(appName: String, appNbr: Int, totalApps: Int, apkFile: String) {
        CcuLog.i(TAG, "downloadFile: Checking for $apkFile")
        val file = File(RenatusApp.getAppContext().getExternalFilesDir(null), apkFile)
        if (file.exists()) {
            CcuLog.i(TAG, "downloadFile: ${file.absolutePath} already exists, skipping download")
            return
        }

        // Request file download
        val url = DOWNLOAD_BASE_URL + apkFile
        CcuLog.d(TAG, "downloadFile: Downloading $url")

        val manager =
            RenatusApp.getAppContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))

        request.setDescription("Downloading $apkFile software")
        request.setTitle("Downloading $apkFile app")
        request.allowScanningByMediaScanner()
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        request.setDestinationInExternalFilesDir(RenatusApp.getAppContext(), null, apkFile)

        val downloadId = manager.enqueue(request)
        CcuLog.d(TAG, "downloadFile: ...download id $downloadId, watching for completion")

        // Watch for download completion or failure
        var finishDownload = false
        var progress = 0
        while (!finishDownload) {
            // Handle any cancellation requests
            if (cancelPending) {
                CcuLog.d(TAG, "downloadFile: Cancel $appName download in progress")
                try {
                    manager.remove(downloadId)
                } catch (e: Exception) {
                    CcuLog.e(TAG, "downloadFile: Error cancelling download $downloadId: ${e.message}")
                }
                return
            }

            val cursor = manager.query(DownloadManager.Query().setFilterById(downloadId))
            if (cursor.moveToFirst()) {
                val statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status: Int = cursor.getInt(statusColumn)
                when (status) {
                    DownloadManager.STATUS_FAILED -> {
                        downloadPausedTime = 0
                        throw DownloadFailedException("Download failed")
                    }

                    DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING -> {
                        downloadPausedTime ++
                        CcuLog.i(TAG, "Download paused from ${downloadPausedTime / 60} minutes")

                        if (downloadPausedTime >= connectionTimeOut) {
                            CcuLog.i(
                                TAG, "Download paused from connection time out limit" +
                                        " ${connectionTimeOut / 60} minutes, so cancelling and  re-trying download"
                            )

                            downloadPausedTime = 0
                            manager.remove(downloadId)

                            downloadFile(
                                appName,
                                appNbr,
                                totalApps,
                                apkFile
                            )
                            finishDownload = true

                            val sizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                            val total: Long = cursor.getLong(sizeIndex)
                            if (total >= 0) {
                                val updatedProgress = getUpdatedProgress(cursor, appNbr, totalApps, total)
                                if (updatedProgress != progress) {
                                    progress = updatedProgress
                                    sendBundleInstallMessage(
                                        BundleInstallState.DOWNLOADING,
                                        progress,
                                        "(App $appNbr/$totalApps) Downloading $appName App..."
                                    )
                                }
                            }
                        } else {
                            sendBundleInstallMessage(
                                BundleInstallState.DOWNLOAD_PAUSED,
                                progress,
                                "(App $appNbr/$totalApps) Downloading $appName App..."
                            )
                        }

                    }

                    DownloadManager.STATUS_RUNNING -> {
                        downloadPausedTime = 0
                        val sizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        val total: Long = cursor.getLong(sizeIndex)
                        if (total >= 0) {
                            val updatedProgress = getUpdatedProgress(cursor, appNbr, totalApps, total)
                            if (updatedProgress != progress) {
                                progress = updatedProgress
                                sendBundleInstallMessage(BundleInstallState.DOWNLOADING, progress, "(App $appNbr/$totalApps) Downloading $appName App...")
                            }
                        }
                    }

                    DownloadManager.STATUS_SUCCESSFUL -> {
                        downloadPausedTime = 0
                        progress = 100
                        finishDownload = true
                    }
                }
            } else {
                CcuLog.i(TAG, "Download cursor not found")
                return
            }

            // Sleep a second so that we aren't just hammering the download manager
            // And sleeping for second also helps to increment the downloadPausedTime
            sleep(1000)
        }

        return
    }

    private fun getUpdatedProgress(cursor: Cursor, appNbr: Int, totalApps: Int, total: Long): Int {
        val bytesSoFar =
            cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
        val downloaded: Long = cursor.getLong(bytesSoFar)
        val updatedProgress =
            (((appNbr.toDouble() - 1F) / totalApps * 100F) + ((downloaded * 100F / total).toInt() / totalApps)).toInt()
        return updatedProgress
    }

    /***
     * Called to download the artifacts in the upgrade bundle
     * @param upgradeBundle - The UpgradeBundle object containing the bundle to upgrade to
     */
    private fun downloadArtifacts(upgradeBundle: UpgradeBundle) {
        state = BundleInstallState.DOWNLOADING
        sendBundleInstallMessage(state, 0, "Downloading artifacts")

        // Download artifacts
        upgradeBundle.componentsToUpgrade.forEachIndexed { index, component ->
            if (cancelPending) {
                CcuLog.d(TAG, "downloadArtifacts: Cancel pending, stopping download")
                return
            }
            try {
                // Write point to indicate component being downloaded
                when (component.target) {
                    CCU -> updateBundleOtaStatus(BundleOtaStatus.OTA_CCU_APP_DOWNLOADING)
                    BAC -> updateBundleOtaStatus(BundleOtaStatus.OTA_BAC_APP_DOWNLOADING)
                    REMOTE -> updateBundleOtaStatus(BundleOtaStatus.OTA_REMOTE_APP_DOWNLOADING)
                    HOME -> updateBundleOtaStatus(BundleOtaStatus.OTA_HOME_APP_DOWNLOADING)
                }

                // Download component apk
                downloadFile(
                    component.target,
                    index + 1,
                    upgradeBundle.componentsToUpgrade.size,
                    component.fileName
                )

                // Write point indicating component download succeeded
                when (component.target) {
                    CCU -> updateBundleOtaStatus(BundleOtaStatus.OTA_CCU_APP_DOWNLOAD_SUCCEEDED)
                    BAC -> updateBundleOtaStatus(BundleOtaStatus.OTA_BAC_APP_DOWNLOAD_SUCCEEDED)
                    REMOTE -> updateBundleOtaStatus(BundleOtaStatus.OTA_REMOTE_APP_DOWNLOAD_SUCCEEDED)
                    HOME -> updateBundleOtaStatus(BundleOtaStatus.OTA_HOME_APP_DOWNLOAD_SUCCEEDED)
                }
            } catch (e: Exception) {
                // Write point indicating component download failed
                when (component.target) {
                    CCU -> updateBundleOtaStatus(BundleOtaStatus.OTA_CCU_APP_DOWNLOAD_FAILED)
                    BAC -> updateBundleOtaStatus(BundleOtaStatus.OTA_BAC_APP_DOWNLOAD_FAILED)
                    REMOTE -> updateBundleOtaStatus(BundleOtaStatus.OTA_REMOTE_APP_DOWNLOAD_FAILED)
                    HOME -> updateBundleOtaStatus(BundleOtaStatus.OTA_HOME_APP_DOWNLOAD_FAILED)
                }

                val msg = "Error downloading ${component.target} apk"
                CcuLog.e(TAG, "downloadArtifacts: $msg, ${e.message}")
                sendBundleInstallMessage(state, 0, msg)
                throw DownloadFailedException(msg)
            }
        }
    }

    /***
     * Called to delete the artifacts after installation
     * @param upgradeBundle - The UpgradeBundle object containing the bundle to upgrade to
     */
    private fun deleteArtifacts(upgradeBundle: UpgradeBundle) {
        upgradeBundle.componentsToUpgrade.forEach { component ->
            val file = File(RenatusApp.getAppContext().getExternalFilesDir(null), component.fileName)
            if (file.exists()) {
                CcuLog.d(TAG, "deleteArtifacts: Deleting ${file.absolutePath}")
                file.delete()
            }
        }
    }

    /***
     * computes the task complete percent based on total number of apps and the current app
     * @appNbr - The current app number (beginning with 1)
     * @totalApps - The total number of apps to install
     */
    private fun computeTaskPercent(appNbr: Int, totalApps: Int): Int {
        return ((appNbr-1).toDouble()/totalApps.toDouble()*100F).toInt()
    }

    /***
     * Formats the installing message based on total number of apps and the current app
     * @appNbr - The current app number (beginning with 1)
     * @totalApps - The total number of apps to install
     * @appName - The name of the app being installed
     */
    private fun formatInstallingMessage(appNbr: Int, totalApps: Int, appName: String): String {
        return "(App $appNbr/$totalApps) Installing $appName App..."
    }

    /***
     * Called to verify that the required artifacts are present before installation
     * @param upgradeBundle - The UpgradeBundle object containing the bundle to upgrade to
     * @param appsToInstall - The list of apps to install
     * @throws Exception - If a required artifact is missing
     */
    private fun preInstallVerification(upgradeBundle: UpgradeBundle, appsToInstall: List<String>) {
        if (appsToInstall.contains(BAC)) {
            val file = File(
                RenatusApp.getAppContext().getExternalFilesDir(null),
                upgradeBundle.bundle.BACArtifact!!.fileName
            )
            if (!file.exists()) {
                updateBundleOtaStatus(BundleOtaStatus.OTA_BAC_APP_DOWNLOAD_FAILED)
                throw Exception("preInstallVerification: Required BAC APK ${file.absolutePath} does not exist")
            }
        }

        if (appsToInstall.contains(REMOTE)) {
            val file = File(
                RenatusApp.getAppContext().getExternalFilesDir(null),
                upgradeBundle.bundle.RemoteArtifact!!.fileName
            )
            if (!file.exists()) {
                updateBundleOtaStatus(BundleOtaStatus.OTA_REMOTE_APP_DOWNLOAD_FAILED)
                throw Exception("preInstallVerification: Required REMOTE APK ${file.absolutePath} does not exist")
            }
        }

        if (appsToInstall.contains(HOME)) {
            val file = File(
                RenatusApp.getAppContext().getExternalFilesDir(null),
                upgradeBundle.bundle.HomeArtifact!!.fileName
            )
            if (!file.exists()) {
                updateBundleOtaStatus(BundleOtaStatus.OTA_HOME_APP_DOWNLOAD_FAILED)
                throw Exception("preInstallVerification: Required HOME APK ${file.absolutePath} does not exist")
            }
        }

        if (appsToInstall.contains(CCU)) {
            val file = File(
                RenatusApp.getAppContext().getExternalFilesDir(null),
                upgradeBundle.bundle.CCUArtifact!!.fileName
            )
            if (!file.exists()) {
                updateBundleOtaStatus(BundleOtaStatus.OTA_CCU_APP_DOWNLOAD_FAILED)
                throw Exception("preInstallVerification: Required CCU APK ${file.absolutePath} does not exist")
            }
        }
    }

    /***
     * Called to install the artifacts in the upgrade bundle
     * @param upgradeBundle - The UpgradeBundle object containing the bundle to upgrade to
     * @throws Exception - If something fails during the installation process
     * If the install fails, an exception will be raised that is caught by the caller
     * and transformed into a BundleInstallState.FAILED state
     */
    private fun installArtifacts(upgradeBundle: UpgradeBundle) {
        state = BundleInstallState.INSTALLING
        sendBundleInstallMessage(state, 0, "Installing artifacts")

        val appsToInstall = upgradeBundle.componentsToUpgrade.map {it.target }

        // Final check before installing
        preInstallVerification(upgradeBundle, appsToInstall)

        val totalApps = appsToInstall.size
        var appNbr = 1
        var rebootRequired = false

        // Install BAC App
        if (appsToInstall.contains(BAC)) {
            CcuLog.d(TAG, "*********************** Installing BAC App ***********************")
            updateBundleOtaStatus(BundleOtaStatus.OTA_BAC_APP_INSTALLING)
            sendBundleInstallMessage(BundleInstallState.INSTALLING, computeTaskPercent(appNbr, totalApps), formatInstallingMessage(appNbr, totalApps, "BAC"))

            try {
                val bacapp = BACApp(upgradeBundle.bundle.BACArtifact!!.fileName)
                bacapp.installApp()
                updateBundleOtaStatus(BundleOtaStatus.OTA_BAC_APP_INSTALL_SUCCEEDED)
            } catch (e: Exception) {
                updateBundleOtaStatus(BundleOtaStatus.OTA_BAC_APP_INSTALL_FAILED)
                sendBundleInstallMessage(BundleInstallState.FAILED, computeTaskPercent(appNbr, totalApps), "BAC App installation failed")
                throw Exception("Error installing BAC App: ${e.message}")
            }

            appNbr++
        }

        // Install Remote App
        if (appsToInstall.contains(REMOTE)) {
            CcuLog.d(TAG, "*********************** Installing REMOTE App ***********************")
            updateBundleOtaStatus(BundleOtaStatus.OTA_REMOTE_APP_INSTALLING)

            sendBundleInstallMessage(BundleInstallState.INSTALLING, computeTaskPercent(appNbr, totalApps), formatInstallingMessage(appNbr, totalApps, "REMOTE"))

            try {
                val remoteapp = RemoteApp(upgradeBundle.bundle.RemoteArtifact!!.fileName)
                remoteapp.installApp()
                updateBundleOtaStatus(BundleOtaStatus.OTA_REMOTE_APP_INSTALL_SUCCEEDED)
            } catch (e: Exception) {
                updateBundleOtaStatus(BundleOtaStatus.OTA_REMOTE_APP_INSTALL_FAILED)
                sendBundleInstallMessage(BundleInstallState.FAILED, computeTaskPercent(appNbr, totalApps), "REMOTE App installation failed")
                throw Exception("Error installing REMOTE App: ${e.message}")
            }

            appNbr++
        }

        // Install Home App - if we install the home app, set a flag for reboot
        if (appsToInstall.contains(HOME)) {
            CcuLog.d(TAG, "*********************** Installing HOME App ***********************")
            updateBundleOtaStatus(BundleOtaStatus.OTA_HOME_APP_INSTALLING)
            sendBundleInstallMessage(BundleInstallState.INSTALLING, computeTaskPercent(appNbr, totalApps), formatInstallingMessage(appNbr, totalApps, "HOME"))

            try {
                val homeapp = HomeApp(upgradeBundle.bundle.HomeArtifact!!.fileName)
                homeapp.installApp()
                rebootRequired = true
                updateBundleOtaStatus(BundleOtaStatus.OTA_HOME_APP_INSTALL_SUCCEEDED)
            } catch (e: Exception) {
                updateBundleOtaStatus(BundleOtaStatus.OTA_HOME_APP_INSTALL_FAILED)
                sendBundleInstallMessage(BundleInstallState.FAILED, computeTaskPercent(appNbr, totalApps), "HOME App installation failed")
                throw Exception("Error installing HOME App: ${e.message}")
            }

            appNbr++
        }

        // Install CCU App
        if (appsToInstall.contains("CCU")) {
            CcuLog.d(TAG, "*********************** Installing CCU App ***********************")
            updateBundleOtaStatus(BundleOtaStatus.OTA_CCU_APP_INSTALLING)
            sendBundleInstallMessage(BundleInstallState.INSTALLING, computeTaskPercent(appNbr, totalApps), formatInstallingMessage(appNbr, totalApps, "CCU"))
            try {
                val ccuapp = CCUApp(upgradeBundle.bundle.CCUArtifact!!.fileName)

                // The following will persist everything that we need to continue
                // the install after a CCU restart.  When the CCU apk is installed via
                // ccuapp.installApp(), it is HIGHLY likely that the app will restart
                persistBundleInstallState(upgradeBundle, rebootRequired)

                ccuapp.installApp()

                // It is highly unlikely that the following line will NOT get hit
                updateBundleOtaStatus(BundleOtaStatus.OTA_CCU_APP_INSTALL_SUCCEEDED)
            } catch (e: Exception) {
                // We clear this state because it will cause the bundled install to be 'completed',
                // which is not the case here, we are in a failure mode
                clearBundleInstallState()
                updateBundleOtaStatus(BundleOtaStatus.OTA_CCU_APP_INSTALL_FAILED)
                sendBundleInstallMessage(BundleInstallState.FAILED, computeTaskPercent(appNbr, totalApps), "CCU App installation failed")
                throw Exception("Error installing HOME App: ${e.message}")
            }
        }

        state = BundleInstallState.COMPLETED
        updateBundleOtaStatus(BundleOtaStatus.OTA_UPDATE_SUCCEEDED)
        sendBundleInstallMessage(state, 100, "Bundle installation complete")
    }

    /***
     * Called when an "ota_update_bundle" command is received
     * @param bundleId - The bundleId to upgrade to
     */
    fun processOTARemoteCommand(bundleId: String) {
        CcuLog.d(TAG, "processOTARemoteCommand: Received OTA bundle command for bundleId $bundleId")
        try {
            val bundle = getUpgradeBundleById(bundleId)
            if (bundle == null || !bundle.upgradeOkay) {
                CcuLog.d(TAG, "processOTARemoteCommand: Bundle $bundleId not applied")
                return
            }

            initiateBundleUpgrade(bundle, this)
        } catch (e: Exception) {
            CcuLog.e(TAG, "processOTARemoteCommand: Error processing OTA bundle command: ${e.message}")
        }
    }

    /***
     * Called to perform the actual bundle upgrade, called from 2 places
     *     - processOTARemoteCommand - When an OTA bundle command is received
     *     - (externally) - When a manual bundle upgrade is initiated
     *
     *     @param upgradeBundle - The UpgradeBundle object containing the bundle to upgrade to
     *     @param listener - Optional listener to receive progress updates
     *     @return - Error message if the upgrade failed, null if successful
     */
    fun initiateBundleUpgrade(upgradeBundle: UpgradeBundle, listener: BundleInstallListener?=null): String? {
        CcuLog.d(TAG, "initiateBundleUpgrade: $upgradeBundle")
        cancelPending = false
        try {
            if (!installSemaphore.tryAcquire()) {
                return "Bundle installation already in progress"
            }

            updateBundleOtaStatus(BundleOtaStatus.OTA_REQUEST_RECEIVED)
            state = BundleInstallState.VALIDATING
            if (!upgradeBundle.upgradeOkay) {
                updateBundleOtaStatus(BundleOtaStatus.OTA_REQUEST_VALIDATION_FAILED)
                state = BundleInstallState.FAILED
                val msg = upgradeBundle.errorMessages.joinToString("\n\n")
                sendBundleInstallMessage(state, 0, msg)
                return msg
            }

            // Add listener if provided
            clearBundleInstallListeners()
            if (listener != null) {
                addBundleInstallListener(listener)
            }

            val appsToInstall = upgradeBundle.componentsToUpgrade.map { it.target }
            CcuLog.i( TAG, "initiateBundleUpgrade: The following components will be upgraded: $appsToInstall")

            // Establish reboot/restart requirements
            val restartTablet =
                appsToInstall.contains(HOME)  // This will be set to true if the HOME app gets installed
            val restartCCU =
                appsToInstall.contains(CCU)   // This will be set to true if the CCU app gets installed (should always be installed)

            // Download artifacts
            try {
                downloadArtifacts(upgradeBundle)
            } catch (e: Exception) {
                state = BundleInstallState.DOWNLOAD_FAILED
                val msg = e?.message ?: "Error downloading artifacts, $e"
                sendBundleInstallMessage(state, 0, msg)
                return msg
            }

            // Install artifacts if the user has not cancelled
            if (!cancelPending) {
                try {
                    state = BundleInstallState.INSTALLING
                    installArtifacts(upgradeBundle)
                } catch (e: Exception) {
                    state = BundleInstallState.FAILED
                    CcuLog.e(TAG, "Error installing artifacts: ${e.message}")
                    val msg = "$REMOTE App installation failed"
                    sendBundleInstallMessage(state, 0, msg)
                    return msg
                }
            }

            // Delete artifacts
            try {
                deleteArtifacts(upgradeBundle)
            } catch (e: Exception) {
                CcuLog.d(TAG, "Error deleting artifacts: ${e.message}, not failing the upgrade")
            }

            // If the user canceled the installation, return
            if (cancelPending) {
                updateBundleOtaStatus(BundleOtaStatus.OTA_UPDATE_CANCELLED)
                state = BundleInstallState.CANCELLED
                sendBundleInstallMessage(state, 0, "Bundle installation cancelled")
                return "Bundle installation cancelled"
            }

            updateBundleOtaStatus(BundleOtaStatus.OTA_UPDATE_SUCCEEDED)

            // If the bundle contains a CCU app, only then update the bundle version
            if (upgradeBundle.componentsToUpgrade.any { it.fileName.contains("CCU") }) {
                updateBundleVersion(upgradeBundle.bundle.bundleName)
            } else {
                CcuLog.d(TAG, "initiateBundleUpgrade: No CCU app in bundle, skipping bundle version update")
            }

            state = BundleInstallState.COMPLETED
            sendBundleInstallMessage(state, 100, "Bundle installation complete")

            // Everything has completed successfully, deal with any restart/reboot requirements
            if (restartTablet) {
                CcuLog.d(TAG, "initiateBundleUpgrade: Restarting tablet")
                UpdateAppRestartCause(BUNDLE_UPDATE_DEVICE_REBOOT)
                RenatusApp.rebootTablet()
            } else if (restartCCU) {
                CcuLog.d(TAG, "initiateBundleUpgrade: Restarting CCU")
                UpdateAppRestartCause(CCU_UPDATE)
                RenatusApp.restartApp()
            }

        } catch(e: Exception) {
            val msg = "Exception: ${e.message}"
            CcuLog.e(TAG, "initiateBundleUpgrade: $msg")
            state = BundleInstallState.FAILED
            sendBundleInstallMessage(state, 0, msg)
            return msg
        } finally {
            CcuLog.d(TAG, "initiateBundleUpgrade: Removing listeners and freeing semaphore")
            installSemaphore.release()
        }
        return null
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun startApkDownloads(
        activity: Activity,
        apkList: List<String>,
        bSilent: Boolean,
        currentFragment: UpdateCCUFragment
    ) {
        val downloadIds = mutableMapOf<Long, File>()
        val successfulDownloads = mutableSetOf<Long>()
        val context = activity.applicationContext
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val finishedDownloads = mutableSetOf<Long>()
        apkList.forEach { apkFile ->
            val file = File(context.getExternalFilesDir(null), apkFile)
            val url = "$DOWNLOAD_BASE_URL$apkFile"

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("Downloading $apkFile")
                setDescription("Downloading $apkFile")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(context, null, apkFile)
            }

            val downloadId = downloadManager.enqueue(request)
            downloadIds[downloadId] = file
            CcuLog.d(L.TAG_CCU_DOWNLOAD, " Started download for $apkFile (ID: $downloadId)")

            // Check download status until it completes
            while (true) {
                sleep(1000)

                val query = DownloadManager.Query().setFilterById(downloadId)
                downloadManager.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                        val fileName = file.name
                        val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                val totalSizeBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                                val downloadSizeMB = if (totalSizeBytes > 0) {
                                    String.format("%.2f", totalSizeBytes.toDouble() / (1024 * 1024)).toDouble()
                                } else 0.0

                                CcuLog.i(L.TAG_CCU_REPLACE, " Success: $fileName")
                                successfulDownloads.add(downloadId)
                                finishedDownloads.add(downloadId)

                                currentFragment.setReplaceProgress(
                                    100,
                                    downloadId,
                                    cursor.getInt(columnIndex),
                                    downloadSizeMB,
                                    currentFragment
                                )
                                return@forEach
                            }

                            DownloadManager.STATUS_FAILED -> {
                                val reasonText = getDownloadFailureReason(reason)
                                CcuLog.e(L.TAG_CCU_REPLACE, " Failed: $fileName. Reason: $reasonText")
                                finishedDownloads.add(downloadId)
                                return@forEach
                            }

                            DownloadManager.STATUS_PENDING,
                            DownloadManager.STATUS_PAUSED -> {
                                val reasonText = getPendingOrPausedReason(reason)
                                CcuLog.w(L.TAG_CCU_REPLACE, " Waiting: $fileName. Reason: $reasonText. Retrying in 5s.")
                                sleep(5000)
                            }

                            else -> {
                                val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                                val progressPercent = if (bytesTotal > 0) (bytesDownloaded * 100L / bytesTotal).toInt() else 0
                                val downloadSizeMB = if (bytesTotal > 0) {
                                    String.format("%.2f", bytesTotal.toDouble() / (1024 * 1024)).toDouble()
                                } else 0.0
                                val downloadedMB = String.format("%.2f", bytesDownloaded.toDouble() / (1024 * 1024))

                                CcuLog.i(
                                    L.TAG_CCU_REPLACE,
                                    " Downloaded: $downloadedMB MB / $downloadSizeMB MB ($progressPercent%) for $fileName (ID: $downloadId)"
                                )

                                if (progressPercent > 0 || downloadSizeMB > 0.0) {
                                    currentFragment.setReplaceProgress(
                                        progressPercent,
                                        downloadId,
                                        status,
                                        downloadSizeMB,
                                        currentFragment
                                    )
                                }
                                sleep(2000)
                            }
                        }
                    } else {
                        CcuLog.w(L.TAG_CCU_REPLACE, " No download info for ID: $downloadId")
                        finishedDownloads.add(downloadId)
                        return@forEach
                    }
                }
            }

        }

        CcuLog.i(
            L.TAG_CCU_REPLACE,
            "Download process completed. " +
                    "Total downloads: ${downloadIds.size}," +
                    " Successful: ${successfulDownloads.size}," +
                    " Finished: ${finishedDownloads.size}"
        )

        val allDownloadsDone = downloadIds.keys.size == finishedDownloads.size
        val hasSuccessfulDownload = successfulDownloads.isNotEmpty()

        if (allDownloadsDone && hasSuccessfulDownload) {
            val allEntries = successfulDownloads.map { id ->
                id to downloadIds[id]
            }

            var isHomeAppPresent = false
            var isCCUAppPresent = false
            var homeAppName = ""
            var ccuAppName = ""
            allEntries.forEachIndexed { index, (id, file) ->
                val apkName = file?.name ?: "Unknown"

                when {
                    apkName.contains("HomeApp") -> {
                        isHomeAppPresent = true
                        homeAppName = apkName
                        return@forEachIndexed
                    }

                    apkName.contains("CCU") -> {
                        isCCUAppPresent = true
                        ccuAppName = apkName
                        return@forEachIndexed
                    }

                    apkName.contains("RemoteApp") -> {
                        CcuLog.i(L.TAG_CCU_REPLACE, "Installing RemoteApp: $apkName (ID: $id)")
                        RemoteApp(apkName).installApp()
                    }

                    apkName.contains("BACapp") -> {
                        CcuLog.i(L.TAG_CCU_REPLACE, "Installing BACApp: $apkName (ID: $id)")
                        BACApp(apkName).installApp()
                    }
                }
            }

            CcuLog.i(L.TAG_CCU_REPLACE,"Home app present: $isHomeAppPresent - fileName $homeAppName" +
                    "\nCCU app present: $isCCUAppPresent - fileName $ccuAppName")
            GlobalScope.launch(Dispatchers.Main) {
                if(isHomeAppPresent && isCCUAppPresent){
                    CcuLog.i(L.TAG_CCU_REPLACE, "Both Home and CCU apps are present, setting reboot flag")
                   PreferenceUtil.setIsRebootRequiredAfterReplaceFlag(true)
                }
                if (isHomeAppPresent) {
                    CcuLog.i(L.TAG_CCU_REPLACE, "Installing.... $homeAppName")
                    HomeApp(homeAppName).installApp()
                }
                if (isCCUAppPresent) {
                    CcuLog.i(L.TAG_CCU_REPLACE, "Installing.... $ccuAppName")
                    val versionsListFromPrivAppToRemove = PrivAppUpdater.getVersionsToRemoveFromPrivApp(context, ccuAppName)
                    if (versionsListFromPrivAppToRemove.isNotEmpty()) {
                        if (PrivAppUpdater.isDBDeleteRequired(ccuAppName)) {
                            PrivAppUpdater.deleteRoomDb()
                        }
                        CCUApp(ccuAppName, versionsListFromPrivAppToRemove).installApp()
                    } else {
                        CCUApp(ccuAppName).installApp()
                    }
                }
                delay(3000)
                if (isHomeAppPresent) {
                    RenatusApp.rebootTablet()
                } else {
                    RenatusApp.restartApp()
                }
            }

        }
    }

    private fun getDownloadFailureReason(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> "Cannot resume"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Device not found"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "File exists"
            DownloadManager.ERROR_FILE_ERROR -> "File error"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "HTTP data error"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "No space"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Too many redirects"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Unhandled HTTP"
            DownloadManager.ERROR_UNKNOWN -> "Unknown error"
            else -> "Code: $reason"
        }
    }

    private fun getPendingOrPausedReason(reason: Int): String {
        return when (reason) {
            DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Queued for WiFi"
            DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Waiting for network"
            DownloadManager.PAUSED_WAITING_TO_RETRY -> "Waiting to retry"
            DownloadManager.PAUSED_UNKNOWN -> "Paused: Unknown reason"
            else -> "Reason code: $reason"
        }
    }
}



