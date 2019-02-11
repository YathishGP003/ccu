package a75f.io.device.serial;

import java.nio.ByteOrder;

import org.javolution.io.Struct;

/**
 * Based on /// Message from the CM to a device to set the device's settings
typedef struct {
  WrmSettings_t wrmSettings;
} CmToWrmOverAirWrmSettingsMessage_t;

from 
https://gitlab.com/75f/firmware/cm3/blob/master/cm3/includes/protocols_wrm.h

 * @author Yinten
 *
 */
public class CmToWrmOverAirWrmSettingsMessage_t extends Struct {
	
	public final WrmSettings_t wrmSettings = inner(new WrmSettings_t());

	@Override
	public ByteOrder byteOrder() {
		return ByteOrder.LITTLE_ENDIAN;
	}

}
