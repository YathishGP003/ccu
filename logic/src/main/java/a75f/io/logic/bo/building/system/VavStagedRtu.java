package a75f.io.logic.bo.building.system;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by samjithsadasivan on 8/14/18.
 */

public class VavStagedRtu extends SystemProfile
{
    
    @JsonIgnore
    public String getProfileName() {
        return "VAV Staged RTU";
    }
}
