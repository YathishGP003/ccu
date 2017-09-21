package a75f.io.logic;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import java.security.SecureRandom;

/**
 * Created by ryanmattison isOn 7/24/17.
 */

class EncryptionPrefs {

    private static final String PREFS_SYSTEM_SETTINGS_CONST = "system_settings";
    private static final String VAR_SYSTEM_SETTINGS_ENCRYPTION_KEY = "system_settings_encryption_key";
    private static final String TAG = EncryptionPrefs.class.getSimpleName();

    private static final byte[] BLE_LINK_KEY = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 0, -1, -1, -1,
            -1, -1, -1, -1, 0};

    private static final byte[] FIRMWARE_SIGNATURE_KEY = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
            0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F};

    //128 bits as bytes.
    private static final int KEY_SIZE = 128 / 8;

    private static SharedPreferences getSystemSettings()
    {
        return L.app().getSharedPreferences(PREFS_SYSTEM_SETTINGS_CONST, Context.MODE_PRIVATE);
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
        systemSettings.putString(VAR_SYSTEM_SETTINGS_ENCRYPTION_KEY, encryptionKeyAsString);
        systemSettings.apply();
        return bytes;
    }

    public static void logEncryptionKey() {
        byte[] encryptionKey = getEncryptionKey();
        String encryptionKeyAsString = Base64.encodeToString(encryptionKey, Base64.DEFAULT);
        Log.i(TAG, "Encryption Key As String: " + encryptionKeyAsString);
    }

    public static byte[] getBLELinkKey()
    {
        return BLE_LINK_KEY;
    }

    public static byte[] getFirmwareSignatureKey()
    {
        return FIRMWARE_SIGNATURE_KEY;
    }



}
