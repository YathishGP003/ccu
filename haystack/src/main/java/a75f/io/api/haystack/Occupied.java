package a75f.io.api.haystack;

import java.util.Calendar;
import java.util.Objects;

public class Occupied {

    private boolean mOccupied;
    private boolean mPrecondition;
    private boolean mOccupancySensing;
    private boolean mForcedOccupied;
    private boolean       mSystemZone;
    private Object        mValue;
    private Double        mCoolingVal;
    private Double        mHeatingVal;
    private long          mMillisecondsUntilNextChange;
    private Schedule.Days mCurrentlyOccupiedScheduleDay;
    private Schedule.Days mNextOccupiedScheduleDay;
    private Double        mHeatingDeadband;
    private Double        mCoolingDeadband;
    private Schedule      vacation;
    private double        unoccupiedZoneSetback;
    private long          temporaryHoldExpiry;
    
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
                Objects.equals(mNextOccupiedScheduleDay, occupied.mNextOccupiedScheduleDay) && Objects.equals(occupied.vacation, vacation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mOccupied, mValue, mCoolingVal, mHeatingVal, mMillisecondsUntilNextChange, mCurrentlyOccupiedScheduleDay, mNextOccupiedScheduleDay, mCoolingDeadband, mHeatingDeadband, vacation);
    }

    public boolean isOccupied() {
        return mOccupied;
    }

    public void setOccupied(boolean occupied) {
        this.mOccupied = occupied;
    }

    public boolean isPreconditioning(){
        return mPrecondition;
    }
    public void setPreconditioning(boolean isPrecondition){
        this.mPrecondition = isPrecondition;
    }

    public boolean isForcedOccupied(){
        return mForcedOccupied;
    }
    public void setForcedOccupied(boolean value){
        this.mForcedOccupied = value;
    }
    
    public boolean isSystemZone()
    {
        return mSystemZone;
    }
    public void setSystemZone(boolean mSystemZone)
    {
        this.mSystemZone = mSystemZone;
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

    public void setOccupancySensed(boolean isOccupancySensed){
        this.mOccupancySensing = isOccupancySensed;
    }
    public boolean isOccupancySensed(){
        return mOccupancySensing;
    }
    public void setVacation(Schedule vacation)
    {
        this.vacation = vacation;
    }


    public Schedule getVacation()
    {
        return vacation;
    }
    
    public double getUnoccupiedZoneSetback()
    {
        return unoccupiedZoneSetback;
    }
    public void setUnoccupiedZoneSetback(double unoccupiedZoneSetback)
    {
        this.unoccupiedZoneSetback = unoccupiedZoneSetback;
    }
    public long getTemporaryHoldExpiry()
    {
        return temporaryHoldExpiry;
    }
    public void setTemporaryHoldExpiry(long temporaryHoldExpiry)
    {
        this.temporaryHoldExpiry = temporaryHoldExpiry;
    }

    public int getCurrentOccupiedSlot(){
        if(mCurrentlyOccupiedScheduleDay != null) {
            Calendar now = Calendar.getInstance();
            now.set(Calendar.HOUR_OF_DAY,mCurrentlyOccupiedScheduleDay.getSthh());
            now.set(Calendar.MINUTE,mCurrentlyOccupiedScheduleDay.getStmm());
            now.set(Calendar.SECOND,30);
            return (now.get(Calendar.HOUR_OF_DAY) * 4) + (now.get(Calendar.MINUTE) / 15);
        }else if(mNextOccupiedScheduleDay != null){
            Calendar nextCal = Calendar.getInstance();
            nextCal.set(Calendar.HOUR_OF_DAY,mNextOccupiedScheduleDay.getSthh());
            nextCal.set(Calendar.MINUTE,mNextOccupiedScheduleDay.getStmm());
            nextCal.set(Calendar.SECOND,30);
            return (nextCal.get(Calendar.HOUR_OF_DAY) * 4) + (nextCal.get(Calendar.MINUTE) / 15);
        }else{
            //Default 9 AM
            return 36;
        }
    }
    public int getCurrentUnOccupiedSlot(){
        if(mCurrentlyOccupiedScheduleDay != null) {
            Calendar now = Calendar.getInstance();
            now.set(Calendar.HOUR_OF_DAY,mCurrentlyOccupiedScheduleDay.getEthh());
            now.set(Calendar.MINUTE,mCurrentlyOccupiedScheduleDay.getEtmm());
            now.set(Calendar.SECOND,30);
            return (now.get(Calendar.HOUR_OF_DAY) * 4) + (now.get(Calendar.MINUTE) / 15);
        }else if(mNextOccupiedScheduleDay != null){
            Calendar nextCal = Calendar.getInstance();
            nextCal.set(Calendar.HOUR_OF_DAY,mNextOccupiedScheduleDay.getEthh());
            nextCal.set(Calendar.MINUTE,mNextOccupiedScheduleDay.getEtmm());
            nextCal.set(Calendar.SECOND,30);
            return (nextCal.get(Calendar.HOUR_OF_DAY) * 4) + (nextCal.get(Calendar.MINUTE) / 15);
        }else{
            //Default 6 PM
            return 72;
        }
    }
}
