package a75f.io.bo.building.hvac;

/**
 * Created by samjithsadasivan on 8/28/18.
 */

public interface Control
{
    void applyOverride(int val);
    void releaseOverride();
}
