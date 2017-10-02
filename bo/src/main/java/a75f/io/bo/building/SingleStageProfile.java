package a75f.io.bo.building;

import android.util.Log;

import java.util.HashMap;
import java.util.Set;

import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.building.definitions.SingleStageMode;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;

/**
 * Created by Yinten on 9/18/2017.
 */

public class SingleStageProfile extends ZoneProfile
{


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
    public void mapRegularUpdate(CmToCcuOverUsbSnRegularUpdateMessage_t regularUpdateMessage)
    {
        mRoomTemperature = (float) regularUpdateMessage.update.roomTemperature.get() / 2.0f;
        Log.i("SingleStageProfile",
                "SingleStageProfile RoomTemperature Update: " + mRoomTemperature + "");
    }


//
//    public float getRoomTemperature()
//    {
//        return mRoomTemperature;
//    }


    public void setRoomTemperature(float roomTemperature)
    {
        this.mRoomTemperature = roomTemperature;
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
