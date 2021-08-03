package a75f.io.logic.cloudservice;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface OtpService {
    @POST("/api/v1/siteCode/site/{siteId}/refresh")
    public Call<ResponseBody> postOTPRefresh(
            @Path("siteId") String siteId
    );

    @GET("/api/v1/siteCode/site/{siteId}")
    public Call<ResponseBody> getOTP(
            @Path("siteId") String siteId
    );

    @POST("/api/v1/siteCode/site/{siteId}/share")
    public Call<ResponseBody> postOTPShare(
            @Path("siteId") String siteId,
            @Query("emailAddress") String emailAddress
    );
}
