package a75f.io.api.haystack;

import org.projecthaystack.HDateTime;

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
    private String ccuRef;

    private String domainName;
    private String equipRef;
    private String incrementVal;
    private String minVal;
    private String maxVal;
    private String sourcePoint;

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
    public void setId(String id) {
        this.id = id;
    }
    public void setEquipRef(String equipRef) {
        this.equipRef = equipRef;
    }
    public String getEquipRef() {
        return equipRef;
    }
    public void setIncrementVal(String incrementVal) {
        this.incrementVal = incrementVal;
    }
    public String getIncrementVal() {
        return incrementVal;
    }
    public void setMinVal(String minVal) {
        this.minVal = minVal;
    }
    public String getMinVal() {
        return minVal;
    }
    public void setMaxVal(String maxVal) {
        this.maxVal = maxVal;
    }
    public String getMaxVal() {
        return maxVal;
    }
    public void setSourcePoint(String sourcePoint){
        this.sourcePoint = sourcePoint;
    }
    public String getUnit()
    {
        return unit;
    }

    public String getCcuRef() {
        return ccuRef;
    }

    public void setCcuRef(String ccuRef) {
        this.ccuRef = ccuRef;
    }

    public String getDomainName() {
        return domainName;
    }
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public static class Builder {
        private String            displayName;
        private ArrayList<String> markers = new ArrayList<>();
        private String            deviceRef;
        private String            siteRef;
        private String kind;
        private String id;
        private String unit;
        private String ccuRef;
        private HDateTime createdDateTime;
        private HDateTime lastModifiedDateTime;
        private String lastModifiedBy;

        private String domainName;
        private String equipRef;
        private String incrementVal;
        private String minVal;
        private String maxVal;
        private String sourcePoint;

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

        public Builder setCcuRef(String ccuRef) {
            this.ccuRef = ccuRef;
            return this;
        }
        public Builder setCreatedDateTime(HDateTime createdDateTime)
        {
            this.createdDateTime = createdDateTime;
            return this;
        }
        public Builder setLastModifiedDateTime(HDateTime lastModifiedDateTime)
        {
            this.lastModifiedDateTime = lastModifiedDateTime;
            return this;
        }
        public Builder setLastModifiedBy(String lastModifiedBy)
        {
            this.lastModifiedBy = lastModifiedBy;
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

        public Builder setDomainName(String domainName)
        {
            this.domainName = domainName;
            return this;
        }
        public Builder setEquipRef(String equipRef)
        {
            this.equipRef = equipRef;
            return this;
        }
        public Builder setIncrementVal(String incrementVal)
        {
            this.incrementVal = incrementVal;
            return this;
        }
        public Builder setMinVal(String minVal)
        {
            this.minVal = minVal;
            return this;
        }
        public Builder setMaxVal(String maxVal)
        {
            this.maxVal = maxVal;
            return this;
        }
        public Builder setSourcePoint(String sourcePoint){
            this.sourcePoint = sourcePoint;
            return this;
        }
    
        public SettingPoint build(){
            SettingPoint p = new SettingPoint();
            p.displayName = this.displayName;
            p.markers = this.markers;
            p.siteRef = this.siteRef;
            p.ccuRef = this.ccuRef;
            p.setCreatedDateTime(createdDateTime);
            p.setLastModifiedDateTime(lastModifiedDateTime);
            p.setLastModifiedBy(lastModifiedBy);
            p.deviceRef = this.deviceRef;
            p.unit = this.unit;
            p.id = this.id;
            p.kind = this.kind;
            p.val = this.val;
            p.domainName = this.domainName;
            p.equipRef = this.equipRef;
            p.incrementVal = this.incrementVal;
            p.minVal = this.minVal;
            p.maxVal = this.maxVal;
            p.sourcePoint = this.sourcePoint;
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
                else if (pair.getKey().equals("ccuRef"))
                {
                    this.ccuRef = pair.getValue().toString();
                }
                else if (pair.getKey().equals("createdDateTime"))
                {
                    this.createdDateTime = HDateTime.make(pair.getValue().toString());
                }
                else if (pair.getKey().equals("lastModifiedDateTime"))
                {
                    this.lastModifiedDateTime = HDateTime.make(pair.getValue().toString());
                }
                else if (pair.getKey().equals("lastModifiedBy"))
                {
                    this.lastModifiedBy = pair.getValue().toString();
                }
                else if (pair.getKey().equals("domainName"))
                {
                    this.domainName = pair.getValue().toString();
                }
                else if (pair.getKey().equals("equipRef"))
                {
                    this.equipRef = pair.getValue().toString();
                }
                else if (pair.getKey().equals("incrementVal"))
                {
                    this.incrementVal = pair.getValue().toString();
                }
                else if (pair.getKey().equals("minVal"))
                {
                    this.minVal = pair.getValue().toString();
                }
                else if (pair.getKey().equals("maxVal"))
                {
                    this.maxVal = pair.getValue().toString();
                }
                else if (pair.getKey().equals("sourcePoint"))
                {
                    this.sourcePoint = pair.getValue().toString();
                }
            }
            return this;
        }
    }
    
}
