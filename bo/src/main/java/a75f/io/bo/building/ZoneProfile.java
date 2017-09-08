package a75f.io.bo.building;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.List;

import a75f.io.bo.serial.CcuToCmOverUsbSnControlsMessage_t;

/**
 * Created by Yinten on 8/15/2017.
 */
@JsonDeserialize(as = LightProfile.class)
@JsonSerialize(as = LightProfile.class)
public abstract class ZoneProfile
{
	public ZoneProfile(){
		
	}
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
	
	public String toString() {
		return mModuleName;
	}
	
}
