package a75f.io.logic.bo.building.definitions;

/**
 * Created by samjithsadasivan on 1/28/19.
 */

public enum DamperShape
{
    ROUND("Round"), SQUARE("Square"), RECTANGULAR("Rectangular");
    
    public String displayName;
    
    DamperShape(String str) {
        displayName = str;
    }
    
    public static DamperShape getEnum(String value) {
        for(DamperShape v : values())
            if(v.displayName.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
