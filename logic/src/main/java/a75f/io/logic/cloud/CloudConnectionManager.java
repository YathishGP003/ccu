package a75f.io.logic.cloud;

import static a75f.io.constants.HttpConstants.APP_NAME_HEADER_NAME;
import static a75f.io.constants.HttpConstants.APP_NAME_HEADER_VALUE;

import android.app.AlarmManager;
import android.content.Context;

import org.jetbrains.annotations.NotNull;
import org.projecthaystack.HDateTime;
import org.projecthaystack.io.HZincReader;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
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
                    String bearerToken =  CCUHsApi.getInstance().getJwt();

                    Request originalRequest = chain.request();
                    Request newRequest = originalRequest.newBuilder()
                            .header(APP_NAME_HEADER_NAME, APP_NAME_HEADER_VALUE)
                            .header("Authorization", "Bearer " + bearerToken)
                            .build();

                    CcuLog.d("CCU_HTTP_REQUEST", "CloudConnectionManager: [" + chain.request().method() + "] " + chain.request().url() + " - Token: " + bearerToken);

                    return chain.proceed(newRequest);
                })
                .addInterceptor(loggingInterceptor)
                .addInterceptor(chain -> {
                        Request request = chain.request();
                        okhttp3.Response response = chain.proceed(request);

                        CcuLog.d("CCU_HTTP_RESPONSE", "CloudConnectionManager: " + response.code() + " - [" + request.method() + "] " + request.url());
                        return response;
                 })
                .connectTimeout(50, TimeUnit.SECONDS)
                .writeTimeout(50, TimeUnit.SECONDS)
                .readTimeout(50, TimeUnit.SECONDS)
                .build();
        return okHttpClient;
    }





    /**
     * This method hits /about end point update System time in CCU and also checks if /about is reachable from CCU
     * @param responseCallback
     */
    public void processAboutResponse(CloudConnectionResponseCallback responseCallback){
        if (!CCUHsApi.getInstance().isCCURegistered()) {
            return;
        }

        Retrofit retrofit = getRetrofitForHaystackBaseUrl();
            Call<ResponseBody> call = retrofit.create(CloudConnectionService.class).getAbout();
            long requestTime = new Date().getTime();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    call.cancel();
                }
            });

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if(response.isSuccessful()) {
                        String result = "";
                        try {
                            result = response.body().string();
                            List<HashMap> rowList = CCUHsApi.getInstance().HGridToList(new HZincReader(result).readGrid());
                            if (rowList.size()>0 && rowList.get(0).containsKey("serverTime")) {
                                long siloCurrTime = HDateTime.make(rowList.get(0).get("serverTime").toString()).millis();
                                if(Math.abs(siloCurrTime - System.currentTimeMillis()) > (30 * 1000)){
                                    Context appContext = Globals.getInstance().getApplicationContext();
                                    AlarmManager alarmMgr = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
                                    alarmMgr.setTime(siloCurrTime);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            CcuLog.i(TAG_CLOUD_CONNECTION_STATUS,
                                    "Exception occurred while processing /about response : " + result);
                        }
                    }
                    CcuLog.i(TAG_CLOUD_CONNECTION_STATUS,
                            "Time taken for the success response " + (new Date().getTime() - requestTime));
                    responseCallback.onSuccessResponse(response.isSuccessful());
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                        CcuLog.i(TAG_CLOUD_CONNECTION_STATUS,
                                    "Time taken for the failed  response " + (new Date().getTime() - requestTime));
                        CcuLog.e(TAG_CLOUD_CONNECTION_STATUS, "Error while Checking cloud connection status " + t.getMessage(),
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
