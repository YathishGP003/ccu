package a75f.io.logic.bo.building.system.dab;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;

/**
 * Created by samjithsadasivan on 3/13/19.
 */

public class DabProfile extends ZoneProfile
{
    DabEquip dabEquip;
    
    public void addDabEquip(short addr, DabProfileConfiguration config, String floorRef, String roomRef) {
        dabEquip = new DabEquip(getProfileType(), addr);
        dabEquip.createEntities(config, floorRef, roomRef);
        dabEquip.init();
    }
    
    public void addDabEquip(short addr) {
        dabEquip = new DabEquip(getProfileType(), addr);
        dabEquip.init();
    }
    
    public void updateDabEquip(DabProfileConfiguration config) {
        dabEquip.update(config);
    }
    
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.PLC;
    }
    
    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return dabEquip.getProfileConfiguration();
    }
    
    @Override
    public Set<Short> getNodeAddresses()
    {
        return new HashSet<Short>(){{
            add((short)dabEquip.nodeAddr);
        }};
    }
    
    @Override
    public Equip getEquip()
    {
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \""+dabEquip.nodeAddr+"\"");
        return new Equip.Builder().setHashMap(equip).build();
    }
    
    @Override
    public void updateZonePoints()
    {
    }
}
