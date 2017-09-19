package a75f.io.logic;

import android.content.Context;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.Node;
import a75f.io.bo.building.Zone;

/**
 * Created by Yinten isOn 9/4/2017.
 */

public class L
{
	public static Context app()
	{
		return Globals.getInstance().getApplicationContext();
	}
	
	/****
	 *
	 * @return
	 */
	public static CCUApplication ccu()
	{
		return Globals.getInstance().ccu();
	}
	
	/****
	 *
	 * @return
	 */
	public static byte[] getEncryptionKey()
	{
		return EncryptionPrefs.getEncryptionKey();
	}
	
	/****
	 *
	 * @return
	 */
	public static byte[] getFirmwareSignatureKey()
	{
		return EncryptionPrefs.getFirmwareSignatureKey();
	}
	
	/****
	 *
	 * @return
	 */
	public static byte[] getBLELinkKey()
	{
		return EncryptionPrefs.getBLELinkKey();
	}
	
	public static void saveCCUState()
	{
		LocalStorage.setApplicationSettings();
		sync();
	}
	
	
	private static void sync()
	{
		//seed all ccus
		//send settings messages
		//send controls messages
	}
	
	public static short generateSmartNodeAddress()
	{
		return LSmartNode.nextSmartNodeAddress();
	}

	public static Zone findZoneByName(String mFloorName, String mRoomName)
	{
		return ZoneBLL.findZoneByName(mFloorName, mRoomName);
	}
	public static Node getSmartNodeAndSeed(Zone zone, short address, String mRoomName)
	{
		return LSmartNode.getSmartNodeAndSeed(zone, address, mRoomName);
	}
	public static void sendLightControlsMessage(Zone zone)
	{
        //TODO: revist
		//ZoneBLL.sendControlsMessage(zone);
	}
	
	
	public static void addZoneProfileToZone(Node node, Zone zone, LightProfile mLightProfile)
	{
		
		saveCCUState();
	}
    

}
