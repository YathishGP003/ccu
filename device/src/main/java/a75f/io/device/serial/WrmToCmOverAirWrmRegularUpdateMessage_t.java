package a75f.io.device.serial;

import org.javolution.io.Struct;

/**
 * Created by samjithsadasivan isOn 7/31/17.
 */

public class WrmToCmOverAirWrmRegularUpdateMessage_t extends Struct
{
	
	public final Unsigned16 wrmAddress      = new Unsigned16(); //16 bit WRM address
	public final Unsigned16 parentShortAddr = new Unsigned16(); //only for router
	public final Unsigned8  lqi             = new Unsigned8(); // lqi of last received data packet
	public final Signed8    rssi            = new Signed8(); // rssi of last received data packet
	public final Unsigned8  battery         = new Unsigned8(); // batt voltage in 100mv
	public final Unsigned16 roomTemperature = new Unsigned16(); // room temp in 1/10F. This is the
	// adjust temp and is offset + measured temp
	
	public final Unsigned16 airflow1Temperature = new Unsigned16(); // airflow temp in 1/10 F
	
	public final Unsigned16 extAnalog1Sense = new Unsigned16(); // external voltage sense in mv units
	
	public final Unsigned8 humidity = new Unsigned8(); // percent
	
	public final Unsigned8 damperPosition = new Unsigned8(); // percent open
	
	public final Unsigned8 set_temperature = new Unsigned8(); // temp in 2x F
	
	public final Unsigned8 measuredMotor1ForwardRpm = new Unsigned8(); // in 1/10s of rpm
	
	public final Unsigned8 measuredMotor1ReverseRpm = new Unsigned8(); // in 1/10s of rpm
	
	public final Unsigned16 batteryStatus = new Unsigned16(2);// 00 - failsafe mode( outline ), 01 33% full * 1 bar ) , 10 100% full ( 2 bars )
	
	public final Unsigned16 extDigital1 = new Unsigned16(1); // occupancy mode from WRM is no longer supported
	
	public final Unsigned16 extDigital2 = new Unsigned16(1); // instead those 2 bits are now used for digital input
	
	public final Unsigned16 occupancyStatus = new Unsigned16(1); // 1 is occupied, 0 is unoccupied
	
	public final Unsigned16 conditioningMode = new Unsigned16(1); // 1 is heating, 0 is cooling
	
	public final Unsigned16 externalPower = new Unsigned16(1); // 0 is battery power, 1 is external DC power
	
	public final Unsigned16 nodeType = new Unsigned16(1); // 0 is end node, 1 is router
	
	public final Unsigned16 motorJammed = new Unsigned16(1); // 0 is motor ok, 1 is motor error
	
	public final Unsigned16 updatedOccupiedTemp = new Unsigned16(1); // 1 is occupied set temp was changed from WRM. this is set in async update packet sent after user interaction
	
	public final Unsigned16 reserved                 = new Unsigned16(6);
	public final Unsigned16 airflow2Temperature      = new Unsigned16(); // airflow temp in 1/10 F // additional bytes to make legacy structure as new_protocol structure
	public final Unsigned16 extAnalog2Sense          = new Unsigned16(); // external voltage sense in mv units // additional bytes to make legacy structure as new_protocol structure
	public final Unsigned16 measuredMotor2ForwardRpm = new Unsigned16(); // in 1/10s of rpm // additional bytes to make legacy structure as new_protocol structure
	public final Unsigned8  measuredMotor2ReverseRpm = new Unsigned8(); // in 1/10s of rpm // additional bytes to make legacy structure as new_protocol structure
}

