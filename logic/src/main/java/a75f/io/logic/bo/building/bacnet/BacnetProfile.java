package a75f.io.logic.bo.building.bacnet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.api.haystack.bacnet.parser.BacnetModelDetailResponse;
import a75f.io.api.haystack.bacnet.parser.BacnetPoint;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;

public class BacnetProfile extends ZoneProfile {

    BacnetEquip bacnetEquip;

    public void addBacAppEquip(String configParam, String modelConfig, String deviceId, String slaveId, String floorRef, String roomRef, BacnetModelDetailResponse equipmentDevice,
                               ProfileType profileType, String moduleLevel,String modelVersion, boolean isSystemEquip) {
        bacnetEquip = new BacnetEquip(profileType, Long.parseLong(slaveId));
        bacnetEquip.createEntities(configParam, modelConfig, deviceId, slaveId, floorRef, roomRef, equipmentDevice,null, moduleLevel, modelVersion, isSystemEquip);
    }

    public void addBacAppEquip(long slaveId, ProfileType profileType) {
        bacnetEquip = new BacnetEquip(profileType, slaveId);
        bacnetEquip.init(slaveId);
    }

    public void updateBacnetEquip(String equipRef, List<BacnetPoint> configParams){
        bacnetEquip.updateHaystackPoints(equipRef,configParams);
    }

    @Override
    public void updateZonePoints() {
    }

    @Override
    public ProfileType getProfileType() {
        return bacnetEquip.profileType;
    }

    @Override
    public <T extends BaseProfileConfiguration> T getProfileConfiguration(short address) {
        return null;
    }

    @Override
    public Set<Short> getNodeAddresses() {
        return new HashSet<>();
    }

    public Long getSlaveId(){
        return bacnetEquip.slaveId;
    }

    @Override
    public Equip getEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and bacnet and not equipRef and " +
                        "group == \"" + bacnetEquip.slaveId + "\"");
        return new Equip.Builder().setHashMap(equip).build();
    }

}
