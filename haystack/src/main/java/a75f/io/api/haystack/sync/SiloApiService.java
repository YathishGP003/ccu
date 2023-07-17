package a75f.io.api.haystack.sync;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Url;


public interface SiloApiService {

        @POST
        Call<ResponseBody> postData(
                @Url String url,
                @Body RequestBody body
        );

        @PUT
        Call<ResponseBody> putData(
                @Url String url,
                @Body RequestBody body
        );

        @GET
        Call<ResponseBody> getData(
                @Url String url
        );
    }

