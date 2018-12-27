package a75f.io.api.haystack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
/**
 * Created by samjithsadasivan on 12/17/18.
 */

/**
 *  A Setting point with value saved as one of the tags.
 */
public class SettingPoint extends Entity
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
    private String deviceRef;
    private String siteRef;
    private String kind;
    private String id;
    private String unit;
    public String getVal()
    {
        return val;
    }
    private String val;
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
    public String getSiteRef()
    {
        return siteRef;
    }
    public String getKind()
    {
        return kind;
    }
    public String getId()
    {
        return id;
    }
    public String getUnit()
    {
        return unit;
    }
    public static class Builder {
        private String            displayName;
        private ArrayList<String> markers = new ArrayList<>();
        private String            deviceRef;
        private String            siteRef;
        private String kind;
        private String id;
        private String unit;
        public Builder setVal(String val)
        {
            this.val = val;
            return this;
        }
        private String val;
        
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
        public Builder setKind(String kind)
        {
            this.kind = kind;
            return this;
        }
        public Builder setId(String id)
        {
            this.id = id;
            return this;
        }
        public Builder setUnit(String unit)
        {
            this.unit = unit;
            return this;
        }
    
        public Builder addMarker(String marker)
        {
            this.markers.add(marker);
            return this;
        }
    
        public SettingPoint build(){
            SettingPoint p = new SettingPoint();
            p.displayName = this.displayName;
            p.markers = this.markers;
            p.siteRef = this.siteRef;
            p.deviceRef = this.deviceRef;
            p.unit = this.unit;
            p.id = this.id;
            p.kind = this.kind;
            p.val = this.val;
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
                else if (pair.getKey().equals("deviceRef"))
                {
                    this.deviceRef = pair.getValue().toString();
                }
                else if (pair.getKey().equals("unit"))
                {
                    this.unit = pair.getValue().toString();
                }
                else if (pair.getKey().equals("kind"))
                {
                    this.kind = pair.getValue().toString();
                }
                else if (pair.getKey().equals("val"))
                {
                    this.val = pair.getValue().toString();
                }
                it.remove();
            }
            return this;
        }
    }
    
}
