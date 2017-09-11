package a75f.io.logic;

import a75f.io.bo.building.BaseEvent;
import a75f.io.bo.serial.comm.SerialEvent;

/**
 * Created by Yinten on 9/10/2017.
 */

public class LEventManager
{
	
	public static void handleEvent(BaseEvent event)
	{
		if(event instanceof SerialEvent)
		{
			SerialBLL.getInstance().handleSerialEvent((SerialEvent) event);
		}
		
	}
	
}
