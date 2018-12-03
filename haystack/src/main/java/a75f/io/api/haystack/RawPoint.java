package a75f.io.api.haystack;

import java.util.ArrayList;

/**
 * Created by samjithsadasivan on 9/6/18.
 */

public class RawPoint
{
    private String            displayName;
    private ArrayList<String> markers;
    private String deviceRef;
    private String pointRef;
    private String port;
    private String type;
    private String unit;
    private String tz;
    private String siteRef;
    private String zoneRef;
    private String floorRef;
    public void setPointRef(String pointRef)
    {
        this.pointRef = pointRef;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getSiteRef()
    {
        return siteRef;
    }
    public String getDisplayName()
    {
        return displayName;
    }
    public ArrayList<String> getMarkers()
    {
        return markers;
    }
    public String getDeviceRef()
    {
        return deviceRef;
    }
    public String getPointRef()
    {
        return pointRef;
    }
    public String getPort()
    {
        return port;
    }
    public String getType()
    {
        return type;
    }
    public String getUnit()
    {
        return unit;
    }
    public String getTz()
    {
        return tz;
    }
    public String getZoneRef()
    {
        return zoneRef;
    }
    public String getFloorRef()
    {
        return floorRef;
    }
    public static class Builder{
        private String            displayName;
        private ArrayList<String> markers = new ArrayList<>();
        private String siteRef;
        private String deviceRef;
        private String pointRef;
        private String port;
        private String type;
        private String unit = "F";
        private String tz;
        private String zoneRef;
        private String floorRef;
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
        public Builder setDeviceRef(String deviceRef)
        {
            this.deviceRef = deviceRef;
            return this;
        }
        public Builder setSiteRef(String siteRef)
        {
            this.siteRef = siteRef;
            return this;
        }
        public Builder setPointRef(String pointRef)
        {
            this.pointRef = pointRef;
            return this;
        }
        public Builder setPort(String port)
        {
            this.port = port;
            return this;
        }
        public Builder setType(String type)
        {
            this.type = type;
            return this;
        }
        public Builder addMarker(String marker)
        {
            this.markers.add(marker);
            return this;
        }
        public Builder setUnit(String unit)
        {
            this.unit = unit;
            return this;
        }
        public Builder setTz(String tz)
        {
            this.tz = tz;
            return this;
        }
        public Builder setZoneRef(String zoneRef)
        {
            this.zoneRef = zoneRef;
            return this;
        }
        public Builder setFloorRef(String floorRef)
        {
            this.floorRef = floorRef;
            return this;
        }
        
        public RawPoint build(){
            RawPoint p = new RawPoint();
            p.displayName = this.displayName;
            p.markers = this.markers;
            p.deviceRef = this.deviceRef;
            p.pointRef = this.pointRef;
            p.port = this.port;
            p.type = this.type;
            p.unit = this.unit;
            p.tz = this.tz;
            p.siteRef = this.siteRef;
            p.zoneRef = this.zoneRef;
            p.floorRef = this.floorRef;
            //CCUHsApi.getInstance().addRawPoint(p);
            return p;
        }
    }
    
    
}
