package a75f.io.logic;

import a75f.io.bo.building.SmartNode;
import a75f.io.util.Globals;

/**
 * Created by Yinten on 8/17/2017.
 */

public class SmartNodeBLL
{
	
	public static short nextSmartNodeAddress()
	{
		short currentBand = Globals.getInstance().getApplicationPreferences().getSmartNodeAddressBand();
		//currentBand + current number of paired smart nodes.
		return (short) (currentBand + Globals.getInstance().getCCUApplication().smartNodes.size());
	}
	
	
	public static void addSmartNodeAndSeed(short mPairingAddress, String mName)
	{
		SmartNode smartNode = new SmartNode();
		smartNode.mAddress = mPairingAddress;
		smartNode.mRoomName = mName;
		Globals.getInstance().getCCUApplication().smartNodes.add(smartNode);
	}
	//	public static Output
	//public Room addModule(String roomName, short address)
	//{
	//		SmartNode smartNode = new SmartNode();
	//		smartNode.mAddress = address;
	//		smartNode.mRoomName = roomName;
	//
	//
	//		Globals.getInstance().getCCUApplication().smartNodes.add(smartNode);
	//		ZoneProfile zoneProfile = new ZoneProfile(roomName);
	//
	//
	//
	//		//zoneProfile.smartNodeOutputs
	//
	//		return ;
	//}
}
