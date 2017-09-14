package a75f.io.logic;

import android.content.Context;

import java.util.ArrayList;
import java.util.UUID;

import a75f.io.bo.building.CCUApplication;
import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.SmartNode;
import a75f.io.bo.building.SmartNodeOutput;
import a75f.io.bo.building.Zone;
import a75f.io.bo.building.definitions.Port;

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
		///
	}
	
	public static SmartNodeOutput findPort(ArrayList<SmartNodeOutput> smartNodeOutputs, Port port, short smartNodeAddress)
	{
		return Globals.getInstance().getLZoneProfile().findPort(smartNodeOutputs, port, smartNodeAddress);
	}
	
	public static short generateSmartNodeAddress()
	{
		return LSmartNode.nextSmartNodeAddress();
	}
	
	public static SmartNodeOutput findSmartNodePortByUUID(UUID mCurrentPortId)
	{
		return LSmartNode.findSmartNodePortByUUID(mCurrentPortId);
	}
	public static Zone findZoneByName(String mFloorName, String mRoomName)
	{
		return ZoneBLL.findZoneByName(mFloorName, mRoomName);
	}
	public static SmartNode getSmartNodeAndSeed(short mSmartNodeAddress, String mRoomName)
	{
		return LSmartNode.getSmartNodeAndSeed(mSmartNodeAddress, mRoomName);
	}
	public static void sendLightControlsMessage(LightProfile mLightProfile)
	{
		ZoneBLL.sendControlsMessage(mLightProfile);
	}
	
	
	public static void addZoneProfileToZone(SmartNode smartNode, Zone zone, LightProfile mLightProfile)
	{
		LSmartNode.addSmartNode(smartNode);
		ZoneBLL.addZoneProfileToZone(zone, mLightProfile);
		saveCCUState();
	}
}
