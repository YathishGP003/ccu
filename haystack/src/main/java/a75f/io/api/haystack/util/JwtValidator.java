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

    /**
     * Checks validity in terms of expiry.
     * @return - true if token is not expired.
     */
    public boolean isValid() {
        String[] parts = tokenString.split("\\.");
        if (parts.length <= 1) {
            throw new JwtValidationException("Invalid token. Cant split to validate");
        }
        byte[] decoded;
        try {
            decoded = Base64.decode(parts[1].getBytes(),
                    Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
        } catch (IllegalArgumentException e) {
            throw new JwtValidationException("Invalid Base-64 Encoding");
        }

        String decodedTokenString = new String(decoded, StandardCharsets.UTF_8);
        long expiry;
        try {
            JSONObject payload = new JSONObject(decodedTokenString);
            expiry = payload.getLong("exp") * 1000;
        } catch (JSONException e) {
            throw new JwtValidationException("Failed to parse Jwt Token : "+e.getMessage());
        }

        if (expiry <= 0) {
            throw new JwtValidationException("Invalid expiry time");
        }
        CcuLog.i(TAG, "Token Expires at " + (new Date(expiry)));
        return expiry > System.currentTimeMillis();
    }
}
