package a75f.io.api.haystack.sync;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.constants.HttpConstants;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

import a75f.io.logger.CcuLog;
import info.guardianproject.netcipher.NetCipher;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.apache.commons.lang3.StringUtils;

public class HttpUtil
{
    private static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;

    public static String executePost(String targetURL, String urlParameters) {
        return executePost(targetURL, urlParameters, CCUHsApi.getInstance().getJwt()); // TODO Matt Rudd - I hate this hack, but the executePost needs a complete rewrite
    }


    public static final MediaType ZINC = MediaType.get("text/zinc; charset=utf-8");
    private static OkHttpClient client = new OkHttpClient();

    private static Call post(String url, String params, String token, Callback callback) {
        RequestBody body = RequestBody.create(params, ZINC);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Length", "" + params.getBytes(StandardCharsets.UTF_8).length)
                .addHeader("Content-Language", "en-US")
                .addHeader("Authorization", " Bearer " + token)
                .post(body)
                .build();
        Call call = client.newCall(request);
        call.enqueue(callback);
        return call;
    }

    public static void executePostAsync(String targetURL, String urlParameters) {

        CcuLog.d("CCU_HS","Posting to " + targetURL + ": " + urlParameters);

        post(targetURL, urlParameters, CCUHsApi.getInstance().getJwt(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                CcuLog.i("CCU_HS","executePostAsync Failed : "+e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseStr = response.body().string();
                    CcuLog.i("CCU_HS","executePostAsync Succeeded : "+responseStr);
                } else {
                    CcuLog.i("CCU_HS","executePostAsync Failed : " + response.message());
                }
            }
        });
    }


    public static String executePost(String targetURL, String urlParameters, String bearerToken)
    {
        CcuLog.i("CCU_HS","Client Token: " + bearerToken);

        if (StringUtils.isNotBlank(bearerToken)) {
            URL url;
            HttpURLConnection connection = null;
            try {
                url = new URL(targetURL);

                if (StringUtils.equals(url.getProtocol(), HttpConstants.HTTP_PROTOCOL)) {
                    connection = (HttpURLConnection)url.openConnection();
                } else {
                    connection = NetCipher.getHttpsURLConnection(url);
                }

                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "text/zinc");
                connection.setRequestProperty("Accept", "text/zinc");
                
                CcuLog.i("CCU_HS",url.toString());
                final int chunkSize = 2048;
                for (int i = 0; i < urlParameters.length(); i += chunkSize) {
                    Log.d("CCU_HS", urlParameters.substring(i, Math.min(urlParameters.length(), i + chunkSize)));
                }

                connection.setRequestProperty("Content-Length", "" + urlParameters.getBytes(StandardCharsets.UTF_8).length);
                connection.setRequestProperty("Content-Language", "en-US");
                connection.setRequestProperty("Authorization", " Bearer " + bearerToken);
                connection.setUseCaches (false);
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setConnectTimeout(HTTP_REQUEST_TIMEOUT_MS);
                connection.setReadTimeout(HTTP_REQUEST_TIMEOUT_MS);

                //Send request
                DataOutputStream wr = new DataOutputStream (connection.getOutputStream());
                wr.write (urlParameters.getBytes(StandardCharsets.UTF_8));
                wr.flush ();

                int responseCode = connection.getResponseCode();
                CcuLog.i("CCU_HS","HttpResponse: responseCode "+responseCode);

                if (responseCode >= 400) {

                    BufferedReader rde = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    String linee;
                    StringBuffer responsee = new StringBuffer();
                    while((linee = rde.readLine()) != null) {
                        responsee.append(linee);
                        responsee.append('\n');
                    }
                    CcuLog.i("CCU_HS","Response error stream: " + responsee.toString());
                }

                //Get Response
                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuffer response = new StringBuffer();
                while((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\n');
                }
                connection.getInputStream().close();
                return responseCode == 200 ? response.toString() : null;

            } catch (Exception e) {
                CcuLog.i("CCU_HS","Exception reading stream: " + e.getLocalizedMessage());

                if(connection != null) {
                    connection.disconnect();
                }
                e.printStackTrace();
                return null;

            } finally {

                if(connection != null) {
                    connection.disconnect();
                }
            }
        }
        return null;
    }

    // TODO Matt Rudd - This method is a hot mess; refactor
    public static String executeJson(String targetUrl, String urlParameters, String token, boolean tokenIsApiKey, String httpMethod) {
        URL url;
        HttpURLConnection connection = null;
        if (StringUtils.isNotBlank(token)) {
            try {
                //Create connection
                url = new URL(targetUrl);

                if (StringUtils.equals(url.getProtocol(), HttpConstants.HTTP_PROTOCOL)) {
                    connection = (HttpURLConnection)url.openConnection();
                } else {
                    connection = NetCipher.getHttpsURLConnection(url);
                }

                connection.setRequestProperty("Content-Type",
                        "application/json");

                CcuLog.i("CCU_HS", Objects.toString(url.toString(),""));
                CcuLog.i("CCU_HS", Objects.toString(urlParameters, ""));

                connection.setRequestProperty("Content-Language", "en-US");

                if (tokenIsApiKey) {
                    connection.setRequestProperty("api-key", token);
                } else {
                    connection.setRequestProperty("Authorization", " Bearer " + token);
                }

                connection.setUseCaches (false);
                connection.setRequestMethod(httpMethod);
                
                connection.setConnectTimeout(HTTP_REQUEST_TIMEOUT_MS);
                connection.setReadTimeout(HTTP_REQUEST_TIMEOUT_MS);

                if (StringUtils.equals(httpMethod, HttpConstants.HTTP_METHOD_GET)) {
                    connection.setDoOutput(false);
                }
                else {
                    connection.setDoOutput(true);
                    connection.setRequestProperty(
                            "Content-Length", "" + urlParameters.getBytes(StandardCharsets.UTF_8).length
                    );
                    //Send request
                    DataOutputStream wr = new DataOutputStream (connection.getOutputStream());
                    wr.write (urlParameters.getBytes(StandardCharsets.UTF_8));
                    wr.flush();
                }

                int responseCode = connection.getResponseCode();
                CcuLog.i("CCU_HS","HttpResponse: responseCode "+responseCode);

                if (responseCode >= 400) {

                    BufferedReader rde = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    String linee;
                    StringBuffer responsee = new StringBuffer();
                    while((linee = rde.readLine()) != null) {
                        responsee.append(linee);
                        responsee.append('\n');
                    }
                    CcuLog.i("CCU_HS","Response error stream: " + responsee.toString());
                }

                //Get Response
                BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuffer response = new StringBuffer();
                while((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\n');
                }

                return responseCode == 200 ? response.toString() : null;

            } catch (Exception e) {

                if(connection != null)
                    connection.disconnect();
                e.printStackTrace();
                return null;

            } finally {
                if(connection != null) {
                    connection.disconnect();
                }
            }
        }
        return null;
    }
}