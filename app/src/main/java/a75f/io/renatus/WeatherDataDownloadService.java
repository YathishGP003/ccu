package a75f.io.renatus;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import a75f.io.renatus.util.CCUUtils;

public class WeatherDataDownloadService extends IntentService {

	static int CALLING_TIME_INTERVAL = 15*60000;
	static String sUrlForecastIO = "https://api.darksky.net/forecast/64f63e0e60f15d3523a8702373e2e3eb/";
	static String sUrlOpenWeather = "http://api.openweathermap.org/data/2.5/weather?lat=44.0&lon=-93.96&units=imperial";
	private static final String SOLCAST_API_URL = "https://api.solcast.com.au/radiation/estimated_actuals?latitude=%f&longitude=%f&api_key=pEtltUsfJmkjVUFTwr9PuLmfh10KLkHO&format=json";
	
    static boolean bStopService = false;
	
	static void stop() {
		bStopService = true;
	}
	
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
    
    public static int dhi;
    public static int dni;
    
    
	public WeatherDataDownloadService() {
		super("WeatherService");
		// TODO Auto-generated constructor stub
	}

	
	@Override
	protected void onHandleIntent(Intent arg0) {
		while (!bStopService) {
	    	synchronized (this) {
	    		try {
	    			//if (CCUApp.DEBUG) Log.d("CCU_WEATHER", "getting weather");
	    			getWeatherData();
	    			wait(CALLING_TIME_INTERVAL);
	    		} catch (Exception e) {
	    			try {
	    				//if (CCUApp.DEBUG) Log.e("CCU_WEATHER", e.getMessage());
						wait(CALLING_TIME_INTERVAL);
					} catch (InterruptedException e1) {
						//if (CCUApp.DEBUG)
						    Log.e("CCU_WEATHER", e1.getMessage());
					}
	    			//if (CCUApp.DEBUG)
	    			    Log.e("CCU_WEATHER", e.getMessage());
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

    public static void getWeatherData() {
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
                    HttpClient httpclient = new DefaultHttpClient();
                    HttpGet httpget = new HttpGet(sUrlForecastIO + String.valueOf(finalLat) + "," + String.valueOf(finalLng)+"?exclude=minutely,hourly,alerts,flags");
                    ResponseHandler<String> responseHandler = new BasicResponseHandler();
                    String responseBody;
                    try {
                        responseBody = httpclient.execute(httpget, responseHandler);
                        JSONObject response = new JSONObject(responseBody);
                        return response;
                    } catch (ClientProtocolException e) {
                        if (BuildConfig.DEBUG) Log.e("CCU_WEATHER", "Exception: " + e.getMessage());
                    } catch (IOException e) {
                        if (BuildConfig.DEBUG) Log.e("CCU_WEATHER", "Exception: " + e.getMessage());
                    } catch (JSONException e) {
                        if (BuildConfig.DEBUG) Log.e("CCU_WEATHER", "Exception: " + e.getMessage());
                    } finally {
                        httpclient.getConnectionManager().closeExpiredConnections();
                    }
                    return null;
                }
            };
            downloader.execute();
            
            //new SolcastDataFetchTask().execute();
        }
    }
    
    private static class SolcastDataFetchTask extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                String responseBody = httpsGET(String.format(SOLCAST_API_URL, lat,lng));
    
                JSONArray forecastArray = new JSONObject(responseBody).getJSONArray("estimated_actuals");
                JSONObject latestForecast = forecastArray.getJSONObject(0);
                dhi = latestForecast.getInt("dhi");
                dni = latestForecast.getInt("dni");
    
                Log.d("CCU_WEATHER","dhi: "+dhi+" dni: "+dni);
            } catch (JSONException e) {
                if (BuildConfig.DEBUG) Log.e("CCU_WEATHER", "Exception: " + e.getMessage());
            }
            return null;
        }
        
        @Override
        protected void onPostExecute(JSONObject response) {
        
        }
        
        @Override
        protected void onPreExecute() {
        }
        
    }
    
    private static String httpsGET(String url){
        
        StringBuilder result = new StringBuilder();
        
        HttpsURLConnection urlConnection = null;
        
        try {
            URL restUrl = new URL(url);
            urlConnection = (HttpsURLConnection) restUrl.openConnection();
            urlConnection.setInstanceFollowRedirects(false);
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            
        }catch( Exception e) {
            e.printStackTrace();
        }
        finally {
            urlConnection.disconnect();
        }
        return result.toString();
    }
    
    
    public static String getSummary() {
        return mSummary;
    }

    public static String getIcon() {
        return micon;
    }

    public static String getDailySummary() {
        return dailysummary;
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

    public static String getTimeZone() {
        return timeZone;
    }

    public static double getHumidity() {
        return mCurrentHumidity;
    }
    
    public static double getPrecipitation() {
        return mPrecipIntensity;
    }

    public static double getOutsideAirEnthalpy() {
        return mOutsideAirEnthalpy;
    }

    public static String getSunriseTime() {
        if (mSunriseTime != null && !mSunriseTime.equalsIgnoreCase(""))
            return mSunriseTime;
        else
            return "06:30";
    }

    public static String getSunsetTime() {
        if (mSunsetTime != null && !mSunsetTime.equalsIgnoreCase(""))
            return mSunsetTime;
        else
            return "18:30";
    }
    
    boolean canGetLocation = false;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;
    Location location;

    public Location getLocation() {
        try {
            LocationManager locationManager = (LocationManager) RenatusApp.getAppContext()
                    .getSystemService(LOCATION_SERVICE);

            // getting GPS status
            boolean isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            boolean isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {
                this.canGetLocation = true;
                if (isNetworkEnabled) {
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
                    Log.d("location", "Network Enabled");
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            lat = location.getLatitude();
                            lng = location.getLongitude();
                        }
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, locationListener);
                        Log.d("location", "GPS Enabled");
                        if (locationManager != null) {
                            location = getLastKnownLocation();
                            Log.d("location", location+" ");
                            if (location != null) {
                                lat = location.getLatitude();
                                lng = location.getLongitude();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            Log.e("location",e.getMessage());
            e.printStackTrace();
        }

        return location;
    }


    private Location getLastKnownLocation() {
        LocationManager mLocationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            Location l = mLocationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

}
