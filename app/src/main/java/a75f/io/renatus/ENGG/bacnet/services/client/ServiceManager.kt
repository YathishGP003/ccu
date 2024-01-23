package a75f.io.renatus.ENGG.bacnet.services.client

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit


class ServiceManager {

    object CcuServiceFactory {
        //private const val BASE_URL = "http://192.168.1.50:5005"


        fun makeCcuService(ipAddress : String): CcuService {
            //val url = "http://192.168.1.50:5005"
            val url = "http://$ipAddress:5005"
            val loggingInterceptor = HttpLoggingInterceptor()
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
            val okHttpClient = OkHttpClient.Builder()
            okHttpClient.connectTimeout(30, TimeUnit.SECONDS)
            okHttpClient.interceptors().add(loggingInterceptor)
            val client = okHttpClient.build()
            return Retrofit.Builder()
                .baseUrl(url)
                //.addConverterFactory(ScalarsConverterFactory.create())
                //.addConverterFactory(GsonConverterFactory.create(gson))
                .addConverterFactory(GsonConverterFactory.create()) // Use Gson converter for JSON
                .client(client)
                .build().create(CcuService::class.java)
        }


    }
}