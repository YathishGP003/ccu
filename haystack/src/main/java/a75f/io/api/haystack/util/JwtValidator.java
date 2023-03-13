package a75f.io.api.haystack.util;

import android.util.Base64;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import a75f.io.logger.CcuLog;

public class JwtValidator {
    private static final String TAG = "JwtValidator";
    private final String tokenString;
    public JwtValidator(String token) {
        tokenString = token;
    }

    public boolean isExpired() throws JSONException {
        String[] parts = tokenString.split("\\.");
        byte[] decoded = android.util.Base64.decode(parts[1].getBytes(),
                        Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        String decodedTokenString = new String(decoded, StandardCharsets.UTF_8);
        JSONObject payload = new JSONObject(decodedTokenString);
        Long expiry = payload.getLong("exp") * 1000;
        if (expiry > 0) {
            CcuLog.i(TAG, "JwtValidator : Token Expires at " + (new Date(expiry)));
            return expiry > System.currentTimeMillis();
        }
        throw new IllegalArgumentException();
    }
}
