package a75f.io.logic.bo.building.system.client

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object ServiceManager {

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            //if (BuildConfig.DEBUG) {
                level = HttpLoggingInterceptor.Level.BODY
            //}
        }

        OkHttpClient.Builder()
                .connectTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .addInterceptor(loggingInterceptor)
                .build()
    }

    private val retrofitCache = ConcurrentHashMap<String, Retrofit>()

    private fun getRetrofit(baseUrl: String): Retrofit {
        return retrofitCache.getOrPut(baseUrl) {
            Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient)
                    .build()
        }
    }

    fun makeCcuService(ipAddress: String): CcuService {
        val baseUrl = "http://$ipAddress:5005/"
        return getRetrofit(baseUrl).create(CcuService::class.java)
    }

    fun makeCcuServiceForMSTP(ipAddress: String): CcuService {
        val baseUrl = "http://$ipAddress:5006/"
        return getRetrofit(baseUrl).create(CcuService::class.java)
    }
}