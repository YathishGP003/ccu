package a75f.io.domain.service

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.BuildConfig
import a75f.io.logger.CcuLog
import io.seventyfivef.domainmodeler.common.model.ExternalModelDto
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit

/**
 * Created by Manjunath K on 12-07-2023.
 */


interface DomainModelerService {
    @GET("/models/modbus/list")
    fun getModbusModelsList(
        @retrofit2.http.Query("tag-names") tagNames: String?
    ): retrofit2.Call<ResponseBody>

    @GET("/models/modbus/{modelId}/modbus-json")
    fun getModelById(
        @retrofit2.http.Path("modelId") modelId: String?,
        @retrofit2.http.Query("version") version: String?
    ): retrofit2.Call<ResponseBody>



    @GET("/hayloft/models/modbus/list")
    fun getExternalModbusModelsList(
        @retrofit2.http.Query("tag-names") tagNames: String?
    ): retrofit2.Call<ResponseBody>

    @GET("/hayloft/models/modbus/{modelId}/modbus-json")
    fun getExternalModelById(
        @retrofit2.http.Path("modelId") modelId: String?,
        @retrofit2.http.Query("version") version: String?
    ): retrofit2.Call<ResponseBody>

    @GET("/models/external/list")
    fun getBacNetModelsList(
        @retrofit2.http.Query("protocol") protocol: String?,
        @retrofit2.http.Query("tag-names") tagNames: String?
    ): retrofit2.Call<ResponseBody>

    @GET("/hayloft/models/external/list")
    fun getExternalBacNetModelsList(
        @retrofit2.http.Query("protocol") protocol: String?,
        @retrofit2.http.Query("tag-names") tagNames: String?
    ): retrofit2.Call<ResponseBody>

    @GET("/models/external/{modelId}")
    fun getModbusModelById(
        @retrofit2.http.Path("modelId") modelId: String?,
        @retrofit2.http.Query("version") version: String?,
    ): retrofit2.Call<ResponseBody>

    @GET("/hayloft/models/external/{modelId}")
    fun getExternalModbusModelById(
        @retrofit2.http.Path("modelId") modelId: String?,
        @retrofit2.http.Query("version") version: String?,
    ): retrofit2.Call<ResponseBody>

    @GET("/models/external/export/{modelId}")
    fun getBacNetModelById2(
        @retrofit2.http.Path("modelId") modelId: String?,
        @retrofit2.http.Query("version") version: String?
    ): retrofit2.Call<ResponseBody>

    @GET("/hayloft/models/external/export/{modelId}")
    fun getExternalBacnetModelById(
        @retrofit2.http.Path("modelId") modelId: String?,
        @retrofit2.http.Query("version") version: String?
    ): retrofit2.Call<ResponseBody>


    @GET("/models/external/export/{modelId}")
    fun getBacNetModelById21(
        @retrofit2.http.Path("modelId") modelId: String?
    ): retrofit2.Call<ExternalModelDto>


}

class ServiceGenerator {
    private val retrofit: Retrofit = getRetrofitForCareTakerBaseUrl()
    fun provideDomainModelerService(): DomainModelerService = retrofit.create(DomainModelerService::class.java)

    private fun getOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS)
        return OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val bearerToken = CCUHsApi.getInstance().jwt
                val originalRequest = chain.request()
                val newRequest = addHeader(originalRequest)
                CcuLog.d(
                    "CCU_HTTP_REQUEST",
                    "DomainService: [" + chain.request().method + "] " + chain.request().url + " - Token: " + bearerToken
                )
                chain.proceed(newRequest)
            })
            .addInterceptor(Interceptor { chain: Interceptor.Chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                CcuLog.d(
                    "CCU_HTTP_RESPONSE",
                    "DomainService: " + response.code + " - [" + request.method + "] " + request.url
                )
                response
            })
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    private fun getRetrofitForCareTakerBaseUrl(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.DOMAIN_API_BASE)
            .client(getOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun addHeader(originalRequest: Request): Request {
        val newRequest = originalRequest.newBuilder()
        when (BuildConfig.BUILD_TYPE) {
            "daikin_prod"  -> {
                newRequest.header("Ocp-Apim-Subscription-Key", BuildConfig.DOMAIN_API_KEY)
            }
            "carrier_prod" -> {
                newRequest.header("Ocp-Apim-Subscription-Key", BuildConfig.DOMAIN_API_KEY)
            }
            "airoverse_prod" -> {
                newRequest.header("Ocp-Apim-Subscription-Key", BuildConfig.DOMAIN_API_KEY)
            }
            else -> {
                newRequest.header("Authorization", "Bearer ${ CCUHsApi.getInstance().jwt}")
                newRequest.header("Accept","application/json")
                newRequest.addHeader("Content-Type", "text/json")
            }
        }
        return newRequest.build()
    }
}