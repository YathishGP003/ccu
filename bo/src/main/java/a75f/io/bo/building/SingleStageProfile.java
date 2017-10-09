package a75f.io.bo.building;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Set;

import a75f.io.bo.building.definitions.ProfileType;
import a75f.io.bo.building.definitions.RoomDataInterface;
import a75f.io.bo.building.definitions.SingleStageMode;
import a75f.io.bo.building.sse.SingleStageLogicalMap;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;

/**
 * Created by Yinten on 9/18/2017.
 */

public class SingleStageProfile extends ZoneProfile
{
    
    //There can be multiple single stage profiles in a zone.
    //We need to keep track of if they are heating, cooling, or non-functional.
    HashMap<Short, SingleStageLogicalMap> mLogicalMap = new HashMap<>();
    
    //MARK
    @JsonIgnore
    private RoomDataInterface mInterface;
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
        float roomTemperature = (float) regularUpdateMessage.update.roomTemperature.get() / 2.0f;
        mLogicalMap.get(Short.valueOf((short) regularUpdateMessage.update.smartNodeAddress.get())).setRoomTemperature(roomTemperature);
        Log.i("SingleStageProfile", "SingleStageProfile RoomTemperature Update: " + roomTemperature + "");
    }
    
    @JsonIgnore
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.SSE;
    }
    
    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return mProfileConfiguration.get(address);
    }
    
    public HashMap<Short, SingleStageLogicalMap> getLogicalMap()
    {
        return mLogicalMap;
    }
    public void setLogicalMap(HashMap<Short, SingleStageLogicalMap> logicalMap)
    {
        this.mLogicalMap = logicalMap;
    }
    
    //MARK
    public double getCMCurrentTemp(boolean b)
    {
        return 75;
    }
    
    //MARK
    @JsonIgnore
    public void setZoneProfileInterface(RoomDataInterface zoneProfileInterface)
    {
        mInterface = zoneProfileInterface;
    }
    
    @JsonIgnore
    //MARK
    public double getDisplayCurrentTemp()
    {
        return 74;
    }
    
    //MARK
    @JsonIgnore
    public double getActualDesiredTemp()
    {
        return 78;
    }
}
