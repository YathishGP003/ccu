package a75f.io.device.mesh;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.SystemProfile;
import a75f.io.logic.bo.building.system.vav.VavIERtu;

public class DaikinIE
{
    public static final String DAIKIN_IE_CLG_URL = "http://%s:8080/BN/MT3/AV/DATClgSetpoint/Present_Value?access-token=12345678";
    public static final String DAIKIN_IE_HTG_URL = "http://%s:8080/BN/MT3/AV/DATHtgSetpoint/Present_Value?access-token=12345678";
    public static final String DAIKIN_IE_SP_URL = "http://%s:8080/BN/MT3/AV/DSPSpt/Present_Value?access-token=12345678";
    public static final String DAIKIN_IE_MSG_BODY = "<requests>\n<request>\n%f\n</request>\n</requests>";
    
    public static void sendControl() {
        Log.d("DAIKIN_IE"," sendDaikinControl :");
        new Thread()
        {
            @Override
            public void run()
            {
                String eqIp = getIEUrl();
                if (eqIp.equals("")) {
                    Log.d("DAIKIN_IE","Invalid IE URL ");
                    return;
                }
                sendCoolingDATAutoControl(String.format(DAIKIN_IE_CLG_URL, eqIp));
                //sendCoolingDAT(String.format(DAIKIN_IE_CLG_URL, eqIp));
                //sendHeatingDAT(String.format(DAIKIN_IE_HTG_URL, eqIp));
                sendStaticPressure(String.format(DAIKIN_IE_SP_URL, eqIp));
            }
        }.start();
    }
    
    public static String getIEUrl() {
        return CCUHsApi.getInstance().readDefaultStrVal("point and system and config and ie and address");
    }
    
    private static void sendCoolingDATAutoControl(String url){
        SystemProfile p = L.ccu().systemProfile;
        double coolingDat = p.getSystemController().getSystemState() == SystemController.State.COOLING ?
                                    p.getCmd("cooling") : p.getCmd ("heating");
        if (coolingDat > 0)
        {
            send(url, String.format(DAIKIN_IE_MSG_BODY, fahrenheitToCelsius(coolingDat)));
        }
    }
    
    private void sendCoolingDAT(String url) {
    
        VavIERtu p = (VavIERtu) L.ccu().systemProfile;
        double coolingDat = p.getSystemController().getSystemState() == SystemController.State.COOLING ?
                                    p.getCmd("cooling") : p.getConfigVal("system and cooling and dat and max");
        if (coolingDat > 0)
        {
            send(url, String.format(DAIKIN_IE_MSG_BODY, fahrenheitToCelsius(coolingDat)));
        }
    }
    
    private void sendHeatingDAT(String url) {
        VavIERtu p = (VavIERtu) L.ccu().systemProfile;
        double heatingDat = p.getSystemController().getSystemState() == SystemController.State.HEATING ?
                                    p.getCmd("cooling") : p.getConfigVal("system and heating and dat and min");
        if (heatingDat > 0)
        {
            send(url, String.format(DAIKIN_IE_MSG_BODY, fahrenheitToCelsius(heatingDat)));
        }
    }
    
    
    private static void sendStaticPressure(String url) {
        send(url, String.format(DAIKIN_IE_MSG_BODY, inchToPascal(L.ccu().systemProfile.getCmd("staticPressure"))));
    }
    
    public static void send(final String urlString,final String data) {
        try
        {
            Log.d("CCU_DAIKIN", urlString + "\n data: \n" + data);
            URL url = new URL(urlString);
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            httpCon.setDoOutput(true);
            httpCon.setRequestMethod("PUT");
            httpCon.setRequestProperty("Content-Type", "text/plain");
            httpCon.setRequestProperty("Authorization", "Bearer=11021962");
        
            OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());
            out.write(data);
            out.close();
        
            StringBuilder sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader((httpCon.getInputStream())));
            String output;
            while ((output = br.readLine()) != null)
            {
                sb.append(output);
            }
            Log.d("DAIKIN_IE", " response \n" + sb.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static double fahrenheitToCelsius(double T) {
        return (T-32)*5/9;
    }
    
    public static double inchToPascal(double P) {
        return P/0.0040146;
    }
    
}
