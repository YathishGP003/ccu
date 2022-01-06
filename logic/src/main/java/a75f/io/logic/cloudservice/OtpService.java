package a75f.io.logic.cloudservice;

import java.util.HashMap;
import java.util.Map;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface OtpService {
    @POST("/api/v1/siteCode/site/{siteId}/refresh")
    Call<ResponseBody> postOTPRefresh(
            @Path("siteId") String siteId
    );

    @GET("/api/v1/siteCode/site/{siteId}")
    Call<ResponseBody> getOTP(
            @Path("siteId") String siteId
    );

    @POST("/api/v1/siteCode/site/{siteId}/share")
    Call<ResponseBody> postOTPShare(
            @Path("siteId") String siteId,
            @Query("emailAddress") String emailAddress
    );

    @GET("/api/v1/siteCode/{code}/validate")
    Call<ResponseBody> ValidateOTP(
            @Path("code") String code
    );

    @POST("/api/v1/devices/{deviceId}/token")
    Call<ResponseBody> postBearerToken(
            @Path("deviceId") String deviceId,
            @Body Map<String, Object> requestBody
            );


}
