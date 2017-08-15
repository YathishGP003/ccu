package a75f.io.bo;


public class SmartNode
{
	
	private String name;
	private Short  mMeshAddress;
	private SmartType mSmartType = SmartType.SmartNode;
	
	
	public SmartNode()
	{
	    /* Default Constructor */
	}
	
	
	public SmartNode(String name)
	{
		this.name = name;
	}
	
	
	public String getName()
	{
		if (name == null || name.equals(""))
		{
			name = "Default Room";
		}
		return name;
	}
	
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	
	
	
	public short getMeshAddress()
	{
		return mMeshAddress;
	}
	
	
	public void setMeshAddress(short meshAddress)
	{
		mMeshAddress = meshAddress;
	}
	
	
	public enum SmartType
	{
		SmartNode, SmartStat
	}
}

