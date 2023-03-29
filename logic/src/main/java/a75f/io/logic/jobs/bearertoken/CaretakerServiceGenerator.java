package a75f.io.logic.jobs.bearertoken;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.constants.HttpConstants;
import a75f.io.logger.CcuLog;
import a75f.io.logic.cloud.RenatusServicesEnvironment;
import a75f.io.logic.cloud.RenatusServicesUrls;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

class CaretakerServiceGenerator {
    
    private static Retrofit getRetrofitClient(String bearerToken) {
        
        OkHttpClient httpClient = new OkHttpClient.Builder()
                                      .addInterceptor(chain -> {
                                          Request original = chain.request();

                                          Request.Builder requestBuilder = original.newBuilder()
                                                                                   .header("Authorization", " Bearer " + bearerToken)
                                                                                   .addHeader("Content-Type", "application/json")
                                                                                   .addHeader(HttpConstants.APP_NAME_HEADER_NAME, HttpConstants.APP_NAME_HEADER_VALUE)
                                                                                   .method(original.method(), original.body());
                
                                          Request request = requestBuilder.build();

                                          CcuLog.d("CCU_HTTP_REQUEST", "CaretakerService: [" + chain.request().method() + "] " + chain.request().url() + " - Token: " + bearerToken);

                                          return chain.proceed(request);
                                      })
                                      .addInterceptor(chain -> {
                                          Request request = chain.request();
                                          Response response = chain.proceed(request);

                                          CcuLog.d("CCU_HTTP_RESPONSE", "CaretakerService: " + response.code() + " - [" + request.method() + "] " + request.url());
                                          return response;
                                      })
                                      .build();

        RenatusServicesUrls urls = RenatusServicesEnvironment.getInstance().getUrls();
        
        Retrofit retrofit = new Retrofit.Builder()
                                .baseUrl(urls.getCaretakerUrl())
                                .addConverterFactory(GsonConverterFactory.create())
                                .client(httpClient)
                                .build();
        
        return retrofit;
    }
    
    public static <S> S createService(Class<S> serviceClass, String bearerToken) {
        return getRetrofitClient(bearerToken).create(serviceClass);
    }
}
