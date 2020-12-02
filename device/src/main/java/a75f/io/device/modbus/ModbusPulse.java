package a75f.io.device.modbus;

import android.util.Log;

import com.felhr.utils.UsbModbusUtils;
import com.google.common.primitives.UnsignedInts;
import com.x75f.modbus4j.base.ModbusUtils;
import com.x75f.modbus4j.msg.ModbusResponse;
import com.x75f.modbus4j.serial.rtu.RtuMessageResponse;
import com.x75f.modbus4j.sero.util.queue.ByteQueue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.modbus.EquipmentDevice;
import a75f.io.api.haystack.modbus.Parameter;
import a75f.io.api.haystack.modbus.Register;
import a75f.io.device.mesh.DLog;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.modbusbox.EquipsManager;

public class ModbusPulse {
    private static final int MODBUS_DATA_START_INDEX = 3;
    private static int registerIndex = 0;

    public static void handleModbusPulseData(byte[] data, int slaveid){
        if(UsbModbusUtils.validSlaveId(slaveid) ) {
            DLog.LogdSerial("*************Event Type Handle MODBUS Data type here**********************:" + Arrays.toString(data));
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
            //ModbusUtils.checkCRC(rtuResponse.getModbusMessage(), queue);

            if(!rtuResponse.getModbusResponse().isException()){
                DLog.LogdSerial("MODBUS Response success==" + rtuResponse.getModbusMessage().toString());
                updateResponseToHaystack(slaveid, rtuResponse,registerType);
            }else {
                DLog.Logd("MODBUS handlingResponse, exception-"+rtuResponse.getModbusResponse().getExceptionMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateResponseToHaystack(int slaveid, RtuMessageResponse response,byte registerType){
        //EquipmentDevice equipmentDevice = EquipsManager.getInstance().fetchProfile(slaveid);
        CCUHsApi hayStack = CCUHsApi.getInstance();
        HashMap device = hayStack.read("device and addr == \""+slaveid+"\"");
        if (device != null && device.size() > 0) {
            Device d = new Device.Builder().setHashMap(device).build();
            updateModbusRespone(device.get("id").toString(), response, registerType);
        }
    }

    private static void updateModbusRespone(String deviceRef, RtuMessageResponse response,byte registerType){

        int startIndex = 3;
        int responseVal = 0;
        CCUHsApi hayStack = CCUHsApi.getInstance();
        
        Register readRegister = LModbus.getModbusCommLock().getRegister();
        
        HashMap phyPoint = hayStack.read("point and physical and register and modbus and registerAddr == \""
                                         +readRegister.getRegisterAddress()+ "\" and deviceRef == \"" + deviceRef + "\"");
        //for(HashMap phyPoint : phyPoints) {
            if (phyPoint.get("pointRef") == null || phyPoint.get("pointRef") == "") {
                Log.d(L.TAG_CCU_MODBUS, "Physical point does not exist for register "
                                                +readRegister.getRegisterAddress() +" and device "+deviceRef);
                return;
            }
            HashMap logPoint = hayStack.read("point and id==" + phyPoint.get("pointRef"));
            //TODO check for valid registerAddr in phypoint based on response addrss?
            //We do get address from 1 till say 247??? based on our locally consumed parameters, we fetch that index and get value for the same.
            //int responseVal = response.getMessageData()[Integer.parseInt(phyPoint.get("registerAddress").toString())];
        
            Log.d(L.TAG_CCU_MODBUS," Response data : "+Arrays.toString(response.getMessageData()));
            double formattedVal = 0;
            switch (UsbModbusUtils.validateFunctionCode(registerType)){
                case UsbModbusUtils.READ_INPUT_REGISTERS:
                case UsbModbusUtils.READ_HOLDING_REGISTERS:
                case UsbModbusUtils.READ_DISCRETE_INPUTS:
                    formattedVal = getRegisterValFromResponse(readRegister, response);
                    //startIndex +=2;
                    break;
                case UsbModbusUtils.WRITE_REGISTER:
                    //TODO:
                    formattedVal = response.getMessageData()[startIndex+1] << 8 | response.getMessageData()[startIndex + 2];
                    break;
                default:
                    Log.d(L.TAG_CCU_MODBUS, "Unknown Register type data "+Arrays.toString(response.getMessageData()));
                    break;
            }
            //if(response.getMessageData()[3] != startIndex)
           
            Log.d(L.TAG_CCU_MODBUS, "Pulse Register: Type "+registerType+ ", Addr "+readRegister.getRegisterAddress()+
                                            " Val "+formattedVal);
            hayStack.writeHisValById(logPoint.get("id").toString(),formattedVal);
            hayStack.writeHisValById(phyPoint.get("id").toString(), formattedVal);
        //}
    
        LModbus.getModbusCommLock().unlock();
    }
    
    public static double getRegisterValFromResponse(Register register, RtuMessageResponse response) {
        double formattedVal = 0;
        if (register.registerType.equals("inputRegister")
                || register.registerType.equals("discreteInput")) {
            //16bit decimal (ir) or 1 bit (di)
            formattedVal = parseIntVal(response);
        } else if (register.registerType.equals("holdingRegister")) {
    
            if (register.getParameterDefinitionType().equals("float")) {
                formattedVal = parseFloatVal(response);
            } else if (register.getParameterDefinitionType().equals("integer")
                  || register.getParameterDefinitionType().equals("decimal")
                  || register.getParameterDefinitionType().equals("range")) {
        
            } else if (register.getParameterDefinitionType().equals("binary")) {
                int position = 0;//TODO : pos - startbit.
                formattedVal = parseBitVal(response, position);
            } else if (register.getParameterDefinitionType().equals("boolean")) {
                String range = "";//register.getParameters().
                formattedVal = parseBitRangeVal(response, range);
            }
        }
        
        return formattedVal;
    }
    
    public static double parseFloatVal(RtuMessageResponse response) {
        int responseVal = (response.getMessageData()[MODBUS_DATA_START_INDEX] & 0xFF) << 24 |
                      (response.getMessageData()[MODBUS_DATA_START_INDEX + 1] & 0xFF) << 16 |
                      (response.getMessageData()[MODBUS_DATA_START_INDEX + 2] & 0xFF) << 8 |
                      (response.getMessageData()[MODBUS_DATA_START_INDEX + 3] & 0xFF);
    
        double formattedVal = Float.intBitsToFloat(responseVal);
    
        if (Double.isNaN(formattedVal)) {
            formattedVal = 0;
        }
        return formattedVal;
    }
    
    public static int parseIntVal(RtuMessageResponse response) {
        return (response.getMessageData()[MODBUS_DATA_START_INDEX] & 0xFF) << 8 |
                      (response.getMessageData()[MODBUS_DATA_START_INDEX + 1] & 0xFF) ;
    }
    
    public static long parseLongVal(RtuMessageResponse response) {
        long responseVal = (response.getMessageData()[MODBUS_DATA_START_INDEX] & 0xFF) << 8 |
                           (response.getMessageData()[MODBUS_DATA_START_INDEX + 1] & 0xFF) ;
        return responseVal;
    }
    
    public static int parseBitRangeVal(RtuMessageResponse response, String range) {
        String [] arrOfLimits = range.split("-");
        
        if (arrOfLimits.length != 2) {
            throw new IllegalArgumentException(" Invalid Range : "+range);
        }
    
        int lowerLimit = Integer.parseInt(arrOfLimits[0]);
        int upperLimit = Integer.parseInt(arrOfLimits[1]);
    
        long responseVal = parseLongVal(response);
        
        int rangeVal = (int) extractBits(responseVal, upperLimit-lowerLimit, lowerLimit);
        return rangeVal;
    }
    
    public static int parseBitVal(RtuMessageResponse response, int position) {
        long responseVal = parseLongVal(response);
        return (int) responseVal & (1 << position);
    }
    
    public static long extractBits(final long l, final int nrBits, final int offset)
    {
        final long rightShifted = l >>> offset;
        final long mask = (1L << nrBits) - 1L;
        return rightShifted & mask;
    }
    
}
