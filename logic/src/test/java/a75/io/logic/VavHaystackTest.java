package a75.io.logic;

import org.junit.Test;

import java.util.ArrayList;

import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.vav.VAVLogicalMap;
import a75f.io.api.haystack.CCUHsApi;

/**
 * Created by samjithsadasivan on 9/12/18.
 */

public class VavHaystackTest
{
    
    @Test
    public void testVavReheatLogicalMap() {
        CCUHsApi api = new CCUHsApi();
        VAVLogicalMap m = new VAVLogicalMap(ProfileType.VAV_REHEAT, 7000);
        ArrayList points = CCUHsApi.getInstance().readAll("point and group == \"7000\"");
        for (Object a : points) {
            System.out.println(a);
        }
        
        m.setRoomTemp(70);
        m.setDamperPos(80);
        m.setReheatPos(60);
        m.setDesiredTemp(72);
        m.setDischargeTemp(87);
        m.setSupplyAirTemp(60);
        
        System.out.println("getRoomTemp "+m.getRoomTemp());
        System.out.println("getDamperPos "+m.getDamperPos());
        System.out.println("getReheatPos "+m.getReheatPos());
        System.out.println("getDesiredTemp "+m.getDesiredTemp());
        System.out.println("getDischargeTemp "+m.getDischargeTemp());
        System.out.println("getSupplyAirTemp "+m.getSupplyAirTemp());
    }
}
