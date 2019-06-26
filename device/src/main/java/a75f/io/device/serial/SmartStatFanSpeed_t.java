package a75f.io.device.serial;


/**
 * Created by samjithsadasivan isOn 8/9/17.
 */

public enum SmartStatFanSpeed_t
{
	FAN_SPEED_OFF,
	FAN_SPEED_AUTO,
	FAN_SPEED_LOW,
	FAN_SPEED_HIGH,/* Firmware reference for CPU and HPU is high but for 2PFCU and 4PFCU it is medium */
	FAN_SPEED_HIGH2 /* Firmware reference for 2PFCU and 4PFCU it is high */
}
