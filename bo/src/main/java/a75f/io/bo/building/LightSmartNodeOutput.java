package a75f.io.bo.building;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;

import a75f.io.bo.serial.CcuToCmOverUsbSnLightingScheduleMessage_t;
import a75f.io.bo.serial.MessageType;
import a75f.io.bo.serial.SmartNodeLightingCircuit_t;

/**
 * Created by samjithsadasivan on 8/29/17.
 */

public class LightSmartNodeOutput extends SmartNodeOutput
{
	
	public boolean override = false;
	public boolean on = false;
	public short dimmable = 100;
	
	public LightingSchedule schedule = null;
	
}
