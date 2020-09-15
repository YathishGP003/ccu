package a75f.io.api.haystack.sync;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import a75f.io.constants.HttpConstants;
import a75f.io.logger.CcuLog;
import info.guardianproject.netcipher.NetCipher;

/**
 * Created by mahesh on 09-09-2020.
 */
public class AsyncHttpPost extends AsyncTask<String, Void, String> {

    private static final String TAG = HttpUtil.class.getSimpleName();

    public interface Listener {
        void onResponse(String result);
    }

    private Listener mListener;
    private String urlParameters, targetURL, bearerToken;

    /**
     * constructor
     */
    public AsyncHttpPost(String url, String data, String token) {
        this.urlParameters = data;
        this.targetURL = url;
        this.bearerToken = token;
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    @Override
    protected String doInBackground(String... strings) {
        {
            CcuLog.i("CCU_HS", "Client Token: " + bearerToken);

            if (StringUtils.isNotBlank(bearerToken)) {
                URL url;
                HttpURLConnection connection = null;
                try {
                    url = new URL(targetURL);

                    if (StringUtils.equals(url.getProtocol(), HttpConstants.HTTP_PROTOCOL)) {
                        connection = (HttpURLConnection) url.openConnection();
                    } else {
                        connection = NetCipher.getHttpsURLConnection(url);
                    }

                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type",
                            "text/zinc");

                    CcuLog.i("CCU_HS", url.toString());
                    final int chunkSize = 2048;
                    for (int i = 0; i < urlParameters.length(); i += chunkSize) {
                        Log.d("CCU_HS", urlParameters.substring(i, Math.min(urlParameters.length(), i + chunkSize)));
                    }

                    connection.setRequestProperty("Content-Length", "" +
                            urlParameters.getBytes(StandardCharsets.UTF_8).length);
                    connection.setRequestProperty("Content-Language", "en-US");
                    connection.setRequestProperty("Authorization", " Bearer " + bearerToken);
                    connection.setUseCaches(false);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);

                    //Send request
                    DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
                    wr.write(urlParameters.getBytes(StandardCharsets.UTF_8));
                    wr.flush();
                    wr.close();

                    int responseCode = connection.getResponseCode();
                    CcuLog.i("CCU_HS", "HttpResponse: responseCode " + responseCode);

                    //Get Response
                    InputStream is = connection.getInputStream();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                    String line;
                    StringBuffer response = new StringBuffer();
                    while ((line = rd.readLine()) != null) {
                        response.append(line);
                        response.append('\n');
                    }
                    rd.close();
                    is.close();

                    return responseCode == 200 ? response.toString() : null;

                } catch (Exception e) {
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
    }

    /**
     * on getting response
     */
    @Override
    protected void onPostExecute(String result) {
        if (mListener != null) {
            mListener.onResponse(result);
        }
    }
}