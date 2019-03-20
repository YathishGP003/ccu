package a75f.io.logic.bo.building.system.vav;

/**
 * Created by samjithsadasivan on 8/14/18.
 */

import java.util.HashMap;

import a75.io.algos.vav.VavTRSystem;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.logic.bo.building.definitions.ProfileType;

/**
 * System profile to handle AHU via IE gateways.
 *
 */
public class VavIERtu extends VavSystemProfile
{
    private static final int CO2_MAX = 1000;
    private static final int CO2_MIN = 400;
    public void initTRSystem() {
        trSystem =  new VavTRSystem();
    }
    
    
    public  int getSystemSAT() {
        return ((VavTRSystem)trSystem).getCurrentSAT();
    }
    
    public  int getSystemCO2() {
        return ((VavTRSystem)trSystem).getCurrentCO2();
    }
    
    public  int getSystemOADamper() {
        return (((VavTRSystem)trSystem).getCurrentCO2() - CO2_MIN) * 100 / (CO2_MAX - CO2_MIN);
    }
    
    public double getStaticPressure() {
        return ((VavTRSystem)trSystem).getCurrentSp();
    }
    
    public String getProfileName() {
        return "VAV IE RTU";
    }
    
    @Override
    public ProfileType getProfileType() {
        return ProfileType.SYSTEM_VAV_IE_RTU;
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
    public void deleteSystemEquip() {
        HashMap equip = CCUHsApi.getInstance().read("equip and system");
        if (equip.get("profile").equals(ProfileType.SYSTEM_VAV_IE_RTU.name())) {
            CCUHsApi.getInstance().deleteEntityTree(equip.get("id").toString());
        }
    }
}
