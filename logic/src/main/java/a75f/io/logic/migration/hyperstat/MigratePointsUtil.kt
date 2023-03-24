package a75f.io.logic.migration.hyperstat

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.Point
import android.util.Log

/**
 * Created by Manjunath K on 13-10-2022.
 */

class MigratePointsUtil {

    companion object {

        const val TAG ="DEV_MIGRATION"
        /** Function to remove and add the markers and update to the haystack
         * @param pointMap
         * @param markersToAdd
         * @param markersToRemove
         * @return
         */
        fun updateMarkers(
            pointMap: HashMap<Any, Any>,
            markersToAdd: Array<String>,
            markersToRemove: Array<String>,
            displayName: String?
        ) {
            if (pointMap.isEmpty()) {
                Log.e(TAG, "updateMarkers: point does not exist $displayName")
                return
            }
            var point = Point.Builder().setHashMap(pointMap)
            Log.i(TAG, "updateMarkers: $pointMap")
            if(displayName != null)
                point.setDisplayName(displayName)
            point = addMarker(point, markersToAdd)
            point = removeMarker(point, markersToRemove)
            updatePoint(point.build())
        }

        /** Function to add the marker
         * @param point
         * @param markersToAdd
         * @return Point.Builder
         */
        private fun addMarker(point: Point.Builder, markersToAdd: Array<String>): Point.Builder {
            markersToAdd.forEach { point.addMarker(it) }
            return point
        }

        /** Function to remove the marker
         * @param point
         * @param markersToRemove
         * @return Point.Builder
         */
        private fun removeMarker(point: Point.Builder, markersToRemove: Array<String>): Point.Builder {
            markersToRemove.forEach { point.removeMarker(it) }
            point.removeMarker("hyperstat")
            if(markersToRemove.contains("enum"))
                point.setEnums(null)
            return point
        }

        /** Function to update the point back to haystack
         * @param updatedPoint
         * @return Point.Builder
         */
        fun updatePoint(updatedPoint: Point) {
            Log.i(TAG, "updatedPoint: ${updatedPoint.displayName} Markers: ${updatedPoint.markers}")
            CCUHsApi.getInstance().updatePoint(updatedPoint, updatedPoint.id)
        }

    }
}