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
        private ArrayList<String> markers = new ArrayList<>();;
        private String            floorRef;
        public Builder setDisplayName(String displayName)
        {
            this.displayName = displayName;
            return this;
        }
        public Builder setMarkers(ArrayList<String> markers)
        {
            this.markers = markers;
            return this;
        }
        public Builder setFloorRef(String floorRef)
        {
            this.floorRef = floorRef;
            return this;
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
