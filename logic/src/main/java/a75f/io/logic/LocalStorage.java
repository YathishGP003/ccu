package a75f.io.logic;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.IOException;
import java.util.HashMap;

import a75f.io.logic.bo.building.CCUApplication;
import a75f.io.logic.bo.haystack.Site;
import a75f.io.logic.haystack.CCUHsApi;

import static a75f.io.logic.LLog.Logd;

class LocalStorage
{
    
    public static final String TAG = LocalStorage.class.getSimpleName();
    
    private static final String PREFS_CCU_SETTINGS_CONST = "ccu_settings";
    private static final String VAR_CCU_SETTINGS         = "ccu_key";
    private static final String VAR_IS_USER_REGISTERED   = "isUserRegistered";
    
    
    public static CCUApplication getApplicationSettings()
    {
        String ccuSettings = getCCUSettings(L.app()).getString(VAR_CCU_SETTINGS, null);
        Logd("==========GET APPLICATION SETTINGS================");
        Logd(ccuSettings != null ? ccuSettings : "Settings are empty");
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
        
        //Add site point if not done already.
        HashMap site = CCUHsApi.getInstance().read("site");
        
        if (site.size() == 0) {
            //TODO - demo
            Site s75f = new Site.Builder()
                             .setDisplayName("75F")
                             .addMarker("site")
                             .setGeoCity("Burnsville")
                             .setGeoState("MN")
                             .setTz("Chicago")
                             .setArea(20000).build();
            CCUHsApi.getInstance().addSite(s75f);
        }
        
        return ccu;
    }
    
    
    public static SharedPreferences getCCUSettings(Context context)
    {
        return context.getSharedPreferences(PREFS_CCU_SETTINGS_CONST, Context.MODE_PRIVATE);
    }
    
    
    public static String getApplicationSettingsAsString()
    {
        String ccuSettings = getCCUSettings(L.app()).getString(VAR_CCU_SETTINGS, null);
        Logd("==========GET APPLICATION SETTINGS================");
        Logd(ccuSettings != null ? ccuSettings : "Settings are empty");
        return ccuSettings;
    }
    
    
    public static boolean getIsUserRegistered()
    {
        return getCCUSettings(L.app()).getBoolean(VAR_IS_USER_REGISTERED, false);
    }
    
    
    public static void setIsUserRegistered(boolean isUserRegistered)
    {
        getCCUSettings(L.app()).edit().putBoolean(VAR_IS_USER_REGISTERED, isUserRegistered).apply();
    }
    
    
    public static void setApplicationSettings()
    {
        try
        {
            String jsonString = JsonSerializer.toJson(L.ccu(), true);
            Logd("==========SET APPLICATION SETTINGS================");
            Logd(jsonString);
            getCCUSettings(L.app()).edit().putString(VAR_CCU_SETTINGS, jsonString).apply();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
