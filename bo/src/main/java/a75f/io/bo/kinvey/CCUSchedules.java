package a75f.io.bo.kinvey;
/**
 * Created by Yinten on 9/4/2017.
 */

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;
import com.kinvey.java.model.KinveyMetaData;

import java.util.ArrayList;

public class CCUSchedules extends GenericJson
{ // For Serialization
	
	@Key("_id")
	public String  id;
	@Key
	public String  monday_OccuTime;
	@Key
	public Integer monday_OccupTemp;
	@Key
	public String  monday_UnoccuTime;
	@Key
	public String  tuesday_OccuTime;
	@Key
	public Integer tuesday_OccupTemp;
	@Key
	public String  tuesday_UnoccuTime;
	@Key
	public String  wednesday_OccuTime;
	@Key
	public Integer wednesday_OccupTemp;
	@Key
	public String  wednesday_UnoccuTime;
	@Key
	public String  thursday_OccuTime;
	@Key
	public Integer thursday_OccupTemp;
	@Key
	public String  thursday_UnoccuTime;
	@Key
	public String  friday_OccuTime;
	@Key
	public Integer friday_OccupTemp;
	@Key
	public String  friday_UnoccuTime;
	@Key
	public String  saturday_OccuTime;
	@Key
	public Integer saturday_OccupTemp;
	@Key
	public String  saturday_UnoccuTime;
	@Key
	public String  sunday_OccuTime;
	@Key
	public Integer sunday_OccupTemp;
	@Key
	public String  sunday_UnoccuTime;
	
	@Key
	public ArrayList<LCMSchedule>           lcm_zone_schedule;
	@Key
	public Object                           wrm_details;
	@Key
	public String                           ccu_id;
	@Key
	public Integer                          room_id;
	@Key
	public Integer                          floor_id;
	@Key
	public String                           name;
	@Key
	public String                           src;



    @Key("_kmd")
    private KinveyMetaData meta;
    @Key("_acl")
    private KinveyMetaData.AccessControlList acl;

	public CCUSchedules()
	{
	}  //GenericJson classes must have a public empty constructor
	
	
	public String getId()
	{
		return id;
	}
	
	
	public void setId(String id)
	{
		this.id = id;
	}
	
	
	public String getMonday_OccuTime()
	{
		return monday_OccuTime;
	}
	
	
	public void setMonday_OccuTime(String monday_OccuTime)
	{
		this.monday_OccuTime = monday_OccuTime;
	}
	
	
	public Integer getMonday_OccupTemp()
	{
		return monday_OccupTemp;
	}
	
	
	public void setMonday_OccupTemp(Integer monday_OccupTemp)
	{
		this.monday_OccupTemp = monday_OccupTemp;
	}
	
	
	public String getMonday_UnoccuTime()
	{
		return monday_UnoccuTime;
	}
	
	
	public void setMonday_UnoccuTime(String monday_UnoccuTime)
	{
		this.monday_UnoccuTime = monday_UnoccuTime;
	}
	
	
	public String getTuesday_OccuTime()
	{
		return tuesday_OccuTime;
	}
	
	
	public void setTuesday_OccuTime(String tuesday_OccuTime)
	{
		this.tuesday_OccuTime = tuesday_OccuTime;
	}
	
	
	public Integer getTuesday_OccupTemp()
	{
		return tuesday_OccupTemp;
	}
	
	
	public void setTuesday_OccupTemp(Integer tuesday_OccupTemp)
	{
		this.tuesday_OccupTemp = tuesday_OccupTemp;
	}
	
	
	public String getTuesday_UnoccuTime()
	{
		return tuesday_UnoccuTime;
	}
	
	
	public void setTuesday_UnoccuTime(String tuesday_UnoccuTime)
	{
		this.tuesday_UnoccuTime = tuesday_UnoccuTime;
	}
	
	
	public String getWednesday_OccuTime()
	{
		return wednesday_OccuTime;
	}
	
	
	public void setWednesday_OccuTime(String wednesday_OccuTime)
	{
		this.wednesday_OccuTime = wednesday_OccuTime;
	}
	
	
	public Integer getWednesday_OccupTemp()
	{
		return wednesday_OccupTemp;
	}
	
	
	public void setWednesday_OccupTemp(Integer wednesday_OccupTemp)
	{
		this.wednesday_OccupTemp = wednesday_OccupTemp;
	}
	
	
	public String getWednesday_UnoccuTime()
	{
		return wednesday_UnoccuTime;
	}
	
	
	public void setWednesday_UnoccuTime(String wednesday_UnoccuTime)
	{
		this.wednesday_UnoccuTime = wednesday_UnoccuTime;
	}
	
	
	public String getThursday_OccuTime()
	{
		return thursday_OccuTime;
	}
	
	
	public void setThursday_OccuTime(String thursday_OccuTime)
	{
		this.thursday_OccuTime = thursday_OccuTime;
	}
	
	
	public Integer getThursday_OccupTemp()
	{
		return thursday_OccupTemp;
	}
	
	
	public void setThursday_OccupTemp(Integer thursday_OccupTemp)
	{
		this.thursday_OccupTemp = thursday_OccupTemp;
	}
	
	
	public String getThursday_UnoccuTime()
	{
		return thursday_UnoccuTime;
	}
	
	
	public void setThursday_UnoccuTime(String thursday_UnoccuTime)
	{
		this.thursday_UnoccuTime = thursday_UnoccuTime;
	}
	
	
	public String getFriday_OccuTime()
	{
		return friday_OccuTime;
	}
	
	
	public void setFriday_OccuTime(String friday_OccuTime)
	{
		this.friday_OccuTime = friday_OccuTime;
	}
	
	
	public Integer getFriday_OccupTemp()
	{
		return friday_OccupTemp;
	}
	
	
	public void setFriday_OccupTemp(Integer friday_OccupTemp)
	{
		this.friday_OccupTemp = friday_OccupTemp;
	}
	
	
	public String getFriday_UnoccuTime()
	{
		return friday_UnoccuTime;
	}
	
	
	public void setFriday_UnoccuTime(String friday_UnoccuTime)
	{
		this.friday_UnoccuTime = friday_UnoccuTime;
	}
	
	
	public String getSaturday_OccuTime()
	{
		return saturday_OccuTime;
	}
	
	
	public void setSaturday_OccuTime(String saturday_OccuTime)
	{
		this.saturday_OccuTime = saturday_OccuTime;
	}
	
	
	public Integer getSaturday_OccupTemp()
	{
		return saturday_OccupTemp;
	}
	
	
	public void setSaturday_OccupTemp(Integer saturday_OccupTemp)
	{
		this.saturday_OccupTemp = saturday_OccupTemp;
	}
	
	
	public String getSaturday_UnoccuTime()
	{
		return saturday_UnoccuTime;
	}
	
	
	public void setSaturday_UnoccuTime(String saturday_UnoccuTime)
	{
		this.saturday_UnoccuTime = saturday_UnoccuTime;
	}
	
	
	public String getSunday_OccuTime()
	{
		return sunday_OccuTime;
	}
	
	
	public void setSunday_OccuTime(String sunday_OccuTime)
	{
		this.sunday_OccuTime = sunday_OccuTime;
	}
	
	
	public Integer getSunday_OccupTemp()
	{
		return sunday_OccupTemp;
	}
	
	
	public void setSunday_OccupTemp(Integer sunday_OccupTemp)
	{
		this.sunday_OccupTemp = sunday_OccupTemp;
	}
	
	
	public String getSunday_UnoccuTime()
	{
		return sunday_UnoccuTime;
	}
	
	
	public void setSunday_UnoccuTime(String sunday_UnoccuTime)
	{
		this.sunday_UnoccuTime = sunday_UnoccuTime;
	}
	
	
	public ArrayList<LCMSchedule> getLcm_zone_schedule()
	{
		return lcm_zone_schedule;
	}
	
	
	public void setLcm_zone_schedule(ArrayList<LCMSchedule> lcm_zone_schedule)
	{
		this.lcm_zone_schedule = lcm_zone_schedule;
	}
	
	
	public Object getWrm_details()
	{
		return wrm_details;
	}
	
	
	public void setWrm_details(Object wrm_details)
	{
		this.wrm_details = wrm_details;
	}
	
	
	public String getCcu_id()
	{
		return ccu_id;
	}
	
	
	public void setCcu_id(String ccu_id)
	{
		this.ccu_id = ccu_id;
	}
	
	
	public Integer getRoom_id()
	{
		return room_id;
	}
	
	
	public void setRoom_id(Integer room_id)
	{
		this.room_id = room_id;
	}
	
	
	public Integer getFloor_id()
	{
		return floor_id;
	}
	
	
	public void setFloor_id(Integer floor_id)
	{
		this.floor_id = floor_id;
	}
	
	
	public String getName()
	{
		return name;
	}
	
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	
	public String getSrc()
	{
		return src;
	}
	
	
	public void setSrc(String src)
	{
		this.src = src;
	}
	

}

