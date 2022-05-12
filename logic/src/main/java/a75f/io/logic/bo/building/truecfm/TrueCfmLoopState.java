package a75f.io.logic.bo.building.truecfm;


public enum TrueCfmLoopState {
    //Naming follows a simple convention SystemState_ZoneState_Limit
    COOLING_COOLING_MIN,
    COOLING_COOLING_MAX,
    COOLING_HEATING_MIN,
    COOLING_HEATING_MAX,
    HEATING_COOLING_MIN,
    HEATING_COOLING_MAX,
    HEATING_HEATING_MIN,
    HEATING_HEATING_MAX
}
