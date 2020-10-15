package a75f.io.device.modbus;

import com.felhr.utils.UsbModbusUtils;
import com.x75f.modbus4j.base.ModbusUtils;
import com.x75f.modbus4j.msg.ModbusResponse;
import com.x75f.modbus4j.serial.rtu.RtuMessageResponse;
import com.x75f.modbus4j.sero.util.queue.ByteQueue;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.Parameter;
import a75f.io.api.haystack.modbus.Register;
import a75f.io.device.mesh.DLog;
import a75f.io.modbusbox.EquipsManager;

public class ModbusPulse {
    private static int registerIndex = 0;

    public static void handleModbusPulseData(byte[] data, int slaveid){
        if(UsbModbusUtils.validSlaveId(slaveid) ) {
            DLog.LogdSerial("*************Event Type Handle MODBUS Data type here**********************:" + data.length + "," + data.toString());
            switch (UsbModbusUtils.validateFunctionCode((data[1] & 0xff))) {
                case UsbModbusUtils.READ_COILS:
                    if(registerIndex == 0)

                    validateResponse(slaveid, data, "registerNumber", UsbModbusUtils.READ_COILS);
                    DLog.LogdSerial("Event Type MODBUS Read Coil :" + data.length + "," + data.toString());
                    break;
                case UsbModbusUtils.READ_DISCRETE_INPUTS:
                    validateResponse(slaveid, data, "registerNumber",UsbModbusUtils.READ_DISCRETE_INPUTS);
                    DLog.LogdSerial("Event Type MODBUS Read Discreate inputs:" + data.length + "," + data.toString());
                    break;
                case UsbModbusUtils.READ_HOLDING_REGISTERS:
                    validateResponse(slaveid, data,"registerNumber", UsbModbusUtils.READ_HOLDING_REGISTERS);
                    DLog.LogdSerial("Event Type MODBUS Read Holding Registers:" + data.length + "," + data.toString());
                    break;
                case UsbModbusUtils.READ_INPUT_REGISTERS:
                    validateResponse(slaveid, data, "registerNumber",UsbModbusUtils.READ_INPUT_REGISTERS);
                    DLog.LogdSerial("Event Type MODBUS Read Input Registers:" + data.length + "," + data.toString());
                    break;
                case UsbModbusUtils.WRITE_COIL:
                    DLog.LogdSerial("Event Type MODBUS write coil:" + data.length + "," + data.toString());
                    break;
                case UsbModbusUtils.WRITE_REGISTER:
                    validateResponse(slaveid, data, "registerNumber",UsbModbusUtils.WRITE_REGISTER);
                    DLog.LogdSerial("Event Type MODBUS write register:" + data.length + "," + data.toString());
                    break;
                case UsbModbusUtils.READ_EXCEPTION_STATUS:
                    DLog.LogdSerial("Event Type MODBUS Read Exception status:" + data.length + "," + data.toString());
                    break;
                case UsbModbusUtils.WRITE_COILS:
                    DLog.LogdSerial("Event Type MODBUS write coils:" + data.length + "," + data.toString());
                    break;
                case UsbModbusUtils.WRITE_REGISTERS:
                    DLog.LogdSerial("Event Type MODBUS Write Registers:" + data.length + "," + data.toString());
                    break;
                case UsbModbusUtils.REPORT_SLAVE_ID:
                    DLog.LogdSerial("Event Type MODBUS Report slave id:" + data.length + "," + data.toString());
                    break;
                case UsbModbusUtils.WRITE_MASK_REGISTER:
                    DLog.LogdSerial("Event Type MODBUS Write Mask Registers:" + data.length + "," + data.toString());
                    break;

            }
        }
    }

    private static void validateResponse(int slaveid, byte[] data, String registerNumber, byte registerType){
        try {
            ByteQueue queue = new ByteQueue(data);

            ModbusResponse response = ModbusResponse.createModbusResponse(queue);
            RtuMessageResponse rtuResponse = new RtuMessageResponse(response);

            // Check the CRC
            ModbusUtils.checkCRC(rtuResponse.getModbusMessage(), queue);

            if(!rtuResponse.getModbusResponse().isException()){
                DLog.LogdSerial("Modbus Response success==" + rtuResponse.getModbusMessage().toString());
                updateResponseToHaystack(slaveid, rtuResponse, registerNumber,registerType);
            }else {
                DLog.Logd("Modbus handlingResponse back, exception-"+rtuResponse.getModbusResponse().getExceptionMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateResponseToHaystack(int slaveid, RtuMessageResponse response, String registerNumber,byte registerType){
        //EquipmentDevice equipmentDevice = EquipsManager.getInstance().fetchProfile(slaveid);
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap device = hayStack.read("device and addr == \""+slaveid+"\"");
        if (device != null && device.size() > 0) {
            Device d = new Device.Builder().setHashMap(device).build();
            updateModbusRespone(slaveid, device.get("id").toString(), response,"", registerType);
        }
    }

    private static void updateModbusRespone(int slaveid, String deviceRef, RtuMessageResponse response, String registerNumber,byte registerType){

        int startIndex = 3;
        int responseVal = 0;
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap phyPoint = hayStack.read("point and physical and register and modbus and registerNumber == \""+registerNumber+ "\" and deviceRef == \"" + deviceRef + "\"");
        //for(HashMap phyPoint : phyPoints) {
            if (phyPoint.get("pointRef") == null || phyPoint.get("pointRef") == "") {
                return;
            }
            HashMap logPoint = hayStack.read("point and id==" + phyPoint.get("pointRef"));
            //TODO check for valid registerAddr in phypoint based on response addrss?
            //We do get address from 1 till say 247??? based on our locally consumed parameters, we fetch that index and get value for the same.
            //int responseVal = response.getMessageData()[Integer.parseInt(phyPoint.get("registerAddress").toString())];
            switch (UsbModbusUtils.validateFunctionCode(registerType)){
                case UsbModbusUtils.READ_INPUT_REGISTERS:
                case UsbModbusUtils.READ_HOLDING_REGISTERS:
                    responseVal = response.getMessageData()[startIndex] << 8 | response.getMessageData()[startIndex + 1];
                    startIndex +=2;
                    break;
                case UsbModbusUtils.WRITE_REGISTER:
                    responseVal = response.getMessageData()[startIndex+1] << 8 | response.getMessageData()[startIndex + 2];
                    break;
                default:
                    responseVal = response.getMessageData()[startIndex];
                    break;
            }
            if(response.getMessageData()[3] != startIndex)
                DLog.Logd("Modbus Pulse = regType="+registerType+","+response.getMessageData()[startIndex]+","+response.getMessageData()[startIndex+1]+","+responseVal);
            hayStack.writeHisValById(logPoint.get("id").toString(),(double)responseVal);
            hayStack.writeHisValById(phyPoint.get("id").toString(), (double) responseVal);
        //}
    }
}
