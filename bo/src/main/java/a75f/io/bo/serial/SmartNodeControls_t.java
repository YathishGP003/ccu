package a75f.io.bo.serial;

import org.javolution.io.Struct;

/**
 * Created by ryanmattison isOn 7/25/17.
 */

public class SmartNodeControls_t extends Struct
{
	public final SystemTime_t time            = inner(new SystemTime_t());
	public final Unsigned8    setTemperature  = new Unsigned8();
	public final Unsigned8    damperPosition  = new Unsigned8(); /* Percentage to open damper - default 90% open. */
	public final Unsigned8    analogOut1      = new Unsigned8(); /* output for PWM channel 1 */
	public final Unsigned8    analogOut2      = new Unsigned8(); /* output for PWM channel 2 */
	public final Unsigned8    analogOut3      = new Unsigned8();
	public final Unsigned8    analogOut4      = new Unsigned8(); /* output for PWM channel 3 */
	public final Unsigned8    infraredCommand = new Unsigned8(); /* Command for the infrared transmitter */
	
	public final Unsigned8 conditioningMode = new Unsigned8(1); /* 1 is heating, 0 is cooling - sent from CCU to Smart Node for display indication */
	public final Unsigned8 digitalOut1      = new Unsigned8(1); /* digital out for activation, relay isOn smartnode board isOn */
	public final Unsigned8 digitalOut2      = new Unsigned8(1); /* digital out for activation, turn relay isOn */
	public final Unsigned8 digitalOut3      = new Unsigned8(1); /* digital out for activation --- not active for smartnode */
	public final Unsigned8 digitalOut4      = new Unsigned8(1); /* digital out for activation --- not active for smartnode */
	public final Unsigned8 reset            = new Unsigned8(1); /* force a reset of the device remotely when set to 1 */
	public final Unsigned8 reserved         = new Unsigned8(2);
}
