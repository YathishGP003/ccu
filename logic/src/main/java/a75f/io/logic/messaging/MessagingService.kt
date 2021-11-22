package a75f.io.logic.messaging

import a75f.io.alerts.cloud.DateTimeTypeConverter
import retrofit2.http.*

import a75f.io.logger.CcuLog
import a75f.io.logic.L
import com.google.gson.GsonBuilder
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import io.reactivex.rxjava3.core.Single
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.joda.time.DateTime
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface MessagingService {
    @PUT("/messages/{channel}/acknowledge")
    fun acknowledgeMessages(
            @Path("channel") channel: String,
            @Query("subscriberId") ccuId: String,
            @Body acknowledgeRequest: AcknowledgeRequest
    ): Single<Response<Void>>
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

        val gson = GsonBuilder()
                .registerTypeAdapter(DateTime::class.java, DateTimeTypeConverter())
                .create()

        return Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build()
    }
}

data class AcknowledgeRequest(
        val eventIds: Set<String>
)