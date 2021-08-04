package a75f.io.logic.bo.building.hyperstatsense;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;


/*
 * created by spoorthidev on 30-May-2021
 */

public class HyperStatSenseProfile extends ZoneProfile {
    HyperStatSenseEquip mHyperStatSenseEquip;

    public void addHyperStatSenseEquip(ProfileType type, int node, HyperStatSenseConfiguration config, String floorRef, String roomRef) {
        mHyperStatSenseEquip = new HyperStatSenseEquip(type, node);
        mHyperStatSenseEquip.createEntities(config, floorRef, roomRef);
        mHyperStatSenseEquip.init();
    }

    public void addHyperStatSenseEquip(int node) {
        mHyperStatSenseEquip = new HyperStatSenseEquip(getProfileType(), node);
        mHyperStatSenseEquip.init();
    }

    public void updateHyperStatSenseEquip(ProfileType type, int node, HyperStatSenseConfiguration config, String floorRef, String roomRef) {
        mHyperStatSenseEquip.update(type, node, config, floorRef, roomRef);
        mHyperStatSenseEquip.init();
    }

    @Override
    public void updateZonePoints() {
        //Not required for current configuartion
    }

    @Override
    public ProfileType getProfileType() {
        return ProfileType.HYPERSTAT_SENSE;
    }

    @Override
    public HyperStatSenseConfiguration getProfileConfiguration(short address) {
        return mHyperStatSenseEquip.getHyperStatSenseConfig();
    }

    @Override
    public Set<Short> getNodeAddresses()
    {
        return new HashSet<Short>(){{
            add((short)mHyperStatSenseEquip.mNodeAddr);
        }};
    }
    
    @Override
    public Equip getEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \"" + mHyperStatSenseEquip.mNodeAddr + "\"");
        return new Equip.Builder().setHashMap(equip).build();
    }
}
