package a75f.io.api.haystack.mock;

import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HVal;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.SettingPoint;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Zone;

/**
 * Mock API class bypasses sync and local data persisting.
 * It also configures Objectbox for unit testing.
 */
public class MockCcuHsApi extends CCUHsApi {
    
    public MockCcuHsApi() {
        super();
    }
    
    @Override
    public String addSite(Site s) {
        return tagsDb.addSite(s);
    }
    
    @Override
    public String addEquip(Equip q) {
        return tagsDb.addEquip(q);
    }
    
    @Override
    public String addPoint(Point p) {
        return tagsDb.addPoint(p);
    }
    
    @Override
    public String addPoint(RawPoint p) {
        return tagsDb.addPoint(p);
    }
    
    @Override
    public String addRemotePoint(RawPoint p, String id) {
        return tagsDb.addPointWithId(p, id);
    }
    
    @Override
    public String addPoint(SettingPoint p) {
        return tagsDb.addPoint(p);
    }
    
    @Override
    public String addPointWithId(SettingPoint p, String id) {
        return tagsDb.addPointWithId(p, id);
    }
    
    @Override
    public String updateSettingPoint(SettingPoint p, String id) {
        return tagsDb.updateSettingPoint(p,id);
    }
    
    @Override
    public String addDevice(Device d) {
        return tagsDb.addDevice(d);
    }
    
    @Override
    public void updateDevice(Device d, String id) {
        tagsDb.updateDevice(d, id);
    }
    
    @Override
    public String addFloor(Floor f) {
        return tagsDb.addFloor(f);
    }
    
    @Override
    public String addZone(Zone z) {
        return tagsDb.addZone(z);
    }
    
    @Override
    public String addRemoteZone(Zone z, String id) {
        return tagsDb.addZoneWithId(z, id);
    }
    
    @Override
    public void updateSite(Site s, String id) {
        tagsDb.updateSite(s, id);
    }
    
    @Override
    public void updateEquip(Equip q, String id) {
        tagsDb.updateEquip(q, id);
    }
    
    @Override
    public void updatePoint(RawPoint r, String id) {
        tagsDb.updatePoint(r, id);
    }
    
    @Override
    public void updatePoint(Point point, String id) {
        tagsDb.updatePoint(point, id);
    }
    
    @Override
    public void updateFloor(Floor r, String id) {
        tagsDb.updateFloor(r, id);
    }
    
    @Override
    public void updateZone(Zone z, String id) {
        tagsDb.updateZone(z, id);
    }
    
    @Override
    public void pointWrite(HRef id, int level, String who, HVal val, HNum dur, String reason) {
        hsClient.pointWrite(id, level, who, val, dur);
    }
}
