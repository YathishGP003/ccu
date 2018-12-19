package a75f.io.logic.bo.building.system;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by samjithsadasivan on 11/5/18.
 */

public class DabStagedRtu extends SystemProfile
{
    @JsonIgnore
    public String getProfileName() {
        return "DAB Staged RTU";
    }
}
