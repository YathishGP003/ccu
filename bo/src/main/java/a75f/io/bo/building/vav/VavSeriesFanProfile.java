package a75f.io.bo.building.vav;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonIgnore;

import a75f.io.bo.building.definitions.ProfileType;

/**
 * Created by samjithsadasivan on 8/23/18.
 */

public class VavSeriesFanProfile extends VavProfile
{
    @JsonIgnore
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.VAV_SERIES_FAN;
    }
    
    @JsonIgnore
    @Override
    public void updateZoneControls(double desiredTemp) {
        Log.d(TAG, "VAV Series Fan Control");
    }
}
