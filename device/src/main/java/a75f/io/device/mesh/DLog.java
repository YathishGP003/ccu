package a75f.io.device.mesh;

import android.util.Log;

import org.javolution.io.Struct;

import java.io.IOException;

import a75f.io.device.json.serializers.JsonSerializer;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
/**
 * Created by Yinten isOn 8/21/2017.
 */

/**
 * This class will log to crashlytics important information for development.
 */
public class DLog
{
	
	private static final String TAG             = DLog.class.getSimpleName();


	public static <T extends Struct>  void
    
    
    
    
    LogdStructAsJson(T struct)
	{
		CcuLog.d(L.TAG_CCU_SERIAL, "MSG: "+struct.toString());

			String structString = null;
		
			//We can bury this exception because when we log a struct to json, it is purely for loggin purposes
			try
			{
				structString = JsonSerializer.toJson(struct, true);
				CcuLog.d(L.TAG_CCU_SERIAL, structString);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			catch (NullPointerException e)
			{
				e.printStackTrace();
			}

	}

	public static <T extends Struct>  void




	tempLogdStructAsJson(T struct)
	{

		String structString = null;


		try
		{
			structString = JsonSerializer.toJson(struct, true);
			CcuLog.d("CCU_SN_MESSAGES", structString+"\n\n");

		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (NullPointerException e)
		{
			e.printStackTrace();
		}

	}
	
	public static void logUSBServiceNotInitialized()
	{
		Log.i(TAG, "USB SERVICE NOT INITIALIZED! ");
	}
	
	
	public  static void Logd(String message)
	{
		CcuLog.d(L.TAG_CCU_SERIAL, message);
	}
	
	public static void LogdSerial(String message)
	{
		CcuLog.d(L.TAG_CCU_SERIAL, message);
	}


	public static boolean isLoggingEnabled() {
    	return true;
	}
}
