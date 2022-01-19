package a75f.io.renatus;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Timer;
import java.util.TimerTask;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.constants.HttpConstants;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.renatus.util.CCUUtils;
import a75f.io.renatus.util.LocationDetails;

public class WeatherDataDownloadService {

	static int CALLING_TIME_INTERVAL = 15*60000;
	static String sUrlForecastIO = "https://api.darksky.net/forecast/64f63e0e60f15d3523a8702373e2e3eb/";

    static boolean bStopService = true;
	
	static double mCurrentTemp = -100;
    static String micon = "";
    static double mCurrentHumidity = 0;
    static double mOutsideAirEnthalpy = 0;
    static String mSunriseTime = "06:30";
    static String mSunsetTime = "19:30";
    static String timeZone = "";
    static String dailysummary = "";
    static double maxtemp = 0;
    static double mintemp = 0;
    static String mSummary = "";
    static double mPrecipIntensity = 0; // Millimeters per hour
    
    public static double cloudCover = 0;
    public static double windSpeed = 0;
    public static double windGust = 0;
    public static double windBearing = 0;
    public static LocationDetails onDataReceived;

    private static void runPeriodicLocationUpdate(LocationDetails locationDetailsRef){

        onDataReceived = locationDetailsRef;
        LocationDetails finalOnDataReceived = onDataReceived;
        TimerTask timertask = new TimerTask() {
            @Override
            public void run() {
                getWeatherData(finalOnDataReceived);
            }
        };
        Timer timer = new Timer();
        timer.schedule(timertask, 0, CALLING_TIME_INTERVAL);
    }

    public static void getWeatherData(LocationDetails onDataReceived ) {
        if(bStopService) {
            bStopService = false;
            runPeriodicLocationUpdate(onDataReceived);
        }

        SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext());
        double lat = spDefaultPrefs.getFloat("lat", 0);
        double lng = spDefaultPrefs.getFloat("lng", 0);
        CcuLog.i(L.TAG_CCU_WEATHER, "Lat :  "+lat+ ", Lng: "+lng);

        if ((lat < 0.05 && lat > -0.05)
                || (lng < 0.05 && lng > -0.05)) {

            String zipCode = spDefaultPrefs.getString("zipcode", "");
            String country = spDefaultPrefs.getString("country", "");
            CCUUtils.getLocationInfo(country + " " +zipCode );


        } else {
            final double finalLat = lat;
            final double finalLng = lng;
            AsyncTask<Void, Integer, JSONObject> downloader = new AsyncTask<Void, Integer, JSONObject>() {
                @Override
                protected void onPostExecute(JSONObject response) {
                    JSONObject current;
                    try {
                        if (response != null) {
                            current = response.getJSONObject("currently");
                            CcuLog.i(L.TAG_CCU_WEATHER,
                                    finalLat + "/" + finalLng + current.toString(4));
                            mCurrentTemp = current.getDouble("temperature");
                            micon = current.getString("icon");
                            mCurrentHumidity = CCUUtils.roundTo2Decimal(current.getDouble("humidity"));
                            mSummary = current.getString("summary");
                            mPrecipIntensity = current.getDouble("precipIntensity");
                            mPrecipIntensity *= 100; // Normally in the range .000x, display 100 times to make more readable.
                            cloudCover = current.getDouble("cloudCover");
                            windSpeed = current.getDouble("windSpeed");
                            windGust = current.getDouble("windGust");
                            windBearing = current.getDouble("windBearing");
                            mOutsideAirEnthalpy = CCUUtils.calculateAirEnthalpy(mCurrentTemp, mCurrentHumidity);
                            JSONArray dailyArray = response.getJSONObject("daily").getJSONArray("data");
                            dailysummary = response.getJSONObject("daily").getString("summary");
                            JSONObject dailyclimate = new JSONObject(dailyArray.get(0).toString());
                            maxtemp = dailyclimate.getDouble("temperatureMax");
                            mintemp = dailyclimate.getDouble("temperatureMin");
                            mSunriseTime = CCUUtils.getTimeFromTimeStamp(dailyArray.getJSONObject(0).getLong("sunriseTime"), response.getString("timezone"));
                            mSunsetTime = CCUUtils.getTimeFromTimeStamp(dailyArray.getJSONObject(0).getLong("sunsetTime"), response.getString("timezone"));
                            timeZone = response.getString("timezone");
                            SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).edit();
                            edit.putFloat("outside_cur_temp", (float) mCurrentTemp);
                            edit.putFloat("outside_hum", (float) mCurrentHumidity);
                            edit.putFloat("outside_precip",(float) mPrecipIntensity);
                            edit.apply();
                            if(onDataReceived != null ) onDataReceived.onDataReceived();
                            CcuLog.i(L.TAG_CCU_WEATHER,
                                    "sunrise today at=" + mSunriseTime + ",sunset today at=" + mSunsetTime +
                                    "CCU_WEATHER"+"max temp is =" + maxtemp + ",min temp is=" + mintemp +
                                            ",msummary " + "is=" + dailysummary
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
                    String requestUrl = sUrlForecastIO + finalLat + "," + finalLng +"?exclude=minutely,hourly,alerts,flags";
                    String response = HttpUtil.executeJson(
                            requestUrl,
                            null,
                            "64f63e0e60f15d3523a8702373e2e3eb",
                            true,
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
