package a75f.io.api.haystack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by samjithsadasivan on 9/5/18.
 */

public class Device
{
    private String            displayName;
    private ArrayList<String> markers;
    private String equipRef;
    private String siteRef;
    private String zoneRef;
    private String floorRef;
    private String addr;
    private String id;
    
    public String getAddr()
    {
        return addr;
    }
    public String getDisplayName()
    {
        return displayName;
    }
    public ArrayList<String> getMarkers()
    {
        return markers;
    }
    public String getEquipRef()
    {
        return equipRef;
    }
    public String getSiteRef() {
        return siteRef;
    }
    public String getZoneRef()
    {
        return zoneRef;
    }
    public String getFloorRef()
    {
        return floorRef;
    }
    public String getId()
    {
        return id;
    }
    private Device(){
    
    }
    
    public static class Builder{
        private String            displayName;
        private ArrayList<String> markers = new ArrayList<>();
        private String equipRef;
        private String addr;
        private String siteRef;
        private String zoneRef;
        private String floorRef;
        private String id;
        public String toString() {
            return displayName;
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
        public Builder setAddr(int addr)
        {
            this.addr = String.valueOf(addr);
            return this;
        }

        public Builder setAddr(String addr)
        {
            this.addr = addr;
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
        public Builder addMarker(String m)
        {
            this.markers.add(m);
            return this;
        }
        public Builder setEquipRef(String eq)
        {
            this.equipRef = equipRef;
            return this;
        }
    
        public Builder setSiteRef(String siteRef)
        {
            this.siteRef = siteRef;
            return this;
        }
        
        public Device build(){
            Device d = new Device();
            d.displayName = this.displayName;
            d.markers = this.markers;
            d.addr = this.addr;
            d.siteRef = this.siteRef;
            d.equipRef = this.equipRef;
            d.zoneRef = this.zoneRef;
            d.floorRef = this.floorRef;
            d.id = this.id;
            return d;
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
                else if(pair.getKey().equals("addr"))
                {
                    this.addr = pair.getValue().toString();
                }
                it.remove();
            }
        
            return this;
        }
    }
}
