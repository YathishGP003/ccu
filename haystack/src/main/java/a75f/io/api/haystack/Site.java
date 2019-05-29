package a75f.io.api.haystack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by samjithsadasivan on 9/4/18.
 */
public class Site extends Entity
{
    private String            displayName;
    private ArrayList<String> markers;
    private String geoCity;
    private String geoState;
    private String geoAddr;
    private String geoCountry;
    private String geoPostalCode;
    private String tz;
    private double area;
    private String id;
    public String getId()
    {
        return id;
    }
    public String getDisplayName()
    {
        return displayName;
    }
    public ArrayList<String> getMarkers()
    {
        return markers;
    }
    public String getGeoCity()
    {
        return geoCity;
    }
    public String getGeoState()
    {
        return geoState;
    }
    public String getGeoAddress()
    {
        return geoAddr;
    }
    public String getGeoPostalCode()
    {
        return geoPostalCode;
    }
    public String getTz()
    {
        return tz;
    }
    public double getArea()
    {
        return area;
    }
    public String getGeoCountry() {return geoCountry;}
    public String toString() {
        return displayName;
    }
    private Site() {
    
    }
    
    public static class Builder
    {
        private String            displayName;
        private ArrayList<String> markers = new ArrayList<>();
        private String geoCity;
        private String geoState;
        private String geoAddr;
        private String geoPostalCode;
        private String tz;
        private String id;
        private String geoCountry;
        private double area;


        public Builder setGeoZip(String siteZip) {
            this.geoPostalCode = siteZip;
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
        public Builder addMarker(String marker)
        {
            this.markers.add(marker);
            return this;
        }
        public Builder setGeoCity(String geoCity)
        {
            this.geoCity = geoCity;
            return this;
        }
        public Builder setGeoState(String geoState)
        {
            this.geoState = geoState;
            return this;
        }
        public Builder setGeoAddress(String geoAddr)
        {
            this.geoAddr = geoAddr;
            return this;
        }
        public Builder setTz(String tz)
        {
            this.tz = tz;
            return this;
        }
        public Builder setArea(int area)
        {
            this.area = area;
            return this;
        }
        public Builder setGeoCountry(String geoCountry) {
            this.geoCountry = geoCountry;
            return this;
        }
        public Site build()
        {
            Site s = new Site();
            s.displayName = this.displayName;
            s.markers = this.markers;
            s.geoAddr = this.geoAddr;
            s.geoCity = this.geoCity;
            s.geoState = this.geoState;
            s.area = this.area;
            s.tz = this.tz;
            s.geoPostalCode = this.geoPostalCode;
            s.id = this.id;
            s.geoCountry = this.geoCountry;
            return s;
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
                else if(pair.getKey().equals("geoAddr"))
                {
                    this.geoAddr = pair.getValue().toString();
                }
                else if(pair.getKey().equals("geoCity"))
                {
                    this.geoCity = pair.getValue().toString();
                }
                else if(pair.getKey().equals("geoState"))
                {
                    this.geoState = pair.getValue().toString();
                }
                else if(pair.getKey().equals("geoCountry"))
                {
                    this.geoCountry = pair.getValue().toString();
                }
                else if(pair.getKey().equals("geoPostalCode"))
                {
                    this.geoPostalCode = pair.getValue().toString();
                }
                else if(pair.getKey().equals("tz"))
                {
                    this.tz = pair.getValue().toString();
                }
                else if(pair.getKey().equals("area"))
                {
                    this.area = Double.parseDouble(pair.getValue().toString().replaceAll("[^0-9]", ""));
                }

                it.remove();
            }

            return this;
        }

    }
}
