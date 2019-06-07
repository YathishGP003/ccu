package a75f.io.logic.bo.building.erm;

import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;

public class EmrProfile extends ZoneProfile
{
    EmrEquip emrEquip;
    
    public void addEmrEquip(short addr, String floorRef, String roomRef) {
        emrEquip = new EmrEquip(getProfileType(), addr);
        emrEquip.createEntities(floorRef, roomRef);
        emrEquip.init();
    }
    
    public void addEmrEquip(short addr) {
        emrEquip = new EmrEquip(getProfileType(), addr);
    }
    
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.EMR;
    }
    
    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return null;
    }
    
    @Override
    public Set<Short> getNodeAddresses()
    {
        return new HashSet<Short>(){{
            add((short)emrEquip.nodeAddr);
        }};
    }
    
    @Override
    public Equip getEquip()
    {
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \"" + emrEquip.nodeAddr + "\"");
        return new Equip.Builder().setHashMap(equip).build();
    }
    
    @Override
    public void updateZonePoints() {
        Log.d(L.TAG_CCU_ZONE, "EmrProfile, Do Nothing !");
    }
}
