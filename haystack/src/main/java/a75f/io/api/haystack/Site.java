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
    private String geoFence;
    private String tz;
    private double area;
    public String  organization;
    public String  installerEmail;
    public String  fcManagerEmail;
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
    public String getGeoFence()
    {
        return geoFence;
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
    public String getOrganization() {return organization;}
    public String getInstallerEmail() {
        return installerEmail;
    }

    public String getFcManagerEmail() {
        return fcManagerEmail;
    }

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
        private String geoFence;
        private String tz;
        private String id;
        private String geoCountry;
        private double area;
        public String  organization;
        public String  fcManagerEmail;
        public String  installerEmail;


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
        public Builder setGeoFence(String geoFence)
        {
            this.geoFence = geoFence;
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

        public Builder setOrgnization(String organization) {
            this.organization = organization;
            return this;
        }

        public Builder setInstaller(String installer){
            this.installerEmail = installer;
            return this;
        }

        public Builder setFcManager(String fcManager){
            this.fcManagerEmail = fcManager;
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
            s.geoFence = this.geoFence;
            s.id = this.id;
            s.geoCountry = this.geoCountry;
            s.organization = this.organization;
            s.fcManagerEmail = this.fcManagerEmail;
            s.installerEmail = this.installerEmail;
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
                else if(pair.getKey().equals("geoFence"))
                {
                    this.geoFence = pair.getValue().toString();
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
                else if(pair.getKey().equals("organization"))
                {
                    this.organization = pair.getValue().toString();
                }
                else if(pair.getKey().equals("fmEmail"))
                {
                    this.fcManagerEmail = pair.getValue().toString();
                }
                else if(pair.getKey().equals("installerEmail"))
                {
                    this.installerEmail = pair.getValue().toString();
                }

                //it.remove();
            }

            return this;
        }

    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        
        if (obj.getClass() != this.getClass()) {
            return false;
        }
        
        Site site = (Site) obj;
        if (displayName.equals(site.displayName) &&
            geoCity.equals(site.geoCity) &&
            geoState.equals(site.geoState) &&
            geoAddr.equals(site.geoAddr) &&
            geoCountry.equals(site.geoCountry) &&
            geoPostalCode.equals(site.geoPostalCode) &&
            tz.equals(site.tz) &&
            organization.equals(site.organization) &&
            installerEmail.equals(site.installerEmail) &&
            fcManagerEmail.equals(site.fcManagerEmail)) {
            return true;
        }
        return false;
    }
}
