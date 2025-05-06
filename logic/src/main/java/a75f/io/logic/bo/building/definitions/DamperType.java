package a75f.io.logic.bo.building.definitions;

/**
 * Created by samjithsadasivan on 1/11/19.
 */

public enum DamperType
{
    ZeroToTenV("0-10v"), TwoToTenV("2-10v"), TenToTwov("10-2v"), TenToZeroV("10-0v"), MAT ("Smart Damper"), ZeroToFiveV("0-5v");
    
    public String displayName;
    
    DamperType(String str) {
        displayName = str;
    }
    
    public static DamperType getEnum(String value) {
        for(DamperType v : values())
            if(v.displayName.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }

    // only for bypass damper ,because for vav and bypass damper enum position is mismatching
    public static String getBypassDamperDamperTypeString(int typeVal) {
        switch (typeVal) {
            case 1: {
                return "2-10v";
            }
            case 2: {
                return "10-0v";
            }
            case 3: {
                return "10-2v";
            }
            case 4: {
                return "Smart Damper";
            }
            case 5: {
                return "0-5v";
            }
            case 0:
            default: {
                return "0-10v";
            }
        }
    }
}
