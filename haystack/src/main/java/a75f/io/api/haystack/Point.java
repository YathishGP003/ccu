package a75f.io.api.haystack;

import org.apache.commons.lang3.StringUtils;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDict;
import org.projecthaystack.HNum;
import org.projecthaystack.HStr;
import org.projecthaystack.HVal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by samjithsadasivan on 9/4/18.
 */

public class Point extends Entity
{
    private String            displayName;
    private ArrayList<String> markers;
    private String            siteRef;
    private String            equipRef;
    private String            unit;
    private String            tz;
    private String            roomRef;
    private String            floorRef;
    private String            group;
    private String            kind;
    private String            id;
    private String            enums;
    private String            minVal;
    private String            maxVal;
    private String            cell;
    private String            incrementVal;
    private String            tunerGroup;
    private String            hisInterpolate;
    private String            shortDis;
    private String ccuRef;
    private int            bacnetId;
    private String            bacnetType;
    private String curStatus;

    private String domainName;

    /**
     * Support for arbitrary KVP. This is only intended for new tags/profiles at this time.
     * Eventually we will move other existing tags to similar format.
     */
    private Map<String, HVal> tags = new HashMap<>();

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }
    public void setSiteRef(String siteRef)
    {
        this.siteRef = siteRef;
    }
    public void setEquipRef(String equipRef)
    {
        this.equipRef = equipRef;
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
    public void setGroup(String group) {
        this.group = group;
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
    public String getKind()
    {
        return kind;
    }
    public String toString() {
        return displayName;
    }
    public String getEnums()
    {
        return enums;
    }
    public String getMinVal(){return minVal;}
    public String getMaxVal(){return maxVal;}
    public String getIncrementVal(){return incrementVal;}
    public String getTunerGroup(){return tunerGroup;}
    public String getHisInterpolate(){return hisInterpolate;}
    public String getShortDis() {
        return shortDis;
    }
    public void setEnums(String enums)
    {
        this.enums = enums;
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
    public int getBacnetId() { return bacnetId; }
    public void setBacnetId(int bacnetId) { this.bacnetId = bacnetId; }
    public String getBacnetType() { return bacnetType; }
    public void setBacnetType(String bacnetType) { this.bacnetType = bacnetType; }
    public String getCurStatus() { return curStatus;}
    public void setCurStatus(String curStatus) { this.curStatus = curStatus;}
    public Map<String, HVal> getTags() {
        return tags;
    }
    public String getCell() {
        return cell;
    }
    public void setCell(String cell) {
        this.cell = cell;
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
        private String roomRef;
        private String floorRef;
        private String group;
        private String enums;
        private String minVal;
        private String maxVal;
        private String cell;
        private String incrementVal;
        private String tunerGroup;
        private String hisInterpolate;
        private String shortDis;
        private String ccuRef;
        private HDateTime createdDateTime;
        private HDateTime lastModifiedDateTime;
        private String lastModifiedBy;
        private Map<String, HVal> tags = new HashMap<>();
        private int            bacnetId;
        private String            bacnetType;
        private String curStatus;

        public Builder setKind(Kind kind)
        {
            this.kind = kind;
            return this;
        }
        private Kind kind;
        private String id;

        private String domainName;

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
        public Builder setCcuRef(String ccuRef)
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
        public Builder setEnums(String enums) {
            this.enums = enums;
            return this;
        }
        public Builder addMarker(String marker)
        {
            if (!StringUtils.isEmpty(marker)) {
                this.markers.add(marker);
            }
            return this;
        }
        public Builder removeMarker(String marker)
        {
            if(this.markers.contains(marker))
                this.markers.remove(marker);
            return this;
        }
        public Builder setMinVal(String min)
        {
            this.minVal = min;
            return this;
        }
        public Builder setMaxVal(String max)
        {
            this.maxVal = max;
            return this;
        }
        public Builder setCell(String cell)
        {
            this.cell = cell;
            return this;
        }
        public Builder setIncrementVal(String inc)
        {
            this.incrementVal = inc;
            return this;
        }
        public Builder setTunerGroup(String tg)
        {
            this.tunerGroup = tg;
            return this;
        }
        public Builder setHisInterpolate(String ipolate)
        {
            this.hisInterpolate = ipolate;
            return this;
        }
        public Builder setShortDis(String shortDis) {
            this.shortDis = shortDis;
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
        public Builder setBacnetId(int bacnetId) {
            this.bacnetId = bacnetId;
            return this;
        }

        public Builder setBacnetType(String bacnetType) {
            this.bacnetType = bacnetType;
            return this;
        }

        public Builder setCurStatus(String curStatus) {
            this.curStatus = curStatus;
            return this;
        }

        public Point build(){
            Point p = new Point();
            p.displayName = this.displayName;
            p.markers = this.markers;
            p.siteRef = this.siteRef;
            p.ccuRef = this.ccuRef;
            p.setCreatedDateTime(createdDateTime);
            p.setLastModifiedDateTime(lastModifiedDateTime);
            p.setLastModifiedBy(lastModifiedBy);
            p.equipRef = this.equipRef;
            p.unit = this.unit;
            p.tz = this.tz;
            p.roomRef = this.roomRef;
            p.floorRef = this.floorRef;
            p.group = this.group;
            p.id = this.id;
            p.kind = this.kind != null ? this.kind.getValue() : Kind.NUMBER.getValue();
            p.enums = this.enums;
            p.minVal = this.minVal;
            p.maxVal = this.maxVal;
            p.cell = this.cell;
            p.incrementVal = this.incrementVal;
            p.tunerGroup = this.tunerGroup;
            p.hisInterpolate = this.hisInterpolate;
            p.shortDis = this.shortDis;
            p.domainName = this.domainName;
            p.tags = this.tags;
            p.bacnetId = this.bacnetId;
            p.bacnetType = this.bacnetType;
            p.curStatus = this.curStatus;
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
                else if (pair.getKey().equals("kind"))
                {
                    String value = pair.getValue().toString();

                    // support old values if needed for migration.
                    if (value.equals("string") || value.equals("String")) {
                        this.kind = Kind.STRING;
                    }
                    else {
                        this.kind = Kind.parse(value);
                    }
                }
                else if (pair.getKey().equals("tz"))
                {
                    this.tz = pair.getValue().toString();
                }
                else if (pair.getKey().equals("enum"))
                {
                    this.enums = pair.getValue().toString();
                }
                else if (pair.getKey().equals("minVal"))
                {
                    this.minVal = pair.getValue().toString();
                }
                else if (pair.getKey().equals("maxVal"))
                {
                    this.maxVal = pair.getValue().toString();
                }
                else if (pair.getKey().equals("incrementVal"))
                {
                    this.incrementVal = pair.getValue().toString();
                }
                else if (pair.getKey().equals("tunerGroup"))
                {
                    this.tunerGroup = pair.getValue().toString();
                }
                else if (pair.getKey().equals("hisInterpolate"))
                {
                    this.hisInterpolate = pair.getValue().toString();
                }else if (pair.getKey().equals("shortDis"))
                {
                    this.shortDis = pair.getValue().toString();
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
                else if (pair.getKey().equals("bacnetId"))
                {
                    this.bacnetId = (int) Double.parseDouble(pair.getValue().toString());
                }
                else if (pair.getKey().equals("bacnetType"))
                {
                    this.bacnetType = pair.getValue().toString();
                }
                else if (pair.getKey().equals("curStatus"))
                {
                    this.curStatus = pair.getValue().toString();
                }
                else {
                    this.tags.put(pair.getKey().toString(), HStr.make(pair.getValue().toString()));
                }
            }
            return this;
        }

        /**
         * Requires entities read using readHDict method that has all values retained as HVal.
         * Map returned by readEntity()/readAllEntities() are already converted to String. Both are retained
         * for now maintain backward compatibility.
         * @param pointDict
         * @return
         */
        public Builder setHDict(HDict pointDict) {
            Iterator it = pointDict.iterator();
            while (it.hasNext()) {
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
                else if (pair.getKey().equals("kind"))
                {
                    String value = pair.getValue().toString();

                    // support old values if needed for migration.
                    if (value.equals("string") || value.equals("String")) {
                        this.kind = Kind.STRING;
                    }
                    else {
                        this.kind = Kind.parse(value);
                    }
                }
                else if (pair.getKey().equals("tz"))
                {
                    this.tz = pair.getValue().toString();
                }
                else if (pair.getKey().equals("enum"))
                {
                    this.enums = pair.getValue().toString();
                }
                else if (pair.getKey().equals("minVal"))
                {
                    this.minVal = pair.getValue().toString();
                }
                else if (pair.getKey().equals("maxVal"))
                {
                    this.maxVal = pair.getValue().toString();
                }
                else if (pair.getKey().equals("incrementVal"))
                {
                    this.incrementVal = pair.getValue().toString();
                }
                else if (pair.getKey().equals("tunerGroup"))
                {
                    this.tunerGroup = pair.getValue().toString();
                }
                else if (pair.getKey().equals("hisInterpolate"))
                {
                    this.hisInterpolate = pair.getValue().toString();
                }else if (pair.getKey().equals("shortDis"))
                {
                    this.shortDis = pair.getValue().toString();
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
                else if (pair.getKey().equals("bacnetId"))
                {
                    this.bacnetId = (int) Double.parseDouble(pair.getValue().toString());
                }
                else if (pair.getKey().equals("bacnetType"))
                {
                    this.bacnetType = pair.getValue().toString();
                }
                else {
                    this.tags.put(pair.getKey().toString(), (HVal) pair.getValue());
                }
            }
            return this;
        }
    }
}
