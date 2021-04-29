package a75f.io.alerts.cloud

import a75f.io.alerts.AlertDefinition
import a75f.io.api.haystack.Alert
import a75f.io.logger.CcuLog
import com.google.gson.*
import hu.akarnokd.rxjava3.retrofit.RxJava3CallAdapterFactory
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.joda.time.DateTime
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.lang.reflect.Type
import java.util.*


/**
 * Service calls for AlertsService V2.
 *
 * Currently using as many of the old DTO objects as possible to maintain
 * as much of existing code as possible for Renatus release.
 *
 * Suggest making Network-only layer DTOs, and converting them to domain layer model objects.
 *
 * @author tcase@75f.io
 * Created on 3/3/21.
 */
interface AlertsService {

   /**
    * Get all alert definitions for a site, predefined and custom.
    */
   @GET("/definitions/sites/{siteId}?size=100&sortField=alert.mSeverity&sortDirection=DESC")
   fun getSiteDefinitions(
      @Path("siteId") siteId: String,
   ): Single<DefinitionsResponse>

   /**
    * Get a single definition based on its id.
    */
   @GET("/definitions/sites/{siteId}/{definitionId}")
   fun getCustomDefinition(
      @Path("siteId") siteId: String,
      @Path("definitionId") defId: String,
   ): Single<AlertDefinition>

   /**
    * Creates an alert for this site in the remote service.
    * The service returns the same alert, with _id & timesteamp added.
    */
   @POST("/alerts/{siteId}")
   fun createAlert(
      @Path("siteId") siteId: String,
      @Body alert: AlertSyncDto
   ): Single<Alert>

   /**
    * Updates an alert for this site in the remote service.
    * The alertId should be server id, i.e. _id returns with create Request.
    * The service returns the same alert, with _id & timesteamp added.
    */
   @PUT("/alerts/{siteId}/{alertId}")
   fun updateAlert(
      @Path("siteId") siteId: String,
      @Path("alertId") alertId: String,
      @Body alert: AlertSyncDto
   ): Single<Alert>

   /**
    * Deletes an alert for this site in the remote service.
    */
   @DELETE("/alerts/{siteId}/{alertId}")
   fun deleteAlert(
      @Path("siteId") siteId: String,
      @Path("alertId") alertId: String
   ): Completable
}



data class DefinitionsResponse(
   val total: Int,
   val data: ArrayList<AlertDefinition>
)

/** Create a separate dto for put and create for slight difference from Alert class, e.g. these must not have _id field */
data class AlertSyncDto(
   val definitionId: String,
   val siteId: String,
   val siteName: String,
   val ccuId: String,
   val ccuName: String,
   val equipId: String?,
   val equipName: String?,
   val startTime: Long,
   val endTime: Long,
   val mTitle: String,
   val mAlertType: String,
   val mSeverity: String,
   val mMessage: String,
   val mNotificationMsg: String,
   val mEnabled: Boolean,
   val isFixed: Boolean,
) {
   companion object {

      @JvmStatic
      fun fromAlert(alert: Alert): AlertSyncDto {
         with(alert) {
            // we can/should take the placeholder out once Madhu updates server.
            return AlertSyncDto(alertDefId,
               siteIdNoAt, siteName, ccuIdNoAt, ccuName, equipId, equipName,
               startTime, endTime,
               mTitle, mAlertType, mSeverity.name, mMessage, mNotificationMsg,
               mEnabled, isFixed)
         }
      }
   }
}


//@Singleton
class ServiceGenerator {

   companion object {
      @JvmStatic
      val instance: ServiceGenerator by lazy {
         ServiceGenerator()
      }
   }

   fun createService(baseUrl: String, token: String): AlertsService {
      CcuLog.d("CCU_ALERTS", "AlertsService: createService $baseUrl, $token")

      return createRetrofit(
         baseUrl,
         HttpHeaders(contentType = "application/json", token = token)
      )
         .create(AlertsService::class.java)
   }

   /**
    * A generic createRetrofit function that should work anywhere in the code.
    *
    * Supply the baseUrl and encoding, and optionally the bearer token or Api-key.
    */
   fun createRetrofit(baseUrl: String, headers: HttpHeaders): Retrofit {
      val okhttpLogging = HttpLoggingInterceptor().apply {
         level = HttpLoggingInterceptor.Level.BODY
      }

      val okHttpClient = OkHttpClient.Builder().apply {
         addInterceptor(okhttpLogging)
         addInterceptor(
            Interceptor { chain ->
               val builder = chain.request().newBuilder()
               headers.token?.let { builder.header("Authorization", "Bearer $it") }
               headers.apiKey?.let { builder.header("api-key", it) }
               headers.encoding?.let { builder.header("Accept-Encoding", it) }
               headers.contentType?.let { builder.header("Content-Type", it) }
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

data class HttpHeaders(
   val contentType: String? = null,
   val encoding: String? = null,
   val token: String? = null,
   val apiKey: String? = null
)

private class DateTimeTypeConverter : JsonSerializer<DateTime>, JsonDeserializer<DateTime?> {
   override fun serialize(src: DateTime, srcType: Type?, context: JsonSerializationContext?): JsonElement {
      return JsonPrimitive(src.toString())
   }

   @Throws(JsonParseException::class)
   override fun deserialize(json: JsonElement, type: Type?, context: JsonDeserializationContext): DateTime {
      return try {
         DateTime(json.asString)
      } catch (e: IllegalArgumentException) {
         // May be it came in formatted as a java.util.Date, so try that
         val date: Date = context.deserialize(json, Date::class.java)
         DateTime(date)
      }
   }
}

