package a75f.io.device.mesh.hyperstat;

import static a75f.io.api.haystack.Tags.HYPERSTAT;

import com.google.protobuf.InvalidProtocolBufferException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.device.HyperStat.HyperStatIduStatusMessage_t;
import a75f.io.device.HyperStat.HyperStatLocalControlsOverrideMessage_t;
import a75f.io.device.HyperStat.HyperStatRegularUpdateMessage_t;
import a75f.io.device.mesh.DLog;
import a75f.io.device.mesh.Pulse;
import a75f.io.device.serial.MessageType;
import a75f.io.logger.CcuLog;
import a75f.io.logic.L;
import a75f.io.logic.interfaces.ZoneDataInterface;

public class HyperStatMsgReceiver {

    private static final int HYPERSTAT_MESSAGE_ADDR_START_INDEX = 1;
    private static final int HYPERSTAT_MESSAGE_ADDR_END_INDEX = 5;
    private static final int HYPERSTAT_MESSAGE_TYPE_INDEX = 13;
    private static final int HYPERSTAT_SERIALIZED_MESSAGE_START_INDEX = 17;
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
            CcuLog.e(L.TAG_CCU_DEVICE, "HyperStatMsgReceiver processMessage processMessage :"+ Arrays.toString(data));

            byte[] addrArray = Arrays.copyOfRange(data, HYPERSTAT_MESSAGE_ADDR_START_INDEX,
                                                  HYPERSTAT_MESSAGE_ADDR_END_INDEX);
            int address = ByteBuffer.wrap(addrArray)
                                    .order(ByteOrder.LITTLE_ENDIAN)
                                    .getInt();

            MessageType messageType = MessageType.values()[data[HYPERSTAT_MESSAGE_TYPE_INDEX]];

            byte[] messageArray = Arrays.copyOfRange(data, HYPERSTAT_SERIALIZED_MESSAGE_START_INDEX, data.length);

            /*
                HyperSplit Messages arrive through the same serialized message as HyperStat messages.

                If a HyperStat/Split Serialized Message is received by the CCU,
                both the HyperStatMsgReceiver and HyperSplitMsgReceiver handler methods are called.

                So, skip evaluation here if the message contents are actually a HyperSplit message.
            */
            if (messageType == MessageType.HYPERSTAT_REGULAR_UPDATE_MESSAGE) {
                HyperStatRegularUpdateMessage_t regularUpdate =
                        HyperStatRegularUpdateMessage_t.parseFrom(messageArray);
                handleRegularUpdate(regularUpdate, address);
            } else if (messageType == MessageType.HYPERSTAT_LOCAL_CONTROLS_OVERRIDE_MESSAGE) {
                HyperStatLocalControlsOverrideMessage_t overrideMessage =
                        HyperStatLocalControlsOverrideMessage_t.parseFrom(messageArray);
                handleOverrideMessage(overrideMessage, address);
            } else if (messageType == MessageType.HYPERSTAT_IDU_STATUS_MESSAGE) {
                HyperStatIduStatusMessage_t p1p2Status =
                    HyperStatIduStatusMessage_t.parseFrom(messageArray);
                HyperStatIduMessageHandler.handleIduStatusMessage(p1p2Status, address, hayStack);
            }

        } catch (InvalidProtocolBufferException e) {
            CcuLog.e(L.TAG_CCU_DEVICE, "Cant parse protobuf data: "+e.getMessage());
        }
    }

    private static void handleRegularUpdate(HyperStatRegularUpdateMessage_t regularUpdateMessage, int nodeAddress) {
        if (DLog.isLoggingEnabled()) {
            CcuLog.i(L.TAG_CCU_DEVICE, "handleRegularUpdate: "+regularUpdateMessage.toString());
        }

        HashMap device = HyperStatControlUtilKt.getHyperStatDevice(nodeAddress);
        Pulse.mDeviceUpdate.put((short) nodeAddress, Calendar.getInstance().getTimeInMillis());
        HyperStatMsgHandlerKt.handleRegularUpdate(regularUpdateMessage, device, nodeAddress, currentTempInterface);
    }

    private static void handleOverrideMessage(HyperStatLocalControlsOverrideMessage_t message, int nodeAddress) {
        HashMap device = HyperStatControlUtilKt.getHyperStatDevice(nodeAddress);
        Equip hsEquip = new Equip.Builder().setHashMap(device).build();
        if (hsEquip.getMarkers().contains(HYPERSTAT)) {
            HyperStatMsgHandlerKt.handleOverrideMsg(message, nodeAddress, hsEquip.getEquipRef(),currentTempInterface);
        }
    }

    public static void setCurrentTempInterface(ZoneDataInterface in) { currentTempInterface = in; }

}