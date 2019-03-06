package a75f.io.logic.bo.building.plc;

import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import a75f.io.api.haystack.CCUHsApi;
import a75f.io.api.haystack.Equip;
import a75f.io.logic.L;
import a75f.io.logic.bo.building.BaseProfileConfiguration;
import a75f.io.logic.bo.building.ZoneProfile;
import a75f.io.logic.bo.building.definitions.ProfileType;

/**
 * Created by samjithsadasivan on 2/25/19.
 */

public class PlcProfile extends ZoneProfile
{
    PlcEquip plcEquip;
    
    public void addPlcEquip(short addr, PlcProfileConfiguration config, String floorRef, String roomRef) {
        plcEquip = new PlcEquip(getProfileType(), addr);
        plcEquip.createEntities(config, floorRef, roomRef);
        plcEquip.init();
    }
    
    public void addPlcEquip(short addr) {
        plcEquip = new PlcEquip(getProfileType(), addr);
        plcEquip.init();
    }
    
    public void updatePlcEquip(PlcProfileConfiguration config) {
        plcEquip.update(config);
    }
    
    @Override
    public ProfileType getProfileType()
    {
        return ProfileType.PLC;
    }
    
    @Override
    public BaseProfileConfiguration getProfileConfiguration(short address)
    {
        return plcEquip.getProfileConfiguration();
    }
    
    @Override
    public Set<Short> getNodeAddresses()
    {
        return new HashSet<Short>(){{
            add((short)plcEquip.nodeAddr);
        }};
    }
    
    @Override
    public Equip getEquip()
    {
        HashMap equip = CCUHsApi.getInstance().read("equip and group == \""+plcEquip.nodeAddr+"\"");
        return new Equip.Builder().setHashMap(equip).build();
    }
    
    @Override
    public void updateZonePoints() {
    
        double pv = plcEquip.getProcessVariable();
        double tv = plcEquip.getTargetValue();
        double cv;
        
        if (plcEquip.isEnabledAnalog2InForSp()) {
            Log.d(L.TAG_CCU_ZONE,"Use analog 2 offset "+plcEquip.getSpVariable());
            tv = plcEquip.getSpVariable() + plcEquip.getSpSensorOffset();
        }
        
        plcEquip.getPIController().updateControlVariable(tv, pv);
        
        if (plcEquip.isEnabledZeroErrorMidpoint()) {
            cv = 50.0 + plcEquip.getPIController().getControlVariable() * 50.0 / plcEquip.getPIController().getMaxAllowedError();
        } else
        {
            cv = plcEquip.getPIController().getControlVariable() * 100.0 / plcEquip.getPIController().getMaxAllowedError();
        }
        plcEquip.setControlVariable(Math.round(100*cv)/100);
        plcEquip.getPIController().dump();
        Log.d(L.TAG_CCU_ZONE, "PlcProfile, pv: "+pv+", tv: "+tv+", cv: "+cv);
    }
    
}
