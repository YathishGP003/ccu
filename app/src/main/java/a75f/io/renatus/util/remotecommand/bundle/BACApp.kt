package a75f.io.renatus.util.remotecommand.bundle

import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.renatus.util.CCUUtils

class BACApp(fileName: String) : BaseBundleApp(fileName) {
    companion object {
        val packageName = L.BAC_APP_PACKAGE_NAME
        val obseletePackageName = L.BAC_APP_PACKAGE_NAME_OBSOLETE
    }

    init {
        this.instructions = listOfNotNull(
            Instruction("pm uninstall --user 0 $obseletePackageName", false),
            if (CCUUtils.isBacAppVersionNeedsToUninstall(CCUUtils.getInstalledBacAppVersion().first)) {
                CcuLog.d(L.TAG_CCU_BUNDLE, "Uninstalling the BACapp")
                Instruction("pm uninstall --user 0 $packageName", false)
            } else {
                CcuLog.d(L.TAG_CCU_BUNDLE, "Uninstalling older BacApp is not required \n Because BAC app is already installed with version above 3.2.18")
                null
            },
            Instruction("pm install -r -d -g $fullDownloadPath"),
        )
    }
}