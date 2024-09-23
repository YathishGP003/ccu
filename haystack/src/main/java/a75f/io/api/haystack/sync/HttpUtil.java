package a75f.io.api.haystack.sync;

import static a75f.io.api.haystack.CCUTagsDb.TAG_CCU_HS;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.constants.HttpConstants;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import a75f.io.logger.CcuLog;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class HttpUtil {

    public static final int HTTP_RESPONSE_OK = 200;
    public static final int HTTP_RESPONSE_ERR_REQUEST = 400;
    public static final int HTTP_RESPONSE_UNAUTHORIZED = 401;

    private static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;

    private static OkHttpClient okHttpClient = null;

    public static String executePost(String targetURL, String urlParameters) {
        return executePost(targetURL, urlParameters, CCUHsApi.getInstance().getJwt()); // TODO Matt Rudd - I hate this hack, but the executePost needs a complete rewrite
    }

    public static final MediaType ZINC = MediaType.get("text/zinc; charset=utf-8");
    private static Response postSync(String url, String params, String token) {
        RequestBody body = RequestBody.create(params, ZINC);
        Request request = new Request.Builder()
                .url(url)
                .addHeader(HttpConstants.APP_NAME_HEADER_NAME, HttpConstants.APP_NAME_HEADER_VALUE)
                .addHeader("Content-Length", "" + params.getBytes(StandardCharsets.UTF_8).length)
                .addHeader("Content-Language", "en-US")
                .addHeader("Authorization", " Bearer " + token)
                .addHeader("Content-Type", "text/zinc")
                .addHeader("Accept", "text/zinc")
                .post(body)
                .build();

        CcuLog.d("CCU_HTTP_REQUEST", "HttpUtil:postSync: [POST] " + url + " - Token: " + token);
        Call call = getSharedOkHttpClient().newCall(request);
        Response response = null;
        try {
            response = call.execute();
            CcuLog.d("CCU_HTTP_RESPONSE", "HttpUtil:postSync: " + response.code() + " - [" + response.request().method() + "] " + response.request().url());
        } catch (IOException e) {
            CcuLog.e("CCU_HTTP_RESPONSE", "HttpUtil:postSync: Failed : " + e);
        }

        return response;
    }

    public static String executePost(String targetURL, String urlParameters, String bearerToken) {
        targetURL = StringUtils.appendIfMissing(targetURL, "/");
        if (StringUtils.isNotBlank(bearerToken)) {
            URL url;
            HttpURLConnection connection = null;
            try {
                url = new URL(targetURL);

                connection = openConnection(urlParameters, url, targetURL, HttpConstants.HTTP_METHOD_POST);

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "text/zinc");
                connection.setRequestProperty("Accept", "text/zinc");

                final int chunkSize = 2048;
                for (int i = 0; i < urlParameters.length(); i += chunkSize) {
                    CcuLog.d("CCU_HS", urlParameters.substring(i, Math.min(urlParameters.length(), i + chunkSize)));
                }

                connection.setRequestProperty(HttpConstants.APP_NAME_HEADER_NAME, HttpConstants.APP_NAME_HEADER_VALUE);
                connection.setRequestProperty("Content-Length", "" + urlParameters.getBytes(StandardCharsets.UTF_8).length);
                connection.setRequestProperty("Content-Language", "en-US");
                connection.setRequestProperty("Authorization", " Bearer " + bearerToken);
                connection.setUseCaches(false);
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setConnectTimeout(HTTP_REQUEST_TIMEOUT_MS);
                connection.setReadTimeout(HTTP_REQUEST_TIMEOUT_MS);


                CcuLog.d("CCU_HTTP_REQUEST", "HttpUtil:executePost: [POST] " + url + " - Token: " + bearerToken);

                //Send request
                DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                wr.write(urlParameters.getBytes(StandardCharsets.UTF_8));
                wr.flush();

                int responseCode = connection.getResponseCode();

                CcuLog.d("CCU_HTTP_RESPONSE", "HttpUtil:executePost: " + responseCode + " - [POST] " + url);

                if (responseCode >= 400) {

                    BufferedReader rde = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    String linee;
                    StringBuilder responsee = new StringBuilder();
                    while ((linee = rde.readLine()) != null) {
                        responsee.append(linee);
                        responsee.append('\n');
                    }
                    CcuLog.e("CCU_HTTP_RESPONSE", "Response error stream: " + responsee);
                }

                //Get Response
                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\n');
                }
                connection.getInputStream().close();
                return responseCode == 200 ? response.toString() : null;

            } catch (Exception e) {
                CcuLog.e("CCU_HTTP_RESPONSE", "Exception reading stream: " + e.getLocalizedMessage());

                if (connection != null) {
                    connection.disconnect();
                }
                e.printStackTrace();
                return null;

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        return null;
    }

    /**
     * Returns EntitySyncResponse object instead of response string.
     *
     */
    public static EntitySyncResponse executeEntitySync(String targetURL, String urlParameters, String bearerToken) {
        targetURL = StringUtils.appendIfMissing(targetURL, "/");
        //TODO: Delete this logs after qa testing is done successfully
        final int chunkSize = 2048;
        for (int i = 0; i < urlParameters.length(); i += chunkSize) {
            CcuLog.d(TAG_CCU_HS, urlParameters.substring(i, Math.min(urlParameters.length(), i + chunkSize)));
        }
        Response response = postSync(targetURL, urlParameters, bearerToken);
        if (response == null) {
            return null;
        }

        EntitySyncResponse syncResponse = new EntitySyncResponse();
        syncResponse.setRespCode(response.code());
        try {
          if (response.code() >= HTTP_RESPONSE_ERR_REQUEST) {
                syncResponse.setErrRespString(response.body().string());
          } else {
                syncResponse.setRespString(response.body().string());
          }
        } catch (IOException e) {
            CcuLog.e(TAG_CCU_HS, "Exception reading stream: " + e);
            syncResponse.setErrRespString("");
            syncResponse.setRespString("");
        }
        return syncResponse;
    }

    // TODO Matt Rudd - This method is a hot mess; refactor
    public static String executeJson(String targetUrl, String urlParameters, String token, boolean tokenIsApiKey, String httpMethod) {
        URL url;
        HttpURLConnection connection = null;
        targetUrl = StringUtils.appendIfMissing(targetUrl, "/");
        if (StringUtils.isNotBlank(token)) {
            try {
                //Create connection
                url = new URL(targetUrl);

                connection = openConnection(urlParameters, url, targetUrl, httpMethod);

                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty(HttpConstants.APP_NAME_HEADER_NAME, HttpConstants.APP_NAME_HEADER_VALUE);

                CcuLog.i("CCU_HS", Objects.toString(url.toString(), ""));
                CcuLog.i("CCU_HS", Objects.toString(urlParameters, ""));

                connection.setRequestProperty("Content-Language", "en-US");

                if (tokenIsApiKey) {
                    connection.setRequestProperty("api-key", token);
                } else {
                    connection.setRequestProperty("Authorization", " Bearer " + token);
                }

                CcuLog.d("CCU_HTTP_REQUEST", "HttpUtil:executeJson: [" + httpMethod + "] " + url + " - Token: " + token);

                connection.setUseCaches(false);
                connection.setRequestMethod(httpMethod);

                connection.setConnectTimeout(HTTP_REQUEST_TIMEOUT_MS);
                connection.setReadTimeout(HTTP_REQUEST_TIMEOUT_MS);

                if (StringUtils.equals(httpMethod, HttpConstants.HTTP_METHOD_GET)) {
                    connection.setDoOutput(false);
                } else {
                    connection.setDoOutput(true);
                    connection.setRequestProperty(
                            "Content-Length", "" + urlParameters.getBytes(StandardCharsets.UTF_8).length
                    );
                    //Send request
                    DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                    wr.write(urlParameters.getBytes(StandardCharsets.UTF_8));
                    wr.flush();
                    wr.close();
                }

                int responseCode = connection.getResponseCode();
                CcuLog.i("CCU_HS", "HttpResponse: responseCode " + responseCode);

                CcuLog.d("CCU_HTTP_RESPONSE", "HttpUtil:executeJson: " + responseCode + " - [POST] " + url);

                if (responseCode >= 400) {

                    BufferedReader rde = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    String linee;
                    StringBuilder response = new StringBuilder();
                    while ((linee = rde.readLine()) != null) {
                        response.append(linee);
                        response.append('\n');
                    }
                    CcuLog.i("CCU_HS", "Response error stream: " + response);
                }

                //Get Response
                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\n');
                }
                return responseCode == 200 ? response.toString() : null;

            } catch (Exception e) {

                if (connection != null)
                    connection.disconnect();
                e.printStackTrace();
                return null;

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        return null;
    }

    private static synchronized OkHttpClient getSharedOkHttpClient() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(HTTP_REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .readTimeout(HTTP_REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .build();
        }
        return okHttpClient;
    }

    @NotNull
    private static Retrofit getRetrofitForHaystackBaseUrl(URL url, OkHttpClient httpClient) {
        return new Retrofit.Builder()
                .baseUrl(url)
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    private static HttpURLConnection openConnection(String urlParameters, URL url, String targetURL, String httpMethod) {
        RequestBody requestBody;
        if (urlParameters != null) {
            requestBody = RequestBody.create(MediaType.parse("text/zinc"), urlParameters);
        } else {
            requestBody = RequestBody.create(MediaType.parse("text/zinc"), "");
        }
        OkHttpClient httpClient = getSharedOkHttpClient();
        SiloApiService siloApiService = getRetrofitForHaystackBaseUrl(url, httpClient).create(SiloApiService.class);
        retrofit2.Call<ResponseBody> call = null;

        try {
            switch (httpMethod) {
                case HttpConstants.HTTP_METHOD_POST:
                    call = siloApiService.postData(
                            targetURL,
                            requestBody
                    );
                    break;
                case HttpConstants.HTTP_METHOD_PUT:
                    call = siloApiService.putData(
                            targetURL,
                            requestBody
                    );
                    break;
                case HttpConstants.HTTP_METHOD_GET:
                    call = siloApiService.getData(
                            targetURL
                    );
                    break;
            }
            return (HttpURLConnection) call.request().url().url().openConnection();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }
}