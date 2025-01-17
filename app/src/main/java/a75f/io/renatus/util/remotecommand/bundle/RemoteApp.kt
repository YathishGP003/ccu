package a75f.io.renatus.util.remotecommand.bundle

class RemoteApp(fileName: String): BaseBundleApp(fileName) {
    companion object {
        val packageName = "io.seventyfivef.remoteaccess"
    }

    init {
        this.instructions = listOf(
            Instruction("pm install -r -d -g --user 0 $fullDownloadPath"),
            Instruction("appops set $packageName PROJECT_MEDIA allow"),
            Instruction("appops set $packageName SYSTEM_ALERT_WINDOW allow"),
            Instruction("pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"),
        )
    }
}