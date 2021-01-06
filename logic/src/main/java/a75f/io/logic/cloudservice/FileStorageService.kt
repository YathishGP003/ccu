package a75f.io.logic.cloudservice

import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.BuildConfig
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
 */
class ServiceGenerator(
   haystack: CCUHsApi
) {

   private val retrofit: Retrofit =
      createRetrofit(haystack.jwt)


   fun provideFileStorageService(): FileStorageService =
      retrofit.create(FileStorageService::class.java)


   private fun createRetrofit(token: String): Retrofit {
      val okhttpLogging = HttpLoggingInterceptor().apply {
         level = HttpLoggingInterceptor.Level.HEADERS
      }

      val okHttpClient = OkHttpClient.Builder().apply {
         addInterceptor(
            Interceptor { chain ->
               val builder = chain.request().newBuilder()
               builder.header("Authorization", "Bearer $token")
               builder.header("Accept-Encoding", "gzip, deflate, br")
               return@Interceptor chain.proceed(builder.build())
            }
         )
         addInterceptor(okhttpLogging)
      }.build()

      return Retrofit.Builder()
         .baseUrl(BuildConfig.FILE_STORAGE_API_BASE)
         .client(okHttpClient)
         .build()
   }
}