package a75f.io.bo.building;

import org.javolution.io.Struct;

import java.util.ArrayList;

import a75f.io.bo.serial.CcuToCmOverUsbSnLightingScheduleMessage_t;
import a75f.io.bo.serial.MessageType;
import a75f.io.bo.serial.SmartNodeLightingCircuit_t;

/**
 * Created by samjithsadasivan on 9/7/17.
 */

public class LightingSchedule
{
	public String name;
	
	boolean normallyOpen;
	
	public ArrayList<LightSchedule> lightSchedules = new ArrayList<>();
	
}
