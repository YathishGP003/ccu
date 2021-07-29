package a75f.io.logic.bo.building.bpos;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;

public class BPOSUtil {

    public static HashMap getbposPoints(String equipID) {

        HashMap points = new HashMap();


        points.put("Profile","Temperature Influencing");

        String equipStatusPoint = CCUHsApi.getInstance().readDefaultStrVal("point and status and " +
                "message and equipRef == \"" + equipID + "\"");
        double humidity = CCUHsApi.getInstance().readHisValByQuery("point and air and humidity and sensor " +
                "and current and group == \"" + equipID + "\"");
        boolean forceoccupied = CCUHsApi.getInstance().readHisValByQuery("point and autoforceoccupied " +
                "and  group == \"" + equipID + "\"")>0;



        if (equipStatusPoint.length() > 0) {
            points.put("Status", equipStatusPoint);
        } else {
            points.put("Status", "OFF");
        }
        points.put("humidity",String.valueOf(humidity));
        if(forceoccupied){
            points.put("forceoccupied","YES");
        }else
            points.put("forceoccupied","NO");

        return points;
    }
}
