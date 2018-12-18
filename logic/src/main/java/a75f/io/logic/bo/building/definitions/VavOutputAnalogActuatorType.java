package a75f.io.logic.bo.building.definitions;

/**
 * Created by samjithsadasivan on 12/17/18.
 */

public enum VavOutputAnalogActuatorType
{
    ZeroToTenV("0-10v"), TwoToTenV("2-10v"), TenToTwov("10-2v"), TenToZeroV("10-0v"), Pulse("Pulse");
    
    public String displayName;
    
    VavOutputAnalogActuatorType(String str) {
        displayName = str;
    }
    
    public static VavOutputAnalogActuatorType getEnum(String value) {
        for(VavOutputAnalogActuatorType v : values())
            if(v.displayName.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
