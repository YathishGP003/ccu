package a75f.io.renatus.util;

/**
 * Created by Suresh on 20/05/2019.
 */

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class Prefs {

    private SharedPreferences sprefs;

    public Prefs(Context ctx) {
        sprefs = PreferenceManager.getDefaultSharedPreferences(ctx);
    }

    public void setInt(String key, int value) {
        Editor editor = sprefs.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public void setBoolean(String key, boolean value) {
        Editor editor = sprefs.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public void setString(String key, String value) {
        Editor editor = sprefs.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public void setFloat(String key, float value) {
        Editor editor = sprefs.edit();
        editor.putFloat(key, value);
        editor.commit();
    }

    public void setLong(String key, long value) {
        Editor editor = sprefs.edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public int getInt(String key) {
        return sprefs.getInt(key, -1);
    }

    public boolean getBoolean(String key) {
        return sprefs.getBoolean(key, false);
    }

    public String getString(String key) {
        return sprefs.getString(key, "");
    }

    public float getFloat(String key) {
        return sprefs.getFloat(key, -1.0f);
    }

    public long getLong(String key) {
        return sprefs.getLong(key, -1);
    }

    public boolean remove(String key) {
        try {
            Editor editor = sprefs.edit();
            editor.remove(key);
            editor.commit();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
