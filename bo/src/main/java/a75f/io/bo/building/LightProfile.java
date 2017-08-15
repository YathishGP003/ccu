package a75f.io.bo.building;

/**
 * Created by Yinten on 8/15/2017.
 */

public class LightProfile extends ZoneProfile
{
	public boolean on              = true;
	public boolean dimmable        = true;
	public short   dimmablePercent = 50;
	
	
	public void on(boolean on)
	{
		this.on = on;
		if (on)
		{
			for (int i = 0; i < this.smartNodeOutputs.size(); i++)
			{
				if (this.smartNodeOutputs.get(i).output == Output.Relay)
				{
					switch (this.smartNodeOutputs.get(i).outputRelayActuatorType)
					{
						case NormallyClose:
							//get this smart node
					}
				}
			}
		}
	}
}
