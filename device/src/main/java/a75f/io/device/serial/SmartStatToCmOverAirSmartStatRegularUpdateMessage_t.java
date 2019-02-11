package a75f.io.device.serial;

import java.nio.ByteOrder;

import org.javolution.io.Struct;

/***
 * based on protocols_smart_stat.h#152
 * @author Yinten
 *
 *
 *
typedef struct {
	
	
  uint16_t address; ///< LW Mesh Address of the device sending the message
  uint16_t parentShortAddr; ///< Next hop (for router)
  uint8_t lqi; ///< Link Quality Indicator of last received data packet
  int8_t rssi; ///< Received Signal Strength Indicator of last received data packet
  uint16_t roomTemperature; ///< Room temperature in 1/10 of a degree F. This is the adjusted temp and is offset + measured temp.
  uint16_t humidity; ///< Humidity in 1/10 of a percent
  uint16_t lightIntensity; ///< Light intensity in 1/10 of a lux
  uint16_t externalAnalogVoltageInput1; ///< External voltage sense in millivolts
  uint16_t externalAnalogVoltageInput2; ///< External voltage sense in millivolts
  uint16_t externalThermistorInput1; ///< External resistance sense in tens of ohms
  uint16_t externalThermistorInput2; ///< External resistance sense in tens of ohms
  SensorReading_t sensorReadings[NUM_SMART_STAT_TYPE_VALUE_SENSOR_READINGS]; ///< List of type-value sensor readings
  uint8_t occupantDetected :1; ///< 0 is no, 1 is yes
} SmartStatToCmOverAirSmartStatRegularUpdateMessage_t;
 */

public class SmartStatToCmOverAirSmartStatRegularUpdateMessage_t extends Struct {

	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	
	public final Unsigned16 address = new Unsigned16();
	public final Unsigned16 parentShortAddr = new Unsigned16();
	
	public final Unsigned8 cmLqi  = new Unsigned8(); /* LQI of this received data packet @ CM */
	public final Signed8   cmRssi = new Signed8(); /* RSSI of this received data packet @ CM */
	
	 public final Unsigned16 roomTemperature = new Unsigned16(); ///< Room temperature in 1/10 of a degree F. This is the adjusted temp and is offset + measured temp.
	 public final Unsigned16 humidity  = new Unsigned16(); ///< Humidity in 1/10 of a percent
	 public final Unsigned16 lightIntensity  = new Unsigned16(); ///< Light intensity in 1/10 of a lux
	 public final Unsigned16 externalAnalogVoltageInput1  = new Unsigned16(); ///< External voltage sense in millivolts
	 public final Unsigned16 externalAnalogVoltageInput2  = new Unsigned16(); ///< External voltage sense in millivolts
	 public final Unsigned16 externalThermistorInput1  = new Unsigned16(); ///< External resistance sense in tens of ohms
	 public final Unsigned16 externalThermistorInput2  = new Unsigned16(); ///< External resistance sense in tens of ohms
	
	 public final SensorReading_t[] sensorReadings = array(new SensorReading_t[MessageConstants.NUM_SS_TYPE_VALUE_SENSOR_READINGS]);
	 public final Unsigned8 occupantDetected = new Unsigned8(1); 
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
	
}
