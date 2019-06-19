package a75f.io.api.haystack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by samjithsadasivan on 9/6/18.
 */

public class RawPoint extends Entity
{
    private String            displayName;
    private ArrayList<String> markers;
    public void setDeviceRef(String deviceRef)
    {
        this.deviceRef = deviceRef;
    }
    public void setSiteRef(String siteRef)
    {
        this.siteRef = siteRef;
    }
    public void setRoomRef(String roomRef)
    {
        this.roomRef = roomRef;
    }
    public void setFloorRef(String floorRef)
    {
        this.floorRef = floorRef;
    }
    private String deviceRef;
    private String pointRef;
    private String port;
    private String type;
    private String unit;
    private String tz;
    private String siteRef;
    private String roomRef;
    private String floorRef;
    private String kind;
    public boolean getEnabled()
    {
        return enabled;
    }
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
    private boolean enabled;
    private String  id;
    public String getId()
    {
        return id;
    }
    public void setId(String id)
    {
        this.id = id;
    }
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
    public String getRoomRef()
    {
        return roomRef;
    }
    public String getFloorRef()
    {
        return floorRef;
    }
    public String getKind()
    {
        return kind;
    }
    public String toString() {
        return displayName;
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
        private String roomRef;
        private String floorRef;
        private String kind;
        public Builder setEnabled(boolean enabled)
        {
            this.enabled = enabled;
            return this;
        }
        private boolean enabled;
        private String  id;
    
        public Builder setKind(String kind)
        {
            this.kind = kind;
            return this;
        }
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
        public Builder setRoomRef(String roomRef)
        {
            this.roomRef = roomRef;
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
            p.roomRef = this.roomRef;
            p.floorRef = this.floorRef;
            p.kind = this.kind;
            p.id = this.id;
            p.enabled = this.enabled;
            //CCUHsApi.getInstance().addRawPoint(p);
            return p;
        }
    
        public Builder setHashMap(HashMap site)
        {
            Iterator it = site.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry pair = (Map.Entry) it.next();
                //System.out.println(pair.getKey() + " = " + pair.getValue());
                if (pair.getKey().equals("id"))
                {
                    this.id = pair.getValue().toString();
                }
                else if (pair.getKey().equals("dis"))
                {
                    this.displayName = pair.getValue().toString();
                }
                else if(pair.getValue().toString().equals("marker")/*pair.getKey().equals("marker")*/) //TODO
                {
                    this.markers.add(pair.getKey().toString()/*pair.getValue().toString()*/);
                }
                else if (pair.getKey().equals("siteRef"))
                {
                    this.siteRef = pair.getValue().toString();
                }
                else if (pair.getKey().equals("floorRef"))
                {
                    this.floorRef = pair.getValue().toString();
                }
                else if (pair.getKey().equals("roomRef"))
                {
                    this.roomRef = pair.getValue().toString();
                }
                else if (pair.getKey().equals("deviceRef"))
                {
                    this.deviceRef = pair.getValue().toString();
                }
                else if (pair.getKey().equals("pointRef"))
                {
                    this.pointRef = pair.getValue().toString();
                }
                else if (pair.getKey().equals("port"))
                {
                    this.port = pair.getValue().toString();
                }
                else if (pair.getKey().equals("analogType"))
                {
                    this.type = pair.getValue().toString();
                }
                else if (pair.getKey().equals("unit"))
                {
                    this.unit = pair.getValue().toString();
                }
                else if (pair.getKey().equals("kind"))
                {
                    this.kind = pair.getValue().toString();
                }
                else if (pair.getKey().equals("enabled"))
                {
                    this.enabled = Boolean.parseBoolean(pair.getValue().toString());
                }
                else if (pair.getKey().equals("tz"))
                {
                    this.tz = pair.getValue().toString();
                }
                //it.remove();
            }
            return this;
        }
    }
    
    
}
