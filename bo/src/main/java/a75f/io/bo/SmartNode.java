package a75f.io.bo;

import a75f.io.bo.interfaces.ISerial;

public class SmartNode implements ISerial
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
	
	
	@Override
	public void fromBytes(byte[] bytes)
	{
	}
	
	
	@Override
	public byte[] toBytes()
	{
		return new byte[0];
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

