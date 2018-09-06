package a75f.io.logic.bo.building.hvac;

/**
 * Created by samjithsadasivan on 6/1/18.
 */

public class VavUnit
{
    public Damper vavDamper;
    public Valve reheatValve;
    
    public VavUnit() {
        vavDamper = new Damper();
        reheatValve = new Valve();
    }
    
}
