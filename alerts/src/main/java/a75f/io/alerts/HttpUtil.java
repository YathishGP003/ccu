package a75f.io.alerts;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import a75f.io.alerts.BuildConfig;
import a75f.io.logger.CcuLog;
import info.guardianproject.netcipher.NetCipher;
import org.apache.commons.lang3.StringUtils;

public class HttpUtil
{

    public static final String HTTP_SCHEME = "http";

    public static String sendRequest(String endpoint, String postData) {
        URL url;
        HttpURLConnection connection = null;
        try {
            //Create connection
            url = new URL(BuildConfig.ALERTS_API_BASE +  endpoint);

            if (StringUtils.equals(url.getProtocol(), HTTP_SCHEME)) {
                connection = (HttpURLConnection)url.openConnection();
            } else {
                connection = NetCipher.getHttpsURLConnection(url);
            }

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/json");
            
            connection.setUseCaches (false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            
            CcuLog.d("CCU_ALERTS",url.toString()+" "+postData);
            //Send request
            DataOutputStream wr = new DataOutputStream (connection.getOutputStream());
            wr.writeBytes (postData);
            wr.flush ();
            wr.close ();
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                CcuLog.i("CCU_ALERTS","HttpError: responseCode "+responseCode);
            }
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
            
            return responseCode == 200 ? response.toString() : null;
            
        } catch (Exception e) {
            
            e.printStackTrace();
            return null;
            
        } finally {
            
            if(connection != null) {
                connection.disconnect();
            }
        }
    }
}
