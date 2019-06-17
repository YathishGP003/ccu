package a75f.io.device.serial;

import org.javolution.io.Struct;

/**
 * Created by samjithsadasivan isOn 8/2/17.
 */

public class FirmwareMetdata_t extends Struct
{
	
	public final Enum8<FirmwareDeviceType_t> deviceType = new Enum8<>(FirmwareDeviceType_t.values()); /* Type of device to be updated */
	
	public final Unsigned8 majorVersion = new Unsigned8(); /* The major version of the new firmware */
	
	public final Unsigned8 minorVersion = new Unsigned8(); /* The minor version of the new firmware */
	
	public final Unsigned32 lengthInBytes = new Unsigned32(); /* The length of the new firmware in bytes */
	
	public final Unsigned8[] signature = array(new Unsigned8[MessageConstants.FIRMWARE_SIGNATURE_LENGTH]); /* The signature for the new firmware */

	public void setSignature(byte[] signatureBytes) {
		for(int i = 0; i < signatureBytes.length; i++) {
			signature[i].set(signatureBytes[i]);
		}
	}
}
