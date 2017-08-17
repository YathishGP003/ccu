package a75f.io.bo.building;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Yinten on 8/15/2017.
 */
@JsonSerialize
public class CCUApplication
{
	public String               CCUTitle      = new String();
	public List<Zone>           zones         = new ArrayList<Zone>();
	public SystemProfile        systemProfile = new SystemProfile();
	public ControlMote          controlMote   = new ControlMote();
	public ArrayList<SmartNode> smartNodes    = new ArrayList<SmartNode>();
	
	
	public SmartNode findSmartNodeByIOUUID(UUID uniqueID)
	{
		for (SmartNode smartNode : smartNodes)
		{
			if (smartNode.hasIOAddress(uniqueID))
			{
				return smartNode;
			}
		}
		return null;
	}
}
