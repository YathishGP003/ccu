package a75f.io.logic.jobs.bearertoken;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

interface CaretakerService {
    
    @GET("/api/v1/devices/{deviceId}/token/refresh")
    Call<BearerToken> getAccessToken(@Path("deviceId") String deviceId);
    
}
