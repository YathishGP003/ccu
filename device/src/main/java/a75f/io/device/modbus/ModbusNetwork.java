
package a75f.io.device.modbus;

import static a75f.io.device.modbus.ModbusModelBuilderKt.buildModbusModelByEquipRef;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.HSUtil;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.Register;
import a75f.io.device.DeviceNetwork;
import a75f.io.device.mesh.LSerial;
import a75f.io.device.serial.MessageType;
import a75f.io.device.serial.ModbusMessage_t;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.system.dab.DabExternalAhu;
import a75f.io.logic.bo.building.system.vav.VavExternalAhu;
import a75f.io.logic.interfaces.ModbusWritableDataInterface;
import a75f.io.messaging.handler.UpdatePointHandler;

public class ModbusNetwork extends DeviceNetwork implements ModbusWritableDataInterface
{
    public static final int READ_REGISTER_ONE = 1;
    public static final int READ_REGISTER_TWO = 2;
    public static final int READ_REGISTER_FOUR = 4;

    public ModbusNetwork() {
        UpdatePointHandler.setModbusWritableDataInterface(this);
        DabExternalAhu.Companion.getInstance().setModbusWritableDataInterface(this);
    }
    @Override
    public void sendMessage() {
        if (!LSerial.getInstance().isModbusConnected()) {
            CcuLog.d(L.TAG_CCU_MODBUS,"ModbusNetwork: Serial device not connected");
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(Globals.getInstance().getApplicationContext());
        LModbus.SERIAL_COMM_TIMEOUT_MS = preferences.getInt("serialCommTimeOut", 300);
        int registerRequestCount = preferences.getInt("registerRequestCount", 3);

        ArrayList<HashMap<Object, Object>> modbusEquips = CCUHsApi.getInstance().readAllEntities("equip and not " +
                "equipRef and modbus");
        modbusEquips.forEach(equipMap -> {
            try {
                EquipmentDevice equipDevice = buildModbusModelByEquipRef(equipMap.get("id").toString());
                List<EquipmentDevice> modbusDeviceList = new ArrayList<>();
                modbusDeviceList.add(equipDevice);
                if (!equipDevice.getEquips().isEmpty())
                    modbusDeviceList.addAll(equipDevice.getEquips());

                for(EquipmentDevice modbusDevice : modbusDeviceList){
                    int count = 0;
                    LModbus.IS_MODBUS_DATA_RECEIVED = false;
                    for (Register register : modbusDevice.getRegisters()) {
                        if(count++ >= registerRequestCount && !LModbus.IS_MODBUS_DATA_RECEIVED)
                            break;
                        LModbus.readRegister((short)modbusDevice.getSlaveId(), register, getRegisterCount(register));

                        CcuLog.d(L.TAG_CCU_MODBUS,
                                "modbus_data_received: "+LModbus.IS_MODBUS_DATA_RECEIVED+"" +
                                        ", count: "+count+
                                        ", registerRequestCount: "+registerRequestCount);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                CcuLog.d(L.TAG_CCU_MODBUS,"Modbus read failed : ");
            }
        });
    }

    private int getRegisterCount(Register register) {
        if (register.getParameterDefinitionType().equals("int64")) {
            return READ_REGISTER_FOUR;
        } else if (register.getParameterDefinitionType().equals("float") ||
                register.getParameterDefinitionType().equals("int32") ||
                register.getParameterDefinitionType().equals("long") ||
                register.getParameterDefinitionType().equals("unsigned long")) {
            return READ_REGISTER_TWO;
        } else {
            return READ_REGISTER_ONE;
        }
    }
    
    public void sendSystemControl() {

    }

    public void writeSystemModbusRegister(String equipRef, ArrayList<String> registerList) {
        HashMap<Object, Object> equipHashMap = CCUHsApi.getInstance().readMapById(equipRef);
        Equip equip = new Equip.Builder().setHashMap(equipHashMap).build();
        List<EquipmentDevice> modbusSubEquipList = new ArrayList<>();
        if (equip.getEquipRef() != null) {
            EquipmentDevice parentEquip = buildModbusModelByEquipRef(equip.getEquipRef());
            if (!parentEquip.getEquips().isEmpty()) {
                modbusSubEquipList.addAll(parentEquip.getEquips());
            }
        } else {
            modbusSubEquipList.add(buildModbusModelByEquipRef(equip.getId()));
        }
        for (String registerId : registerList) {
            HashMap<Object, Object> writablePoint = CCUHsApi.getInstance().readMapById(registerId);
            if (writablePoint.isEmpty()) {
                CcuLog.e(L.TAG_CCU_MODBUS, "Cant find the point to update "+registerId);
                return;
            }
            short groupId = Short.parseShort(writablePoint.get("group").toString());
            HashMap<Object, Object> physicalPoint = CCUHsApi.getInstance()
                    .readEntity("point and pointRef == \"" + writablePoint.get("id").toString() + "\"");

            if (!physicalPoint.isEmpty()) {
                for (EquipmentDevice modbusDevice : modbusSubEquipList) {
                    for (Register register : modbusDevice.getRegisters()) {
                        if (Integer.parseInt(physicalPoint.get("registerAddress").toString())
                                == register.getRegisterAddress()) {
                            float priorityVal = (float) HSUtil.getPriorityVal(registerId);
                            CcuLog.i(L.TAG_CCU_MODBUS, "Write mb register "
                                    + register.getRegisterAddress() + " val " + priorityVal);
                            LModbus.writeRegister(groupId, register, priorityVal);
                        }
                    }
                }
            }
        }
    }

    public void writeRegister(String id ) {
        HashMap<Object, Object> writablePoint = CCUHsApi.getInstance().readMapById(id);
        if (writablePoint.isEmpty()) {
            CcuLog.e(L.TAG_CCU_MODBUS, "Cant find the point to update "+id);
            return;
        }

        Point point = new Point.Builder().setHashMap(writablePoint).build();
        HashMap<Object, Object> equipHashMap = CCUHsApi.getInstance().readMapById(point.getEquipRef());
        Equip equip = new Equip.Builder().setHashMap(equipHashMap).build();

        short groupId = Short.parseShort(writablePoint.get("group").toString());
        List<EquipmentDevice> modbusSubEquipList = new ArrayList<>();
        if (equip.getEquipRef() != null) {
            EquipmentDevice parentEquip = buildModbusModelByEquipRef(equip.getEquipRef());
            if (!parentEquip.getEquips().isEmpty()) {
                modbusSubEquipList.addAll(parentEquip.getEquips());
            }
        } else {
            modbusSubEquipList.add(buildModbusModelByEquipRef(equip.getId()));
        }

        HashMap<Object, Object> physicalPoint = CCUHsApi.getInstance()
                .readEntity("point and pointRef == \""+writablePoint.get("id").toString()+"\"");
        
        if (!physicalPoint.isEmpty()) {
            for (EquipmentDevice modbusDevice : modbusSubEquipList){
                for (Register register : modbusDevice.getRegisters()) {
                    if (Integer.parseInt(physicalPoint.get("registerAddress").toString())
                            == register.getRegisterAddress()) {
                        int priorityVal = (int) HSUtil.getPriorityVal(id);
                        CcuLog.i(L.TAG_CCU_MODBUS, "Write mb register "
                                + register.getRegisterAddress() + " val " + priorityVal);

                        LModbus.writeRegister(groupId, register, priorityVal);

                    }
                }
            }
        }

    }
}
