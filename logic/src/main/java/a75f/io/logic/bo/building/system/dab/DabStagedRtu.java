package a75f.io.logic.bo.building.system.dab;

import com.fasterxml.jackson.annotation.JsonIgnore;

import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.system.SystemProfile;

/**
 * Created by samjithsadasivan on 11/5/18.
 */

public class DabStagedRtu extends SystemProfile
{
    @JsonIgnore
    public String getProfileName() {
        return "DAB Staged RTU";
    }
    
    @Override
    public ProfileType getProfileType() {
        return ProfileType.SYSTEM_DAB_STAGED_RTU;
    }
    
    @Override
    public void doSystemControl() {
        DabSystemController.getInstance().runDabSystemControlAlgo();
        //updateSystemPoints();
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
    public boolean isCoolingActive(){
        return false;
    }
    
    @Override
    public boolean isHeatingActive(){
        return false;
    }
    
    @Override
    public synchronized void deleteSystemEquip() {
    }
    
    @Override
    public String getStatusMessage(){
        return "";
    }
}
