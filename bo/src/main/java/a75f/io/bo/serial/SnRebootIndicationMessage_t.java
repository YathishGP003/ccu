package a75f.io.bo.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan on 8/1/17.
 */

public class SnRebootIndicationMessage_t extends Struct {

    public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
    public final Unsigned16 smartNodeAddress = new Unsigned16(); /* LW Mesh Address of the Smart Node sending the message */

    public final Unsigned8 smartNodeMajorFirmwareVersion = new Unsigned8();
    public final Unsigned8 smartNodeMinorFirmwareVersion = new Unsigned8();
    public final Unsigned8 rtsMajorFirmwareVersion = new Unsigned8();
    public final Unsigned8 rtsMinorFirmwareVersion = new Unsigned8();
    public final Unsigned8 rebootCause = new Unsigned8(); /* Bit 7 - Backup Reset Flag
                                                           * Bit 6 - Software Reset Flag
                                                           * Bit 5 - Watchdog Reset Flag
                                                           * Bit 4 - External Reset Flag
                                                           * Bit 3 - Reserved
                                                           * Bit 2 - VDD Brownout Reset Flag
                                                           * Bit 1 - CORE Brownout Reset Flag
                                                           * Bit 0 - Power On Reset Flag
                                                           */
    public final Enum8<FirmwareDeviceType_t> smartNodeDeviceType =
                                                    new Enum8<>(FirmwareDeviceType_t.values());

    public final SmartNodeDeviceId_t smartNodeDeviceId = inner(new SmartNodeDeviceId_t()); /* Storage for device id */

    public final FirmwareSerialNumber_t smartNodeSerialNumber = inner(new FirmwareSerialNumber_t()); /* Storage for serial number */

    public final FirmwareSerialNumber_t rtsSerialNumber = inner(new FirmwareSerialNumber_t());

    public final Unsigned8 smartNodMajoreHardwareVersion = new Unsigned8();

    public final Unsigned8 smartNodeMinorHardwareVersion = new Unsigned8();

    public final Unsigned8 smartNodeContractManufacturerCode = new Unsigned8();

    public final Unsigned8[] smartNodeManufactureDate = array(new Unsigned8[MessageConstants.SN_MANUFACTURE_DATE_LENGTH]);

    @Override
    public ByteOrder byteOrder() {
        return ByteOrder.LITTLE_ENDIAN;
    }

}
