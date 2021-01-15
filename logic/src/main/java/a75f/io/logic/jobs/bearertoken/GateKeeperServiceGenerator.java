package a75f.io.logic.jobs.bearertoken;

import a75f.io.api.haystack.BuildConfig;
import a75f.io.api.haystack.CCUHsApi;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

class GateKeeperServiceGenerator {
    
    private static Retrofit getRetrofitClient(String bearerToken) {
        
        OkHttpClient httpClient = new OkHttpClient.Builder()
                                      .addInterceptor(chain -> {
                                          Request original = chain.request();
                                          Request.Builder requestBuilder = original.newBuilder()
                                                                                   .header("Authorization", " Bearer " + bearerToken)
                                                                                   .addHeader("Content-Type", "application/json")
                                                                                   .method(original.method(), original.body());
                
                                          Request request = requestBuilder.build();
                                          return chain.proceed(request);
                                      })
                                      .build();
        
        Retrofit retrofit = new Retrofit.Builder()
                                .baseUrl(BuildConfig.GATEKEEPER_API_BASE)
                                .addConverterFactory(GsonConverterFactory.create())
                                .client(httpClient)
                                .build();
        
        return retrofit;
    }
    
    public static <S> S createService(Class<S> serviceClass, String bearerToken) {
        return getRetrofitClient(bearerToken).create(serviceClass);
    }
}
