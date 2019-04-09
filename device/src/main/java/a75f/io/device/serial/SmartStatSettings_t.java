package a75f.io.device.serial;

import org.javolution.io.Struct;

/**
 * Created by Anilkumar isOn 1/4/19.
 */

public class SmartStatSettings_t extends Struct
{
	public final UTF8String roomName = new UTF8String(MessageConstants.ROOM_NAME_MAX_LENGTH); /* A string describing the name of the room or zone where the Smart Node is installed */
	
	public final Enum8<SmartStatProfileMap_t> profileBitmap = new Enum8<>(SmartStatProfileMap_t.values());;//inner(new SmartStatProfileBitmap_t()); /* Determines which profiles are enabled */
	
	public final Unsigned8 maxUserTemp = new Unsigned8(); /* Maximum temperature the user can set in degrees F (default 80) */
	
	public final Unsigned8 minUserTemp = new Unsigned8(); /* Minimum temperature the user can set in degrees F (default 60) */
	
	public final Signed8 temperatureOffset = new Signed8(); /* Offset to be added to the measured room temperature (default 0). Unit is 1/10 of a degree F. */
	
	public final Unsigned8 heatingDeadBand = new Unsigned8(); /* Amount above set temperature at which heating is activated. Unit is degrees F. */
	
	public final Unsigned8 coolingDeadBand = new Unsigned8(); /* Amount below set temperature at which cooling is activated. Unit is degrees F. */
	
	public final Unsigned8 holdTimeInMinutes = new Unsigned8(); /* Amount of time in minutes to apply a local control override before switching back to the schedule */
	
	public final Unsigned8 changeToUnoccupiedTime = new Unsigned8(); /* Time at which the schedule next changes to unoccupied (15 minute increments from midnight) */
	public final Unsigned8 changeToOccupiedTime = new Unsigned8(); /* Time at which the schedule next changes to occupied (15 minute increments from midnight) */
	
	public final Unsigned8 lightingIntensityForOccupantDetected = new Unsigned8(); /* Lighting intensity (%) to use when occupants are detected */
	
	public final SmartStatEnabledRelaysBitmap_t enabledRelaysBitmap = inner (new SmartStatEnabledRelaysBitmap_t()); /* Determines which relays are being used in the current setup */
	public final SmartStatOtherBitMap_t otherBitMaps = inner (new SmartStatOtherBitMap_t()); /* Determines about other bits being enabled */
	

	public class SmartStatOtherBitMap_t extends Struct
	{
		public final Unsigned8 centigrade = new Unsigned8(1);
		public final Unsigned8 occupancySensor         = new Unsigned8(1);
		public final Unsigned8 heatPumpUnitChangeOverB  = new Unsigned8(1);
		public final Unsigned8 enableExternal10kTempSensor    = new Unsigned8(1);
		public final Unsigned8 enableBeaconing           = new Unsigned8(1);
		public final Unsigned8 reserved                = new Unsigned8(3);
	}
	public class SmartStatEnabledRelaysBitmap_t extends Struct
	{
		public final Unsigned8 relay1 = new Unsigned8(1);

		public final Unsigned8 relay2 = new Unsigned8(1);

		public final Unsigned8 relay3 = new Unsigned8(1);

		public final Unsigned8 relay4 = new Unsigned8(1);

		public final Unsigned8 relay5 = new Unsigned8(1);

		public final Unsigned8 relay6 = new Unsigned8(1);

		public final Unsigned8 reserved = new Unsigned8(2);
	}
}
