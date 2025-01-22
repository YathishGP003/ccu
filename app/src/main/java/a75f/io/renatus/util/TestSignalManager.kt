package a75f.io.renatus.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.sync.PointWriteCache
import a75f.io.domain.api.PhysicalPoint
import a75f.io.logger.CcuLog
import org.projecthaystack.HDictBuilder
import org.projecthaystack.HNum
import org.projecthaystack.HRef
import org.projecthaystack.HVal

object TestSignalManager {

    private val testEntityMap = mutableMapOf<String, ArrayList<HashMap<Any, Any>>>()

    private fun saveWritableArray(id: String, levelArrayList: ArrayList<HashMap<Any, Any>>) {
        if(!testEntityMap.containsKey(id)){
            testEntityMap[id] = levelArrayList
        }
    }

    private fun clearPointPriorityVal(id: String?) {
        CcuLog.d("TestSignal", "--clearPointPriorityVal--")
        val values: java.util.ArrayList<*> = CCUHsApi.getInstance().readPoint(id)
        if (values.size > 0) {
            for (l in 1..values.size) {
                val valMap = values[l - 1] as java.util.HashMap<*, *>
                if (valMap["val"] != null) {
                    CCUHsApi.getInstance().clearPointArrayLevel(id, l, false)
                }
            }
        }
    }

    fun restoreWritableArray(id: String) {
        val values = testEntityMap[id]
        if (values != null) {
            if (values.size > 0) {
                for (l in 1..values.size) {
                    val valMap = values[l - 1] as java.util.HashMap<*, *>
                    if (valMap["val"] != null) {
                        val b: HDictBuilder = HDictBuilder()
                            .add("id", HRef.copy(id))
                            .add("level", HNum.make(l))
                            .add("who", CCUHsApi.getInstance().getCCUUserName())
                            .add("duration", HNum.make(0, "ms"))
                            .add("val", HNum.make(valMap["val"].toString().toDouble()))
                            .add("reason", "test-mode")
                        PointWriteCache.Companion.getInstance().writePoint(id, b.toDict())
                    }
                }
            }
        }
        testEntityMap.remove(id)
    }

    fun restoreAllPoints() {
        val keys = testEntityMap.keys.toList()
        for (key in keys) {
            restoreWritableArray(key)
        }
        testEntityMap.clear()
    }

    fun backUpRestorePoint(physicalPoint: PhysicalPoint, testCommand: Boolean) {
        CcuLog.d("backUpRestorePoint", "--testCommand--$testCommand")
        backUpPoint(physicalPoint)
    }

    fun backUpPoint(physicalPoint: PhysicalPoint) {
        try {
            CcuLog.d("TestSignal", "--backUpPoint--")
            saveWritableArray(
                physicalPoint.id,
                CCUHsApi.getInstance().readPoint(physicalPoint.id)
            )
            clearPointPriorityVal(physicalPoint.id)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun backUpPoint(id: String) {
        saveWritableArray(
            id,
            CCUHsApi.getInstance().readPoint(id)
        )
        clearPointPriorityVal(id)
    }
}