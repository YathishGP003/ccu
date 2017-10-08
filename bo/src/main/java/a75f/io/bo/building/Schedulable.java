package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;

import a75f.io.bo.building.definitions.OverrideType;
import a75f.io.bo.building.definitions.ScheduleMode;

/**
 * Created by Yinten on 9/21/2017.
 */

public class Schedulable
{
    
    protected String              mNamedSchedule = "";
    protected ArrayList<Schedule> mSchedules     = new ArrayList<>();
    protected short mLogicalValue;
    protected OverrideType mOverrideType = OverrideType.NONE;
    protected ScheduleMode mScheduleMode = ScheduleMode.ZoneSchedule;
    
    /***
     * This value is used differently depending on the OverrideType
     * OVERRIDE_TIME_RELEASE_NEXT_SCHEDULE_BOUND means that the next time a scheduled time wants to change the value, release the override.
     * RELEASE_TIME means the time that this override should be released.   An Example would be 2 hours in the future.
     *
     * Returns if the override was released.
     */
    @JsonIgnore
    protected long mOverrideMillis;
    
    
    @JsonIgnore
    public boolean hasSchedules()
    {
        return !mSchedules.isEmpty() || (mNamedSchedule != null && !mNamedSchedule.equals(""));
    }
    
    

    
    
    public void setNamedSchedule(String namedSchedule)
    {
        this.setScheduleMode(ScheduleMode.NamedSchedule);
        this.mNamedSchedule = namedSchedule;
    }
    
    
    public short getLogicalValue()
    {
        return mLogicalValue;
    }
    
    
    public void setLogicalValue(short logicalValue)
    {
        this.mLogicalValue = mLogicalValue;
    }
    
    
    public void addSchedules(ArrayList<Schedule> schedules, ScheduleMode circuitSchedule)
    {
        this.mSchedules = schedules;
        this.mScheduleMode = circuitSchedule;
    }
    
    
    public ArrayList<Schedule> getSchedules()
    {
        return mSchedules;
    }
    
    
    public void setSchedules(ArrayList<Schedule> schedules)
    {
        this.mSchedules = schedules;
    }
    
    
    public long getOverrideMillis()
    {
        return mOverrideMillis;
    }
    
    
    public void setOverrideMillis(long overrideMillis)
    {
        this.mOverrideMillis = overrideMillis;
    }
    
    
    public ScheduleMode getScheduleMode()
    {
        return mScheduleMode;
    }
    
    
    public void setScheduleMode(ScheduleMode scheduleMode)
    {
        this.mScheduleMode = scheduleMode;
    }
    
    
    public OverrideType getOverrideType()
    {
        return mOverrideType;
    }
    
    
    public void setOverrideType(OverrideType mOverrideType)
    {
        this.mOverrideType = mOverrideType;
    }
    
    
    @JsonIgnore
    public void setOverride(long overrideTimeMillis, OverrideType overrideType, short val)
    {
        this.mOverrideType = overrideType;
        this.mOverrideMillis = overrideTimeMillis;
        this.mLogicalValue = val;
    }
    
    
    public void removeOverride()
    {
        this.mOverrideType = OverrideType.NONE;
        this.mOverrideMillis = 0;
        this.mLogicalValue = 0;
    }
    
    
    public String getNamedSchedule()
    {
        return mNamedSchedule;
    }
}
