package a75f.io.logic;

import java.util.ArrayList;
import java.util.List;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.building.SmartNode;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.MessageType;

/**
 * Created by Yinten isOn 8/17/2017.
 */

public class MapBLL
{
	
	public static List<CcuToCmOverUsbDatabaseSeedSnMessage_t> generateSeedMessages(CCUApplication ccuApplication)
	{
		List<CcuToCmOverUsbDatabaseSeedSnMessage_t> seedSnMessage_ts = new ArrayList<>();
		
		CcuToCmOverUsbDatabaseSeedSnMessage_t ccuToCmOverUsbDatabaseSeedSnMessage_t = new CcuToCmOverUsbDatabaseSeedSnMessage_t();
		
		
		//Find all the smart nodes, add all the io for the smart nodes, find profiles associated with the smart nodes
		for(SmartNode smartNode : ccuApplication.smartNodes)
		{
			ccuToCmOverUsbDatabaseSeedSnMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
			ccuToCmOverUsbDatabaseSeedSnMessage_t.smartNodeAddress.set(smartNode.mAddress);
			
			
			//ccuToCmOverUsbDatabaseSeedSnMessage_t.putEncrptionKey()
			
		}
		
		//ccuToCmOverUsbDatabaseSeedSnMessage_t.smartNodeAddress.set(smartNode.address);
		//			ccuToCmOverUsbDatabaseSeedSnMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
		//			//ccuToCmOverUsbDatabaseSeedSnMessage_t.putEncrptionKey(Encryp);
		//			ZoneProfile zoneProfile = ccuApplication.zones.get(0).zoneProfiles.get(0);
		//			ccuToCmOverUsbDatabaseSeedSnMessage_t.controls.analogOut1.set((short) 0);
		//
		//			ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.profileBitmap.lightingControl.set((short) 1);
		//			ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.ledBitmap.analogIn1.set((short)1);
		//			ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.lightingIntensityForOccupantDetected.set((short) 0);
		//			ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.minLightingControlOverrideTimeInMinutes.set((short) 1);
		//			ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.roomName.set(smartNode.roomName);
		
		
		
		return seedSnMessage_ts;
	}
	
	
	
	
	
	
}
