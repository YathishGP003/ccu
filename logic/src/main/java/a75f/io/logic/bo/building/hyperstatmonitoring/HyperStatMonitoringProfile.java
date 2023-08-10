package a75f.io.logic.bo.building.hyperstatmonitoring;

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

public class HyperStatMonitoringProfile extends ZoneProfile {
    HyperStatMonitoringEquip monitoringEquip;

    public void addHyperStatMonitoringEquip(ProfileType type, int node, HyperStatMonitoringConfiguration config, String floorRef, String roomRef) {
        monitoringEquip = new HyperStatMonitoringEquip(type, node);
        monitoringEquip.createEntities(config, floorRef, roomRef);
        monitoringEquip.init();
    }

    public void addHyperStatMonitoringEquip(int node) {
        monitoringEquip = new HyperStatMonitoringEquip(getProfileType(), node);
        monitoringEquip.init();
    }

    public void updateHyperStatMonitoringEquip(ProfileType type, int node, HyperStatMonitoringConfiguration config, String floorRef, String roomRef) {
        monitoringEquip.update(type, node, config, floorRef, roomRef);
        monitoringEquip.init();
    }

    @Override
    public void updateZonePoints() {
        //Not required for current configuartion
    }

    @Override
    public ProfileType getProfileType() {
        return ProfileType.HYPERSTAT_MONITORING;
    }

    @Override
    public HyperStatMonitoringConfiguration getProfileConfiguration(short address) {
        return monitoringEquip.getHyperStatMonitoringConfig();
    }

    @Override
    public Set<Short> getNodeAddresses()
    {
        return new HashSet<Short>(){{
            add((short) monitoringEquip.mNodeAddr);
        }};
    }
    
    @Override
    public Equip getEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \"" + monitoringEquip.mNodeAddr + "\"");
        return new Equip.Builder().setHashMap(equip).build();
    }
}
