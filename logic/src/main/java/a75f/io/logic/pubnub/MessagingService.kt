package a75f.io.logic.pubnub

import retrofit2.http.*

import a75f.io.logger.CcuLog
import a75f.io.logic.L
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit

interface MessagingService {
    @Multipart
    @POST("/messages/{siteId}/acknowledge")
    fun acknowledgeMessages(
            @Path("siteId") siteId: String,
            @Query("subscriberId") ccuId: String,
            @Body acknowledgeRequest: AcknowledgeRequest
    ): Call<ResponseBody>
}


class ServiceGenerator {

    companion object {
        @JvmStatic
        val instance: ServiceGenerator by lazy {
            ServiceGenerator()
        }
    }

    fun createService(baseUrl: String, token: String): MessagingService {
        CcuLog.d(L.TAG_CCU_MESSAGING, "MessagingService: createService $baseUrl, $token")

        return createRetrofit(
                baseUrl,
                token
        ).create(MessagingService::class.java)
    }

    fun createRetrofit(baseUrl: String, token: String): Retrofit {
        val okhttpLogging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder().apply {
            addInterceptor(okhttpLogging)
            addInterceptor(
                    Interceptor { chain ->
                        val builder = chain.request().newBuilder()
                        builder.header("Authorization", "Bearer $token")
                        return@Interceptor chain.proceed(builder.build())
                    }
            )
        }.build()

        return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .build()
    }
}

data class AcknowledgeRequest(
        val eventIds: List<String>,
)