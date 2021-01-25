package a75f.io.logic.pubnub;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

class PbPreferences {
    
    private static final String PB_PREFS_LAST_HANDLED_TIME = "pbLastHandledTimeToken";
    
    public static Long getLastHandledTimeToken(Context context) {
        SharedPreferences spDefaultPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        return spDefaultPrefs.getLong(PB_PREFS_LAST_HANDLED_TIME, 0);
    }
    
    public static void setLastHandledTimeToken(Long timeToken, Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(PB_PREFS_LAST_HANDLED_TIME, timeToken);
        editor.commit();
    }
}
