package a75f.io.device.modbus;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
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
    
        ArrayList<HashMap<Object, Object>> modbusEquips = CCUHsApi.getInstance()
                                                                  .readAllEntities("equip and modbus");
        for (HashMap equip : modbusEquips) {
            try {
                Short slaveId = Short.parseShort(equip.get("group").toString());
                EquipmentDevice modbusDevice = EquipsManager.getInstance().fetchProfileBySlaveId(slaveId);
                for (Register register : modbusDevice.getRegisters()) {
                    LModbus.readRegister(slaveId, register, getRegisterCount(register));
                }
            } catch (Exception e) {
                e.printStackTrace();
                CcuLog.d(L.TAG_CCU_MODBUS,"Modbus read failed : "+equip.toString());
            }
        }
    }

    private int getRegisterCount(Register register) {
        
        if (register.getParameterDefinitionType().equals("long") || register.getParameterDefinitionType().equals("unsigned long") || register.getParameterDefinitionType().equals("int64")) {
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
