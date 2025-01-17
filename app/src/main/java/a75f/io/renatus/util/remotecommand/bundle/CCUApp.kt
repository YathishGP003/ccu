package a75f.io.renatus.util.remotecommand.bundle

class CCUApp(fileName: String): BaseBundleApp(fileName) {
    companion object {
        val packageName = "a75f.io.renatus"
    }

    init {
        this.instructions = listOf(
            Instruction("mount -o rw,remount /system"),
            Instruction("pm install -r -d -g $fullDownloadPath"),
        )
    }
}