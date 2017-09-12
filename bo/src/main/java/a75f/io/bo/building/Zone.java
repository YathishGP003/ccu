package a75f.io.bo.building;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Yinten isOn 8/15/2017.
 */
//Also known as room.
public class Zone
{
	public String roomName = "Default Zone";
	public LightProfile mLightProfile;
	
	
	public Zone()
	{
	}
	
	
	//Also known as zone name.
	public Zone(String roomName)
	{
		this.roomName = roomName;
	}
	
	
	@Override
	public String toString()
	{
		return roomName;
	}
	
	
	public ZoneProfile findLightProfile()
	{
		return mLightProfile != null ? mLightProfile : new LightProfile();
	}
	
	
	public Short[] findSmartNodeAddresses()
	{
		Set<Short> shortHashSet = new HashSet<Short>();
		for (SmartNodeInput smartNodeInput : mLightProfile.smartNodeInputs)
		{
			shortHashSet.add(smartNodeInput.mSmartNodeAddress);
		}
		for (SmartNodeOutput smartNodeOutput : mLightProfile.smartNodeOutputs)
		{
			shortHashSet.add(smartNodeOutput.mSmartNodeAddress);
		}
		return shortHashSet.toArray(new Short[shortHashSet.size()]);
	}
}
