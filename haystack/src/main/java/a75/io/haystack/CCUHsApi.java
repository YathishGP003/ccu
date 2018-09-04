package a75.io.haystack;

import org.projecthaystack.HDict;

import a75.io.bo.haystack.Equip;
import a75.io.bo.haystack.Point;
import a75.io.bo.haystack.Site;

/**
 * Created by samjithsadasivan on 9/3/18.
 */

public class CCUHsApi
{
    private static CCUHsApi instance = new CCUHsApi();
    
    public AndroidHSClient hsClient;
    public CCUTagsDb tagsDb;
    
    public static CCUHsApi getInstance(){
        return instance;
    }
    private CCUHsApi() {
        hsClient = new AndroidHSClient();
        tagsDb = (CCUTagsDb) hsClient.db();
    }
    
    
    
    public void addSite(Site s) {
        addSite(s.getDisplayName(), s.getGeoCity(), s.getGeoState(), s.getTz(), s.getArea());
    }
    
    private void addSite(String site, String city, String state, String tz, int area) {
        tagsDb.addSite(site, city, state, tz, area);
    }
    
    public void addEquip(Equip q){
        StringBuilder marker = new StringBuilder();
        for (String m : q.getMarkers()) {
            marker.append(m+" ");
        }
        addEquip(q.getSiteRef(), q.getDisplayName(), marker.toString().trim());
    }
    
    private void addEquip(String siteName, String dis, String markers) {
        HDict site = tagsDb.getSite(siteName);
        tagsDb.addEquip(site, dis, markers);
    }
    
    public void addPoint(Point p) {
        StringBuilder marker = new StringBuilder();
        for (String m : p.getMarkers()) {
            marker.append(m+" ");
        }
        addPoint(p.getEquipRef(), p.getDisplayName(), p.getUnit(), marker.toString().trim(), p.getTz());
    }
    
    private void addPoint(String equipName, String dis, String unit, String markers, String tz) {
        HDict eq = tagsDb.getEquip(equipName);
        tagsDb.addPoint(eq, equipName+"-"+dis, unit, markers, tz);
    }
    
    public void createVavReheatEquipment(String siteName, String equip) {
        
        HDict site = tagsDb.getSite(siteName);
        HDict eq = tagsDb.addVavEquip(site, equip);
    
        tagsDb.addPoint(eq, equip+"-DAT",  "\u00B0F", "discharge air temp sensor","Chicago");
        tagsDb.addPoint(eq, equip+"-SAT",  "\u00B0F", "entering air temp sensor","Chicago");
        tagsDb.addPoint(eq, equip+"-Humidity",  "\u00B0F", "zone air humidity sensor","Chicago");
        tagsDb.addPoint(eq, equip+"-HumiditySP",  "\u00B0F", "zone air humidity sp","Chicago");
        tagsDb.addPoint(eq, equip+"-CO2",  "\u00B0F", "zone air co2 sensor","Chicago");
        tagsDb.addPoint(eq, equip+"-CO2SP",  "\u00B0F", "zone air co2 sp","Chicago");
        tagsDb.addPoint(eq, equip+"-MaxCoolingDamperPos",  "\u00B0F", "cooling max damper sp","Chicago");
        tagsDb.addPoint(eq, equip+"-MinCoolingDamperPos",  "\u00B0F", "cooling min damper sp","Chicago");
        tagsDb.addPoint(eq, equip+"-MaxHeatingDamperPos",  "\u00B0F", "heating max damper sp","Chicago");
        tagsDb.addPoint(eq, equip+"-MinHeatingDamperPos",  "\u00B0F", "heating min damper sp","Chicago");
    
        tagsDb.addPoint(eq, equip+"-damperPos",  "\u00B0F", "air damper cmd","Chicago");
        tagsDb.addPoint(eq, equip+"-reheatPos",  "\u00B0F", "reheat cmd","Chicago");
        
    }
    
    
}
