package a75f.io.api.haystack.sync

import a75f.io.logger.CcuLog
import org.projecthaystack.HDict
import java.util.concurrent.ConcurrentHashMap

class PointWriteCache private constructor() {
    private val pointWriteCache = ConcurrentHashMap<String, HDict>()

    fun writePoint(id: String, pointWriteDict: HDict) {
        CcuLog.i(POINT_WRITE_TAG, "Add Point Id to PointWriteCache: "+id +" PointWriteCache size :"+pointWriteCache.size)
        pointWriteCache[id] = pointWriteDict
    }

    fun clearPointWriteInCache(id: String) {
        CcuLog.i(POINT_WRITE_TAG, "Removed Id from PointWriteCache: "+id +" PointWriteCache size :"+pointWriteCache.size)
        pointWriteCache.remove(id)
    }

    fun getPointWriteList(): List<Map.Entry<String, HDict>> {
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