package a75f.io.logic;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;

import a75f.io.logic.bo.building.CCUApplication;

class LocalStorage
{
    
    public static final String TAG = LocalStorage.class.getSimpleName();
    
    private static final String PREFS_CCU_SETTINGS_CONST = "ccu_settings";
    private static final String VAR_CCU_SETTINGS         = "ccu_key";
    
    
    public static CCUApplication getApplicationSettings()
    {
        String ccuSettings = getCCUSettings(L.app()).getString(VAR_CCU_SETTINGS, null);
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
        CCUApplication ccu = new CCUApplication();
        return ccu;
    }
    
    
    public static SharedPreferences getCCUSettings(Context context)
    {
        return context.getSharedPreferences(PREFS_CCU_SETTINGS_CONST, Context.MODE_PRIVATE);
    }

}
