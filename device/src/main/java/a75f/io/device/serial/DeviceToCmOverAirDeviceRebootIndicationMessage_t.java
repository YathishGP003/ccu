package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by James Liu on 1/15/17.
 */

public class DeviceToCmOverAirDeviceRebootIndicationMessage_t extends Struct
{
	
	public final Enum8<MessageType> messageType      = new Enum8<>(MessageType.values());
	public final Unsigned16         address = new Unsigned16(); /* LW Mesh Address of the device sending the message */
	
	public final Unsigned8                   masterDeviceMajorFirmwareVersion = new Unsigned8();
	public final Unsigned8                   masterDeviceMinorFirmwareVersion = new Unsigned8();
	public final Unsigned8                  	slaveDeviceMajorFirmwareVersion       = new Unsigned8();
	public final Unsigned8                   slaveDeviceMinorFirmwareVersion       = new Unsigned8();
	public final Unsigned8                   rebootCause                   = new Unsigned8(); /* Bit 7 - Backup Reset Flag
	                                                       * Bit 6 - Software Reset Flag
                                                           * Bit 5 - Watchdog Reset Flag
                                                           * Bit 4 - External Reset Flag
                                                           * Bit 3 - Reserved
                                                           * Bit 2 - VDD Brownout Reset Flag
                                                           * Bit 1 - CORE Brownout Reset Flag
                                                           * Bit 0 - Power On Reset Flag
                                                           */
	public final Enum8<FirmwareDeviceType_t> deviceType           = new Enum8<>(FirmwareDeviceType_t.values());
	
	public final SmartNodeDeviceId_t deviceId = inner(new SmartNodeDeviceId_t()); /* Storage for device id */
	
	public final FirmwareSerialNumber_t masterDeviceSerialNumber = inner(new FirmwareSerialNumber_t()); /* Storage for serial number */
	
	public final FirmwareSerialNumber_t slaveDeviceSerialNumber = inner(new FirmwareSerialNumber_t());
	
	public final Unsigned8 majorHardwareVersion = new Unsigned8();
	
	public final Unsigned8 minorHardwareVersion = new Unsigned8();
	
	public final Unsigned8 contractManufacturerCode = new Unsigned8();
	
	public final Unsigned8[] manufactureDate = array(new Unsigned8[MessageConstants.SN_MANUFACTURE_DATE_LENGTH]);
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
