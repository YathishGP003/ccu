package a75f.io.logic.bo.building.bpos;

import java.util.HashSet;
import java.util.Set;

import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;


public class BPOSProfile  extends ZoneProfile {
    BPOSEquip mBPOSEquip;


    public void addBPOSEquip(ProfileType type, int node, BPOSConfiguration config, String floorRef, String roomRef){
        mBPOSEquip = new BPOSEquip(type,node);
        mBPOSEquip.createEntities(config,floorRef,roomRef);
        mBPOSEquip.init();
    }

    public void addBPOSEquip( int node){
        mBPOSEquip = new BPOSEquip(ProfileType.BPOS,node);
        mBPOSEquip.init();
    }

    public void updateBPOS(ProfileType type,int node, BPOSConfiguration config, String floorRef, String roomRef){
        mBPOSEquip.update(type, node, config, floorRef, roomRef);
        mBPOSEquip.init();
    }

    @Override
    public void updateZonePoints() {
           //
    }

    @Override
    public ProfileType getProfileType() {
        return ProfileType.BPOS;
    }

    @Override
    public BPOSConfiguration getProfileConfiguration(short address) {
        return mBPOSEquip.getbposconfiguration();
    }

    @Override
    public Set<Short> getNodeAddresses()
    {
        return new HashSet<Short>(){{
            add((short)mBPOSEquip.mNodeAddr);
        }};
    }


}
