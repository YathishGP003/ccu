package a75f.io.domain.service

import a75f.io.api.haystack.bacnet.parser.BacnetModelDetailResponse
import io.seventyfivef.domainmodeler.common.model.ExternalModelDto
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by Manjunath K on 12-07-2023.
 */

interface ResponseCallback {
    fun onSuccessResponse(response: String?)
    fun onErrorResponse(response: String?)
}

interface ResponseCallbackBacnetModelDetail {
    fun onSuccessResponse(response: BacnetModelDetailResponse?)
    fun onErrorResponse(response: String?)
}

interface ResponseCallbackExternalModelDto {
    fun onSuccessResponse(response: ExternalModelDto?)
    fun onErrorResponse(response: String?)
}