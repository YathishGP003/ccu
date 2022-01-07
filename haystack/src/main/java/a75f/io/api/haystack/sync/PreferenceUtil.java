package a75f.io.api.haystack.sync;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceUtil {
    private static final String PREF_UUID_MIGRATION_COMPLETED = "uuidMigrationCompleted";
    public static boolean getUuidMigrationCompleted(Context appContext) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        return sharedPreferences.getBoolean(PREF_UUID_MIGRATION_COMPLETED,false);
    }
    
    public static void setUuidMigrationCompleted(boolean isMigrated, Context appContext) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(PREF_UUID_MIGRATION_COMPLETED, isMigrated);
        editor.apply();
    }
}
