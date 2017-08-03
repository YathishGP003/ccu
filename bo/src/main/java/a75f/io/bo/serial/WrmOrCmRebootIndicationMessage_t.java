package a75f.io.bo.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan on 8/1/17.
 */

public class WrmOrCmRebootIndicationMessage_t extends Struct {

    public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
    public final Unsigned16 wrmAddress = new Unsigned16();

    public final Unsigned8 majorFirmwareVersion = new Unsigned8();

    public final Unsigned8 minorFirmwareVersion = new Unsigned8();

    public final Unsigned8 rebootCause = new Unsigned8(); /* Bit 5 – SRF: Software Reset Flag
                                            * Bit 4 – PDIRF: Program and Debug Interface Reset Flag
                                            * Bit 3 – WDRF: Watchdog Reset Flag
                                            * Bit 2 – BORF: Brownout Reset Flag
                                            * Bit 1 – EXTRF: External Reset Flag
                                            * Bit 0 – PORF: Power On Reset Flag
                                            */

    public final Unsigned8[] deviceId = array (new Unsigned8[3]); // Storage for device id

    public final Unsigned8[] deviceSerial = array ( new Unsigned8[MessageConstants.WRM_SERIAL_NUMBER_LENGTH]); // Storage for serial number

    @Override
    public ByteOrder byteOrder() {
        return ByteOrder.LITTLE_ENDIAN;
    }

}
