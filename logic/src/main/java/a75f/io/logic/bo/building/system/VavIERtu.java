package a75f.io.logic.bo.building.system;

/**
 * Created by samjithsadasivan on 8/14/18.
 */

import com.fasterxml.jackson.annotation.JsonIgnore;

import a75.io.algos.vav.VavTRSystem;

/**
 * System profile to handle AHU via IE gateways.
 *
 */
public class VavIERtu extends SystemProfile
{
    private static final int CO2_MAX = 1000;
    private static final int CO2_MIN = 400;
    public VavIERtu() {
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
    
    public int getStaticPressure() {
        return (int)((VavTRSystem)trSystem).getCurrentSp();
    }
    
    @JsonIgnore
    public String getProfileName() {
        return "VAV IE RTU";
    }
}
