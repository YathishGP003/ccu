package a75f.io.util.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.IOException;

import a75f.io.bo.json.serializers.JsonSerializer;
import a75f.io.util.Globals;

/**
 * Created by Yinten on 8/21/2017.
 */

public class ApplicationPreference
{
	
	public static final  short  RYAN_PORT                   = 9600;
	public static final  String TAG                         = LocalStorage.class.getSimpleName();
	private static final String PREFS_CCU_PREFERENCES_CONST = "ccu_preferences";
	private static final String PREFS_APPLICATION           = "application_preferences";
	
	short mSmartNodeAddressRange;
	
	
	@JsonIgnore
	public static ApplicationPreference getApplicationSettings()
	{
		String ccuSettings = getCCUSettings().getString(PREFS_CCU_PREFERENCES_CONST, null);
		if (ccuSettings != null && !ccuSettings.equals(""))
		{
			try
			{
				return (ApplicationPreference) JsonSerializer
						                               .fromJson(ccuSettings, ApplicationPreference.class);
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return new ApplicationPreference();
	}
	
	
	@JsonIgnore
	private static SharedPreferences getCCUSettings()
	{
		return Globals.getInstance().getApplicationContext()
		              .getSharedPreferences(PREFS_APPLICATION, Context.MODE_PRIVATE);
	}
	
	
	public short getSmartNodeAddressBand()
	{
		if (mSmartNodeAddressRange == 0)
		{
			mSmartNodeAddressRange = RYAN_PORT;
			save();
		}
		return mSmartNodeAddressRange;
	}
	
	
	@JsonIgnore
	private void save()
	{
		try
		{
			String jsonString = JsonSerializer.toJson(this, false);
			getCCUSettings().edit().putString(PREFS_CCU_PREFERENCES_CONST, jsonString).apply();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	
	public void setSmartNodeAddressRange(short smartNodeAddressRange)
	{
		this.mSmartNodeAddressRange = mSmartNodeAddressRange;
		save();
	}
}
