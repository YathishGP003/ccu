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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

import a75f.io.logger.CcuLog;
import info.guardianproject.netcipher.NetCipher;
import org.apache.commons.lang3.StringUtils;

public class HttpUtil
{

    public static String executePost(String targetURL, String urlParameters) {
        return executePost(targetURL, urlParameters, CCUHsApi.getInstance().getJwt()); // TODO Matt Rudd - I hate this hack, but the executePost needs a complete rewrite
    }

    public static synchronized String executePost(String targetURL, String urlParameters, String bearerToken)
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
                connection.setRequestProperty("Content-Type",
                        "text/zinc");

                CcuLog.i("CCU_HS",url.toString());
                final int chunkSize = 2048;
                for (int i = 0; i < urlParameters.length(); i += chunkSize) {
                    Log.d("CCU_HS", urlParameters.substring(i, Math.min(urlParameters.length(), i + chunkSize)));
                }

                //System.out.println(targetURL);
                //System.out.println(urlParameters);
                connection.setRequestProperty("Content-Length", "" +
                                                                Integer.toString(urlParameters.getBytes("UTF-8").length));
                connection.setRequestProperty("Content-Language", "en-US");
                connection.setRequestProperty("Authorization", " Bearer " + bearerToken);
                connection.setUseCaches (false);
                connection.setDoInput(true);
                connection.setDoOutput(true);

                //Send request
                DataOutputStream wr = new DataOutputStream (connection.getOutputStream());
                wr.write (urlParameters.getBytes("UTF-8"));
                wr.flush ();
                wr.close ();

                int responseCode = connection.getResponseCode();
                CcuLog.i("CCU_HS","HttpResponse: responseCode "+responseCode);

                //Get Response
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuffer response = new StringBuffer();
                while((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\n');
                }
                rd.close();
                is.close();

                return responseCode == 200 ? response.toString() : null;

            } catch (Exception e) {
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
    public static synchronized String executeJson(String targetUrl, String urlParameters, String token, boolean tokenIsApiKey, String httpMethod) {
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

                if (StringUtils.equals(httpMethod, HttpConstants.HTTP_METHOD_GET)) {
                    connection.setDoOutput(false);
                }
                else {
                    connection.setDoOutput(true);
                    connection.setRequestProperty(
                            "Content-Length", "" + Integer.toString(urlParameters.getBytes("UTF-8").length)
                    );
                    //Send request
                    DataOutputStream wr = new DataOutputStream (connection.getOutputStream());
                    wr.write (urlParameters.getBytes("UTF-8"));
                    wr.flush();
                    wr.close();
                }

                int responseCode = connection.getResponseCode();
                CcuLog.i("CCU_HS","HttpResponse: responseCode "+responseCode);

                //Get Response
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuffer response = new StringBuffer();
                while((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\n');
                }
                rd.close();
                is.close();

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
