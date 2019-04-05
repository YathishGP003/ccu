package a75f.io.device.serial;

import org.javolution.io.Struct;

/**
 * Created by samjithsadasivan isOn 8/9/17.
 */

public class SmartStatSettings_t extends Struct
{
	public final UTF8String roomName = new UTF8String(MessageConstants.ROOM_NAME_MAX_LENGTH); /* A string describing the name of the room or zone where the Smart Node is installed */
	
	public final SmartStatProfileBitmap_t profileBitmap = inner(new SmartStatProfileBitmap_t()); /* Determines which profiles are enabled */
	
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
	
	public final Unsigned8 showCentigrade = new Unsigned8(1); /* show F or C */
	
	public final Unsigned8 enableOccupancyDetection = new Unsigned8(1); /* 1 if occupancy detection is enabled */
	
	public final Unsigned8 reserved = new Unsigned8(6);

	public class SmartStatProfileBitmap_t extends Struct
	{
		public final Unsigned8 convetionalPackageUnit = new Unsigned8(1); /* 1 is heating, 0 is cooling - sent from CCU to Smart Node for display indication */
		public final Unsigned8 commercialPackageUnit         = new Unsigned8(1); /* digital out for activation */  //set light control to 1 ..
		public final Unsigned8 heatPumpUnit  = new Unsigned8(1); /* digital out for activation */
		public final Unsigned8 PipeFanCoilUnit2    = new Unsigned8(1); /* digital out for activation */
		public final Unsigned8 PipeFanCoilUnit4           = new Unsigned8(1); /* digital out for activation */
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
