 package a75f.io.device.serial;

import java.nio.ByteOrder;

import org.javolution.io.Struct;

/***
 * 
 * @
  * from protocols_smart_stat.h
  * /// Message from the CM to the CCU passing along a device's regular update
typedef struct {
 SmartStatToCmOverAirSmartStatRegularUpdateMessage_t update; ///< The regular update from the device
 uint8_t cmLqi; ///< LQI of this received data packet at the CM
 int8_t	cmRssi; ///< RSSI of this received data packet at the CM
} CmToCcuOverUsbSmartStatRegularUpdateMessage_t;

  * 
  */

public class CmToCcuOverUsbSmartStatRegularUpdateMessage_t extends Struct {
	public final SmartStatToCmOverAirSmartStatRegularUpdateMessage_t update = inner(new SmartStatToCmOverAirSmartStatRegularUpdateMessage_t());
	
	public final Unsigned8 cmLqi  = new Unsigned8(); /* LQI of this received data packet @ CM */
	public final Signed8   cmRssi = new Signed8(); /* RSSI of this received data packet @ CM */
	
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}

}
