package a75f.io.messaging.client


import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.cloud.RenatusServicesEnvironment
import a75f.io.logic.connectnode.SequenceMetaDataDTO
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit


interface SeqService {
    @GET("device-seq/fetch")
    fun fetchSequence(@Query("seqId") seqId: String?): retrofit2.Call<ResponseBody>
}

class SequenceService {
    private val retrofit: Retrofit = getVersionManagementRetrofit()
    private fun getVersionManagementRetrofit(): Retrofit {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .apply {
                        addHeader("Authorization", "Bearer ${CCUHsApi.getInstance().jwt}")
                    }
                    .build()
                chain.proceed(request)
            })
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(RenatusServicesEnvironment.instance.urls.sequencerUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }


    fun retrieveMetaData(bundleName: String): SequenceMetaDataDTO? {
        val resp = getVersionManagementRetrofit().create(SeqService::class.java).fetchSequence(bundleName).execute()
        val response = resp.body()?.string() ?: return null
        return Gson().fromJson(response, SequenceMetaDataDTO::class.java)
    }
}