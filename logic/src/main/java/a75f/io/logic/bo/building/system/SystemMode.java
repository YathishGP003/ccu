package a75f.io.logic.bo.building.system;

/**
 * Created by samjithsadasivan on 1/18/19.
 */

public enum SystemMode
{
    OFF("OFF"), AUTO("AUTO"), COOLONLY("COOL ONLY"), HEATONLY("HEAT ONLY");
    
    public String displayName;
    
    SystemMode(String str) {
        displayName = str;
    }
    
    public int getValue() {
        return ordinal() + 1;
    }
    
    public static SystemMode getEnum(String value) {
        for(SystemMode v : values())
            if(v.displayName.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
