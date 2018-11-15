package a75.io.logic;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.vav.VAVLogicalMap;
import a75f.io.logic.bo.building.vav.VavProfileConfiguration;

/**
 * Created by samjithsadasivan on 9/12/18.
 */

public class VavHaystackTest
{
    
    @Test
    public void testVavReheatLogicalMap() {
        CCUHsApi api = new CCUHsApi();
        api.tagsDb.init();
        api.tagsDb.tagsMap = new HashMap<>();
        api.tagsDb.writeArrays = new HashMap<>();
        api.tagsDb.idMap = new HashMap<>();
        VAVLogicalMap m = new VAVLogicalMap(ProfileType.VAV_REHEAT, 7000);
        m.createHaystackPoints(new VavProfileConfiguration());
        ArrayList points = CCUHsApi.getInstance().readAll("point and group == \"7000\"");
        /*for (Object a : points) {
            System.out.println(a);
        }*/
    
        System.out.println("getRoomTemp "+m.getCurrentTemp());
        System.out.println("getDamperPos "+m.getDamperPos());
        System.out.println("getReheatPos "+m.getReheatPos());
        System.out.println("getDesiredTemp "+m.getDesiredTemp());
        System.out.println("getDischargeTemp "+m.getDischargeTemp());
        System.out.println("getSupplyAirTemp "+m.getSupplyAirTemp());
        
        m.setCurrentTemp(70);
        m.setDamperPos(80);
        m.setReheatPos(60);
        m.setDesiredTemp(72);
        m.setDischargeTemp(87);
        m.setSupplyAirTemp(60);
        
        System.out.println("getRoomTemp "+m.getCurrentTemp());
        System.out.println("getDamperPos "+m.getDamperPos());
        System.out.println("getReheatPos "+m.getReheatPos());
        System.out.println("getDesiredTemp "+m.getDesiredTemp());
        System.out.println("getDischargeTemp "+m.getDischargeTemp());
        System.out.println("getSupplyAirTemp "+m.getSupplyAirTemp());
    
    
        ;
        //System.out.println(api.tagsDb.tagsMap);
        System.out.println(api.tagsDb.writeArrays);
        //api.tagsDb.saveString();
        //System.out.println(api.tagsDb.tagsString);
        //api.tagsDb.init();
        //System.out.println(api.tagsDb.tagsMap);
        System.out.println(api.tagsDb.writeArrays);
    
        VAVLogicalMap m1 = new VAVLogicalMap(ProfileType.VAV_REHEAT, 7000);
        
        m1.updateLoopParams();
    
        System.out.println("getRoomTemp "+m1.getCurrentTemp());
        System.out.println("getDamperPos "+m1.getDamperPos());
        System.out.println("getReheatPos "+m1.getReheatPos());
        System.out.println("getDesiredTemp "+m1.getDesiredTemp());
        System.out.println("getDischargeTemp "+m1.getDischargeTemp());
        System.out.println("getSupplyAirTemp "+m1.getSupplyAirTemp());
        
    }
}
