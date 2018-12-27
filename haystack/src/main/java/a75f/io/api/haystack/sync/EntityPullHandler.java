package a75f.io.api.haystack.sync;

import org.projecthaystack.HGrid;
import org.projecthaystack.HRef;
import org.projecthaystack.io.HGridFormat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.HayStackConstants;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.SettingPoint;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Zone;

/**
 * Created by samjithsadasivan on 12/21/18.
 */

public class EntityPullHandler
{
    public void doPullSite(String site) {
        HGridFormat format = HGridFormat.find("text/plain", true);
        doPullSiteTree(format.makeReader(new ByteArrayInputStream(site.getBytes(StandardCharsets.UTF_8))).readGrid());
    }
    
    public void doPullSiteTree(HGrid grid) {
        EntityParser parser = new EntityParser(grid);
        CCUHsApi hsApi = CCUHsApi.getInstance();
        if (parser.getSite() != null) {
            Site site = parser.getSite();
            String siteLuid = hsApi.addSite(site);
            hsApi.putUIDMap(siteLuid, site.getId());
            
            doPullFloorTree(siteLuid, grid);
        }
        
    }
    
    public void doPullSite(HGrid grid) {
        EntityParser parser = new EntityParser(grid);
        CCUHsApi hsApi = CCUHsApi.getInstance();
        if (parser.getSite() != null)
        {
            Site site = parser.getSite();
            String siteLuid = hsApi.addSite(site);
            hsApi.putUIDMap(siteLuid, site.getId());
        }
    }
    
    public void doPullFloorTree(String siteLuid, HGrid grid) {
        EntityParser parser = new EntityParser(grid);
        CCUHsApi hsApi = CCUHsApi.getInstance();
        //Floors
        for (Floor f : parser.getFloors()) {
            f.setSiteRef(siteLuid);
            String floorLuid = hsApi.addFloor(f);
            hsApi.putUIDMap(floorLuid, f.getId());
            //Zones
            for (Zone z : parser.getZones()) {
                if (z.getFloorRef().equals(f.getId())) {
                    z.setSiteRef(siteLuid);
                    z.setFloorRef(floorLuid);
                    String zoneLuid = hsApi.addZone(z);
                    hsApi.putUIDMap(zoneLuid, z.getId());
                    //Equips
                    for (Equip q : parser.getEquips()) {
                        if (q.getZoneRef().equals(z.getId())) {
                            q.setSiteRef(siteLuid);
                            q.setFloorRef(floorLuid);
                            q.setZoneRef(zoneLuid);
                            String equipLuid = hsApi.addEquip(q);
                            hsApi.putUIDMap(equipLuid, q.getId());
                            //Points
                            for (Point p : parser.getPoints()) {
                                if (p.getEquipRef().equals(q.getId()))
                                {
                                    p.setSiteRef(siteLuid);
                                    p.setFloorRef(floorLuid);
                                    p.setZoneRef(zoneLuid);
                                    p.setEquipRef(equipLuid);
                                    hsApi.putUIDMap(hsApi.addPoint(p), p.getId());
                                }
                            }
    
                            //Node devices
                            for (Device d : parser.getDevices()) {
                                if (d.getEquipRef() != null && d.getEquipRef().equals(q.getId())
                                        && d.getZoneRef() != null && d.getZoneRef().equals(z.getId())) {
                                    d.setSiteRef(siteLuid);
                                    d.setFloorRef(floorLuid);
                                    d.setZoneRef(zoneLuid);
                                    d.setEquipRef(equipLuid);
                                    String deviceLuid = hsApi.addDevice(d);
                                    hsApi.putUIDMap(deviceLuid, d.getId());
                                    //Physical Points
                                    for (RawPoint p : parser.getPhyPoints()) {
                                        if (p.getDeviceRef().equals(d.getId()))
                                        {
                                            p.setSiteRef(siteLuid);
                                            p.setFloorRef(floorLuid);
                                            p.setZoneRef(zoneLuid);
                                            p.setDeviceRef(deviceLuid);
                                            p.setPointRef(getLuid(p.getDeviceRef()));
                                            hsApi.putUIDMap(hsApi.addPoint(p), p.getId());
                                        }
                                    }
                                }
                            }
                        } else if (q.getZoneRef().equals("@SYSTEM")) {
                            q.setSiteRef(siteLuid);
                            q.setFloorRef("@SYSTEM");
                            q.setZoneRef("@SYSTEM");
                            String equipLuid = hsApi.addEquip(q);
                            hsApi.putUIDMap(equipLuid, q.getId());
                            //Points
                            for (Point p : parser.getPoints()) {
                                if (p.getEquipRef().equals(q.getId()))
                                {
                                    p.setSiteRef(siteLuid);
                                    p.setFloorRef("@SYSTEM");
                                    p.setZoneRef("@SYSTEM");
                                    p.setEquipRef(equipLuid);
                                    hsApi.putUIDMap(hsApi.addPoint(p), p.getId());
                                }
                            }
                        }
                    }
                
                }
            }
        }
    
        //CM Devices
        for (Device d : parser.getDevices())
        {
            if (d.getMarkers().contains("cm"))
            {
                d.setSiteRef(siteLuid);
                String deviceLuid = hsApi.addDevice(d);
                hsApi.putUIDMap(deviceLuid, d.getId());
                for (SettingPoint p : parser.getSettingPoints())
                {
                    if (p.getDeviceRef().equals(d.getId()))
                    {
                        p.setSiteRef(siteLuid);
                        p.setDeviceRef(deviceLuid);
                        hsApi.putUIDMap(hsApi.addPoint(p), p.getId());
                    }
                }
            }
        }
    
        //CCU Devices
        for (Device d : parser.getDevices())
        {
            if (d.getMarkers().contains("ccu"))
            {
                d.setSiteRef(siteLuid);
                String deviceLuid = hsApi.addDevice(d);
                hsApi.putUIDMap(deviceLuid, d.getId());
                hsApi.addOrUpdateConfigProperty(HayStackConstants.CUR_CCU, HRef.copy(deviceLuid));
            }
        }
    }
    
    public String getLuid(String guid) {
        Iterator it = CCUHsApi.getInstance().tagsDb.idMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            if (entry.getValue().equals(guid))
            {
                return entry.getKey().toString();
            }
        }
        return null;
    }
}
