package a75f.io.renatus.util.retrofit;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by Aniket on 19/07/21.
 */

public interface ApiInterface {
    @GET("siteCode/{code}/validate")
    Call<ResponseBody> ValidateOTP(@Path("code") String code);

}
