package a75f.io.renatus.util.remotecommand.bundle

import a75f.io.logic.L

class HomeApp(fileName: String) : BaseBundleApp(fileName) {
    companion object {
        val packageName = L.HOME_APP_PACKAGE_NAME
        val oldPackageName = L.HOME_APP_PACKAGE_NAME_OBSOLETE
    }

    init {
        this.instructions = listOf(
            Instruction("mount -o rw,remount /system"),
            Instruction("pm uninstall --user 0 $packageName", false),
            Instruction("rm -f /system/priv-app/75fHome*.apk", false),
            Instruction("pm uninstall --user 0 $oldPackageName", false),
            Instruction("rm -f /system/priv-app/Home*.apk", false),
            Instruction("mv $fullDownloadPath /system/priv-app/"),
            Instruction("chmod 644 /system/priv-app/$fileName"),
            Instruction("chown root.root /system/priv-app/$fileName"),
            Instruction("pm install -r -d -g --user 0 /system/priv-app/$fileName", false),
            Instruction("cmd package set-home-activity --user 0 \"$packageName/.MainActivity\"", false),
            Instruction("am start $packageName", false),
        )
    }
}