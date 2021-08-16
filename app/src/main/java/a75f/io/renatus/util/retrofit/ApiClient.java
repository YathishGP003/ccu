package a75f.io.renatus.util.retrofit;


import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import a75f.io.logic.cloud.RenatusServicesEnvironment;
import a75f.io.logic.cloud.RenatusServicesUrls;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class ApiClient
{

    //public static final String BASE_URL = "https://caretaker-75f-service-dev.azurewebsites.net/";
    private static Retrofit retrofit = null;

    public interface ApiCallBack {
        void Success(retrofit2.Response<ResponseBody> response) throws IOException;
        void Failure(retrofit2.Response<ResponseBody> response) throws IOException;
        void Error(Throwable t);
    }

    public static Retrofit getApiClient() throws NoSuchAlgorithmException, KeyManagementException {
        if (retrofit == null) {
            final OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .readTimeout(1, TimeUnit.MINUTES)
                    .connectTimeout(1, TimeUnit.MINUTES)
                    .build();
            /*retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient)
                    .build();*/
            RenatusServicesUrls urls = RenatusServicesEnvironment.getInstance().getUrls();

            retrofit = new Retrofit.Builder()
                    .baseUrl(urls.getCaretakerUrl())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient)
                    .build();
        }
        return retrofit;
    }

    public static void getApiResponse(Call<ResponseBody> call, final ApiCallBack apiCallBack) {
        try
        {
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, retrofit2.Response<ResponseBody> response)
                {

                    try
                    {
                        if (response.isSuccessful())
                        {
                            apiCallBack.Success(response);
                        }
                        else
                        {
                            apiCallBack.Failure(response);
                        }
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    apiCallBack.Error(t);
                }
            });
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}


