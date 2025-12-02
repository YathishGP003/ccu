package a75f.io.renatus.registration

import a75f.io.api.haystack.BuildConfig
import a75f.io.api.haystack.CCUHsApi
import a75f.io.api.haystack.sync.HttpUtil
import a75f.io.constants.HttpConstants
import a75f.io.logger.CcuLog
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.concurrent.CountDownLatch

class CountryCacheManager {

    companion object {

        private const val FILE_NAME = "countries.json"
        private const val TAG = "COUNTRIES_CACHE"
        private const val STATIC_COUNTRIES =
            "[{\"id\":\"c4252b58-eff1-4e14-85ba-5d138f940cc3\",\"country\":\"India\",\"region\":\"South Asia\",\"label\":\"IN\"},{\"id\":\"f4aaeae7-daf0-46bc-b686-ce26b7c112e8\",\"country\":\"United Kingdom\",\"region\":\"Europe\",\"label\":\"UK\"},{\"id\":\"07290835-a1a2-4bb2-a448-e79d03626ef9\",\"country\":\"Antigua & Barbuda\",\"region\":\"abc\",\"label\":\"UK\"},{\"id\":\"e8455fab-797d-4783-ba8f-66867f23bf3e\",\"country\":\"St. Barthélemy\",\"region\":\"Island country\",\"label\":\"BL\"},{\"id\":\"82eeba27-32ef-4a08-8451-18fb37b2cd7b\",\"country\":\"Åland Islands\",\"region\":\"Autonomous region\",\"label\":\"ALA\"},{\"id\":\"f3be6da1-6f95-452e-88b3-520dc76d250a\",\"country\":\"Iceland\",\"region\":\"Autonomous region\",\"label\":\"ALA\"},{\"id\":\"4a8b0dda-b632-4b34-8e36-24938cb7ba91\",\"country\":\"Oman\",\"region\":\"Autonomous region\",\"label\":\"ALA\"}]\n";
        fun getCountries(context: Context, shouldFetch: Boolean): ArrayList<CountryItem> {

            val countryItems = ArrayList<CountryItem>()
            val latch = CountDownLatch(1)

            Thread {

                try {
                    val json = if (shouldFetch) {
                        val fresh = fetchFromBackend()
                        if (fresh != null) {
                            CcuLog.d(TAG, "res from backend :  response : $fresh")
                            saveCache(context, fresh)
                            fresh
                        } else {
                            CcuLog.d(TAG, "country response is null")
                            readCache(context) ?: STATIC_COUNTRIES
                        }
                    } else {
                        CcuLog.d(TAG, "country response is null -  shouldFetch: false")
                        readCache(context) ?: STATIC_COUNTRIES
                    }

                    val listType = object : TypeToken<List<CountryItem>>() {}.type
                    val parsedList = Gson().fromJson<ArrayList<CountryItem>>(json, listType)

                    countryItems.addAll(parsedList)

                    CcuLog.d(TAG, "Total Countries: ${parsedList.size}")

                    parsedList.forEach {
                        CcuLog.d(TAG, "country: ${it.country}")
                    }

                } catch (e: Exception) {
                    CcuLog.e(TAG, "Error loading countries", e)
                } finally {
                    latch.countDown()
                }

            }.start()

            // Wait for thread to complete
            try {
                latch.await()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            return countryItems
        }

        private fun fetchFromBackend(): String? {
            return try {
                val response = HttpUtil.executeJsonWithApiKey(
                    CCUHsApi.getInstance().authenticationUrl + "country/fetchAll",
                    null,
                    BuildConfig.CARETAKER_API_KEY,
                    HttpConstants.HTTP_METHOD_GET
                )
                response?.responseMessage
            } catch (e: Exception) {
                e.printStackTrace();
                CcuLog.d(TAG, "exception while fetching countries.")
                null
            }
        }

        private fun saveCache(context: Context, data: String) {
            try {
                val file = File(context.filesDir, FILE_NAME)
                file.writeText(data, Charsets.UTF_8)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun readCache(context: Context): String? {
            val file = File(context.filesDir, FILE_NAME)
            return if (file.exists()) file.readText(Charsets.UTF_8) else null
        }
    }
}
