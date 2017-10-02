package a75f.io.logic;

import a75f.io.bo.building.Output;

import static a75f.io.logic.LZoneProfile.getScheduledVal;

/**
 * Created by Yinten on 10/2/2017.
 */

public class LOutput
{
    
    public static boolean isOn(Output output)
    {
        boolean retVal = false;
        if (getScheduledVal(output) == 100)
        {
            retVal = true;
        }
        else
        {
            retVal = false;
        }
        return retVal;
    }
}
