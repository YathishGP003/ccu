package a75f.io.renatus;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.constants.HttpConstants;
import a75f.io.logger.CcuLog;
import a75f.io.renatus.util.CCUUtils;
import a75f.io.renatus.util.LocationDetails;

public class WeatherDataDownloadService extends IntentService {

	static int CALLING_TIME_INTERVAL = 15*60000;
	static String sUrlForecastIO = "https://api.darksky.net/forecast/64f63e0e60f15d3523a8702373e2e3eb/";

    static boolean bStopService = false;
	
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

	public WeatherDataDownloadService() {
		super("WeatherService");
		// TODO Auto-generated constructor stub
	}

	
	@Override
	protected void onHandleIntent(Intent arg0) {
		while (!bStopService) {
	    	synchronized (this) {
	    		try {
	    			getWeatherData(null);
	    			wait(CALLING_TIME_INTERVAL);
	    		} catch (Exception e) {
	    			try {
						wait(CALLING_TIME_INTERVAL);
					} catch (InterruptedException e1) {
                        Log.e("CCU_WEATHER", e1.getMessage());
					}
                        Log.e("CCU  _WEATHER", e.getMessage());
	    		}
	    	}
	    }

    }


    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            lat = location.getLongitude();
            lng = location.getLatitude();
            Log.d("location4",lat+" "+lng);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {
            lat = location.getLongitude();
            lng = location.getLatitude();
            Log.d("location5",lat+" "+lng);
        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    private static double lat = 0;
    private static double lng = 0;

    public static void getWeatherData(LocationDetails onDataReceived ) {
        SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext());
        lat = spDefaultPrefs.getFloat("lat", 0);
        lng = spDefaultPrefs.getFloat("lng", 0);
        Log.d("locationy",lat+" "+lng);
        Log.d("location1", lat + " " + lng);
        if ((lat < 0.05 && lat > -0.05)
                || (lng < 0.05 && lng > -0.05)) {

            String zipCode = spDefaultPrefs.getString("zipcode", "");
            String country = spDefaultPrefs.getString("country", "");


            //CCUUtils.getLocationInfo(zipCode + ", " + country);
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
                            if (BuildConfig.DEBUG)
                                Log.d("CCU_WEATHER", String.valueOf(finalLat) + "/" + String.valueOf(finalLng) + current.toString(4));
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
                            edit.commit();
                            if(onDataReceived != null ) onDataReceived.onDataReceived();
                            Log.d("CCU_WEATHER", "sunrise today at=" + mSunriseTime + ",sunset today at=" + mSunsetTime);
                            Log.d("CCU_WEATHER", "max temp is =" + maxtemp + ",min temp is=" + mintemp + ",msummary is=" + dailysummary);
                        } else {
                            mCurrentTemp = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getFloat("outside_cur_temp", (float) mCurrentTemp);
                            mCurrentHumidity = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getFloat("outside_hum", (float) mCurrentHumidity);
                            mOutsideAirEnthalpy = CCUUtils.calculateAirEnthalpy(mCurrentTemp, mCurrentHumidity);
                            mPrecipIntensity = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext()).getFloat("outside_precip", (float) mPrecipIntensity);
                        }
                    } catch (JSONException e) {
                        if (BuildConfig.DEBUG) Log.e("CCU_WEATHER", "Exception: " + e.getMessage());
                    } catch (NullPointerException e) {
                        if (BuildConfig.DEBUG) Log.e("CCU_WEATHER", "Exception: " + e.getMessage());
                    }
                    
                }

                @Override
                protected JSONObject doInBackground(Void... params) {
                    JSONObject jsonResponse = null;
                    String requestUrl = sUrlForecastIO + String.valueOf(finalLat) + "," + String.valueOf(finalLng)+"?exclude=minutely,hourly,alerts,flags";
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
                            CcuLog.e("CCU_WEATHER", "Unable to parse JSON to retrieve weather data");
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
    
    Location location;
}
