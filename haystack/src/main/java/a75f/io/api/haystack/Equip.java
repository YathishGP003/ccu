package a75f.io.api.haystack;

import java.util.ArrayList;

/**
 * Created by samjithsadasivan on 9/4/18.
 */

public class Equip
{
    private String            displayName;
    private ArrayList<String> markers;
    private String            siteRef;
    private String roomRef;
    private String floorRef;
    private String group;
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
    public ArrayList<String> getMarkers()
    {
        return markers;
    }
    public String getSiteRef()
    {
        return siteRef;
    }
    public static class Builder{
        private String            displayName;
        private ArrayList<String> markers = new ArrayList<>();
        private String            siteRef;
        private String roomRef;
        private String floorRef;
        private String group;
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
            q.roomRef = this.roomRef;
            q.floorRef = this.floorRef;
            q.group = this.group;
            //CCUHsApi.getInstance().addEquip(q);
            return q;
        }
    }
    
    /*private void createEquip() {
        StringBuilder marker = new StringBuilder();
        for (String m : markers) {
            marker.append(m+" ");
        }
        CCUHsApi.getInstance().addEquip(siteRef, displayName, marker.toString().trim());
    }*/

}
