package a75f.io.logic;

import a75f.io.logic.bo.building.Output;

import static a75f.io.logic.LZoneProfile.getScheduledVal;

/**
 * Created by Yinten on 10/2/2017.
 */

public class LOutput
{
    
    public static boolean isOn(Output output)
    {
        return getScheduledVal(output) != 0;
    }
}
