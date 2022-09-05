
package a75f.io.device.modbus;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.Register;
import a75f.io.device.DeviceNetwork;
import a75f.io.device.mesh.LSerial;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.pubnub.ModbusWritableDataInterface;
import a75f.io.logic.pubnub.UpdatePointHandler;
import a75f.io.modbusbox.EquipsManager;

public class ModbusNetwork extends DeviceNetwork implements ModbusWritableDataInterface
{
    public static final int READ_REGISTER_ONE = 1;
    public static final int READ_REGISTER_TWO = 2;
    public static final int READ_REGISTER_FOUR = 4;

    public ModbusNetwork() {
        UpdatePointHandler.setModbusWritableDataInterface(this);
    }
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
                LModbus.setHeartbeatUpdateReceived(false);
                for (Register register : modbusDevice.getRegisters()) {
                    LModbus.readRegister(slaveId, register, getRegisterCount(register));
                    LModbus.setHeartbeatUpdateReceived(true);
                }
                if (LModbus.getHeartbeatUpdateReceived()){
                    updateHeartBeat(slaveId,CCUHsApi.getInstance(),true);
                } else {
                    updateHeartBeat(slaveId,CCUHsApi.getInstance(), false);
                }
            } catch (Exception e) {
                e.printStackTrace();
                CcuLog.d(L.TAG_CCU_MODBUS,"Modbus read failed : "+equip.toString());
            }
        }
    }

    private static void updateHeartBeat(int slaveId, CCUHsApi hayStack, boolean messageRead){
        HashMap equip = hayStack.read("equip and modbus and group == \"" + slaveId + "\"");
        HashMap heartBeatPoint = hayStack.read("point and heartbeat and equipRef == \""+equip.get("id")+ "\"");
        if(heartBeatPoint.size() == 0){
            return;
        }
        if (messageRead) {
            hayStack.writeHisValueByIdWithoutCOV(heartBeatPoint.get("id").toString(), 1.0);
        } else {
            hayStack.writeHisValueByIdWithoutCOV(heartBeatPoint.get("id").toString(), 0.0);
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

    public void writeRegister(String id) {
        HashMap<Object, Object> writablePoint = CCUHsApi.getInstance().readMapById(id);
        if (writablePoint.isEmpty()) {
            CcuLog.e(L.TAG_CCU_MODBUS, "Cant find the point to update "+id);
            return;
        }

        short groupId = Short.parseShort(writablePoint.get("group").toString());
        EquipmentDevice modbusDevice = EquipsManager.getInstance()
                .fetchProfileBySlaveId(groupId);

        HashMap<Object, Object> physicalPoint = CCUHsApi.getInstance()
                .readEntity("point and pointRef == \""+writablePoint.get("id").toString()+"\"");
        
        if (!physicalPoint.isEmpty()) {
            for (Register register : modbusDevice.getRegisters()) {
                if (Integer.parseInt(physicalPoint.get("registerAddress").toString())
                                                == register.getRegisterAddress()) {
                    int priorityVal = (int) HSUtil.getPriorityVal(id);
                    CcuLog.i(L.TAG_CCU_MODBUS, "Write mb register "
                                            +register.getRegisterAddress()+" val "+priorityVal);
                    if (LSerial.getInstance().isModbusConnected()) {
                        LModbus.writeRegister(groupId, register, priorityVal);
                    }
                }
            }
        }

    }
}
