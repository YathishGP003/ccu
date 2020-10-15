package a75f.io.device.modbus;

import android.util.Log;

import com.x75f.modbus4j.msg.ModbusRequest;
import com.x75f.modbus4j.msg.ReadCoilsRequest;
import com.x75f.modbus4j.msg.ReadDiscreteInputsRequest;
import com.x75f.modbus4j.msg.ReadHoldingRegistersRequest;
import com.x75f.modbus4j.msg.ReadInputRegistersRequest;
import com.x75f.modbus4j.msg.WriteRegisterRequest;
import com.x75f.modbus4j.serial.rtu.RtuMessageRequest;

public class LModbus {

    public static byte[] getModbusData(Short slaveid, String registerType, int registerAddr, int numberOfRegisters){
        //byte[] bytes = {0xa, 3, 0, 0, 0, 14, (byte) 0xC5, (byte) 0x75};
            ModbusRequest request;
            RtuMessageRequest rtuMessageRequest;
            try {
                //EquipmentDevice equipmentDevice = EquipsManager.getInstance().fetchProfileBySlaveId(slaveid);
                Log.d("Modbus","LModbus getModbusData="+slaveid+","+registerType+","+registerAddr);
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
}
