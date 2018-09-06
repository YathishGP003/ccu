package a75f.io.logic.bo.haystack;

import java.util.ArrayList;

/**
 * Created by samjithsadasivan on 9/4/18.
 */

public class Site
{
    private String            displayName;
    private ArrayList<String> markers;
    private String geoCity;
    private String geoState;
    private String geoAddress;
    private String tz;
    private int area;
    public String getDisplayName()
    {
        return displayName;
    }
    public ArrayList<String> getMarkers()
    {
        return markers;
    }
    public String getGeoCity()
    {
        return geoCity;
    }
    public String getGeoState()
    {
        return geoState;
    }
    public String getGeoAddress()
    {
        return geoAddress;
    }
    public String getTz()
    {
        return tz;
    }
    public int getArea()
    {
        return area;
    }
    private Site() {
    
    }
    
    public static class Builder
    {
        private String            displayName;
        private ArrayList<String> markers = new ArrayList<>();
        private String geoCity;
        private String geoState;
        private String geoAddress;
        private String tz;
        private int area;
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
        public Builder addMarker(String marker)
        {
            this.markers.add(marker);
            return this;
        }
        public Builder setGeoCity(String geoCity)
        {
            this.geoCity = geoCity;
            return this;
        }
        public Builder setGeoState(String geoState)
        {
            this.geoState = geoState;
            return this;
        }
        public Builder setGeoAddress(String geoAddress)
        {
            this.geoAddress = geoAddress;
            return this;
        }
        public Builder setTz(String tz)
        {
            this.tz = tz;
            return this;
        }
        public Builder setArea(int area)
        {
            this.area = area;
            return this;
        }
        
        public Site build()
        {
            Site s = new Site();
            s.displayName = this.displayName;
            s.markers = this.markers;
            s.geoAddress = this.geoAddress;
            s.geoCity = this.geoCity;
            s.geoState = this.geoState;
            s.area = this.area;
            s.tz = this.tz;
            //CCUHsApi.getInstance().addSite(s);
            return s;
        }
    }
    
    /*private void createSite() {
        CCUHsApi.getInstance().addSite(displayName, geoCity, geoState, tz, area);
    }*/


}
