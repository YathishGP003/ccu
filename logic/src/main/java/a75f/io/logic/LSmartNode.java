package a75f.io.logic;

import a75f.io.bo.building.Floor;
import a75f.io.bo.building.Node;
import a75f.io.bo.building.Zone;

import static a75f.io.logic.L.ccu;

/**
 * Created by Yinten isOn 8/17/2017.
 */

class LSmartNode
{
    
    public static short nextSmartNodeAddress()
    {
        short currentBand = ccu().getSmartNodeAddressBand();
        int amountOfNodes = 0;
        for (Floor floors : ccu().getFloors())
        {
            for (Zone zone : floors.mRoomList)
            {
                amountOfNodes += zone.getNodes().size();
            }
        }
        return (short) (currentBand + amountOfNodes);
    }
    
    
    public static Node getSmartNodeAndSeed(Zone zone, short mPairingAddress, String mName)
    {
        Node node = null;
        if (zone.getNodes().containsKey(mPairingAddress))
        {
            node = zone.getNodes().get(mPairingAddress);
        }
        else
        {
            node = new Node();
            node.setAddress(mPairingAddress);
        }
        
        //seedSmartNode(node, mName);
        return node;
    }
    
    
    
    
}
