package a75f.io.logic.bo.building.vav;

import a75f.io.logic.bo.building.BaseProfileConfiguration;

/**
 * Created by samjithsadasivan on 6/27/18.
 */

public class VavProfileConfiguration extends BaseProfileConfiguration
{
    
    int minDamperCooling;
    int maxDamperCooling;
    int minDamperHeating;
    int maxDamperHeating;
    
    public int getMinDamperCooling()
    {
        return minDamperCooling;
    }
    public void setMinDamperCooling(int minDamperCooling)
    {
        this.minDamperCooling = minDamperCooling;
    }
    public int getMaxDamperCooliing()
    {
        return maxDamperCooling;
    }
    public void setMaxDamperCooliing(int maxDamperCooliing)
    {
        this.maxDamperCooling = maxDamperCooliing;
    }
    public int getMinDamperHeating()
    {
        return minDamperHeating;
    }
    public void setMinDamperHeating(int minDamperHeating)
    {
        this.minDamperHeating = minDamperHeating;
    }
    public int getMaxDamperHeating()
    {
        return maxDamperHeating;
    }
    public void setMaxDamperHeating(int maxDamperHeating)
    {
        this.maxDamperHeating = maxDamperHeating;
    }
}
