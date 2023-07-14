package a75f.io.domain.service

import a75f.io.api.haystack.CCUHsApi
import a75f.io.domain.BuildConfig
import a75f.io.logger.CcuLog
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

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
        @retrofit2.http.Path("modelId") modelId: String?
    ): retrofit2.Call<ResponseBody>

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
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $bearerToken")
                    .header("Accept","application/json")
                    .addHeader("Content-Type", "text/json")
                    .build()
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
            .build()
    }
    private fun getRetrofitForCareTakerBaseUrl(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.DOMAIN_API_BASE)
            .client(getOkHttpClient())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

}