package a75f.io.logic;

import java.util.ArrayList;

import a75f.io.bo.building.LightProfile;
import a75f.io.bo.building.SmartNode;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;
import a75f.io.bo.serial.MessageType;
import a75f.io.logic.cache.Globals;

/**
 * Created by Yinten isOn 8/17/2017.
 */

public class SmartNodeBLL
{
	
	public static short nextSmartNodeAddress()
	{
		short currentBand = Globals.getInstance().getCCUApplication().getSmartNodeAddressBand();
		//currentBand + current number of paired smart nodes.
		return (short) (currentBand + Globals.getInstance().getCCUApplication().smartNodes.size());
	}
	
	
	public static SmartNode getSmartNodeAndSeed(short mPairingAddress, String mName)
	{
		for (SmartNode smartNode : Globals.getInstance().getCCUApplication().smartNodes)
		{
			if (smartNode.mAddress == mPairingAddress)
			{
				seedSmartNode(smartNode, mName);
				return smartNode;
			}
		}
		SmartNode smartNode = new SmartNode();
		smartNode.mAddress = mPairingAddress;
		seedSmartNode(smartNode, mName);
		return smartNode;
	}
	
	
	private static void seedSmartNode(SmartNode smartNode, String mRoomName)
	{
		CcuToCmOverUsbDatabaseSeedSnMessage_t ccuToCmOverUsbDatabaseSeedSnMessage_t =
				new CcuToCmOverUsbDatabaseSeedSnMessage_t();
		ccuToCmOverUsbDatabaseSeedSnMessage_t.messageType
				.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
		ccuToCmOverUsbDatabaseSeedSnMessage_t.smartNodeAddress.set(smartNode.mAddress);
		ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.lightingIntensityForOccupantDetected
				.set((short) 100);
		ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.minLightingControlOverrideTimeInMinutes
				.set((short) 1);
		ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.roomName.set(mRoomName);
		ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.profileBitmap.lightingControl.set((short) 1);
		SerialBLL.getInstance().sendSerialStruct(ccuToCmOverUsbDatabaseSeedSnMessage_t);
	}
	
	
	public static void addSmartNode(SmartNode smartNode)
	{
		ArrayList<SmartNode> smartNodes = Globals.getInstance().getCCUApplication().smartNodes;
		if (!smartNodes.contains(smartNode))
		{
			smartNodes.add(smartNode);
		}
	}
	
	
	
}
