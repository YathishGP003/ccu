package a75f.io.renatus.util

import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.sync.HttpUtil
import a75f.io.constants.HttpConstants
import a75f.io.logger.CcuLog
import a75f.io.logic.Globals
import a75f.io.logic.L
import a75f.io.logic.cloud.RenatusServicesEnvironment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit


object CloudStatus {
    private val renatusServicesUrls = RenatusServicesEnvironment.instance.urls

    data class ServiceStatus(
        val serviceName: String,

        val healthCheckUrl: String,
        val isUp: Boolean,

        val infoUrl: String,
        val serviceVersion: String,

        val isNetworkDown: Boolean = false
    )

    private suspend fun checkAllServices(): List<ServiceStatus> = coroutineScope {
        val isNetworkConnected = NetworkUtil.isNetworkConnected(Globals.getInstance().applicationContext)
        val services = listOf(
            "Alerts" to Pair(renatusServicesUrls.alertsHealth,
                renatusServicesUrls.alertsInfo),
            "Authorization" to Pair(
                renatusServicesUrls.authorization,
                renatusServicesUrls.authorization
            ),
            "File Storage" to Pair(
                renatusServicesUrls.fileStorageHealth,
                renatusServicesUrls.fileStorageInfo
            ),
            "Gateway Service" to Pair(
                renatusServicesUrls.gatewayApiHealth,
                renatusServicesUrls.gatewayApiInfo
            ),
            "Hayloft" to Pair(renatusServicesUrls.hayloftHealth,
                renatusServicesUrls.hayloftInfo),
            "Haystack" to Pair(
                renatusServicesUrls.haystackHealth,
                renatusServicesUrls.haystackInfo
            ),
            "Messaging" to Pair(
                renatusServicesUrls.messagingHealth,
                renatusServicesUrls.messagingInfo
            ),
            "Site Manager" to Pair(
                renatusServicesUrls.siteManagerHealth,
                renatusServicesUrls.siteManagerInfo
            ),
            "Site Sequencer" to Pair(
                renatusServicesUrls.sequenceRunnerHealth,
                renatusServicesUrls.sequenceRunnerInfo
            ),
            "Tuners" to Pair(renatusServicesUrls.tunersHealth,
                renatusServicesUrls.tunersInfo),
            "Version Management" to Pair(
                renatusServicesUrls.versionManagementHealth,
                renatusServicesUrls.versionManagementInfo
            ),
            "Weather" to Pair(renatusServicesUrls.weather,
                renatusServicesUrls.weather),
        )


        services.map { (name, urls) ->
            async(Dispatchers.IO) {
                try {
                    val healthCheckResponse = getResponse(urls.first)
                    val infoResponse: String? = if (name == "Hayloft") {
                        getHayloftVersion(urls.second)
                    } else {
                        getResponse(urls.second)
                    }
                    CcuLog.d(L.TAG_CCU_CLOUD_STATUS, "Service $name health check response: $healthCheckResponse \n" +
                            "Service $name info response: $infoResponse")

                    if (healthCheckResponse.isNullOrEmpty()) {
                        if (isNetworkConnected) {
                            CcuLog.e(L.TAG_CCU_CLOUD_STATUS, "Empty response for service $name health check but network is up")
                            return@async ServiceStatus(name, urls.first, false, urls.second, "")
                        } else {
                            CcuLog.e(L.TAG_CCU_CLOUD_STATUS, "Network is down")
                            return@async ServiceStatus(name, urls.first, false, urls.second, "", isNetworkDown = true)
                        }
                    }

                    val isUp = JSONObject(healthCheckResponse)
                        .optString("status")
                        .equals("UP", ignoreCase = true)

                    if (infoResponse.isNullOrEmpty()) {
                        CcuLog.e(L.TAG_CCU_CLOUD_STATUS, "Empty response for service $name info")
                        return@async ServiceStatus(name, urls.first, isUp, urls.second, "")
                    }

                    val serviceVersion = JSONObject(infoResponse)
                        .optJSONObject("app")
                        ?.optString("version") ?: ""

                    ServiceStatus(name, urls.first, isUp, urls.second, serviceVersion)

                } catch (e: Exception) {
                    CcuLog.e(L.TAG_CCU_CLOUD_STATUS, "Error checking service $name: ${e.message}")
                    e.printStackTrace()
                    ServiceStatus(name, urls.first, false, urls.second, "")
                }
            }

        }.awaitAll()
    }

    private fun getResponse(url: String): String? {
        return HttpUtil.executeJson(
            url,
            null,
            CCUHsApi.getInstance().getJwt(),
            false,
            HttpConstants.HTTP_METHOD_GET
        )

    }

    fun checkAllServices(callback: (List<ServiceStatus>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val result = checkAllServices()
            withContext(Dispatchers.Main) {
                callback(result)
            }
        }
    }

    // For haylot version we need to use Retrofit since it "/" suffix is not allowed in url
    interface DomainModelerApi {
        @GET("actuator/info")
        fun getActuatorInfo(): Call<ResponseBody>
    }

    private fun getCloudStatusRetrofit(baseUrl: String): Retrofit {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder().build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
    }

    private fun getHayloftVersion(baseUrl: String): String? {
        val result =
            getCloudStatusRetrofit(baseUrl).create(DomainModelerApi::class.java)
                .getActuatorInfo().execute()

        return result.body()?.string()
    }

}