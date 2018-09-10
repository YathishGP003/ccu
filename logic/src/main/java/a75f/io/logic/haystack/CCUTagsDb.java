package a75f.io.logic.haystack;

import org.projecthaystack.HBool;
import org.projecthaystack.HDateTime;
import org.projecthaystack.HDateTimeRange;
import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HHisItem;
import org.projecthaystack.HMarker;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HStr;
import org.projecthaystack.HUri;
import org.projecthaystack.HVal;
import org.projecthaystack.HWatch;
import org.projecthaystack.server.HOp;
import org.projecthaystack.server.HServer;
import org.projecthaystack.server.HStdOps;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.UUID;

import a75f.io.logic.bo.haystack.Device;
import a75f.io.logic.bo.haystack.Equip;
import a75f.io.logic.bo.haystack.Point;
import a75f.io.logic.bo.haystack.RawPoint;
import a75f.io.logic.bo.haystack.Site;

/**
 * Created by samjithsadasivan on 8/31/18.
 */

public class CCUTagsDb extends HServer
{
    
    HashMap recs = new HashMap();//L.ccu().tagsMap;
    
    //TODO- TEMP for Unit testing
    
    public HashMap getDbMap() {
        return recs;
    }
    
    
    public HDict addSite(String dis, String geoCity, String geoState, String timeZone, int area)
    {
        HDict site = new HDictBuilder()
                             .add("id",       HRef.make(dis))
                             .add("dis",      dis)
                             .add("site",     HMarker.VAL)
                             .add("geoCity",  geoCity)
                             .add("geoState", geoState)
                             .add("geoAddr",  "" +geoCity + "," + geoState)
                             .add("tz",       timeZone)
                             .add("area",     HNum.make(area, "ft\u00B2"))
                             .toDict();
        recs.put(dis, site);
        return site;
        //addMeter(site, dis+"-Meter");
        //addAhu(site,   dis+"-AHU1");
        //addAhu(site,   dis+"-AHU2");
    }
    
    public HDict getSite(String ref) {
        return (HDict) recs.get(ref);
    }
    
    public void addMeter(HDict site, String dis)
    {
        HDict equip = new HDictBuilder()
                              .add("id",       HRef.make(dis))
                              .add("dis",      dis)
                              .add("equip",     HMarker.VAL)
                              .add("elecMeter", HMarker.VAL)
                              .add("siteMeter", HMarker.VAL)
                              .add("siteRef",   site.get("id"))
                              .toDict();
        recs.put(dis, equip);
        //addPoint(equip, dis+"-KW",  "kW",  "elecKw");
        //addPoint(equip, dis+"-KWH", "kWh", "elecKwh");
    }
    
    public HDict addAhuEquip(HDict site, String dis)
    {
        HDict equip = new HDictBuilder()
                              .add("id",      HRef.make(dis))
                              .add("dis",     dis)
                              .add("equip",   HMarker.VAL)
                              .add("ahu",     HMarker.VAL)
                              .add("siteRef", site.get("id"))
                              .toDict();
        recs.put(dis, equip);
        //addPoint(equip, dis+"-Fan",    null,      "discharge air fan cmd");
        //addPoint(equip, dis+"-Cool",   null,      "cool cmd");
        //addPoint(equip, dis+"-Heat",   null,      "heat cmd");
        //addPoint(equip, dis+"-DTemp",  "\u00B0F", "discharge air temp sensor");
        //addPoint(equip, dis+"-RTemp",  "\u00B0F", "return air temp sensor");
        //addPoint(equip, dis+"-ZoneSP", "\u00B0F", "zone air temp sp writable");
        return equip;
    }
    
    public HDict addVavEquip(HDict site, String dis)
    {
        HDict equip = new HDictBuilder()
                              .add("id",      HRef.make(dis))
                              .add("dis",     dis)
                              .add("equip",   HMarker.VAL)
                              .add("vav",     HMarker.VAL)
                              .add("siteRef", site.get("id"))
                              .toDict();
        recs.put(dis, equip);
        return equip;
    }
    
    public void addEquip(HDict site, String dis, String markers)
    {
        HDictBuilder equip = new HDictBuilder()
                              .add("id",      HRef.make(dis))
                              .add("dis",     dis)
                              .add("siteRef", site.get("id"));
        StringTokenizer st = new StringTokenizer(markers);
        while (st.hasMoreTokens()) equip.add(st.nextToken());
        recs.put(dis, equip.toDict());
    }
    
    public void addSite(Site s)
    {
        HDictBuilder site = new HDictBuilder()
                             .add("id", HRef.make(UUID.randomUUID().toString()))
                             .add("dis", s.getDisplayName())
                             .add("site", HMarker.VAL)
                             .add("geoCity", s.getGeoCity())
                             .add("geoState", s.getGeoState())
                             .add("geoAddr", "" + s.getGeoCity() + "," + s.getGeoState())
                             .add("tz", s.getTz())
                             .add("area", HNum.make(s.getArea(), "ft\u00B2"));
                
    
        for (String m : s.getMarkers()) {
            site.add(m);
        }
        
        //recs.put(s.getDisplayName(), site.toDict());
        HRef id = (HRef)site.get("id");
        recs.put(id.toVal(), site.toDict());
    }
    
    public String addEquip(Equip q)
    {
        HDict site = getSite(q.getSiteRef());
        HDictBuilder equip = new HDictBuilder()
                                     .add("id",      HRef.make(UUID.randomUUID().toString()))
                                     .add("dis",     q.getDisplayName())
                                     .add("siteRef", q.getSiteRef()/*site.get("id")*/);
        for (String m : q.getMarkers()) {
            equip.add(m);
        }
        HRef id = (HRef)equip.get("id");
        //recs.put(q.getDisplayName(), equip.toDict());
        recs.put(id.toVal(), equip.toDict());
        return id.toCode();
    }
    
    
    public HDict getEquip(String dis) {
        return (HDict) recs.get(dis);
    }
    
    public String addPoint(Point p)
    {
        HDict eq = getEquip(p.getEquipRef());
        HDictBuilder b = new HDictBuilder()
                                 .add("id",       HRef.make(UUID.randomUUID().toString()))
                                 .add("dis",      p.getDisplayName())
                                 .add("point",    HMarker.VAL)
                                 .add("his",      HMarker.VAL)
                                 .add("siteRef",  p.getSiteRef()/*eq.get("siteRef")*/)
                                 .add("equipRef", p.getEquipRef()/*eq.get("id")*/)
                                 .add("roomRef",  p.getRoomRef())
                                 .add("floorRef", p.getFloorRef())
                                 .add("kind",     p.getUnit() == null ? "Bool" : "Number")
                                 .add("tz",       p.getTz());
        if (p.getUnit() != null) b.add("unit", p.getUnit());
        
        for (String m : p.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef)b.get("id");
        //recs.put(p.getDisplayName(), b.toDict());
        recs.put(id.toVal(), b.toDict());
        return id.toCode();
    }
    
    public String addPoint(RawPoint p)
    {
        HDictBuilder b = new HDictBuilder()
                                 .add("id",       HRef.make(UUID.randomUUID().toString()))
                                 .add("dis",      p.getDisplayName())
                                 .add("point",    HMarker.VAL)
                                 .add("writable",    HMarker.VAL)
                                 .add("his",      HMarker.VAL)
                                 .add("deviceRef", p.getDeviceRef())
                                 .add("pointRef",p.getPointRef())
                                 .add("port",p.getPort())
                                 .add("type",p.getType());
        
        for (String m : p.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef)b.get("id");
        //recs.put(p.getDisplayName(), b.toDict());
        recs.put(id.toVal(), b.toDict());
        return id.toCode();
    }
    
    public String addDevice(Device d)
    {
        HDictBuilder b = new HDictBuilder()
                                 .add("id",       HRef.make(UUID.randomUUID().toString()))
                                 .add("dis",      d.getDisplayName())
                                 .add("device",    HMarker.VAL)
                                 .add("his",      HMarker.VAL)
                                 .add("deviceAddr",      d.getAddr());
    
        for (String m : d.getMarkers()) {
            b.add(m);
        }
        HRef id = (HRef)b.get("id");
        //recs.put(d.getDisplayName(), b.toDict());
        recs.put(id.toVal(), b.toDict());
        return id.toCode();
    }
    
    
    
    //////////////////////////////////////////////////////////////////////////
    // Ops
    //////////////////////////////////////////////////////////////////////////
    
    public HOp[] ops()
    {
        return new HOp[] {
                HStdOps.about,
                HStdOps.ops,
                HStdOps.formats,
                HStdOps.read,
                HStdOps.nav,
                HStdOps.pointWrite,
                HStdOps.hisRead,
                HStdOps.invokeAction,
        };
    }
    
    public HDict onAbout() { return about; }
    private final HDict about = new HDictBuilder()
                                        .add("serverName",  hostName())
                                        .add("vendorName", "Haystack Java Toolkit")
                                        .add("vendorUri", HUri.make("http://project-haystack.org/"))
                                        .add("productName", "Haystack Java Toolkit")
                                        .add("productVersion", "2.0.0")
                                        .add("productUri", HUri.make("http://project-haystack.org/"))
                                        .toDict();
    
    private static String hostName()
    {
        try { return InetAddress.getLocalHost().getHostName(); }
        catch (Exception e) { return "Unknown"; }
    }
    
    //////////////////////////////////////////////////////////////////////////
    // Reads
    //////////////////////////////////////////////////////////////////////////
    
    protected HDict onReadById(HRef id) {
        return (HDict)recs.get(id.val);
    }
    
    protected Iterator iterator() { return recs.values().iterator(); }
    
    //////////////////////////////////////////////////////////////////////////
    // Navigation
    //////////////////////////////////////////////////////////////////////////
    
    protected HGrid onNav(String navId)
    {
        // test database navId is record id
        HDict base = null;
        if (navId != null) base = readById(HRef.make(navId));
        
        // map base record to site, equip, or point
        String filter = "site";
        if (base != null)
        {
            if (base.has("site")) filter = "equip and siteRef==" + base.id().toCode();
            else if (base.has("equip")) filter = "point and equipRef==" + base.id().toCode();
            else filter = "navNoChildren";
        }
        
        // read children of base record
        HGrid grid = readAll(filter);
        
        // add navId column to results
        HDict[] rows = new HDict[grid.numRows()];
        Iterator it = grid.iterator();
        for (int i=0; it.hasNext(); ) rows[i++] = (HDict)it.next();
        for (int i=0; i<rows.length; ++i)
            rows[i] = new HDictBuilder().add(rows[i]).add("navId", rows[i].id().val).toDict();
        return HGridBuilder.dictsToGrid(rows);
    }
    
    protected HDict onNavReadByUri(HUri uri)
    {
        return null;
    }
    
    //////////////////////////////////////////////////////////////////////////
    // Watches
    //////////////////////////////////////////////////////////////////////////
    
    protected HWatch onWatchOpen(String dis, HNum lease)
    {
        throw new UnsupportedOperationException();
    }
    
    protected HWatch[] onWatches()
    {
        throw new UnsupportedOperationException();
    }
    
    protected HWatch onWatch(String id)
    {
        throw new UnsupportedOperationException();
    }
    
    //////////////////////////////////////////////////////////////////////////
    // Point Write
    //////////////////////////////////////////////////////////////////////////
    
    protected HGrid onPointWriteArray(HDict rec)
    {
        CCUTagsDb.WriteArray array = (CCUTagsDb.WriteArray)writeArrays.get(rec.id());
        if (array == null) array = new CCUTagsDb.WriteArray();
        
        HGridBuilder b = new HGridBuilder();
        b.addCol("level");
        b.addCol("levelDis");
        b.addCol("val");
        b.addCol("who");
        
        for (int i=0; i<17; ++i)
            b.addRow(new HVal[] {
                    HNum.make(i+1),
                    HStr.make("" + (i + 1)),
                    array.val[i],
                    HStr.make(array.who[i]),
            });
        return b.toGrid();
    }
    
    protected void onPointWrite(HDict rec, int level, HVal val, String who, HNum dur, HDict opts)
    {
        System.out.println("onPointWrite: " + rec.dis() + "  " + val + " @ " + level + " [" + who + "]");
        CCUTagsDb.WriteArray array = (CCUTagsDb.WriteArray)writeArrays.get(rec.id());
        if (array == null) writeArrays.put(rec.id(), array = new CCUTagsDb.WriteArray());
        array.val[level-1] = val;
        array.who[level-1] = who;
    }
    
    static class WriteArray
    {
        final HVal[] val = new HVal[17];
        final String[] who = new String[17];
    }
    
    // hacky, but keep it simple for servlet environment
    static HashMap writeArrays = new HashMap();
    
    //////////////////////////////////////////////////////////////////////////
    // History
    //////////////////////////////////////////////////////////////////////////
    
    public HHisItem[] onHisRead(HDict entity, HDateTimeRange range)
    {
        // generate dummy 15min data
        ArrayList acc = new ArrayList();
        HDateTime ts = range.start;
        boolean isBool = ((HStr)entity.get("kind")).val.equals("Bool");
        while (ts.compareTo(range.end) <= 0)
        {
            HVal val = isBool ?
                               (HVal) HBool.make(acc.size() % 2 == 0) :
                                                                              (HVal)HNum.make(acc.size());
            HDict item = HHisItem.make(ts, val);
            if (ts != range.start) acc.add(item);
            ts = HDateTime.make(ts.millis() + 15*60*1000);
        }
        return (HHisItem[])acc.toArray(new HHisItem[acc.size()]);
    }
    
    public void onHisWrite(HDict rec, HHisItem[] items)
    {
        throw new RuntimeException("Unsupported");
    }
    
    //////////////////////////////////////////////////////////////////////////
    // Actions
    //////////////////////////////////////////////////////////////////////////
    
    public HGrid onInvokeAction(HDict rec, String action, HDict args)
    {
        System.out.println("-- invokeAction \"" + rec.dis() + "." + action + "\" " + args);
        return HGrid.EMPTY;
    }
}
