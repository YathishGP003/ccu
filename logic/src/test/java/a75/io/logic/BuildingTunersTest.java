package a75.io.logic;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Site;
import a75f.io.logic.tuners.SystemTunerUtil;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.VavTunerUtil;
import a75f.io.logic.tuners.BuildingTuners;

/**
 * Created by samjithsadasivan on 10/5/18.
 */

public class BuildingTunersTest
{
    @Test
    public void testBuildingTuners() {
        CCUHsApi api = new CCUHsApi();
        api.tagsDb.init();
        api.tagsDb.tagsMap = new HashMap<>();
        api.tagsDb.writeArrays = new HashMap<>();
    
        Site s75f = new Site.Builder()
                            .setDisplayName("75F")
                            .addMarker("site")
                            .setGeoCity("Burnsville")
                            .setGeoState("MN")
                            .setTz("Chicago")
                            .setArea(10000).build();
        api.addSite(s75f);
        
        
        BuildingTuners tuners = BuildingTuners.getInstance();
    
        ArrayList points = api.readAll("equip and tuner");
        for (Object a : points) {
            System.out.println(a);
        }
        Assert.assertTrue(points.size() == 1);
    
        tuners.addBuildingTunerEquip();
        ArrayList pnts = api.readAll("equip and tuner");
        Assert.assertTrue(pnts.size() == 1);
    }
    
    @Test
    public void testVavTuners() {
        CCUHsApi api = new CCUHsApi();
        api.tagsDb.init();
        api.tagsDb.tagsMap = new HashMap<>();
        api.tagsDb.writeArrays = new HashMap<>();
    
        Site s75f = new Site.Builder()
                            .setDisplayName("75F")
                            .addMarker("site")
                            .setGeoCity("Burnsville")
                            .setGeoState("MN")
                            .setTz("Chicago")
                            .setArea(10000).build();
        api.addSite(s75f);
    
    
        BuildingTuners tuners = BuildingTuners.getInstance();
        tuners.addBuildingTunerEquip();
        
        tuners.addDefaultVavTuners();
        VavTunerUtil.dump();
        
        VavTunerUtil.setCoolingDeadband(2.0, TunerConstants.VAV_BUILDING_VAL_LEVEL);
        VavTunerUtil.setHeatingDeadband(2.0, TunerConstants.VAV_BUILDING_VAL_LEVEL);
        VavTunerUtil.setProportionalGain(1, TunerConstants.VAV_BUILDING_VAL_LEVEL);
        VavTunerUtil.setIntegralGain(1, TunerConstants.VAV_BUILDING_VAL_LEVEL);
        VavTunerUtil.setProportionalSpread(10, TunerConstants.VAV_BUILDING_VAL_LEVEL);
        VavTunerUtil.setIntegralTimeout(60, TunerConstants.VAV_BUILDING_VAL_LEVEL);
        
        VavTunerUtil.dump();
        
        
    }
    
    @Test
    public void testSystemTuners() {
        CCUHsApi api = new CCUHsApi();
        api.tagsDb.init();
        api.tagsDb.tagsMap = new HashMap<>();
        api.tagsDb.writeArrays = new HashMap<>();
        api.tagsDb.removeIdMap = new HashMap<>();
        api.tagsDb.idMap = new HashMap<>();
    
        Site s75f = new Site.Builder()
                            .setDisplayName("75F")
                            .addMarker("site")
                            .setGeoCity("Burnsville")
                            .setGeoState("MN")
                            .setTz("Chicago")
                            .setArea(10000).build();
        api.addSite(s75f);
    
    
        BuildingTuners.getInstance();
        
        SystemTunerUtil.setTuner("analog1","max", 14, 10);
        System.out.println(SystemTunerUtil.getTuner("analog1","max"));
    
        SystemTunerUtil.setTuner("analog1","max", 15, 8);
        System.out.println(SystemTunerUtil.getTuner("analog1","max"));
        
        double val = SystemTunerUtil.getTuner("heatingSat", "max", TunerConstants.SYSTEM_BUILDING_VAL_LEVEL);
        System.out.println(val != 0);
    }
    
    
}
