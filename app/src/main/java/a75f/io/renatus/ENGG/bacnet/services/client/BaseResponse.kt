package a75f.io.renatus.ENGG.bacnet.services.client

data class BaseResponse<T>(val data: T? = null, val error: String? = null)