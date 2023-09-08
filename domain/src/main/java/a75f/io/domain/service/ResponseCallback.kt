package a75f.io.domain.service

import org.json.JSONException
import org.json.JSONObject

/**
 * Created by Manjunath K on 12-07-2023.
 */

interface ResponseCallback {
    fun onSuccessResponse(response: String?)
    fun onErrorResponse(response: String?)
}