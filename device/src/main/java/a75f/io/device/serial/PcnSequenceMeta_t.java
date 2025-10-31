package a75f.io.device.serial;

import org.javolution.io.Struct;

import java.nio.charset.StandardCharsets;

/**
 * Created by pramod isOn 22/09/2025.
 */

public class PcnSequenceMeta_t extends Struct
{

//	public final Enum8<FirmwareComponentType_t> deviceType = new Enum8<>(FirmwareComponentType_t.values()); /* Type of device to be updated */
	public final Unsigned32 lengthInBytes = new Unsigned32(); /* The length of the new firmware in bytes */

	public final Unsigned8[] signature = array(new Unsigned8[MessageConstants.FIRMWARE_SIGNATURE_LENGTH]); /* The signature for the new firmware */

	public final Unsigned8[] name = array(new Unsigned8[MessageConstants.LOW_CODE_SEQUENCE_NAME_MAX_LENGTH]); /* The name of the new sequence */
	
	public final Unsigned16 sequenceId = new Unsigned16(); /* The minor version of the new firmware */
	public final Unsigned8 slaveId = new Unsigned8(); /* The ID of the slave to be updated */


	public void setName(String nameBytes) {
		byte[] bytes = nameBytes.getBytes(StandardCharsets.UTF_8); // or use "UTF-8" if not using Java 7+

		for (int i = 0; i < name.length; i++) {
			byte b = (i < bytes.length) ? bytes[i] : 0; // pad with 0 if input is shorter
			if (name[i] == null) {
				name[i] = new Unsigned8(b & 0xFF);
			} else {
				name[i].set((short) (b & 0xFF));
			}
		}
	}

	public void setSignature(byte[] signatureBytes) {
		for(int i = 0; i < signatureBytes.length; i++) {
			signature[i].set(signatureBytes[i]);
		}
	}
}
