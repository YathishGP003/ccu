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
	public String      CCUTitle = new String();
	public ArrayList<Floor> floors   = new ArrayList<Floor>();
	
	public SystemProfile        systemProfile = new SystemProfile();
	public ControlMote          controlMote   = new ControlMote();
	public ArrayList<SmartNode> smartNodes    = new ArrayList<>();
	
	
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
	
	public SmartNodeOutput findSmartNodePortByUUID(UUID id) {
		for (Floor f : floors) {
			for (Zone z : f.mRoomList) {
				for (ZoneProfile p : z.zoneProfiles) {
					
					if (p instanceof LightProfile) {
						for (SmartNodeOutput op : ((LightProfile) p).smartNodeOutputs) {
							if (id.equals(op.mUniqueID)) {
								return op;
							}
						}
					} else {
						for (SmartNodeOutput op : p.smartNodeOutputs) {
							if (id.equals(op.mUniqueID)) {
								return op;
							}
						}
					}
				}
			}
		}
		return null;
	}
	
}
