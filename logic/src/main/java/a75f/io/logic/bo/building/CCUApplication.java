package a75f.io.logic.bo.building;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import a75f.io.logic.bo.building.bypassdamper.BypassDamperProfile;
import a75f.io.logic.bo.building.oao.OAOProfile;
import a75f.io.logic.bo.building.system.SystemProfile;
import a75f.io.logic.bo.haystack.device.ControlMote;

/**
 * Created by Yinten isOn 8/15/2017.
 */
@JsonSerialize
public class CCUApplication
{

    public SystemProfile   systemProfile = null;
    public Set<ZoneProfile> zoneProfiles  = ConcurrentHashMap.newKeySet();
    public OAOProfile oaoProfile = null;
    public BypassDamperProfile bypassDamperProfile = null;

    private String           mTitle        = "";
    private ArrayList<Floor> mfloors       = new ArrayList<Floor>();
    private short           mSmartNodeAddressBand;
    private String mCCUName;
    

    public String getCCUName(){
        return mCCUName;
    }
    public void setCCUName(String name){
        mCCUName = name;
    }
    
    public short getSmartNodeAddressBand()
    {
        return mSmartNodeAddressBand;
    }

    public void setSmartNodeAddressBand(short smartNodeAddressBand)
    {
        this.mSmartNodeAddressBand = smartNodeAddressBand;
    }

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


    @JsonIgnore
    public String getFloorRef(short addr) {
        for (Floor f : mfloors) {
            for (Zone z : f.mZoneList) {
                for (ZoneProfile zp : z.mZoneProfiles) {
                    zp.getNodeAddresses().contains(addr);
                    return f.mFloorRef;
                }
            }
        }
        return "";
     }

}
