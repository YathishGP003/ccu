package a75f.io.renatus.util.remotecommand.bundle

import a75f.io.logic.L

class BACApp(fileName: String) : BaseBundleApp(fileName) {
    companion object {
        val packageName = L.BAC_APP_PACKAGE_NAME
        val obseletePackageName = L.BAC_APP_PACKAGE_NAME_OBSOLETE
    }

    init {
        this.instructions = listOf(
            Instruction("pm uninstall --user 0 $obseletePackageName", false),
            Instruction("pm uninstall --user 0 $packageName", false),
            Instruction("pm install -r $fullDownloadPath"),
        )
    }
}