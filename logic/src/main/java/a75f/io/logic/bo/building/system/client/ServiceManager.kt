package a75f.io.logic.bo.building.system.client

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private var retrofit: Retrofit? = null

    fun makeCcuService(ipAddress: String): CcuService {
        val url = "http://$ipAddress:5005"

        if (retrofit == null || retrofit!!.baseUrl().toString() != url) {
            retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build()
        }

        return retrofit!!.create(CcuService::class.java)
    }
}