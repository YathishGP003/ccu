package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.json.PackageVersion;

import java.util.ArrayList;

import a75f.io.bo.building.definitions.MockTime;
import a75f.io.bo.building.definitions.OverrideType;

/**
 * Created by Yinten on 9/21/2017.
 */

public class Schedulable {
    
    public ArrayList<Schedule> getSchedules()
    {
        return mSchedules;
    }
    public void setSchedules(ArrayList<Schedule> mSchedules)
    {
        this.mSchedules = mSchedules;
    }
    protected ArrayList<Schedule> mSchedules = new ArrayList<>();
    protected short mLogicalValue;
    protected OverrideType mOverrideType = OverrideType.NONE;

    public void addSchedule(Schedule schedule)
    {
        mSchedules.add(schedule);
    }

    /***
     * This circuit is in manual control mode, zone profile and schedules will be ignored.
     * There are different types of overrides.  If the override type is null.
     * a schedule will be used rather than the overridden logical value
     * @return isInOverRide
     */
    @JsonIgnore
    public boolean isOverride() {
        checkOverrides();
        return mOverrideType != OverrideType.NONE;
    }


    /*
    If a circuit is overrode & has no schedules, check it against the zone's list of schedules.
    returns if override was removed.
     */
    public boolean checkOverrides(ArrayList<Schedule> schedules) {
        if (this.mOverrideType == OverrideType.OVERRIDE_TIME_RELEASE_NEXT_SCHEDULE_BOUND && this.checkBoundCrossed(schedules, mOverrideMillis)) {
            removeOverride();
            return true;
        }
        return false;
    }


    @JsonIgnore
    public void setOverride(long overrideTimeMillis, OverrideType overrideType, short val) {
        this.mOverrideType = overrideType;
        this.mOverrideMillis = overrideTimeMillis;
        this.mLogicalValue = val;
    }


    public void removeOverride() {
        this.mOverrideType = OverrideType.NONE;
        this.mOverrideMillis = 0;
        this.mLogicalValue = 0;
    }


    /***
     * This value is used differently depending on the OverrideType
     * OVERRIDE_TIME_RELEASE_NEXT_SCHEDULE_BOUND means that the next time a scheduled time wants to change the value, release the override.
     * RELEASE_TIME means the time that this override should be released.   An Example would be 2 hours in the future.
     *
     * Returns if the override was released.
     */
    @JsonIgnore
    protected long mOverrideMillis;

    protected boolean checkOverrides() {
        if (this.mOverrideType == OverrideType.OVERRIDE_TIME_RELEASE_NEXT_SCHEDULE_BOUND && this.crossedBound()) {
            removeOverride();
            return true;

        } else if (this.mOverrideType == OverrideType.RELEASE_TIME && this.crossedReleaseTime()) {
            removeOverride();
            return true;
        }

        return false;
    }

    protected short resolveLogicalValue() {
        if (this.mOverrideType ==
                OverrideType.OVERRIDE_TIME_RELEASE_NEXT_SCHEDULE_BOUND) {
            if (this.crossedBound()) {
                removeOverride();
            } else {
                return mLogicalValue;
            }
        } else if (this.mOverrideType == OverrideType.RELEASE_TIME) {
            if (this.crossedReleaseTime()) {
                removeOverride();
            } else {
                return mLogicalValue;
            }
        }

        //If any of the overrides were removed or there wasn't one in the first place
        if (this.mOverrideType == OverrideType.NONE) {
            if (this.hasSchedules()) {
                return this.getScheduledVal();
            }
        }
        //TODO: send alert bad state.
        return 0;
    }

    @JsonIgnore
    public boolean checkBoundCrossed(ArrayList<Schedule> schedules, long mOverrideMillis) {
        if (schedules.size() > 0) {
            for (Schedule schedule : schedules) {
                if (schedule.crossedBound(mOverrideMillis)) {
                    return true;
                }
            }
        }
        return false;
    }

    @JsonIgnore
    protected boolean isInSchedule() {
        for (Schedule schedule : mSchedules) {
            if (schedule.isInSchedule()) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnore
    public short getScheduledVal() {
        for (Schedule schedule : mSchedules) {
            if (schedule.isInSchedule()) {
                return schedule.getVal();
            }
        }
        return 0;
    }


    @JsonIgnore
    public boolean hasSchedules() {
        return !mSchedules.isEmpty();
    }


    @JsonIgnore
    protected boolean crossedBound() {
        //if the smartnode output has schedules, wait for it to cross a bound to remove the
        // override.
        if (hasSchedules()) {
            return checkBoundCrossed(mSchedules, mOverrideMillis);
        }

        return false;
    }

    //If the override time is a time limit.   As soon as it crosses the curren time release it.
    private boolean crossedReleaseTime() {
        return mOverrideMillis > MockTime.getInstance().getMockTime();
    }

    public short getLogicalValue() {
        return mLogicalValue;
    }

    public void setLogicalValue(short logicalValue) {
        this.mLogicalValue = mLogicalValue;
    }
}
