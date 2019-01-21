package a75f.io.logic.bo.building;

/**
 * Created by samjithsadasivan on 11/9/18.
 */

public enum ZonePriority
{
    NO(0, 0), LOW(1, 1), MEDIUM(2, 10), HIGH(3, 50);
    
    public int multiplier;
    public int val;
    ZonePriority(int m, int n) {
        multiplier = m;
        val = n;
    }
}
