package a75f.io.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

import a75f.io.bo.building.definitions.OutputType;
import a75f.io.bo.building.definitions.Port;
import a75f.io.bo.serial.MessageConstants;
/**
 * Created by Yinten on 9/12/2017.
 */

/***
 * A circuit is an IO from a device like the smart node. Extensions of this class are
 * Input and Output.   Input and Output should be renamed to
 * CircuitInput and CircuitOutput.  This maintains the logical to physical mapping of a circuit IO.
 */
public abstract class Circuit
{
    
    @JsonIgnore
    public    boolean mConfigured;
    protected Port  mPort;
    protected short mAddress;
    protected UUID   uuid  = UUID.randomUUID();
    protected String mName = "";
    protected short   mVal;
    @JsonIgnore
    protected long    mOverrideMillis;
    @JsonIgnore
    protected boolean mOverride = false; //This circuit controls it's own value
    
    
    @JsonIgnore
    public OutputType getOutputType()
    {
        switch (mPort)
        {
            case RELAY_ONE:
                return OutputType.Relay;
            case RELAY_TWO:
                return OutputType.Relay;
            case ANALOG_OUT_ONE:
                return OutputType.Analog;
            case ANALOG_OUT_TWO:
                return OutputType.Analog;
            default:
                return OutputType.Analog;
        }
    }
    
    
    /***
     *  This is used when settings a smart node circuit name, when setting a smart node light
     *  controls message.
     * @return name when communicating via serial to Smart Node
     */
    @JsonIgnore
    public String getCircuitName()
    {
        if (mName.length() > MessageConstants.MAX_LIGHTING_CONTROL_CIRCUIT_LOGICAL_NAME_BYTES)
        {
            return mName.substring(0, 17) + "...";
        }
        else
        {
            return mName;
        }
    }
    
    
    public UUID getUuid()
    {
        return uuid;
    }
    
    
    public void setUuid(UUID uuid)
    {
        this.uuid = uuid;
    }
    
    
    /***
     * This circuit is in manual control mode, zone profile and schedules will be ignored.
     * @return isInOverRide
     */
    @JsonIgnore
    public boolean isOverride()
    {
        return mOverride;
    }
    
    
    @JsonIgnore
    public void setOverride(long overrideTimeMillis, boolean override, short val)
    {
        this.mOverride = override;
        this.mOverrideMillis = overrideTimeMillis;
        this.mVal = val;
    }
    
    
    public void removeOverride()
    {
        this.mOverride = false;
        this.mOverrideMillis = 0;
        this.mVal = 0;
    }
    
    
    public String getName()
    {
        return mName;
    }
    
    
    public void setName(String mName)
    {
        this.mName = mName;
    }
    
    
    public Port getPort()
    {
        return mPort;
    }
    
    
    public void setPort(Port port)
    {
        this.mPort = port;
    }
    
    
    public short getAddress()
    {
        return mAddress;
    }
    
    
    public void setAddress(short address)
    {
        this.mAddress = address;
    }
}
