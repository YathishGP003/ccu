package a75f.io.logic.preconfig

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface PreconfigurationService {
    @GET("findByActivationCode")
    fun validatePasscode(@Query("activationCode") passcode: String): Call<ResponseBody>
}