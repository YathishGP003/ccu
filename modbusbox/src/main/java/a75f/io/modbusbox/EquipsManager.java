package a75f.io.modbusbox;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
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


    public EquipmentDevice fetchProfileByEquipTypeAndName(String equipType, String name){
        return processor.getEquipByEquipTypeAndName(equipType, name);
    }
    public EquipmentDevice fetchProfileByVendorAndModel(String vendor, String model){
        return processor.getEquipByVendorAndModel(vendor, model);
    }
    public List<String> getAllModbusNamesByEquipType(String equipType){
        return processor.getEquipNamesByProfile(equipType);
    }

}

