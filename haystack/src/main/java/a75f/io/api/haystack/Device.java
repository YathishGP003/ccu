package a75f.io.api.haystack;

import java.util.ArrayList;

/**
 * Created by samjithsadasivan on 9/5/18.
 */

public class Device
{
    private String            displayName;
    private ArrayList<String> markers;
    private String equipRef;
    private String addr;
    private String siteRef;
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
    private Device(){
    
    }
    
    public static class Builder{
        private String            displayName;
        private ArrayList<String> markers = new ArrayList<>();
        private String equipRef;
        private String addr;
        private String siteRef;
    
        public Builder setAddr(int addr)
        {
            this.addr = String.valueOf(addr);
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
        
        public String build(){
            Device d = new Device();
            d.displayName = this.displayName;
            d.markers = this.markers;
            d.addr = this.addr;
            d.siteRef = this.siteRef;
            d.equipRef = this.equipRef;
            return CCUHsApi.getInstance().addDevice(d);
        }
    }
    
    /*private void createDevice() {
        StringBuilder marker = new StringBuilder();
        for (String m : markers) {
            marker.append(m+" ");
        }
        CCUHsApi.getInstance().addDevice(displayName, markers.toString().trim());
    }*/
}
