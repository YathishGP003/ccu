package a75f.io.api.haystack.modbus;

import com.google.gson.annotations.SerializedName;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;

@Entity
public class ModbusEquipsInfo {

    @SerializedName("equipmentDevices")
    @Id
    public long id;
    public String zoneRef = null;
    public String equipRef = null;
    public String modbusEquipId = null;
    public int slaveId;

    public int getSlaveId(){
        return slaveId;
    }
    public void setSlaveId(int id){
        this.slaveId = id;
    }

}