package a75f.io.logic.bo.building;

import java.util.ArrayList;

/**
 * Created by samjithsadasivan on 11/27/18.
 */

public class Site
{
    public String            displayName;
    public ArrayList<String> markers;
    public String            geoCity;
    public String            geoState;
    public String            geoAddr;
    public String            geoCountry;
    public String            geoPostalCode;
    public String            geoZip;
    public String            tz;
    public double            area;
    
    public Site() {
    
    }
    public Site(String name,String city, String state, String country,String zip, String tz, double area) {
        this.displayName = name;
        this.geoCity = city;
        this.geoState = state;
        this.geoCountry = country;
        this.geoPostalCode = zip;
        this.tz = tz;
        this.area = area;
    }
    
    public Site (a75f.io.api.haystack.Site s) {
        this.displayName = s.getDisplayName();
        this.geoCity = s.getGeoCity();
        this.geoState = s.getGeoState();
        this.geoCountry = s.getGeoCountry();
        this.geoPostalCode = s.getGeoPostalCode();
        this.geoAddr = s.getGeoAddress();
        this.tz = s.getTz();
        this.area = s.getArea();
    }
}
