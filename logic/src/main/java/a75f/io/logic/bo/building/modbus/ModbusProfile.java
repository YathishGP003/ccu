package a75f.io.logic.bo.building.modbus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.Parameter;
import a75f.io.api.haystack.modbus.Register;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;

public class ModbusProfile extends ZoneProfile {

    ModbusEquip modBusEquip;

    public void addMbEquip(short slaveId, String floorRef, String roomRef, EquipmentDevice equipmentDevice,
                           List<Parameter> configParams, ProfileType profileType,
                           List<EquipmentDevice> subEquipmentDevices, String modbusLevel,String modelVersion, String port) {
        modBusEquip = new ModbusEquip(profileType, slaveId);
        String equipRef = modBusEquip.createEntities(floorRef, roomRef, equipmentDevice, configParams,
                null, false, modbusLevel, modelVersion, false,false, false,
                null, false, "", null, null, null, port);
        equipmentDevice.setEquips(null);
        List<EquipmentDevice> intermediateList = new ArrayList<>();
        for(EquipmentDevice subEquipmentDevice : subEquipmentDevices){
            List<Parameter> parameterList = new ArrayList<>();
            if (Objects.nonNull(subEquipmentDevice.getRegisters())) {
                for (Register registerTemp : subEquipmentDevice.getRegisters()) {
                    if (registerTemp.getParameters() != null) {
                        for (Parameter parameterTemp : registerTemp.getParameters()) {
                            parameterTemp.setRegisterNumber(registerTemp.getRegisterNumber());
                            parameterTemp.setRegisterAddress(registerTemp.getRegisterAddress());
                            parameterTemp.setRegisterType(registerTemp.getRegisterType());
                            parameterTemp.setParameterDefinitionType(registerTemp.getParameterDefinitionType());
                            parameterTemp.setMultiplier(registerTemp.getMultiplier());
                            parameterTemp.setWordOrder(registerTemp.getWordOrder());
                            parameterList.add(parameterTemp);
                        }
                    }
                }
            }
            if(subEquipmentDevice.getSlaveId() == 0){
                subEquipmentDevice.setSlaveId(equipmentDevice.getSlaveId());
            }
            boolean isSlaveIdSameAsParent = subEquipmentDevice.getSlaveId() == equipmentDevice.getSlaveId();
            String subEquipRef = modBusEquip.createEntities(floorRef, roomRef, subEquipmentDevice, parameterList,
                    equipRef, isSlaveIdSameAsParent, modbusLevel,null, false,false, false,
                    null, true, "", null, null, null, port);
            subEquipmentDevice.setDeviceEquipRef(subEquipRef);
            intermediateList.add(subEquipmentDevice);
        }
        equipmentDevice.setEquips(intermediateList);
    }

    public void addMbEquip(short slaveId, ProfileType profileType) {
        modBusEquip = new ModbusEquip(profileType, slaveId);
        modBusEquip.init(slaveId);
    }

    public void updateModbusEquip(short slaveId, List<Parameter> configParams){
        modBusEquip.updateHaystackPoints(null, configParams);
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

    @Override
    public Set<Short> getNodeAddresses() {
        return new HashSet<Short>() {{
            add(modBusEquip.slaveId);
        }};
    }
    public short getSlaveId(){
        return modBusEquip.slaveId;
    }

    @Override
    public Equip getEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and modbus and not equipRef and " +
                        "group == \"" + modBusEquip.slaveId + "\"");
        return new Equip.Builder().setHashMap(equip).build();
    }

    public String getPort(){
        HashMap equip = CCUHsApi.getInstance().read("equip and modbus and not equipRef and " +
                "group == \"" + modBusEquip.slaveId + "\"");
        Object port = equip.get("port");
        return port != null ? port.toString() : "";
    }

}
