package a75f.io.util.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.json.serializers.JsonSerializer;
import a75f.io.util.Globals;

/**
 * Created by Yinten on 8/15/2017.
 */

public class LocalStorage
{
	
	public static final String TAG = LocalStorage.class.getSimpleName();
	
	private static final String PREFS_CCU_SETTINGS_CONST = "ccu_settings";
	private static final String VAR_CCU_SETTINGS         = "ccu_key";
	
	
	public static CCUApplication getApplicationSettings()
	{
		String ccuSettings = getCCUSettings().getString(PREFS_CCU_SETTINGS_CONST, null);
		if (ccuSettings != null && !ccuSettings.equals(""))
		{
			try
			{
				return (CCUApplication) JsonSerializer.fromJson(ccuSettings, CCUApplication.class);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return new CCUApplication();
	}
	
	
	public static void setApplicationSettings(CCUApplication ccuApplication)
	{
		try
		{
			String jsonString = JsonSerializer.toJson(ccuApplication, false);
			getCCUSettings().edit().putString(jsonString, null).apply();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	private static SharedPreferences getCCUSettings()
	{
		return Globals.getInstance().getApplicationContext().getSharedPreferences(PREFS_CCU_SETTINGS_CONST, Context.MODE_PRIVATE);
	}
}
