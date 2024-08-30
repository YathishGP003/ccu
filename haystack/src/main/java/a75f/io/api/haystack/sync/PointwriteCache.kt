package a75f.io.api.haystack.sync

import a75f.io.logger.CcuLog
import org.projecthaystack.HDict
import java.util.concurrent.ConcurrentHashMap

class PointWriteCache private constructor() {
    private val pointWriteCache = ConcurrentHashMap<String, Array<HDict>>()

    fun writePoint(id: String, pointWriteDict: HDict) {
        CcuLog.i(POINT_WRITE_TAG, "Add Point Id to PointWriteCache: "+id +" PointWriteCache size :"+pointWriteCache.size)
        if (pointWriteCache.containsKey(id)) {
            val pointWriteArray = pointWriteCache[id]?.toMutableList() ?: mutableListOf()
            pointWriteArray.removeIf {
                it.getInt("level") == pointWriteDict.getInt("level")
            }
            pointWriteArray.add(pointWriteDict)
            pointWriteCache[id] = pointWriteArray.toTypedArray()
        } else {
            pointWriteCache[id] = arrayOf(pointWriteDict)
        }
    }

    fun clearPointWriteInCache(id: String, pointLevel: String) {
        CcuLog.i(POINT_WRITE_TAG, "Removed Id from PointWriteCache: "+id +" PointWriteCache size :"+pointWriteCache.size)
        if(pointWriteCache.containsKey(id)) {
            val pointWriteArray = pointWriteCache[id]?.toMutableList() ?: mutableListOf()
            pointWriteArray.removeIf { it.get("level").toString() == pointLevel }
            pointWriteCache[id] = pointWriteArray.toTypedArray()
            if (pointWriteArray.isEmpty()) {
                pointWriteCache.remove(id)
            }
        }
    }

    fun getPointWriteList(): List<Map.Entry<String, Array<HDict>>> {
        return pointWriteCache.entries.toList()
    }

    companion object {
        @Volatile
        private var instance: PointWriteCache? = null
        fun getInstance(): PointWriteCache {
            return instance ?: synchronized(this) {
                instance ?: PointWriteCache().also { instance = it }
            }
        }

        const val POINT_WRITE_TAG = "pointWriteHandler"
    }
}