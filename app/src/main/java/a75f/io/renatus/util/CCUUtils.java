package a75f.io.renatus.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Address;

import com.google.common.base.Strings;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.renatus.R;


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
	public enum SMARTSTAT_OP_FANSPEED{FANSPEED_OFF,FANSPEED_AUTO,FANSPEED_LOW, FANSPEED_HIGH, FANSPEED_HIGH2}
	public enum STANDALONE_CONDITION_FANSPEED{FANSPEED_LOW, FANSPEED_HIGH} //For Fan Auto modes
	public enum SMARTSTAT_OP_CONDITIONING_MODE{STANDALONE_OFF, STANDALONE_AUTO, STANDALONE_HEATING,STANDALONE_COOLING}
	public enum STANDALONE_CONDITIONING_MODE{COOLING, HEATING} //For operational Auto modes

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
		70 N = LN( T ) : REM Natural Logarithm
		80 L = -10440.4 / T - 11.29465 - 0.02702235 * T + 1.289036E-005 * T ^ 2 - 2.478068E-009 * T ^ 3 + 6.545967 * N
		90 S = LN-1( L ) : REM Inverse Natural Logarithm
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
		CcuLog.d(L.TAG_CCU_WEATHER, String.format("Enthalpy %f %f %f", mCurrentTemp, mCurrentHumidity, H));
		return H;
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

	public static String getValue(String item) {

        String value = "NA";
        if (!Strings.isNullOrEmpty(item)) {
            value = item;
        }
        return value;
    }
	
	public static void resetPasswords(Context context) {
		Prefs prefs = new Prefs(Globals.getInstance().getApplicationContext());
		final String DEFAULT_RESET_PASSWORD = "7575";
		if (prefs.getBoolean(context.getString(R.string.SET_ZONE_PASSWORD))) {
			prefs.setString(context.getString(R.string.ZONE_SETTINGS_PASSWORD_KEY), DEFAULT_RESET_PASSWORD);
		}

		if (prefs.getBoolean(context.getString(R.string.SET_SYSTEM_PASSWORD))) {
			prefs.setString(context.getString(R.string.SYSTEM_SETTINGS_PASSWORD_KEY), DEFAULT_RESET_PASSWORD);
		}

		if (prefs.getBoolean(context.getString(R.string.SET_BUILDING_PASSWORD))) {
			prefs.setString(context.getString(R.string.BUILDING_SETTINGS_PASSWORD_KEY), DEFAULT_RESET_PASSWORD);
		}

		if (prefs.getBoolean(context.getString(R.string.SET_SETUP_PASSWORD))) {
			prefs.setString(context.getString(R.string.USE_SETUP_PASSWORD_KEY), DEFAULT_RESET_PASSWORD);
		}

	}

	public static void updateMigrationDiagWithAppVersion(){
		PackageManager manager = Globals.getInstance().getApplicationContext().getPackageManager();
		try {
			PackageInfo info = manager.getPackageInfo(Globals.getInstance().getApplicationContext().getPackageName(), 0);
			String appVersion = info.versionName;
			String migrationVersion=appVersion.substring(appVersion.lastIndexOf('_') + 1);
			CCUHsApi.getInstance().writeDefaultVal("point and diag and migration", migrationVersion);
			CcuLog.d(L.TAG_CCU_MIGRATION_UTIL, "Update Migration Diag Point "+migrationVersion);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}

	}

}
