package a75f.io.renatus.util.retrofit;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Created by ptbr-1167 on 4/10/17.
 */

public interface ApiInterface {
    @GET("api/v1/siteCode/site/{siteId}/code/{code}/validate")
    Call<ResponseBody> ValidateOTP(@Path("siteId") String siteId,
                                  @Path("code") String code);

}
