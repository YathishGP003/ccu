package a75f.io.bo.building;

import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;

/**
 * Created by Yinten on 9/18/2017.
 */

public class SingleStageProfile extends ZoneProfile
{
    @Override
    public short mapCircuit(Output output)
    {
        return 0;
    }
    
    
    @Override
    public void mapSeed(CcuToCmOverUsbDatabaseSeedSnMessage_t seedMessage)
    {
    }
}
