package a75f.io.renatus;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.constants.HttpConstants;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.cloud.RenatusServicesEnvironment;
import a75f.io.renatus.util.CCUUtils;

public class WeatherDataDownloadService {

	static int CALLING_TIME_INTERVAL = 15*60000;

    static boolean bStopService = true;
	
	static double mCurrentTemp = -100;
    static String micon = "";
    static double mCurrentHumidity = 0;
    static double mOutsideAirEnthalpy = 0;
    static String mSunriseTime = "06:30";
    static String mSunsetTime = "19:30";
    static double maxtemp = 0;
    static double mintemp = 0;
    static String mSummary = "";
    static double mPrecipIntensity = 0; // Millimeters per hour
    
    public static double cloudCover = 0;
    public static double windSpeed = 0;
    public static double windGust = 0;
    public static double windBearing = 0;

    private static void runPeriodicWeatherUpdate(){
        TimerTask timertask = new TimerTask() {
            @Override
            public void run() {
                getWeatherData();
            }
        };
        Timer timer = new Timer();
        timer.schedule(timertask, 0, CALLING_TIME_INTERVAL);
    }

    public static void getWeatherData() {
        if(bStopService) {
            bStopService = false;
            runPeriodicWeatherUpdate();
        }

        String weatherUrl = RenatusServicesEnvironment.getInstance().getUrls().getWeatherUrl();
        String bearerToken = CCUHsApi.getInstance().getJwt();

        HashMap<Object, Object> site = CCUHsApi.getInstance().readEntity("site");
        if (!site.containsKey("weatherRef")) {
            CcuLog.w(L.TAG_CCU_WEATHER, "Failed to get weather conditions - Site does not have a weatherRef");
            return;
        }
        String weatherRef = site.get("weatherRef").toString();


        SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext());
        double lat = spDefaultPrefs.getFloat("lat", 0);
        double lng = spDefaultPrefs.getFloat("lng", 0);
        CcuLog.i(L.TAG_CCU_WEATHER, "Lat :  "+lat+ ", Lng: "+lng);

        if ((lat < 0.05 && lat > -0.05) || (lng < 0.05 && lng > -0.05)) {
            CcuLog.w(L.TAG_CCU_WEATHER, "Failed to get weather conditions - Site not correctly Geo-Coded");
            return;
        }

        AsyncTask<Void, Integer, JSONObject> downloader = new AsyncTask<Void, Integer, JSONObject>() {
            @Override
            protected void onPostExecute(JSONObject response) {
                JSONObject current;
                try {
                    if (response != null) {
                        current = response.getJSONObject("currentWeather");

                        mCurrentTemp = current.getDouble("airTemp");

                        // TODO: Resolve Icons
                        micon = current.getString("icon");
                        mCurrentHumidity = CCUUtils.roundTo2Decimal(current.getDouble("humidity"));
                        mSummary = current.getString("description");

                        // TODO: Verify
                        // Convert mm/min to mm/hr
                        mPrecipIntensity = current.getDouble("precipitation") * 25.4 * 60;

                        cloudCover = current.getDouble("cloudage");
                        windSpeed = current.getDouble("windSpeed");
                        windGust = current.getDouble("windGust");
                        windBearing = current.getDouble("windBearing");
                        mOutsideAirEnthalpy = CCUUtils.calculateAirEnthalpy(mCurrentTemp, mCurrentHumidity);


                        maxtemp = current.getDouble("maxDailyTemp");
                        mintemp = current.getDouble("minDailyTemp");

                        mSunriseTime = CCUUtils.getTimeFromTimeStamp(current.getLong("sunrise"), "UTC");
                        mSunsetTime = CCUUtils.getTimeFromTimeStamp(current.getLong("sunset"), "UTC");

                        SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                        edit.putFloat("outside_cur_temp", (float) mCurrentTemp);
                        edit.putFloat("outside_hum", (float) mCurrentHumidity);
                        edit.putFloat("outside_precip",(float) mPrecipIntensity);
                        edit.apply();

                        CcuLog.i(L.TAG_CCU_WEATHER,
                                "sunrise today at=" + mSunriseTime + ",sunset today at=" + mSunsetTime +
                                "CCU_WEATHER"+"max temp is =" + maxtemp + ",min temp is=" + mintemp
                        );
                    } else {
                        mCurrentTemp = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getFloat("outside_cur_temp", (float) mCurrentTemp);
                        mCurrentHumidity = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getFloat("outside_hum", (float) mCurrentHumidity);
                        mOutsideAirEnthalpy = CCUUtils.calculateAirEnthalpy(mCurrentTemp, mCurrentHumidity);
                        mPrecipIntensity = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getFloat("outside_precip", (float) mPrecipIntensity);
                    }
                } catch (JSONException | NullPointerException e) {
                    CcuLog.i(L.TAG_CCU_WEATHER,"Exception: " + e.getMessage());
                }
            }

            @Override
            protected JSONObject doInBackground(Void... params) {
                JSONObject jsonResponse = null;
                String requestUrl = String.format(weatherUrl + "%s/current/@%s", weatherUrl, weatherRef);
                String response = HttpUtil.executeJson(
                        requestUrl,
                        null,
                        bearerToken,
                        false,
                        HttpConstants.HTTP_METHOD_GET
                );

                if (response != null) {
                    try {
                        jsonResponse = new JSONObject(response);
                    } catch (JSONException e) {
                        CcuLog.e(L.TAG_CCU_WEATHER, "Unable to parse JSON to retrieve weather data "+ e.getMessage());
                    }
                }

                return jsonResponse;
            }
        };
        downloader.execute();
    }
    
    
    public static String getSummary() {
        return mSummary;
    }

    public static String getIcon() {
        return micon;
    }

    public static double getTemperature() {
        return mCurrentTemp;
    }

    public static double getMaxTemperature() {
        return maxtemp;
    }

    public static double getMinTemperature() {
        return mintemp;
    }

    public static double getHumidity() {
        return mCurrentHumidity;
    }
    
    public static double getPrecipitation() {
        return mPrecipIntensity;
    }
}
