package a75f.io.logic.bo.building.system.dab;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import a75.io.algos.GenericPIController;
import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;
import a75f.io.logic.bo.building.hvac.Damper;

import static a75f.io.logic.bo.building.ZoneState.COOLING;
import static a75f.io.logic.bo.building.ZoneState.DEADBAND;
import static a75f.io.logic.bo.building.ZoneState.HEATING;

/**
 * Created by samjithsadasivan on 3/13/19.
 */

public class DabProfile extends ZoneProfile
{
    DabEquip dabEquip;
    
    public void addDabEquip(short addr, DabProfileConfiguration config, String floorRef, String roomRef) {
        dabEquip = new DabEquip(getProfileType(), addr);
        dabEquip.createEntities(config, floorRef, roomRef);
        dabEquip.init();
    }
    
    public void addDabEquip(short addr) {
        dabEquip = new DabEquip(getProfileType(), addr);
        dabEquip.init();
    }
    
    public void updateDabEquip(DabProfileConfiguration config) {
        dabEquip.update(config);
    }
    
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.PLC;
    }
    
    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return dabEquip.getProfileConfiguration();
    }
    
    @Override
    public Set<Short> getNodeAddresses()
    {
        return new HashSet<Short>(){{
            add((short)dabEquip.nodeAddr);
        }};
    }
    
    @Override
    public Equip getEquip()
    {
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \""+dabEquip.nodeAddr+"\"");
        return new Equip.Builder().setHashMap(equip).build();
    }
    
    @Override
    public void updateZonePoints()
    {
        double setTempCooling = dabEquip.getDesiredTempCooling();
        double setTempHeating = dabEquip.getDesiredTempHeating();
        double roomTemp = dabEquip.getCurrentTemp();
        GenericPIController damperOpController = dabEquip.damperController;
        
        Damper damper = new Damper();
        if (roomTemp > setTempCooling)
        {
            //Zone is in Cooling
            if (state != COOLING)
            {
                state = COOLING;
                damperOpController.reset();
            }
            damperOpController.updateControlVariable(roomTemp, setTempCooling);
        }
        else if (roomTemp < setTempHeating)
        {
            //Zone is in heating
            if (state != HEATING)
            {
                state = HEATING;
                damperOpController.reset();
            }
            damperOpController.updateControlVariable(setTempHeating, roomTemp);
        } else {
            if (state != DEADBAND) {
                state = DEADBAND;
                damperOpController.reset();
            }
           
        }
        
        setDamperLimits(damper);
        
        damper.currentPosition = (int)(damper.minPosition + (damper.maxPosition - damper.minPosition) * (damperOpController.getControlVariable() / damperOpController.getMaxAllowedError()));
    
        dabEquip.setDamperPos(damper.currentPosition);
    }
    
    protected void setDamperLimits(Damper d) {
        switch (state) {
            case COOLING:
                d.minPosition = (int)dabEquip.getDamperLimit("cooling", "min");
                d.maxPosition = (int)dabEquip.getDamperLimit("cooling", "max");
                break;
            case HEATING:
                d.minPosition = (int)dabEquip.getDamperLimit("heating", "min");;
                d.maxPosition = (int)dabEquip.getDamperLimit("heating", "max");;
                break;
            case DEADBAND:
                d.minPosition = 40;
                d.maxPosition = 80;
                break;
        }
    }
}
