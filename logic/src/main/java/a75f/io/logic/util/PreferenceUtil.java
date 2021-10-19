package a75f.io.logic.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class PreferenceUtil {
    private static Context context;

    public static void setContext(Context c) {
        context= c;
    }

    public static String getOTPGeneratedToAddOrReplaceCCU() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("OTPToAddOrReplaceCCU","");
    }

    public static void setOTPGeneratedToAddOrReplaceCCU(String otp) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("OTPToAddOrReplaceCCU", otp);
        editor.apply();
    }

    public static boolean isHeartbeatMigrationDone() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("heartbeatMigration",false);
    }

    public static void setHeartbeatMigrationStatus(boolean isMigrated) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("heartbeatMigration", isMigrated);
        editor.apply();
    }

    public static boolean isHeartbeatMigrationAsDiagDone() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("heartbeatMigrationAsDiagWithRssi",false);
    }

    public static void setHeartbeatMigrationAsDiagStatus(boolean isMigrated) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("heartbeatMigrationAsDiagWithRssi", isMigrated);
        editor.apply();
    }

    public static boolean isFirmwareVersionPointMigrationDone() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("firmwareVersionPointMigration",false);
    }

    public static void setFirmwareVersionPointMigrationStatus(boolean isMigrated) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("firmwareVersionPointMigration", isMigrated);
        editor.apply();
    }

    public static boolean isOAODamperOpenPointsMigrationDone() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getBoolean("OAODamperOpenPointsMigration",false);
    }

    public static void setOAODamperOpenPointsMigrationStatus(boolean isMigrated) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("OAODamperOpenPointsMigration", isMigrated);
        editor.apply();
    }

    public static boolean isHeartbeatTagMigrationDone() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Log.d("heartbeattag", "isHeartbeatTagMigrationDone return "+sharedPreferences.getBoolean("heartbeattagMigration",false));
        return sharedPreferences.getBoolean("heartbeattagMigration",false);
    }

    public static void setHeartbeatTagMigrationStatus(boolean isMigrated) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("heartbeattagMigration", isMigrated);
        editor.apply();
    }
}
