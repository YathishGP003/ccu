package a75f.io.logic.migration

import a75f.io.alerts.cloud.DateTimeTypeConverter
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import com.google.gson.GsonBuilder
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.joda.time.DateTime
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

interface MigrationService {
    @POST("/schedule/migrate-schedule/{siteId}")
    fun triggerSchedulerMigration(
        @Path("siteId") siteId: String,
    ): Call<ResponseBody>
}

//TODO : ServiceGenerator currently exists in multiple modules. It is should be refactored to
// remove the duplication
class ServiceGenerator {

    companion object {
        @JvmStatic
        val instance: ServiceGenerator by lazy {
            ServiceGenerator()
        }
    }

    fun createService(baseUrl: String, token: String): MigrationService {
        CcuLog.d(L.TAG_CCU_MESSAGING, "MigrationService: createService $baseUrl, $token")

        return createRetrofit(
            baseUrl,
            token
        ).create(MigrationService::class.java)
    }

    private fun createRetrofit(baseUrl: String, token: String): Retrofit {
        val okhttpLogging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder().apply {
            addInterceptor(okhttpLogging)
            connectTimeout(60, TimeUnit.SECONDS)
            readTimeout(60,TimeUnit.SECONDS)
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

data class MigrationResponse(val siteRef : String,
val userId : String,
  val ccuId : String,
 val scheduleStartTime : String,
                              val scheduleEndTime : String,
                               val migrationStatus : String,
                             val message : String,
                              val zoneIdPointDetailsMap : List<String>,
                             val buildingOccupancyId : String,
                             val namedSchedules : List<String>,
                             val specialSchedules : List<String>,
                              val latestBuildingLimits : Map<String,Double>,
                             val siteBuildingUserLimitIds : List<String>
)
