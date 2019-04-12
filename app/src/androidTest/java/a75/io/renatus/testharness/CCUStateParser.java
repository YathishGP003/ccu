package a75.io.renatus.testharness;

import android.content.Context;

import org.projecthaystack.HDict;
import org.projecthaystack.HDictBuilder;
import org.projecthaystack.HGrid;
import org.projecthaystack.HGridBuilder;
import org.projecthaystack.HNum;
import org.projecthaystack.HRef;
import org.projecthaystack.HRow;
import org.projecthaystack.HStr;
import org.projecthaystack.client.HClient;
import org.projecthaystack.io.HZincWriter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Site;
import a75f.io.api.haystack.sync.EntityPullHandler;
import a75f.io.api.haystack.sync.HttpUtil;
import a75f.io.device.json.serializers.JsonSerializer;
import a75f.io.logic.Globals;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.CCUApplication;
import a75f.io.logic.bo.building.Floor;
import a75f.io.logic.bo.building.Zone;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.vav.VavProfile;
import a75f.io.logic.tuners.BuildingTuners;

/**
 * Created by samjithsadasivan on 4/4/18.
 */

public class CCUStateParser
{
    public void parseAndInjectState(Context c, String stateJson){
        try
        {
            CCUApplication appStruct = (CCUApplication) JsonSerializer.fromJson(stateJson, CCUApplication.class);
            L.saveCCUState(appStruct);
            System.out.println(JsonSerializer.toJson(appStruct, true));
            addHayStackData(appStruct);
        }
        catch (IOException e)
        {
        
            e.printStackTrace();
        
        }
        
    }
    
    public void addHayStackData(CCUApplication app) {
        
        if (app.defaultSite == null) {
            return;
        }
        //HashMap site = CCUHsApi.getInstance().read("site");
        
        //new CCUHsApi();
        
        Site s75f = new Site.Builder()
                            .setDisplayName(app.defaultSite.displayName)
                            .addMarker("site")
                            .addMarker("orphan")
                            .setGeoCity(app.defaultSite.geoCity)
                            .setGeoState(app.defaultSite.geoState)
                            .setTz(app.defaultSite.tz)
                            .setGeoZip(app.defaultSite.geoZip)
                            .setArea((int)app.defaultSite.area).build();
        String siteRef = CCUHsApi.getInstance().addSite(s75f);
        
        BuildingTuners.getInstance();
        
        for (Floor f : app.getFloors())
        {
            a75f.io.api.haystack.Floor hsFloor = new a75f.io.api.haystack.Floor.Builder()
                                                         .setDisplayName(f.mFloorName).setSiteRef(siteRef).build();
            f.mFloorRef = CCUHsApi.getInstance().addFloor(hsFloor);
            
            for (Zone z :f.mZoneList) {
                a75f.io.api.haystack.Zone hsZone = new a75f.io.api.haystack.Zone.Builder()
                                                           .setDisplayName(z.roomName)
                                                           .setFloorRef(f.mFloorRef)
                                                           .build();
                z.mZoneRef = CCUHsApi.getInstance().addZone(hsZone);
                
                for (ZoneProfile p : z.mZoneProfiles) {
                    
                    for (Short node : p.getNodeAddresses()) {
                        switch(p.getProfileType()) {
                            case VAV_REHEAT:
                            case VAV_SERIES_FAN:
                            case VAV_PARALLEL_FAN:
                                ((VavProfile)p).addLogicalMapAndPoints(node, p.getProfileConfiguration(node), null, null);
                                //BuildingTuners.getInstance().addDefaultVavTuners();
                                break;
                            
                        }
                        
                    }
                }
            }
        }
    
        L.saveCCUState();
        CCUHsApi.getInstance().syncEntityTree();
        
    }
    
    public void pullHayStackDb(String siteId) {
        
        HClient hClient = new HClient(HttpUtil.HAYSTACK_URL, "ryan", "ryan");
        HDict siteDict = new HDictBuilder().add("id", HRef.make(siteId)).toDict();
        HGrid siteGrid = hClient.call("read", HGridBuilder.dictToGrid(siteDict));
    
        EntityPullHandler h = new EntityPullHandler();
        h.doPullSite(siteGrid);
        /*HGridFormat format = HGridFormat.find("text/plain", true);
        h.doPullFloorTree(CCUHsApi.getInstance().read("site").get("id").toString(),
                format.makeReader(new ByteArrayInputStream(siteData.getBytes(StandardCharsets.UTF_8))).readGrid());*/
        h.doPullFloorTree(CCUHsApi.getInstance().read("site").get("id").toString(),
                        CCUHsApi.getInstance().getRemoteSiteDetails(siteId));
    
        //System.out.println(CCUHsApi.getInstance().tagsDb.tagsMap);
    
        ArrayList<HashMap> writablePoints = CCUHsApi.getInstance().readAll("point and writable");
        for (HashMap m : writablePoints) {
            HDict pid = new HDictBuilder().add("id",HRef.copy(CCUHsApi.getInstance().getGUID(m.get("id").toString()))).toDict();
            System.out.println("Request: "+HZincWriter.gridToString(HGridBuilder.dictToGrid(pid)));
            HGrid wa = hClient.call("pointWrite",HGridBuilder.dictToGrid(pid));
            wa.dump();
        
            ArrayList<HashMap> valList = new ArrayList<>();
            Iterator it = wa.iterator();
            while (it.hasNext()) {
                HashMap<Object, Object> map = new HashMap<>();
                HRow r = (HRow) it.next();
                HRow.RowIterator ri = (HRow.RowIterator) r.iterator();
                while (ri.hasNext()) {
                    HDict.MapEntry e = (HDict.MapEntry) ri.next();
                    map.put(e.getKey(), e.getValue());
                }
                valList.add(map);
            }
        
            for(HashMap v : valList)
            {
                CCUHsApi.getInstance().getHSClient().pointWrite(HRef.copy(m.get("id").toString()),
                        Integer.parseInt(v.get("level").toString()), v.get("who").toString(),
                        m.get("kind").toString().equals("string") ? HStr.make(v.get("val").toString()) : HNum.make(Double.parseDouble(v.get("val").toString())),HNum.make(0));
            }
        
        }
        
        //BuildingTuners.getInstance();
        
        CCUHsApi.getInstance().saveTagsData();
        Globals.getInstance().loadEquipProfiles();
        
    }
    
    public CCUApplication parseState(String stateJson){
        try
        {
            CCUApplication appStruct = (CCUApplication) JsonSerializer.fromJson(stateJson, CCUApplication.class);
            return appStruct;
        }
        catch (IOException e)
        {
            
            e.printStackTrace();
            
        }
        return null;
    }

    public void setDeviceId(String id) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            System.out.println("Test : setprop ccuid_test " + id);
            os.writeBytes("setprop ccuid_test "+id);
        } catch (IOException e) {
            System.out.println("Failed to set device Id " + e.getMessage());
        }
    }
    
    
}
