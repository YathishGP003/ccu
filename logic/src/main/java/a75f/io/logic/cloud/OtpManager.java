package a75f.io.logic.cloud;


import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.ccu.restore.CCU;
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

    private static final String TAG_CCU_OTP_BUILDING_PASSCODE = "CCU_BUILDING_PASSCODE";
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

    public void validateOTP(String enteredOTP, ResponseCallback responseCallBack){
        Retrofit retrofit = getRetrofitForCareTakerBaseUrl();
        Call<ResponseBody> call = retrofit.create(OtpService.class).ValidateOTP(enteredOTP);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    try {
                        responseCallBack.onErrorResponse(new JSONObject("{\"response\": \"ERROR\"}"));
                        CcuLog.e(TAG_CCU_OTP_BUILDING_PASSCODE, new Gson().toJson(response.errorBody().string()));
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                try {
                    String result = response.body().string();
                    CcuLog.i(TAG_CCU_OTP_BUILDING_PASSCODE, result);
                    responseCallBack.onSuccessResponse(new JSONObject(result));
                } catch (Exception e) {
                    e.printStackTrace();
                    CcuLog.e(TAG_CCU_OTP_BUILDING_PASSCODE, e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                CcuLog.e(TAG_CCU_OTP_BUILDING_PASSCODE, "Error while reading Building Passcode "+t.getMessage(), t);
                try {
                    responseCallBack.onErrorResponse(new JSONObject("{\"response\": \"ERROR\"}"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void postOTPRefresh(String siteID, ResponseCallback responseCallBack){
        Retrofit retrofit = getRetrofitForCareTakerBaseUrl();
        Call<ResponseBody> call = retrofit.create(OtpService.class).postOTPRefresh(siteID);
        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    try {
                        responseCallBack.onErrorResponse(new JSONObject("{\"response\": \"ERROR\"}"));
                        CcuLog.e(TAG_CCU_OTP_BUILDING_PASSCODE, new Gson().toJson(response.errorBody().string()));
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                try {
                    String result = response.body().string();
                    CcuLog.i(TAG_CCU_OTP_BUILDING_PASSCODE, result);
                    responseCallBack.onSuccessResponse(new JSONObject(result));
                } catch (Exception e) {
                    e.printStackTrace();
                    CcuLog.e(TAG_CCU_OTP_BUILDING_PASSCODE, e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                CcuLog.e(TAG_CCU_OTP_BUILDING_PASSCODE, "Error while reading OTP "+t.getMessage(), t);
                try {
                    responseCallBack.onErrorResponse(new JSONObject("{\"response\": \"ERROR\"}"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void getOTP(String siteID, ResponseCallback responseCallBack, boolean isFromAboutPage){
        Retrofit retrofit = getRetrofitForCareTakerBaseUrl();
        Call<ResponseBody> call = retrofit.create(OtpService.class).getOTP(siteID);
        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    try {
                        CcuLog.e(TAG_CCU_OTP_BUILDING_PASSCODE, new Gson().toJson(response.errorBody().string()));
                        responseCallBack.onErrorResponse(new JSONObject("{\"response\": \"ERROR\"}"));
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                try {
                    String result = response.body().string();
                    CcuLog.i(TAG_CCU_OTP_BUILDING_PASSCODE, result);
                    JSONObject otpResponse = new JSONObject(result);
                    if((Boolean) ((JSONObject) otpResponse.get("siteCode")).get("expired")){
                        if(isFromAboutPage){
                            responseCallBack.onSuccessResponse(new JSONObject("{\"siteCode\":{\"code\":\"\"}}"));
                        }
                        else{
                            postOTPRefresh(siteID, responseCallBack);
                        }
                        return;

                    }
                    responseCallBack.onSuccessResponse(otpResponse);
                } catch (Exception e) {
                    e.printStackTrace();
                    CcuLog.e(TAG_CCU_OTP_BUILDING_PASSCODE, e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                CcuLog.e(TAG_CCU_OTP_BUILDING_PASSCODE, "Error while reading OTP "+t.getMessage(), t);
                try {
                    responseCallBack.onErrorResponse(new JSONObject("{\"response\": \"ERROR\"}"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void postOTPShare(String siteID, String emailAddress, ResponseCallback responseCallBack){
        Retrofit retrofit = getRetrofitForCareTakerBaseUrl();
        Call<ResponseBody> call = retrofit.create(OtpService.class).postOTPShare(siteID, emailAddress);
        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    try {
                        CcuLog.e(TAG_CCU_OTP_BUILDING_PASSCODE, new Gson().toJson(response.errorBody().string()));
                        responseCallBack.onSuccessResponse(new JSONObject("{\"response\": \"ERROR\"}"));
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                    }
                    return;
                }
                try {
                    responseCallBack.onSuccessResponse(new JSONObject("{\"response\": \"OK\"}"));
                } catch (Exception e) {
                    e.printStackTrace();
                    CcuLog.e(TAG_CCU_OTP_BUILDING_PASSCODE, e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                try {
                    responseCallBack.onSuccessResponse(new JSONObject("{\"response\": \"ERROR\"}"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                CcuLog.e(TAG_CCU_OTP_BUILDING_PASSCODE, "Error while reading OTP "+t.getMessage(), t);
            }
        });
    }

    public void postBearerToken(CCU ccu, String buildingPassCode, ResponseCallback responseCallBack){
        Retrofit retrofit = getRetrofitForCareTakerBaseUrl();
        Map<String, Object> reqParams = new HashMap<>();
        reqParams.put("siteCode", buildingPassCode);
        Call<ResponseBody> call =
                retrofit.create(OtpService.class).postBearerToken(StringUtils.prependIfMissing(ccu.getCcuId(), "@"), reqParams);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try{
                    if(response.isSuccessful()){
                        JSONObject responseJSON = new JSONObject(response.body().string());
                        CCUHsApi.getInstance().setJwt(responseJSON.getString("accessToken"));
                        responseCallBack.onSuccessResponse(responseJSON);
                    }
                    else{
                        responseCallBack.onErrorResponse(new JSONObject("{\"response\": \"ERROR\"}"));
                    }
                } catch(Exception ex){
                    ex.printStackTrace();
                }

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                try {
                    responseCallBack.onErrorResponse(new JSONObject("{\"response\": \"ERROR\"}"));
                    t.printStackTrace();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
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
