package a75f.io.logic.bo.building.hvac;

/**
 * Created by samjithsadasivan on 8/28/18.
 */

public class VavAcbUnit extends VavUnit
{
    public Valve chwValve;
    public boolean condensateStatus;

    public VavAcbUnit() {
        super();
        chwValve = new Valve();
    }

}
