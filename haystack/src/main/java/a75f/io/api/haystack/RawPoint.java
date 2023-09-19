package a75f.io.api.haystack;

import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HStr;
import org.projecthaystack.HVal;

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
    private String ccuRef;
    private String roomRef;
    private String floorRef;
    private String kind;
    private String shortDis;
    private String minVal;
    private String maxVal;
    private String registerAddress;
    private String registerNumber;
    private String startBit;
    private String endBit;
    private String registerType;
    private String parameterId;

    private String domainName;
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

    /**
     * Support for arbitrary KVP. This is only intended for new tags/profiles at this time.
     * Eventually we will move other existing tags to similar format.
     */
    private Map<String, HVal> tags = new HashMap<>();

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
    public void setUnit(String unit) { this.unit = unit; }
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

    public String getShortDis() {
        return shortDis;
    }

    public String getMinVal() {
        return minVal;
    }

    public String getMaxVal() {
        return maxVal;
    }
    public String getRegisterAddress() {
        return registerAddress;
    }
    public String getRegisterNumber() {
        return registerNumber;
    }
    public String getStartBit() {
        return startBit;
    }
    public String getEndBit() {
        return endBit;
    }
    public String getRegisterType() {
        return registerType;
    }
    public String getParameterId() {
        return parameterId;
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
        private ArrayList<String> markers = new ArrayList<>();
        private String siteRef;
        private String ccuRef;
        private HDateTime createdDateTime;
        private HDateTime lastModifiedDateTime;
        private String lastModifiedBy;
        private String deviceRef;
        private String pointRef;
        private String port;
        private String type;
        private String unit;
        private String tz;
        private String roomRef;
        private String floorRef;
        private Kind kind;
        private String shortDis;
        private String minVal;
        private String maxVal;
        private String registerAddress;
        private String registerNumber;
        private String startBit;
        private String endBit;
        private String registerType;
        private String parameterId;
        private String domainName;

        private Map<String, HVal> tags = new HashMap<>();
        public Builder setEnabled(boolean enabled)
        {
            this.enabled = enabled;
            return this;
        }
        private boolean enabled;
        private String  id;

        public Builder setKind(Kind kind)
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
        public Builder setCceRef(String ccuRef)
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

        public Builder setShortDis(String shortDis) {
            this.shortDis = shortDis;
            return this;
        }

        public Builder setMinVal(String minVal) {
            this.minVal = minVal;
            return this;
        }

        public Builder setMaxVal(String maxVal) {
            this.maxVal = maxVal;
            return this;
        }

        public Builder setRegisterAddress(String addr) {
            this.registerAddress = addr;
            return this;
        }

        public Builder setRegisterNumber(String regNumber) {
            this.registerNumber = regNumber;
            return this;
        }

        public Builder setStartBit(String startBit) {
            this.startBit = startBit;
            return this;
        }

        public Builder setEndBit(String endBit) {
            this.endBit = endBit;
            return this;
        }
    
        public Builder setRegisterType(String registerType) {
            this.registerType = registerType;
            return this;
        }

        public Builder setParameterId(String parameterId) {
            this.parameterId = parameterId;
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
            p.ccuRef = this.ccuRef;
            p.setCreatedDateTime(createdDateTime);
            p.setLastModifiedDateTime(lastModifiedDateTime);
            p.setLastModifiedBy(lastModifiedBy);
            p.roomRef = this.roomRef;
            p.floorRef = this.floorRef;
            p.kind = this.kind != null ? this.kind.getValue() : Kind.NUMBER.getValue();
            p.id = this.id;
            p.enabled = this.enabled;
            p.shortDis = this.shortDis;
            p.minVal = this.minVal;
            p.maxVal = this.maxVal;
            p.registerAddress = this.registerAddress;
            p.registerNumber = this.registerNumber;
            p.startBit = this.startBit;
            p.endBit = this.endBit;
            p.registerType = this.registerType;
            p.parameterId = this.parameterId;
            p.domainName = this.domainName;
            p.tags = this.tags;
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
                    String value = pair.getValue().toString();

                    // support old values if needed for migration.
                    if (value.equalsIgnoreCase("string")) {
                        this.kind = Kind.STRING;
                    }
                    else {
                        this.kind = Kind.parse(value);
                    }
                }
                else if (pair.getKey().equals("portEnabled"))
                {
                    // we could/should say = pair.getValue == HBool.TRUE, but will hold off for now (warroom & keep code consistent)
                    this.enabled = Boolean.parseBoolean(pair.getValue().toString());
                }
                else if (pair.getKey().equals("tz"))
                {
                    this.tz = pair.getValue().toString();
                }
                else if (pair.getKey().equals("shortDis"))
                {
                    this.shortDis = pair.getValue().toString();
                }
                else if (pair.getKey().equals("maxVal"))
                {
                    this.maxVal = pair.getValue().toString();
                }
                else if (pair.getKey().equals("minVal"))
                {
                    this.minVal = pair.getValue().toString();
                }
                else if (pair.getKey().equals("registerAddress"))
                {
                    this.registerAddress = pair.getValue().toString();
                }
                else if (pair.getKey().equals("registerNumber"))
                {
                    this.registerNumber = pair.getValue().toString();
                }
                else if (pair.getKey().equals("startBit"))
                {
                    this.startBit = pair.getValue().toString();
                }
                else if (pair.getKey().equals("endBit"))
                {
                    this.endBit = pair.getValue().toString();
                }
                else if (pair.getKey().equals("registerType"))
                {
                    this.registerType = pair.getValue().toString();
                }
                else if (pair.getKey().equals("parameterId"))
                {
                    this.parameterId = pair.getValue().toString();
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
                } else {
                    this.tags.put(pair.getKey().toString(), (HVal) pair.getValue());
                }

            }
            return this;
        }

        public Builder setHDict(HDict pointDict)
        {
            Iterator it = pointDict.iterator();
            while (it.hasNext())
            {
                HDict.MapEntry pair =  (HDict.MapEntry) it.next();
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
                    String value = pair.getValue().toString();

                    // support old values if needed for migration.
                    if (value.equalsIgnoreCase("string")) {
                        this.kind = Kind.STRING;
                    }
                    else {
                        this.kind = Kind.parse(value);
                    }
                }
                else if (pair.getKey().equals("portEnabled"))
                {
                    // we could/should say = pair.getValue == HBool.TRUE, but will hold off for now (warroom & keep code consistent)
                    this.enabled = Boolean.parseBoolean(pair.getValue().toString());
                }
                else if (pair.getKey().equals("tz"))
                {
                    this.tz = pair.getValue().toString();
                }
                else if (pair.getKey().equals("shortDis"))
                {
                    this.shortDis = pair.getValue().toString();
                }
                else if (pair.getKey().equals("maxVal"))
                {
                    this.maxVal = pair.getValue().toString();
                }
                else if (pair.getKey().equals("minVal"))
                {
                    this.minVal = pair.getValue().toString();
                }
                else if (pair.getKey().equals("registerAddress"))
                {
                    this.registerAddress = pair.getValue().toString();
                }
                else if (pair.getKey().equals("registerNumber"))
                {
                    this.registerNumber = pair.getValue().toString();
                }
                else if (pair.getKey().equals("startBit"))
                {
                    this.startBit = pair.getValue().toString();
                }
                else if (pair.getKey().equals("endBit"))
                {
                    this.endBit = pair.getValue().toString();
                }
                else if (pair.getKey().equals("registerType"))
                {
                    this.registerType = pair.getValue().toString();
                }else if (pair.getKey().equals("parameterId"))
                {
                    this.parameterId = pair.getValue().toString();
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
                else {
                    this.tags.put(pair.getKey().toString(), (HVal) pair.getValue());
                }
            }
            return this;
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (this.getClass() != obj.getClass()) return false;
        RawPoint that = (RawPoint) obj;
        if (!this.domainName.equals(that.domainName)) return false;
        if (!this.displayName.equals(that.displayName)) return false;
        if (!this.deviceRef.equals(that.deviceRef)) return false;
        if (!this.pointRef.equals(that.pointRef)) return false;
        if (!this.type.equals(that.type)) return false;
        if (!this.unit.equals(that.unit)) return false;
        if (!this.port.equals(that.port)) return false;
        if (this.enabled != that.enabled) return false;
        if (this.markers.size() != (that.markers.size())) return false;
        for (String marker : this.markers) {
            if (!this.markers.contains(marker)) {
                return false;
            }
        }
        return true;
    }


}
