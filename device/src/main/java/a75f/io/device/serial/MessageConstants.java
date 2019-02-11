package a75f.io.device.serial;

/**
 * Created by samjithsadasivan isOn 8/1/17.
 */

public class MessageConstants
{
	
	public static final int MAX_SERIAL_NUMBER_LENGTH = 16;
	public static final int SN_SERIAL_NUMBER_LENGTH  = 16;
	
	public static final int WRM_SERIAL_NUMBER_LENGTH = 11;
	
	public static final int CM_SERIAL_NUMBER_LENGTH = 11;
	
	public static final int SN_MANUFACTURE_DATE_LENGTH = 13;
	
	public static final int FIRMWARE_UPDATE_PACKET_SIZE = 32;
	
	public static final int INVALID_FIRMWARE_PACKET_SEQ_NUMBER = 0xFFFF;
	
	public static final int APP_KEY_LENGTH = 16;
	
	public static final int HEATING_MODE = 1;
	
	public static final int COOLING_MODE = 0;
	
	public static final int OCCUPIED = 1;
	
	public static final int UNOCCUPIED = 0;
	
	public static final int EXTERNAL_POWER = 1;
	
	public static final int MOTOR_ERROR = 1;
	
	public static final int AUTO_OCCUPIED = 0;
	
	public static final int FORCED_OCCUPIED = 1;
	
	public static final int FORCED_UNOCCUPIED = 2;
	
	public static final int NUM_SN_TYPE_VALUE_SENSOR_READINGS = 6;
	
	public static final int MAX_LIGHTING_CONTROL_CIRCUIT_LOGICAL_NAME_BYTES = 20;
	
	public static final int MAX_LIGHTING_CONTROL_CIRCUIT_SCHEDULE_ENTRIES = 10;
	
	public static final int ROOM_NAME_MAX_LENGTH = 25;
	
	public static final int FIRMWARE_SIGNATURE_LENGTH = 32;
	
	public static final int MESH_ENCRYPTION_KEY_LENGTH = 16;
	
	public static final int NUM_SS_TYPE_VALUE_SENSOR_READINGS = 6;
}
