package a75f.io.modbusbox;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.ModbusEquipsInfo;

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

    public List<EquipmentDevice> getAllEquipments(){
        return processor.getAllEquips();
    }

    public void saveProfile(EquipmentDevice equipmentDevice){
        processor.saveConfig(equipmentDevice);
    }
    public EquipmentDevice fetchProfile(String equipRef){
        return processor.getConfig(equipRef);
    }
    public List<EquipmentDevice> getAllMbEquips(String zoneRef){
        return processor.getEquipByZoneRef(zoneRef);
    }
    public EquipmentDevice fetchProfileBySlaveId(int slaveId){
        return processor.getEquipBySlave(slaveId);
    }

    public void deleteModules(ArrayList<Short> slaveIds){
        processor.removeDevice(slaveIds);
    }

    public void deleteEquipsByFloor(String floorRef){
        processor.removeDeviceByFloor(floorRef);
    }

    public void deleteEquipByZone(String zoneRef){
        processor.removeDeviceByZone(zoneRef);
    }


    public List<EquipmentDevice> getEnergyMeterSysEquipments(){
        return processor.getAllEMSysEquips();
    }

    public List<EquipmentDevice> getAllBtuMeters(){
        return processor.getAllBTUMeterDevicesEquips();
    }
}

