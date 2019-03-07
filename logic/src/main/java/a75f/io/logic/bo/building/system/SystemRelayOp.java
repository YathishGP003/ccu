package a75f.io.logic.bo.building.system;

/**
 * Created by samjithsadasivan on 2/25/19.
 */

public class SystemRelayOp
{
    public boolean isEnabled()
    {
        return isEnabled;
    }
    public void setEnabled(boolean enabled)
    {
        isEnabled = enabled;
    }
    private boolean isEnabled;
    
    public double getRelayAssociation()
    {
        return relayAssociation;
    }
    public void setRelayAssociation(double relayAssociation)
    {
        this.relayAssociation = relayAssociation;
    }
    private double relayAssociation;
    
    public boolean isAvailable()
    {
        return isAvailable;
    }
    public void setAvailable(boolean available)
    {
        isAvailable = available;
    }
    private boolean isAvailable;
}
