package a75f.io.bo.kinvey;

import com.google.api.client.json.GenericJson;
import com.google.api.client.util.Key;

/**
 * Created by Yinten on 9/20/2017.
 */

public class AlgoTuningParameters extends GenericJson
{
    @Key("_id")
    private String id = DalContext.getInstance().getKinveyId();
    
    @Key("_ccuName")
    private String mCCUName = "";
    
    public static class LightTuners
    {
        public static final String LIGHT_INTENSITY_OCCUPANT_DETECTED              =
                "mLightingIntensityOccupantDetected";
        public static final String LIGHT_MIN_LIGHTING_CONTROL_OVERRIDE_IN_MINUTES =
                "mMinLightingControlOverrideInMinutes";
    }
    
    public static class SSETuners
    {
        public static final String SSE_COOLING_DEADBAND  = "mCoolingDeadBand";
        public static final String SSE_HEATING_DEADBAND  = "mHeatingDeadBand";
        public static final String SSE_BUILDING_MAX_TEMP = "mBuildingAllowNoHotter";
        public static final String SSE_BUILDING_MIN_TEMP = "mBuildingAllowNoCooler";
        public static final String SSE_USER_MIN_TEMP     = "mUserAllowNoCooler";
        public static final String SSE_USER_MAX_TEMP     = "mUserAllowNoHotter";
        public static final String SSE_USER_ZONE_SETBACK = "mZoneSetBack";
        public static final String SSE_FORCED_OCCU_TIME  = "mForcedOccupiedTimePeriod";
    }
}