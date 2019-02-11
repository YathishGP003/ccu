package a75f.io.logic.bo.building.system.dab;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
    public void doSystemControl() {
    
    }
    
    @Override
    public void addSystemEquip() {
    
    }
    
    @Override
    public synchronized void deleteSystemEquip() {
    }
}
