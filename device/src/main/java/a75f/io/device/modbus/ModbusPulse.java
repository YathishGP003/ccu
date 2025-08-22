package a75f.io.device.modbus;


import com.felhr.utils.UsbModbusUtils;
import com.x75f.modbus4j.msg.ModbusResponse;
import com.x75f.modbus4j.serial.rtu.RtuMessageResponse;
import com.x75f.modbus4j.sero.util.queue.ByteQueue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;
import a75f.io.api.haystack.modbus.Register;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.util.CCUUtils;

import org.apache.commons.lang3.StringUtils;
import org.projecthaystack.HStr;

public class ModbusPulse {
    private static final int MODBUS_DATA_START_INDEX = 3;
    private static int registerIndex = 0;
    private static Map<Integer, Long> lastHisItemMap = new HashMap<>();

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
                case UsbModbusUtils.READ_EXCEPTION_STATUS:
                case UsbModbusUtils.WRITE_COILS:
                case UsbModbusUtils.WRITE_REGISTERS:
                case UsbModbusUtils.REPORT_SLAVE_ID:
                case UsbModbusUtils.WRITE_MASK_REGISTER:
                    break;
                case UsbModbusUtils.WRITE_REGISTER:
                    validateResponse(slaveid, data, "registerNumber",UsbModbusUtils.WRITE_REGISTER);
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
                CcuLog.d(L.TAG_CCU_MODBUS, "Response success==" + rtuResponse.getModbusMessage().toString());
                updateResponseToHaystack(slaveid, rtuResponse, registerType);
            }else {
                CcuLog.d(L.TAG_CCU_MODBUS,
                      "handlingResponse, exception-"+rtuResponse.getModbusResponse().getExceptionMessage());
            }
        } catch (Exception e) {
            CcuLog.e(L.TAG_CCU_DEVICE, "error ", e);
        }
    }

    private static void updateResponseToHaystack(int slaveid, RtuMessageResponse response, byte registerType){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        List<HashMap<Object, Object>> deviceList = hayStack.readAllEntities("device and addr == \""+slaveid+"\"");

        StringBuilder deviceRefString = new StringBuilder("(");
        int index = 0;
        for(HashMap<Object, Object> device : deviceList){
            deviceRefString.append("deviceRef == ");
            deviceRefString.append("\""+StringUtils.prependIfMissing(device.get("id").toString(), "@")+"\"");
            if(index == deviceList.size()-1){
                deviceRefString.append(" ) ");
            }
            else{
                deviceRefString.append(" or ");
            }
            index++;
        }
        if (!deviceList.isEmpty()) {
            LModbus.IS_MODBUS_DATA_RECEIVED = true;
            updateModbusResponse(deviceRefString.toString(), response, registerType);
            updateHeartBeatPoint(slaveid, hayStack);
        }
    }

    private static void updateHeartBeatPoint(int slaveId, CCUHsApi hayStack){
        List<HashMap<Object, Object>> equipList =
                hayStack.readAllEntities("equip and modbus and group == \"" + slaveId +
                        "\"");
        for(HashMap<Object, Object> equip : equipList) {
            if(CCUUtils.isModbusHeartbeatRequired(equip, hayStack)) {
                HashMap<Object, Object> heartBeatPoint = hayStack.readEntity("point and (heartBeat or heartbeat) and equipRef == " +
                        "\"" + equip.get("id") + "\"");
                long current_millis = System.currentTimeMillis();
                if(!lastHisItemMap.containsKey(slaveId)) {
                    hayStack.writeHisValueByIdWithoutCOV(heartBeatPoint.get("id").toString(), 1.0);
                    lastHisItemMap.put(slaveId, current_millis);
                } else if((current_millis - lastHisItemMap.get(slaveId)) > 40000) {
                    hayStack.writeHisValueByIdWithoutCOV(heartBeatPoint.get("id").toString(), 1.0);
                    lastHisItemMap.put(slaveId, current_millis);
                }
            }
        }
    }

    private static void updateModbusResponse(String deviceRefString, RtuMessageResponse response, byte registerType){
        CCUHsApi hayStack = CCUHsApi.getInstance();
        Register readRegister = LModbus.getModbusCommLock().getRegister();

        HashMap phyPoint = hayStack.read("point and physical and register and modbus" +
                                         " and registerType == \""+readRegister.getRegisterType()+"\""+
                                         " and registerAddress == \""+readRegister.getRegisterAddress()+ "\""+
                                         " and parameterId == \""+readRegister.getParameters().get(0).getParameterId()+ "\""+
                                         " and "+ deviceRefString);

        if (phyPoint.get("pointRef") == null || phyPoint.get("pointRef") == "") {
            CcuLog.d(L.TAG_CCU_MODBUS, "Physical point does not exist for register "
                                            +readRegister.getRegisterAddress() +" and device "+deviceRefString);
            return;
        }
        HashMap logPoint = hayStack.read("point and id==" + phyPoint.get("pointRef"));
//        double priorityVal = hayStack.readPointPriorityVal(logPoint.get("id").toString());
        CcuLog.d(L.TAG_CCU_MODBUS,"Response data : "+Arrays.toString(response.getMessageData()));
        double formattedVal = 0;
        switch (UsbModbusUtils.validateFunctionCode(registerType)){
            case UsbModbusUtils.READ_INPUT_REGISTERS:
            case UsbModbusUtils.READ_HOLDING_REGISTERS:
            case UsbModbusUtils.READ_DISCRETE_INPUTS:
            case UsbModbusUtils.READ_COILS:
                formattedVal = getRegisterValFromResponse(readRegister, response);

                if(!logPoint.containsKey("writable")) {
                    hayStack.writeHisValById(logPoint.get("id").toString(),formattedVal);
                    hayStack.writeHisValById(phyPoint.get("id").toString(), formattedVal);
                } else if (logPoint.containsKey("writable") &&
                        !(logPoint.containsKey(Tags.SCHEDULABLE)
                        && (logPoint.containsKey(Tags.SCHEDULE_REF) || logPoint.containsKey(Tags.EVENT_REF)))) {
                    hayStack.writeHisValById(logPoint.get("id").toString(),formattedVal);
                    hayStack.writeHisValById(phyPoint.get("id").toString(), formattedVal);
                    hayStack.writePoint(logPoint.get("id").toString(), formattedVal);
                }
                //startIndex +=2;
                break;
            case UsbModbusUtils.WRITE_COIL:
            case UsbModbusUtils.WRITE_REGISTER:
                //Parsed only for logging.
                formattedVal = (response.getMessageData()[MODBUS_DATA_START_INDEX+1] << 8)
                                | (response.getMessageData()[MODBUS_DATA_START_INDEX + 2]);
                break;
            default:
                CcuLog.d(L.TAG_CCU_MODBUS, "Unknown Register type data "+Arrays.toString(response.getMessageData()));
                break;
        }
        CcuLog.d(L.TAG_CCU_MODBUS, "Pulse Register: Type "+registerType+ ", Addr "+readRegister.getRegisterAddress()+
                                        " Val "+formattedVal);

        LModbus.getModbusCommLock().unlock();
//        if ((priorityVal != formattedVal) && logPoint.containsKey("writable") && (logPoint.containsKey(Tags.SCHEDULABLE)
//                        && (logPoint.containsKey(Tags.SCHEDULE_REF) || logPoint.containsKey(Tags.EVENT_REF)))) {
//            CcuLog.d(L.TAG_CCU_MODBUS, "Received Modbus value is different from Haystack priority value. " +
//                    "Haystack priority value: " + priorityVal + ", Modbus value: " + formattedVal + ", hence sending the priority data to the device.");
//            if(readRegister.parameterDefinitionType.equals("float")) {
//                LModbus.writeRegister(Integer.parseInt(logPoint.get("group").toString()), readRegister, (float) priorityVal);
//            }  else {
//                LModbus.writeRegister(Integer.parseInt(logPoint.get("group").toString()), readRegister, (int) priorityVal);
//            }
//        }
    }
    
    public static double getRegisterValFromResponse(Register register, RtuMessageResponse response) {
        double respVal = 0;
        CcuLog.d("CCU_MODBUS","reg param type "+ register.getParameterDefinitionType());

        if (register.registerType.equals("discreteInput") || register.registerType.equals("coil")) {
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
                
                if (!register.getParameters().isEmpty()) {
                    respVal = parseBitVal(response, register.getParameters().get(0).bitParam);
                }
            } else if (register.getParameterDefinitionType().equals("boolean")) {
                
                if (!register.getParameters().isEmpty()) {
                    if(register.getParameters().get(0).bitParamRange != null) {
                        respVal = parseBitRangeVal(response, register.getParameters().get(0).bitParamRange);
                    } else {
                        respVal = parseBitRangeVal(response, register.getParameters().get(0).getStartBit(), register.getParameters().get(0).getEndBit());
                    }
                }
            }  else if (register.getParameterDefinitionType().equals("int64")) {
                
                if (!register.getParameters().isEmpty()) {
                    if (register.getWordOrder() != null && register.getWordOrder().equals("littleEndian")) {
                        respVal = parseLittleEndianInt64Val(response);
                    } else {
                        respVal = parseInt64Val(response);
                    }
                }
            } else if(register.getParameterDefinitionType().equals("unsigned long")){
                if (!register.getParameters().isEmpty()) {
                    if (register.getWordOrder() != null && register.getWordOrder().equals("littleEndian")) {
                        respVal = parseLittleEndianUnsignedLongVal(response);
                    } else {
                        respVal = parseUnsignedLongVal(response);
                    }
                }
            } else if(register.getParameterDefinitionType().equals("long") ||
                    register.getParameterDefinitionType().equals("int32")){
                if (!register.getParameters().isEmpty()) {
                    if (register.getWordOrder() != null && register.getWordOrder().equals("littleEndian")) {
                        respVal = parseLittleEndianInt32Val(response);
                    } else {
                        respVal = parseInt32Val(response);
                    }
                }
            }

            if(Objects.nonNull(register.multiplier)&&!register.getParameterDefinitionType().equals("boolean")
                    &&!register.getParameterDefinitionType().equals("binary")){
                double multiplierValue = Double.parseDouble(register.multiplier);
                if(register.getParameters().get(0).getLogicalPointTags().stream().anyMatch
                        (it -> it.getTagName().equals("writable"))){
                    respVal = respVal / multiplierValue;
                }else {
                    respVal = respVal * multiplierValue;
                }
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
        } else {
            formattedVal = Math.round(formattedVal * 100.0) / 100.0;
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
        } else {
            formattedVal = Math.round(formattedVal * 100.0) / 100.0;
        }
        return formattedVal;
    }
    
    public static int parseByteVal(RtuMessageResponse response) {
        return response.getMessageData()[MODBUS_DATA_START_INDEX] & 0xFF ;
    }
    
    public static int parseIntVal(RtuMessageResponse response) {
        return (short) ((response.getMessageData()[MODBUS_DATA_START_INDEX] & 0xFF) << 8 |
                      (response.getMessageData()[MODBUS_DATA_START_INDEX + 1] & 0xFF)) ;
    }
    
    public static long parseLongVal(RtuMessageResponse response) {
        return (response.getMessageData()[MODBUS_DATA_START_INDEX] & 0xFF) << 8 |
                           (response.getMessageData()[MODBUS_DATA_START_INDEX + 1] & 0xFF);
    }

    public static long parseUnsignedLongVal(RtuMessageResponse response) {
        long responseVal = 0;
        for (int i = 0; i < 4; i++) {
            responseVal <<= Long.BYTES;
            responseVal |= (response.getMessageData()[MODBUS_DATA_START_INDEX + i] & 0xFF);
        }
        return responseVal;
    }

    public static int parseInt32Val(RtuMessageResponse response) {
        int responseVal = 0;
        for (int i = 0; i < 4; i++) {
            responseVal <<= Long.BYTES;
            responseVal |= (response.getMessageData()[MODBUS_DATA_START_INDEX + i] & 0xFF);
        }
        return responseVal;
    }

    public static long parseLittleEndianUnsignedLongVal(RtuMessageResponse response) {
        return ((long)response.getMessageData()[MODBUS_DATA_START_INDEX + 2] & 0xFF) << 24 |
                ((long)response.getMessageData()[MODBUS_DATA_START_INDEX + 3] & 0xFF) << 16 |
                ((long)response.getMessageData()[MODBUS_DATA_START_INDEX] & 0xFF) << 8 |
                ((long)response.getMessageData()[MODBUS_DATA_START_INDEX + 1] & 0xFF);
    }

    public static int parseLittleEndianInt32Val(RtuMessageResponse response) {
        return ((int)response.getMessageData()[MODBUS_DATA_START_INDEX + 2] & 0xFF) << 24 |
                ((int)response.getMessageData()[MODBUS_DATA_START_INDEX + 3] & 0xFF) << 16 |
                ((int)response.getMessageData()[MODBUS_DATA_START_INDEX] & 0xFF) << 8 |
                ((int)response.getMessageData()[MODBUS_DATA_START_INDEX + 1] & 0xFF);
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
        return ((long)response.getMessageData()[MODBUS_DATA_START_INDEX + 6] & 0xFF) << 56 |
                           ((long)response.getMessageData()[MODBUS_DATA_START_INDEX + 7] & 0xFF) << 48 |
                           ((long)response.getMessageData()[MODBUS_DATA_START_INDEX + 4] & 0xFF) << 40 |
                           ((long)response.getMessageData()[MODBUS_DATA_START_INDEX + 5] & 0xFF) << 32 |
                           ((long)response.getMessageData()[MODBUS_DATA_START_INDEX + 2] & 0xFF) << 24 |
                           ((long)response.getMessageData()[MODBUS_DATA_START_INDEX + 3] & 0xFF) << 16 |
                           ((long)response.getMessageData()[MODBUS_DATA_START_INDEX] & 0xFF) << 8 |
                           ((long)response.getMessageData()[MODBUS_DATA_START_INDEX + 1] & 0xFF);
    }
    
    public static int parseBitRangeVal(RtuMessageResponse response, String range) {
        String [] arrOfLimits = range.split("-");
        
        if (arrOfLimits.length != 2) {
            CcuLog.w(L.TAG_CCU_MODBUS, "Invalid bit range format: " + range);
            return 0;
        }
    
        int lowerLimit = Integer.parseInt(arrOfLimits[0]);
        int upperLimit = Integer.parseInt(arrOfLimits[1]);
    
        long responseVal = parseLongVal(response);

        return (int) extractBits(responseVal, upperLimit-lowerLimit + 1, lowerLimit);
    }

    public static int parseBitRangeVal(RtuMessageResponse response, Integer lowerLimit, Integer upperLimit) {
        if(lowerLimit == null || upperLimit == null || lowerLimit > upperLimit) {
            CcuLog.w(L.TAG_CCU_MODBUS, "Invalid bit range: lowerLimit=" + lowerLimit + ", upperLimit=" + upperLimit);
            return 0;
        }

        long responseVal = parseLongVal(response);

        return (int) extractBits(responseVal, upperLimit-lowerLimit + 1, lowerLimit);

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
