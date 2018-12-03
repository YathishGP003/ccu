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
    public String            geoAddress;
    public String            geoZip;
    public String            tz;
    public double            area;
    
    public Site() {
    
    }
    public Site(String name,String city, String state, String zip, String tz, double area) {
        this.displayName = name;
        this.geoCity = city;
        this.geoState = state;
        this.geoZip = zip;
        this.tz = tz;
        this.area = area;
    }
    
    public Site (a75f.io.api.haystack.Site s) {
        this.displayName = s.getDisplayName();
        this.geoCity = s.getGeoCity();
        this.geoState = s.getGeoState();
        this.geoZip = s.getGeoZip();
        this.tz = s.getTz();
        this.area = s.getArea();
    }
}
