package a75f.io.bo.building;

/**
 * Created by Yinten on 8/15/2017.
 */

public class LightProfile extends ZoneProfile
{
	public boolean on              = true;
	public boolean dimmable        = true;
	public short   dimmablePercent = 50;
	
	
	
	
	public LightProfile(String name)
	{
		super(name);
	}
	
	
	public void on(boolean on)
	{
		this.on = on;
		if (on)
		{
			for (int i = 0; i < this.smartNodeOutputs.size(); i++)
			{
				switch (this.smartNodeOutputs.get(i).mOutput)
				{
					case Relay:
						switch (this.smartNodeOutputs.get(i).mOutputRelayActuatorType)
						{
							case NormallyClose:
								break;
							
							default:
								break;
						}
						break;
					
					case Analog:
						break;
				}
			}
		}
	}
}
