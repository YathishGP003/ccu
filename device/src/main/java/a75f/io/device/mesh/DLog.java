package a75f.io.device.mesh;

import android.util.Log;

import org.javolution.io.Struct;

import java.io.IOException;

import a75f.io.device.json.serializers.JsonSerializer;
/**
 * Created by Yinten isOn 8/21/2017.
 */

/**
 * This class will log to crashlytics important information for development.
 */
public class DLog
{
	
	private static final String TAG             = DLog.class.getSimpleName();
	private static final String SERIAL_TAG      = "Serial";
    public static final String UPDATED_ZONE_TAG = "UPDATE_ZONE";
    public static final String UPDATED_STRUCT     = "UPDATED_STRUCT";
    
    
    public static <T extends Struct>  void LogdStructAsJson(T struct)
	{
		//if (BuildConfig.DEBUG)
		//{
			String structString = null;
		
			//We can bury this exception because when we log a struct to json, it is purely for loggin purposes
			try
			{
				structString = JsonSerializer.toJson(struct, true);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			Log.d(UPDATED_STRUCT, structString);
		//}
	}
	
	
	public static void logUSBServiceNotInitialized()
	{
		Log.i(TAG, "USB SERVICE NOT INITIALIZED! ");
	}
	
	
	public  static void Logd(String message)
	{
	//	if (BuildConfig.DEBUG)
		//{
			Log.d(TAG, message);
		//}
	}
	
	public static void LogdSerial(String message)
	{
		
	//	if (BuildConfig.DEBUG)
	//	{
			Log.d(SERIAL_TAG, message);
		//}
	}
	
	public static String objectNullString(Object object)
	{
		return object == null ? " is null. " : " is not null. ";
	}
	
	
	public static void Logw(String message)
	{
		Log.w(TAG, message);
	}
}
