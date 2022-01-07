package a75f.io.modbusbox;

import android.content.Context;
import android.util.Log;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.EquipmentDevice_;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.query.QueryBuilder;
public class EquipProcessor
{
    private ArrayList<EquipmentDevice> equipmentDevices;
    private ArrayList<EquipmentDevice> energyMeterDevices;
    /**
     * Hold the Modbus BTU Meter Device List
     */
    private ArrayList<EquipmentDevice> modbusBTUMeterDevices;

    ModbusParser parser;
    Context mContext;
    private BoxStore boxStore;
    private Box<EquipmentDevice> modbusBox;
    ObjectMapper objectMapper;
    private ArrayList<EquipmentDevice> energyMeterSystemDevices;

    EquipProcessor(Context c) {
        mContext = c;
        if(boxStore != null && !boxStore.isClosed())
        {
            boxStore.close();
        }

        boxStore = CCUHsApi.getInstance().tagsDb.getBoxStore();
        modbusBox = boxStore.boxFor(EquipmentDevice.class);

        parser = new ModbusParser();

        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        equipmentDevices = parser.parseAllEquips(c);
        energyMeterDevices = parser.parseEneryMeterEquips(c);
        energyMeterSystemDevices = parser.parseEneryMeterSystemEquips(c);
        modbusBTUMeterDevices = parser.readBTUMeterDeviceDetails(c);
        readExternalJsonData();

        for(EquipmentDevice equipmentDevice:equipmentDevices){
            addEquips(equipmentDevice);
        }
        for(EquipmentDevice equipmentDevice:energyMeterSystemDevices){
                       addEquips(equipmentDevice);
        }
        for(EquipmentDevice equipmentDevice:energyMeterDevices){
            addEquips(equipmentDevice);
        }

        for(EquipmentDevice equipmentDevice:modbusBTUMeterDevices){
            addEquips(equipmentDevice);
        }

    }


    public void readExternalJsonData(){
        // Read external Json data

        parser.readExternalJSONFromDir("/sdcard/ccu/modbus", equipmentDevices, modbusBTUMeterDevices,
                energyMeterSystemDevices, energyMeterDevices);
        Log.i("CCU_MODBUS", "Modbus external JSON file scan completed");

    }

    public void addEquips(EquipmentDevice equipmentDevice) {
        if (getMbEquip(equipmentDevice.getModbusEquipIdId()) == null) {
            modbusBox.put(equipmentDevice);
        }
    }

    public List<EquipmentDevice> getAllEMEquips() {
        QueryBuilder<EquipmentDevice> mbQuery = modbusBox.query();
        mbQuery.equal(EquipmentDevice_.isPaired, false);
        mbQuery.equal(EquipmentDevice_.equipType, "EMR_ZONE");
        return mbQuery.build().find();
    }
    public List<EquipmentDevice> getAllEMSysEquips(){
        QueryBuilder<EquipmentDevice> mbQuery = modbusBox.query();
        mbQuery.equal(EquipmentDevice_.equipType,"EMR");
        mbQuery.equal(EquipmentDevice_.isPaired,false);
        return mbQuery.build().find();
    }

    public List<EquipmentDevice> getAllEquips(){
        QueryBuilder<EquipmentDevice> mbQuery = modbusBox.query();
        mbQuery.equal(EquipmentDevice_.isPaired,false);
        mbQuery.notEqual(EquipmentDevice_.equipType,"EMR");
        mbQuery.notEqual(EquipmentDevice_.equipType,"BTU");
        mbQuery.notEqual(EquipmentDevice_.equipType,"EMR_ZONE");
        return mbQuery.build().find();
    }

    public EquipmentDevice getMbEquip(String equipId){
        QueryBuilder<EquipmentDevice> mbQuery = modbusBox.query();
        mbQuery.equal(EquipmentDevice_.modbusEquipIdId,equipId);
        return mbQuery.build().findFirst();
    }

    public void saveConfig(EquipmentDevice profileConfig){
        modbusBox.put(profileConfig);
    }
    public void removeDevice(ArrayList<Short> slaveIds){
        for (Short slaveId: slaveIds){
            EquipmentDevice device = getEquipBySlave(slaveId);
            if(device != null){
                modbusBox.remove(device.id);
            }
        }
    }

    public void removeDeviceByFloor(String floorRef){
        List<EquipmentDevice> device = getEquipByFloorRef(floorRef);
        for (EquipmentDevice eq : device){
            modbusBox.remove(eq.id);
        }
    }

    public void removeDeviceByZone(String zoneRef){
        List<EquipmentDevice> device = getEquipByZoneRef(zoneRef);
        for (EquipmentDevice eq : device){
            modbusBox.remove(eq.id);
        }
    }

    public EquipmentDevice getConfig(String equipRef){
        QueryBuilder<EquipmentDevice> configQuery = modbusBox.query();
        configQuery.equal(EquipmentDevice_.equipRef, equipRef);
        return configQuery.build().findFirst();
    }
    public EquipmentDevice getEquipBySlave(int slaveId){
        QueryBuilder<EquipmentDevice> configQuery = modbusBox.query();
        configQuery.equal(EquipmentDevice_.slaveId, slaveId);
        configQuery.equal(EquipmentDevice_.isPaired, true);
        return configQuery.build().findFirst();
    }

    public List<EquipmentDevice> getEquipByFloorRef(String floorRef){
        QueryBuilder<EquipmentDevice> configQuery = modbusBox.query();
        configQuery.equal(EquipmentDevice_.floorRef, floorRef);
        configQuery.equal(EquipmentDevice_.isPaired, true);
        return configQuery.build().find();
    }
    
   public List<EquipmentDevice> getEquipByZoneRef(String zoneRef){
       QueryBuilder<EquipmentDevice> configQuery = modbusBox.query();
       configQuery.equal(EquipmentDevice_.zoneRef, zoneRef);
       configQuery.equal(EquipmentDevice_.isPaired, true);
       return configQuery.build().find();
   }

    public List<EquipmentDevice> getAllBTUMeterDevicesEquips(){
        QueryBuilder<EquipmentDevice> configQuery = modbusBox.query();
        configQuery.equal(EquipmentDevice_.isPaired, false);
        configQuery.equal(EquipmentDevice_.equipType, "BTU");
        return  configQuery.build().find();
    }

    private static boolean isFirstTime = true;
    public EquipmentDevice getEquipByEquipTypeAndName(String equipType, String name){
        if(isFirstTime) {
            for (EquipmentDevice equipmentDevice : equipmentDevices) {
                addEquips(equipmentDevice);
            }
            for (EquipmentDevice equipmentDevice : energyMeterSystemDevices) {
                addEquips(equipmentDevice);
            }
            for (EquipmentDevice equipmentDevice : energyMeterDevices) {
                addEquips(equipmentDevice);
            }

            for (EquipmentDevice equipmentDevice : modbusBTUMeterDevices) {
                addEquips(equipmentDevice);
            }
            isFirstTime = false;
        }
        QueryBuilder<EquipmentDevice> configQuery = modbusBox.query();
        configQuery.equal(EquipmentDevice_.equipType, equipType);
        configQuery.equal(EquipmentDevice_.name, name);
        return configQuery.build().findFirst();
    }

}
