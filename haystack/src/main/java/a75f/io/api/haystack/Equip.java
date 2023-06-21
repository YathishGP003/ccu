package a75f.io.api.haystack;

import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HVal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by samjithsadasivan on 9/4/18.
 */

public class Equip extends Entity
{
    private String            displayName;
    private HashSet<String> markers;
    private String siteRef;
    private String roomRef;
    private String floorRef;
    private String createByApplication;
    private String ccuRef;

    private String domainName;

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    private String vendor;
    private String model;
    public void setAhuRef(String ahuRef)
    {
        this.ahuRef = ahuRef;
    }
    public String getAhuRef()
    {
        return ahuRef;
    }
    private String ahuRef;
    public String getGatewayRef()
    {
        return gatewayRef;
    }
    public void setGatewayRef(String ahuRef)
    {
        this.gatewayRef = ahuRef;
    }
    private String gatewayRef;//only for default System profile, eg: zones with standalone equipments which doesnt relay on System
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

    private Map<String, HVal> tags = new HashMap<>();
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
    public String getId()
    {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getGroup()
    {
        return group;
    }
    public String getRoomRef()
    {
        return roomRef;
    }
    public String getFloorRef()
    {
        return floorRef;
    }
    public String getDisplayName()
    {
        return displayName;
    }
    public HashSet<String> getMarkers()
    {
        return markers;
    }
    public String getSiteRef()
    {
        return siteRef;
    }
    public String getCreateByApplication() { return createByApplication; }
    public String toString() {
        return displayName;
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

    public Map<String, HVal> getTags() {
        return tags;
    }
    public static class Builder{
        private String            displayName;
        private HashSet<String> markers = new HashSet<>();
        private String            siteRef;
        private String roomRef;
        private String floorRef;
        private String group;
        private String createdByApplication;
        private String ccuRef;
        private HDateTime createdDateTime;
        private HDateTime lastModifiedDateTime;
        private String lastModifiedBy;

        private String domainName;

        public Builder setAhuRef(String ahuRef)
        {
            this.ahuRef = ahuRef;
            return this;
        }
        private String ahuRef;
        public Builder setGatewayRef(String gatewayRef)
        {
            this.gatewayRef = gatewayRef;
            return this;
        }
        private String gatewayRef;
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
        private String vendor;
        private String model;

        private Map<String, HVal> tags = new HashMap<>();

        public Builder setVendor(String vendor)
        {
            this.vendor = vendor;
            return this;
        }
        public Builder setModel(String model)
        {
            this.model = model;
            return this;
        }
        public Builder setGroup(String group)
        {
            this.group = group;
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
        public Builder setDisplayName(String displayName)
        {
            this.displayName = displayName;
            return this;
        }
        public Builder setMarkers(HashSet<String> markers)
        {
            this.markers = markers;
            return this;
        }
        public Builder addMarker(String m) {
            this.markers.add(m);
            return this;
        }
        public Builder removeMarker(String marker)
        {
            if(this.markers.contains(marker))
                this.markers.remove(marker);
            return this;
        }
        public Builder setSiteRef(String siteRef)
        {
            this.siteRef = siteRef;
            return this;
        }
        public Builder setCcuRefRef(String ccuRef)
        {
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
        public Builder setCreatedByApplication(String createdByApplication) {
            this.createdByApplication = createdByApplication;
            return this;
        }

        public Builder setDomainName(String domainName) {
            this.domainName = domainName;
            return this;
        }
        public Builder addTag(String tag, HVal val) {
            this.tags.put(tag, val);
            return this;
        }
        public Equip build() {
            
            Equip q = new Equip();
            q.displayName = this.displayName;
            q.markers = this.markers;
            q.siteRef = this.siteRef;
            q.roomRef = this.roomRef;
            q.floorRef = this.floorRef;
            q.group = this.group;
            q.profile = this.profile;
            q.priority = this.priority;
            q.ahuRef = this.ahuRef;
            q.gatewayRef = this.gatewayRef;
            q.id = this.id;
            q.tz = this.tz;
            q.model = this.model;
            q.vendor = this.vendor;
            q.createByApplication = this.createdByApplication;
            q.ccuRef = this.ccuRef;
            q.setCreatedDateTime(createdDateTime);
            q.setLastModifiedDateTime(lastModifiedDateTime);
            q.setLastModifiedBy(lastModifiedBy);
            q.setDomainName(domainName);
            q.tags = this.tags;
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
                else if(pair.getValue().toString().equals("marker")/*pair.getKey().equals("marker")*/) //TODO
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
                else if(pair.getKey().equals("roomRef"))
                {
                    this.roomRef = pair.getValue().toString();
                }
                else if(pair.getKey().equals("ahuRef"))
                {
                    this.ahuRef = pair.getValue().toString();
                }
                else if(pair.getKey().equals("gatewayRef"))
                {
                    
                    this.gatewayRef = pair.getValue().toString();
                }
                else if(pair.getKey().equals("profile"))
                {
                    this.profile  = pair.getValue().toString();
                }
                else if(pair.getKey().equals("group"))
                {
                    this.group = pair.getValue().toString();
                }
                else if(pair.getKey().equals("priorityLevel"))
                {
                    this.priority = pair.getValue().toString();
                }
                else if(pair.getKey().equals("tz"))
                {
                    this.tz = pair.getValue().toString();
                }
                else if(pair.getKey().equals("vendor"))
                {
                    this.vendor = pair.getValue().toString();
                }
                else if(pair.getKey().equals("model"))
                {
                    this.model = pair.getValue().toString();
                }
                else if(pair.getKey().equals("createdByApplication"))
                {
                    this.createdByApplication = pair.getValue().toString();
                }
                else if(pair.getKey().equals("ccuRef"))
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
                //it.remove();
            }
            return this;
        }

        public Builder setHDict(HDict equipDict)
        {

            Iterator it = equipDict.iterator();
            while (it.hasNext()) {
                HDict.MapEntry pair =  (HDict.MapEntry) it.next();
                if(pair.getKey().equals("id"))
                {
                    this.id = pair.getValue().toString();
                }
                else if(pair.getKey().equals("dis"))
                {
                    this.displayName = pair.getValue().toString();
                }
                else if(pair.getValue().toString().equals("marker")/*pair.getKey().equals("marker")*/) //TODO
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
                else if(pair.getKey().equals("roomRef"))
                {
                    this.roomRef = pair.getValue().toString();
                }
                else if(pair.getKey().equals("ahuRef"))
                {
                    this.ahuRef = pair.getValue().toString();
                }
                else if(pair.getKey().equals("gatewayRef"))
                {

                    this.gatewayRef = pair.getValue().toString();
                }
                else if(pair.getKey().equals("profile"))
                {
                    this.profile  = pair.getValue().toString();
                }
                else if(pair.getKey().equals("group"))
                {
                    this.group = pair.getValue().toString();
                }
                else if(pair.getKey().equals("priorityLevel"))
                {
                    this.priority = pair.getValue().toString();
                }
                else if(pair.getKey().equals("tz"))
                {
                    this.tz = pair.getValue().toString();
                }
                else if(pair.getKey().equals("vendor"))
                {
                    this.vendor = pair.getValue().toString();
                }
                else if(pair.getKey().equals("model"))
                {
                    this.model = pair.getValue().toString();
                }
                else if(pair.getKey().equals("createdByApplication"))
                {
                    this.createdByApplication = pair.getValue().toString();
                }
                else if(pair.getKey().equals("ccuRef"))
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
                else {
                    this.tags.put(pair.getKey().toString(), (HVal) pair.getValue());
                }
            }
            return this;
        }
    }
}
