package a75f.io.device.daikin;

import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.Occupancy;
import a75f.io.logic.bo.building.system.SystemController;
import a75f.io.logic.bo.building.system.vav.VavIERtu;
import a75f.io.logic.jobs.ScheduleProcessJob;
import a75f.io.logic.tuners.TunerUtil;

public class DaikinIE
{
    public static final String DAIKIN_IE_CLG_URL = "http://%s:8080/BN/MT3/AV/DATClgSetpoint/Present_Value?access-token=12345678";
    public static final String DAIKIN_IE_HTG_URL = "http://%s:8080/BN/MT3/AV/DATHtgSetpoint/Present_Value?access-token=12345678";
    public static final String DAIKIN_IE_SP_URL = "http://%s:8080/BN/MT3/AV/DSPSpt/Present_Value?access-token=12345678";
    public static final String DAIKIN_IE_HUMIDITY_SP_URL = "http://%s:8080/BN/MT3/AV/HumiditySpt/Present_Value?access-token=12345678";
    public static final String DAIKIN_IE_HUMIDITY_IN_URL = "http://%s:8080/BN/MT3/AV/SpaceRHNetIn/Present_Value?access-token=12345678";
    public static final String DAIKIN_IE_OAMIN_URL = "http://%s:8080/BN/MT3/AV/Net_OAMinPos/Present_Value?access-token=12345678";
    public static final String DAIKIN_IE_OCCMODE_URL = "http://%s:8080/BN/MT3/MV/OccMode/Present_Value?access-token=12345678";
    public static final String DAIKIN_IE_APPMODE_URL = "http://%s:8080/BN/MT3/MI/NetApplicMode/Present_Value?access-token=12345678";
    public static final String DAIKIN_IE_HUMIDITY_MODE_URL = "http://%s:8080/BN/MT3/MI/HumidityCtrl/Present_Value?access-token=12345678";
    public static final String DAIKIN_IE_MSG_BODY = "<requests>\n<request>\n%f\n</request>\n</requests>";
    
    enum OccMode {Occ, Unocc, tntOvrd, Auto, UnInit}
    enum NetApplicMode {Null, Off, HeatOnly, CoolOnly, FanOnly, Auto, Invalid, UnInit}
    enum HumidityCtrl {None,RelHum,DewPt,Always, UnInit}
    
    static OccMode occMode = OccMode.UnInit;
    static NetApplicMode appMode = NetApplicMode.UnInit;
    static HumidityCtrl humCtrl = HumidityCtrl.UnInit;
    
    public static void sendControl() {
        Log.d("DAIKIN_IE"," sendDaikinControl :");

        if(Globals.getInstance().isTestMode())
            return;
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
                
                VavIERtu systemProfile = (VavIERtu) L.ccu().systemProfile;
                
                if (systemProfile.getConfigEnabled("fan") > 0)
                {
                    sendStaticPressure(String.format(DAIKIN_IE_SP_URL, eqIp));
                }
                
                if (L.ccu().oaoProfile != null) {
                    double oaMin = CCUHsApi.getInstance().readHisValByQuery("point and his and outside and air and damper and cmd");
                    send(String.format(DAIKIN_IE_OAMIN_URL, eqIp), String.format(DAIKIN_IE_MSG_BODY, oaMin));
                }
                
                if (systemProfile.getConfigEnabled("humidification") > 0) {
                    if (humCtrl != HumidityCtrl.RelHum)
                    {
                        send(String.format(DAIKIN_IE_HUMIDITY_MODE_URL, getIEUrl()), String.format(DAIKIN_IE_MSG_BODY, (double)HumidityCtrl.RelHum.ordinal() ));
                        humCtrl = HumidityCtrl.RelHum;
                    }
                    send(String.format(DAIKIN_IE_HUMIDITY_IN_URL, getIEUrl()), String.format(DAIKIN_IE_MSG_BODY,
                                                                systemProfile.getSystemController().getAverageSystemHumidity()));
                    send(String.format(DAIKIN_IE_HUMIDITY_SP_URL, getIEUrl()), String.format(DAIKIN_IE_MSG_BODY,
                                                                TunerUtil.readSystemUserIntentVal("target and max and inside and humidity")));
                } else {
                    if (humCtrl != HumidityCtrl.None)
                    {
                        send(String.format(DAIKIN_IE_HUMIDITY_MODE_URL, getIEUrl()), String.format(DAIKIN_IE_MSG_BODY, (double)HumidityCtrl.None.ordinal() ));
                        humCtrl = HumidityCtrl.None;
                    }
                }
    
                if (ScheduleProcessJob.getSystemOccupancy() == Occupancy.OCCUPIED
                    || ScheduleProcessJob.getSystemOccupancy() == Occupancy.FORCEDOCCUPIED
                    || ScheduleProcessJob.getSystemOccupancy() == Occupancy.PRECONDITIONING
                    || ScheduleProcessJob.getSystemOccupancy() == Occupancy.OCCUPANCYSENSING
                    || systemProfile.getSystemController().getSystemState() != SystemController.State.OFF) {
                    if (occMode != OccMode.Occ)
                    {
                        send(String.format(DAIKIN_IE_OCCMODE_URL, eqIp), String.format(DAIKIN_IE_MSG_BODY, (double)OccMode.Occ.ordinal()));
                        occMode = OccMode.Occ;
                    }
                } else {
                    if (occMode != OccMode.Unocc)
                    {
                        send(String.format(DAIKIN_IE_OCCMODE_URL, eqIp), String.format(DAIKIN_IE_MSG_BODY, (double)OccMode.Unocc.ordinal()));
                        occMode = OccMode.Unocc;
                    }
                }
                
                if (systemProfile.getSystemController().getSystemState() != SystemController.State.OFF) {
                    if (appMode != NetApplicMode.Auto)
                    {
                        send(String.format(DAIKIN_IE_APPMODE_URL, eqIp), String.format(DAIKIN_IE_MSG_BODY, (double)NetApplicMode.Auto.ordinal()));
                        appMode = NetApplicMode.Auto;
                    }
                } else {
                    if (appMode != NetApplicMode.FanOnly)
                    {
                        send(String.format(DAIKIN_IE_APPMODE_URL, eqIp), String.format(DAIKIN_IE_MSG_BODY, (double)NetApplicMode.FanOnly.ordinal()));
                        appMode = NetApplicMode.FanOnly;
                    }
                }
                
                
            }
        }.start();
    }
    
    public static String getIEUrl() {
        return CCUHsApi.getInstance().readDefaultStrVal("point and system and config and ie and ipAddress");
    }
    
    private static void sendCoolingDATAutoControl(String url){
        CcuLog.d("DAIKIN_IE"," DATClgSetpoint (F) - "+L.ccu().systemProfile.getCmd("dat and setpoint"));
        send(url, String.format(DAIKIN_IE_MSG_BODY, fahrenheitToCelsius(L.ccu().systemProfile.getCmd("dat and setpoint"))));
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
    
    
    public static void sendStaticPressure(String url) {
        send(url, String.format(DAIKIN_IE_MSG_BODY, inchToPascal(L.ccu().systemProfile.getCmd("fan"))));
    }
    
    public static void sendCoolingDATAutoControl(final Double val){
        send(String.format(DAIKIN_IE_CLG_URL, getIEUrl()), String.format(DAIKIN_IE_MSG_BODY, fahrenheitToCelsius(val)));
    }
    
    
    public static void sendStaticPressure(final Double val) {
        send(String.format(DAIKIN_IE_SP_URL, getIEUrl()), String.format(DAIKIN_IE_MSG_BODY, inchToPascal(val)));
    }
    
    public static void sendHumidityInput(final Double val) {
        send(String.format(DAIKIN_IE_HUMIDITY_IN_URL, getIEUrl()), String.format(DAIKIN_IE_MSG_BODY, val));
    }
    
    public static void sendOAMinPos(final Double val) {
        send(String.format(DAIKIN_IE_OAMIN_URL, getIEUrl()), String.format(DAIKIN_IE_MSG_BODY, val));
    }
    
    public static void send(final String urlString,final String data) {
        new AsyncTask<String, Void, String>() {

            @Override
            protected String doInBackground(String... strings) {
                StringBuilder sb = new StringBuilder();
                try
                {
                    Log.d("DAIKIN_IE", urlString + "\n data: \n" + data);
                    URL url = new URL(urlString);
                    HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
                    httpCon.setDoOutput(true);
                    httpCon.setRequestMethod("PUT");
                    httpCon.setRequestProperty("Content-Type", "text/plain");
                    httpCon.setRequestProperty("Authorization", "Bearer=11021962");

                    OutputStreamWriter out = new OutputStreamWriter(httpCon.getOutputStream());
                    out.write(data);
                    out.close();

                    BufferedReader br = new BufferedReader(new InputStreamReader((httpCon.getInputStream())));
                    String output;
                    while ((output = br.readLine()) != null)
                    {
                        sb.append(output);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return sb.toString();
            }

            @Override
            protected void onPostExecute(String response) {
                Log.d("DAIKIN_IE", " response \n" + response);
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "");

    }
    
    public static void sendAsync(final String urlString,final String data) {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                send(urlString, data);
            }
        }).start();
       
    }
    
    public static double fahrenheitToCelsius(double T) {
        return (T-32)*5/9;
    }
    
    public static double inchToPascal(double P) {
        return P/0.0040146;
    }
    
}
