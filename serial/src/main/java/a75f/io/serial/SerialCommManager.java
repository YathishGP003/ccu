package a75f.io.serial;

import android.os.Message;

import org.greenrobot.eventbus.EventBus;

import a75f.io.bo.interfaces.ISerial;

/**
 * Created by samjithsadasivan on 7/24/17.
 */

public class SerialCommManager {

    public static final int ESC_BYTE = 0xD9;
    public static final int SOF_BYTE = 0x00;
    public static final int EOF_BYTE = 0x03;

    public static final SerialCommManager INSTANCE = new SerialCommManager();

    public enum MESSAGETYPE {
        FSV_PAIRING_REQ,
        CM_PAIRING_REPONSE,
        FSV_PAIRING_CONFIRM,
        CM_SCHEDULE,
        CM_SETTINGS,
        FSV_REGULAR_UPDATE,
        FSV_SETTINGS_UPDATE,
        CCU_PAIRING_ENABLE,
        CCU_PAIRING_DISABLE,
        CCU_RELAY_ACTIVATION,
        CCU_PAIRING_SEED,
        CCU_PAIRING_FLUSH,
        CCU_SCHEDULE,
        CCU_SETTINGS,
        CCU_CLOCK_UPDATE,
        CM_REGULAR_UPDATE,
        FSV_REBOOT,
        FSV_SETTINGS_REQUEST,
        FSV_SCHEDULE_REQUEST,
        FSV_OVER_AIR_REGULAR_UPDATE,
        FSV_CLOCK_REQUEST,
        CM_CLOCK_UPDATE,
        CM_ERROR_REPORT,
        CCU_REQUEST_PAIRING_TABLE,
        CM_DUMP_PAIRING_TABLE,
        CCU_HEARTBEAT_UPDATE,

        CM_TO_CCU_OVER_USB_SN_REBOOT,
        CM_TO_CCU_OVER_USB_SN_SET_TEMPERATURE_UPDATE,
        CCU_TO_CM_OVER_USB_SN_SET_TEMPERATURE_ACK,
        SN_TO_CM_OVER_AIR_SN_SETTINGS_REQUEST,
        SN_TO_CM_OVER_AIR_SN_CONTROLS_REQUEST,
        CM_TO_SN_OVER_AIR_SN_SETTINGS,
        CM_TO_SN_OVER_AIR_SN_CONTROLS,
        SN_TO_CM_OVER_AIR_SN_REGULAR_UPDATE,
        CM_TO_CCU_OVER_USB_SN_REGULAR_UPDATE,
        CCU_TO_CM_OVER_USB_DATABASE_SEED_SN,
        CCU_TO_CM_OVER_USB_DATABASE_FLUSH_SN,
        CCU_TO_CM_OVER_USB_SN_SETTINGS,
        CCU_TO_CM_OVER_USB_SN_CONTROLS,
        CCU_TO_CM_OVER_USB_FIRMWARE_METADATA,
        SN_TO_CM_OVER_AIR_FIRMWARE_METADATA_REQUEST,
        CM_TO_SN_OVER_AIR_FIRMWARE_METADATA,
        CM_TO_CCU_OVER_USB_FIRMWARE_PACKET_REQUEST,
        CCU_TO_CM_OVER_USB_FIRMWARE_PACKET,
        SN_TO_CM_OVER_AIR_FIRMWARE_PACKET_REQUEST,
        CM_TO_SN_OVER_AIR_FIRMWARE_PACKET,
        SN_TO_CM_OVER_AIR_SN_LIGHTING_SCHEDULE_REQUEST,
        CM_TO_SN_OVER_AIR_SN_LIGHTING_SCHEDULE,
        CCU_TO_CM_OVER_USB_SN_LIGHTING_SCHEDULE,
        NUM_PROTOCOL_MESSAGE_TYPES

    };

    private enum CM_ERROR_TYPE {
        BAD_PACKET_FRAMING,
        DBASE_FULL,
        CCU_UPDATE_FSV_NOT_IN_DBASE,
        FLUSH_FSV_NOT_IN_DBASE,
        GOT_PAIRING_BUT_NOT_ENABLED,
        GOT_PAIRING_CONFIRM_FROM_WRONG_FSV,
        COMMAND_NOT_RECOGNIZED,
        FSV_REGULAR_UPDATE_FSV_NOT_IN_DBASE,
        FSV_ADDRESS_NOT_MATCH_SOURCE,
        FSV_SETTING_REQUEST_NOT_IN_DBASE,
        FSV_SCHEDULE_REQUEST_NOT_IN_DBASE,
        FSV_SETTINGS_UPDATE_NOT_IN_DBASE,
        FSV_REBOOT_NOT_IN_DBASE,
        FSV_CLOCK_REQUEST_NOT_IN_DBASE,
        SN_SETTINGS_REQUEST_NOT_IN_DBASE,
        SN_CONTROLS_REQUEST_NOT_IN_DBASE,
        SN_REGULAR_UPDATE_SN_NOT_IN_DBASE,
        SN_FIRMWARE_METADADTA_REQUEST_NOT_IN_DBASE,
        SN_FIRMWARE_PACKET_REQUEST_NOT_IN_DBASE,
        SN_LIGHTING_SCHEDULE_REQUEST_NOT_IN_DBASE,
        SN_MAX_ALLOWED_LIGHTING_MODULES_EXCEEDED
    };

    private enum STATES {
        PARSE_INIT,
        ESC_BYTE_RCVD,
        SOF_BYTE_RCVD,
        LEN_BYTE_RCVD,
        ESC_BYTE_IN_DATA_RCVD,
        CRC_RCVD,
        ESC_BYTE_AS_END_OF_PACKET_RCVD,
        BAD_PACKET,
        DATA_AVAILABLE
    };

    //Message wrapper to be used on EventBus
    public class SerialCommEvent {

        private Message mMessage;

        public SerialCommEvent(Message msg) {
            this.mMessage = msg;
        }

        public Message getMessage() {
            return mMessage;
        }
    }

    private SerialCommManager() {

    }

    public static SerialCommManager getInstance() {
        return INSTANCE;
    }

    public void registerSerialListener(Object l) {
        EventBus.getDefault().register(l);
    }

    public void unregisterSerialListener(Object l) {
        EventBus.getDefault().register(l);
    }

    public void sendData(ISerial payload) {
        SerialCommService.getSerialService().sendData(payload);
    }

}
