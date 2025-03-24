package a75f.io.device.mesh.hypersplit;

import static a75f.io.api.haystack.Tags.HYPERSTATSPLIT;
import static a75f.io.device.mesh.Pulse.getHumidityConversion;
import static a75f.io.logic.bo.util.CCUUtils.isCurrentTemperatureWithinLimits;

import com.google.protobuf.InvalidProtocolBufferException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import a75f.io.domain.api.DomainName;
import a75f.io.logger.CcuLog;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.Port;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.StandaloneConditioningMode;
import a75f.io.logic.bo.building.hvac.StandaloneFanStage;
import a75f.io.logic.bo.building.hyperstatsplit.common.PossibleConditioningMode;
import a75f.io.logic.bo.building.hyperstatsplit.common.PossibleFanMode;
import a75f.io.logic.bo.building.hyperstatsplit.common.HSSplitHaystackUtil;
import a75f.io.logic.bo.building.hyperstatsplit.profiles.cpuecon.HyperStatSplitCpuEconProfile;
import a75f.io.logic.bo.haystack.device.HyperStatSplitDevice;
import a75f.io.logic.bo.util.CCUUtils;
import a75f.io.logic.bo.util.TemperatureMode;
import a75f.io.logic.interfaces.ZoneDataInterface;
import a75f.io.logic.util.uiutils.HyperStatSplitUserIntentHandler;

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
           if (messageType == MessageType.HYPERSPLIT_REGULAR_UPDATE_MESSAGE) {
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
        if(Globals.getInstance().isTemporaryOverrideMode()) {
            Pulse.updateRssiPointIfAvailable(hayStack, device.get("id").toString(), regularUpdateMessage.getRegularUpdateCommon().getRssi(), regularUpdateMessage.getRegularUpdateCommon().getRssi(), nodeAddress);
            return;
        }
        DeviceHSUtil.getEnabledSensorPointsWithRefForDevice(device, hayStack)
                .forEach(point -> writePortInputsToHaystackDatabase(point, regularUpdateMessage, hayStack, equipRef));
        // PM2.5 sensor is the only DYNAMIC_SENSOR point for HSS
        if (regularUpdateMessage.getRegularUpdateCommon().getPm2P5() > 0.0) {
            HashMap<Object, Object> pm25PhyPoint = hayStack.readEntity("point and domainName == \"" + DomainName.pm25Sensor + "\" and deviceRef == \"" + device.get("id").toString() + "\"");
            RawPoint sp = new RawPoint.Builder().setHashMap(pm25PhyPoint).build();

            if (sp.getPointRef() == null || sp.getPointRef().isEmpty()) {
                HyperStatSplitDevice stat = new HyperStatSplitDevice();
                sp = stat.addEquipSensorFromRawPoint(sp, Port.SENSOR_PM2P5, nodeAddress);
            }
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
            hyperStatSplitCpuEconProfile.updateZonePoints();
        }

    }

    private static void writePortInputsToHaystackDatabase(RawPoint rawPoint,
                                                          HyperSplit.HyperSplitRegularUpdateMessage_t regularUpdateMessage,
                                                          CCUHsApi hayStack,
                                                          String equipRef) {
        Point point = DeviceHSUtil.getLogicalPointForRawPoint(rawPoint, hayStack);

        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_DEVICE,
                    "writePortInputsToHaystackDatabase: logical point for " + rawPoint.getDisplayName() + " " + point + "; domainName " + rawPoint.getDomainName());
        }
        if (point == null || rawPoint.getDomainName() == null || rawPoint.getDomainName().isEmpty()) {
            return;
        }
        if (rawPoint.getDomainName().equals(DomainName.rssi)) {
            writeHeartbeat(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (rawPoint.getDomainName().equals(DomainName.currentTemp)) {
            writeRoomTemp(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (rawPoint.getDomainName().equals(DomainName.universal1In)) {
            writeUniversalInVal(rawPoint, point, hayStack,
                    regularUpdateMessage.getUniversalInput1Value());
        }
        else if (rawPoint.getDomainName().equals(DomainName.universal2In)) {
            writeUniversalInVal(rawPoint, point, hayStack,
                    regularUpdateMessage.getUniversalInput2Value());
        }
        else if (rawPoint.getDomainName().equals(DomainName.universal3In)) {
            writeUniversalInVal(rawPoint, point, hayStack,
                    regularUpdateMessage.getUniversalInput3Value());
        }
        else if (rawPoint.getDomainName().equals(DomainName.universal4In)) {
            writeUniversalInVal(rawPoint, point, hayStack,
                    regularUpdateMessage.getUniversalInput4Value());
        }
        else if (rawPoint.getDomainName().equals(DomainName.universal5In)) {
            writeUniversalInVal(rawPoint, point, hayStack,
                    regularUpdateMessage.getUniversalInput5Value());
        }
        else if (rawPoint.getDomainName().equals(DomainName.universal6In)) {
            writeUniversalInVal(rawPoint, point, hayStack,
                    regularUpdateMessage.getUniversalInput6Value());
        }
        else if (rawPoint.getDomainName().equals(DomainName.universal7In)) {
            writeUniversalInVal(rawPoint, point, hayStack,
                    regularUpdateMessage.getUniversalInput7Value());
        }
        else if (rawPoint.getDomainName().equals(DomainName.universal8In)) {
            writeUniversalInVal(rawPoint, point, hayStack,
                    regularUpdateMessage.getUniversalInput8Value());
        }
        else if (rawPoint.getDomainName().equals(DomainName.humiditySensor)) {
            writeHumidityVal(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (rawPoint.getDomainName().equals(DomainName.illuminanceSensor)) {
            writeIlluminanceVal(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (rawPoint.getDomainName().equals(DomainName.occupancySensor)) {
            writeOccupancyVal(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (rawPoint.getDomainName().equals(DomainName.co2Sensor)) {
            writeCO2Val(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (rawPoint.getDomainName().equals(DomainName.soundSensor)) {
            writeSoundVal(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (rawPoint.getDomainName().equals(DomainName.pm25Sensor)) {
            writePm2p5Val(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (rawPoint.getDomainName().equals(DomainName.pm10Sensor)) {
            writePm10Val(rawPoint, point, regularUpdateMessage, hayStack);
        }
        else if (rawPoint.getDomainName().equals(DomainName.mixedAirTempSensor)) {
            writeMixedAirTempSensorVal(rawPoint, point, regularUpdateMessage, hayStack, equipRef);
        }
        else if (rawPoint.getDomainName().equals(DomainName.mixedAirHumiditySensor)) {
            writeMixedAirHumiditySensorVal(rawPoint, point, regularUpdateMessage, hayStack, equipRef);
        }
        else if (rawPoint.getDomainName().equals(DomainName.supplyAirTemperature)) {
            writeSupplyAirTempSensorVal(rawPoint, point, regularUpdateMessage, hayStack, equipRef);
        }
        else if (rawPoint.getDomainName().equals(DomainName.supplyAirHumiditySensor)) {
            writeSupplyAirHumiditySensorVal(rawPoint, point, regularUpdateMessage, hayStack, equipRef);
        }
        else if (rawPoint.getDomainName().equals(DomainName.outsideAirTempSensor)) {
            writeOutsideAirTempSensorVal(rawPoint, point, regularUpdateMessage, hayStack, equipRef);
        }
        else if (rawPoint.getDomainName().equals(DomainName.outsideAirHumiditySensor)) {
            writeOutsideAirHumiditySensorVal(rawPoint, point, regularUpdateMessage, hayStack, equipRef);
        }
        else if (rawPoint.getDomainName().equals(DomainName.ductStaticPressureSensor)) {
            writePressureSensorVal(rawPoint, point, regularUpdateMessage, hayStack, equipRef);
        }
        else if (Port.valueOf(rawPoint.getPort()) == Port.SENSOR_UVI) {
            writeUviSensorVal(rawPoint, point, regularUpdateMessage, hayStack);
        } else {
            CcuLog.d(L.TAG_CCU_DEVICE, "No handler for domainName: " + rawPoint.getDomainName());
        }
    }

    private static void writeSupplyAirTempSensorVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack, String equipRef) {

        if (isSensorBusAddressMapped(hayStack, 0, 0, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Temperature());
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Temperature()));
        }
        else if (isSensorBusAddressMapped(hayStack, 1, 0, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Temperature());
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Temperature()));
        }
        else if (isSensorBusAddressMapped(hayStack, 2, 0, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Temperature());
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Temperature()));
        }

        // Temperature/Humidity will never be mapped to Address 3 for HS Split

    }

    private static void writeSupplyAirHumiditySensorVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack, String equipRef) {

        if (isSensorBusAddressMapped(hayStack, 0, 0, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Humidity());
            hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Humidity()));
        }
        else if (isSensorBusAddressMapped(hayStack, 1, 0, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Humidity());
            hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Humidity()));
        }
        else if (isSensorBusAddressMapped(hayStack, 2, 0, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Humidity());
            hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Humidity()));
        }

        // Temperature/Humidity will never be mapped to Address 3 for HS Split

    }

    private static void writeMixedAirTempSensorVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack, String equipRef) {

        if (isSensorBusAddressMapped(hayStack, 0, 1, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Temperature());
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Temperature()));
        }
        else if (isSensorBusAddressMapped(hayStack, 1, 1, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Temperature());
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Temperature()));
        }
        else if (isSensorBusAddressMapped(hayStack, 2, 1, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Temperature());
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Temperature()));
        }

        // Temperature/Humidity will never be mapped to Address 3 for HS Split

    }

    private static void writeMixedAirHumiditySensorVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack, String equipRef) {

        if (isSensorBusAddressMapped(hayStack, 0, 1, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Humidity());
            hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Humidity()));
        }
        else if (isSensorBusAddressMapped(hayStack, 1, 1, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Humidity());
            hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Humidity()));
        }
        else if (isSensorBusAddressMapped(hayStack, 2, 1, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Humidity());
            hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Humidity()));
        }

        // Temperature/Humidity will never be mapped to Address 3 for HS Split

    }

    private static void writeOutsideAirTempSensorVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack, String equipRef) {

        if (isSensorBusAddressMapped(hayStack, 0, 2, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Temperature());
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Temperature()));
        }
        else if (isSensorBusAddressMapped(hayStack, 1, 2, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Temperature());
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Temperature()));
        }
        else if (isSensorBusAddressMapped(hayStack, 2, 2, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Temperature());
            hayStack.writeHisValById(point.getId(), Pulse.getRoomTempConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Temperature()));
        }

        // Temperature/Humidity will never be mapped to Address 3 for HS Split

    }

    private static void writeOutsideAirHumiditySensorVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack, String equipRef) {

        if (isSensorBusAddressMapped(hayStack, 0, 2, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Humidity());
            hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Humidity()));
        }
        else if (isSensorBusAddressMapped(hayStack, 1, 2, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Humidity());
            hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress1Humidity()));
        }
        else if (isSensorBusAddressMapped(hayStack, 2, 2, equipRef)) {
            hayStack.writeHisValById(rawPoint.getId(), (double)regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Humidity());
            hayStack.writeHisValById(point.getId(), getHumidityConversion((double) regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress2Humidity()));
        }

        // Temperature/Humidity will never be mapped to Address 3 for HS Split

    }

    private static void writePressureSensorVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack, String equipRef) {

        if (isPressureSensorBusEnabled(hayStack, equipRef)) {
            // physical and logical points are in units of in wc. Regular update message comes in Pa.
            // Unit Conversion = 1 in wc / 248.8 Pa
            double pressureInWc = regularUpdateMessage.getSensorBusReadingsConnect().getSensorAddress0Pressure() / 248.8;
            hayStack.writeHisValById(rawPoint.getId(), (double) pressureInWc);
            hayStack.writeHisValById(point.getId(), (double) pressureInWc);
        }

        // Pressure will only be mapped to Address 3 for HS Split

    }

    private static void writeUviSensorVal(RawPoint rawPoint, Point point, HyperSplit.HyperSplitRegularUpdateMessage_t
            regularUpdateMessage, CCUHsApi hayStack) {

        hayStack.writeHisValById(rawPoint.getId(), (double) regularUpdateMessage.getRegularUpdateCommon().getUltravioletIndex());
        hayStack.writeHisValById(point.getId(), (double) regularUpdateMessage.getRegularUpdateCommon().getUltravioletIndex());

    }

    private static boolean isSensorBusAddressMapped(CCUHsApi hsApi, int addr, int association, String equipRef) {

        switch (addr) {
            case 0:
                if (hsApi.readDefaultVal("point and domainName == \"" + DomainName.sensorBusAddress0Enable + "\" and equipRef == \"" + equipRef + "\"") == 1.0 &&
                        hsApi.readDefaultVal("point and domainName == \"" + DomainName.temperatureSensorBusAdd0 + "\" and equipRef == \"" + equipRef + "\"").intValue() == association) return true;
                break;
            case 1:
                if (hsApi.readDefaultVal("point and domainName == \"" + DomainName.sensorBusAddress1Enable + "\" and equipRef == \"" + equipRef + "\"") == 1.0 &&
                        hsApi.readDefaultVal("point and domainName == \"" + DomainName.temperatureSensorBusAdd1 + "\" and equipRef == \"" + equipRef + "\"").intValue() == association) return true;
                break;
            case 2:
                if (hsApi.readDefaultVal("point and domainName == \"" + DomainName.sensorBusAddress2Enable + "\" and equipRef == \"" + equipRef + "\"") == 1.0 &&
                        hsApi.readDefaultVal("point and domainName == \"" + DomainName.temperatureSensorBusAdd2 + "\" and equipRef == \"" + equipRef + "\"").intValue() == association) return true;
                break;
            case 3:
                if (hsApi.readDefaultVal("point and domainName == \"" + DomainName.sensorBusAddress3Enable + "\" and equipRef == \"" + equipRef + "\"") == 1.0 &&
                        hsApi.readDefaultVal("point and domainName == \"" + DomainName.pressureSensorBusAdd3 + "\" and equipRef == \"" + equipRef + "\"").intValue() == association) return true;
                break;
        }
        return false;

    }

    private static boolean isPressureSensorBusEnabled(CCUHsApi hsApi, String equipRef) {
        return hsApi.readDefaultVal("point and domainName == \"" + DomainName.sensorBusPressureEnable + "\" and equipRef == \"" + equipRef + "\"") == 1.0
                && hsApi.readDefaultVal("point and domainName == \"" + DomainName.pressureSensorBusAdd0 + "\" and equipRef == \"" + equipRef + "\"") > 0.0;
    }

    private static void writeHeartbeat(RawPoint rawPoint, Point point,
                                       HyperSplit.HyperSplitRegularUpdateMessage_t regularUpdateMessage, CCUHsApi hayStack){
        hayStack.writeHisValueByIdWithoutCOV(rawPoint.getId(), (double)regularUpdateMessage.getRegularUpdateCommon().getRssi());
        hayStack.writeHisValueByIdWithoutCOV(point.getId(), 1.0);
        if (currentTempInterface != null) {
            currentTempInterface.refreshHeartBeatStatus(String.valueOf((point.getGroup())));
        }
    }

    private static void writeRoomTemp(RawPoint rawPoint, Point point,
                                      HyperSplit.HyperSplitRegularUpdateMessage_t regularUpdateMessage, CCUHsApi hayStack) {
        Double currentTemp = Pulse.getRoomTempConversion((double) regularUpdateMessage.getRegularUpdateCommon().getRoomTemperature());
        HashMap<Object, Object> currentTempPoint = hayStack.readMapById(rawPoint.getId());
      if(isCurrentTemperatureWithinLimits(currentTemp/10,currentTempPoint)) {
          hayStack.writeHisValById(rawPoint.getId(), currentTemp);
          double curRoomTemp = hayStack.readHisValById(point.getId());
          hayStack.writeHisValById(point.getId(), currentTemp);
          if (currentTempInterface != null) {
              currentTempInterface.updateTemperature(curRoomTemp, Short.parseShort(point.getGroup()));
          }
      }
      else {
          CcuLog.d(L.TAG_CCU_DEVICE,"Invalid Current Temp "+ currentTemp);
      }
    }

    private static void writeUniversalInVal(RawPoint rawPoint, Point point, CCUHsApi hayStack, int val) {
        CcuLog.i(L.TAG_CCU_DEVICE, "writeUniversalInVal: "+rawPoint.getDomainName()+" " +rawPoint.getType());

        if (rawPoint.getEnabled() && (thermistorPoints.contains(point.getDomainName()))) writeThermistorInVal(rawPoint, point, hayStack, val);
        else if (rawPoint.getEnabled() && (voltagePoints.containsKey(point.getDomainName()))) writeVoltageInVal(rawPoint, point, hayStack, val);
        else if (rawPoint.getEnabled() && (digitalNoPoints.contains(point.getDomainName()))) writeDigitalNOInVal(rawPoint, point, hayStack, val);
        else if (rawPoint.getEnabled() && (digitalNcPoints.contains(point.getDomainName()))) writeDigitalNCInVal(rawPoint, point, hayStack, val);

    }

    private static List<String> thermistorPoints = Arrays.asList(
            DomainName.thermistorInput,
            DomainName.dischargeAirTemperature,
            DomainName.mixedAirTemperature,
            DomainName.outsideTemperature
    );

    private static Map<String, MinMaxVoltage> voltagePoints;
    static {
        Map<String, MinMaxVoltage> pointsMap = new HashMap();
        pointsMap.put(DomainName.voltageInput, new MinMaxVoltage(0, 10, 0, 10));
        pointsMap.put(DomainName.ductStaticPressureSensor1_1, new MinMaxVoltage(0, 10, 0, 1));
        pointsMap.put(DomainName.ductStaticPressureSensor1_2, new MinMaxVoltage(0, 10, 0, 2));
        pointsMap.put(DomainName.ductStaticPressureSensor1_10, new MinMaxVoltage(0, 10, 0, 10));
        pointsMap.put(DomainName.buildingStaticPressureSensor_1, new MinMaxVoltage(0, 10, 0, 1));
        pointsMap.put(DomainName.buildingStaticPressureSensor_2, new MinMaxVoltage(0, 10, 0, 2));
        pointsMap.put(DomainName.buildingStaticPressureSensor_10, new MinMaxVoltage(0, 10, 0, 10));
        pointsMap.put(DomainName.outsideAirDamperFeedback, new MinMaxVoltage(0, 10, 0, 100));
        pointsMap.put(DomainName.currentTx10, new MinMaxVoltage(0, 10, 0, 10));
        pointsMap.put(DomainName.currentTx20, new MinMaxVoltage(0, 10, 0, 20));
        pointsMap.put(DomainName.currentTx30, new MinMaxVoltage(0, 10, 0, 30));
        pointsMap.put(DomainName.currentTx50, new MinMaxVoltage(0, 10, 0, 50));
        pointsMap.put(DomainName.currentTx60, new MinMaxVoltage(0, 10, 0, 60));
        pointsMap.put(DomainName.currentTx100, new MinMaxVoltage(0, 10, 0, 100));
        pointsMap.put(DomainName.currentTx120, new MinMaxVoltage(0, 10, 0, 120));
        pointsMap.put(DomainName.currentTx150, new MinMaxVoltage(0, 10, 0, 150));
        pointsMap.put(DomainName.currentTx200, new MinMaxVoltage(0, 10, 0, 200));
        voltagePoints = Collections.unmodifiableMap(pointsMap);
    }

    private static List<String> digitalNoPoints = Arrays.asList(
            DomainName.dischargeFanAMStatus,
            DomainName.dischargeFanRunStatus,
            DomainName.dischargeFanTripStatus,
            DomainName.exhaustFanRunStatus,
            DomainName.exhaustFanTripStatus,
            DomainName.filterStatusNO,
            DomainName.condensateStatusNO,
            DomainName.fireAlarmStatus,
            DomainName.highDifferentialPressureSwitch,
            DomainName.lowDifferentialPressureSwitch,
            DomainName.condensateStatusNO,
            DomainName.emergencyShutoffNO,
            DomainName.genericAlarmNO
    );

    private static List<String> digitalNcPoints = Arrays.asList(
            DomainName.filterStatusNC,
            DomainName.condensateStatusNC,
            DomainName.emergencyShutoffNC,
            DomainName.genericAlarmNC
    );

    /*
        Bit [15] of int val should be a "1" to indicate thermistor UIN mapping.
        Bits [14-0] represent the thermistor reading (in tens of ohms).
     */
    private static void writeThermistorInVal(RawPoint rawPoint, Point point, CCUHsApi hayStack, int val) {
        // If 15th bit is "0", then HyperSplit says input is type "voltage" and not type "thermistor". That's a problem.
        if (getBit(val, 15) == 0) {
            CcuLog.w(L.TAG_CCU_DEVICE, "Universal input " + rawPoint + " is mapped as thermistor in CCU, but stored as type Voltage in HyperStat.");
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
            CcuLog.w(L.TAG_CCU_DEVICE, "Universal input " + rawPoint + " is mapped as voltage in CCU, but stored as type Thermistor in HyperStat.");
            return;
        } else {
            double mvReading = getBits(val, 0, 14);
            // Write physical point value in mV
            hayStack.writeHisValById(rawPoint.getId(), mvReading);
            // Write logical point value after scaling to engineering units
            hayStack.writeHisValById(point.getId(), getUniversalInVoltageConversion(point, mvReading/1000));
        }
    }


    private static double getUniversalInVoltageConversion(Point point, double volts) {
        MinMaxVoltage minMaxVoltage = voltagePoints.get(point.getDomainName());
        if (voltagePoints.containsKey(point.getDomainName())) {
            // This should never happen, because we are hard-coding the min/max voltages right now.
            if (minMaxVoltage.maxVoltage == minMaxVoltage.minVoltage) return volts;

            return volts * (minMaxVoltage.maxEngVal - minMaxVoltage.minEngVal) / (minMaxVoltage.maxVoltage - minMaxVoltage.minVoltage);
        }

        return volts;
    }

    /*
        Bit [15] of int val should be a "1" to indicate thermistor UIN mapping.
        Bits [14-0] represent the resistance reading (in tens of ohms).
        Following the door/window sensor convention: 10kOhm or above is "open", less than 10kOhm is "closed".
     */
    private static void writeDigitalNOInVal(RawPoint rawPoint, Point point, CCUHsApi hayStack, int val) {

        // If 15th bit is "0", then HyperSplit says input is type "voltage" and not type "thermistor". That's a problem.
        if (getBit(val, 15) == 0) {
            CcuLog.w(L.TAG_CCU_DEVICE, "Universal input " + rawPoint + " is mapped as digital input in CCU, but stored as type Voltage in HyperStat.");
            return;
        } else {
            double ohmsReading = 10.0 * getBits(val, 0, 14);
            // Write physical point value in kOhms
            hayStack.writeHisValById(rawPoint.getId(), ohmsReading/1000);
            // Write logical point value. For normally open sensor, 10kOhm or above is "normal" (0), less than 10kOhm is "fault" (1)
            hayStack.writeHisValById(point.getId(), (ohmsReading >= 10000)? 0.0 : 1.0);
        }
    }

    /*
        Bit [15] of int val should be a "1" to indicate thermistor UIN mapping.
        Bits [14-0] represent the resistance reading (in tens of ohms).
        Following the door/window sensor convention: 10kOhm or above is "open", less than 10kOhm is "closed".
     */
    private static void writeDigitalNCInVal(RawPoint rawPoint, Point point, CCUHsApi hayStack, int val) {

        // If 15th bit is "0", then HyperSplit says input is type "voltage" and not type "thermistor". That's a problem.
        if (getBit(val, 15) == 0) {
            CcuLog.w(L.TAG_CCU_DEVICE, "Universal input " + rawPoint + " is mapped as digital input in CCU, but stored as type Voltage in HyperStat.");
            return;
        } else {
            double ohmsReading = 10.0 * getBits(val, 0, 14);
            // Write physical point value in kOhms
            hayStack.writeHisValById(rawPoint.getId(), ohmsReading/1000);
            // Write logical point value. For normally closed sensor, 10kOhm or above is "fault" (1), less than 10kOhm is "normal" (0)
            hayStack.writeHisValById(point.getId(), (ohmsReading < 10000)? 0.0 : 1.0);
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
        CcuLog.i(L.TAG_CCU_DEVICE, "OccupantDetected : "+regularUpdateMessage.getRegularUpdateCommon().getOccupantDetected());
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
        double currentCoolingDesiredTemp = HyperSplitMessageGenerator.getDesiredTempCooling(hsEquip.getId());
        double currentHeatingDesiredTemp = HyperSplitMessageGenerator.getDesiredTempHeating(hsEquip.getId());

        if(currentHeatingDesiredTemp != heatingDesiredTemp || currentCoolingDesiredTemp != coolingDesiredTemp) {
            double averageDesiredTemp = (coolingDesiredTemp + heatingDesiredTemp)/2;

            HashMap coolingDtPoint = hayStack.readEntity("point and domainName == \"" + DomainName.desiredTempCooling + "\" and equipRef == \""
                    + hsEquip.getId() + "\"");
            if (!coolingDtPoint.isEmpty() && temperatureMode == TemperatureMode.COOLING) {
                CCUHsApi.getInstance().writeHisValById(coolingDtPoint.get("id").toString(), coolingDesiredTemp);
            } else {
                CcuLog.e(L.TAG_CCU_DEVICE, "coolingDtPoint does not exist: " + hsEquip.getDisplayName());
            }

            HashMap heatingDtPoint = hayStack.read("point and domainName == \"" + DomainName.desiredTempHeating + "\" and equipRef == \""
                    + hsEquip.getId() + "\"");
            if (!heatingDtPoint.isEmpty() && temperatureMode == TemperatureMode.HEATING) {
                CCUHsApi.getInstance().writeHisValById(heatingDtPoint.get("id").toString(), heatingDesiredTemp);
            } else {
                CcuLog.e(L.TAG_CCU_DEVICE, "heatingDtPoint does not exist: " + hsEquip.getDisplayName());
            }

            HashMap dtPoint = hayStack.read("point and domainName == \"" + DomainName.desiredTemp + "\" and equipRef == \""
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
        CcuLog.d("CCU_MODETEST", "updateConditioningMode: mode " + mode + ", possibleMode " + possibleMode);
        int conditioningMode;
        if (mode == HyperSplit.HyperSplitConditioningMode_e.HYPERSPLIT_CONDITIONING_MODE_AUTO.ordinal()){
            if (possibleMode != PossibleConditioningMode.BOTH) {
                CcuLog.i(L.TAG_CCU_DEVICE, mode+" Invalid conditioning mode "); return;
            }
            conditioningMode = StandaloneConditioningMode.AUTO.ordinal();
        } else if (mode == HyperSplit.HyperSplitConditioningMode_e.HYPERSPLIT_CONDITIONING_MODE_HEATING.ordinal()){
            if (possibleMode == PossibleConditioningMode.COOLONLY) {
                CcuLog.i(L.TAG_CCU_DEVICE, mode+"Invalid conditioning mode"); return;
            }
            conditioningMode = StandaloneConditioningMode.HEAT_ONLY.ordinal();
        } else if (mode == HyperSplit.HyperSplitConditioningMode_e.HYPERSPLIT_CONDITIONING_MODE_COOLING.ordinal()){
            if (possibleMode == PossibleConditioningMode.HEATONLY) {
                CcuLog.i(L.TAG_CCU_DEVICE, mode+"Invalid conditioning mode"); return;
            }
            conditioningMode = StandaloneConditioningMode.COOL_ONLY.ordinal();
        } else {
            if (mode != PossibleConditioningMode.OFF.ordinal()) {
                CcuLog.i(L.TAG_CCU_DEVICE, mode+" Invalid conditioning mode "); return;
            }
            conditioningMode = StandaloneConditioningMode.OFF.ordinal();
        }
        // TODO: check how this is reflected in UI
        HyperStatSplitUserIntentHandler.Companion.updateHyperStatSplitUIPoints(equipId,
                "zone and sp and conditioning and mode", conditioningMode, WhoFiledConstants.HYPERSTAT_SPLIT_WHO);
    }

    private static void sendAcknowledge(int address){
        CcuLog.i(L.TAG_CCU_DEVICE, "Sending Acknowledgement");
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
        if(fanMode!= -1 && !HyperStatSplitUserIntentHandler.Companion.isFanModeChangeUnnecessary(equipId, fanMode)) {
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
        CcuLog.i(L.TAG_CCU_DEVICE, "PossibleFanMode: "+mode + " selectedMode  "+selectedMode);
        if(selectedMode == 0 ) return StandaloneFanStage.OFF.ordinal();
        switch (mode){
            case OFF:
                if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_OFF.ordinal() == selectedMode ) {
                    return StandaloneFanStage.OFF.ordinal();
                } else {
                    CcuLog.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }
            case LOW:
                if(selectedMode == HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                }else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_LOW.ordinal() == selectedMode ) {
                    return StandaloneFanStage.LOW_CUR_OCC.ordinal();
                } else {
                    CcuLog.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }
            case MED:
                if(selectedMode == HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                }else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_MED.ordinal() == selectedMode ) {
                    return StandaloneFanStage.MEDIUM_CUR_OCC.ordinal();
                } else {
                    CcuLog.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }
            case HIGH:
                if(selectedMode == HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                } else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_HIGH.ordinal() == selectedMode ) {
                    return StandaloneFanStage.HIGH_CUR_OCC.ordinal();
                } else {
                    CcuLog.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }

            case LOW_MED:
                if(selectedMode == HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                } else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_LOW.ordinal() == selectedMode ) {
                    return StandaloneFanStage.LOW_CUR_OCC.ordinal();
                } else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_MED.ordinal() == selectedMode ) {
                    return StandaloneFanStage.MEDIUM_CUR_OCC.ordinal();
                } else {
                    CcuLog.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }

            case LOW_HIGH:
                if(selectedMode == HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                } else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_LOW.ordinal() == selectedMode ) {
                    return StandaloneFanStage.LOW_CUR_OCC.ordinal();
                } else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_HIGH.ordinal() == selectedMode ) {
                    return StandaloneFanStage.HIGH_CUR_OCC.ordinal();
                } else {
                    CcuLog.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }

            case MED_HIGH:
                if(selectedMode == HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_AUTO.ordinal()) {
                    return StandaloneFanStage.AUTO.ordinal();
                } else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_MED.ordinal() == selectedMode ) {
                    return StandaloneFanStage.MEDIUM_CUR_OCC.ordinal();
                } else if(HyperSplit.HyperSplitFanSpeed_e.HYPERSPLIT_FAN_SPEED_HIGH.ordinal() == selectedMode ) {
                    return StandaloneFanStage.HIGH_CUR_OCC.ordinal();
                } else {
                    CcuLog.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
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
                    CcuLog.i(L.TAG_CCU_DEVICE, "Invalid Fan mode"); return -1;
                }
        }
        return -1;
    }
    
}

class MinMaxVoltage {
    double minVoltage;
    double maxVoltage;
    double minEngVal;
    double maxEngVal;

    public MinMaxVoltage(double minVoltage, double maxVoltage, double minEngVal, double maxEngVal) {
        this.minVoltage = minVoltage;
        this.maxVoltage = maxVoltage;
        this.minEngVal = minEngVal;
        this.maxEngVal = maxEngVal;
    }
}
