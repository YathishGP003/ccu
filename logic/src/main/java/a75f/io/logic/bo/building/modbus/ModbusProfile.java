package a75f.io.logic.bo.building.modbus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.Parameter;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;

public class ModbusProfile extends ZoneProfile {

    ModbusEquip modBusEquip;

    public void addMbEquip(short slaveId, String floorRef, String roomRef, EquipmentDevice equipmentInfo, List<Parameter> configParams, ProfileType profileType) {
        modBusEquip = new ModbusEquip(profileType, slaveId);
        modBusEquip.createEntities(floorRef, roomRef,equipmentInfo, configParams);
        modBusEquip.init(slaveId);
    }

    public void addMbEquip(short slaveId, ProfileType profileType) {
        modBusEquip = new ModbusEquip(profileType, slaveId);
        modBusEquip.init(slaveId);
    }

    public void updateMbEquip(short slaveId,String floorRef, String zoneRef, EquipmentDevice equipmentDevice, List<Parameter> configParams) {
        modBusEquip.updateHaystackPoints(getEquip().getId(),zoneRef,equipmentDevice,configParams);
        modBusEquip.init(slaveId);
    }
    @Override
    public void updateZonePoints() {

    }

    @Override
    public ProfileType getProfileType() {

        return modBusEquip.profileType;
    }

    @Override
    public <T extends BaseProfileConfiguration> T getProfileConfiguration(short address) {
        return null;
    }

    public List<Parameter> getMbProfileConfiguration(short address){
        return modBusEquip.getProfileConfiguration(address);
    }
    @Override
    public Set<Short> getNodeAddresses() {
        return new HashSet<Short>() {{
            add((short) modBusEquip.slaveId);
        }};
    }
    public short getSlaveId(){
        return modBusEquip.slaveId;
    }

    @Override
    public Equip getEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \"" + modBusEquip.slaveId + "\"");
        return new Equip.Builder().setHashMap(equip).build();
    }

    public List<Parameter> getConfiguredParameters(){
        return modBusEquip.configuredParams;
    }
}
