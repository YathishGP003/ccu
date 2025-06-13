package a75f.io.domain.util

import a75f.io.domain.api.Point

/**
 * Created by Manjunath K on 09-05-2025.
 */

// This class is used for the controller where some time we will calculate the value for the point
// but we will not update to the cloud, but controller will use the db functions to read the value
// so overriding the required DB function to use the point with dynamic value
data class CalibratedPoint(val name: String, val ref: String, var data: Double): Point(name, ref) {
    override fun readHisVal(): Double {
        return data
    }
}

