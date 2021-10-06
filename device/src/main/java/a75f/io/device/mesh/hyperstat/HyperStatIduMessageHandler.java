package a75f.io.device.mesh.hyperstat;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.device.HyperStat.HyperStatIduControlsMessage_t;
import a75f.io.device.HyperStat.HyperStatIduStatusMessage_t;
import a75f.io.device.mesh.DLog;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.vrv.VrvControlMessageCache;

/**
 * Handle IDU specific message for HyperStat VRV Variant.
 */
class HyperStatIduMessageHandler {
    
    public static void handleIduStatusMessage(HyperStatIduStatusMessage_t iduStatus,
                                               int nodeAddress, CCUHsApi hayStack) {
        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_SERIAL, iduStatus.toString());
        }
    
        VrvControlMessageCache msgCache = VrvControlMessageCache.getInstance();
        
        if (msgCache.isControlsPendingResponse(nodeAddress)){
            if (iduStatus.getResponseType() > 0 && !msgCache.isControlsPendingDelivery(nodeAddress)) {
                msgCache.resetControlsPending(nodeAddress);
                CcuLog.d(L.TAG_CCU_SERIAL, "resetControlsPending for "+nodeAddress);
            } else {
                CcuLog.d(L.TAG_CCU_SERIAL, "Ignore IDU Status , Controls pending for "+nodeAddress);
                return;
            }
        }
        
        //Temporary solution till response is implemented
        /*if (msgCache.isControlsPendingResponse(nodeAddress)) {
            CcuLog.d(L.TAG_CCU_SERIAL, "Ignore IDU Status , Controls pending for " +
                                       ""+nodeAddress+" timer "+msgCache.getControlsPendingTimer(nodeAddress));
            return;
        }*/
        
        setOperationMode(iduStatus.getOperationMode(), nodeAddress, hayStack);
        setFanSpeed(iduStatus.getFanSpeed(), nodeAddress, hayStack);
        setAirflowDirection(iduStatus.getAirflowDirection(), nodeAddress, hayStack);
        setCoolHeatRight(iduStatus.getCoolHeatRight(), nodeAddress, hayStack);
        setMasterOperationMode(iduStatus.getMasterOperationMode(), nodeAddress, hayStack);
        setErrorStatus(iduStatus.getErrorStatus(), nodeAddress, hayStack);
        setIduState(iduStatus.getIndoorUnitState(), nodeAddress, hayStack);
        setIduFilterStatus(iduStatus.getFilterStatus(), nodeAddress, hayStack);
        setIduDiagnosticStatus(iduStatus.getDiagnosticStatus(), nodeAddress, hayStack);
        setIduTemp(iduStatus.getIduTemperature(), nodeAddress, hayStack);
        setCapabilities(iduStatus.getSupportBitfield(), nodeAddress, hayStack);
        setFanSpeedControlLevelCapability(iduStatus.getSupportFanSpeedControlLevels(), nodeAddress, hayStack);
        setGroupAddress(iduStatus.getGroupAddress(), nodeAddress, hayStack);
    }
    
    private static void setOperationMode( int opMode, int address, CCUHsApi hayStack) {
        int currOpMode = getOperationMode(address, hayStack);
        if (opMode != currOpMode) {
            //Generate Alert
            hayStack.writeDefaultVal("userIntent and operation and mode and group == \""+address+ "\"",
                                     (double)opMode);
            hayStack.writeHisValByQuery("userIntent and operation and mode and group == \""+address+ "\"",
                                     (double)opMode);
        }
    }
    
    private static void setFanSpeed( int fanSpeed, int address, CCUHsApi hayStack) {
        int currFanSpeed = getFanSpeed(address, hayStack);
        if (fanSpeed != currFanSpeed) {
            //Generate Alert
            hayStack.writeDefaultVal("userIntent and fanSpeed and group == \""+address+ "\"",
                                     (double)fanSpeed);
            hayStack.writeHisValByQuery("userIntent and fanSpeed and group == \""+address+ "\"",
                                        (double)fanSpeed);
        }
    }
    
    private static void setAirflowDirection( int airflowDir, int address, CCUHsApi hayStack) {
        int currAirflowDir = getAirflowDirection(address, hayStack);
        if (airflowDir != currAirflowDir) {
            //Generate Alert
            hayStack.writeDefaultVal("userIntent and airflowDirection and group == \""+address+ "\"",
                                     (double)airflowDir);
            hayStack.writeHisValByQuery("userIntent and airflowDirection and group == \""+address+ "\"",
                                        (double)airflowDir);
        }
    }
    
    private static void setCoolHeatRight( int coolHeatRight, int address, CCUHsApi hayStack) {
        int currCoolHeatRight = getCoolHeatRight(address, hayStack);
        if (coolHeatRight != currCoolHeatRight) {
            hayStack.writeHisValByQuery("coolHeatRight and group == \""+address+ "\"",
                                        (double)coolHeatRight);
            int masterControllerMode = getMasterController(address, hayStack);
            
            if (coolHeatRight == 0 && masterControllerMode != 0) {
                //When coolHeatRight is A , reset masterControllerMode
                setMasterController(address, 0, hayStack);
            } else if (coolHeatRight == 1 && masterControllerMode != 1) {
                //When coolHeatRight is C , reset masterControllerMode must be set to Master
                setMasterController(address, 1, hayStack);
            } else if (coolHeatRight == 2 && masterControllerMode != 0) {
                //When coolHeatRight is C , reset masterControllerMode must be set to Not-Master
                setMasterController(address, 0, hayStack);
            }
        }
    }
    
    private static void setMasterOperationMode(int masterOpMode, int address, CCUHsApi hayStack) {
        masterOpMode = masterOpMode + 1;//IDU sends opMode without 'Off' state, add an offset (1) to the received value.
        if (masterOpMode != hayStack.readHisValByQuery("master and operation and mode and group == \""+address+"\"")) {
            hayStack.writeHisValByQuery("master and operation and mode and group == \"" + address + "\"",
                                        (double) masterOpMode);
        }
    }
    
    private static void setErrorStatus(int errorStatus, int address, CCUHsApi hayStack) {
        if (errorStatus != hayStack.readHisValByQuery("idu and errorStatus and group == \""+address+"\"")) {
            hayStack.writeHisValByQuery("idu and errorStatus and group == \"" + address + "\"",
                                        (double) errorStatus);
        }
    }
    
    private static void setIduState(int iduState, int address, CCUHsApi hayStack) {
        if (iduState != hayStack.readHisValByQuery("idu and connectionStatus and group == \""+address+"\"")) {
            hayStack.writeHisValByQuery("idu and connectionStatus and group == \"" + address + "\"",
                                                (double) iduState);
        }
    }
    
    private static void setIduFilterStatus(int filterStatus, int address, CCUHsApi hayStack) {
        if (filterStatus != hayStack.readHisValByQuery("idu and filterStatus and group == \""+address+"\"")) {
            hayStack.writeHisValByQuery("idu and filterStatus and group == \""+address+"\"", (double)filterStatus);
        }
    }
    
    private static void setIduDiagnosticStatus(int diagStatus, int address, CCUHsApi hayStack) {
        if (diagStatus != hayStack.readHisValByQuery("idu and diagnosticStatus and group == \""+address+"\"")) {
            hayStack.writeHisValByQuery("idu and diagnosticStatus and group == \""+address+"\"", (double)diagStatus);
        }
    }
    
    private static void setIduTemp(int iduTemp, int address, CCUHsApi hayStack) {
        if (iduTemp != hayStack.readHisValByQuery("idu and temp and group == \""+address+"\"")) {
            hayStack.writeHisValByQuery("idu and temp and group == \""+address+"\"", (double)iduTemp);
        }
    }
    
    private static void setGroupAddress(int grpAddr, int address, CCUHsApi hayStack) {
        if (grpAddr != hayStack.readHisValByQuery("groupAddress and group == \""+address+"\"")) {
            hayStack.writeHisValByQuery("groupAddress and group == \""+address+"\"", (double)grpAddr);
        }
    }
    
    private static void setCapabilities(int capabilityBitField, int address, CCUHsApi hayStack) {
        
        int fanSpeedAutoCapability = capabilityBitField & 0x1 ;
        HashMap fanSpeedAutoCapabilityPoint = hayStack
                                  .read("capability and fanSpeed and auto and group == \""+address+"\"");
        if (!fanSpeedAutoCapabilityPoint.isEmpty() &&
                fanSpeedAutoCapability != hayStack.readHisValById(fanSpeedAutoCapabilityPoint.get("id").toString())) {
            
            hayStack.writeHisValById(fanSpeedAutoCapabilityPoint.get("id").toString(), (double)fanSpeedAutoCapability );
        }
    
        int airflowDirectionSupport = capabilityBitField & 0x2 ;
        HashMap airflowDirectionSupportPoint = hayStack
                                                  .read("capability and airflowDirection and support and group == \""+address+"\"");
        if (!airflowDirectionSupportPoint.isEmpty() &&
            airflowDirectionSupport != hayStack.readHisValById(airflowDirectionSupportPoint.get("id").toString())) {
        
            hayStack.writeHisValById(airflowDirectionSupportPoint.get("id").toString(), (double)airflowDirectionSupport );
        }
    
    
        int airflowDirectionAuto = capabilityBitField & 0x4 ;
        HashMap airflowDirectionAutoPoint = hayStack
                                                   .read("capability and airflowDirection and auto and group == \""+address+"\"");
        if (!airflowDirectionAutoPoint.isEmpty() &&
            airflowDirectionAuto != hayStack.readHisValById(airflowDirectionAutoPoint.get("id").toString())) {
        
            hayStack.writeHisValById(airflowDirectionAutoPoint.get("id").toString(), (double)airflowDirectionAuto );
        }
    }
    
    private static void setFanSpeedControlLevelCapability(int fanSpeedControl, int address, CCUHsApi hayStack) {
        if (fanSpeedControl != hayStack.readHisValByQuery("capability and fanSpeed and controlLevel and group == \""+address+
                                                          "\"")) {
            hayStack.writeHisValByQuery("capability and fanSpeed and controlLevel and group == \""+address+
                                        "\"", (double)fanSpeedControl);
        }
    }
    
    
    public static HyperStatIduControlsMessage_t getIduControlMessage(int address, CCUHsApi hayStack) {
        return HyperStatIduControlsMessage_t.newBuilder()
                                   .setOperationMode(getOperationMode(address, hayStack))
                                   .setAirflowDirection(getAirflowDirection(address, hayStack))
                                   .setFanSpeed(getFanSpeed(address, hayStack))
                                   .setHeatingSetTemperature(getHeatingDesiredTemp(address, hayStack))
                                   .setCoolingSetTemperature(getCoolingDesiredTemp(address, hayStack))
                                   .setMasterController(getMasterController(address, hayStack))
                                   .build();
    }
    
    
    private static int getOperationMode(int address, CCUHsApi hayStack) {
        return hayStack.readPointPriorityValByQuery("userIntent and operation and mode and group == \""+address+
                                                    "\"").intValue();
    }
    
    private static int getAirflowDirection(int address, CCUHsApi hayStack) {
        return hayStack.readPointPriorityValByQuery("userIntent and airflowDirection and group == \""+address+
                                                    "\"").intValue();
    }
    
    private static int getCoolHeatRight(int address, CCUHsApi hayStack) {
        return hayStack.readHisValByQuery("coolHeatRight and group == \""+address+"\"").intValue();
    }
    
    private static int getFanSpeed(int address, CCUHsApi hayStack) {
        return hayStack.readPointPriorityValByQuery("userIntent and fanSpeed and group == \""+address+"\"").intValue();
    }
    
    private static int getMasterController(int address, CCUHsApi hayStack) {
        return hayStack.readDefaultVal("config and mode and masterController and group == \""+address+"\"").intValue();
    }
    
    private static void setMasterController(int address, int masterControlMode, CCUHsApi hayStack) {
        hayStack.writeDefaultVal("config and mode and masterController and group == \""+address+"\"",
                                        (double)masterControlMode);
    }
    
    private static int getHeatingDesiredTemp(int address, CCUHsApi hayStack) {
        return hayStack.readPointPriorityValByQuery("desired and temp and heating and group == \""+address+"\"").intValue() * 2;
    }
    
    private static int getCoolingDesiredTemp(int address, CCUHsApi hayStack) {
        return hayStack.readPointPriorityValByQuery("desired and temp and cooling and group == \""+address+"\"").intValue() * 2;
    }
}
