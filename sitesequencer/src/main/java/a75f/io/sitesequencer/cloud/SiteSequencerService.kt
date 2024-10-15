package a75f.io.sitesequencer.cloud

import a75f.io.api.haystack.CCUHsApi
import a75f.io.constants.HttpConstants
import a75f.io.logger.CcuLog
import a75f.io.sitesequencer.SequencerParser
import a75f.io.sitesequencer.SiteSequencerDefinition
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.joda.time.DateTime
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.lang.reflect.Type
import java.util.Date


interface SiteSequencerService {

    @GET("/seq-mgmt/sequencesPerCcu")
    suspend fun getSiteDefinitions(
        @Query("siteRef") siteRef: String,
    ): List<SiteSequencerDefinition> //<DefinitionsResponse>
}


data class DefinitionsResponse(
    val total: Int,
    val data: ArrayList<SiteSequencerDefinition>
)

class DateTimeTypeConverter : JsonSerializer<DateTime>, JsonDeserializer<DateTime?> {
    override fun serialize(
        src: DateTime,
        srcType: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src.toString())
    }

    @Throws(JsonParseException::class)
    override fun deserialize(
        json: JsonElement,
        type: Type?,
        context: JsonDeserializationContext
    ): DateTime {
        return try {
            DateTime(json.asString)
        } catch (e: IllegalArgumentException) {
            // May be it came in formatted as a java.util.Date, so try that
            val date: Date = context.deserialize(json, Date::class.java)
            DateTime(date)
        }
    }
}


class ServiceGenerator {

    companion object {
        @JvmStatic
        val instance: ServiceGenerator by lazy {
            ServiceGenerator()
        }
    }

    fun createService(baseUrl: String): SiteSequencerService {
        CcuLog.d(SequencerParser.TAG_CCU_SITE_SEQUENCER, "SequencerService: createService $baseUrl")

        return createRetrofit(baseUrl)
            .create(SiteSequencerService::class.java)
    }

    /**
     * A generic createRetrofit function that should work anywhere in the code.
     *
     * Supply the baseUrl and encoding, and optionally the bearer token or Api-key.
     */
    private fun createRetrofit(baseUrl: String): Retrofit {
        val okhttpLogging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder().apply {
            addInterceptor(okhttpLogging)
            addInterceptor(
                Interceptor { chain ->
                    val bearerToken = CCUHsApi.getInstance().jwt

                    CcuLog.d(
                        SequencerParser.TAG_CCU_SITE_SEQUENCER,
                        "SequencerService: [${chain.request().method}] ${chain.request().url} - Token: $bearerToken"
                    )

                    val builder = chain.request().newBuilder()
                        .header(
                            HttpConstants.APP_NAME_HEADER_NAME,
                            HttpConstants.APP_NAME_HEADER_VALUE
                        )
                        .header("Authorization", "Bearer $bearerToken")
                        .header("Content-Type", "application/json")

                    return@Interceptor chain.proceed(builder.build())
                }
            )
            addInterceptor(
                Interceptor { chain ->
                    val request = chain.request()
                    val response = chain.proceed(request)

                    CcuLog.d(
                        SequencerParser.TAG_CCU_SITE_SEQUENCER,
                        "SequencerService: ${response.code} - [${request.method}] ${request.url}"
                    )
                    response
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
