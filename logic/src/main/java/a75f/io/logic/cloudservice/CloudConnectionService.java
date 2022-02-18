package a75f.io.logic.cloudservice;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;

public interface CloudConnectionService {
    @GET("/about")
    Call<ResponseBody> getAbout();

}