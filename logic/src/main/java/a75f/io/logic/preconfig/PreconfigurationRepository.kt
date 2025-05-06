package a75f.io.logic.preconfig

import a75f.io.logger.CcuLog
import a75f.io.logic.L
import android.content.Context
import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Response


object PreconfigurationRepository {

    @JvmOverloads
    fun getConfigurationData(data: String = "", context: Context) : PreconfigurationData? {
        val jsonData = data.ifEmpty {
            context.getSharedPreferences("preconfiguration_data", Context.MODE_PRIVATE)
                    .getString("data", null)
        }
        val gson = Gson()
        return gson.fromJson(jsonData, PreconfigurationData::class.java)
    }

    fun fetchPreconfigurationData(passcode: String): String? {
        CcuLog.d(L.TAG_PRECONFIGURATION, "PreconfigurationRepository : fetchPreconfigurationData: $passcode")
        val call = PreconfigurationRetrofitClient.apiService.validatePasscode(passcode)
        val response: Response<ResponseBody> = call.execute() // blocking
        return if (response.isSuccessful) {
            val rawJson = response.body()?.string()
            CcuLog.d(L.TAG_PRECONFIGURATION, "Raw JSON: $rawJson")
            rawJson
        } else {
            CcuLog.e(L.TAG_PRECONFIGURATION, "Failed with code: ${response.code()}")
            null
        }
    }

    fun persistPreconfigurationData(jsonString : String, context: Context) {
        val sharedPreferences = context.getSharedPreferences("preconfiguration_data", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("data", jsonString)
        editor.apply()
    }
}