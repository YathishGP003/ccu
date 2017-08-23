package a75f.io.bo.building;

import java.util.ArrayList;
import java.util.List;

import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;

/**
 * Created by Yinten on 8/15/2017.
 */

public abstract class ZoneProfile
{
	
	public ZoneProfile(String name)
	{
		this.mModuleName = name;
	}
	
	public String mModuleName;
	public Schedule              schedule         = new Schedule();
	public List<Sensor>          sensors          = new ArrayList<Sensor>();
	public List<SmartNodeInput>  smartNodeInputs  = new ArrayList<SmartNodeInput>();
	public List<SmartNodeOutput> smartNodeOutputs = new ArrayList<SmartNodeOutput>();
	
	public abstract List<CcuToCmOverUsbSnControlsMessage_t> getControlsMessage();
}
