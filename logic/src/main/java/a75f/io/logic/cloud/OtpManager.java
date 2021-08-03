package a75f.io.logic.cloud;


import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.cloudservice.OtpService;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OtpManager {

    private static final String TAG_CCU_OTP = "CCU_OTP";
    private OkHttpClient getOkHttpClient(){
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request originalRequest = chain.request();
                    Request newRequest = originalRequest.newBuilder()
                            .header("Authorization", "Bearer "+ CCUHsApi.getInstance().getJwt())
                            .addHeader("Content-Type", "application/json")
                            .build();
                    return chain.proceed(newRequest);
                })
                .addInterceptor(loggingInterceptor)
                .build();
        return okHttpClient;
    }

    public void postOTPRefresh(String siteID, OtpResponseCallBack otpResponseCallBack){
        Retrofit retrofit = getRetrofitForCareTakerBaseUrl();
        Call<ResponseBody> call = retrofit.create(OtpService.class).postOTPRefresh(siteID);
        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    try {
                        otpResponseCallBack.onOtpErrorResponse(new JSONObject("{\"response\": \"ERROR\"}"));
                        CcuLog.e(TAG_CCU_OTP, new Gson().toJson(response.errorBody().string()));
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                try {
                    String result = response.body().string();
                    CcuLog.i(TAG_CCU_OTP, result);
                    otpResponseCallBack.onOtpResponse(new JSONObject(result));
                } catch (Exception e) {
                    e.printStackTrace();
                    CcuLog.e(TAG_CCU_OTP, e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                CcuLog.e(TAG_CCU_OTP, "Error while reading OTP "+t.getMessage(), t);
                try {
                    otpResponseCallBack.onOtpErrorResponse(new JSONObject("{\"response\": \"ERROR\"}"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void getOTP(String siteID, OtpResponseCallBack otpResponseCallBack){
        Retrofit retrofit = getRetrofitForCareTakerBaseUrl();
        Call<ResponseBody> call = retrofit.create(OtpService.class).getOTP(siteID);
        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    try {
                        CcuLog.e(TAG_CCU_OTP, new Gson().toJson(response.errorBody().string()));
                        otpResponseCallBack.onOtpErrorResponse(new JSONObject("{\"response\": \"ERROR\"}"));
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                try {
                    String result = response.body().string();
                    CcuLog.i(TAG_CCU_OTP, result);
                    JSONObject otpResponse = new JSONObject(result);
                    if((Boolean) ((JSONObject) otpResponse.get("siteCode")).get("expired")){
                        postOTPRefresh(siteID, otpResponseCallBack);
                        return;
                    }
                    otpResponseCallBack.onOtpResponse(otpResponse);
                } catch (Exception e) {
                    e.printStackTrace();
                    CcuLog.e(TAG_CCU_OTP, e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                CcuLog.e(TAG_CCU_OTP, "Error while reading OTP "+t.getMessage(), t);
                try {
                    otpResponseCallBack.onOtpErrorResponse(new JSONObject("{\"response\": \"ERROR\"}"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void postOTPShare(String siteID, String emailAddress, OtpResponseCallBack otpResponseCallBack){
        Retrofit retrofit = getRetrofitForCareTakerBaseUrl();
        Call<ResponseBody> call = retrofit.create(OtpService.class).postOTPShare(siteID, emailAddress);
        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    try {
                        CcuLog.e(TAG_CCU_OTP, new Gson().toJson(response.errorBody().string()));
                        otpResponseCallBack.onOtpResponse(new JSONObject("{\"response\": \"ERROR\"}"));
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                try {
                    otpResponseCallBack.onOtpResponse(new JSONObject("{\"response\": \"OK\"}"));
                } catch (Exception e) {
                    e.printStackTrace();
                    CcuLog.e(TAG_CCU_OTP, e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                try {
                    otpResponseCallBack.onOtpResponse(new JSONObject("{\"response\": \"ERROR\"}"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                CcuLog.e(TAG_CCU_OTP, "Error while reading OTP "+t.getMessage(), t);
            }
        });
    }

    @NotNull
    private Retrofit getRetrofitForCareTakerBaseUrl() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(RenatusServicesEnvironment.getInstance().getUrls().getCaretakerUrl())
                .client(getOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit;
    }
}
