package a75f.io.bo.building;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Yinten on 8/15/2017.
 */

public abstract class ZoneProfile
{
	public Schedule              schedule         = new Schedule();
	public List<Sensor>          sensors          = new ArrayList<Sensor>();
	public List<SmartNodeInput>  smartNodeInputs  = new ArrayList<SmartNodeInput>();
	public List<SmartNodeOutput> smartNodeOutputs = new ArrayList<SmartNodeOutput>();
}
