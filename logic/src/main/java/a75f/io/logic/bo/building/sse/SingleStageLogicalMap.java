package a75f.io.logic.bo.building.sse;

import java.util.HashMap;

import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.SingleStageMode;

/**
 * Created by ryant on 10/9/2017.
 */

public class SingleStageLogicalMap
{
    
    private float mRoomTemperature;
    private HashMap<Port, SingleStageMode> mLogicalMap = new HashMap<>();
   
    public HashMap<Port, SingleStageMode> getLogicalMap()
    {
        return mLogicalMap;
    }
    
    public void setLogicalMap(HashMap<Port, SingleStageMode> logicalMap)
    {
        this.mLogicalMap = logicalMap;
    }
    
    public float getRoomTemperature()
    {
        return mRoomTemperature;
    }
    
    public void setRoomTemperature(float roomTemperature)
    {
        this.mRoomTemperature = roomTemperature;
    }
  
}