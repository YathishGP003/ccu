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

       public static final String BASE_URL = "https://caretaker-75f-service-dev.azurewebsites.net/";
    private static Retrofit retrofit = null;
    private static Retrofit retrofitWithAuth = null;
    static boolean dialogStatus = true;
    static String responseVal="";
    static String token;

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
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(getOKClient())
                    .build();
        }
        return retrofit;
    }


    private static OkHttpClient getOKClient() throws KeyManagementException, NoSuchAlgorithmException {

        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };

        // Install the all-trusting trust manager
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        // Create an ssl socket factory with our all-trusting manager
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.sslSocketFactory(sslSocketFactory);
        httpClient.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });

        httpClient.readTimeout(60, TimeUnit.SECONDS);
        httpClient.connectTimeout(60, TimeUnit.SECONDS);
        httpClient.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                Request request = original.newBuilder()
                        .method(original.method(), original.body())
                        .build();

                return chain.proceed(request);
            }
        });

        return httpClient.build();
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


