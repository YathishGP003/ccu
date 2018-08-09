package a75.io.logic;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import a75f.io.logic.InfluxDbUtil;

/**
 * Created by samjithsadasivan on 7/18/18.
 */

public class UnitTest
{
    @Test
    public void testNewInfluxLib() {
        
        HashMap<String, String> msgStr = new HashMap<>();
        
        msgStr.put("setTemp", String.valueOf(72.0));
        msgStr.put("roomTemp", String.valueOf(120.0));
        
        long time = (System.currentTimeMillis()) + 7200000;
        
        String url = new InfluxDbUtil.URLBuilder().setProtocol(InfluxDbUtil.HTTPS)
                                                  .setHost("influx-a75f.aivencloud.com")
                                                  .setPort(27304)
                                                  .setOp(InfluxDbUtil.WRITE)
                                                  .setDatabse("defaultdb")
                                                  .setUser("avnadmin")
                                                  .setPassword("mhur2n42y4l58xlx")
                                                  .buildUrl();
    
    
        SimpleDateFormat timeFormat = new SimpleDateFormat("MMdd_HHmm", Locale.US);
        String measurementTime = timeFormat.format(System.currentTimeMillis());
    
        System.out.println(measurementTime);
    
        InfluxDbUtil.writeData(url,"VAVTest", msgStr, time);
    
    }
    
    @Test
    public void testTime() {
    
        String url = "https://api.solcast.com.au/radiation/estimated_actuals?latitude=%f&longitude=%f&api_key=pEtltUsfJmkjVUFTwr9PuLmfh10KLkHO&format=json";
        
        double lat = 44.8951897;
        double longitude = -93.3105883;
        
        System.out.println(String.format(url,lat,longitude));
        
    
        SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
        int i = 0;
        String dateInString = "24-07-2018 "+i+":0:0";
        try
        {
            Date date = sdf.parse(dateInString);
            System.out.println(date.getTime());
        } catch(Exception e) {
            e.printStackTrace();
        }
        
        System.out.println(System.currentTimeMillis());
        
    }
    
    @Test
    public void solcastTest() {
    
        final String SOLCAST_API_URL = "https://api.solcast.com.au/radiation/estimated_actuals?latitude=%f&longitude=%f&api_key=pEtltUsfJmkjVUFTwr9PuLmfh10KLkHO&format=json";
    
        HttpClient httpclient = new DefaultHttpClient();
    
        double lat = 44.8951897;
        double lng = -93.3105883;
    
        HttpGet httpget = new HttpGet(String.format(SOLCAST_API_URL, lat,lng));
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String responseBody;
        try {
            responseBody = httpclient.execute(httpget, responseHandler);
            JSONObject response = new JSONObject(responseBody);
    
            JSONObject params = response.getJSONObject("estimated_actuals");
            int dhi = params.getInt("dhi");
            int dni = params.getInt("dni");
            
            System.out.println("SUCESSSSSSS dni:"+dni+" dhi:"+dhi);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            httpclient.getConnectionManager().closeExpiredConnections();
        }
    }
    
    
    @Test
    public void testCode() {
        double requestHours = 0;
    
        //This is the zone/system Request Hours divided by the zone/system run-hours (the hours in any Mode other than Unoccupied Mode)
        //since the last reset, expressed as a percentage
        double cumulativeRequestHoursPercent;
        
        double requestIntervalMins = 1;
    
        int currentRequests;
        currentRequests = 4;
    
        requestHours = requestHours + (currentRequests * requestIntervalMins/60);
        
        System.out.println(requestHours);
    }
    
    @Test
    public void daikinIETest(){
    
        String urlString = "http://10.1.10.21:8080/BN/MT3/AV/DATClgSetpoint/Present_Value?access-token=123456789";
        String DAIKIN_IE_MSG_BODY = "<requests>\n<request>\n%f\n</request>\n</requests>\n";
        String data = String.format(DAIKIN_IE_MSG_BODY, 23.0);
    
        System.out.println(urlString);
        System.out.println(data);
        try
        {
            URL url = new URL(urlString);
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setDoOutput(true);
            httpCon.setRequestMethod("PUT");
            httpCon.setRequestProperty("Content-Type", "text/plain");
            httpCon.setRequestProperty("Authorization", "Bearer=11021962");
            
            OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());
            out.write(data);
            out.close();
    
            //System.out.println(httpCon.getResponseCode());
            BufferedReader br = new BufferedReader(new InputStreamReader((httpCon.getInputStream())));
            StringBuilder sb = new StringBuilder();
            String output;
            while ((output = br.readLine()) != null)
            {
                sb.append(output);
            }
            System.out.println(sb);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    
        /*InputStream is = null;
    
        try {
        
            HttpClient httpClient = new DefaultHttpClient();
            HttpPut httpput = new HttpPut(urlString);
            httpput.setHeader("Authorization","Bearer=11021962");
            httpput.setHeader("Content-type", "application/json");
            // Create the string entity
            StringEntity input = new StringEntity("{\"status\":\"accepted\"}");
            // set the content type to json
            input.setContentType("text/plain");
            // set the entity property of the httpput
            // request to the created input.
            httpput.setEntity(input);
            HttpResponse httpResponse = httpClient.execute(httpput);
            HttpEntity httpEntity = httpResponse.getEntity();
            is = httpEntity.getContent();
        
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                                                                                    is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "n");
            }
            //is.close();
        
            String json = sb.toString();
            Log.e("JSONStr", json);
        } catch (Exception e) {
            e.getMessage();
            Log.e("Buffer Error", "Error converting result " + e.toString());
        }*/
        
    
    
}}
