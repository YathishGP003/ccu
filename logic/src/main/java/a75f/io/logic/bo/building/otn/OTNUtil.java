package a75f.io.logic.bo.building.otn;

import java.util.HashMap;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logger.CcuLog;

/*
 * created by spoorthidev on 3-August-2021
 */

public class OTNUtil {

    private static String LOG_TAG = "OTNUtil";

    public static HashMap getOTNPoints(String equipID) {
        HashMap points = new HashMap();
        points.put("Profile", "Temperature Influencing");
        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and " +
                "message and equipRef == \"" + equipID + "\"");
        double humidity = CCUHsApi.getInstance().readHisValByQuery("point and air and humidity " +
                "and sensor " +
                "and equipRef == \"" + equipID + "\"");
        double forceoccupied = CCUHsApi.getInstance().readHisValByQuery("point and (occupancy or occupied) and" +
                " mode and equipRef == \"" + equipID + "\"");

        CcuLog.d(LOG_TAG, "equipStatusPoint =" + equipStatusPoint +
                "humidity =" + humidity + "forceoccupied" + forceoccupied);

        if (!equipStatusPoint.isEmpty()) {
            points.put("Status", equipStatusPoint);
        } else {
            points.put("Status", "OFF");
        }
        points.put("humidity", String.valueOf(humidity));
        points.put("forceoccupied", forceoccupied);
        return points;
    }
}
