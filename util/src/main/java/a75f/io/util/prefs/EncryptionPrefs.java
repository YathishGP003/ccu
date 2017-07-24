package a75f.io.util.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.security.SecureRandom;

import a75f.io.util.Globals;

/**
 * Created by ryanmattison on 7/24/17.
 */

public class EncryptionPrefs {

    private static final String PREFS_SYSTEM_SETTINGS_CONST = "system_settings";
    private static final String VAR_SYSTEM_SETTINGS_ENCRYPTION_KEY = "system_settings_encryption_key";
    private static final String TAG = EncryptionPrefs.class.getSimpleName();

    //128 bits as bytes.
    private static final int KEY_SIZE = 128 / 8;

    private static SharedPreferences getSystemSettings()
    {
        return Globals.getInstance().getApplicationContext().getSharedPreferences(PREFS_SYSTEM_SETTINGS_CONST, Context.MODE_PRIVATE);
    }

    public static byte[] getEncryptionKey() {
        String encryptionKeyAsString = getSystemSettings().getString(VAR_SYSTEM_SETTINGS_ENCRYPTION_KEY, null);
        if(encryptionKeyAsString == null || encryptionKeyAsString.equalsIgnoreCase(""))
            return fillEncryptionKey();
        else
            return Base64.decode(encryptionKeyAsString, Base64.DEFAULT);
    }

    private static byte[] fillEncryptionKey() {
        SharedPreferences.Editor systemSettings = getSystemSettings().edit();
        SecureRandom secureRandom = new SecureRandom();
        byte[] bytes = secureRandom.generateSeed(KEY_SIZE);
        String encryptionKeyAsString = Base64.encodeToString(bytes, Base64.DEFAULT);
        systemSettings.putString("encryption_key", encryptionKeyAsString);
        systemSettings.apply();
        return bytes;
    }

    public static void logEncryptionKey() {
        byte[] encryptionKey = getEncryptionKey();
        String encryptionKeyAsString = Base64.encodeToString(encryptionKey, Base64.DEFAULT);
        Log.i(TAG, "Encryption Key As String: " + encryptionKeyAsString);
    }


}
