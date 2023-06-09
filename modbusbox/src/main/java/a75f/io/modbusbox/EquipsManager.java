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

    public EquipProcessor getProcessor() {
        return processor;
    }

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
    /*public EquipmentDevice fetchProfile(String equipRef){
        return processor.getConfig(equipRef);
    }*/
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


    public List<EquipmentDevice> getEnergyMeterEquipments() {
        return processor.getAllEMEquips();
    }

    public List<EquipmentDevice> getEnergyMeterSysEquipments(){
        return processor.getAllEMSysEquips();
    }

    public List<EquipmentDevice> getAllBtuMeters(){
        return processor.getAllBTUMeterDevicesEquips();
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

    public List<EquipmentDevice> getModbusSubEquip(Equip equip, Point point) {
        List<EquipmentDevice> modbusSubEquipList = new ArrayList<>();
        HashMap<Object, Object> parentEquipHashMap = CCUHsApi.getInstance().readMapById(equip.getEquipRef());
        Equip parentEquip = new Equip.Builder().setHashMap(parentEquipHashMap).build();
        EquipmentDevice modbusDevice = EquipsManager.getInstance().fetchProfileBySlaveId(Short.parseShort(parentEquip.getGroup()));
        for (EquipmentDevice modbusSubEquip : modbusDevice.getEquips()) {
            if (Integer.parseInt(point.getGroup()) == modbusSubEquip.getSlaveId()) {
                modbusSubEquipList.add(modbusSubEquip);
            }
        }
        return modbusSubEquipList;
    }

}

