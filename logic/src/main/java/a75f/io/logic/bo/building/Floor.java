package a75f.io.logic.bo.building;

import java.util.ArrayList;

/**
 * Created by samjithsadasivan isOn 8/8/17.
 */

public class Floor
{
    public static final String FLOOR_NAME  = "FloorName";
    public static final String FLOOR_WEBID = "FloorWebId";
    public int             mFloorId;
    public String          mFloorName;
    public String          mKinveyWebId;
    public ArrayList<Zone> mZoneList;
    
    
    public Floor()
    {
    }
    
    
    public Floor(int floorId, String webId, String floor)
    {
        mFloorId = floorId;
        mKinveyWebId = webId;
        mFloorName = floor;
        mZoneList = new ArrayList<Zone>();
    }
    
    
    public String toString()
    {
        return mFloorName;
    }
    
}
