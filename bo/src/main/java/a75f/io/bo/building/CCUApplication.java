package a75f.io.bo.building;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Yinten isOn 8/15/2017.
 */
@JsonSerialize
public class CCUApplication
{
    private ArrayList<Schedule> mDefaultLightSchedule = new ArrayList<Schedule>();
    private ArrayList<Schedule> mDefaultTemperatureSchedule = new ArrayList<Schedule>();
    private  HashMap<String, Object> mDefaultCCUTuners = new HashMap<>();
    public  SystemProfile           systemProfile    = new SystemProfile();
    public  ControlMote             controlMote      = new ControlMote();
    private String                  mTitle           = "";
    private ArrayList<Floor>        mfloors          = new ArrayList<Floor>();
    private short                mSmartNodeAddressBand;
    
    public short getSmartNodeAddressBand()
    {
        return mSmartNodeAddressBand;
    }
    
    public void setSmartNodeAddressBand(short smartNodeAddressBand)
    {
        this.mSmartNodeAddressBand = smartNodeAddressBand;
    }
    //
    //
    //
    //	public Node findSmartNodeByAddress(short smartNodeAddress)
    //	{
    //		for (Node node : nodes)
    //		{
    //			if (node.getmAddress() == smartNodeAddress)
    //			{
    //				return node;
    //			}
    //		}
    //		return null;
    //	}
    //
    //
    //
    //
    //
    //	public ZoneProfile findZoneProfileByUUID(UUID uuid)
    //	{
    //		for (Floor f : floors)
    //		{
    //			for (Zone z : f.mRoomList)
    //			{
    //				if (z.mLightProfile.uuid.equals(uuid))
    //				{
    //					return z.mLightProfile;
    //				}
    //			}
    //		}
    //		return null;
    //	}
    //
    //
    //	public ArrayList<ZoneProfile> findAllZoneProfiles()
    //	{
    //		ArrayList<ZoneProfile> zoneProfiles = new ArrayList<>();
    //		for (Floor f : floors)
    //		{
    //			for (Zone z : f.mRoomList)
    //			{
    //				if (z.mLightProfile != null)
    //				{
    //					zoneProfiles.add(z.mLightProfile);
    //				}
    //			}
    //		}
    //		return zoneProfiles;
    //	}
    
    public ArrayList<Floor> getFloors()
    {
        return mfloors;
    }
    
    public void setFloors(ArrayList<Floor> floors)
    {
        this.mfloors = floors;
    }
    
    public String getTitle()
    {
        return mTitle;
    }
    
    public void setTitle(String title)
    {
        this.mTitle = title;
    }
    
    public HashMap<String, Object> getDefaultCCUTuners()
    {
        return mDefaultCCUTuners;
    }
    
    
    public void setDefaultCCUTuners(HashMap<String, Object> defaultCCUTuners)
    {
        this.mDefaultCCUTuners = defaultCCUTuners;
    }
    
    //These will be provided as tuners when I get around ot it.
    public ArrayList<Schedule> getDefaultLightSchedule()
    {
        return mDefaultLightSchedule;
    }
    
    
    public void setDefaultLightSchedule(ArrayList<Schedule> defaultLightSchedule)
    {
        this.mDefaultLightSchedule = defaultLightSchedule;
    }
    
    
    //These will be provided as tuners when I get around to it.
    public ArrayList<Schedule> getDefaultTemperatureSchedule()
    {
        return mDefaultTemperatureSchedule;
    }
    
    
    public void setDefaultTemperatureSchedule(ArrayList<Schedule> defaultTemperatureSchedule)
    {
        this.mDefaultTemperatureSchedule = defaultTemperatureSchedule;
    }
}
