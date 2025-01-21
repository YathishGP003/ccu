package a75f.io.renatus.util.remotecommand.bundle

class CCUApp(fileName: String): BaseBundleApp(fileName) {
    companion object {
        val packageName = "a75f.io.renatus"
    }

    init {
        this.instructions = listOf(
            Instruction("mount -o rw,remount /system"),
            Instruction("rm -f /system/priv-app/CCU*.apk", false),
            Instruction("rm -f /system/priv-app/RENATUS*.apk", false),
            Instruction("cp $fullDownloadPath /system/priv-app/$fileName"),
            Instruction("chmod 644 /system/priv-app/$fileName"),
            Instruction("chown root.root /system/priv-app/$fileName"),
            Instruction("pm install -r -d -g --user 0 /system/priv-app/$fileName", false),
        )
    }
}