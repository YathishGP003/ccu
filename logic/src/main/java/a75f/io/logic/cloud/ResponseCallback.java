package a75f.io.logic.cloud;

import org.json.JSONException;
import org.json.JSONObject;

public interface ResponseCallback {
    public void onSuccessResponse(JSONObject response) throws JSONException;
    public void onErrorResponse(JSONObject response) throws JSONException;
}
