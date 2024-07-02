package a75f.io.device.modbus;

import android.util.Log;

import com.x75f.modbus4j.ModbusConversions;
import com.x75f.modbus4j.msg.ModbusRequest;
import com.x75f.modbus4j.msg.ReadCoilsRequest;
import com.x75f.modbus4j.msg.ReadDiscreteInputsRequest;
import com.x75f.modbus4j.msg.ReadHoldingRegistersRequest;
import com.x75f.modbus4j.msg.ReadInputRegistersRequest;
import com.x75f.modbus4j.msg.WriteCoilRequest;
import com.x75f.modbus4j.msg.WriteRegisterRequest;
import com.x75f.modbus4j.msg.WriteRegistersRequest;
import com.x75f.modbus4j.serial.rtu.RtuMessageRequest;

import java.util.Objects;

import a75f.io.api.haystack.modbus.Register;
import a75f.io.device.mesh.LSerial;
import a75f.io.device.serial.MessageType;
import a75f.io.device.serial.ModbusFloatMessage_t;
import a75f.io.device.serial.ModbusMessage_t;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;

public class LModbus {
    
    public static final String MODBUS_REGISTER_DISCRETE_INPUT = "discreteInput";
    public static final String MODBUS_REGISTER_HOLDING = "holdingRegister";
    public static final String MODBUS_REGISTER_INPUT = "inputRegister";
    public static final String MODBUS_REGISTER_READ_COIL = "readCoil";
    public static final String MODBUS_REGISTER_WRITE_COIL = "writeCoil";
    public static final String MODBUS_REGISTER_COIL = "coil";
    public static int SERIAL_COMM_TIMEOUT_MS = 1000;
    public static boolean IS_MODBUS_DATA_RECEIVED = false;
    private static SerialCommLock modbusCommLock = new SerialCommLock();
    public static SerialCommLock getModbusCommLock() {
        return modbusCommLock;
    }

    public static byte[] getModbusData(Short slaveid, String registerType, int registerAddr, int numberOfRegisters){
            ModbusRequest request;
            RtuMessageRequest rtuMessageRequest;
            try {
                 switch (registerType) {
                    case MODBUS_REGISTER_COIL:
                         request = new ReadCoilsRequest(slaveid, registerAddr, numberOfRegisters);
                         rtuMessageRequest = new RtuMessageRequest(request);
                         return rtuMessageRequest.getMessageData();
                    case MODBUS_REGISTER_DISCRETE_INPUT:
                         request = new ReadDiscreteInputsRequest(slaveid, registerAddr, numberOfRegisters);
                         rtuMessageRequest = new RtuMessageRequest(request);
                         return rtuMessageRequest.getMessageData();
                    case MODBUS_REGISTER_HOLDING:
                         request = new ReadHoldingRegistersRequest(slaveid, registerAddr, numberOfRegisters);
                         rtuMessageRequest = new RtuMessageRequest(request);
                         return rtuMessageRequest.getMessageData();
                    case MODBUS_REGISTER_INPUT:
                        request = new ReadInputRegistersRequest(slaveid,registerAddr, numberOfRegisters);
                        rtuMessageRequest = new RtuMessageRequest(request);
                        return rtuMessageRequest.getMessageData();
                }
            }catch (Exception e){
                CcuLog.e(L.TAG_CCU_DEVICE, "error ", e);
            }
        return null;
    }
    
    /**
     * This may be done asynchronously.But Modbus response does not contain the register address we are reading.
     * Hence for now, wait until the response is received.
     * */
    
    public static synchronized void readRegister(Short slaveId, Register register, int offset) {
        CcuLog.d(L.TAG_CCU_MODBUS,"Read Register "+register.toString()+" SERIAL_COMM_TIMEOUT_MS "+SERIAL_COMM_TIMEOUT_MS+" slaveId "+slaveId);
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
            if(Objects.nonNull(register.multiplier)&&!register.getParameterDefinitionType().equals("boolean")&&!register.getParameterDefinitionType().equals("binary")){
                int multiplierValue = (int) Double.parseDouble(register.multiplier);
                writeValue = writeValue * multiplierValue;
            }
            ModbusRequest request;
            if (register.getRegisterType().equals(MODBUS_REGISTER_HOLDING)) {
                request = new WriteRegisterRequest(slaveId, register.getRegisterAddress(), writeValue);
            } else if (register.getRegisterType().equals(MODBUS_REGISTER_COIL)) {
                request = new WriteCoilRequest(slaveId, register.getRegisterAddress(), writeValue > 0);
            } else {
                CcuLog.d(L.TAG_CCU_MODBUS,
                         "Write cannot be executed : Invalid Register Type "+register.getRegisterType());
                return;
            }
            RtuMessageRequest rtuMessageRequest = new RtuMessageRequest(request);

            byte[] data = rtuMessageRequest.getMessageData();

            ModbusMessage_t modbusMessage = getModbusMessage(data);


            LSerial.getInstance().sendSerialToCM(modbusMessage);



            if (LSerial.getInstance().isModbusConnected()) {
                LSerial.getInstance().sendSerialToModbus(rtuMessageRequest.getMessageData());
            }
            LModbus.getModbusCommLock().lock(register, SERIAL_COMM_TIMEOUT_MS);
        } catch (Exception e) {
            Log.d(L.TAG_CCU_MODBUS, "Modbus write failed. "+register.getRegisterAddress()+" : "+writeValue+" "+e.getMessage());
        }
    }

    private static ModbusMessage_t getModbusMessage(byte[] data) {

        ModbusMessage_t modbusMessage  = new ModbusMessage_t();
        modbusMessage.messageType.set(MessageType.MODBUS_MESSAGE);
        modbusMessage.slaveId.set(data[0]);
        modbusMessage.functionCode.set(data[1]);
        modbusMessage.startingAddressHigh.set(data[2]);
        modbusMessage.startingAddressLow.set(data[3]);
        modbusMessage.quantityOfCoilsHigh.set(data[4]);
        modbusMessage.quantityOfCoilsLow.set(data[5]);
        modbusMessage.errorCheckLow.set(data[6]);
        modbusMessage.errorCheckHigh.set(data[7]);

        return modbusMessage;
    }

    private static ModbusFloatMessage_t getModbusFloatMessage(byte[] data) {

        ModbusFloatMessage_t modbusMessage  = new ModbusFloatMessage_t();
        modbusMessage.messageType.set(MessageType.MODBUS_MESSAGE);
        modbusMessage.slaveId.set(data[0]);
        modbusMessage.functionCode.set(data[1]);
        modbusMessage.startingAddressHigh.set(data[2]);
        modbusMessage.startingAddressLow.set(data[3]);
        modbusMessage.quantityOfCoilsHigh.set(data[4]);
        modbusMessage.quantityOfCoilsLow.set(data[5]);
        modbusMessage.byteCount.set(data[6]);
        modbusMessage.registerVal0.set(data[7]);
        modbusMessage.registerVal1.set(data[8]);
        modbusMessage.registerVal2.set(data[9]);
        modbusMessage.registerVal3.set(data[10]);
        modbusMessage.errorCheckLow.set(data[11]);
        modbusMessage.errorCheckHigh.set(data[12]);

        return modbusMessage;
    }

    public static synchronized void writeRegister(int slaveId, Register register, float writeValue) {
        CcuLog.d(L.TAG_CCU_MODBUS, "writeRegister Register " + register.toString()+" writeValue "+writeValue);
        try {
            if(Objects.nonNull(register.multiplier)&&!register.getParameterDefinitionType().equals("boolean")&&!register.getParameterDefinitionType().equals("binary")){
                float multiplierValue =  Float.parseFloat(register.multiplier);
                writeValue = writeValue * multiplierValue;
            }
            ModbusRequest request;
            short[] shortValues = ModbusConversions.floatToRegisters(writeValue);
            if (register.getRegisterType().equals(MODBUS_REGISTER_HOLDING)) {
                request = new WriteRegistersRequest(slaveId,  register.getRegisterAddress(), shortValues);
            } else if (register.getRegisterType().equals(MODBUS_REGISTER_COIL)) {
                request = new WriteCoilRequest(slaveId, register.getRegisterAddress(), writeValue > 0);
            } else {
                CcuLog.d(L.TAG_CCU_MODBUS,
                        "Write cannot be executed : Invalid Register Type "+register.getRegisterType());
                return;
            }

            RtuMessageRequest rtuMessageRequest = new RtuMessageRequest(request);
            byte[] data = rtuMessageRequest.getMessageData();
            ModbusFloatMessage_t modbusMessage = getModbusFloatMessage(data);
            LSerial.getInstance().sendSerialToCM(modbusMessage);

            if (LSerial.getInstance().isModbusConnected()) {
                LSerial.getInstance().sendSerialToModbus(rtuMessageRequest.getMessageData());
            }
            LModbus.getModbusCommLock().lock(register, SERIAL_COMM_TIMEOUT_MS);
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_DEVICE, "error ", e);
            Log.d(L.TAG_CCU_MODBUS, "Modbus write failed. "+register.getRegisterAddress()+" : "+writeValue+" "+e.getMessage());
        }
    }
}
