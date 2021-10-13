package a75f.io.renatus.util;

import a75f.io.renatus.R;

/**
 * Created by Manjunath K on 01-09-2021.
 */
public class RelayUtil {
    /**
     * Relay can be associated with any of the following option
     * @param fanLevel
     * @return drop down list string array
     *  FAN_LOW_SPEED, FAN_MEDIUM_SPEED, FAN_HIGH_SPEED, in relay association Enum respectively 6,7,8
     *  here fan level 0 refers no fan mode are selected
     *  if fan level 21 then (FAN_LOW_SPEED + FAN_MEDIUM_SPEED + FAN_HIGH_SPEED) all the stage of fans are selected
     *      6     -> only low fan is selected
     *      7     -> only medium fan is selected
     *      8     -> only high fan is selected
     *   (6+7+8)  -> all the stages are selected
     *   (6+7)    -> low,medium are selected
     *   (7+8)    -> Medium and High are selected
     *   (6+8)    -> Low and High are selected
     */
    public static int getFanOptionByLevel(int fanLevel){

        if(fanLevel == 0) return R.array.smartstat_fanmode_off;
        if(fanLevel == 6) return R.array.smartstat_fanmode_low;
        if(fanLevel == 7) return R.array.hyperstate_only_medium_fanmode;
        if(fanLevel == 8) return R.array.hyperstate_only_high_fanmode;
        if(fanLevel == 21) return R.array.smartstat_2pfcu_fanmode;
        if(fanLevel == 13) return R.array.smartstat_2pfcu_fanmode_medium;
        if(fanLevel == 15) return R.array.hyperstate_medium_high_fanmode;
        if(fanLevel == 14) return R.array.smartstat_fanmode;
        return R.array.smartstat_fanmode_off;
    }
}
