package a75f.io.api.haystack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by samjithsadasivan on 9/4/18.
 */

public class Equip
{
    private String            displayName;
    private ArrayList<String> markers;
    private String            siteRef;
    private String zoneRef;
    private String floorRef;
    private String group;
    public String getProfile()
    {
        return profile;
    }
    private String profile;
    public String getPriority()
    {
        return priority;
    }
    private String priority;
    private String id;
    public String getTz()
    {
        return tz;
    }
    private String tz;
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
    public String toString() {
        return displayName;
    }
    public static class Builder{
        private String            displayName;
        private ArrayList<String> markers = new ArrayList<>();
        private String            siteRef;
        private String zoneRef;
        private String floorRef;
        private String group;
        public Builder setTz(String tz)
        {
            this.tz = tz;
            return this;
        }
        private String tz;
        public Builder setPriority(String priority)
        {
            this.priority = priority;
            return this;
        }
        private String priority;
        private String id;
        public Builder setProfile(String profile)
        {
            this.profile = profile;
            return this;
        }
        private String profile;
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
        public Builder addMarker(String m) {
            this.markers.add(m);
            return this;
        }
        public Builder setSiteRef(String siteRef)
        {
            this.siteRef = siteRef;
            return this;
        }
        public Equip build() {
            
            Equip q = new Equip();
            q.displayName = this.displayName;
            q.markers = this.markers;
            q.siteRef = this.siteRef;
            q.zoneRef = this.zoneRef;
            q.floorRef = this.floorRef;
            q.group = this.group;
            q.profile = this.profile;
            q.priority = this.priority;
            q.id = this.id;
            q.tz = this.tz;
            return q;
        }
        
        public Builder setHashMap(HashMap site)
        {
            
            Iterator it = site.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                //System.out.println(pair.getKey() + " = " + pair.getValue());
                if(pair.getKey().equals("id"))
                {
                    this.id = pair.getValue().toString();
                }
                else if(pair.getKey().equals("dis"))
                {
                    this.displayName = pair.getValue().toString();
                }
                else if(pair.getValue().equals("marker")/*pair.getKey().equals("marker")*/) //TODO
                {
                    this.markers.add(pair.getKey().toString()/*pair.getValue().toString()*/);
                }
                else if(pair.getKey().equals("siteRef"))
                {
                    this.siteRef = pair.getValue().toString();
                }
                else if(pair.getKey().equals("floorRef"))
                {
                    this.floorRef = pair.getValue().toString();
                }
                else if(pair.getKey().equals("zoneRef"))
                {
                    this.zoneRef = pair.getValue().toString();
                }
                else if(pair.getKey().equals("profile"))
                {
                    this.profile  = pair.getValue().toString();
                }
                else if(pair.getKey().equals("group"))
                {
                    this.group = pair.getValue().toString();
                }
                else if(pair.getKey().equals("priority"))
                {
                    this.priority = pair.getValue().toString();
                }
                else if(pair.getKey().equals("tz"))
                {
                    this.tz = pair.getValue().toString();
                }
                it.remove();
            }
            return this;
        }
    }
}
