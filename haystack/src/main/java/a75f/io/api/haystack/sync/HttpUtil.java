package a75f.io.api.haystack.sync;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import info.guardianproject.netcipher.NetCipher;

/**
 * Created by samjithsadasivan on 10/17/18.
 */

public class HttpUtil
{
    
    public static final String HAYSTACK_URL = "https://renatusv2.azurewebsites.net/";
    
    public static String executePost(String targetURL, String urlParameters)
    {
        URL url;
        HttpsURLConnection connection = null;
        try {
            //Create connection
            url = new URL(targetURL);
            //connection = (HttpsURLConnection)url.openConnection();
            connection = NetCipher.getHttpsURLConnection(url);//TODO - Hack for SSLException
            //connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "text/zinc");
            
            System.out.println(targetURL);
            System.out.println(urlParameters);
            connection.setRequestProperty("Content-Length", "" +
                                                            Integer.toString(urlParameters.getBytes("UTF-8").length));
            connection.setRequestProperty("Content-Language", "en-US");
            
            connection.setUseCaches (false);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            
            //Send request
            DataOutputStream wr = new DataOutputStream (connection.getOutputStream());
            wr.write (urlParameters.getBytes("UTF-8"));
            wr.flush ();
            wr.close ();
            
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
            return connection.getResponseCode() == 200 ? response.toString() : null;
            
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
