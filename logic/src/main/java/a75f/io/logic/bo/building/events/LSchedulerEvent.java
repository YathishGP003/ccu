package a75f.io.logic.bo.building.events;

import java.util.UUID;

import a75f.io.logic.bo.building.BaseEvent;
import a75f.io.logic.bo.building.definitions.LScheduleAction;

/**
 * Created by Yinten on 9/10/2017.
 */

public class LSchedulerEvent extends BaseEvent
{
	private UUID            mUUID;
	private LScheduleAction mLScheduleAction;
	
	
	public LSchedulerEvent()
	{
	}
	
	
	public LSchedulerEvent(UUID mUUID, LScheduleAction mLScheduleAction)
	{
		this.mUUID = mUUID;
		this.mLScheduleAction = mLScheduleAction;
	}
	
	
	public LSchedulerEvent(UUID mUUID)
	{
		this.mUUID = mUUID;
	}
	
	
	public LScheduleAction getmLScheduleAction()
	{
		return mLScheduleAction;
	}
	
	
	public void setmLScheduleAction(LScheduleAction mLScheduleAction)
	{
		this.mLScheduleAction = mLScheduleAction;
	}
	
	
	public UUID getmUUID()
	{
		return mUUID;
	}
	
	
	public void setmUUID(UUID mUUID)
	{
		this.mUUID = mUUID;
	}
}
