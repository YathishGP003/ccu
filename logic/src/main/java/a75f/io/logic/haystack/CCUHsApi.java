package a75f.io.logic.haystack;

import a75f.io.logic.bo.haystack.Device;
import a75f.io.logic.bo.haystack.Equip;
import a75f.io.logic.bo.haystack.Point;
import a75f.io.logic.bo.haystack.RawPoint;
import a75f.io.logic.bo.haystack.Site;

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
        tagsDb.addSite(s);
    }
    
    public void addEquip(Equip q){
        tagsDb.addEquip(q);
    }
    
    public String addPoint(Point p) {
        return tagsDb.addPoint(p);
    }
    
    public String addPoint(RawPoint p) {
        return tagsDb.addPoint(p);
    }
    
    public String addDevice(Device d) {
        return tagsDb.addDevice(d);
    }
}
