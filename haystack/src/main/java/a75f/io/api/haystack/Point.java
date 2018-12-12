package a75f.io.api.haystack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by samjithsadasivan on 9/4/18.
 */

public class Point
{
    private String displayName;
    private ArrayList<String> markers;
    private String siteRef;
    private String equipRef;
    private String unit;
    private String tz;
    private String zoneRef;
    private String floorRef;
    private String group;
    private String id;
    public String getId()
    {
        return id;
    }
    public String getGroup()
    {
        return group;
    }
    public String getZoneRef()
    {
        return zoneRef;
    }
    public String getFloorRef()
    {
        return floorRef;
    }
    public String getDisplayName()
    {
        return displayName;
    }
    public ArrayList<String> getMarkers()
    {
        return markers;
    }
    public String getSiteRef()
    {
        return siteRef;
    }
    public String getEquipRef()
    {
        return equipRef;
    }
    public String getUnit()
    {
        return unit;
    }
    public String getTz()
    {
        return tz;
    }
    public String toString() {
        return displayName;
    }
    private Point(){
    }
    
    public static class Builder{
        private String displayName;
        private ArrayList<String> markers = new ArrayList<>();
        private String siteRef;
        private String equipRef;
        private String unit;
        private String tz;
        private String zoneRef;
        private String floorRef;
        private String group;
        private String id;
        public Builder setGroup(String group)
        {
            this.group = group;
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
        public Builder setSiteRef(String siteRef)
        {
            this.siteRef = siteRef;
            return this;
        }
        public Builder setEquipRef(String equipRef)
        {
            this.equipRef = equipRef;
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
        public Builder addMarker(String marker)
        {
            this.markers.add(marker);
            return this;
        }
        
        public Point build(){
            Point p = new Point();
            p.displayName = this.displayName;
            p.markers = this.markers;
            p.siteRef = this.siteRef;
            p.equipRef = this.equipRef;
            p.unit = this.unit;
            p.tz = this.tz;
            p.zoneRef = this.zoneRef;
            p.floorRef = this.floorRef;
            p.group = this.group;
            p.id = this.id;
            //CCUHsApi.getInstance().addPoint(p);
            return p;
        }
    
        public Builder setHashMap(HashMap site)
        {
            Iterator it = site.entrySet().iterator();
            while (it.hasNext())
            {
                Map.Entry pair = (Map.Entry) it.next();
                System.out.println(pair.getKey() + " = " + pair.getValue());
                if (pair.getKey().equals("id"))
                {
                    this.id = pair.getValue().toString();
                }
                else if (pair.getKey().equals("dis"))
                {
                    this.displayName = pair.getValue().toString();
                }
                else if (pair.getKey().equals("marker"))
                {
                    this.markers.add(pair.getValue().toString());
                }
                else if (pair.getKey().equals("siteRef"))
                {
                    this.siteRef = pair.getValue().toString();
                }
                else if (pair.getKey().equals("floorRef"))
                {
                    this.floorRef = pair.getValue().toString();
                }
                else if (pair.getKey().equals("zoneRef"))
                {
                    this.zoneRef = pair.getValue().toString();
                }
                else if (pair.getKey().equals("equipRef"))
                {
                    this.equipRef = pair.getValue().toString();
                }
                else if (pair.getKey().equals("group"))
                {
                    this.group = pair.getValue().toString();
                }
                else if (pair.getKey().equals("unit"))
                {
                    this.unit = pair.getValue().toString();
                }
                else if (pair.getKey().equals("tz"))
                {
                    this.tz = pair.getValue().toString();
                }
                it.remove();
            }
            return this;
        }
    }
}
