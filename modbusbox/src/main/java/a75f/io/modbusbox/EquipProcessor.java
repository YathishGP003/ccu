package a75f.io.modbusbox;

import android.content.Context;

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
    Context mContext;
    private BoxStore boxStore;
    private Box<EquipmentDevice> modbusBox;
    ObjectMapper objectMapper;


    EquipProcessor(Context c) {
        mContext = c;
        if(boxStore != null && !boxStore.isClosed())
        {
            boxStore.close();
        }

        boxStore = CCUHsApi.getInstance().tagsDb.getBoxStore();
        modbusBox = boxStore.boxFor(EquipmentDevice.class);
        objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }


    public List<String> getEquipNamesByProfile(String equipType){

        QueryBuilder<EquipmentDevice> configQuery = modbusBox.query();
        configQuery.equal(EquipmentDevice_.equipType, equipType);
        List<String> modbusNames = new ArrayList<>();
        for(EquipmentDevice equipmentDevice : configQuery.build().find()){
            modbusNames.add(equipmentDevice.getName());
        }
        return modbusNames;
    }

    public EquipmentDevice getEquipBySlave(int slaveId){
        QueryBuilder<EquipmentDevice> configQuery = modbusBox.query();
        configQuery.equal(EquipmentDevice_.slaveId, slaveId);
        configQuery.equal(EquipmentDevice_.isPaired, true);
        return configQuery.build().findFirst();
    }


    public EquipmentDevice getEquipByEquipTypeAndName(String equipType, String name){
        QueryBuilder<EquipmentDevice> configQuery = modbusBox.query();
        configQuery.equal(EquipmentDevice_.equipType, equipType);
        configQuery.equal(EquipmentDevice_.name, name);
        return configQuery.build().findFirst();
    }

    public  EquipmentDevice getEquipByVendorAndModel(String vendor, String model){

        QueryBuilder<EquipmentDevice> configQuery = modbusBox.query();
        configQuery.equal(EquipmentDevice_.vendor, vendor);
        configQuery.contains(EquipmentDevice_.modelNumbers, model);
        return configQuery.build().findFirst();
    }


}
