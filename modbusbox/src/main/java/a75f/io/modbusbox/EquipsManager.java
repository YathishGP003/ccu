package a75f.io.modbusbox;

import android.content.Context;

import a75f.io.api.haystack.modbus.EquipmentDevice;

public class EquipsManager
{
    private Context mContext;
    private static EquipsManager mInstance;
    EquipProcessor processor;

    public EquipsManager(Context c) {
        if (processor == null){
            processor = new EquipProcessor(c);
        }
    }

    public void setApplicationContext(Context mApplicationContext) {
        if (this.mContext == null) {
            this.mContext = mApplicationContext;
        }
    }

    public static EquipsManager getInstance(Context c) {
        if (mInstance == null) {
            mInstance = new EquipsManager(c);
        }
        return mInstance;
    }

    public static EquipsManager getInstance()
    {
        if (mInstance == null)
        {
            throw new IllegalStateException("No instance found");
        }
        return mInstance;
    }
    
    public void init(Context c) {
        mContext = c;
    }


    public EquipmentDevice fetchProfileBySlaveId(int slaveId){
        return processor.getEquipBySlave(slaveId);
    }

}

