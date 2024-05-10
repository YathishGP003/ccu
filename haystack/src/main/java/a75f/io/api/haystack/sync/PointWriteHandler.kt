package a75f.io.api.haystack.sync

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import org.projecthaystack.HDict
import org.projecthaystack.HGridBuilder
import org.projecthaystack.HRow
import org.projecthaystack.io.HZincReader
import org.projecthaystack.io.HZincWriter

class PointWriteHandler {

    fun processPointWrites(pointWriteList: List<Map.Entry<String, HDict>>) {
        CcuLog.i(PointWriteCache.POINT_WRITE_TAG, "Process PointWrites "+pointWriteList.size)

        if (pointWriteList.isEmpty()) {
            return
        }
        pointWriteList.chunked(POINT_WRITE_BATCH_SIZE) { chunk ->
            chunk.map { it.value }
        }.forEach { pointWriteBatch ->
            processBatch(pointWriteBatch, CCUHsApi.getInstance())
        }
    }

    private fun processBatch(pointWriteBatch: List<HDict>, ccuHsApi: CCUHsApi) {
        val pointWriteGrid = HGridBuilder.dictsToGrid(pointWriteBatch.toTypedArray())
        val pointWriteManyResponse = HttpUtil.executeEntitySync(
            ccuHsApi.hsUrl + ENDPOINT_POINT_WRITE_MANY,
            HZincWriter.gridToString(pointWriteGrid), ccuHsApi.jwt
        )
        if (pointWriteManyResponse.respCode == HttpUtil.HTTP_RESPONSE_OK) {
            CcuLog.i(PointWriteCache.POINT_WRITE_TAG, "PointWriteMany Success Response Code :"+pointWriteManyResponse.respCode )
        } else if (pointWriteManyResponse.respCode >= HttpUtil.HTTP_RESPONSE_ERR_REQUEST) {
            CcuLog.i(PointWriteCache.POINT_WRITE_TAG, "PointWriteMany Failed Response Code :"+pointWriteManyResponse.respCode )
            EntitySyncErrorHandler.handle400HttpError(
                ccuHsApi,
                pointWriteManyResponse.errRespString
            )
        }
        val pointWriteIterator = pointWriteGrid.iterator()
        while (pointWriteIterator.hasNext()) {
            val row = pointWriteIterator.next() as HRow
            val pointId = row["id"].toString()
            PointWriteCache.getInstance().clearPointWriteInCache(pointId)
        }
    }

    companion object {
        @Volatile
        private var instance: PointWriteHandler? = null
        fun getInstance(): PointWriteHandler {
            return instance ?: synchronized(this) {
                instance ?: PointWriteHandler().also {
                    instance = it
                }
            }
        }

        private const val POINT_WRITE_BATCH_SIZE = 20
        private const val ENDPOINT_POINT_WRITE_MANY = "pointWriteMany"
    }
}
