package a75f.io.logic.bo.building;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Tags;

/**
 * Created by samjithsadasivan isOn 8/8/17.
 */

public class Floor
{
    public static final String FLOOR_NAME  = "FloorName";
    public static final String FLOOR_WEBID = "FloorWebId";
    public int             mFloorId;
    public String          mFloorName;
    public String          mFloorRef;
    public ArrayList<Zone> mZoneList;
    
    
    public Floor()
    {
    }
    
    
    public Floor(int floorId, String webId, String floor)
    {
        mFloorId = floorId;
    
        HashMap siteMap = CCUHsApi.getInstance().read(Tags.SITE);
        String siteRef = (String) siteMap.get(Tags.ID);
        
        a75f.io.api.haystack.Floor hsFloor = new a75f.io.api.haystack.Floor.Builder()
                                                    .setDisplayName(floor)
                                                    .setSiteRef(siteRef)
                                                    .build();
        mFloorRef = CCUHsApi.getInstance().addFloor(hsFloor);
        mFloorName = floor;
        mZoneList = new ArrayList<Zone>();
    }
    
    
    public String toString()
    {
        return mFloorName;
    }
    
}
