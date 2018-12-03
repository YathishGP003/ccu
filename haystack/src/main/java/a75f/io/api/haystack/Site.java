package a75f.io.api.haystack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by samjithsadasivan on 9/4/18.
 */
public class Site
{
    private String            displayName;
    private ArrayList<String> markers;
    private String geoCity;
    private String geoState;
    private String geoAddress;
    private String geoZip;
    private String tz;
    private double area;
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
        return geoAddress;
    }
    public String getGeoZip()
    {
        return geoZip;
    }
    public String getTz()
    {
        return tz;
    }
    public double getArea()
    {
        return area;
    }
    private Site() {
    
    }
    
    public static class Builder
    {
        private String            displayName;
        private ArrayList<String> markers = new ArrayList<>();
        private String geoCity;
        private String geoState;
        private String geoAddress;
        private String geoZipCode;
        private String tz;
        private String id;

        private double area;


        public Builder setGeoZip(String siteZip) {
            this.geoZipCode = siteZip;
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
        public Builder setGeoAddress(String geoAddress)
        {
            this.geoAddress = geoAddress;
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
        
        public Site build()
        {
            Site s = new Site();
            s.displayName = this.displayName;
            s.markers = this.markers;
            s.geoAddress = this.geoAddress;
            s.geoCity = this.geoCity;
            s.geoState = this.geoState;
            s.area = this.area;
            s.tz = this.tz;
            s.geoZip = this.geoZipCode;
            return s;
        }

        public Builder setHashMap(HashMap site)
        {
            Iterator it = site.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                System.out.println(pair.getKey() + " = " + pair.getValue());
                if(pair.getKey().equals("id"))
                {
                    this.id = (String)pair.getValue();
                }
                else if(pair.getKey().equals("dis"))
                {
                    this.displayName = (String)pair.getValue();
                }
                else if(pair.getKey().equals("geoCity"))
                {
                    this.geoCity = (String)pair.getValue();
                }
                else if(pair.getKey().equals("geoState"))
                {
                    this.geoState = (String)pair.getValue();
                }
                else if(pair.getKey().equals("geoZipCode"))
                {
                    this.geoZipCode = (String)pair.getValue();
                }
                else if(pair.getKey().equals("area"))
                {
                    this.area = (Double)pair.getValue();
                }

                it.remove();
            }

            return this;
        }

    }
}
