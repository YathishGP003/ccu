package a75f.io.api.haystack;

import java.util.Objects;

public class Occupied {

    private boolean mOccupied;
    private Object mValue;
    private Double mCoolingVal;
    private Double mHeatingVal;
    private long mMillisecondsUntilNextChange;
    private Schedule.Days mCurrentlyOccupiedScheduleDay;
    private Schedule.Days mNextOccupiedScheduleDay;
    private Double mHeatingDeadband;
    private Double mCoolingDeadband;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Occupied occupied = (Occupied) o;
        return mOccupied == occupied.mOccupied &&
                mMillisecondsUntilNextChange == occupied.mMillisecondsUntilNextChange &&
                Objects.equals(mCoolingDeadband, occupied.mCoolingDeadband) &&
                Objects.equals(mHeatingDeadband, occupied.mHeatingDeadband) &&
                Objects.equals(mValue, occupied.mValue) &&
                Objects.equals(mCoolingVal, occupied.mCoolingVal) &&
                Objects.equals(mHeatingVal, occupied.mHeatingVal) &&
                Objects.equals(mCurrentlyOccupiedScheduleDay, occupied.mCurrentlyOccupiedScheduleDay) &&
                Objects.equals(mNextOccupiedScheduleDay, occupied.mNextOccupiedScheduleDay);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mOccupied, mValue, mCoolingVal, mHeatingVal, mMillisecondsUntilNextChange, mCurrentlyOccupiedScheduleDay, mNextOccupiedScheduleDay, mCoolingDeadband, mHeatingDeadband);
    }

    public boolean isOccupied() {
        return mOccupied;
    }

    public void setOccupied(boolean occupied) {
        this.mOccupied = occupied;
    }

    public Object getValue() {
        return mValue;
    }

    public void setValue(Object value) {
        this.mValue = value;
    }

    public Double getHeatingVal() {
        return mHeatingVal;
    }

    public void setHeatingVal(Double heatingVal) {
        this.mHeatingVal = heatingVal;
    }

    public Double getCoolingVal() {
        return mCoolingVal;
    }

    public void setCoolingVal(Double coolingVal) {
        this.mCoolingVal = coolingVal;
    }

    public long getMillisecondsUntilNextChange() {
        return mMillisecondsUntilNextChange;
    }

    public void setMillisecondsUntilNextChange(long millisecondsUntilNextChange) {
        this.mMillisecondsUntilNextChange = millisecondsUntilNextChange;
    }

    public void setCurrentlyOccupiedSchedule(Schedule.Days days) {
        mCurrentlyOccupiedScheduleDay = days;
    }

    public Schedule.Days getCurrentlyOccupiedSchedule() {
        return mCurrentlyOccupiedScheduleDay;
    }

    public void setNextOccupiedSchedule(Schedule.Days days) {
        mNextOccupiedScheduleDay = days;
    }

    public Schedule.Days getNextOccupiedSchedule() {
        return mNextOccupiedScheduleDay;
    }

    public void setHeatingDeadBand(double heatingDeadBand)
    {
        mHeatingDeadband = heatingDeadBand;
    }

    public void setCoolingDeadBand(double coolingDeadband)
    {
        mCoolingDeadband = coolingDeadband;
    }

    public double getHeatingDeadBand() { return mHeatingDeadband; }
    public double getCoolingDeadBand() { return mCoolingDeadband; }
}
