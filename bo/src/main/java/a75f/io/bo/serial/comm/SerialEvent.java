package a75f.io.bo.serial.comm;

import a75f.io.bo.building.BaseEvent;

public class SerialEvent extends BaseEvent
{
	private SerialAction mSerialAction;
	private byte[]       mBytes;
	
	
	public SerialEvent()
	{ /* Default constructor */ }
	
	
	public SerialEvent(SerialAction mAction)
	{
		this.mSerialAction = mAction;
		this.mBytes = null;
	}
	
	
	public SerialEvent(SerialAction mAction, byte[] bytes)
	{
		this.mSerialAction = mAction;
		this.mBytes = bytes;
	}
	
	
	public SerialAction getSerialAction()
	{
		return mSerialAction;
	}
	
	
	public void setSerialAction(SerialAction serialAction)
	{
		this.mSerialAction = serialAction;
	}
	
	
	public byte[] getBytes()
	{
		return mBytes;
	}
	
	
	public void setBytes(byte[] bytes)
	{
		this.mBytes = bytes;
	}
}