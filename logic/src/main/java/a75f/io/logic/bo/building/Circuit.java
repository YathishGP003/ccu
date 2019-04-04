package a75f.io.logic.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

import a75f.io.logic.bo.building.definitions.OutputType;
import a75f.io.logic.bo.building.definitions.Port;
/**
 * Created by Yinten on 9/12/2017.
 */

/***
 * A circuit is an IO from a device like the smart node. Extensions of this class are
 * Input and Output.   Input and Output should be renamed to
 * CircuitInput and CircuitOutput.  This maintains the logical to physical mapping of a circuit IO.
 */
public abstract class Circuit extends Schedulable
{

    @JsonIgnore
    public    boolean mConfigured;
    protected Port  mPort;
    protected short mAddress;
    protected UUID   uuid  = UUID.randomUUID();
    protected String mName = "";
    
    
    protected short mTestVal;
    
    
    


    @JsonIgnore
    public OutputType getOutputType()
    {
        switch (mPort)
        {
            case RELAY_ONE:
                return OutputType.Relay;
            case RELAY_TWO:
                return OutputType.Relay;
            case RELAY_THREE:
                return OutputType.Relay;
            case RELAY_FOUR:
                return OutputType.Relay;
            case RELAY_FIVE:
                return OutputType.Relay;
            case RELAY_SIX:
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
        if (mName.length() > 20 /*MessageConstants.MAX_LIGHTING_CONTROL_CIRCUIT_LOGICAL_NAME_BYTES*/)
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
    
    @JsonIgnore
    public short getTestVal()
    {
        return mTestVal;
    }
    
    
    @JsonIgnore
    public void setTestVal(short testVal)
    {
        this.mTestVal = testVal;
    }
    
}
