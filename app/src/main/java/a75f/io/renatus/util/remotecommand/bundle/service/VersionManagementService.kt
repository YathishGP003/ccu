package a75f.io.renatus.util.remotecommand.bundle.service

import a75f.io.api.haystack.BuildConfig
import a75f.io.logger.CcuLog
import a75f.io.logic.L
import a75f.io.logic.cloud.RenatusServicesEnvironment
import a75f.io.renatus.util.remotecommand.bundle.models.BundleDTO
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface VersionManagementServiceInterface {
    @GET("/bundle/{bundleId}")
    fun retrieveBundleById(@Path("bundleId") bundleId: String): retrofit2.Call<ResponseBody>

    @GET("/bundle")
    fun retrieveRecommendedBundle(@Query("recommendedOnly") recommendedOnly: Boolean): retrofit2.Call<ResponseBody>
}

class VersionManagementService(private var bearerToken: String) {
    private val retrofit: Retrofit = getVersionManagementRetrofit()

    private fun getVersionManagementRetrofit(): Retrofit {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor(Interceptor { chain ->
                val request = chain.request().newBuilder()
                    .apply {
                        if(bearerToken.isEmpty()){
                            addHeader("api-key", BuildConfig.HAYSTACK_API_KEY)
                        } else {
                            addHeader("Authorization", "Bearer $bearerToken")
                        }
                    }
                    .build()
                chain.proceed(request)
            })
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(RenatusServicesEnvironment.instance.urls.versionManagementUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    fun retrieveBundleByName(bundleName: String): BundleDTO? {
        val resp = getVersionManagementRetrofit().create(VersionManagementServiceInterface::class.java).retrieveRecommendedBundle(false).execute()
        val response = resp.body()?.string() ?: return null

        try {
            val gson = Gson().fromJson(response, JsonArray::class.java)
            for (bundleObj in gson) {
                if (bundleObj.asJsonObject.get("name").asString == bundleName) {
                    CcuLog.i(L.TAG_CCU_UPDATE, "Found bundle: $bundleObj")
                    return BundleDTO(bundleObj.asJsonObject)
                }
            }
            return null
        } catch (e: Exception) {
            CcuLog.e(L.TAG_CCU_UPDATE, "Error processing bundle GET response: $e")
            throw e
        }
    }

    fun retrieveRecommendedBundleRetro(): BundleDTO? {
        val resp = getVersionManagementRetrofit().create(VersionManagementServiceInterface::class.java).retrieveRecommendedBundle(true).execute()
        val response = resp.body()?.string() ?: return null

        try {
            val gson = Gson().fromJson(response, JsonArray::class.java)
            for (bundleObj in gson) {
                CcuLog.i(L.TAG_CCU_UPDATE, "Found bundle: $bundleObj")
                return BundleDTO(bundleObj.asJsonObject)
            }
            return null
        } catch (e: Exception) {
            CcuLog.e(L.TAG_CCU_UPDATE, "Error processing bundle GET response: $e")
            throw e
        }
    }

    fun retrieveBundleByIdRetro(bundleId: String): BundleDTO? {
        val resp = getVersionManagementRetrofit().create(VersionManagementServiceInterface::class.java).retrieveBundleById(bundleId).execute()
        val response = resp.body()?.string() ?: return null

        try {
            val bundleObj = Gson().fromJson(response, JsonObject::class.java)
            if (bundleObj.asJsonObject.get("id").asString == bundleId) {
                CcuLog.i(L.TAG_CCU_UPDATE, "Found bundle: $bundleObj")
                return BundleDTO(bundleObj.asJsonObject)
            }
        } catch (e: Exception) {
            CcuLog.e(L.TAG_CCU_UPDATE, "Error processing bundle GET response: $response")
            throw e
        }
        return null
    }
}