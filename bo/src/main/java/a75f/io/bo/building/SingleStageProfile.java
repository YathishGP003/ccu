package a75f.io.bo.building;

import android.util.Log;

import java.util.HashMap;
import java.util.Set;

import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.building.definitions.SingleStageMode;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;

/**
 * Created by Yinten on 9/18/2017.
 */

public class SingleStageProfile extends ZoneProfile
{

    private static final short TODO = 0;
    //There can be multiple single stage profiles in a zone.
    //We need to keep track of if they are heating, cooling, or non-functional.
    HashMap<Short, SingleStageMode> mSingleStageModeHashMap = new HashMap<>();

    private float mRoomTemperature;
    //SSE works independtly just an aggregator for the smart node.
    //Relay 1 is heating or cooling
    //Relay 2 is fan
    //If the mode is occupied, fan is always on.
    //If the heating or cooling is on, fan must be on.
    //Multiple smart nodes can be in the zone.  The smart node should be assigned to heating or
    // cooling.


    @Override
    public short mapCircuit(Output output)
    {
        short temperature = resolveLogicalValue(output);
        //The smartnode circuit is in override mode, check to see if a schedule hasn't crossed a
        // bound.  If a schedule did cross a bound remove the override and continue.
        switch (output.getOutputType())
        {
            case Relay:
                switch (output.mOutputRelayActuatorType)
                {
                    case NormallyClose:
                        switch (output.getPort())
                        {
                            case RELAY_ONE:
                                //this is the compressor
                                return TODO;
                            case RELAY_TWO:
                                //This is the fan port
                                return TODO;
                        }
                        ///Defaults to normally open
                    case NormallyOpen:
                        //Normal one
                        switch (output.getPort())
                        {
                            case RELAY_ONE:
                                //This is the compressor port
                                return TODO;
                            case RELAY_TWO:
                                //This is the fan port
                                return TODO;
                        }
                        return TODO;
                }
                break;
        }
        return (short) 0;
    }


    @Override
    public void mapControls(CcuToCmOverUsbSnControlsMessage_t controlsMessage_t)
    {
        controlsMessage_t.controls.setTemperature.set((short) (getDesiredTemperature() * 2));
    }


    @Override
    public void mapSeed(CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage)
    {
        seedMessage.settings.profileBitmap.singleStageEquipment.set((short) 1);

    }


    @Override
    public void mapRegularUpdate(CmToCcuOverUsbSnRegularUpdateMessage_t regularUpdateMessage)
    {
        mRoomTemperature = (float) regularUpdateMessage.update.roomTemperature.get() / 2.0f;
        Log.i("SingleStageProfile",
                "SingleStageProfile RoomTemperature Update: " + mRoomTemperature + "");
    }


    public boolean isOccupancyMode()
    {
        return isInSchedule();
    }

    public float getRoomTemperature()
    {
        return mRoomTemperature;
    }


    public void setRoomTemperature(float roomTemperature)
    {
        this.mRoomTemperature = roomTemperature;
    }


    public float getDesiredTemperature()
    {
        return resolveLogicalValue();
    }


    public int getHeatingDeadband()
    {
        //TODO: tuning
        return 1; //AlgoTuningParameters.SSE_Heating_Deadband;
    }


    public int getCoolingDeadband()
    {
        //TODO: tuning
        return 1; ///return AlgoTuningParameters.SSE_Cooling_Deadband;
    }


    public HashMap<Short, SingleStageMode> getSingleStageModeHashMap()
    {
        return mSingleStageModeHashMap;
    }


    public void setSingleStageModeHashMap(HashMap<Short, SingleStageMode> singleStageModeHashMap)
    {
        mSingleStageModeHashMap = singleStageModeHashMap;
    }


    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.SSE;
    }
    
    
    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return null;
    }
    
    
    @Override
    public Set<Short> getNodeAddresses()
    {
        return null;
    }
    
    
    @Override
    public void removeProfileConfiguration(Short selectedModule)
    {
    }
}
