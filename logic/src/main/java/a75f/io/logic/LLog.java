package a75f.io.logic;

import android.util.Log;

import org.javolution.io.Struct;

import java.io.IOException;

import a75f.io.bo.json.serializers.JsonSerializer;
/**
 * Created by Yinten isOn 8/21/2017.
 */

/**
 * This class will log to crashlytics important information for development.
 */
public class LLog
{
	
	private static final String TAG = LLog.class.getSimpleName();
	
	
	public static void logStructAsJSON(Struct struct)
	{
		Log.i(TAG, "YOLO");
		//if (BuildConfig.DEBUG)
		//{
			String structString = null;
			try
			{
				structString = JsonSerializer.toJson(struct, true);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			System.out.println("Struct As String:\n" + structString + "\n");
		//}
	}
	
	
	public static void logUSBServiceNotInitialized()
	{
		Log.i(TAG, "USB SERVICE NOT INITIALIZED! ");
	}
	
	
	public  static void Logd(String message)
	{
		if (BuildConfig.DEBUG)
		{
			Log.d(TAG, message);
		}
	}
}
