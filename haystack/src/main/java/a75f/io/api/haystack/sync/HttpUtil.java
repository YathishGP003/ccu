package a75f.io.api.haystack.sync;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

    private static final String TAG = HttpUtil.class.getSimpleName();

    public static final String HAYSTACK_URL = "https://renatusv2.azurewebsites.net/";

    public static final String CLIENT_ID = "d7682439-ac41-408b-bf72-b89a98490bdf";
    public static final String TENANT_ID = "941d8a61-4be2-4622-8ace-ed8ee5696d99";
    public static final String CLIENT_SECRET = "8tHwP3ykcKabD+J8tDrhex7HmVtrzF3zfXt56cF6h7c=";
    public static final String SCOPE = "234afeb7-497b-45a3-aa76-db2a549f17d4%2F.default";


    public static String clientToken = "";
    //JsonParser parser = new JsonParser();
    //JsonElement jsonTree = parser.parse(tokenJson);
    //JsonObject asJsonObject = jsonTree.getAsJsonObject();
    //String token = asJsonObject.get("access_token").getAsString();
    public static String executePost(String targetURL, String urlParameters)
    {
        if(clientToken.equalsIgnoreCase(""))
        {
            clientToken = parseToken(authorizeToken(CLIENT_ID, "", CLIENT_SECRET, TENANT_ID));
            System.out.println("Client Token: " + clientToken);
        }
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
            connection.setRequestProperty("Authorization", " Bearer " + clientToken);
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
            if(connection.getResponseCode() == 401)
            {
                clientToken = parseToken(authorizeToken(CLIENT_ID, "", CLIENT_SECRET, TENANT_ID));
                System.out.println("Client Token: " + clientToken);
                executePost(targetURL, urlParameters);
            }

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


    public static String parseToken(String jsonResponse)
    {
        JsonParser parser = new JsonParser();
        JsonElement jsonTree = parser.parse(jsonResponse);

        JsonObject asJsonObject = jsonTree.getAsJsonObject();

        String token = asJsonObject.get("access_token").getAsString();

        return token;
    }


    //curl -X POST -H "Content-Type: application/x-www-form-urlencoded"
    // -d 'client_id={client id}&scope=https%3A%2F%2Fgraph.microsoft.com%2F.default&
    // client_secret={client secret}=&
    // grant_type=client_credentials'
    // ‘https://login.microsoftonline.com/{tenantid}/oauth2/v2.0/token'

    //{"token_type":"Bearer",
    // "expires_in":3600,
    // "ext_expires_in":0,
    // "access_token":"eyJ0eXAiOiJKV1QiLCJub25jZSI6IkFRQUJBQUFBQUFDNXVuYTBFVUZnVElGOEVsYXh0V2pUaFJIZTZZSVA2eWYzZmtldTJIYndJRUl5TGR2SHBBb1FJWEg1R3JVS2dQZHhFNHFleXI0WXRwcjlrVVRnc2FrN2dUOG9YcFVsSlpDZS1nLTFqaEphOFNBQSIsImFsZyI6IlJTMjU2IiwieDV0Ijoid1VMbVlmc3FkUXVXdFZfLWh4VnRESkpaTTRRIiwia2lkIjoid1VMbVlmc3FkUXVXdFZfLWh4VnRESkpaTTRRIn0.eyJhdWQiOiJodHRwczovL2dyYXBoLm1pY3Jvc29mdC5jb20iLCJpc3MiOiJodHRwczovL3N0cy53aW5kb3dzLm5ldC85NDFkOGE2MS00YmUyLTQ2MjItOGFjZS1lZDhlZTU2OTZkOTkvIiwiaWF0IjoxNTQxNzgzODQ1LCJuYmYiOjE1NDE3ODM4NDUsImV4cCI6MTU0MTc4Nzc0NSwiYWlvIjoiNDJSZ1lKakhYODMrU25JeGY4NVQ0MVhoU2pHTkFBPT0iLCJhcHBfZGlzcGxheW5hbWUiOiJSZW5hdHVzQW5kcm9pZCIsImFwcGlkIjoiZDc2ODI0MzktYWM0MS00MDhiLWJmNzItYjg5YTk4NDkwYmRmIiwiYXBwaWRhY3IiOiIxIiwiaWRwIjoiaHR0cHM6Ly9zdHMud2luZG93cy5uZXQvOTQxZDhhNjEtNGJlMi00NjIyLThhY2UtZWQ4ZWU1Njk2ZDk5LyIsIm9pZCI6IjU3MDNiMjA2LTM1OTMtNDE5Ny05N2RiLTAyOWZlNjA4ODY3YSIsInN1YiI6IjU3MDNiMjA2LTM1OTMtNDE5Ny05N2RiLTAyOWZlNjA4ODY3YSIsInRpZCI6Ijk0MWQ4YTYxLTRiZTItNDYyMi04YWNlLWVkOGVlNTY5NmQ5OSIsInV0aSI6InpHR0VQSXJoN0UtemNWZ3p1eWtpQUEiLCJ2ZXIiOiIxLjAiLCJ4bXNfdGNkdCI6MTU0MTUyNjk0M30.P2UBFqovARX0K-XccwmA1SzBmAfUhTfxjNWDXjvh9XS6M8uiBFMSZ1CkZxW818mOcQPEQiY61GTMhbAN6KAL3dY3iTs0EJHkR42MgY_PTUSuVi80fyNReXrZnbG18Fl_Oy1TSSYdMIZotVafQNO3tYkNMFPPXJhe_bNpdFpYFukxoZleQ_Vg3Zg0XFS6CzNdAO1gLU01c2kEDvgkbqafBZgC55NH2KojwKcGXu0ZmqbzkP9RxGv2iZPaRCUZRM0WqIfD3zLqQRtT98MqJOd0Wkxa6j5OZIwMD9qb3mAMQBjKsXZ2LxthPwtnClW9iQ3JcSJV6Y2_5Dy8jqDiJhG1UA"}
    public static String authorizeToken(String client_id, String scope, String client_secret, String tenantId)
    {
        String contentType = "application/x-www-form-urlencoded";
        URL url;
        HttpsURLConnection connection = null;
        try {

            //Create connection
            url = new URL("https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token");
            //connection = (HttpsURLConnection)url.openConnection();
            connection = NetCipher.getHttpsURLConnection(url);//TODO - Hack for SSLException
            //connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    contentType);

            String urlParameters = "grant_type=client_credentials&client_id=d7682439-ac41-408b-bf72-b89a98490bdf&client_secret=8tHwP3ykcKabD%2BJ8tDrhex7HmVtrzF3zfXt56cF6h7c%3D&scope=234afeb7-497b-45a3-aa76-db2a549f17d4%2F.default";

                   // "client_id=" + client_id + "&scope=" + SCOPE +
                    //"&client_secret=" + client_secret + "&grant_type=client_credentials";

            System.out.println(url.toString());
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
