package a75f.io.device.modbus;

import android.util.Log;

import com.x75f.modbus4j.msg.ModbusRequest;
import com.x75f.modbus4j.msg.ReadCoilsRequest;
import com.x75f.modbus4j.msg.ReadDiscreteInputsRequest;
import com.x75f.modbus4j.msg.ReadHoldingRegistersRequest;
import com.x75f.modbus4j.msg.ReadInputRegistersRequest;
import com.x75f.modbus4j.msg.WriteRegisterRequest;
import com.x75f.modbus4j.serial.rtu.RtuMessageRequest;

import a75f.io.api.haystack.modbus.Register;
import a75f.io.device.mesh.LSerial;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class LModbus {
    
    private static final int SERIAL_COMM_TIMEOUT_MS = 1000;
    private static SerialCommLock modbusCommLock = new SerialCommLock();
    
    public static SerialCommLock getModbusCommLock() {
        return modbusCommLock;
    }

    public static byte[] getModbusData(Short slaveid, String registerType, int registerAddr, int numberOfRegisters){
            ModbusRequest request;
            RtuMessageRequest rtuMessageRequest;
            try {
                 switch (registerType) {
                    case "readCoil":
                         request = new ReadCoilsRequest(slaveid, registerAddr, numberOfRegisters);
                         rtuMessageRequest = new RtuMessageRequest(request);
                         return rtuMessageRequest.getMessageData();
                    case "discreteInput":
                         request = new ReadDiscreteInputsRequest(slaveid, registerAddr, numberOfRegisters);
                         rtuMessageRequest = new RtuMessageRequest(request);
                         return rtuMessageRequest.getMessageData();
                    case "holdingRegister":
                         request = new ReadHoldingRegistersRequest(slaveid, registerAddr, numberOfRegisters);
                         rtuMessageRequest = new RtuMessageRequest(request);
                         return rtuMessageRequest.getMessageData();
                    case "inputRegister":
                        request = new ReadInputRegistersRequest(slaveid,registerAddr, numberOfRegisters);
                        rtuMessageRequest = new RtuMessageRequest(request);
                        return rtuMessageRequest.getMessageData();
                    case "writeRegister":
                        request = new WriteRegisterRequest(slaveid,registerAddr,1);
                        rtuMessageRequest = new RtuMessageRequest(request);
                        return rtuMessageRequest.getMessageData();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        return null;
    }
    
    public static synchronized void readRegister(Short slaveId, Register register, int offset) {
        CcuLog.d(L.TAG_CCU_MODBUS,"Read Register "+register.toString());
        byte[] requestData = LModbus.getModbusData(slaveId,
                                                   register.registerType,
                                                   register.registerAddress,
                                                   offset);
        LSerial.getInstance().sendSerialToModbus(requestData);
        LModbus.getModbusCommLock().lock(register, SERIAL_COMM_TIMEOUT_MS);
    }
    
    public static synchronized void writeRegister(int slaveId, Register register, int writeValue) {
        CcuLog.d(L.TAG_CCU_MODBUS, "writeRegister Register " + register.toString()+" writeValue "+writeValue);
        try {
            ModbusRequest request = new WriteRegisterRequest(slaveId, register.getRegisterAddress(), writeValue);
            RtuMessageRequest rtuMessageRequest = new RtuMessageRequest(request);
            LSerial.getInstance().sendSerialToModbus(rtuMessageRequest.getMessageData());
            LModbus.getModbusCommLock().lock(register, SERIAL_COMM_TIMEOUT_MS);
        } catch (Exception e) {
            Log.d(L.TAG_CCU_MODBUS, "Modbus write failed. "+register.getRegisterAddress()+" : "+writeValue+" "+e.getMessage());
        }
    }
}
