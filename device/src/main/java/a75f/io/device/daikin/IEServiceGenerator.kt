package a75f.io.device.daikin

import a75f.io.logger.CcuLog
import a75f.io.logic.L
import com.google.gson.GsonBuilder
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.concurrent.TimeUnit

class IEServiceGenerator {
    companion object {
        @JvmStatic
        val instance: IEServiceGenerator by lazy {
            IEServiceGenerator()
        }
    }

    fun createService(baseUrl: String): IEService {
        return createRetrofit(
            baseUrl
        ).create(IEService::class.java)
    }

    fun createRetrofit(baseUrl: String): Retrofit {
        val okhttpLogging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder().apply {
            addInterceptor(
                Interceptor { chain ->
                    val builder = chain.request().newBuilder()
                    builder.header("Content-Type", "text/plain")
                    builder.header("Authorization", "Bearer=11021962")
                    return@Interceptor chain.proceed(builder.build())
                }
            )
            addInterceptor(okhttpLogging)
            addNetworkInterceptor(okhttpLogging)
            connectTimeout(30, TimeUnit.SECONDS)
        }.build()
        CcuLog.i(L.TAG_CCU_DEVICE, "create retrofit  $baseUrl")

        val gson = GsonBuilder()
            .setLenient()
            .create()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build()
    }
}