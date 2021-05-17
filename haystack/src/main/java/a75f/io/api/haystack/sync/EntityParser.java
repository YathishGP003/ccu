package a75f.io.api.haystack.sync;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HRow;
import org.projecthaystack.UnknownRecException;
import org.projecthaystack.io.HGridFormat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Device;
import a75f.io.api.haystack.Entity;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Floor;
import a75f.io.api.haystack.Point;
import a75f.io.api.haystack.RawPoint;
import a75f.io.api.haystack.Schedule;
import a75f.io.api.haystack.SettingPoint;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.Zone;

/**
 * Created by samjithsadasivan on 12/21/18.
 */

/**
 * Parses a Grid or String to retrieve haystack data
 */

public class EntityParser
{
    private ArrayList<HashMap> mRows;
    private HGrid mGrid;
    private int index;
    
    private Site site;
    private ArrayList<Floor>        floors        = new ArrayList<>();
    private ArrayList<Zone>         zones         = new ArrayList<>();
    private ArrayList<Equip>        equips        = new ArrayList<>();
    private ArrayList<Device>       devices       = new ArrayList<>();
    private ArrayList<Point>        points        = new ArrayList<>();
    private ArrayList<RawPoint>     phyPoints     = new ArrayList<>();
    private ArrayList<SettingPoint> settingPoints = new ArrayList<>();
    private ArrayList<Schedule> schedules = new ArrayList<>();
    
    public EntityParser(HGrid grid) {
        mGrid = grid;
        parse();
    }
    
    public EntityParser(String data) {
        HGridFormat format = HGridFormat.find("text/plain", true);
        mGrid =  format.makeReader(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8))).readGrid();
        parse();
    }
    
    private void parse() {
        if (mGrid == null) {
            return;
        }
        mRows = new ArrayList<>();
        try {
            if (mGrid != null)
            {
                Iterator it = mGrid.iterator();
                while (it.hasNext())
                {
                    HashMap<Object, Object> map = new HashMap<>();
                    HRow r = (HRow) it.next();
                    HRow.RowIterator ri = (HRow.RowIterator) r.iterator();
                    while (ri.hasNext())
                    {
                        HDict.MapEntry m = (HDict.MapEntry) ri.next();
                        map.put(m.getKey(), m.getValue());
                    }
                    mRows.add(map);
    
                    if (map.get("site") != null) {
                        site = new Site.Builder().setHashMap(map).build();
                    } else if (map.get("floor") != null) {
                        floors.add(new Floor.Builder().setHashMap(map).build());
                    } else if (map.get("room") != null) {
                        zones.add(new Zone.Builder().setHashMap(map).build());
                    } else if (map.get("equip") != null) {
                        equips.add(new Equip.Builder().setHashMap(map).build());
                    } else if (map.get("device") != null) {
                        devices.add(new Device.Builder().setHashMap(map).build());
                    } else if (map.get("point") != null && map.get("physical") != null) {
                        phyPoints.add(new RawPoint.Builder().setHashMap(map).build());
                    } else if (map.get("point") != null && map.get("setting") != null) {
                        settingPoints.add(new SettingPoint.Builder().setHashMap(map).build());
                    } else if (map.get("point") != null ) {
                        points.add(new Point.Builder().setHashMap(map).build());
                    } else if (map.get("schedule") != null) {
                        schedules.add(new Schedule.Builder().setHDict(new HDictBuilder().add(r).toDict()).build());
                    }
                }
            }
        } catch (UnknownRecException e) {
            e.printStackTrace();
        }
    }
    
    public void reset() {
        index = 0;
    }
    public Entity getNext() {
    
        if (index >= mRows.size()) {
            return null;
        }
        HashMap m = mRows.get(index++);
        if (m.get("site") != null) {
           return new Site.Builder().setHashMap(m).build();
        } else if (m.get("floor") != null) {
            return new Floor.Builder().setHashMap(m).build();
        } else if (m.get("zone") != null) {
            return new Zone.Builder().setHashMap(m).build();
        } else if (m.get("equip") != null) {
            return new Equip.Builder().setHashMap(m).build();
        } else if (m.get("device") != null) {
            return new Device.Builder().setHashMap(m).build();
        } else if (m.get("point") != null && m.get("physical") != null) {
            return new RawPoint.Builder().setHashMap(m).build();
        } else if (m.get("point") != null && m.get("setting") != null) {
            return new SettingPoint.Builder().setHashMap(m).build();
        } else if (m.get("point") != null ) {
            return new Point.Builder().setHashMap(m).build();
        }
        
        return null;
    }
    
    public Site getSite() {
        return site;
    }
    public ArrayList<Floor> getFloors() {
        return floors;
    }
    public ArrayList<Zone> getZones() {
        return zones;
    }
    public ArrayList<Equip> getEquips() {
        return equips;
    }
    public ArrayList<Device> getDevices() {
        return devices;
    }
    public ArrayList<Point> getPoints() {
        return points;
    }
    public ArrayList<RawPoint> getPhyPoints() {
        return phyPoints;
    }
    public ArrayList<SettingPoint> getSettingPoints()
    {
        return settingPoints;
    }
    public ArrayList<Schedule> getSchedules() {
        return schedules;
    }
    
    public void importSchedules() {
        for(Schedule s : getSchedules()) {
            if (s.getMarkers().contains("building"))
            {
                String guid = s.getId();
                s.setmSiteId(CCUHsApi.getInstance().getSiteIdRef().toString());
                CCUHsApi.getInstance().addSchedule(guid, s.getScheduleHDict());
                CCUHsApi.getInstance().setSynced("@" + guid, "@" + guid);
            }
        }
    }
    
    public void importBuildingTuner() {
        
        CCUHsApi hsApi = CCUHsApi.getInstance();
        for (Equip q : getEquips()) {
            if (q.getMarkers().contains("tuner"))
            {
                q.setSiteRef(hsApi.getSiteIdRef().toString());
                q.setFloorRef("@SYSTEM");
                q.setRoomRef("@SYSTEM");
                String equipLuid = hsApi.addRemoteEquip(q, q.getId().replace("@",""));
                hsApi.setSynced(equipLuid, q.getId());
                //Points
                for (Point p : getPoints())
                {
                    if (p.getEquipRef().equals(q.getId()))
                    {
                        p.setSiteRef(hsApi.getSiteIdRef().toString());
                        p.setFloorRef("@SYSTEM");
                        p.setRoomRef("@SYSTEM");
                        p.setEquipRef(equipLuid);
                        hsApi.setSynced(hsApi.addRemotePoint(p, p.getId().replace("@", "")), p.getId());
                    }
                }
            }
        }
    
    }
    
    public void importSite() {
        CCUHsApi hsApi = CCUHsApi.getInstance();
        if (getSite() != null)
        {
            Site site = getSite();
            String siteLuid = hsApi.addRemoteSite(site, site.getId().replace("@", ""));
            hsApi.setSynced(siteLuid, site.getId());
        }
    }
    
    
}
