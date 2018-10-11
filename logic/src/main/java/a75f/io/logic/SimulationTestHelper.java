package a75f.io.logic;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by samjithsadasivan on 7/24/18.
 */

public class SimulationTestHelper
{
    /**
     * TODO - Test time stamps are currently hardcoded to 24, July 2018.
     * Currently we have 13 tests running at the start of each hour starting with 12.00am.
     * Current test number should be set from shell via property vav_test
     * */
    
    
    public static long getVavTestStartTime() {
        
        int testNo = SystemProperties.getInt("vav_test",0);
        Log.d("VAV","Start VavTest :"+testNo);
        if (testNo == 0) {
            return 0;
        } else if (testNo == 100) {
            return 100;
        }
    
        SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
        String dateInString = "24-07-2018 "+(testNo-1)+":0:0";
        try
        {
            Date date = sdf.parse(dateInString);
            return date.getTime();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
}
