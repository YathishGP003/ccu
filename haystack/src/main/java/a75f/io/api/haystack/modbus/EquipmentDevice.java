package a75f.io.api.haystack.modbus;


import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.annotation.Convert;
import io.objectbox.annotation.Entity;
import io.objectbox.annotation.Id;
import io.objectbox.converter.PropertyConverter;

@Entity
public class EquipmentDevice {

    @Id
    public long id;
    @SerializedName("modbusEquipId (_id)")
    @Expose
    private String modbusEquipIdId;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("description")
    @Expose
    private String description;
    @SerializedName("equipType")
    @Expose
    private String equipType;
    @SerializedName("vendor")
    @Expose
    private String vendor;
    @SerializedName("model")
    @Expose
    @Convert(converter = StringListConverter.class, dbType = String.class)
    private List<String> modelNumbers = null;
    @SerializedName("registers")
    @Expose
    @Convert(converter = EncounterRegisterConverter.class, dbType = String.class)
    private List<Register> registers = null;
    private int slaveId;

    public String zoneRef = null;
    public String floorRef = null;
    public String equipRef = null;
    public boolean isPaired;

    public EquipmentDevice(){

    }
    public String getModbusEquipIdId() {
        return modbusEquipIdId;
    }

    public void setModbusEquipIdId(String modbusEquipIdId) {
        this.modbusEquipIdId = modbusEquipIdId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSlaveId() {
        return slaveId;
    }

    public void setSlaveId(int val) {
        this.slaveId = val;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEquipType() {
        return equipType;
    }

    public void setEquipType(String equipType) {
        this.equipType = equipType;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public List<String> getModelNumbers() {
        return modelNumbers;
    }

    public void setModelNumbers(List<String> modelNumbers) {
        this.modelNumbers = modelNumbers;
    }

    public List<Register> getRegisters() {
        return registers;
    }

    public void setRegisters(List<Register> registers) {
        this.registers = registers;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static class StringListConverter implements PropertyConverter<List<String>, String> {
        @Override
        public List<String> convertToEntityProperty(String databaseValue) {
            if (databaseValue == null)
                return new ArrayList<>();
            try {
                JSONArray array = new JSONArray(databaseValue);
                ArrayList<String> ret = new ArrayList<>();
                for (int i = 0; i < array.length(); i++) {
                    ret.add(array.getString(i));
                }
                return ret;
            } catch (Exception e) {
                return new ArrayList<>();
            }
        }

        @Override
        public String convertToDatabaseValue(List<String> entityProperty) {
            try {
                if (entityProperty == null)
                    return null;
                return new JSONArray(entityProperty).toString();
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static class EncounterRegisterConverter implements PropertyConverter<List<Register>, String> {

        @Override
        public List<Register> convertToEntityProperty(String databaseValue) {
            if (databaseValue == null) {
                return null;
            }

            return new Gson().fromJson(databaseValue, new TypeToken<List<Register>>() {
            }.getType());
        }

        @Override
        public String convertToDatabaseValue(List<Register> registerList) {
            if (registerList == null) {
                return null;
            }

            return new Gson().toJson(registerList);
        }
    }


    public String getZoneRef() {
        return zoneRef;
    }

    public void setZoneRef(String zoneRef) {
        this.zoneRef = zoneRef;
    }

    public String getEquipRef() {
        return equipRef;
    }

    public void setEquipRef(String equipRef) {
        this.equipRef = equipRef;
    }

    public boolean isPaired() {
        return isPaired;
    }

    public void setPaired(boolean paired) {
        this.isPaired = paired;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFloorRef() {
        return floorRef;
    }

    public void setFloorRef(String floorRef) {
        this.floorRef = floorRef;
    }
}
