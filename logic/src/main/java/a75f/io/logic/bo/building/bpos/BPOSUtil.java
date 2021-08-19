package a75f.io.logic.bo.building.bpos;

import android.util.Log;
import java.util.HashMap;
import a75f.io.api.haystack.CCUHsApi;

/*
 * created by spoorthidev on 3-August-2021
 */

public class BPOSUtil {

    private static String LOG_TAG = "BPOSUtil";

    public static HashMap getbposPoints(String equipID) {
        HashMap points = new HashMap();
        points.put("Profile", "Temperature Influencing");
        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and status and " +
                "message and equipRef == \"" + equipID + "\"");
        double humidity = CCUHsApi.getInstance().readHisValByQuery("point and air and humidity " +
                "and sensor " +
                "and equipRef == \"" + equipID + "\"");
        double forceoccupied = CCUHsApi.getInstance().readHisValByQuery("point and occupancy and" +
                " sensor and equipRef == \"" + equipID + "\"");

        Log.d("BPOSUtil", "equipStatusPoint =" + equipStatusPoint +
                "humidity =" + humidity + "forceoccupied" + forceoccupied);

        if (equipStatusPoint.length() > 0) {
            points.put("Status", equipStatusPoint);
        } else {
            points.put("Status", "OFF");
        }
        points.put("humidity", String.valueOf(humidity));
        points.put("forceoccupied", forceoccupied);
        return points;
    }
}
