package a75f.io.device.mesh.hypersplit;

import static a75f.io.api.haystack.Tags.HYPERSTATSPLIT;
import static a75f.io.device.mesh.Pulse.getHumidityConversion;

import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Objects;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.constants.WhoFiledConstants;
import a75f.io.device.HyperSplit;
import a75f.io.device.mesh.DLog;
import a75f.io.device.mesh.DeviceHSUtil;
import a75f.io.device.mesh.DeviceUtil;
import a75f.io.device.mesh.LSerial;
import a75f.io.device.mesh.MeshUtil;
import a75f.io.device.mesh.Pulse;
import a75f.io.device.mesh.ThermistorUtil;
import a75f.io.device.serial.CcuToCmOverUsbDeviceTempAckMessage_t;
import a75f.io.device.serial.MessageType;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode;
import a75f.io.logic.bo.building.hvac.StandaloneFanStage;
import a75f.io.logic.bo.building.hyperstatsplit.common.PossibleConditioningMode;
import a75f.io.logic.bo.building.hyperstatsplit.common.PossibleFanMode;
import a75f.io.logic.bo.building.hyperstatsplit.common.HSSplitHaystackUtil;
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconEquip;
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconProfile;
import a75f.io.logic.bo.haystack.device.HyperStatSplitDevice;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.bo.util.TemperatureMode;
import a75f.io.logic.interfaces.ZoneDataInterface;
import a75f.io.logic.jobs.HyperStatSplitUserIntentHandler;

public class HyperSplitMsgReceiver {

    private static final int HYPERSPLIT_MESSAGE_ADDR_START_INDEX = 1;
    private static final int HYPERSPLIT_MESSAGE_ADDR_END_INDEX = 5;
    private static final int HYPERSPLIT_MESSAGE_TYPE_INDEX = 13;
    private static final int HYPERSPLIT_SERIALIZED_MESSAGE_START_INDEX = 17;
    private static ZoneDataInterface currentTempInterface = null;

    public static void processMessage(byte[] data, CCUHsApi hayStack) {
        try {

            /*
             * Message Type - 1 byte
             * Address - 4 bytes
             * CM lqi - 4 bytes
             * CM rssi - 4 bytes
             * Message types - 4 bytes
             *
             * Actual Serialized data starts at index 17.
             */
            CcuLog.e(L.TAG_CCU_DEVICE, "HyperSplitMsgReceiver processMessage processMessage :"+ Arrays.toString(data));

            byte[] addrArray = Arrays.copyOfRange(data, HYPERSPLIT_MESSAGE_ADDR_START_INDEX,
                    HYPERSPLIT_MESSAGE_ADDR_END_INDEX);
            int address = ByteBuffer.wrap(addrArray)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .getInt();

            MessageType messageType = MessageType.values()[data[HYPERSPLIT_MESSAGE_TYPE_INDEX]];

            byte[] messageArray = Arrays.copyOfRange(data, HYPERSPLIT_SERIALIZED_MESSAGE_START_INDEX, data.length);

                /*
                HyperSplit Messages arrive through the same serialized message as HyperStat messages.

                If a HyperStat/Split Serialized Message is received by the CCU,
                both the HyperStatMsgReceiver and HyperSplitMsgReceiver handler methods are called.

                So, skip evaluation here if the message contents are actually a HyperStat message.
            */
            if (messageType == MessageType.HYPERSTAT_REGULAR_UPDATE_MESSAGE
                    || messageType == MessageType.HYPERSTAT_IDU_STATUS_MESSAGE) {
                return;
            } else if (messageType == MessageType.HYPERSPLIT_REGULAR_UPDATE_MESSAGE) {
                HyperSplit.HyperSplitRegularUpdateMessage_t regularUpdate =
                        HyperSplit.HyperSplitRegularUpdateMessage_t.parseFrom(messageArray);

                handleRegularUpdate(regularUpdate, address, hayStack);
            } else if (messageType == MessageType.HYPERSTAT_LOCAL_CONTROLS_OVERRIDE_MESSAGE) {
                HyperSplit.HyperSplitLocalControlsOverrideMessage_t overrideMessage =
                        HyperSplit.HyperSplitLocalControlsOverrideMessage_t.parseFrom(messageArray);
                handleOverrideMessage(overrideMessage, address, hayStack);
            }

            // Add support for IDU here if VRV is picked up

        } catch (InvalidProtocolBufferException e) {
            CcuLog.e(L.TAG_CCU_DEVICE, "Cant parse protobuf data: "+e.getMessage());
        }
    }

    private static void handleRegularUpdate(HyperSplit.HyperSplitRegularUpdateMessage_t regularUpdateMessage, int nodeAddress,
                                            CCUHsApi hayStack) {
        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_DEVICE, "handleRegularUpdate: "+regularUpdateMessage.toString());
        }
        HashMap device = hayStack.read("device and addr == \"" + nodeAddress + "\"");
        String equipRef = (String)device.get("equipRef");

        Pulse.mDeviceUpdate.put((short) nodeAddress, Calendar.getInstance().getTimeInMillis());

        DeviceHSUtil.getEnabledSensorPointsWithRefForDevice(device, hayStack)
                .forEach( point -> writePortInputsToHaystackDatabase( point, regularUpdateMessage, hayStack, equipRef));

        writeSensorInputsToHaystackDatabase(regularUpdateMessage.getRegularUpdateCommon(), nodeAddress);

    }

    private static void writeSensorInputsToHaystackDatabase(HyperSplit.RegularUpdateCommon_t common, int nodeAddress) {

        HyperStatSplitDevice hss = new HyperStatSplitDevice(nodeAddress);

        /*
            Onboard sensor points that are not part of base HyperLite (e.g. PM2.5) are not created with the device.

            Point is only created when it appears in a regular update message.

            Regular update message
         */

        if (common.getPressure() > 0) {
            RawPoint sp = hss.getRawPoint(Port.SENSOR_PRESSURE);
            if (sp == null) sp = hss.createSensorPoints(Port.SENSOR_PRESSURE);
        }

        if (common.getPm2P5() > 0) {
            RawPoint sp = hss.getRawPoint(Port.SENSOR_PM2P5);
            if (sp == null) sp = hss.createSensorPoints(Port.SENSOR_PM2P5);
        }

        if (common.getPm10() > 0) {
            RawPoint sp = hss.getRawPoint(Port.SENSOR_PM10);
            if (sp == null) sp = hss.createSensorPoints(Port.SENSOR_PM10);
        }

        if (common.getUltravioletIndex() > 0) {
            RawPoint sp = hss.getRawPoint(Port.SENSOR_UVI);
            if (sp == null) sp = hss.createSensorPoints(Port.SENSOR_UVI);
        }

    }

    private static void handleOverrideMessage(HyperSplit.HyperSplitLocalControlsOverrideMessage_t message, int nodeAddress,
                                              CCUHsApi hayStack) {

        HashMap equipMap = CCUHsApi.getInstance().read("equip and group == \""+nodeAddress+"\"");
        Equip hsEquip = new Equip.Builder().setHashMap(equipMap).build();

        // HyperSplit and HyperStat local controls override messages share an index.
        // So, handler method has to be called for both types.
        // The HyperSplit message processing is only done if the equip has a "hyperstatsplit" tag.
        if (hsEquip.getMarkers().contains(HYPERSTATSPLIT)) {

            CcuLog.i(L.TAG_CCU_DEVICE, "OverrideMessage: "+message.toString());

            writeDesiredTemp(message, hsEquip, hayStack);
            updateFanConditioningMode(hsEquip.getId(),message.getFanSpeed().ordinal(),message.getConditioningModeValue(),hsEquip,nodeAddress);
            ZoneProfile profile = L.getProfile((short) nodeAddress);
            runProfileAlgo(profile,(short) nodeAddress);

            /** send the updated control  message*/
            HyperSplitMessageSender.sendControlMessage(nodeAddress, hsEquip.getId());
            sendAcknowledge(nodeAddress);

        }
    }

    private static void runProfileAlgo(ZoneProfile profile, short nodeAddress){

        if (profile instanceof HyperStatSplitCpuEconProfile) {
            HyperStatSplitCpuEconProfile hyperStatSplitCpuEconProfile = (HyperStatSplitCpuEconProfile) profile;
            hyperStatSplitCpuEconProfile.processHyperStatSplitCPUEconProfile((HyperStatSplitCpuEconEquip) Objects.requireNonNull(hyperStatSplitCpuEconProfile.getHyperStatSplitEquip(nodeAddress)));
        }

    }

    private static void writePortInputsToHaystackDatabase(RawPoint rawPoint,
                                                          HyperSplit.HyperSplitRegularUpdateMessage_t regularUpdateMessage,
                                                          CCUHsApi hayStack,
                                                          String equipRef) {
        Point point = DeviceHSUtil.getLogicalPointForRawPoint(rawPoint, hayStack);

        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_DEVICE,
                    "writePortInputsToHaystackDatabase: logical point for " + rawPoint.getDisplayName() + " " + point + "; port " + rawPoint.getPort());
        }
        if (point == null) {
            return;
        }
        if (Port.valueOf(rawPoint.getPort()) == Port.RSSI) {
            writeHeartbeat(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_RT) {
            writeRoomTemp(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.UNIVERSAL_IN_ONE) {
            writeUniversalInVal(rawPoint, point, hayStack,
                    regularUpdateMessage.getUniversalInput1Value());
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.UNIVERSAL_IN_TWO) {
            writeUniversalInVal(rawPoint, point, hayStack,
                    regularUpdateMessage.getUniversalInput2Value());
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.UNIVERSAL_IN_THREE) {
            writeUniversalInVal(rawPoint, point, hayStack,
                    regularUpdateMessage.getUniversalInput3Value());
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.UNIVERSAL_IN_FOUR) {
            writeUniversalInVal(rawPoint, point, hayStack,
                    regularUpdateMessage.getUniversalInput4Value());
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.UNIVERSAL_IN_FIVE) {
            writeUniversalInVal(rawPoint, point, hayStack,
                    regularUpdateMessage.getUniversalInput5Value());
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.UNIVERSAL_IN_SIX) {
            writeUniversalInVal(rawPoint, point, hayStack,
                    regularUpdateMessage.getUniversalInput6Value());
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.UNIVERSAL_IN_SEVEN) {
            writeUniversalInVal(rawPoint, point, hayStack,
                    regularUpdateMessage.getUniversalInput7Value());
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.UNIVERSAL_IN_EIGHT) {
            writeUniversalInVal(rawPoint, point, hayStack,
                    regularUpdateMessage.getUniversalInput8Value());
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_RH) {
            writeHumidityVal(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_ILLUMINANCE) {
            writeIlluminanceVal(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_OCCUPANCY) {
            writeOccupancyVal(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_CO2) {
            writeCO2Val(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_VOC) {
            writeVOCVal(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_SOUND) {
            writeSoundVal(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_PM2P5) {
            writePm2p5Val(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_PM10) {
            writePm10Val(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_MAT) {
            writeMixedAirTempSensorVal(rawPoint, point, regularUpdateMessage, hayStack, equipRef);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_MAH) {
            writeMixedAirHumiditySensorVal(rawPoint, point, regularUpdateMessage, hayStack, equipRef);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_SAT) {
            writeSupplyAirTempSensorVal(rawPoint, point, regularUpdateMessage, hayStack, equipRef);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_SAH) {
            writeSupplyAirHumiditySensorVal(rawPoint, point, regularUpdateMessage, hayStack, equipRef);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_OAT) {
            writeOutsideAirTempSensorVal(rawPoint, point, regularUpdateMessage, hayStack, equipRef);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_OAH) {
            writeOutsideAirHumiditySensorVal(rawPoint, point, regularUpdateMessage, hayStack, equipRef);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_PRESSURE) {
            writePressureSensorVal(rawPoint, point, regularUpdateMessage, hayStack, equipRef);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_UVI) {
            writeUviSensorVal(rawPoint, point, regularUpdateMessage, hayStack, equipRef);
        }
    }

    private static void writeSupplyAirTempSensorVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack, String equipRef) {

        if (isSensorBusAddressMapped(hayStack, "addr0", 0, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Temperature());
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Temperature()));
        }
        else if (isSensorBusAddressMapped(hayStack, "addr1", 0, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Temperature());
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Temperature()));
        }
        else if (isSensorBusAddressMapped(hayStack, "addr2", 0, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Temperature());
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Temperature()));
        }

        // Temperature/Humidity will never be mapped to Address 3 for HS Split

    }

    private static void writeSupplyAirHumiditySensorVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack, String equipRef) {

        if (isSensorBusAddressMapped(hayStack, "addr0", 0, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Humidity());
            hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Humidity()));
        }
        else if (isSensorBusAddressMapped(hayStack, "addr1", 0, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Humidity());
            hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Humidity()));
        }
        else if (isSensorBusAddressMapped(hayStack, "addr2", 0, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Humidity());
            hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Humidity()));
        }

        // Temperature/Humidity will never be mapped to Address 3 for HS Split

    }

    private static void writeMixedAirTempSensorVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack, String equipRef) {

        if (isSensorBusAddressMapped(hayStack, "addr0", 1, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Temperature());
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Temperature()));
        }
        else if (isSensorBusAddressMapped(hayStack, "addr1", 1, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Temperature());
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Temperature()));
        }
        else if (isSensorBusAddressMapped(hayStack, "addr2", 1, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Temperature());
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Temperature()));
        }

        // Temperature/Humidity will never be mapped to Address 3 for HS Split

    }

    private static void writeMixedAirHumiditySensorVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack, String equipRef) {

        if (isSensorBusAddressMapped(hayStack, "addr0", 1, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Humidity());
            hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Humidity()));
        }
        else if (isSensorBusAddressMapped(hayStack, "addr1", 1, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Humidity());
            hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Humidity()));
        }
        else if (isSensorBusAddressMapped(hayStack, "addr2", 1, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Humidity());
            hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Humidity()));
        }

        // Temperature/Humidity will never be mapped to Address 3 for HS Split

    }

    private static void writeOutsideAirTempSensorVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack, String equipRef) {

        if (isSensorBusAddressMapped(hayStack, "addr0", 2, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Temperature());
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Temperature()));
        }
        else if (isSensorBusAddressMapped(hayStack, "addr1", 2, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Temperature());
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Temperature()));
        }
        else if (isSensorBusAddressMapped(hayStack, "addr2", 2, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Temperature());
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Temperature()));
        }

        // Temperature/Humidity will never be mapped to Address 3 for HS Split

    }

    private static void writeOutsideAirHumiditySensorVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack, String equipRef) {

        if (isSensorBusAddressMapped(hayStack, "addr0", 2, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Humidity());
            hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Humidity()));
        }
        else if (isSensorBusAddressMapped(hayStack, "addr1", 2, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Humidity());
            hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Humidity()));
        }
        else if (isSensorBusAddressMapped(hayStack, "addr2", 2, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Humidity());
            hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Humidity()));
        }

        // Temperature/Humidity will never be mapped to Address 3 for HS Split

    }

    private static void writePressureSensorVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack, String equipRef) {

        // TODO: does pressure reading have a multiplier? Confirm.
        if (isSensorBusAddressMapped(hayStack, "addr3", 0, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress3Pressure());
            hayStack.writeHisValById(point.getId(), (double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress3Pressure());
        }

        // Pressure will only be mapped to Address 3 for HS Split

    }

    private static void writeUviSensorVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack, String equipRef) {

        hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getRegularUpdateCommon().getUltravioletIndex());
        hayStack.writeHisValById(point.getId(), (double) regularUpdateMessage.getRegularUpdateCommon().getUltravioletIndex());

    }

    private static boolean isSensorBusAddressMapped(CCUHsApi hsApi, String addr, int association, String equipRef) {

        if (hsApi.readDefaultVal("point and " + addr + " and config and sensorBus and enabled and equipRef == \"" + equipRef + "\"") == 1.0 &&
            hsApi.readDefaultVal("point and " + addr + " and config and sensorBus and association and equipRef == \"" + equipRef + "\"").intValue() == association) return true;

        return false;

    }

    private static void writeHeartbeat(RawPoint rawPoint, Point point,
                                       HyperSplit.HyperSplitRegularUpdateMessage_t regularUpdateMessage, CCUHsApi hayStack){
        hayStack.writeHisValueByIdWithoutCOV(rawPoint.getId(), (double)regularUpdateMessage.getRegularUpdateCommon().getRssi());
        hayStack.writeHisValueByIdWithoutCOV(point.getId(), (double)regularUpdateMessage.getRegularUpdateCommon().getRssi());
    }

    private static void writeRoomTemp(RawPoint rawPoint, Point point,
                                      HyperSplit.HyperSplitRegularUpdateMessage_t regularUpdateMessage, CCUHsApi hayStack) {

        hayStack.writeHisValById(rawPoint.getId(), (double) regularUpdateMessage.getRegularUpdateCommon().getRoomTemperature());

        double receivedRoomTemp = Pulse.getRoomTempConversion((double) regularUpdateMessage.getRegularUpdateCommon().getRoomTemperature());
        double curRoomTemp = hayStack.readHisValById(point.getId());
        if (curRoomTemp != receivedRoomTemp) {
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getRegularUpdateCommon().getRoomTemperature()));
            if (currentTempInterface != null) {
                currentTempInterface.updateTemperature(curRoomTemp, Short.parseShort(point.getGroup()));
            }
        }
    }

    private static void writeUniversalInVal(RawPoint rawPoint, Point point, CCUHsApi hayStack, int val) {
        Log.i(L.TAG_CCU_DEVICE, "writeUniversalInVal: "+rawPoint.getPort()+" " +rawPoint.getType());

        if (isUniversalInMappedToThermistor(rawPoint)) writeThermistorInVal(rawPoint, point, hayStack, val);
        else if (isUniversalInMappedToVoltage(rawPoint)) writeVoltageInVal(rawPoint, point, hayStack, val);
        else if (isUniversalInMappedToDigitalInNO(rawPoint)) writeDigitalNOInVal(rawPoint, point, hayStack, val);
        else if (isUniversalInMappedToDigitalInNC(rawPoint)) writeDigitalNCInVal(rawPoint, point, hayStack, val);
        else if (isUniversalInMappedToGenericResistiveIn(rawPoint)) writeGenericResistiveInVal(rawPoint, point, hayStack, val);

    }

    // If "analogType" == 5 (Supply Air Temperature), 6 (Mixed Air Temperature), or 7 (Outside Air Temperature), process the point as a thermistor
    // For HSS Universal Inputs, "analogType" tag maps to the UniversalInAssociation index, NOT the SensorManager index.
    private static boolean isUniversalInMappedToThermistor(RawPoint rawPoint) {

        return rawPoint.getEnabled()
                && (rawPoint.getType().equals("5")
                    || rawPoint.getType().equals("6")
                    || rawPoint.getType().equals("7"));
    }

    // If "analogType" == 14 (Generic Voltage), 0-4 (Current TX's), or 12-13 (Duct Pressures), process the point as 0-10V
    // For HSS Universal Inputs, "analogType" tag maps to the UniversalInAssociation index, NOT the SensorManager index.
    private static boolean isUniversalInMappedToVoltage(RawPoint rawPoint) {
        return rawPoint.getEnabled()
                && (rawPoint.getType().equals("0")
                    || rawPoint.getType().equals("1")
                    || rawPoint.getType().equals("2")
                    || rawPoint.getType().equals("3")
                    || rawPoint.getType().equals("4")
                    || rawPoint.getType().equals("12")
                    || rawPoint.getType().equals("13")
                    || rawPoint.getType().equals("14"));
    }

    // If "analogType" == 9 (Filter NO) or 11 (Condensate NO), process the point as a normally open digital input.
    // For HSS Universal Inputs, "analogType" tag maps to the UniversalInAssociation index, NOT the SensorManager index.
    private static boolean isUniversalInMappedToDigitalInNO(RawPoint rawPoint) {
        return rawPoint.getEnabled()
                && (rawPoint.getType().equals("9")
                || rawPoint.getType().equals("11"));
    }

    // If "analogType" == 8 (Filter NC) or 10 (Condensate NC), process the point as a normally closed digital input.
    // For HSS Universal Inputs, "analogType" tag maps to the UniversalInAssociation index, NOT the SensorManager index.
    private static boolean isUniversalInMappedToDigitalInNC(RawPoint rawPoint) {
        return rawPoint.getEnabled()
                && (rawPoint.getType().equals("8")
                || rawPoint.getType().equals("10"));
    }

    // If "analogType" == 1 (Generic Resistance), process the point as a raw resistive input.
    // For HSS Universal Inputs, "analogType" tag maps to the UniversalInAssociation index, NOT the SensorManager index.
    private static boolean isUniversalInMappedToGenericResistiveIn(RawPoint rawPoint) {
        return rawPoint.getEnabled() && rawPoint.getType().equals("15");
    }

    /*
        Bit [15] of int val should be a "1" to indicate thermistor UIN mapping.
        Bits [14-0] represent the thermistor reading (in tens of ohms).
     */
    private static void writeThermistorInVal(RawPoint rawPoint, Point point, CCUHsApi hayStack, int val) {
        // If 15th bit is "0", then HyperSplit says input is type "voltage" and not type "thermistor". That's a problem.
        if (getBit(val, 15) == 0) {
            Log.w(L.TAG_CCU_DEVICE, "Universal input " + rawPoint + " is mapped as thermistor in CCU, but stored as type Voltage in HyperStat.");
            return;
        } else {
            double ohmsReading = 10.0 * getBits(val, 0, 14);
            // Write physical point value in kOhms
            hayStack.writeHisValById(rawPoint.getId(), ohmsReading/1000);
            // Write logical point value based on thermistor table lookup
            hayStack.writeHisValById(point.getId(), CCUUtils.roundToOneDecimal(ThermistorUtil.getThermistorValueToTemp(ohmsReading)));
        }
    }

    /*
        Bit [15] of int val should be a "0" to indicate voltage UIN mapping.
        Bits [14-0] represent the voltage reading (in millivolts).
     */
    private static void writeVoltageInVal(RawPoint rawPoint, Point point, CCUHsApi hayStack, int val) {

        // If 15th bit is "1", then HyperSplit says input is type "thermistor" and not type "voltage". That's a problem.
        if (getBit(val, 15) == 1) {
            Log.w(L.TAG_CCU_DEVICE, "Universal input " + rawPoint + " is mapped as voltage in CCU, but stored as type Thermistor in HyperStat.");
            return;
        } else {
            double mvReading = getBits(val, 0, 14);
            // Write physical point value in mV
            hayStack.writeHisValById(rawPoint.getId(), mvReading);
            // Write logical point value after scaling to engineering units
            hayStack.writeHisValById(point.getId(), getUniversalInVoltageConversion(rawPoint, mvReading/1000));
        }
    }


    private static double getUniversalInVoltageConversion(RawPoint point, double volts) {

        // Current TX (0-10V = 0-10A)
        if (point.getType().equals("0")) {
            return volts;
        }
        // Current TX (0-10V = 0-20A)
        else if (point.getType().equals("1")) {
            return 2 * volts;
        }
        // Current TX (0-10V = 0-50A)
        else if (point.getType().equals("2")) {
            return 5 * volts;
        }
        // Current TX (0-10V = 0-100A)
        else if (point.getType().equals("3")) {
            return 10 * volts;
        }
        // Current TX (0-10V = 0-150A)
        else if (point.getType().equals("4")) {
            return 15 * volts;
        }
        // Pressure Sensor (0-10V = 0-1"wc)
        else if (point.getType().equals("12")) {
            return volts / 10;
        }
        // Pressure Sensor (0-10V = 0-2"wc)
        else if (point.getType().equals("13")) {
            return volts / 5;
        }
        // Else, leave logical point unscaled (Generic Voltage = Type 0)
        else {
            return volts;
        }

    }

    // TODO: verify that these bitwise operations work once we have a physical or virtual device. May need to reverse big-endian/little-endian somewhere in this method.
    /*
        Bit [15] of int val should be a "1" to indicate thermistor UIN mapping.
        Bits [14-0] represent the resistance reading (in tens of ohms).
        Following the door/window sensor convention: 10kOhm or above is "open", less than 10kOhm is "closed".
     */
    private static void writeDigitalNOInVal(RawPoint rawPoint, Point point, CCUHsApi hayStack, int val) {

        // If 15th bit is "0", then HyperSplit says input is type "voltage" and not type "thermistor". That's a problem.
        if (getBit(val, 15) == 0) {
            Log.w(L.TAG_CCU_DEVICE, "Universal input " + rawPoint + " is mapped as digital input in CCU, but stored as type Voltage in HyperStat.");
            return;
        } else {
            double ohmsReading = 10.0 * getBits(val, 0, 14);
            // Write physical point value in kOhms
            hayStack.writeHisValById(rawPoint.getId(), ohmsReading/1000);
            // Write logical point value. For normally open sensor, 10kOhm or above is "normal" (0), less than 10kOhm is "fault" (1)
            hayStack.writeHisValById(point.getId(), (ohmsReading >= 10000)? 0.0 : 1.0);
        }
    }

    // TODO: verify that these bitwise operations work once we have a physical or virtual device. May need to reverse big-endian/little-endian somewhere in this method.
    /*
        Bit [15] of int val should be a "1" to indicate thermistor UIN mapping.
        Bits [14-0] represent the resistance reading (in tens of ohms).
        Following the door/window sensor convention: 10kOhm or above is "open", less than 10kOhm is "closed".
     */
    private static void writeDigitalNCInVal(RawPoint rawPoint, Point point, CCUHsApi hayStack, int val) {

        // If 15th bit is "0", then HyperSplit says input is type "voltage" and not type "thermistor". That's a problem.
        if (getBit(val, 15) == 0) {
            Log.w(L.TAG_CCU_DEVICE, "Universal input " + rawPoint + " is mapped as digital input in CCU, but stored as type Voltage in HyperStat.");
            return;
        } else {
            double ohmsReading = 10.0 * getBits(val, 0, 14);
            // Write physical point value in kOhms
            hayStack.writeHisValById(rawPoint.getId(), ohmsReading/1000);
            // Write logical point value. For normally closed sensor, 10kOhm or above is "fault" (1), less than 10kOhm is "normal" (0)
            hayStack.writeHisValById(point.getId(), (ohmsReading < 10000)? 0.0 : 1.0);
        }
    }

    // TODO: verify that these bitwise operations work once we have a physical or virtual device. May need to reverse big-endian/little-endian somewhere in this method.
    /*
        Bit [15] of int val should be a "1" to indicate thermistor UIN mapping.
        Bits [14-0] represent the resistance reading (in tens of ohms).
        For Generic Resistance type, there is no scaling. Logical point value is the same as physical point value.
     */
    private static void writeGenericResistiveInVal(RawPoint rawPoint, Point point, CCUHsApi hayStack, int val) {

        // If 15th bit is "0", then HyperSplit says input is type "voltage" and not type "thermistor". That's a problem.
        if (getBit(val, 15) == 0) {
            Log.w(L.TAG_CCU_DEVICE, "Universal input " + rawPoint + " is mapped as digital input in CCU, but stored as type Voltage in HyperStat.");
            return;
        } else {
            double ohmsReading = 10.0 * getBits(val, 0, 14);
            // Write physical point value in kOhms
            hayStack.writeHisValById(rawPoint.getId(), ohmsReading/1000);
            // Write logical point value in kOhms
            hayStack.writeHisValById(point.getId(), ohmsReading/1000);
        }
    }

    private static int getBit(int n, int index) {
        return ((n >> index) & 1);
    }

    private static int getBits(int n, int start, int end) {
        return n & (((1 << (end-start + 1)) - 1) << start );
    }

    private static void writeHumidityVal(RawPoint rawPoint, Point point,
                                         HyperSplit.HyperSplitRegularUpdateMessage_t regularUpdateMessage, CCUHsApi hayStack) {
        hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getRegularUpdateCommon().getHumidity());
        hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getRegularUpdateCommon().getHumidity()));
    }

    private static void writeOccupancyVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack) {
        Log.i(L.TAG_CCU_DEVICE, "OccupantDetected : "+regularUpdateMessage.getRegularUpdateCommon().getOccupantDetected());
        hayStack.writeHisValById(rawPoint.getId(), regularUpdateMessage.getRegularUpdateCommon().getOccupantDetected()?1.0:0);
        hayStack.writeHisValById(point.getId(), regularUpdateMessage.getRegularUpdateCommon().getOccupantDetected()?1.0:0);
    }

    private static void writeIlluminanceVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack) {
        hayStack.writeHisValById(rawPoint.getId(), (double) (regularUpdateMessage.getRegularUpdateCommon().getIlluminance()));
        hayStack.writeHisValById(point.getId(), (double) (regularUpdateMessage.getRegularUpdateCommon().getIlluminance()));
    }

    private static void writeCO2Val(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack) {
        hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getRegularUpdateCommon().getCo2());
        hayStack.writeHisValById(point.getId(), (double) regularUpdateMessage.getRegularUpdateCommon().getCo2());
    }

    private static void writeVOCVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack) {
        hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getRegularUpdateCommon().getVoc());
        hayStack.writeHisValById(point.getId(), (double) regularUpdateMessage.getRegularUpdateCommon().getVoc());
    }

    private static void writeSoundVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack) {
        hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getRegularUpdateCommon().getSound());
        hayStack.writeHisValById(point.getId(), (double) regularUpdateMessage.getRegularUpdateCommon().getSound());
    }

    private static void writePm2p5Val(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack) {
        hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getRegularUpdateCommon().getPm2P5());
        hayStack.writeHisValById(point.getId(), (double) regularUpdateMessage.getRegularUpdateCommon().getPm2P5());
    }

    private static void writePm10Val(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack) {
        hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getRegularUpdateCommon().getPm10());
        hayStack.writeHisValById(point.getId(), (double) regularUpdateMessage.getRegularUpdateCommon().getPm10());
    }

    private static void writeDesiredTemp(HyperSplit.HyperSplitLocalControlsOverrideMessage_t message, Equip hsEquip,
                                         CCUHsApi hayStack) {
        int modeType = CCUHsApi.getInstance().readHisValByQuery("zone and hvacMode and roomRef" +
                " == \"" + hsEquip.getRoomRef() + "\"").intValue();
        TemperatureMode temperatureMode = TemperatureMode.values()[modeType];
        CcuLog.e(L.TAG_CCU_DEVICE, "Hypersplit Desired Temperature ModeType: " +
                TemperatureMode.values()[modeType]);
        double coolingDesiredTemp = (double) message.getSetTempCooling() / 2;
        double heatingDesiredTemp = (double) message.getSetTempHeating() / 2;
        double currentCoolingDesiredTemp = HyperSplitMessageGenerator.getDesiredTempCooling(hsEquip.getId(), temperatureMode);
        double currentHeatingDesiredTemp = HyperSplitMessageGenerator.getDesiredTempHeating(hsEquip.getId(), temperatureMode);

        if(currentHeatingDesiredTemp != heatingDesiredTemp || currentCoolingDesiredTemp != coolingDesiredTemp) {
            double averageDesiredTemp = (coolingDesiredTemp + heatingDesiredTemp)/2;

            HashMap coolingDtPoint = hayStack.read("point and temp and desired and cooling and sp and equipRef == \""
                    + hsEquip.getId() + "\"");
            if (!coolingDtPoint.isEmpty() && temperatureMode == TemperatureMode.COOLING) {
                CCUHsApi.getInstance().writeHisValById(coolingDtPoint.get("id").toString(), coolingDesiredTemp);
            } else {
                CcuLog.e(L.TAG_CCU_DEVICE, "coolingDtPoint does not exist: " + hsEquip.getDisplayName());
            }

            HashMap heatingDtPoint = hayStack.read("point and temp and desired and heating and sp and equipRef == \""
                    + hsEquip.getId() + "\"");
            if (!heatingDtPoint.isEmpty() && temperatureMode == TemperatureMode.HEATING) {
                CCUHsApi.getInstance().writeHisValById(heatingDtPoint.get("id").toString(), heatingDesiredTemp);
            } else {
                CcuLog.e(L.TAG_CCU_DEVICE, "heatingDtPoint does not exist: " + hsEquip.getDisplayName());
            }

            HashMap dtPoint = hayStack.read("point and temp and desired and average and sp and equipRef == \""
                    + hsEquip.getId() + "\"");
            if (!dtPoint.isEmpty()) {
                CCUHsApi.getInstance().writeHisValById(dtPoint.get("id").toString(), (double) message.getSetTempCooling());
            } else {
                CcuLog.e(L.TAG_CCU_DEVICE, "dtPoint does not exist: " + hsEquip.getDisplayName());
            }
            CcuLog.e(L.TAG_CCU_DEVICE, "coolingDesiredTemp " + coolingDesiredTemp + " heatingDesiredTemp " + heatingDesiredTemp + "  averageDesiredTemp " + averageDesiredTemp);

            switch (temperatureMode) {
                case DUAL:
                    DeviceUtil.updateDesiredTempFromDevice(new Point.Builder().setHashMap(coolingDtPoint).build(),
                            new Point.Builder().setHashMap(heatingDtPoint).build(),
                            new Point.Builder().setHashMap(dtPoint).build(),
                            coolingDesiredTemp, heatingDesiredTemp, averageDesiredTemp, hayStack,  WhoFiledConstants.HYPERSTAT_SPLIT_WHO);
                    break;
                case COOLING:
                    DeviceUtil.updateDesiredTempFromDevice(new Point.Builder().setHashMap(coolingDtPoint).build(),
                            new Point.Builder().setHashMap(heatingDtPoint).build(),
                            new Point.Builder().setHashMap(dtPoint).build(),
                            coolingDesiredTemp, 0, averageDesiredTemp, hayStack,  WhoFiledConstants.HYPERSTAT_SPLIT_WHO);
                    break;
                case HEATING:
                    DeviceUtil.updateDesiredTempFromDevice(new Point.Builder().setHashMap(coolingDtPoint).build(),
                            new Point.Builder().setHashMap(heatingDtPoint).build(),
                            new Point.Builder().setHashMap(dtPoint).build(),
                            0, heatingDesiredTemp, averageDesiredTemp, hayStack,  WhoFiledConstants.HYPERSTAT_SPLIT_WHO);
                    break;
            }
        }
    }

    public static void setCurrentTempInterface(ZoneDataInterface in) {
        currentTempInterface = in;
    }

    public static void updateConditioningMode(String equipId, int mode, PossibleConditioningMode possibleMode){
        Log.d("CCU_MODETEST", "updateConditioningMode: mode " + mode + ", possibleMode " + possibleMode);
        int conditioningMode;
        if (mode == HyperSplit.HyperSplitConditioningMode_e.HYPERSPLIT_CONDITIONING_MODE_AUTO.ordinal()){
            if (possibleMode != PossibleConditioningMode.BOTH) {
                Log.i(L.TAG_CCU_DEVICE, mode+" Invalid conditioning mode "); return;
            }
            conditioningMode = StandaloneConditioningMode.AUTO.ordinal();
        } else if (mode == HyperSplit.HyperSplitConditioningMode_e.HYPERSPLIT_CONDITIONING_MODE_HEATING.ordinal()){
            if (possibleMode == PossibleConditioningMode.COOLONLY) {
                Log.i(L.TAG_CCU_DEVICE, mode+"Invalid conditioning mode"); return;
            }
            conditioningMode = StandaloneConditioningMode.HEAT_ONLY.ordinal();
        } else if (mode == HyperSplit.HyperSplitConditioningMode_e.HYPERSPLIT_CONDITIONING_MODE_COOLING.ordinal()){
            if (possibleMode == PossibleConditioningMode.HEATONLY) {
                Log.i(L.TAG_CCU_DEVICE, mode+"Invalid conditioning mode"); return;
            }
            conditioningMode = StandaloneConditioningMode.COOL_ONLY.ordinal();
        } else {
            if (mode != PossibleConditioningMode.OFF.ordinal()) {
                Log.i(L.TAG_CCU_DEVICE, mode+" Invalid conditioning mode "); return;
            }
            conditioningMode = StandaloneConditioningMode.OFF.ordinal();
        }
        // TODO: check how this is reflected in UI
        HyperStatSplitUserIntentHandler.Companion.updateHyperStatSplitUIPoints(equipId,
                "zone and sp and conditioning and mode", conditioningMode, WhoFiledConstants.HYPERSTAT_SPLIT_WHO);
    }

    private static void sendAcknowledge(int address){
        Log.i(L.TAG_CCU_DEVICE, "Sending Acknowledgement");
        if (!LSerial.getInstance().isConnected()) {
            CcuLog.d(L.TAG_CCU_DEVICE,"Device not connected !!");
            return;
        }
        CcuToCmOverUsbDeviceTempAckMessage_t msg = new CcuToCmOverUsbDeviceTempAckMessage_t();
        msg.messageType.set(MessageType.CCU_TO_CM_OVER_USB_SN_SET_TEMPERATURE_ACK);
        msg.smartNodeAddress.set(address);
        MeshUtil.sendStructToCM(msg);
    }

    public static void updateFanMode(String equipId, int mode, PossibleFanMode possibleMode){
        int fanMode = getLogicalFanMode(possibleMode,mode);
        if(fanMode!= -1) {
            HyperStatSplitUserIntentHandler.Companion.updateHyperStatSplitUIPoints(
                    equipId, "zone and sp and fan and operation and mode", fanMode, WhoFiledConstants.HYPERSTAT_SPLIT_WHO);
        }

    }

    public static void updateFanConditioningMode(String equipId, int fanMode,int conditioningMode, Equip equip, int nodeAddress){
        PossibleConditioningMode possibleConditioningMode = PossibleConditioningMode.OFF;
        PossibleFanMode possibleFanMode = PossibleFanMode.OFF;

        if(equip.getProfile().equalsIgnoreCase(ProfileType.HYPERSTATSPLIT_CPU.name())){
            possibleConditioningMode = HSSplitHaystackUtil.Companion.getPossibleConditioningModeSettings(nodeAddress);
            possibleFanMode = HSSplitHaystackUtil.Companion.getPossibleFanModeSettings(nodeAddress);
        }
        
        updateFanMode(equipId,fanMode,possibleFanMode);
        updateConditioningMode(equipId,conditioningMode,possibleConditioningMode);
    }

    private static int getLogicalFanMode(PossibleFanMode mode,int selectedMode){
        Log.i(L.TAG_CCU_DEVICE, "PossibleFanMode: "+mode + " selectedMode  "+selectedMode);
        if(selectedMode == 0 ) return StandaloneFanStage.OFF.ordinal();
        switch (mode){
            case OFF:
                if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_OFF.ordinal() == selectedMode ) {
                    return StandaloneFanStage.OFF.ordinal();
                } else {
                    Log.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }
            case LOW:
                if(selectedMode == HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                }else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_LOW.ordinal() == selectedMode ) {
                    return StandaloneFanStage.LOW_CUR_OCC.ordinal();
                } else {
                    Log.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }
            case MED:
                if(selectedMode == HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                }else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_MED.ordinal() == selectedMode ) {
                    return StandaloneFanStage.MEDIUM_CUR_OCC.ordinal();
                } else {
                    Log.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }
            case HIGH:
                if(selectedMode == HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                } else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_HIGH.ordinal() == selectedMode ) {
                    return StandaloneFanStage.HIGH_CUR_OCC.ordinal();
                } else {
                    Log.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }

            case LOW_MED:
                if(selectedMode == HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                } else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_LOW.ordinal() == selectedMode ) {
                    return StandaloneFanStage.LOW_CUR_OCC.ordinal();
                } else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_MED.ordinal() == selectedMode ) {
                    return StandaloneFanStage.MEDIUM_CUR_OCC.ordinal();
                } else {
                    Log.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }

            case LOW_HIGH:
                if(selectedMode == HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                } else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_LOW.ordinal() == selectedMode ) {
                    return StandaloneFanStage.LOW_CUR_OCC.ordinal();
                } else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_HIGH.ordinal() == selectedMode ) {
                    return StandaloneFanStage.HIGH_CUR_OCC.ordinal();
                } else {
                    Log.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }

            case MED_HIGH:
                if(selectedMode == HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                } else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_MED.ordinal() == selectedMode ) {
                    return StandaloneFanStage.MEDIUM_CUR_OCC.ordinal();
                } else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_HIGH.ordinal() == selectedMode ) {
                    return StandaloneFanStage.HIGH_CUR_OCC.ordinal();
                } else {
                    Log.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }
            case LOW_MED_HIGH:
                if(selectedMode == HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                } else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_LOW.ordinal() == selectedMode ) {
                    return StandaloneFanStage.LOW_CUR_OCC.ordinal();
                } else if(selectedMode == HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_MED.ordinal()){
                    return StandaloneFanStage.MEDIUM_CUR_OCC.ordinal();
                } else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_HIGH.ordinal() == selectedMode ) {
                    return StandaloneFanStage.HIGH_CUR_OCC.ordinal();
                } else {
                    Log.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }
        }
        return -1;
    }
    
}
