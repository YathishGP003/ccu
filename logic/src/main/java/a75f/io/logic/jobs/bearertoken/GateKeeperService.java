package a75f.io.logic.jobs.bearertoken;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

interface GateKeeperService {
    
    @GET("/device/{deviceId}/refreshToken")
    Call<BearerToken> getAccessToken(@Path("deviceId") String deviceId);
    
}
