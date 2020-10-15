package a75f.io.api.haystack.modbus;

import com.google.gson.annotations.SerializedName;

import a75f.io.api.haystack.Equip;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.annotation.Transient;

@Entity
public class ModbusEquipsInfo {

    @SerializedName("equipmentDevices")
    @Id
    public long id;
    @Transient
    public EquipmentDevice equipmentDevices = null;
    public String zoneRef = null;
    public String equipRef = null;
    public String modbusEquipId = null;
    public int slaveId;

    public ModbusEquipsInfo(){

    }
    public ModbusEquipsInfo(String zoneRef, String equipRef, EquipmentDevice equipmentDevices) {
        this.zoneRef = zoneRef;
        this.equipRef = equipRef;
        this.equipmentDevices = equipmentDevices;
    }
    public ModbusEquipsInfo(String modbusEquipId, EquipmentDevice equipmentDevices) {
        this.modbusEquipId = modbusEquipId;
        this.equipmentDevices = equipmentDevices;
    }
    public ModbusEquipsInfo(EquipmentDevice equipmentDevices) {
        this.equipmentDevices = equipmentDevices;
    }
    public EquipmentDevice getEquipmentDevices() {
        return equipmentDevices;
    }

    public void setEquipmentDevice(EquipmentDevice equipmentDevices) {
        this.equipmentDevices = equipmentDevices;
    }

    public String getModbusZone() {
        return zoneRef;
    }

    public void setModbusZone(String zoneRef) {
        this.zoneRef = zoneRef;
    }

    public String getModbusEquipRef() {
        return equipRef;
    }

    public void setModbusEquipRef(String equipRef) {
        this.equipRef = equipRef;
    }

    public String getModbusEquipId() {
        return modbusEquipId;
    }

    public void setModbusEquipId(String equipRef) {
        this.modbusEquipId = equipRef;
    }

    public int getSlaveId(){
        return slaveId;
    }
    public void setSlaveId(int id){
        this.slaveId = id;
    }

}