package a75f.io.logic.bo.building.definitions;

/**
 * Created by samjithsadasivan on 1/11/19.
 */

public enum ReheatType
{
    ZeroToTenV("0-10v"), TwoToTenV("2-10v"), TenToTwov("10-2v"), TenToZeroV("10-0v"), Pulse ("Pulsed Electric"),
    OneStage("1 Stage"), TwoStage("2 Stage");
    
    public String displayName;
    
    ReheatType(String str) {
        displayName = str;
    }
    
    public static ReheatType getEnum(String value) {
        for(ReheatType v : values())
            if(v.displayName.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
