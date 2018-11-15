package a75f.io.logic.bo.building;

/**
 * Created by samjithsadasivan on 11/9/18.
 */

public enum ZonePriority
{
    NO(0), LOW(1), MEDIUM(2), HIGH(3);
    
    public int multiplier;
    ZonePriority(int m) {
        multiplier = m;
    }
}
