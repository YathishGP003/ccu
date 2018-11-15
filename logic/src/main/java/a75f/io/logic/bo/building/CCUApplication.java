package a75f.io.logic.bo.building;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.logic.bo.building.system.SystemProfile;
import a75f.io.logic.bo.building.system.VavAnalogRtu;

/**
 * Created by Yinten isOn 8/15/2017.
 */
@JsonSerialize
public class CCUApplication
{
    /*
        These need to be moved into tuners
     */
    private ArrayList<Schedule> mDefaultLightSchedule = new ArrayList<Schedule>();
    private ArrayList<Schedule> mDefaultTemperatureSchedule = new ArrayList<Schedule>();

    private int mTestTimeHH;
    private int mTestTimeMM;
    private int mTestTimeDoW;

    public int getTestTimeHH()
    {
        return mTestTimeHH;
    }
    public void setTestTimeHH(int testTimeHH)
    {
        this.mTestTimeHH = testTimeHH;
    }
    public int getTestTimeMM()
    {
        return mTestTimeMM;
    }
    public void setTestTimeMM(int testTimeMM)
    {
        this.mTestTimeMM = testTimeMM;
    }
    public int getTestTimeDoW()
    {
        return mTestTimeDoW;
    }
    public void setTestTimeDoW(int testTimeDoW)
    {
        this.mTestTimeDoW = testTimeDoW;
    }


    /*
        Named Schedules
     */
    private HashMap<String, NamedSchedule> mLCMNamedSchedules = new HashMap<>();
    
    
    public SystemProfile systemProfile = new VavAnalogRtu();//TODO- TEMP
    
    public  ControlMote      controlMote   = new ControlMote();
    private String           mTitle        = "";
    private ArrayList<Floor> mfloors       = new ArrayList<Floor>();
    private short           mSmartNodeAddressBand;

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


    public HashMap<String, NamedSchedule> getLCMNamedSchedules()
    {
        return mLCMNamedSchedules;
    }


    public void setLCMNamedSchedules(HashMap<String, NamedSchedule> namedSchedules)
    {
        this.mLCMNamedSchedules = namedSchedules;
    }
    
    public String getFloor(short addr) {
        for (Floor f : mfloors) {
            for (Zone z : f.mZoneList) {
                for (ZoneProfile zp : z.mZoneProfiles) {
                    zp.getNodeAddresses().contains(addr);
                    return f.mFloorName;
                }
            }
        }
        return "";
     }
    
    public String getRoom(short addr) {
        for (Floor f : mfloors) {
            for (Zone z : f.mZoneList) {
                for (ZoneProfile zp : z.mZoneProfiles) {
                    zp.getNodeAddresses().contains(addr);
                    return z.roomName;
                }
            }
        }
        return "";
    }
}
