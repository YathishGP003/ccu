package a75f.io.bo.building;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by Yinten isOn 8/15/2017.
 */
@JsonSerialize
public class CCUApplication
{
	public String           CCUTitle = new String();
	public ArrayList<Floor> floors   = new ArrayList<Floor>();
	
	public SystemProfile        systemProfile = new SystemProfile();
	public ControlMote          controlMote   = new ControlMote();
	public ArrayList<SmartNode> smartNodes    = new ArrayList<>();
	private short mSmartNodeAddressBand;
	
	
	public short getSmartNodeAddressBand()
	{
		return mSmartNodeAddressBand;
	}
	
	
	public void setSmartNodeAddressBand(short smartNodeAddressBand)
	{
		this.mSmartNodeAddressBand = smartNodeAddressBand;
	}
	
	
	public SmartNode findSmartNodeByAddress(short smartNodeAddress)
	{
		for (SmartNode smartNode : smartNodes)
		{
			if (smartNode.mAddress == smartNodeAddress)
			{
				return smartNode;
			}
		}
		return null;
	}
	
	
	public SmartNodeOutput findSmartNodePortByUUID(UUID id)
	{
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
	
	
	public ZoneProfile findZoneProfileByUUID(UUID uuid)
	{
		for (Floor f : floors)
		{
			for (Zone z : f.mRoomList)
			{
				if (z.mLightProfile.uuid.equals(uuid))
				{
					return z.mLightProfile;
				}
			}
		}
		return null;
	}
	
	
	public ArrayList<ZoneProfile> findAllZoneProfiles()
	{
		ArrayList<ZoneProfile> zoneProfiles = new ArrayList<>();
		for (Floor f : floors)
		{
			for (Zone z : f.mRoomList)
			{
				if (z.mLightProfile != null)
				{
					zoneProfiles.add(z.mLightProfile);
				}
			}
		}
		return zoneProfiles;
	}
}
