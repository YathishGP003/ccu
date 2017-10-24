package a75f.io.logic;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;

import a75f.io.bo.building.Circuit;
import a75f.io.bo.building.Day;
import a75f.io.bo.building.Output;
import a75f.io.bo.building.Schedulable;
import a75f.io.bo.building.Schedule;
import a75f.io.bo.building.ZoneProfile;
import a75f.io.bo.building.definitions.MockTime;
import a75f.io.bo.building.definitions.OverrideType;

/**
 * Created by Yinten on 9/10/2017.
 */

class LZoneProfile
{
    private static final String TAG = "ZoneProfile";
    
    //If an output is overiden, but doesn't have schedules, it should fall back onto the zone
    //profile schedules to see if it can release it's override.
    // or if it has schedules, it should use the circuit's logical values.  This behavior can
    //be overrode at a specific profile level.
    public static float resolveZoneProfileLogicalValue(ZoneProfile zoneProfile, Output output)
    {
        if ((isOverride(output) && !output.hasSchedules() && !checkCircuitOverrides(output, zoneProfile)) || output.hasSchedules())
        {
            return resolveLogicalValue(output);
        }
        else
        {
            return resolveLogicalValue(zoneProfile);
        }
    }
    
    /***
     * This circuit is in manual control mode, zone profile and schedules will be ignored.
     * There are different types of overrides.  If the override type is null.
     * a schedule will be used rather than the overridden logical value
     * @return isInOverRide
     */
    @JsonIgnore
    public static boolean isOverride(Schedulable schedulable)
    {
        checkOverrides(schedulable);
        return schedulable.getOverrideType() != OverrideType.NONE;
    }
    
    /*
    If a circuit is overrode & has no schedules, check it against the zone's list of schedules.
    returns if override was removed.
     */
    public static boolean checkCircuitOverrides(Circuit circuit, Schedulable scheduable)
    {
        if (circuit.getOverrideType() == OverrideType.OVERRIDE_TIME_RELEASE_NEXT_SCHEDULE_BOUND && checkBoundCrossed(scheduable.getSchedules(), circuit.getOverrideMillis()))
        {
            circuit.removeOverride();
            return true;
        }
        return false;
    }
    
    public static boolean checkBoundCrossed(ArrayList<Schedule> schedules, long mOverrideMillis)
    {
        if (schedules.size() > 0)
        {
            for (Schedule schedule : schedules)
            {
                if (schedule.crossedBound(mOverrideMillis))
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    protected static boolean checkOverrides(Schedulable schedulable)
    {
        if (schedulable.getOverrideType() == OverrideType.OVERRIDE_TIME_RELEASE_NEXT_SCHEDULE_BOUND && crossedBound(schedulable))
        {
            schedulable.removeOverride();
            return true;
        }
        else if (schedulable.getOverrideType() == OverrideType.RELEASE_TIME && crossedReleaseTime(schedulable))
        {
            schedulable.removeOverride();
            return true;
        }
        return false;
    }
    
    public static float resolveLogicalValue(Schedulable schedulable)
    {
        if (schedulable.getOverrideType() == OverrideType.OVERRIDE_TIME_RELEASE_NEXT_SCHEDULE_BOUND)
        {
            if (crossedBound(schedulable))
            {
                schedulable.removeOverride();
            }
            else
            {
                return schedulable.getLogicalValue();
            }
        }
        else if (schedulable.getOverrideType() == OverrideType.RELEASE_TIME)
        {
            if (crossedReleaseTime(schedulable))
            {
                schedulable.removeOverride();
            }
            else
            {
                return schedulable.getLogicalValue();
            }
        }
        //If any of the overrides were removed or there wasn't one in the first place
        if (schedulable.getOverrideType() == OverrideType.NONE)
        {
            if (schedulable.hasSchedules())
            {
                return getScheduledVal(schedulable);
            }
        }
        //TODO: send alert bad state.
        return 0;
    }
    
    public static short getScheduledVal(Schedulable schedulable)
    {
        for (Schedule schedule : schedulable.getSchedules())
        {
            if (schedule.isInSchedule())
            {
                return getCurrentSchedule(schedule).getVal();
            }
        }
        return 0;
    }
    
    private static boolean crossedBound(Schedulable schedulable)
    {
        //if the smartnode output has schedules, wait for it to cross a bound to remove the
        // override.
        if (schedulable.hasSchedules())
        {
            return checkBoundCrossed(L.resolveSchedules(schedulable), schedulable.getOverrideMillis());
        }
        return false;
    }
    
    //If the override time is a time limit.   As soon as it crosses the curren time release it.
    private static boolean crossedReleaseTime(Schedulable schedulable)
    {
        return schedulable.getOverrideMillis() > MockTime.getInstance().getMockTime();
    }
    
    public static Day getCurrentSchedule(Schedule schedule)
    {
        long mockTime = MockTime.getInstance().getMockTime();
        for (int i = 0; i < schedule.getScheduledIntervals().size(); i++)
        {
            if (schedule.getScheduledIntervals().get(i).contains(mockTime))
            {
                return schedule.getDays().get(i);
            }
        }
        return null;
    }
    
    public static short resolveAnyValue(ZoneProfile zoneProfile)
    {
        Log.i(TAG, "This should be getting the next schedule, not 0");
        if(zoneProfile.hasSchedules())
        {
            Schedule schedule = zoneProfile.getSchedules().get(0);
            Day day = schedule.getDays().get(0);
            Log.i(TAG, "Using Day.getVal(): " + day.getVal());
            return day.getVal();
        }
        
        return 0;
    }
    
    public static boolean isNamedSchedule(Schedulable schedulable)
    {
        return schedulable.getNamedSchedule() != null && !schedulable.getNamedSchedule().equals("");
    }
    public static float resolveZoneProfileLogicalValue(ZoneProfile profile)
    {
        return resolveLogicalValue(profile);
    }
}
