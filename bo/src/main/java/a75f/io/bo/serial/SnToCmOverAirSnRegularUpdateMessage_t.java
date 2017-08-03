package a75f.io.bo.serial;

import org.javolution.io.Struct;

import java.nio.ByteOrder;

/**
 * Created by samjithsadasivan on 8/2/17.
 */

public class SnToCmOverAirSnRegularUpdateMessage_t extends Struct
{
	
	public final Enum8<MessageType> messageType = new Enum8<>(MessageType.values());
	
	public final Unsigned16 smartNodeAddress = new Unsigned16(); /* LW Mesh Address of the Smart Node sending the message */
	
	public final Unsigned16 parentShortAddr = new Unsigned16(); /* only for router we have next hop */
	
	public final Unsigned8 lqi = new Unsigned8(); /* Link Quality Indicator of last received data packet */
	
	public final Unsigned8 rssi = new Unsigned8(); /* Received Signal Strength Indicator of last received data packet */
	
	public final Unsigned8 roomTemperature = new Unsigned8(); /* room temp in 1/10 F. This is the adjusted temp and is offset + measured temp */
	
	public final Unsigned16 airflow1Temperature = new Unsigned16(); /* airflow temp in 1/10 F */
	
	public final Unsigned16 airflow2Temperature = new Unsigned16(); /* airflow temp in 1/10 F */
	
	public final Unsigned16 externalAnalogVoltageInput1 = new Unsigned16(); /* external voltage sense in millivolts */
	
	public final Unsigned16 externalAnalogVoltageInput2 = new Unsigned16(); /* external voltage sense in millivolts */
	
	public final Unsigned16 externalThermistorInput1 = new Unsigned16(); /* external resistance sense in tens of ohms */
	
	public final Unsigned16 externalThermistorInput2 = new Unsigned16(); /* external resistance sense in tens of ohms */
	
	public final Unsigned8 damperPosition = new Unsigned8(); /* percent open */
	
	public final Unsigned8 setTemperature = new Unsigned8(); /* in 2x degrees Fahrenheit */
	
	public final Unsigned8 hvacSupplyVoltage = new Unsigned8(); /* in volts */
	
	public final Unsigned8 measuredMotor1ForwardRpm = new Unsigned8(); /* in 1/10s of rpm */
	
	public final Unsigned8 measuredMotor1ReverseRpm = new Unsigned8(); /* in 1/10s of rpm */
	
	public final Unsigned8 measuredMotor2ForwardRpm = new Unsigned8(); // in 1/10s of rpm */
	
	public final Unsigned8 measuredMotor2ReverseRpm = new Unsigned8(); // in 1/10s of rpm */
	
	public final SmartNodeSensorReading_t[] sensorReadings = array(new SmartNodeSensorReading_t[MessageConstants.NUM_SN_TYPE_VALUE_SENSOR_READINGS]);
	
	public final Unsigned8 batteryStatus = new Unsigned8(2); /* 00 = failsafe mode (outline), 01 = 33% full (1 bar) , 10 = 100% full (2 bars) */
	
	public final Unsigned8 conditioningMode = new Unsigned8(1); /* 1 is heating, 0 is cooling */
	
	public final Unsigned8 externalPower = new Unsigned8(1); /* 0 is battery power, 1 is external DC power */
	
	public final Unsigned8 nodeType = new Unsigned8(1); /* 0 is end node, 1 is router */
	
	public final Unsigned8 damper1CalibrationError = new Unsigned8(1); /* 0 is no error, 1 is error */
	
	public final Unsigned8 damper2CalibrationError = new Unsigned8(1); /* 0 is no error, 1 is error */
	
	public final Unsigned8 reserved = new Unsigned8(1);
	
	@Override
	public ByteOrder byteOrder()
	{
		return ByteOrder.LITTLE_ENDIAN;
	}
}
