package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;

/**
 * Created by Yinten isOn 8/15/2017.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "clazz")
public abstract class ZoneProfile
{
    
    public static final String              TAG        = ZoneProfile.class.getSimpleName();
    @JsonIgnore
    protected           UUID                uuid       = UUID.randomUUID();
    protected           ArrayList<Schedule> mSchedules = new ArrayList<>();
    protected           HashSet<UUID>       mInputs    = new HashSet<>();
    protected           HashSet<UUID>       mOutputs   = new HashSet<>();
    protected short mLogicalValue;
    
    
    public ZoneProfile()
    {
    }
    
    
    @JsonIgnore
    public abstract short mapCircuit(Output output);
    
    
    public HashSet<UUID> getOutputs()
    {
        return mOutputs;
    }
    
    
    public void setOutputs(HashSet<UUID> outputs)
    {
        mOutputs = outputs;
    }
    
    
    public HashSet<UUID> getInputs()
    {
        return mInputs;
    }
    
    
    public void setInputs(HashSet<UUID> inputs)
    {
        mInputs = inputs;
    }
    
    
    public short getLogicalValue()
    {
        return mLogicalValue;
    }
    
    
    public void setLogicalValue(short logicalValue)
    {
        this.mLogicalValue = mLogicalValue;
    }
    
    
    protected short resolveLogicalValue(Output output)
    {
        if (output.isOverride())
        {
            if (crossedBound(output))
            {
                output.removeOverride();
            }
            else
            {
                return output.mVal;
            }
        }
        if (output.hasSchedules())
        {
            return output.getScheduledVal();
        }
        else if (this.hasSchedules())
        {
            return this.getScheduledVal();
        }
        else
        {
            return mLogicalValue;
        }
    }
    
    
    @JsonIgnore
    protected boolean crossedBound(Output output)
    {
        //if the smartnode output has schedules, wait for it to cross a bound to remove the
        // override.
        if (output.hasSchedules())
        {
            return checkBoundCrossed(output.mSchedules, output.mOverrideMillis);
        }
        else
        {
            return checkBoundCrossed(this.mSchedules, output.mOverrideMillis);
        }
    }
    
    
    @JsonIgnore
    public boolean hasSchedules()
    {
        return !mSchedules.isEmpty();
    }
    
    
    @JsonIgnore
    public short getScheduledVal()
    {
        for (Schedule schedule : mSchedules)
        {
            if (schedule.isInSchedule())
            {
                return schedule.getVal();
            }
        }
        return 0;
    }
    
    @JsonIgnore
    protected boolean isInSchedule()
    {
        for (Schedule schedule : mSchedules)
        {
            if (schedule.isInSchedule())
            {
                return true;
            }
        }
        return false;
    }
    
    
    @JsonIgnore
    protected boolean checkBoundCrossed(ArrayList<Schedule> mSchedules, long mOverrideMillis)
    {
        if (hasSchedules())
        {
            for (Schedule schedule : mSchedules)
            {
                if (schedule.crossedBound(mOverrideMillis))
                {
                    return true;
                }
            }
        }
        return false;
    }
    
    
    protected short resolveZoneLogicalValue()
    {
        if (this.hasSchedules())
        {
            return this.getScheduledVal();
        }
        else
        {
            return mLogicalValue;
        }
    }
    
    
    protected short scaleDimmablePercent(short localDimmablePercent, int scale)
    {
        return (short) ((float) scale * ((float) localDimmablePercent / 100.0f));
    }
    
    public abstract void mapControls(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t);
    public abstract void mapSeed(CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage);
    public abstract void mapRegularUpdate(CmToCcuOverUsbSnRegularUpdateMessage_t regularUpdateMessage);
    public abstract ProfileType getProfileType();
}
