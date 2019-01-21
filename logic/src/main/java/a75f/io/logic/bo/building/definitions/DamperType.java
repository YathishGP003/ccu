package a75f.io.logic.bo.building.definitions;

/**
 * Created by samjithsadasivan on 1/11/19.
 */

public enum DamperType
{
    ZeroToTenV("0-10v"), TwoToTenV("2-10v"), TenToTwov("10-2v"), TenToZeroV("10-0v"), MAT ("MAT");
    
    public String displayName;
    
    DamperType(String str) {
        displayName = str;
    }
    
    public static DamperType getEnum(String value) {
        for(DamperType v : values())
            if(v.displayName.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
