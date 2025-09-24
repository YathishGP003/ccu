package a75f.io.renatus.util.remotecommand.bundle

import java.io.File

class CCUApp(
    fileName: String,
    versionsListFromPrivAppToRemove: List<Pair<File, Long>> = emptyList()
): BaseBundleApp(fileName) {
    companion object {
        const val packageName = "a75f.io.renatus"
    }

    init {
        if (versionsListFromPrivAppToRemove.isNotEmpty()) {
            this.instructions = buildList {
                add(Instruction("mount -o rw,remount /system"))
                add(Instruction("cp $fullDownloadPath /system/priv-app/"))
                add(Instruction("chmod 644 /system/priv-app/$fileName"))
                add(Instruction("chown root.root /system/priv-app/$fileName"))

                versionsListFromPrivAppToRemove.forEach { (file, _) ->
                    add(Instruction("rm -f ${file.absolutePath}", false))
                }
               add(Instruction("pm install -r -d -g $fullDownloadPath", false))
            }
        } else {
            this.instructions = listOf(
                Instruction("mount -o rw,remount /system"),
                Instruction("pm install -r -d -g $fullDownloadPath"),
            )
        }
    }
}