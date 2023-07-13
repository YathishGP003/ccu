package a75f.io.domain.service

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by Manjunath K on 12-07-2023.
 */

class DomainService {

    private val apiService = ServiceGenerator().provideDomainModelerService()

    fun readModbusModelsList(query: String, callback: ResponseCallback) {
        val call: Call<ResponseBody> = apiService.getModbusModelsList(query)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful)
                    callback.onSuccessResponse(response.body()?.string())
                 else
                    callback.onErrorResponse(response.body()?.toString())
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback.onErrorResponse(t.stackTraceToString())
            }
        })
    }


    fun readModelById(modelId: String,callback: ResponseCallback){
        val call: Call<ResponseBody> = apiService.getModelById(modelId)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful)
                    callback.onSuccessResponse(response.body()?.string())
                else
                    callback.onErrorResponse(response.body()?.string())
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback.onErrorResponse(t.stackTraceToString())
            }
        })
    }

}
