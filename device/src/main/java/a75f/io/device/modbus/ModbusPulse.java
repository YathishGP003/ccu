package a75f.io.device.modbus;

import android.util.Log;

import com.felhr.utils.UsbModbusUtils;
import com.x75f.modbus4j.msg.ModbusResponse;
import com.x75f.modbus4j.serial.rtu.RtuMessageResponse;
import com.x75f.modbus4j.sero.util.queue.ByteQueue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.modbus.Register;
import a75f.io.device.mesh.DLog;
import a75f.io.logic.L;

public class ModbusPulse {
    private static final int MODBUS_DATA_START_INDEX = 3;
    private static int registerIndex = 0;

    public static void handleModbusPulseData(byte[] data, int slaveid){
        if(UsbModbusUtils.validSlaveId(slaveid) ) {
            switch (UsbModbusUtils.validateFunctionCode((data[1] & 0xff))) {
                case UsbModbusUtils.READ_COILS:
                    validateResponse(slaveid, data, "registerNumber", UsbModbusUtils.READ_COILS);
                    break;
                case UsbModbusUtils.READ_DISCRETE_INPUTS:
                    validateResponse(slaveid, data, "registerNumber",UsbModbusUtils.READ_DISCRETE_INPUTS);
                    break;
                case UsbModbusUtils.READ_HOLDING_REGISTERS:
                    validateResponse(slaveid, data,"registerNumber", UsbModbusUtils.READ_HOLDING_REGISTERS);
                    break;
                case UsbModbusUtils.READ_INPUT_REGISTERS:
                    validateResponse(slaveid, data, "registerNumber",UsbModbusUtils.READ_INPUT_REGISTERS);
                    break;
                case UsbModbusUtils.WRITE_COIL:
                    break;
                case UsbModbusUtils.WRITE_REGISTER:
                    validateResponse(slaveid, data, "registerNumber",UsbModbusUtils.WRITE_REGISTER);
                    break;
                case UsbModbusUtils.READ_EXCEPTION_STATUS:
                    break;
                case UsbModbusUtils.WRITE_COILS:
                    break;
                case UsbModbusUtils.WRITE_REGISTERS:
                    break;
                case UsbModbusUtils.REPORT_SLAVE_ID:
                    break;
                case UsbModbusUtils.WRITE_MASK_REGISTER:
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
                Log.d(L.TAG_CCU_MODBUS, "Response success==" + rtuResponse.getModbusMessage().toString());
                updateResponseToHaystack(slaveid, rtuResponse,registerType);
            }else {
                Log.d(L.TAG_CCU_MODBUS,
                      "handlingResponse, exception-"+rtuResponse.getModbusResponse().getExceptionMessage());
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
            updateModbusRespone(device.get("id").toString(), response, registerType);
        }
    }

    private static void updateModbusRespone(String deviceRef, RtuMessageResponse response,byte registerType){

        CCUHsApi hayStack = CCUHsApi.getInstance();
        Register readRegister = LModbus.getModbusCommLock().getRegister();

        HashMap phyPoint = hayStack.read("point and physical and register and modbus" +
                                         " and registerType == \""+readRegister.getRegisterType()+"\""+
                                         " and registerAddress == \""+readRegister.getRegisterAddress()+ "\""+
                                         " and parameterId == \""+readRegister.getParameters().get(0).getParameterId()+ "\""+
                                         " and deviceRef == \"" + deviceRef + "\"");
        
        if (phyPoint.get("pointRef") == null || phyPoint.get("pointRef") == "") {
            Log.d(L.TAG_CCU_MODBUS, "Physical point does not exist for register "
                                            +readRegister.getRegisterAddress() +" and device "+deviceRef);
            return;
        }
        HashMap logPoint = hayStack.read("point and id==" + phyPoint.get("pointRef"));
        Log.d(L.TAG_CCU_MODBUS,"Response data : "+Arrays.toString(response.getMessageData()));
        double formattedVal = 0;
        switch (UsbModbusUtils.validateFunctionCode(registerType)){
            case UsbModbusUtils.READ_INPUT_REGISTERS:
            case UsbModbusUtils.READ_HOLDING_REGISTERS:
            case UsbModbusUtils.READ_DISCRETE_INPUTS:
                formattedVal = getRegisterValFromResponse(readRegister, response);
                hayStack.writeHisValById(logPoint.get("id").toString(),formattedVal);
                hayStack.writeHisValById(phyPoint.get("id").toString(), formattedVal);
                
                if (logPoint.containsKey("writable")) {
                    hayStack.writePoint(logPoint.get("id").toString(), formattedVal);
                }
                //startIndex +=2;
                break;
            case UsbModbusUtils.WRITE_REGISTER:
                //Parsed only for logging.
                formattedVal = (response.getMessageData()[MODBUS_DATA_START_INDEX+1] << 8)
                                | (response.getMessageData()[MODBUS_DATA_START_INDEX + 2]);
                break;
            default:
                Log.d(L.TAG_CCU_MODBUS, "Unknown Register type data "+Arrays.toString(response.getMessageData()));
                break;
        }
        Log.d(L.TAG_CCU_MODBUS, "Pulse Register: Type "+registerType+ ", Addr "+readRegister.getRegisterAddress()+
                                        " Val "+formattedVal);
    
        LModbus.getModbusCommLock().unlock();
    }
    
    public static double getRegisterValFromResponse(Register register, RtuMessageResponse response) {
        double respVal = 0;
        Log.d("CCU_MODBUS","reg param type "+ register.getParameterDefinitionType());

        if (register.registerType.equals("discreteInput")) {
            //16bit decimal (ir) or 1 bit (di)
            respVal = parseByteVal(response);
        } else if (register.registerType.equals("inputRegister") || register.registerType.equals("holdingRegister")) {
            if (register.getParameterDefinitionType().equals("float")) {
                    if (register.getWordOrder() != null && register.getWordOrder().equals("littleEndian")) {
                        respVal = parseLittleEndianFloatVal(response);
                    } else {
                        respVal = parseFloatVal(response);
                    }
            } else if (register.getParameterDefinitionType().equals("integer")
                  || register.getParameterDefinitionType().equals("decimal")
                  || register.getParameterDefinitionType().equals("range")) {

                respVal = parseIntVal(response);


            } else if (register.getParameterDefinitionType().equals("binary")) {
                
                if (register.getParameters().size() > 0) {
                    respVal = parseBitVal(response, register.getParameters().get(0).bitParam);
                }
            } else if (register.getParameterDefinitionType().equals("boolean")) {
                
                if (register.getParameters().size() > 0) {
                    respVal = parseBitRangeVal(response, register.getParameters().get(0).bitParamRange);
                }
            }  else if (register.getParameterDefinitionType().equals("int64") ||
                        register.getParameterDefinitionType().equals("unsigned long") ||
                        register.getParameterDefinitionType().equals("long")) {
                
                if (register.getParameters().size() > 0) {
                    if (register.getWordOrder() != null && register.getWordOrder().equals("littleEndian")) {
                        respVal = parseLittleEndianInt64Val(response);
                    } else {
                        respVal = parseInt64Val(response);
                    }
                }
            }

            if(Objects.nonNull(register.multiplier)&&!register.getParameterDefinitionType().equals("boolean")&&!register.getParameterDefinitionType().equals("binary")){
                double multiplierValue= Double.parseDouble(register.multiplier);
                respVal=respVal*multiplierValue;
            }
        }
        
        return respVal;
    }
    
    public static double parseFloatVal(RtuMessageResponse response) {
        /*int responseVal = (response.getMessageData()[MODBUS_DATA_START_INDEX] & 0xFF) << 24 |
                      (response.getMessageData()[MODBUS_DATA_START_INDEX + 1] & 0xFF) << 16 |
                      (response.getMessageData()[MODBUS_DATA_START_INDEX + 2] & 0xFF) << 8 |
                      (response.getMessageData()[MODBUS_DATA_START_INDEX + 3] & 0xFF);*/
    
        int responseVal = 0;
        for (int i = 0; i < 4; i++) {
            responseVal <<= Long.BYTES ;
            responseVal |= (response.getMessageData()[MODBUS_DATA_START_INDEX + i] & 0xFF);
        }
    
        double formattedVal = Float.intBitsToFloat(responseVal);
    
        if (Double.isNaN(formattedVal)) {
            formattedVal = 0;
        }
        return formattedVal;
    }
    
    public static double parseLittleEndianFloatVal(RtuMessageResponse response) {
        
        int responseVal = (response.getMessageData()[MODBUS_DATA_START_INDEX + 2] & 0xFF) << 24 |
                      (response.getMessageData()[MODBUS_DATA_START_INDEX + 3] & 0xFF) << 16 |
                      (response.getMessageData()[MODBUS_DATA_START_INDEX] & 0xFF) << 8 |
                      (response.getMessageData()[MODBUS_DATA_START_INDEX + 1] & 0xFF);
        
        double formattedVal = Float.intBitsToFloat(responseVal);
        
        if (Double.isNaN(formattedVal)) {
            formattedVal = 0;
        }
        return formattedVal;
    }
    
    public static int parseByteVal(RtuMessageResponse response) {
        return response.getMessageData()[MODBUS_DATA_START_INDEX] & 0xFF ;
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
    
    public static long parseInt64Val(RtuMessageResponse response) {
        long responseVal = 0;
        for (int i = 0; i < Long.BYTES; i++) {
            responseVal <<= Long.BYTES;
            responseVal |= (response.getMessageData()[MODBUS_DATA_START_INDEX + i] & 0xFF);
        }
        return responseVal;
    }
    
    public static long parseLittleEndianInt64Val(RtuMessageResponse response) {
        long responseVal = ((long)response.getMessageData()[MODBUS_DATA_START_INDEX + 6] & 0xFF) << 56 |
                           ((long)response.getMessageData()[MODBUS_DATA_START_INDEX + 7] & 0xFF) << 48 |
                           ((long)response.getMessageData()[MODBUS_DATA_START_INDEX + 4] & 0xFF) << 40 |
                           ((long)response.getMessageData()[MODBUS_DATA_START_INDEX + 5] & 0xFF) << 32 |
                           ((long)response.getMessageData()[MODBUS_DATA_START_INDEX + 2] & 0xFF) << 24 |
                           ((long)response.getMessageData()[MODBUS_DATA_START_INDEX + 3] & 0xFF) << 16 |
                           ((long)response.getMessageData()[MODBUS_DATA_START_INDEX] & 0xFF) << 8 |
                           ((long)response.getMessageData()[MODBUS_DATA_START_INDEX + 1] & 0xFF);
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
        
        int rangeVal = (int) extractBits(responseVal, upperLimit-lowerLimit + 1, lowerLimit);
        return rangeVal;
    }
    
    public static int parseBitVal(RtuMessageResponse response, int position) {
        long responseVal = parseLongVal(response);
        return (responseVal & (1 << position)) > 0 ? 1 : 0;
    }
    
    public static long extractBits(final long l, final int nrBits, final int offset)
    {
        final long rightShifted = l >>> offset;
        final long mask = (1L << nrBits) - 1L;
        return rightShifted & mask;
    }
    
}
