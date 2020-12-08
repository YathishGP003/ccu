package a75f.io.device.modbus;


import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Zone;
import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.Register;
import a75f.io.device.DeviceNetwork;
import a75f.io.device.mesh.LSerial;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.modbusbox.EquipsManager;

public class ModbusNetwork extends DeviceNetwork
{
    public static final int READ_REGISTER_ONE = 1;
    public static final int READ_REGISTER_TWO = 2;
    public static final int READ_REGISTER_FOUR = 4;
    
    @Override
    public void sendMessage() {
        
        if (!LSerial.getInstance().isModbusConnected()) {
            CcuLog.d(L.TAG_CCU_MODBUS,"ModbusNetwork: Serial device not connected");
            return;
        }
        try {
            for (Floor floor : HSUtil.getFloors()) {
                for (Zone zone : HSUtil.getZones(floor.getId())) {
                    CcuLog.d(L.TAG_CCU_MODBUS,"SERIAL_ =======Modbus Zone: " + zone.getDisplayName() + " =================="+","+zone.getMarkers().contains("modbus"));
                    //send request for modbus modules alone
                    for (Equip equip : HSUtil.getEquips(zone.getId())) {
                        if (equip.getMarkers().contains("modbus")) {
                            EquipmentDevice modbusDevice = EquipsManager.getInstance().fetchProfileBySlaveId(Short.parseShort(equip.getGroup()));
                            
                            for(Register register: modbusDevice.getRegisters()) {
                                LModbus.readRegister(Short.parseShort(equip.getGroup()), register, getRegisterCount(register));
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getRegisterCount(Register register) {
        
        if (register.getParameterDefinitionType().equals("long") || register.getParameterDefinitionType().equals("Int64")) {
            return READ_REGISTER_FOUR;
        } else if (register.getParameterDefinitionType().equals("float")) {
            return READ_REGISTER_TWO;
        } else {
            return READ_REGISTER_ONE;
        }
    }
    
    public void sendSystemControl() {
        //CcuLog.d(L.TAG_CCU_DEVICE, "Modbus SendSystemControl");
    }
}
