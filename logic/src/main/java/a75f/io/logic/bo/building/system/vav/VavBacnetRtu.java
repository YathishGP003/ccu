package a75f.io.logic.bo.building.system.vav;

import java.util.HashMap;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.bo.building.definitions.ProfileType;

/**
 * Created by samjithsadasivan on 11/5/18.
 */

public class VavBacnetRtu extends VavSystemProfile
{
    
    public String getProfileName() {
        return "VAV Bacnet RTU";
    }
    
    @Override
    public void doSystemControl() {
    
    }
    
    @Override
    public void addSystemEquip() {
    
    }
    
    @Override
    public boolean isCoolingAvailable() {
        return false;
    }
    
    @Override
    public boolean isHeatingAvailable() {
        return false;
    }
    
    @Override
    public ProfileType getProfileType() {
        return ProfileType.SYSTEM_VAV_BACNET_RTU;
    }
    
    @Override
    public void deleteSystemEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and system");
        if (equip.get("profile").equals(ProfileType.SYSTEM_VAV_BACNET_RTU.name())) {
            CCUHsApi.getInstance().deleteEntityTree(equip.get("id").toString());
        }
    }
    
    @Override
    public String getStatusMessage(){
        return "";
    }
    
    
}
