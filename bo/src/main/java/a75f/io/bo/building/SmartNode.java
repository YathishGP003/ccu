package a75f.io.bo.building;

import java.util.UUID;

/**
 * Created by Yinten on 8/15/2017.
 */

public class SmartNode
{
	public String roomName = new String();
	public int  address;
	public UUID relay1Id;
	public UUID relay2Id;
	public UUID analog1OutId;
	public UUID analog2OutId;
	public UUID analog1InId;
	public UUID analog2InId;
	
	
	public boolean hasIOAddress(UUID uniqueID)
	{
		if (uniqueID == relay1Id)
		{
			return true;
		}
		else if (uniqueID == relay2Id)
		{
			return true;
		}
		else if (uniqueID == analog1OutId)
		{
			return true;
		}
		else if (uniqueID == analog2OutId)
		{
			return true;
		}
		else if (uniqueID == analog1InId)
		{
			return true;
		}
		else if (uniqueID == analog2InId)
		{
			return true;
		}
		return false;
	}
}
