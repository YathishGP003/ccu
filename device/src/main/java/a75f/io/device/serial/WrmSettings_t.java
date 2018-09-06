package a75f.io.device.serial;

import org.javolution.io.Struct;

/**
 * Created by samjithsadasivan isOn 8/1/17.
 */

public class WrmSettings_t extends Struct
{
	
	public final SystemTime_t time = inner(new SystemTime_t());
	
	public final Unsigned8 setTemperatureFromCcu        = new Unsigned8(); // default 150 - temp in 2x F
	public final Unsigned8 damperPosition               = new Unsigned8(); // default 90 - percent open. CCU telling WRM where to open. This is a hack for now. Should be in separate message since we waste b/w sending entire message
	public final Unsigned8 maxUserTemp                  = new Unsigned8(); // default 80 - in deg F
	public final Unsigned8 minUserTemp                  = new Unsigned8(); // default 60 - in deg F
	public final Unsigned8 maxDamperOpen                = new Unsigned8(); // default 100 - in percent
	public final Unsigned8 minDamperOpen                = new Unsigned8(); // default 40 - in percent
	public final Signed8 temperatureOffset            = new Signed8(); // default 0 - in 1/10 deg F. This is added to the measured temp
	public final Unsigned8 motorOperationalCurrent      = new Unsigned8(); // default 20 - the current in ma motor draws when steady state @5v
	public final Unsigned8 motorStallCurrent            = new Unsigned8(); // default 80 - the current in ma motor draws when stalled @5v
	public final Unsigned8 indicatedForwardMotorRpm     = new Unsigned8(); // default 22 - the actual rpm used in all settings. Normally should be equal to measured rpm but may be overridden for testing
	public final Unsigned8 indicatedReverseMotorRpm     = new Unsigned8(); // default 22 - the actual rpm used in all settings. Normally should be equal to measured rpm but may be overridden for testing
	public final Unsigned8 forwardMotorBacklash         = new Unsigned8(); // default 5 - backlash to be added when motor changes direction from reverse to forward - in % damper opening
	public final Unsigned8 reverseMotorBacklash         = new Unsigned8(); // default 5 - backlash to be added when motor changes direction from forward to reverse - in % damper opening
	public final Unsigned8 proportionalConstant         = new Unsigned8(); // default 50 - k constants for Proportional in 1/100
	public final Unsigned8 integralConstant             = new Unsigned8(); // default 50 - k constant for Integral in 1/100
	public final Unsigned8 proportionalTemperatureRange = new Unsigned8(); // default 15 - temp range in 1/10 deg that the proportional control will apply
	public final Unsigned8 integrationTime              = new Unsigned8(); // default 30 - time in minutes the integration takes to max out
	public final Unsigned8 airflowHeatingTemperature    = new Unsigned8(); // default 105 - airflow temperature in deg F above which we consider unit is in heating mode for failsafe mode
	public final Unsigned8 airflowCoolingTemperature    = new Unsigned8(); // default 60 - airflow temperature in deg F below which we consider unit is in cooling mode for failsafe mode
	public final Unsigned8 conditioningMode             = new Unsigned8(1); // 1 is heating, 0 is cooling - sent from CCU to WRM for display indication
	public final Unsigned8 occupancyDetectorEnabled     = new Unsigned8(1); // 1 enabled. WRM should only turn isOn detector if enabled
	public final Unsigned8 resetWrm                     = new Unsigned8(1); // force a reset of the WRM remotely when set to 1
	public final Unsigned8 digitalOut1                  = new Unsigned8(1); // digital out for activation
	public final Unsigned8 digitalOut2                  = new Unsigned8(1); // digital out for activation
	public final Unsigned8 showCentigrade               = new Unsigned8(1); // show F or C
	public final Unsigned8 displayHold                  = new Unsigned8(1); // if 'hold' is to be shown to signify temporary hold if temperature changed during occupied system schedule
	public final Unsigned8 militaryTime                 = new Unsigned8(1); // determine if we are using 24 hour format or not
	public final Unsigned8 analogOut1                   = new Unsigned8(); // output for pwm channel 1 // additional bytes to make legacy structure as new_protocol structure
	public final Unsigned8 analogOut2                   = new Unsigned8(); // output for pwm channel 2 // additional bytes to make legacy structure as new_protocol structure
}
