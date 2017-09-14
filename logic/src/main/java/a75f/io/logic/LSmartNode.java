package a75f.io.logic;

import java.util.ArrayList;
import java.util.UUID;

import a75f.io.bo.building.Floor;
import a75f.io.bo.building.SmartNode;
import a75f.io.bo.building.SmartNodeOutput;
import a75f.io.bo.building.Zone;
import a75f.io.bo.serial.CcuToCmOverUsbDatabaseSeedSnMessage_t;
import a75f.io.bo.serial.MessageType;

import static a75f.io.logic.L.ccu;

/**
 * Created by Yinten isOn 8/17/2017.
 */

class LSmartNode
{
	
	public static short nextSmartNodeAddress()
	{
		short currentBand = ccu().getSmartNodeAddressBand();
		return (short) (currentBand + ccu().smartNodes.size());
	}
	
	public static SmartNode getSmartNodeAndSeed(short mPairingAddress, String mName)
	{
		for (SmartNode smartNode : ccu().smartNodes)
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
		CcuToCmOverUsbDatabaseSeedSnMessage_t ccuToCmOverUsbDatabaseSeedSnMessage_t = new CcuToCmOverUsbDatabaseSeedSnMessage_t();
		ccuToCmOverUsbDatabaseSeedSnMessage_t.messageType.set(MessageType.CCU_TO_CM_OVER_USB_DATABASE_SEED_SN);
		ccuToCmOverUsbDatabaseSeedSnMessage_t.smartNodeAddress.set(smartNode.mAddress);
		ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.lightingIntensityForOccupantDetected.set((short) 100);
		ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.minLightingControlOverrideTimeInMinutes.set((short) 1);
		ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.roomName.set(mRoomName);
		ccuToCmOverUsbDatabaseSeedSnMessage_t.settings.profileBitmap.lightingControl.set((short) 1);
		LSerial.getInstance().sendSerialStruct(ccuToCmOverUsbDatabaseSeedSnMessage_t);
	}
	
	public static void addSmartNode(SmartNode smartNode)
	{
		ArrayList<SmartNode> smartNodes = ccu().smartNodes;
		if (!smartNodes.contains(smartNode))
		{
			smartNodes.add(smartNode);
		}
	}
	
	public static SmartNodeOutput findSmartNodePortByUUID(UUID id)
	{
		ArrayList<Floor> floors = ccu().getFloors();
		for (Floor f : floors)
		{
			for (Zone z : f.mRoomList)
			{
				if (z.mLightProfile != null)
				{
					for (SmartNodeOutput op : z.mLightProfile.smartNodeOutputs)
					{
						if (id.equals(op.getUuid()))
						{
							return op;
						}
					}
				}
			}
		}
		return null;
	}
}
