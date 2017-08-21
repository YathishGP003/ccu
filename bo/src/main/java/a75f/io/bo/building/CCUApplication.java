package a75f.io.bo.building;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;

/**
 * Created by Yinten on 8/15/2017.
 */
@JsonSerialize
public class CCUApplication
{
	public String      CCUTitle = new String();
	public ArrayList<Floor> floors   = new ArrayList<Floor>();
	
	public SystemProfile        systemProfile = new SystemProfile();
	public ControlMote          controlMote   = new ControlMote();
	public ArrayList<SmartNode> smartNodes    = new ArrayList<SmartNode>();
	public short mSmartNodeBand;
	
	
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
}
