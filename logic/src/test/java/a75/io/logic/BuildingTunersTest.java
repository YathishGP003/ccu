package a75.io.logic;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.Site;
import a75f.io.logic.bo.building.vav.VavProfileConfiguration;
import a75f.io.logic.tuners.BuildingTuners;
import a75f.io.logic.tuners.TunerConstants;
import a75f.io.logic.tuners.VavTunerUtil;

/**
 * Created by samjithsadasivan on 10/5/18.
 */

public class BuildingTunersTest
{
    @Test
    public void testBuildingTuners() {
        CCUHsApi api = new CCUHsApi();
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
        api.testHarnessEnabled = true;
        Site s75f = new Site.Builder()
                            .setDisplayName("75F")
                            .addMarker("site")
                            .setGeoCity("Burnsville")
                            .setGeoState("MN")
                            .setTz("Chicago")
                            .setArea(10000).build();
        String siteRef = api.addSite(s75f);
    
    
        BuildingTuners tuners = BuildingTuners.getInstance();
        tuners.addBuildingTunerEquip();
        
        tuners.addDefaultVavTuners();
        HashMap tuner = CCUHsApi.getInstance().read("equip and tuner");
        String tunerEquipRef = tuner.get("id").toString();
        VavTunerUtil.dump(tunerEquipRef);
        
        VavTunerUtil.setCoolingDeadband(tunerEquipRef,2.0, TunerConstants.VAV_BUILDING_VAL_LEVEL);
        VavTunerUtil.setHeatingDeadband(tunerEquipRef,2.0, TunerConstants.VAV_BUILDING_VAL_LEVEL);
        VavTunerUtil.setProportionalGain(tunerEquipRef, 1, TunerConstants.VAV_BUILDING_VAL_LEVEL);
        VavTunerUtil.setIntegralGain(tunerEquipRef, 1, TunerConstants.VAV_BUILDING_VAL_LEVEL);
        VavTunerUtil.setProportionalSpread(tunerEquipRef, 10, TunerConstants.VAV_BUILDING_VAL_LEVEL);
        VavTunerUtil.setIntegralTimeout(tunerEquipRef, 60, TunerConstants.VAV_BUILDING_VAL_LEVEL);
        
        //VavTunerUtil.dump(tunerEquipRef);
        VavProfileConfiguration v = new VavProfileConfiguration();
        v.minDamperCooling = 0;
        v.maxDamperCooling = 100;
        v.minDamperHeating = 0;
        v.maxDamperHeating = 0;
    
        Equip vavEquip = new Equip.Builder()
                          .setSiteRef(siteRef)
                          .setDisplayName("TestVavEquip")
                          .setRoomRef(null)
                          .setFloorRef(null)
                          .setProfile("VAV_REHEAT")
                          .setPriority("LOW")
                          .addMarker("equip").addMarker("vav").addMarker("zone").addMarker("equipHis")
                          .setTz("Chicago")
                          .setGroup("400")
                          .build();
        String equipRef = CCUHsApi.getInstance().addEquip(vavEquip);
        
        System.out.println("Add VAV Equip #######");
        tuners.addEquipVavTuners("TestVavEquip",equipRef, v);
        VavTunerUtil.dump(equipRef);
        
        
        
    }
    
    
    
}
