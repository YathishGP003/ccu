package a75f.io.logic.bo.building.system.client

data class BaseResponse<T>(val data: T? = null, val error: String? = null)