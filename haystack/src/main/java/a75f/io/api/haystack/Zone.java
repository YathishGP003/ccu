package a75f.io.api.haystack;

import java.util.ArrayList;

/**
 * Created by samjithsadasivan on 11/19/18.
 */

public class Zone
{
    private String            displayName;
    private ArrayList<String> markers;
    private String            floorRef;
    public String getDisplayName()
    {
        return displayName;
    }
    public ArrayList<String> getMarkers()
    {
        return markers;
    }
    public String getFloorRef()
    {
        return floorRef;
    }
    public static class Builder {
        private String            displayName;
        private ArrayList<String> markers;
        private String            floorRef;
        public void setDisplayName(String displayName)
        {
            this.displayName = displayName;
        }
        public void setMarkers(ArrayList<String> markers)
        {
            this.markers = markers;
        }
        public void setFloorRef(String floorRef)
        {
            this.floorRef = floorRef;
        }
        
        public Zone build() {
            Zone z = new Zone();
            z.displayName = this.displayName;
            z.floorRef = this.floorRef;
            z.markers = this.markers;
            return z;
        }
    }
}
