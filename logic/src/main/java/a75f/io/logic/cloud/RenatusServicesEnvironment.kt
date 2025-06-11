package a75f.io.logic.cloud

import a75f.io.alerts.AlertManager
import a75f.io.api.haystack.CCUHsApi
import a75f.io.logic.BuildConfig
import android.annotation.SuppressLint
import android.content.SharedPreferences
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import java.net.InetAddress

/**
 * Provides set of URLs for Renatus Services.
 *
 * If this is a production, or staging, qa or dev build, the URLs come directly from build config.
 *
 * If this is a local build, i.e. pointed to a sandbox, the URLs are constructed based on a chosen
 * base IP address which may come from the build config or be overridden in shared preferences from
 * a previous user override action.
 *
 * @author Tony Case (case.tony@gmail.com)
 * Created on 1/29/21.
 */

private const val HTTP = "http://"

private const val ALERTS_EXT = ":8087"

private const val CARETAKER_EXT = ":3100/api/v1/"
private const val HAYSTACK_EXT = ":8085/v1/"
private const val FILESTORAGE_EXT = ":8081"
private const val MESSAGING_EXT = ":8082"
private const val WEATHER_EXT = ":3001/api/v1"
private const val CCU_VERSION_EXT = ""

private const val PREF_LOCAL_BASE_IP_VALUE = "pref_local_base_ip_value"
private const val PREF_LOCAL_BASE_IP_ALL_VALUES = "pref_local_base_ip_all_values"


// @Singleton
class RenatusServicesEnvironment(
   val sharedPreferences: SharedPreferences
) {

   // use this instance creation until we introduce DI
   companion object {

      @JvmStatic
      lateinit var instance: RenatusServicesEnvironment

      @JvmStatic
      fun createWithSharedPrefs(sharedPreferences: SharedPreferences): RenatusServicesEnvironment {
         if (! this::instance.isInitialized) {
            instance = RenatusServicesEnvironment(sharedPreferences)
         }
         return instance
      }
   }

   /**
    * The set of Renatus service URLs, i.e. for Caretaker, Haystack, Alerts & Filestorage.
    */
   val urls: RenatusServicesUrls
      get() {
         return when (BuildConfig.BUILD_TYPE) {
            "local" -> localServiceUrls
            else -> nonLocalServiceUrls
         }
      }

   private val nonLocalServiceUrls: RenatusServicesUrls
      get() {
         return RenatusServicesUrls(
            BuildConfig.CARETAKER_API_BASE,
            BuildConfig.HAYSTACK_API_BASE,
            BuildConfig.ALERTS_API_BASE,
            BuildConfig.FILE_STORAGE_API_BASE,
            BuildConfig.MESSAGING_API_BASE,
            BuildConfig.WEATHER_API_BASE,
            BuildConfig.GATEWAY_API_BASE,
            BuildConfig.CCU_VERSION_API_BASE,
            BuildConfig.CCU_FILE_SIZE_API_BASE,
            BuildConfig.SEQUENCER_API_BASE,
            BuildConfig.VERSION_MANAGEMENT_API_BASE,
            BuildConfig.PRECONFIG_API_BASE,
            BuildConfig.ALERTS_HEALTH,
            BuildConfig.AUTHORIZATION,
            BuildConfig.FILE_STORAGE_HEALTH,
            BuildConfig.GATEWAY_API_HEALTH,
            BuildConfig.HAYLOFT_HEALTH,
            BuildConfig.HAYSTACK_HEALTH,
            BuildConfig.MESSAGING_HEALTH,
            BuildConfig.SITE_MANAGER_HEALTH,
            BuildConfig.SEQUENCE_RUNNER_HEALTH,
            BuildConfig.WEATHER,
            BuildConfig.TUNERS_HEALTH,
            BuildConfig.VERSION_MANAGEMENT_HEALTH,

            BuildConfig.ALERTS_INFO,
            BuildConfig.FILE_STORAGE_INFO,
            BuildConfig.GATEWAY_API_INFO,
            BuildConfig.HAYLOFT_INFO,
            BuildConfig.HAYSTACK_INFO,
            BuildConfig.MESSAGING_INFO,
            BuildConfig.SITE_MANAGER_INFO,
            BuildConfig.SEQUENCE_RUNNER_INFO,
            BuildConfig.TUNERS_INFO,
            BuildConfig.VERSION_MANAGEMENT_INFO
         )
      }

   private val localServiceUrls: RenatusServicesUrls
      get() {
         val baseIp = getLocalBaseIp()
         return RenatusServicesUrls(
            HTTP + baseIp + CARETAKER_EXT,
            HTTP + baseIp + HAYSTACK_EXT,
            HTTP + baseIp + ALERTS_EXT,
            HTTP + baseIp + FILESTORAGE_EXT,
            HTTP + baseIp + MESSAGING_EXT,
            HTTP + baseIp + WEATHER_EXT,
            HTTP + baseIp,
            HTTP + baseIp + CCU_VERSION_EXT,
            HTTP + baseIp +CCU_VERSION_EXT,
            HTTP + baseIp +CCU_VERSION_EXT,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp,
            HTTP + baseIp
         )
      }

   @SuppressLint("ApplySharedPref")
   fun setLocalBaseIpAddress(ipAddress: String): Single<Boolean> {

      sharedPreferences.edit().putString(PREF_LOCAL_BASE_IP_VALUE, ipAddress).commit()
      setupUrls()
      return pingServices()
         .doOnSuccess { isReachable -> if (isReachable) addToPrepopulateList(ipAddress) }
   }

   /**
    * Return a Single observable indicating whether our local base Ip address is reachable or not.
    * Returns Single.error in the case where Ip address is malformed or some other unexpected
    * network error occurs.
    */
   fun pingServices(): Single<Boolean>  {  // String, HttpResponse code, something.
      return Single.create<Boolean> { observer ->
         try {
            val isReachable = InetAddress.getByName(getLocalBaseIp())
               .isReachable(3_000)
            observer.onSuccess(isReachable)
            if (isReachable) addToPrepopulateList(getLocalBaseIp())
         } catch (ex: Exception) {
            observer.onError(ex)
         }
      }.subscribeOn(Schedulers.io())
   }

   /**
    * A list of past base IP addressed for local sandbox.
    */
   fun getIpListForPrepopulate(): List<String> =
      sharedPreferences.getStringSet(PREF_LOCAL_BASE_IP_ALL_VALUES, emptySet())?.toList() ?: emptyList()

   fun getLocalBaseIp() =
      sharedPreferences.getString(PREF_LOCAL_BASE_IP_VALUE, BuildConfig.API_BASE_IP) ?: "missing"

   private fun addToPrepopulateList(ipAddress: String) {
      val list: MutableSet<String> = sharedPreferences
         .getStringSet(PREF_LOCAL_BASE_IP_ALL_VALUES, null) ?: mutableSetOf()
      list.add(ipAddress)
      sharedPreferences.edit().putStringSet(PREF_LOCAL_BASE_IP_ALL_VALUES, list).apply()
   }

   private fun setupUrls() {
      AlertManager.getInstance().setAlertsApiBase(urls.alertsUrl)
      CCUHsApi.getInstance().resetBaseUrls(urls.haystackUrl, urls.caretakerUrl, urls.gatewayUrl)
   }
}

data class RenatusServicesUrls(
   val caretakerUrl: String,
   val haystackUrl: String,
   val alertsUrl: String,
   val remoteStorageUrl: String,
   val messagingUrl: String,
   val weatherUrl: String,
   val gatewayUrl : String,
   val recommendedCCUVersion : String,
   val getCCUFileSize : String,
   val sequencerUrl: String,
   val versionManagementUrl: String,
   val preconfigurationUrl: String,
   val alertsHealth: String,
   val authorization: String,
   val fileStorageHealth: String,
   val gatewayApiHealth: String,
   val hayloftHealth: String,
   val haystackHealth: String,
   val messagingHealth: String,
   val siteManagerHealth: String,
   val sequenceRunnerHealth: String,
   val weather: String,
   val tunersHealth: String,
   val versionManagementHealth: String,
   val alertsInfo: String,
   val fileStorageInfo: String,
   val gatewayApiInfo: String,
   val hayloftInfo: String,
   val haystackInfo: String,
   val messagingInfo: String,
   val siteManagerInfo: String,
   val sequenceRunnerInfo: String,
   val tunersInfo: String,
   val versionManagementInfo: String,
) {
   // useful for local environment
   val base: String
      get() = alertsUrl.removePrefix(HTTP).removeSuffix(ALERTS_EXT)
}
