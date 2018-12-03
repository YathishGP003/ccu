package a75.io.renatus.testharness;

import android.content.Context;

import java.io.DataOutputStream;
import java.io.IOException;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Site;
import a75f.io.device.json.serializers.JsonSerializer;
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
                                ((VavProfile)p).addLogicalMapAndPoints(node, p.getProfileConfiguration(node));
                                BuildingTuners.getInstance().addDefaultVavTuners();
                                break;
                            
                        }
                        
                    }
                }
            }
        }
    
        L.saveCCUState();
        CCUHsApi.getInstance().syncEntityTree();
        
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
