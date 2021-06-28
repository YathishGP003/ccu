package a75f.io.device.daikin

import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface IEService {

    @PUT("/BN/MT3/{pointType}/{pointName}/Present_Value?resp-format=eXML&access-token=123456789")
    fun writePoint(
        @Path("pointName") pointName: String,
        @Path("pointType") pointType: String,
        @Body pointVal : String
    )

    @GET("/BN/MT3/{pointType}/{pointName}/Present_Value?resp-format=eXML&access-token=123456789")
    fun readPoint(
        @Path("pointName") pointName: String,
        @Path("pointType") pointType: String
    )
}

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

    /**
     * A generic createRetrofit function that should work anywhere in the code.
     *
     * Supply the baseUrl and encoding, and optionally the bearer token or Api-key.
     */
    fun createRetrofit(baseUrl: String): Retrofit {
        val okhttpLogging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder().apply {
            addInterceptor(okhttpLogging)
            addInterceptor(
                Interceptor { chain ->
                    val builder = chain.request().newBuilder()
                    builder.header("Content-Type", "text/plain")
                    builder.header("Authorization", "Bearer=11021962")
                    return@Interceptor chain.proceed(builder.build())
                }
            )
        }.build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
            .build()
    }
}