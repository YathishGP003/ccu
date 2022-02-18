package a75f.io.logic.cloud;

import org.jetbrains.annotations.NotNull;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.cloudservice.CloudConnectionService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CloudConnectionManager {
    private static final String TAG_CLOUD_CONNECTION_STATUS = "CLOUD_CONNECTION_STATUS";

    private OkHttpClient getOkHttpClient(){
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request originalRequest = chain.request();
                    Request newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer "+ CCUHsApi.getInstance().getJwt())
                            .build();
                    return chain.proceed(newRequest);
                })
                .addInterceptor(loggingInterceptor)
                .build();
        return okHttpClient;
    }

    public void getCloudConnectivityStatus(CloudConnectionResponseCallback responseCallback){
        Retrofit retrofit = getRetrofitForHaystackBaseUrl();
        Call<ResponseBody> call = retrofit.create(CloudConnectionService.class).getAbout();
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                responseCallback.onSuccessResponse(response.isSuccessful());
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                CcuLog.e(TAG_CLOUD_CONNECTION_STATUS, "Error while Checking cloud connection status "+t.getMessage(),
                        t);
                responseCallback.onErrorResponse(false);
            }
        });
    }

    @NotNull
    private Retrofit getRetrofitForHaystackBaseUrl() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RenatusServicesEnvironment.getInstance().getUrls().getHaystackUrl().replace("v1", "v2"))
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit;
    }
}
