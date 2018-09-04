package a75.io.bo.haystack;

import java.util.ArrayList;

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
    private Point(){
    }
    
    public static class Builder{
        private String displayName;
        private ArrayList<String> markers = new ArrayList<>();
        private String siteRef;
        private String equipRef;
        private String unit;
        private String tz;
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
            //p.createPoint();
            return p;
        }
    }
    
    /*private void createPoint() {
        StringBuilder marker = new StringBuilder();
        for (String m : markers) {
            marker.append(m+" ");
        }
        CCUHsApi.getInstance().createPoint(equipRef, displayName, unit, marker.toString().trim(), tz);
    }*/
}
