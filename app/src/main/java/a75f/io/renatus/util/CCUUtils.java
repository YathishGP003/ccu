package a75f.io.renatus.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.location.Address;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.base.Strings;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import a75f.io.renatus.BuildConfig;
import a75f.io.renatus.RenatusApp;
import a75f.io.renatus.WeatherDataDownloadService;
import a75f.io.renatus.util.HttpsUtils.HTTPUtils;

public class CCUUtils {
	private static List<Address> locAddress= new ArrayList<Address>();
	public enum SMARTNODE_SENSOR_TYPE {
		SN_SENSOR_NONE("NONE"),
		SN_SENSOR_HUMIDITY("humidity"),
		SN_SENSOR_CO2("co2"),
		SN_SENSOR_CO("co"),
		SN_SENSOR_NO("no2"),
		SN_SENSOR_VOC("voc"),
		SN_SENSOR_PRESSURE("pressure"),
		SN_SENSOR_OCCUPANCY("occupancy"),
		SN_ENERGY_METER_HIGH("emr_high"),
		SN_ENERGY_METER_LOW("emr_low"),
		SN_SENSOR_SOUND("sound"),
		SN_SENSOR_CO2_EQUIVALENT("co2_equivalent"),
		SN_SENSOR_ILLUMINANCE ("illuminance"),
		SN_SENSOR_UVI("uvi");
		String name;

		SMARTNODE_SENSOR_TYPE(String val) {
			name = val;
		}

		public String toString() {
			return name;
		}
	}

	/*
	  FIRMWARE_DEVICE_WIRELESS_RTH = 0,

  		FIRMWARE_DEVICE_SMART_NODE = 1,

  		FIRMWARE_DEVICE_CONTROL_MOTE = 2,

  		FIRMWARE_DEVICE_SMART_STAT = 3,

  		FIRMWARE_DEVICE_SMART_STAT_REMOTE = 4,

  		FIRMWARE_DEVICE_SMART_LITE = 5,

  		FIRMWARE_DEVICE_SMART_STAT_BACK = 6

  		WRM_DEVICE("WRM"),
  		SMARTNODE("SmartNode"),
  		SMART_STAT_REMOTE("SmartStat remote"),
  		ITM_SMARTSTAT("SmartStat"),
  		SMART_LITE("SmartLite"),
  		SMART_STAT_BACK("SmartStatBack");
  		//
	 */
	public enum WRM_DEVICE_ERROR_TYPE
	{
		RTH_DEVICE("RTH"),
		SMARTNODE("SmartNode"),
		CONTROLMOTE("ControlMote"),
		SMARTSTAT("SmartStat"),
		SMART_STAT_REMOTE("SmartStat remote"),
		ITM_SMARTSTAT("SmartLite"),
		SMART_STAT_BACK("SmartStatBack"),
		BATT_OPERATED_TEMP_MONITOR("Battery Temperature Monitor");
		String name;
		WRM_DEVICE_ERROR_TYPE(String val) {
			name = val;
		}

		public String toString() {
			return name;
		}

	}


	public enum WRM_DEVICE_TYPE {
		WRM_DEVICE("WRM"),
		SMARTNODE("SmartNode"),
		SMART_STAT_REMOTE("SmartStat remote"),
		ITM_SMARTSTAT("SmartStat"),
		SMART_LITE("SmartLite"),
		SMART_STAT_BACK("SmartStatBack"),
		BATT_OPERATED_TEMP_MONITOR("Battery Temperature Monitor");
		String name;

		WRM_DEVICE_TYPE(String val) {
			name = val;
		}

		public String toString() {
			return name;
		}
	}

	//Standalone conditioning and operational modes
	public enum SMARTSTAT_OP_FANSPEED{FANSPEED_OFF,FANSPEED_AUTO,FANSPEED_LOW, FANSPEED_HIGH, FANSPEED_HIGH2};
	public enum STANDALONE_CONDITION_FANSPEED{FANSPEED_LOW, FANSPEED_HIGH}; //For Fan Auto modes
	public enum SMARTSTAT_OP_CONDITIONING_MODE{STANDALONE_OFF, STANDALONE_AUTO, STANDALONE_HEATING,STANDALONE_COOLING}
	public enum STANDALONE_CONDITIONING_MODE{COOLING, HEATING}; //For operational Auto modes
    public static String valToTime(int value) {
    	StringBuilder mBuilder = new StringBuilder();
        java.util.Formatter mFmt = new java.util.Formatter(mBuilder, java.util.Locale.US);
        Object[] mArgs = new Object[2];
        mArgs[0] = value/4;
        mArgs[1] = (value%4)*(15);
        mBuilder.delete(0, mBuilder.length());
        mFmt.format("%02d:%02d", mArgs);
        return mFmt.toString();
    }

    public static String valToTime12Hr(int value) {
    	StringBuilder mBuilder = new StringBuilder();
        java.util.Formatter mFmt = new java.util.Formatter(mBuilder, java.util.Locale.US);
        Object[] mArgs = new Object[2];
        if (value/4 > 12)
        	mArgs[0] = value/4 - 12;
        else
        	mArgs[0] = value/4;
        mArgs[1] = (value%4)*(15);
        mBuilder.delete(0, mBuilder.length());
        if (value/4 > 11)
        	mFmt.format("%02d:%02d PM", mArgs);
        else {
			if(value < 4)
				mFmt.format("12:%02d AM",mArgs[1]);
			else
				mFmt.format("%02d:%02d AM", mArgs);
		}
        return mFmt.toString();
    }

	public static boolean isxlargedevice(Context c){
		if ((c.getResources().getConfiguration().screenLayout &
				Configuration.SCREENLAYOUT_SIZE_MASK) ==
				Configuration.SCREENLAYOUT_SIZE_XLARGE) {
			// on a large screen device ...
			return true;

		}else{
			return false;
		}
	}

    public static Calendar valToTimeCal(int value) {
		int hour = value/4;
		int min = (value%4)*(15);
		Calendar now = Calendar.getInstance();
		now.set(Calendar.HOUR_OF_DAY, hour);
		now.set(Calendar.MINUTE, min);
		return now;
    }

	public static String strToDateMMMDD(Date val){
		SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.US);
		return sdf.format(val);
	}

	public static String forcedModeCal(){
		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aaa");
		Date a=new Date();
		//a.setTime(System.currentTimeMillis()+(AlgoTuningParameters.getHandle().getForcedOccupiedTimePeriod()*60 * 1000));
		return sdf.format(a);
	}
	public static boolean isForcedTimeEnd(String forceTime){
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aaa");
			Date parse = sdf.parse(forceTime);
			Calendar fot = sdf.getCalendar();
			fot.setTime(parse);
			Calendar c = Calendar.getInstance();
			fot.set(Calendar.DATE,c.get(Calendar.DATE));
			fot.set(Calendar.MONTH,c.get(Calendar.MONTH));
			fot.set(Calendar.YEAR, c.get(Calendar.YEAR));
			Date ft = new Date(fot.getTimeInMillis());
			Date et = new Date();
			et.setTime(System.currentTimeMillis());
			return et.after(ft);
		}catch (ParseException pe){

		}
		return false;
	}
    public static Date strToDateMMDDYYYY(String val) throws ParseException {
    	String myFormat = "MM-dd-yyyy"; //In which you need put here
		SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.US);
		return sdf.parse(val);
    }

    public static String valToTime1x60(int value) {
    	StringBuilder mBuilder = new StringBuilder();
        java.util.Formatter mFmt = new java.util.Formatter(mBuilder, java.util.Locale.US);
        Object[] mArgs = new Object[2];
        mArgs[0] = value/60;
        mArgs[1] = (value%60)*(60/60);
        mBuilder.delete(0, mBuilder.length());
        mFmt.format("%02d:%02d", mArgs);
        return mFmt.toString();
    }

    public static int timeToVal(String str) {
    	int hr = Integer.parseInt(str.substring(0, 2));
    	int min = Integer.parseInt(str.substring(3,5));
    	if (str.contains("PM") && hr < 12)
    		return (48+(hr*4))+(min/15);
		else if(str.contains("AM") && hr == 12){
			return 0+(min / 15);
		} else {
			return (hr * 4) + (min / 15);
		}
    }

    public static int dp2px(Context context, int dp) {
    	final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    public static int getCurrentTimeSlot() {
    	Calendar calendar = GregorianCalendar.getInstance();
    	return (calendar.get(Calendar.HOUR_OF_DAY)*4) + (calendar.get(Calendar.MINUTE)/15);
    }

	public static int getCurrentDayOfWeekWithMondayAsStart() {
		Calendar calendar = GregorianCalendar.getInstance();
		switch (calendar.get(Calendar.DAY_OF_WEEK))
		{
			case Calendar.MONDAY: return 0;
			case Calendar.TUESDAY: return 1;
			case Calendar.WEDNESDAY: return 2;
			case Calendar.THURSDAY: return 3;
			case Calendar.FRIDAY: return 4;
			case Calendar.SATURDAY: return 5;
			case Calendar.SUNDAY: return 6;
		}
		return 0;
	}

	public static String getNameOfDayWithMondayAsStart(int nDay) {
		switch (nDay) {
		case 0: return "monday";
		case 1: return "tuesday";
		case 2: return "wednesday";
		case 3: return "thursday";
		case 4: return "friday";
		case 5: return "saturday";
		case 6: return "sunday";
		}
		return "";
	}

	public static String getNameOfDayWithMondayAsStartShortForm(int nDay) {
		switch (nDay) {
		case 0: return "mon";
		case 1: return "tue";
		case 2: return "wed";
		case 3: return "thu";
		case 4: return "fri";
		case 5: return "sat";
		case 6: return "sun";
		}
		return "";
	}

	public static ArrayList<String> getCountriesList() {
        Locale[] locales = Locale.getAvailableLocales();
        ArrayList<String> countries = new ArrayList<String>();
        for (Locale locale : locales) {
            String country = locale.getDisplayCountry();
            if (country.trim().length()>0 && !countries.contains(country)) {
                countries.add(country);
            }
        }
        Collections.sort(countries);
        countries.add(0,"");
        countries.add(1, Locale.US.getDisplayCountry());
        return countries;
    }

	public static String getMD5(String str) {
		try {
			MessageDigest digest;
			digest = MessageDigest.getInstance("MD5");
			digest.reset();
			digest.update(str.getBytes());
			byte[] hashCode = digest.digest();
			StringBuffer MD5Hash = new StringBuffer();
            for (int i = 0; i < hashCode.length; i++)
            {
                String h = Integer.toHexString(0xFF & hashCode[i]);
                while (h.length() < 2)
                    h = "0" + h;
                MD5Hash.append(h);
            }
            return MD5Hash.toString();
		} catch (NoSuchAlgorithmException e1) {
			e1.printStackTrace();
			return str;
		}
	}

	public static synchronized String dateTimeYYYYMMDD(Date dateTime) {
		SimpleDateFormat formatYYYYMMDD = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
		return formatYYYYMMDD.format(dateTime);
	}

	public static synchronized String dateTimeYYYYMMDDHHMMSS(Date dateTime) {
		SimpleDateFormat formatYYYYMMDDHHMMSS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		return formatYYYYMMDDHHMMSS.format(dateTime);
	}

	public static synchronized String dateTimeYYYYMMDDHHMM(Date dateTime) {
		SimpleDateFormat formatYYYYMMDDHHMM = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
		return formatYYYYMMDDHHMM.format(dateTime);
	}

	public static synchronized String dateTimeYYYYMMDDHHMM_WITH_T(Date dateTime) {
		SimpleDateFormat formatYYYYMMDDHHMM_WITH_T = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US);
		return formatYYYYMMDDHHMM_WITH_T.format(dateTime);
	}

	public static synchronized String dateTimeYYYYMMDDHHMMSS_GMT(Date dateTime) {
		SimpleDateFormat formatYYYYMMDDHHMMSS_GMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		formatYYYYMMDDHHMMSS_GMT.setTimeZone(TimeZone.getTimeZone("GMT"));
		return formatYYYYMMDDHHMMSS_GMT.format(dateTime);
	}

	public static synchronized String dateTimeMMDDHHMMSS(Date dateTime) {
		SimpleDateFormat formatMMDDHHMMSS = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.US);
		return formatMMDDHHMMSS.format(dateTime);
	}

	public static synchronized String dateTimeMMMDDHHMM(Date dateTime) {
		SimpleDateFormat formatMMMDDHHMM = new SimpleDateFormat("MMM-dd HH:mm", Locale.US);
		return formatMMMDDHHMM.format(dateTime);
	}

	public static synchronized String dateTimeMMDDYYYY(Date dateTime) {
		SimpleDateFormat formatMMDDYYYY = new SimpleDateFormat("MM-dd-yyyy", Locale.US);
		return formatMMDDYYYY.format(dateTime);
	}
	public static synchronized String dateTimeHHMM(Date dateTime) {
		SimpleDateFormat formatMMDDYYYY = new SimpleDateFormat("hh:mm a", Locale.US);
		return formatMMDDYYYY.format(dateTime);
	}

	/*public static String getCurrentVersion() {
    	PackageManager pm = CCUApp.getAppContext().getPackageManager();
		PackageInfo pi;
		try {
			pi = pm.getPackageInfo("com.x75fahrenheit.ccu", 0);
			return pi.versionName + "."+ String.valueOf(pi.versionCode);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}*/

	/*public static int getCurrentVersionNumber() {
    	PackageManager pm = CCUApp.getAppContext().getPackageManager();
		PackageInfo pi;
		try {
			pi = pm.getPackageInfo("com.x75fahrenheit.ccu", 0);
			return pi.versionCode;
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
	}*/

	public static double roundTo2Decimal(double number) {
		DecimalFormat df = new DecimalFormat("#.#");
		return Double.parseDouble(df.format(number));
	}
	public static double roundToTwoDecimal(double number) {
		DecimalFormat df = new DecimalFormat("#.##");
		return Double.parseDouble(df.format(number));
	}
	public static double calculateAirEnthalpy(double mCurrentTemp, double mCurrentHumidity) {
	/*	10 REM ENTHALPY CALCULATION
		20 REM Assumes standard atmospheric pressure (14.7 psi), see line 110
		30 REM Dry-bulb temperature in degrees F (TEMP)
		40 REM Relative humidity in percentage (RH)
		50 REM Enthalpy in BTU/LB OF DRY AIR (H)
		60 T = TEMP + 459.67
		70 N = LN( T ) : REM Natural Logrithm
		80 L = -10440.4 / T - 11.29465 - 0.02702235 * T + 1.289036E-005 * T ^ 2 - 2.478068E-009 * T ^ 3 + 6.545967 * N
		90 S = LN-1( L ) : REM Inverse Natural Logrithm
		100 P = RH / 100 * S
		110 W = 0.62198 * P / ( 14.7 - P )
		120 H = 0.24 * TEMP + W * ( 1061 + 0.444 * TEMP )
	*/
	/*  A = .007468 * DB^2 - .4344 * DB + 11.1769

		B = .2372 * DB + .1230

		Enth = A * RH + B
*/
/*		double t = mCurrentTemp + 459.67;
		double n = Math.log(t);
		double l = -10440.4/t - 11.29465 - 0.02702235*t + Math.pow(0.00001289036*t,2) - Math.pow(0.000000002478068*t, 3) + 6.545967*n;
		double s = Math.exp(l);
		double p = mCurrentHumidity*s;
		double w = (0.62198*p)/(14.7-p);
		double h = 0.24*mCurrentTemp + w*(1061+0.444*mCurrentTemp);
		Log.d("CCU_WEATHER", String.format("%f %f %f %f %f %f %f", t, n, l, s, p, w, h));
*/
		double A = 0.007468* Math.pow(mCurrentTemp,2) - 0.4344*mCurrentTemp + 11.1769;
		double B = 0.2372*mCurrentTemp + 0.1230;
		double H = A*mCurrentHumidity+B;
		Log.d("CCU_WEATHER", String.format("Enthalpy %f %f %f", mCurrentTemp, mCurrentHumidity, H));
		return H;
	}

	public static double calculateRHForTemperatureChange(double rh, double oldTemp, double newTemp) {
		double kOldTemp = convertFToKTemp(oldTemp);
		double kNewTemp = convertFToKTemp(newTemp);
		double pH2OOldTemp = getPH2O(kOldTemp);
		double pH2ONewTemp = getPH2O(kNewTemp);
		double newRH = (pH2OOldTemp/pH2ONewTemp*kOldTemp/kNewTemp*rh);
		Log.d("CCU_DCV", String.format("Old RH %f, Old Temp %f(%f), New Temp %f(%f), Old PH2O %f, New PH2O %f, New RH %f", rh, kOldTemp, oldTemp, kNewTemp, newTemp, pH2OOldTemp, pH2ONewTemp, newRH));
		return newRH;
	}

	private static double convertFToKTemp(double inTemp) {
		return (inTemp+459.67)*5.0/9.0;
	}

	private static double getPH2O(double inTemp) {
		return Math.exp(20.386-5132/inTemp);
	}


	static boolean isRunning = false;
	public static List<Address> getLocationInfo(final String locadd) {
		Log.d("CCU_WEATHER",locadd);
		if(locadd.isEmpty() || isRunning)

			return locAddress;
		else {
			final AsyncTask<Void, Integer, List<Address>> task = new AsyncTask<Void, Integer, List<Address>>() {

				@Override
				protected void onPostExecute(List<Address> addresses) {
					isRunning = false;
					Log.d("CCU_WEATHER","ccutils onPostExe="+addresses.size());
					if (!addresses.isEmpty()) {
						double lat = (float) addresses.get(0).getLatitude();
						double lng = (float) addresses.get(0).getLongitude();
						String address = addresses.get(0).getAddressLine(0) ;
						SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext());
						spDefaultPrefs.edit().putFloat("lat", (float) lat).commit();
						spDefaultPrefs.edit().putFloat("lng",  (float) lng).commit();
						spDefaultPrefs.edit().putString("address",address).commit();
						WeatherDataDownloadService.getWeatherData();
					}
					super.onPostExecute(addresses);
				}

				@Override
				protected List<Address> doInBackground(Void... params) {
					isRunning = true;
					locAddress.clear();
					SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(RenatusApp.getAppContext());

					if (BuildConfig.DEBUG)
					{
						StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
						StrictMode.setThreadPolicy(policy);
					}

					HttpClient client = HTTPUtils.getNewHttpClient();
					try {
						//String address = locadd.replaceAll(" ", "%20");
                        String address = locadd.replaceAll(" ", "+");
						//https://maps.google.com/maps/api/geocode/json?address=54016, United States&sensor=false&key=AIzaSyD3mUArjl1fvA7EBy6M8x8FJSKpKS3RmOg
						HttpPost httppost = new HttpPost( "https://maps.google.com/maps/api/geocode/json?address=" + address + "&key=AIzaSyD3mUArjl1fvA7EBy6M8x8FJSKpKS3RmOg");

						org.apache.http.HttpResponse response;
						StringBuilder stringBuilder = new StringBuilder();

						response = client.execute(httppost);
						HttpEntity entity = response.getEntity();
						InputStream stream = entity.getContent();
						int b;
						while ((b = stream.read()) != -1) {
							stringBuilder.append((char) b);
						}
						client.getConnectionManager().closeExpiredConnections();
						Log.d("CCU_WEATHER","success auto on response="+response.getStatusLine() + ","+stringBuilder.toString());
						JSONObject jsonObject = new JSONObject(stringBuilder.toString());
						JSONArray array = (JSONArray) jsonObject.get("results");
						if(array.length() > 0) {
							for (int i = 0; i < 1; i++) {
								double lon = 0;
								double lat = 0;
								String name = "";
								try {
									lon = array.getJSONObject(i).getJSONObject("geometry")
											.getJSONObject("location").optDouble("lng");

									lat = array.getJSONObject(i).getJSONObject("geometry")
											.getJSONObject("location").optDouble("lat");
									name = array.getJSONObject(i)
											.optString("formatted_address");
									Address addr = new Address(Locale.getDefault());
									addr.setLatitude(lat);
									addr.setLongitude(lon);
									addr.setAddressLine(0, name != null ? name : "");
									spDefaultPrefs.edit().putFloat("lat", (float) lat).commit();
									spDefaultPrefs.edit().putFloat("lng", (float) lon).commit();
									locAddress.add(addr);
								} catch (JSONException e) {
									e.printStackTrace();

								}
							}
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
					catch (ClientProtocolException e1) {
					}
					catch (IOException e2) {
					}finally {
						client.getConnectionManager().closeExpiredConnections();
					}
					return locAddress;
				}
			};
			task.execute();
		}
		return locAddress;

	}
	public static String getTimeFromTimeStamp(long timeStamp, String timezone) {
		String ret= null;

		try {
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.US);
			sdf.setTimeZone(TimeZone.getTimeZone(timezone));
			Date netDate = new Date(timeStamp *1000);
			ret =sdf.format(netDate);
		}catch (Exception e){
			e.printStackTrace();
		}
		return ret;
	}

	public static Calendar getTimeFromHourString(String val, int offset){
		int hr = Integer.parseInt(val.substring(0,2));
		int min = Integer.parseInt(val.substring(3,5));
		if(offset != 0){
			min = min+offset;
			if(min > 60){
				hr = hr + (min  / 60);
				min = min %60;
			}
		}
		Calendar cal = Calendar.getInstance();

		if(hr == 24){
			cal.set(Calendar.HOUR_OF_DAY, 23);
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			cal.set(Calendar.MILLISECOND, 999);
		}else {
			cal.set(Calendar.HOUR_OF_DAY, hr);
			cal.set(Calendar.MINUTE, min);
			cal.set(Calendar.SECOND, 30);
		}

		return cal;

	}
	public static Date getEndOfDay() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 23);
		calendar.set(Calendar.MINUTE, 59);
		calendar.set(Calendar.SECOND, 59);
		calendar.set(Calendar.MILLISECOND, 999);
		return calendar.getTime();
	}

    public static Date getStartOfDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    public static String getValue(String item) {
        String value = "NA";
        if (!Strings.isNullOrEmpty(item)) {
            value = item;
        }
        return value;
    }

}
