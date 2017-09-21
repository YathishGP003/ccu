package a75f.io.bo.building;

import android.util.Log;

import a75f.io.bo.building.definitions.SingleStageMode;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.CmToCcuOverUsbSnRegularUpdateMessage_t;

/**
 * Created by Yinten on 9/18/2017.
 */

public class SingleStageProfile extends ZoneProfile
{
    private SingleStageMode mSingleStageMode;
    
    //SSE works independtly just an aggregator for the smart node.
    private short mSmartNodeAddress;
    private float mRoomTemperature;
    
    
    @Override
    public short mapCircuit(Output output)
    {
        return 0;
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
        Log.i("SingleStageProfile", mRoomTemperature + "");
    }
    
    
    public SingleStageMode getSingleStageMode()
    {
        return mSingleStageMode;
    }
    
    
    public void setSingleStageMode(SingleStageMode singleStageMode)
    {
        this.mSingleStageMode = singleStageMode;
    }
}
