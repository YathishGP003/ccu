package a75f.io.device.mesh

import android.util.Base64

/**
 * Author: Manjunath Kundaragi
 * Created on: 21-07-2025
 */
object Base64Util {
    // Encode String to Base64
    fun encode(input: String): String {
        return Base64.encodeToString(input.toByteArray(), Base64.DEFAULT)
    }

    // Decode Base64 to String
    fun decode(base64: String?): String {
        val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
        return String(decodedBytes)
    }
}
