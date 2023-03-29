package a75f.io.logic.cloudservice

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logger.CcuLog
import a75f.io.logic.cloud.RenatusServicesEnvironment
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/**
 * Retrofit service for remote file storage API
 *
 * @author Tony Case
 * Created on 1/13/21.
 */

interface FileStorageService {
   @Multipart
   @PUT("/files/{container}/{siteId}/{fileId}")
   fun uploadFileTo(
      @Path("container") container: String,
      @Path("siteId") siteId: String,
      @Path("fileId") filename: String,
      @Part file: MultipartBody.Part
   ): Call<ResponseBody>
}

/**
 * Creates the Retrofit services.
 *
 * Since this depends on the RenatusServicesEnvironment Urls, which could change
 * during runtime, this must be re-initialized, retrofit re-created, every time t
 * hose base URLs change.
 * Currently, this is created only when (and every time) the Upload logs feature is used,
 * but more generally we might use this for all our networking.  Then, this could
 * be recreated right from RenatusServicesEnvironment.
 */
class ServiceGenerator {

   private val retrofit: Retrofit =
      createRetrofit()


   fun provideFileStorageService(): FileStorageService =
      retrofit.create(FileStorageService::class.java)


   /**
    * This client has generous timeout periods set to help large log file uploads.
    */
   private fun createRetrofit(): Retrofit {
      val okhttpLogging = HttpLoggingInterceptor().apply {
         level = HttpLoggingInterceptor.Level.HEADERS
      }

      val remoteStorageUrl = RenatusServicesEnvironment.instance.
      urls.remoteStorageUrl

      val okHttpClient = OkHttpClient.Builder().apply {
         addInterceptor(
            Interceptor { chain ->
               val bearerToken = CCUHsApi.getInstance().jwt

               val builder = chain.request().newBuilder()
               builder.header("Authorization", "Bearer $bearerToken")
               builder.header("Accept-Encoding", "gzip, deflate, br")

               CcuLog.d("CCU_HTTP_REQUEST", "FileBackupService: [" + chain.request().method + "] " + chain.request().url + " - Token: " + bearerToken);

               return@Interceptor chain.proceed(builder.build())
            }
         )
         addInterceptor(okhttpLogging)
         addInterceptor(
               Interceptor { chain ->
                   val request = chain.request()
                   val response = chain.proceed(request)

                   CcuLog.d("CCU_HTTP_RESPONSE", "FileBackupService: ${response.code} - [${request.method}] ${request.url}")
                   response
               }
         )
         connectTimeout(60, TimeUnit.SECONDS)
         readTimeout(60, TimeUnit.SECONDS)
         writeTimeout(60, TimeUnit.SECONDS)
      }.build()

      return Retrofit.Builder()
         .baseUrl(remoteStorageUrl)
         .client(okHttpClient)
         .build()
   }
}