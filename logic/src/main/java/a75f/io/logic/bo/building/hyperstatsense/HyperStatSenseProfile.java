package a75f.io.logic.bo.building.hyperstatsense;

import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.plc.PlcEquip;
import a75f.io.logic.bo.building.plc.PlcProfileConfiguration;

public class HyperStatSenseProfile extends ZoneProfile {
    HyperStatSenseEquip mHyperStatSenseEquip;

    public void addHyperStatSenseEquip(ProfileType type,int node, HyperStatSenseConfiguration config, String floorRef, String roomRef) {
        mHyperStatSenseEquip = new HyperStatSenseEquip(type,node);
        mHyperStatSenseEquip.createEntities(config, floorRef, roomRef);
        mHyperStatSenseEquip.init();
    }

    public void addHyperStatSenseEquip(int node) {
        mHyperStatSenseEquip = new HyperStatSenseEquip(getProfileType(),node);
        mHyperStatSenseEquip.init();
    }


    @Override
    public void updateZonePoints() {

    }

    @Override
    public ProfileType getProfileType() {
        return mHyperStatSenseEquip.mProfileType;
    }

    @Override
    public <T extends BaseProfileConfiguration> T getProfileConfiguration(short address) {
        return null;
    }
}
