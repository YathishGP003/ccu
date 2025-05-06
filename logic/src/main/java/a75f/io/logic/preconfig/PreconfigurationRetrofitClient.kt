package a75f.io.logic.preconfig

import a75f.io.alerts.AlertProcessor.TAG_CCU_HTTP_REQUEST
import a75f.io.alerts.AlertProcessor.TAG_CCU_HTTP_RESPONSE
import a75f.io.constants.HttpConstants
import a75f.io.logger.CcuLog
import a75f.io.logic.cloud.RenatusServicesEnvironment.Companion.instance
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object PreconfigurationRetrofitClient {

    val apiService: PreconfigurationService by lazy {

        val okhttpLogging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val apiKey = "preConfig-40da4c33-31a0-4dab-9ef8-4a58ee0832a8"
        val okHttpClient = OkHttpClient.Builder().apply {
            addInterceptor(okhttpLogging)
            readTimeout(60, TimeUnit.SECONDS)
            writeTimeout(60, TimeUnit.SECONDS)
            connectTimeout(60, TimeUnit.SECONDS)
            addInterceptor(
                Interceptor { chain ->
                    CcuLog.d(TAG_CCU_HTTP_REQUEST, "PreconfigurationService: [${chain.request().method}] ${chain.request().url} - Token: $apiKey")

                    val builder = chain.request().newBuilder()
                        .header(HttpConstants.APP_NAME_HEADER_NAME, HttpConstants.APP_NAME_HEADER_VALUE)
                        .header("api-key", apiKey)
                        .header("Content-Type", "application/json")

                    return@Interceptor chain.proceed(builder.build())
                }
            )
            addInterceptor(
                Interceptor { chain ->
                    val request = chain.request()
                    val response = chain.proceed(request)

                    CcuLog.d(TAG_CCU_HTTP_RESPONSE, "PreconfigurationService: ${response.code} - [${request.method}] ${request.url}")
                    response
                }
            )
        }.build()

        val urls = instance.urls

        Retrofit.Builder()
            .baseUrl(urls.preconfigurationUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PreconfigurationService::class.java)
    }
}