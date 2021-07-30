package a75f.io.logic.cloud;

import org.json.JSONException;
import org.json.JSONObject;

public interface OtpResponseCallBack {
    public void onOtpResponse(JSONObject response) throws JSONException;
    public void onOtpErrorResponse(JSONObject response) throws JSONException;
}
