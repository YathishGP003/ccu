package a75f.io.domain.service

import a75f.io.domain.BuildConfig
import a75f.io.logger.CcuLog
import android.content.Context
import io.seventyfivef.domainmodeler.common.model.ExternalModelDto
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by Manjunath K on 12-07-2023.
 */

class DomainService {

    private val apiService = ServiceGenerator().provideDomainModelerService()

    fun readModbusModelsList(query: String, callback: ResponseCallback) {
        val call: Call<ResponseBody> = if (BuildConfig.BUILD_TYPE.contentEquals("carrier_prod")
            || BuildConfig.BUILD_TYPE.contentEquals("daikin_prod") || BuildConfig.BUILD_TYPE.contentEquals("airoverse_prod")) {
            apiService.getExternalModbusModelsList(query)
        } else {
            apiService.getModbusModelsList(query)
        }
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful)
                    callback.onSuccessResponse(response.body()?.string())
                 else
                    callback.onErrorResponse(response.raw().message)
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback.onErrorResponse(t.message)
            }
        })
    }


    fun readModelById(modelId: String,version: String, callback: ResponseCallback){
        val call: Call<ResponseBody> = if (BuildConfig.BUILD_TYPE.contentEquals("carrier_prod")
            || BuildConfig.BUILD_TYPE.contentEquals("daikin_prod")
            || BuildConfig.BUILD_TYPE.contentEquals("airoverse_prod")) {
            apiService.getExternalModelById(modelId,version)
        } else {
            apiService.getModelById(modelId,version)
        }
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful)
                    callback.onSuccessResponse(response.body()?.string())
                else
                    callback.onErrorResponse(response.raw().message)
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback.onErrorResponse(t.message)
            }
        })
    }

    fun readBacNetModelsList(query: String, callback: ResponseCallback) {
        val call: Call<ResponseBody> = apiService.getBacNetModelsList(query)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful)
                    callback.onSuccessResponse(response.body()?.string())
                else
                    callback.onErrorResponse(response.raw().message)
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback.onErrorResponse(t.message)
            }
        })
    }
    fun readBacNetModelById(context: Context, modelId: String, version: String, callback: ResponseCallback) {
        val call: Call<ResponseBody> = apiService.getBacNetModelById2(modelId, version)
        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful)
                    callback.onSuccessResponse(response.body()?.string())
                else
                    callback.onErrorResponse(response.raw().message)
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                callback.onErrorResponse(t.message)
            }
        })
    }
}
